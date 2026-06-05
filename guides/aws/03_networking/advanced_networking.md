# Advanced Networking — SAP-C02 Level Deep Dive

---

## 1. The Problem

The core networking services (VPC, VPN, Direct Connect, ALB/NLB) get you to production. But at enterprise scale and for the SAP-C02 exam, you need to understand the layers beneath: how BGP actually controls which path traffic takes, how to build architectures that handle partial failures gracefully, how to share network infrastructure across accounts without re-architecting, and how to get line-rate network performance for HPC workloads.

This file covers:
- **BGP** as a control plane for hybrid routing decisions
- **ECMP** for VPN bandwidth aggregation
- **PrivateLink deep-dive** for service exposure patterns
- **VPC sharing via RAM** for multi-account architectures
- **IPv6** architecture patterns
- **Enhanced Networking and EFA** for performance-critical workloads
- **Global Accelerator** vs CloudFront vs Route53

---

## 2. BGP Fundamentals for AWS

### 2.1 What BGP Does in AWS

**Border Gateway Protocol (BGP)** is the routing protocol used by:
- Site-to-Site VPN connections (when dynamic routing is selected)
- Direct Connect (mandatory — all DX connections use BGP)
- Transit Gateway routing

BGP exchanges **reachability information** (which CIDRs exist where) between **Autonomous Systems (AS)**. In AWS:
- AWS VPC network has AS numbers in the range `7224` (AWS private ASN)
- Your on-premises network uses your ASN (public or private, typically `65000` range)
- BGP peers exchange routes, and the **BGP attributes** control path preference

### 2.2 BGP Terminology

| Term | Definition | AWS Context |
|---|---|---|
| **ASN (Autonomous System Number)** | Unique identifier for a network | Your on-prem network has one; AWS has one |
| **BGP Peer** | Router that exchanges routes via BGP | Your CGW ↔ AWS VGW/TGW |
| **NLRI (Network Layer Reachability Information)** | The prefixes being advertised | Your on-prem CIDRs, VPC CIDRs |
| **BGP Attributes** | Properties of a route that influence path selection | LOCAL_PREF, AS_PATH, MED |
| **eBGP** | BGP between different AS numbers | On-prem ↔ AWS |
| **iBGP** | BGP within same AS | Rarely relevant in AWS context |
| **Route Reflector** | Distributes iBGP routes | AWS DX Gateway uses this internally |

### 2.3 BGP Path Selection Algorithm

When multiple paths exist to the same destination, BGP selects the best path using this ordered list:

```
1. WEIGHT (Cisco-specific, local to router, highest preferred)
2. LOCAL_PREF (highest preferred; used within same AS for outbound preference)
3. Locally originated routes preferred over learned routes
4. AS_PATH length (shortest preferred; used for inbound preference)
5. ORIGIN type (IGP < EGP < Incomplete)
6. MED (lowest preferred; advisory metric for inbound preference)
7. eBGP path preferred over iBGP
8. Lowest IGP metric to BGP next hop
9. Oldest route
10. Lowest Router ID
```

### 2.4 Controlling Outbound Routing (VPC → On-Prem)

**Outbound** = traffic leaving AWS toward on-premises.
Controlled by **LOCAL_PREF** attribute (set on AWS VGW/TGW for the routes learned from your on-prem).

```
Scenario: DX primary, VPN backup

DX Connection BGP Configuration:
  → On-prem advertises 10.100.0.0/16 over DX with no special attributes
  → AWS receives it; internally sets LOCAL_PREF=200 (via VGW BGP settings)

VPN Connection BGP Configuration:
  → On-prem advertises 10.100.0.0/16 over VPN
  → AWS receives it; internally sets LOCAL_PREF=100

AWS routing: LOCAL_PREF 200 > 100 → DX is preferred for outbound traffic.
When DX fails → only VPN route remains → traffic fails over automatically.
```

**How to set LOCAL_PREF in AWS:**
When creating/editing a VPN or DX connection, you can configure the BGP configuration. For DX, LOCAL_PREF is set via the routing policy in the VIF configuration. For VPN, it can be set in TGW route tables using BGP communities.

### 2.5 Controlling Inbound Routing (On-Prem → VPC)

**Inbound** = traffic from on-premises entering AWS.
AWS cannot directly set LOCAL_PREF on the on-premises side. Instead, influence inbound routing with:

**AS_PATH Prepending:**
Make one path look longer (less preferred) by prepending your own AS number multiple times:

```
DX Advertisement: 10.0.0.0/16 with AS_PATH: 65000        (length 1, preferred)
VPN Advertisement: 10.0.0.0/16 with AS_PATH: 65000 65000 65000  (length 3, less preferred)

On-prem routing: shorter AS_PATH → prefers DX for inbound traffic to AWS
```

**MED (Multi-Exit Discriminator):**
Lower MED is preferred. Set on routes advertised to AWS:
```
DX: advertise 10.0.0.0/16 with MED=100    (lower = preferred)
VPN: advertise 10.0.0.0/16 with MED=200   (higher = less preferred)
```

MED is only compared between routes from the **same AS** and is not always honored by AWS. AS_PATH prepending is more reliable.

### 2.6 BGP Communities on Direct Connect

AWS provides **BGP community tags** to control how routes advertised via DX are propagated within AWS's network. Used on **Public VIF** and **Transit VIF** (NOT Private VIF).

#### Inbound Communities (you apply to your routes, AWS applies propagation rules):

| Community Tag | Scope |
|---|---|
| `7224:7100` | Propagate to local AWS region only |
| `7224:7200` | Propagate to all DX-connected regions |
| `7224:7300` | Propagate globally (all AWS regions) |

**Use case:** If your on-prem network is in the US, advertise routes with `7224:7100` so only US-East-1 (nearest region) uses your DX for internet-bound traffic. Without this, all AWS regions might try to route internet traffic via your DX, consuming bandwidth.

#### No Export Community:

| Community Tag | Meaning |
|---|---|
| `7224:9100` | AWS does not advertise this route to other AWS regions |
| `7224:9200` | AWS does not advertise to other AWS accounts |
| `7224:9300` | Combined: no cross-region, no cross-account |

```
Example BGP Advertisement (Public VIF):
  Prefix: 203.0.113.0/24
  Communities: 7224:7100, 7224:9300

  → Only the local DX region will learn this route
  → Other AWS accounts will not see this route advertised
```

**Critical gotcha:** BGP communities on DX only work for **Public VIF** and **Transit VIF**. They do NOT apply to Private VIF routes.

---

## 3. ECMP for VPN Bandwidth Aggregation

### 3.1 The Problem

A single Site-to-Site VPN tunnel supports up to **1.25 Gbps**. For workloads requiring more bandwidth over VPN (migration, large data flows, backup), a single tunnel is insufficient.

### 3.2 ECMP with Transit Gateway

Transit Gateway supports **Equal-Cost Multi-Path (ECMP)** routing across multiple VPN connections. All connections must advertise the **same CIDR with equal AS_PATH length**.

```
Architecture:
On-prem router
├── VPN Connection 1 → TGW (tunnel A + tunnel B)
├── VPN Connection 2 → TGW (tunnel C + tunnel D)
├── VPN Connection 3 → TGW (tunnel E + tunnel F)
└── VPN Connection 4 → TGW (tunnel G + tunnel H)

Each connection = up to 1.25 Gbps
4 connections with ECMP = up to ~5 Gbps aggregate

BGP Config (on-prem router):
  All 4 connections advertise: 10.100.0.0/16 with AS_PATH: 65000
  (same AS_PATH length = equal cost = ECMP)
```

### 3.3 ECMP Configuration Requirements

1. **TGW must be the VPN termination point** — ECMP does NOT work with VGW
2. **BGP required** (not static routing)
3. **Equal AS_PATH length** across all connections
4. Same prefix advertised from all connections
5. **Enable ECMP** on TGW VPN attachment (enabled by default with BGP)

### 3.4 ECMP with DX LAG

Direct Connect **Link Aggregation Groups (LAG)** also use ECMP across physical connections:
- Up to 4 connections at the same DX location
- All same speed (all 1G, all 10G, or all 100G)
- Aggregate bandwidth = N × single link speed
- Single logical interface for routing

---

## 4. PrivateLink Deep Dive

### 4.1 Architecture Components

```
Producer Account (Service Owner):
  EC2/ECS/Lambda → NLB (required) → PrivateLink Service Endpoint
                                          ↑ AWS creates this
                                     (VPC Endpoint Service)

Consumer Account 1:
  VPC → Interface VPC Endpoint → PrivateLink → NLB → Service

Consumer Account 2:
  VPC → Interface VPC Endpoint → PrivateLink → NLB → Service
```

### 4.2 Producer Setup

1. Deploy service behind a **Network Load Balancer** (ALB is NOT supported as producer)
2. Create a **VPC Endpoint Service** from the NLB:
   - Can configure acceptance required (manually approve each consumer) or auto-accept
   - Gets a service name: `com.amazonaws.vpce.<region>.<vpce-svc-id>`
   - Can share across AWS accounts or entire organizations

```bash
# CLI: Create endpoint service
aws ec2 create-vpc-endpoint-service-configuration \
  --network-load-balancer-arns arn:aws:elasticloadbalancing:... \
  --acceptance-required   # manual approval of consumers
```

### 4.3 Consumer Setup

1. Create an **Interface VPC Endpoint** in the consumer's VPC:
   - Specify the service name from the producer
   - Select subnets (one per AZ for HA)
   - Creates ENIs in each selected subnet
2. Enable **Private DNS** (if supported by service): overrides the service's public DNS to resolve to the private ENI IPs

```bash
# CLI: Create interface endpoint
aws ec2 create-vpc-endpoint \
  --vpc-id vpc-xxx \
  --service-name com.amazonaws.vpce.us-east-1.vpce-svc-xxx \
  --vpc-endpoint-type Interface \
  --subnet-ids subnet-a subnet-b \
  --security-group-ids sg-xxx \
  --private-dns-enabled
```

### 4.4 DNS Resolution for PrivateLink

When private DNS is enabled:
```
Consumer EC2 query: api.service.com
↓
Route53 Resolver in Consumer VPC
↓
Resolves to private IP of Interface Endpoint ENI (e.g., 10.0.5.23)
↓
Traffic goes to ENI → PrivateLink → Producer NLB → Service
```

When private DNS is NOT enabled (or for custom domains):
```
Consumer must use: vpce-xxx.vpce.amazonaws.com (endpoint-specific DNS)
Or: configure a Route53 Private Hosted Zone with CNAME/Alias record
```

### 4.5 PrivateLink Key Properties

| Property | Detail |
|---|---|
| **No VPC peering required** | Producer and consumer VPCs can have overlapping CIDRs |
| **Directional** | Consumer can reach producer; producer cannot initiate to consumer |
| **Cross-account** | Works natively; producer shares service name with consumer |
| **Cross-region** | Supported (consumer endpoint in one region, producer in another — via inter-region PrivateLink) |
| **NLB required** | ALB cannot be the producer NLB |
| **Security** | Consumer endpoint has a Security Group; producer SG must allow from NLB |
| **Scale** | One NLB serves unlimited consumers |

### 4.6 PrivateLink vs VPC Peering

| | VPC Peering | PrivateLink |
|---|---|---|
| **Overlapping CIDRs** | Not allowed | Allowed |
| **Bidirectional access** | Yes | No (one direction) |
| **Scale (consumers)** | Limited (125 peerings/VPC) | Unlimited |
| **DNS** | Consumer uses producer's IP directly | Private DNS resolves to ENI |
| **Cost** | Free (just data transfer) | Hourly + per-GB charges |
| **On-prem access** | Not via peering | Not directly (needs on-prem DNS) |
| **Best for** | Full VPC-to-VPC connectivity | Exposing a specific service |

---

## 5. VPC Sharing via AWS RAM

### 5.1 The Problem

Traditional multi-account architecture requires a separate VPC per account. This leads to:
- VPC peering sprawl
- Duplicated NAT Gateways across accounts
- Fragmented IP address space
- Complex DNS and routing configurations

### 5.2 VPC Sharing Architecture

**VPC Owner account** creates the VPC and shares **subnets** (not the entire VPC) with **Participant accounts** using AWS Resource Access Manager (RAM).

```
Network Account (VPC Owner):
  VPC: 10.0.0.0/16
  ├── Subnet: 10.0.0.0/24 (shared with App Account A)
  ├── Subnet: 10.0.1.0/24 (shared with App Account B)
  ├── Subnet: 10.0.10.0/24 (shared with Database Account)
  ├── NAT Gateway (shared infrastructure)
  └── TGW attachment (shared connectivity)

App Account A:
  Launch EC2, RDS, Lambda IN 10.0.0.0/24
  (subnet is owned by Network Account, but resources are in App Account A)

App Account B:
  Launch EC2, ECS IN 10.0.1.0/24
```

### 5.3 Key Properties of VPC Sharing

| Property | Detail |
|---|---|
| **What's shared** | Subnets (not the entire VPC, not route tables, not SGs) |
| **Who manages the VPC** | VPC owner (Network Account) manages IGW, NAT GW, route tables |
| **Who manages resources** | Participant accounts manage their own EC2, RDS, etc. |
| **Security Groups** | **Per-account** — App Account A's SGs are not visible to App Account B |
| **Routing** | Owner's route tables apply; participants cannot modify them |
| **Cost savings** | Share NAT Gateways, TGW attachments across accounts |
| **RAM requirement** | AWS Organizations must be enabled; RAM must be enabled for org sharing |

### 5.4 Security Group Cross-Account Limitation

This is the key gotcha: **Security Groups cannot be referenced across participant accounts**.

```
App Account A SG: sg-a-prod (allows TCP 443 from 0.0.0.0/0)
App Account B SG: sg-b-prod

App Account A EC2 CANNOT add a rule: "allow from sg-b-prod"
  ← This only works between accounts that have VPC peering with cross-account SG referencing

Solution options:
1. Use IP CIDR ranges instead of SG references
2. Use VPC peering (if you still need cross-account SG refs)
3. Use separate security groups per account with CIDR-based rules
```

### 5.5 Sharing Within AWS Organizations

```bash
# Share subnet with specific OU or entire org
aws ram create-resource-share \
  --name shared-subnet-share \
  --resource-arns arn:aws:ec2:us-east-1:NETWORK-ACCOUNT-ID:subnet/subnet-xxx \
  --principals arn:aws:organizations::ROOT-ACCOUNT:ou/o-xxx/ou-xxx
```

Participants receive a RAM invitation and must accept before using the subnet.

---

## 6. IPv6 Architecture

### 6.1 IPv6 in AWS

| Component | IPv6 Support |
|---|---|
| VPC | Optional, /56 CIDR from Amazon's pool |
| Subnet | /64 CIDR per subnet |
| EC2 | Gets a global unicast IPv6 address (public by default) |
| IGW | Handles IPv6 outbound and inbound |
| EIGW (Egress-Only IGW) | IPv6 outbound only (no inbound) |
| ALB | Supports IPv6 (dualstack) |
| NLB | Supports IPv6 (dualstack) |
| NAT Gateway | NOT supported for IPv6 (use EIGW instead) |
| Security Groups | Can specify IPv6 CIDRs |
| NACLs | Can specify IPv6 CIDRs |
| Route Tables | Separate entries for IPv6 routes |

### 6.2 IPv6 is Always Public

AWS IPv6 addresses are **global unicast addresses** — there are no private IPv6 ranges in AWS. Every IPv6-enabled EC2 instance gets a publicly routable address. Privacy control comes from:
- **Route tables**: don't add `::/0 → IGW` for private subnets
- **EIGW**: allows outbound-only IPv6 (like NAT Gateway for IPv4)
- **Security Groups/NACLs**: block unwanted inbound IPv6

### 6.3 Dual-Stack Architecture

```
VPC: 10.0.0.0/16 (IPv4) + 2600:1f18:xxx::/56 (IPv6)

Public Subnet:
  Route Table:
    10.0.0.0/16 → local
    0.0.0.0/0   → igw-xxx       ← IPv4 internet
    ::/0        → igw-xxx       ← IPv6 internet (inbound + outbound)
  EC2: 10.0.0.5 (private IPv4) + 54.1.2.3 (public IPv4) + 2600:1f18:xxx::1 (IPv6)

Private Subnet:
  Route Table:
    10.0.0.0/16 → local
    0.0.0.0/0   → nat-gw-xxx    ← IPv4 outbound
    ::/0        → eigw-xxx      ← IPv6 outbound ONLY (no inbound)
  EC2: 10.0.10.5 (private IPv4) + 2600:1f18:xxx::2 (IPv6, publicly routable but no inbound route)
```

### 6.4 IPv6-Only Subnets

New feature: subnets that are **IPv6 only** (no IPv4 addresses). Used for:
- Cost savings (no IPv4 EIP needed)
- Future-proofing
- Workloads that only need IPv6

EC2 instances in IPv6-only subnets get DNS names that resolve to IPv6 addresses. They cannot communicate with IPv4-only resources directly (need NAT64 or dual-stack intermediary).

### 6.5 ALB/NLB Dual-Stack

```
ALB in dualstack mode:
  IPv4 DNS: myalb-xxx.us-east-1.elb.amazonaws.com → 54.x.x.x
  IPv6 DNS: myalb-xxx.us-east-1.elb.amazonaws.com → 2600:1f18:xxx::1

Clients can connect via either protocol.
ALB → targets always via IPv4 (regardless of client protocol).
NLB dualstack: can forward IPv6 to IPv6-enabled targets.
```

---

## 7. Network Performance

### 7.1 Enhanced Networking (ENA)

Standard EC2 networking uses software-based virtualization, which adds CPU overhead and latency. Enhanced Networking uses **single-root I/O virtualization (SR-IOV)** to provide near-hardware-level performance.

| Feature | Standard Networking | Enhanced Networking (ENA) |
|---|---|---|
| **Technology** | Software virtualization | SR-IOV |
| **Max bandwidth** | ~1 Gbps | Up to 100 Gbps |
| **Latency** | Higher | Lower (hardware-level) |
| **PPS (packets/sec)** | Lower | Millions PPS |
| **CPU overhead** | Higher | Lower |
| **Cost** | Included | Included (no extra charge) |

**ENA (Elastic Network Adapter):**
- Default for all current-gen instance types (C5, M5, R5, P3, etc.)
- Up to **100 Gbps** on supported instances (C5n, P3dn, etc.)
- Enabled automatically on supported AMIs

**ENA Express:**
- Feature available on supported instance types
- Reduces tail latency for latency-sensitive workloads
- Uses SRD (Scalable Reliable Datagram) protocol under the hood
- Enable at the ENI level

### 7.2 Elastic Fabric Adapter (EFA)

EFA goes further than ENA by providing **OS-bypass capabilities** for inter-node communication in HPC and ML clusters.

| Feature | ENA | EFA |
|---|---|---|
| **Protocol** | Standard TCP/IP | OS-bypass (bypasses kernel network stack) |
| **Use case** | General networking | HPC, MPI, ML distributed training |
| **Supported OS** | Linux and Windows | **Linux only** |
| **MPI support** | No | Yes (libfabric/OpenMPI) |
| **Latency** | Low | Ultra-low (microseconds) |
| **Available on** | All current-gen | Select instance types (hpc6a, p4d, c5n, etc.) |

**EFA is ENA + OS bypass:**
- Still appears as an ENI in the VPC
- Regular TCP/IP traffic still works via ENA path
- MPI/NCCL traffic uses EFA OS-bypass path
- Must be in the same VPC; placement group recommended for lowest latency

```bash
# Launch EFA-enabled instance
aws ec2 run-instances \
  --instance-type c5n.18xlarge \
  --network-interfaces '[{"DeviceIndex":0,"InterfaceType":"efa","SubnetId":"subnet-xxx","Groups":["sg-xxx"]}]'
```

### 7.3 Placement Groups

| Type | Network Characteristic | Use Case |
|---|---|---|
| **Cluster** | Lowest latency, highest PPS — all instances in same rack/AZ | HPC, ML training, low-latency apps |
| **Spread** | Different hardware, different AZs | HA critical instances |
| **Partition** | Groups of instances on separate racks | Hadoop, Cassandra, Kafka |

For EFA/HPC workloads: always use **Cluster placement group** to minimize inter-node latency.

---

## 8. AWS Global Accelerator

### 8.1 What It Does

Global Accelerator gives you **static anycast IP addresses** that route to the nearest AWS edge location, then forward traffic over the AWS global backbone to your application in the target region.

```
User (Tokyo)
     ↓ connects to anycast IP (always same 2 IPs)
AWS Edge Location (Tokyo)
     ↓ AWS global backbone (private, optimized)
Application Load Balancer (us-east-1)
     ↓
EC2 instances
```

### 8.2 How Anycast Works

Two static IPs are provided (from the `anycast.accelerator.amazonaws.com` DNS). These IPs are advertised from every AWS edge location globally. Users connect to the **nearest edge** automatically (BGP anycast routing).

Once traffic enters the AWS edge:
- Travels over AWS's private backbone (not the internet)
- Provides consistent, low-latency path to the origin

### 8.3 Key Features

| Feature | Detail |
|---|---|
| **Static IPs** | 2 anycast IPs (fixed; can be whitelisted by partners/clients) |
| **Protocol support** | TCP and UDP (not just HTTP like CloudFront) |
| **Health checks** | Built-in; automatically routes around unhealthy endpoints |
| **Traffic dials** | Control percentage of traffic per endpoint group (like weighted) |
| **Endpoint groups** | One per AWS region; can have multiple ALBs/NLBs/EIPs per group |
| **Failover** | Near-instant (<30 seconds) vs DNS-based failover (TTL-dependent) |
| **DDoS protection** | AWS Shield Standard included |
| **Cost** | ~$0.025/hr per accelerator + $0.01/GB processed |

### 8.4 Global Accelerator vs CloudFront vs Route53 Latency

| Feature | Global Accelerator | CloudFront | Route53 Latency |
|---|---|---|---|
| **Protocol** | TCP, UDP (any) | HTTP/HTTPS | DNS-based (any) |
| **Caching** | No | Yes (edge caching) | No |
| **Static IP** | Yes (2 anycast IPs) | No (changes) | No |
| **Routing logic** | Anycast to nearest edge → backbone | Nearest edge serves cached content | DNS directs to best region |
| **Failover speed** | Seconds (non-DNS) | Minutes (DNS TTL) | Minutes (DNS TTL) |
| **Non-HTTP support** | Yes (gaming, IoT, streaming) | No | Yes (but DNS-only) |
| **Origin regions** | 1 or more AWS regions | 1 or more origins | Multiple records |
| **Primary use** | Improve performance for TCP/UDP, static IPs | Accelerate HTTP content, caching | Multi-region HTTP failover/routing |
| **Works with** | ALB, NLB, EC2, EIP | S3, ALB, custom origin | Any DNS-resolvable endpoint |

---

## 5. Decision Tree

### Global Accelerator vs CloudFront vs Route53 Latency?

```
Need caching at edge?
└── YES → CloudFront (HTTP/HTTPS only, edge caching reduces origin load)

Need static IPs for IP whitelisting?
└── YES → Global Accelerator (2 fixed anycast IPs)

Need non-HTTP protocol (UDP, TCP gaming, MQTT, SMTP)?
└── YES → Global Accelerator

Need DNS-level routing with geo/latency/failover logic?
└── YES, and HTTP is fine → Route53 latency policy → ALB per region
└── YES, and need fast failover → Global Accelerator (no DNS TTL dependence)

Optimizing HTTP/HTTPS web application globally with caching?
└── CloudFront with Route53 Alias

Improving latency for a TCP-based API globally?
└── Global Accelerator → NLB per region
```

### BGP: How to Prefer DX over VPN for outbound traffic (VPC → on-prem)?

```
Set LOCAL_PREF on the DX BGP session higher than VPN:
  DX: LOCAL_PREF=200  (higher = preferred for outbound)
  VPN: LOCAL_PREF=100

Or: ensure DX advertises more-specific routes (longer prefix = higher priority)
```

### BGP: How to make on-prem prefer DX for inbound traffic (on-prem → AWS)?

```
Advertise VPC CIDRs with shorter AS_PATH over DX:
  DX: 10.0.0.0/16 with AS_PATH: 65000           (length 1, preferred)
  VPN: 10.0.0.0/16 with AS_PATH: 65000 65000 65000  (length 3, deprioritized)
```

### Which PrivateLink or VPC Connectivity Model?

```
Two VPCs, full bidirectional connectivity, no CIDR overlap?
└── VPC Peering (simple, cost-effective)

One service exposed to many consumers (possibly with CIDR overlap)?
└── PrivateLink (NLB producer, interface endpoint consumers)

Many VPCs, transitive routing, shared on-prem connectivity?
└── Transit Gateway

Subnets shared across accounts (same VPC)?
└── VPC Sharing via RAM
```

### ENA vs EFA?

```
Standard application networking?
└── ENA (always-on for current gen instances)

High Performance Computing (MPI workloads)?
└── EFA (+ cluster placement group + Linux only)

Distributed ML training (NCCL)?
└── EFA (p3dn.24xlarge, p4d.24xlarge, hpc6a.48xlarge)
```

---

## 6. Common Patterns

### Pattern 1: Full Hybrid with BGP Failover

```
On-Prem Data Center
├── Router (ASN 65000)
│   ├── DX Private VIF → DXGW → TGW (BGP, LOCAL_PREF=200 via AS_PATH short)
│   └── Site-to-Site VPN → TGW (BGP, LOCAL_PREF=100 via AS_PATH long)
│
└── TGW (us-east-1)
    ├── VPC-A attachment (10.0.0.0/16)
    ├── VPC-B attachment (10.1.0.0/16)
    └── VPC-C attachment (10.2.0.0/16)

Normal state: All traffic on DX
DX failure: Automatic BGP failover to VPN in <60 seconds
DX recovery: BGP reconverges, traffic returns to DX
```

### Pattern 2: SaaS Service via PrivateLink

```
SaaS Provider Account (us-east-1):
  EC2/ECS app → NLB (multi-AZ) → VPC Endpoint Service
  Service name: com.amazonaws.vpce.us-east-1.vpce-svc-abc123
  Acceptance required: YES (manual approval)

Customer Account 1 (us-east-1):
  Interface Endpoint → SaaS service (approved)
  Private DNS: saas-api.example.com → 10.0.5.23 (endpoint ENI)
  EC2 → curl https://saas-api.example.com  ← goes via PrivateLink

Customer Account 2 (different VPC, overlapping CIDRs 10.0.0.0/16):
  Same setup works — CIDRs don't matter with PrivateLink
  Interface Endpoint in their 10.0.0.0/24 subnet
```

### Pattern 3: HPC Cluster with EFA

```
Cluster Placement Group (us-east-1a):
  p4d.24xlarge ─┐
  p4d.24xlarge ─┤── EFA (OS-bypass, NCCL)
  p4d.24xlarge ─┤
  p4d.24xlarge ─┘

Storage: Amazon FSx for Lustre (parallel file system, in same AZ)
Networking: 400 Gbps aggregate EFA bandwidth per instance
Routing: All within same AZ, same placement group

Training job:
  NCCL uses EFA OS-bypass → GPU-to-GPU communication without kernel
  Effectively reaches 400 Gbps NIC speed without CPU involvement
```

### Pattern 4: Multi-Region Application with Global Accelerator

```
Global Accelerator
├── Static IP 1: 1.2.3.4  (anycast from all AWS edges)
├── Static IP 2: 5.6.7.8
│
├── Endpoint Group us-east-1 (traffic dial: 50%)
│   └── ALB → EC2 fleet
│
└── Endpoint Group eu-west-1 (traffic dial: 50%)
    └── ALB → EC2 fleet

User in Tokyo → connects to Tokyo edge → AWS backbone → eu-west-1 ALB
  (if us-east-1 is closer in latency, routed there instead)

If us-east-1 health check fails:
  Traffic dial: us-east-1=0%, eu-west-1=100%
  (or use automatic failover — Global Accelerator detects failure)
```

### Pattern 5: VPC Sharing for Shared Infrastructure

```
Network Account (VPC owner):
  VPC 10.0.0.0/16:
    Subnet-A 10.0.0.0/24  → shared with BU1 Account via RAM
    Subnet-B 10.0.1.0/24  → shared with BU2 Account via RAM
    NAT Gateway (one per AZ) ← BU1 and BU2 share this
    TGW attachment ← single attachment for all VPCs

BU1 Account:
  Deploys EC2 instances in Subnet-A (10.0.0.0/24)
  Creates its own Security Groups (BU1 SGs not visible to BU2)
  Bills EC2 to BU1; NAT Gateway data charged to Network Account

BU2 Account:
  Deploys ECS in Subnet-B (10.0.1.0/24)
  Creates its own SGs

Cost savings:
  1 NAT Gateway vs 2 (one per account) = 50% NAT savings
  1 TGW attachment vs 2 = 50% attachment savings
```

---

## 7. Gotchas

### Global Accelerator Charges Per Accelerator
Even with zero traffic, you pay ~$0.025/hr per accelerator (~$18/month). Don't create accelerators for dev/test environments. Also charges $0.01/GB for data transfer. For low-traffic applications, Route53 latency routing may be cheaper.

### PrivateLink Requires NLB — ALB Cannot Be Producer
This trips up many architects. You CANNOT expose a service behind an ALB via PrivateLink. The producer must use an **NLB**. If your service is already behind an ALB, you need to put an NLB in front of the ALB (which is an anti-pattern performance-wise), or re-architect to use NLB directly. There is also an option to use an NLB with IP targets pointing to ALB IPs, but this is fragile as ALB IPs change.

### VPC Sharing Security Groups Are Per-Account
When accounts share a VPC via RAM, each account can only reference its own SGs. Account A cannot use Account B's SG as a rule source/destination. This limits the "allow from my app server SG" pattern commonly used in single-account architectures. Use CIDR-based rules or network ACLs as a workaround.

### EFA is Linux-Only
EFA for OS-bypass HPC/ML workloads only works on Linux. Windows instances can use ENA (standard enhanced networking) but cannot use EFA's OS-bypass mode. If you're running Windows-based HPC (rare but exists), you're limited to standard ENA performance.

### BGP Communities Only Work on DX Public/Transit VIFs
BGP communities for controlling route propagation scope (`7224:7100`, `7224:7200`, `7224:7300`) only apply to **Public VIF** and **Transit VIF**. They are NOT supported on **Private VIF**. For Private VIF routing control, you use LOCAL_PREF and AS_PATH prepending.

### Global Accelerator Does Not Cache
Unlike CloudFront, Global Accelerator has no caching. It's purely a routing optimization that uses the AWS backbone to reduce latency. If someone uses it expecting cache hits (like CloudFront), they'll be surprised that every request hits the origin.

### ECMP Requires BGP (No Static Routing)
ECMP across multiple VPN connections to TGW only works with **BGP routing**. Static VPN routes do not support ECMP. If you configured Site-to-Site VPN with static routing and need more bandwidth, you must switch to BGP.

### DX BGP ASN Conflicts
If you use the same private BGP ASN (65000–65534) for your on-prem AND AWS DX connections, you may see routing loops or dropped sessions. Use distinct ASNs. AWS recommends using ASN 65000 for on-prem and accepting the AWS-assigned private ASN (64512–65534) or using a public ASN if you own one.

### Route53 Geoproximity Requires Traffic Flow
Unlike all other Route53 routing policies, **Geoproximity requires Route53 Traffic Flow** (visual traffic policy editor). Traffic Flow has an additional monthly cost per policy (~$50/month). If cost is a concern, Geolocation (which doesn't require Traffic Flow) may be sufficient.

---

## 8. Hands-On Lab (Free Tier)

### Objective: BGP route preference experiment using two VPN connections to TGW (simulating DX+VPN failover behavior with BGP AS_PATH manipulation)

**Note:** This lab requires two simulated VPN endpoints. We'll use two OpenSwan EC2 instances in separate VPCs to simulate two different on-prem connections with different AS_PATH lengths.

**Cost:** ~$0.10/hr for two VPN connections to TGW. Terminate after lab.

#### Architecture

```
"HQ VPC" (10.100.0.0/16) - Primary connection
  OpenSwan EC2 → VPN Connection 1 → TGW
  BGP: AS 65001, advertises 10.100.0.0/16 with AS_PATH: 65001 (short = preferred)

"DR VPC" (10.101.0.0/16) - Secondary connection
  OpenSwan EC2 → VPN Connection 2 → TGW
  BGP: AS 65002, advertises 10.100.0.0/16 with AS_PATH: 65002 65002 65002 (long = backup)

Target VPC (10.0.0.0/16) attached to TGW
  EC2 → traffic to 10.100.0.0/16 should prefer VPN Connection 1
```

#### Step 1: Create Transit Gateway

1. VPC Console → Transit Gateways → Create
2. Name: `bgp-test-tgw`
3. ASN: `64512` (AWS default)
4. DNS support: Enable
5. VPN ECMP support: Enable (needed for ECMP in production)
6. Create

#### Step 2: Create Three VPCs

| VPC | CIDR | Purpose |
|---|---|---|
| Target VPC | 10.0.0.0/16 | Application (attach to TGW) |
| HQ VPC | 10.100.0.0/16 | Simulated primary on-prem |
| DR VPC | 10.101.0.0/16 | Simulated backup on-prem |

#### Step 3: Attach Target VPC to TGW

1. Transit Gateway Attachments → Create
2. Type: VPC
3. TGW: `bgp-test-tgw`
4. VPC: Target VPC
5. Subnets: select private subnet

#### Step 4: Create Two Customer Gateways

Launch OpenSwan EC2 in HQ VPC and DR VPC (assign Elastic IPs to both).

```
CGW-Primary: EIP of HQ OpenSwan, BGP ASN 65001
CGW-Secondary: EIP of DR OpenSwan, BGP ASN 65002
```

#### Step 5: Create Two VPN Connections to TGW

```
VPN-1: Customer Gateway = CGW-Primary, TGW = bgp-test-tgw, Routing: Dynamic (BGP)
VPN-2: Customer Gateway = CGW-Secondary, TGW = bgp-test-tgw, Routing: Dynamic (BGP)
```

Download both configuration files.

#### Step 6: Configure OpenSwan with AS_PATH Manipulation

**HQ OpenSwan (Primary — short AS_PATH):**
```bash
# /etc/quagga/bgpd.conf equivalent (or openswan + quagga combo)
# Advertise 10.100.0.0/16 with natural AS_PATH: 65001
router bgp 65001
  neighbor <AWS-BGP-IP-1> remote-as 64512
  network 10.100.0.0/16   # normal advertisement
```

**DR OpenSwan (Secondary — prepend AS_PATH):**
```bash
router bgp 65002
  neighbor <AWS-BGP-IP-2> remote-as 64512
  network 10.100.0.0/16
  neighbor <AWS-BGP-IP-2> route-map PREPEND out

route-map PREPEND permit 10
  set as-path prepend 65002 65002 65002   # ← makes path look longer (less preferred)
```

#### Step 7: Observe TGW Route Table

1. Transit Gateways → Transit Gateway Route Tables → default route table
2. Routes tab → look for 10.100.0.0/16
3. Should show two routes with different attachment IDs and path attributes
4. Active route should be via VPN-1 (shorter AS_PATH)

```bash
# CLI view
aws ec2 search-transit-gateway-routes \
  --transit-gateway-route-table-id tgw-rtb-xxx \
  --filters Name=type,Values=propagated
```

#### Step 8: Simulate Primary Failure

1. Stop the OpenSwan instance in HQ VPC
2. VPN Connection 1 tunnel state → DOWN
3. Wait ~60 seconds for BGP to detect failure
4. Check TGW route table → 10.100.0.0/16 should now route via VPN-2

Ping from Target VPC EC2 to 10.100.0.x — should continue working via backup path.

#### Step 9: Observe Global Accelerator (Bonus — requires internet-facing ALB)

If you have a public ALB from the load balancer lab:

1. Global Accelerator → Create Accelerator
2. Name: `lab-accelerator`
3. Add listener: TCP 80
4. Add endpoint group: us-east-1
5. Add endpoint: your ALB
6. Note the two static anycast IPs provided

```bash
# Test from your machine
curl http://<static-anycast-ip>/
# Compare latency vs direct ALB DNS
time curl http://<alb-dns>/
time curl http://<static-ip>/
```

#### Cleanup

```
1. Delete Global Accelerator
2. Delete both VPN connections
3. Delete Customer Gateways
4. Release Elastic IPs
5. Delete TGW Attachments
6. Delete Transit Gateway
7. Terminate all EC2 instances
8. Delete all 3 VPCs
```

---

*File last updated for SAP-C02 exam objectives. Always verify current AWS limits in official documentation.*
