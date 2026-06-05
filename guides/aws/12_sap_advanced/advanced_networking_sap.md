# SAP — Advanced Networking: BGP, ECMP, DX Redundancy

## 1. The Problem

Enterprise connectivity at scale surfaces routing decisions that don't exist in simple setups:

- Two Direct Connect (DX) circuits: which one handles traffic? What happens when one fails? How do you guarantee automatic failover?
- Two DX connections to the same location: does traffic load-balance or does one sit idle?
- A customer wants 12.5 Gbps of VPN bandwidth: individual tunnels cap at 1.25 Gbps — how do you aggregate?
- Network Firewall in an inspection VPC: how do you prevent traffic asymmetry when return traffic takes a different path?
- BGP communities on DX: how do you influence AWS's routing decisions for your prefixes?

These are BGP routing engineering problems applied to AWS infrastructure.

---

## 2. What AWS Built

| Component | Purpose |
|-----------|---------|
| **BGP on Direct Connect** | Dynamic routing between on-premises and AWS |
| **ECMP on Transit Gateway** | Equal-Cost Multi-Path — load balance across multiple tunnels |
| **Direct Connect Gateway** | Connect one DX to multiple VPCs/regions |
| **Transit VIF** | Single VIF to Transit Gateway → scales to many VPCs |
| **SiteLink** | On-prem ↔ on-prem routing via AWS backbone (DX) |
| **TGW Appliance Mode** | Prevent asymmetric routing through inspection appliances |

---

## 3. How It Works

### BGP Fundamentals for AWS

**BGP in the AWS context:**
```
AWS uses BGP AS number: 7224

Your on-premises router uses a BGP ASN:
  Private ASN: 64512–65534 (most common for enterprise)
  Public ASN: if you own one

BGP session (BGP peering):
  On-premises router ↔ AWS (on Direct Connect virtual interface)
  Exchanges: route prefixes (which IP ranges are reachable via this path)
```

**BGP Path Attributes (for exam and production):**

**LOCAL_PREF — outbound traffic preference (AWS → on-premises):**
```
Set on YOUR on-premises router for routes advertised TO AWS
Higher LOCAL_PREF = preferred path

Scenario: 2 DX connections, prefer Primary for outbound traffic
  Primary DX router: set LOCAL_PREF = 200 for all AWS prefixes
  Secondary DX router: set LOCAL_PREF = 100 for all AWS prefixes

Result: AWS uses Primary DX for traffic destined to on-premises
(Unless Primary fails → failover to Secondary automatically)

LOCAL_PREF is applied ON-PREMISES and is internal to your AS
```

**AS_PATH Prepending — inbound traffic preference (on-premises → AWS):**
```
Manipulate inbound routing by making a path appear LONGER (less preferred)

Your on-premises AS: 65001
You advertise 192.168.0.0/16 via both DX connections

Primary DX advertisement:
  192.168.0.0/16: AS_PATH = 65001 (natural, short)

Secondary DX advertisement (prepend to make it longer):
  192.168.0.0/16: AS_PATH = 65001 65001 65001 (3x prepend)

Result: AWS BGP sees Primary path as shorter → prefers Primary for inbound
When Primary fails: Secondary path used (longer but only available path)

Prepend using: "set as-path prepend 65001 65001 65001" in route-map
```

**MED (Multi-Exit Discriminator) — suggest preferred entry point:**
```
Used when you have multiple connections to the SAME ISP/provider
Suggests to adjacent AS which path to prefer entering YOUR AS

Lower MED = preferred

AWS usage: AWS sets MED on DX advertisements to suggest preferred paths
Less commonly tuned by customers (LOCAL_PREF and AS_PATH are more used)
```

**BGP Communities on Direct Connect:**

AWS honors specific BGP community tags to control how your routes are propagated:
```
7224:7100 — Do not export routes beyond the local region
7224:7200 — Export routes to all AWS regions in same continent
7224:7300 — Export routes globally to all AWS regions

Use cases:
  7224:7100: on-prem prefix only needed in one AWS region (don't advertise globally)
  7224:7300: on-prem prefix needed in all regions (global backbone connectivity)

Set on your router: "set community 7224:7300" in route-map for DX VIF
```

---

### Direct Connect Redundancy Models

**No Redundancy (avoid for production):**
```
On-premises router → 1 DX connection → AWS
Single point of failure: DX port, DX location, fiber cut
RPO: undefined (complete failure until circuit restored = days)
```

**High Availability (2 DX, same location):**
```
On-premises router 1 → DX Port 1 → DX Location → AWS
On-premises router 2 → DX Port 2 → DX Location → AWS

Protects against: DX port failure, router failure
Does NOT protect against: DX location failure (datacenter disaster)

BGP configuration: PRIMARY preferred, SECONDARY standby
  LOCAL_PREF: Primary=200, Secondary=100
  Result: automatic failover when Primary DX BGP session drops
```

**Maximum Resiliency (2 DX, 2 separate locations):**
```
On-premises router 1 → DX Location 1 → AWS
On-premises router 2 → DX Location 2 → AWS

Protects against: single DX location failure
Two completely separate physical paths
AWS recommends: different telecom providers at each location

This is AWS's recommended model for enterprise production
```

**Ultra High Availability (4 connections, 2 locations):**
```
On-premises router 1 → DX Location 1 → AWS Connection 1
On-premises router 1 → DX Location 1 → AWS Connection 2 (2 ports same location)
On-premises router 2 → DX Location 2 → AWS Connection 3
On-premises router 2 → DX Location 2 → AWS Connection 4

4 independent connections, 2 locations
Protects against: 3 simultaneous failures
Use case: financial institutions, critical infrastructure
```

**DX + VPN Backup:**
```
Primary: Direct Connect (dedicated, high bandwidth, low latency)
Backup: Site-to-Site VPN over internet

BGP configuration:
  DX: BGP session with SHORT AS_PATH and HIGH LOCAL_PREF
  VPN: BGP session with LONGER AS_PATH and LOW LOCAL_PREF

Result: DX preferred, VPN used only when DX fails
VPN provides: internet-based backup, encryption (DX not encrypted by default)

DX encryption options:
  Option 1: MACsec (Layer 2 encryption) — available on DX Dedicated
  Option 2: IPSec over DX (VPN tunnel over DX for encryption + DX bandwidth)
```

---

### ECMP with Transit Gateway

**Why ECMP matters:**
```
VPN tunnel limit: each Site-to-Site VPN provides 2 tunnels × 1.25 Gbps = 2.5 Gbps max
For 10 Gbps bandwidth: need 8 tunnels (in 4 VPN connections)
ECMP enables load balancing across all tunnels simultaneously

Without TGW (using VGW): no ECMP → VGW only uses single active tunnel
With TGW: ECMP supported → load balances across multiple tunnels

Aggregate bandwidth: up to 50 Gbps per VPC attachment with multiple VPN connections
Typical: 10 tunnels × 1.25 Gbps = 12.5 Gbps
```

**ECMP Configuration:**
```
Requirements for ECMP:
  1. Must use Transit Gateway (not VGW — VGW has no ECMP)
  2. All tunnels must advertise identical BGP attributes
     - Same prefix (same IP range)
     - Same AS_PATH length
     - Same LOCAL_PREF
     (If any attribute differs, BGP picks "best" path → no ECMP)

TGW setting: Equal-cost multi-path routing = Enabled

VPN connections to TGW:
  VPN-1: tunnel-1 (1.25 Gbps), tunnel-2 (1.25 Gbps)
  VPN-2: tunnel-1 (1.25 Gbps), tunnel-2 (1.25 Gbps)
  VPN-3: tunnel-1 (1.25 Gbps), tunnel-2 (1.25 Gbps)
  ...
  All 8+ tunnels with identical BGP attributes → ECMP load balancing
```

---

### Complex TGW Topologies

**Inspection VPC Pattern (Security Appliance):**
```
Problem: Route spoke VPC traffic through Network Firewall for inspection

Architecture:
  Spoke VPC A (10.1.0.0/16) → TGW
  Spoke VPC B (10.2.0.0/16) → TGW
  Inspection VPC (10.0.0.0/24) → TGW
    ├── Network Firewall
    └── TGW attachment (Appliance Mode = ENABLED)

TGW Route Tables:
  Spoke Route Table:
    10.0.0.0/8 → Inspection VPC attachment (all traffic to inspection)
    0.0.0.0/0  → Inspection VPC attachment

  Inspection Route Table:
    10.1.0.0/16 → Spoke A attachment
    10.2.0.0/16 → Spoke B attachment

WITHOUT Appliance Mode:
  Traffic from A → B goes through Firewall
  Return traffic from B → A might take different TGW path
  Firewall sees one-way traffic → marks as invalid → drops
  ASYMMETRIC ROUTING BREAKS STATEFUL INSPECTION

WITH TGW Appliance Mode:
  TGW pins flows to same Availability Zone attachment
  Forward and return traffic through same Firewall instance
  Stateful inspection works correctly

Must enable: Appliance Mode on the Inspection VPC's TGW attachment
```

**Shared Services VPC:**
```
SharedServices VPC connected to TGW:
  - Active Directory
  - Route53 Resolver endpoints
  - CI/CD tooling
  - Monitoring collectors

TGW Route Table for Shared Services:
  10.0.0.0/8 → All spoke attachments (can reach all VPCs)
  Spoke VPCs can reach SharedServices but NOT each other
  (Inter-spoke communication blocked unless explicit route exists)
```

**TGW Cross-Region Peering:**
```
TGW us-east-1 ←→ TGW eu-west-1 (peering attachment)

Route tables: STATIC ROUTES only (no BGP on TGW peering)
  us-east-1 TGW: 10.2.0.0/16 → EU TGW peering attachment (manual)
  eu-west-1 TGW: 10.1.0.0/16 → US TGW peering attachment (manual)

This is a gotcha: TGW peering = static routes
(vs TGW Connect which uses BGP for dynamic routing)
```

**TGW Connect (GRE + BGP):**
```
For SD-WAN appliances or high-bandwidth dynamic routing:

TGW Connect attachment:
  - Uses GRE tunnel over existing VPC or DX attachment
  - BGP sessions for dynamic route exchange
  - Bandwidth: up to 5 Gbps per Connect peer (vs 1.25 Gbps for VPN)
  - Use: SD-WAN, high-bandwidth 3rd-party NVAs

Architecture:
  On-premises SD-WAN appliance
  → DX (transport layer)
  → TGW Connect attachment
  → BGP session for dynamic prefix exchange
```

---

### Direct Connect Gateway Advanced

**Private VIF + VGW vs Transit VIF + TGW:**
```
Private VIF + VGW:
  Each VPC needs its own Private VIF
  DX Gateway can connect: 1 DX ↔ 10 VGWs (10 VPCs max)
  Scales poorly for many VPCs

Transit VIF + TGW:
  One Transit VIF connects DX → DX Gateway → TGW
  TGW serves thousands of VPCs
  Better: one connection, all VPCs

DX Gateway limits:
  Max VGW associations: 10
  Max TGW associations: 3
  Max prefixes from on-prem: 100 (advertised to AWS)
  Max prefixes from AWS: 200 (advertised to on-prem)
```

**SiteLink:**
```
Enable on-premises locations to communicate via AWS backbone:
  Chicago DC → DX → AWS backbone → DX → NYC DC

Without SiteLink: traffic must go through customer VPC router
With SiteLink: AWS backbone routes between DX connections directly

Use case: low-latency private WAN between global offices using AWS global network
Cost: SiteLink data transfer pricing (extra vs standard DX)

Enable: SiteLink option on DX virtual interface
```

---

### IPv6 Networking

```
Dual-stack VPC (IPv4 + IPv6):
  VPC CIDR: 10.0.0.0/16 (IPv4) + 2600:1f18::/32 (IPv6, AWS-assigned)
  Subnets: /64 IPv6 prefix per subnet
  EC2 instances: dual-stack (both IPv4 + IPv6 addresses)

IPv6-only subnets:
  Lambda (VPC mode) supports IPv6-only
  Fargate tasks support IPv6-only
  Use for: modern workloads, internal-only microservices

Egress-only Internet Gateway (EIGW):
  IPv6 equivalent of NAT Gateway
  Allows: IPv6 instances to initiate outbound internet connections
  Blocks: inbound IPv6 connections from internet
  Free (unlike NAT Gateway which charges per GB)

Direct Connect + IPv6:
  Advertise IPv6 prefixes over BGP (separate BGP session for IPv6 = BGP4+)
  Both Private VIF and Transit VIF support IPv6 prefixes
```

---

### Network Performance

**EFA (Elastic Fabric Adapter):**
```
For HPC workloads requiring ultra-low latency inter-node communication:
  OS-bypass: application communicates directly with network hardware
  MPI (Message Passing Interface) support
  AWS libfabric provider: efa

Use cases:
  HPC (weather modeling, genomics, computational fluid dynamics)
  Distributed ML training (all-reduce operations)
  Real-time financial simulations

Works with: C5n, P4d, Hpc6a, and other HPC instance types
EFA + Placement Group (cluster): maximize network performance between instances
```

**Enhanced Networking (ENA):**
```
Standard for all modern EC2 instances:
  SR-IOV: hardware virtualization bypasses hypervisor for network
  Bandwidth: up to 100 Gbps on supported instances (100Gbps instances: C5n, P4d)
  Lower latency, lower CPU overhead vs legacy networking

ENA vs EFA:
  ENA: Enhanced Networking (standard, automatic on modern instances)
  EFA: OS-bypass networking (additional capability, manual install)
  Both use the same hardware; EFA adds OS-bypass capability
```

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| AWS BGP ASN | 7224 |
| VPN tunnel bandwidth | 1.25 Gbps per tunnel |
| VPN tunnels per connection | 2 |
| TGW ECMP max VPN connections | Limited by TGW bandwidth (50 Gbps per VPC attachment) |
| DX Gateway max VGW associations | 10 |
| DX Gateway max TGW associations | 3 |
| DX prefix limit (on-prem to AWS) | 100 |
| DX prefix limit (AWS to on-prem) | 200 |
| TGW cross-region routing | Static routes only |
| TGW Connect bandwidth per peer | 5 Gbps |
| Maximum resiliency DX | 2 locations × 2 connections = 4 connections |
| SiteLink | Enabled per virtual interface |
| EFA | Manual installation; works with libfabric |

---

## 5. Decision Tree

### BGP Tuning: Active-Active vs Active-Passive
```
Want traffic on BOTH connections simultaneously?
  → Active-Active:
      LOCAL_PREF: equal on both paths
      AS_PATH: equal on both paths
      Result: ECMP, load-balanced

Want one primary, one standby?
  → Active-Passive:
      Outbound (AWS→OnPrem): higher LOCAL_PREF on primary router
      Inbound (OnPrem→AWS): shorter AS_PATH on primary advertisement
      Result: primary always used, secondary automatic failover
```

### TGW Topology Selection
```
Need stateful inspection (NGFW/IDS) for east-west traffic?
└─ TGW with Inspection VPC + Appliance Mode

Need on-prem to connect dynamically (BGP) at high bandwidth?
└─ TGW Connect (GRE tunnel, BGP, 5 Gbps per peer)

Need static site-to-site connectivity?
└─ TGW with Site-to-Site VPN (ECMP if multiple)

Need cross-region connectivity?
└─ TGW cross-region peering (static routes only)
```

### DX Resilience Tier
```
Business criticality?
  Very High (financial, healthcare, government)
  └─ Maximum Resiliency (2 DX locations × 2 connections)
       Optionally + VPN backup for insurance

High (enterprise production)
  └─ High Availability (2 DX same location)
       + VPN backup (different path)

Medium (non-critical production)
  └─ Single DX + VPN backup

Low (development, test)
  └─ VPN only (no DX)
```

---

## 6. Common Patterns

### Pattern 1: Enterprise Active-Passive DX with VPN Backup
```
BGP Configuration:

On-prem router (Primary DX):
  to AWS: advertise 192.168.0.0/16 via Primary DX
  from AWS: set LOCAL_PREF 200 on received routes

On-prem router (Secondary DX):
  to AWS: advertise 192.168.0.0/16 with 3× AS_PATH prepend
  from AWS: set LOCAL_PREF 100 on received routes

On-prem router (VPN backup):
  to AWS: advertise 192.168.0.0/16 with 5× AS_PATH prepend
  from AWS: set LOCAL_PREF 50 on received routes

Result:
  Normal: Primary DX carries all traffic (both directions)
  Primary DX fails: Secondary DX activates automatically
  Both DX fail: VPN backup activates (failover in seconds via BGP)
  VPN failback: when DX restores, BGP reconverges to prefer DX
```

### Pattern 2: 10 Gbps Aggregated VPN
```
4 VPN connections to same TGW:
  VPN-1: 2 tunnels (1.25 Gbps each) = 2.5 Gbps
  VPN-2: 2 tunnels (1.25 Gbps each) = 2.5 Gbps
  VPN-3: 2 tunnels (1.25 Gbps each) = 2.5 Gbps
  VPN-4: 2 tunnels (1.25 Gbps each) = 2.5 Gbps
  Total: 10 Gbps aggregate with ECMP

BGP on each tunnel: identical LOCAL_PREF + identical AS_PATH
TGW: ECMP enabled

ECMP load balancing:
  Per-flow (5-tuple: src/dst IP/port + protocol)
  Each flow goes to one tunnel, different flows to different tunnels
  Not per-packet (avoids out-of-order delivery)
```

### Pattern 3: Global Network with SiteLink
```
Company offices: New York, London, Singapore
Each has DX to AWS

Without SiteLink:
  NY ↔ London: traffic NY → DX → AWS VPC → DX → London
  Requires: VPC router, complex routing, higher latency

With SiteLink:
  NY ↔ London: traffic NY → DX → AWS backbone → DX → London
  AWS global backbone: low-latency private network
  No intermediate VPC needed

SiteLink enables: private WAN built on AWS infrastructure
Cost: SiteLink data processing fee per GB
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **TGW Appliance Mode required for NGFW** | Without Appliance Mode, stateful firewalls see only one direction of a TCP flow → drop connections. Enable on the Inspection VPC's TGW attachment. |
| **DX SiteLink costs extra** | SiteLink data transfer is priced separately from regular DX data transfer. Significant cost for high-volume inter-site traffic. |
| **ECMP requires identical BGP attributes** | If one tunnel advertises LOCAL_PREF 200 and another advertises 100, BGP picks the single best path — no ECMP. All tunnels must be identical for load balancing. |
| **TGW cross-region peering uses static routes only** | Unlike VPC peering or TGW within a region, TGW cross-region peering does NOT support BGP. You must manually create and maintain route table entries. |
| **VGW does not support ECMP** | Only Transit Gateway supports ECMP for VPN connections. If you're using VGW (old Virtual Private Gateway), you get active/passive tunnels only. |
| **DX is not encrypted by default** | Direct Connect provides private connectivity but NOT encryption. Use MACsec (Layer 2) or IPSec VPN over DX for encryption. |
| **BGP communities only honored on DX VIFs** | AWS BGP community values (7224:7100 etc.) only work on Direct Connect virtual interfaces, not on VPN connections. |
| **DX bandwidth is committed but not guaranteed in all scenarios** | DX bandwidth is the port speed, not per-VIF. Multiple VIFs on one port share the port bandwidth. |
| **EFA requires agent installation** | EFA capability requires the EFA installer to be run inside the AMI/instance. It's not automatic even when EFA-capable instance type and ENI are used. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Configure ECMP VPN connections to a Transit Gateway and understand routing tables.

### Step 1: Create a Transit Gateway
```
VPC Console → Transit Gateways → Create transit gateway
  Name: lab-tgw
  ASN: 64512 (private ASN)
  ECMP: Enable equal-cost multipath routing = Yes  ← Important
  Default route table: Yes
  Auto-accept shared attachments: No

Wait: ~3 minutes for TGW to become Available
```

### Step 2: Create a Test VPC and Attach to TGW
```
Create VPC: 10.0.0.0/16
  - Create subnet: 10.0.1.0/24 (us-east-1a)
  - Create TGW attachment:
      VPC: your VPC
      TGW: lab-tgw
      Subnets: your subnet
```

### Step 3: Create Two Site-to-Site VPN Connections
```
VPC Console → Site-to-Site VPN → Create VPN connection

VPN-1:
  Name: lab-vpn-1
  Target gateway type: Transit gateway → lab-tgw
  Customer gateway: Create new
    IP address: 1.2.3.4 (fake, for lab purposes)
    BGP ASN: 65001
  Routing: Dynamic (BGP)
  Tunnel options: leave defaults

VPN-2: (same customer gateway, second VPN connection)
  Name: lab-vpn-2
  Target: lab-tgw
  Customer gateway: same as above
  BGP ASN: 65001
  Routing: Dynamic (BGP)

Download configuration for each VPN (select your router vendor)
```

### Step 4: Examine TGW Route Tables
```
VPC Console → Transit Gateway Route Tables
  → Default route table → Routes tab

  Should see:
  Destination     Target              State
  10.0.0.0/16    VPC attachment      Active (propagated from VPC)

  Propagation tab: VPC attachment is propagating its prefix

After VPN BGP establishes (would need real on-prem router):
  Dynamic routes from on-prem would appear here automatically
```

### Step 5: Understand BGP Route Differences
```
# Read the downloaded VPN config files
# Note: each VPN has 2 tunnels with different:
#   - Outside IP addresses (AWS side)
#   - Inside IP addresses (BGP peer IPs)
#   - Pre-shared keys

# For ECMP to work:
#   All tunnels must advertise identical prefixes via BGP
#   All tunnels must have identical LOCAL_PREF
#   AWS side: ECMP enabled on TGW

# Verify TGW ECMP setting:
aws ec2 describe-transit-gateways \
  --query 'TransitGateways[*].[TransitGatewayId,Options.VpnEcmpSupport]'
# Should show: "enable"
```

### Step 6: Review BGP Community Documentation
```
# Review the AWS Direct Connect BGP community documentation
# Key communities to know for SAP-C02:
#   7224:7100 - local region only
#   7224:7200 - all regions, same continent
#   7224:7300 - all regions globally

# These are applied on your on-premises router when advertising prefixes to DX
# Cannot test without actual DX connection
```

### Step 7: Clean Up
```
Delete VPN connections (each ~$0.05/hr — delete immediately after lab)
Delete TGW attachments
Delete TGW
Delete VPC
```

---

## Summary Reference Card

```
BGP ATTRIBUTES:
  LOCAL_PREF: set on-prem for AWS→OnPrem preference (higher = preferred)
  AS_PATH prepend: make path look longer → less preferred for inbound
  MED: suggest entry point to adjacent AS (lower = preferred)
  BGP Communities (DX): 7224:7100 local / 7224:7200 continent / 7224:7300 global

DX REDUNDANCY:
  No redundancy:     single circuit (avoid)
  HA:                2 DX same location (device failure protection)
  Max Resiliency:    2 DX 2 locations (location failure protection)
  Ultra:             4 DX 2 locations
  DX + VPN backup:   DX primary (LOCAL_PREF high) + VPN standby (LOCAL_PREF low)

ECMP WITH TGW:
  TGW only (not VGW)
  Identical BGP attributes across all tunnels
  VPN: 1.25 Gbps/tunnel × 8 tunnels = 10 Gbps aggregate

TGW TOPOLOGIES:
  Inspection VPC: Appliance Mode REQUIRED for stateful NGFW
  Cross-region peering: static routes ONLY (no BGP)
  TGW Connect: GRE tunnel + BGP, 5 Gbps per peer, for SD-WAN

DX GATEWAY:
  Transit VIF → TGW = scalable (not Private VIF + VGW per VPC)
  SiteLink: on-prem ↔ on-prem via AWS backbone (extra cost)
  Max VGW: 10, Max TGW: 3, Max prefixes from on-prem: 100
```
