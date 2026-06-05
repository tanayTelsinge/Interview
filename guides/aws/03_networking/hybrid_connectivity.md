# Hybrid Connectivity — VPN, Direct Connect, Transit Gateway

## The Problem
Enterprises don't migrate to cloud overnight. They need secure, reliable connectivity between on-premises data centers and AWS VPCs — with options ranging from quick internet-based encryption to dedicated private lines with consistent bandwidth.

---

## Site-to-Site VPN

### How It Works
```
On-Premises Router (Customer Gateway) ──── IPSec tunnels ────→ VGW or TGW (AWS)
                                          2 tunnels for HA
```

**Components:**
- **Customer Gateway (CGW):** AWS config object representing your on-prem router/firewall (public IP + optional BGP ASN)
- **Virtual Private Gateway (VGW):** AWS-side termination, attached to one VPC
- **Transit Gateway:** AWS-side termination for multi-VPC scenarios (preferred at scale)

**Characteristics:**
```
Setup:      Minutes to hours
Bandwidth:  Up to 1.25 Gbps per tunnel (internet-dependent)
Latency:    Variable (public internet)
Encryption: Yes — IPSec AES-256
Cost:       ~$0.05/hr + data transfer
Use when:   Quick setup, cost-sensitive, variable latency OK
```

**ECMP over TGW:** Multiple VPN tunnels to TGW with BGP → aggregate bandwidth (4 tunnels = 5 Gbps). Requires TGW (VGW does NOT support ECMP).

### Client VPN (remote workers)
```
User laptop → OpenVPN → Client VPN Endpoint (in VPC) → private resources
Auth: Active Directory, SAML/SSO, certificate-based
Cost: ~$0.10/hr per subnet association + $0.05/hr per active connection
```

---

## AWS Direct Connect (DX)

### The Problem with VPN
Internet-based VPN: variable latency, limited throughput, no SLA, jitter affects real-time workloads.

### How It Works
```
Your Data Center ──── your router ──── cross-connect cable ──── AWS DX Location
                                       (co-lo facility)          ──── AWS backbone ──── AWS Region
```

**Connection Types:**
| Type | Speed | Provisioning | Who Provides |
|---|---|---|---|
| Dedicated | 1G / 10G / 100G | Weeks-months | AWS direct |
| Hosted | 50Mbps–10Gbps | Days-weeks | APN Partner |

**Virtual Interfaces (VIFs):**
```
Private VIF → specific VPC (via VGW or DX Gateway)
Public VIF  → AWS public services (S3, EC2 public IPs) over private backbone
Transit VIF → Transit Gateway (multi-VPC, preferred for scale)
```

### Direct Connect Gateway
One DX Gateway → multiple VPCs in multiple regions (cross-account supported):
```
On-Premises ──── DX ────→ DX Gateway ──→ VPC us-east-1
                                      ──→ VPC eu-west-1
                                      ──→ VPC ap-southeast-1
```
**Important:** DX Gateway does NOT enable VPC-to-VPC traffic. On-prem ↔ VPC only.

### Key Characteristics
```
Setup:      Weeks-months (dedicated) / Days (hosted)
Bandwidth:  Consistent (not shared internet)
Latency:    Low and consistent
Encryption: NOT by default → add VPN over DX or MACsec
Cost:       Port-hour + data transfer out (~$2.25/hr for 10G + $0.02/GB)
Use when:   Consistent performance, large data transfer, compliance, low latency
```

### DX Resilience Models
```
No Resilience:   1 DX, 1 location (dev only)
HA:              2 DX, SAME location (device failure protection)
Max Resiliency:  2 DX, 2 DIFFERENT locations (recommended for prod)
Ultra:           4 DX, 2 locations (mission-critical)

Best practice: DX primary + VPN fallback (BGP prefers DX, failover to VPN)
```

### SiteLink
Route on-prem ↔ on-prem traffic via AWS backbone using existing DX connections:
```
Office A (DX London) ──→ AWS backbone ──→ Office B (DX New York)
```

---

## AWS Transit Gateway (TGW)

### The Problem (VPC Peering at Scale)
```
N VPCs: non-transitive peering = N×(N-1)/2 connections
10 VPCs = 45 connections. No shared VPN or DX.
```

### How It Works
Hub-and-spoke: everything connects to TGW, TGW routes between them:
```
TGW ──→ VPC Prod
    ──→ VPC Dev
    ──→ VPC Shared Services
    ──→ VPN (on-premises Office 1)
    ──→ VPN (on-premises Office 2)
    ──→ DX (via Transit VIF)
```

### TGW Route Tables (Traffic Segmentation)
```
Default: all attachments can talk to each other

Custom segmentation:
  Prod route table  → associates Prod VPC, propagates Shared Services + on-prem VPN
  Dev route table   → associates Dev VPC, propagates Shared Services only (no prod/on-prem)
```

### Key Features
| Feature | Notes |
|---|---|
| **Cross-region peering** | TGW ↔ TGW across regions (static routes only) |
| **ECMP** | Multiple VPN tunnels → aggregate bandwidth (needs BGP + TGW) |
| **Multicast** | Supported (financial, media streaming) |
| **Appliance Mode** | Prevents asymmetric routing through NGFW appliances |

### TGW Pricing
```
Per attachment-hour: $0.05 (VPC, VPN, DX, peering)
Per GB processed:    $0.02
10 VPCs + 2 VPNs = 12 × $0.05 = $0.60/hr ≈ $438/month (plus data)
```

---

## Decision Tree

```
Connectivity requirement?
│
├── Quick, encrypted, internet-based, one VPC?
│   └── Site-to-Site VPN + VGW
│
├── Quick, encrypted, multiple VPCs?
│   └── Site-to-Site VPN + Transit Gateway
│
├── Consistent bandwidth, low latency, private, compliance?
│   └── Direct Connect
│       ├── One VPC → Private VIF + DX Gateway
│       ├── Many VPCs → Transit VIF + TGW
│       └── AWS public services (S3) → Public VIF
│
├── Many VPCs need to communicate (hub-and-spoke)?
│   └── Transit Gateway
│
├── Redundancy for DX?
│   └── DX + VPN backup (BGP failover) OR 2 DX at 2 locations
│
└── Remote user access?
    └── Client VPN (OpenVPN)
```

---

## Common Patterns

### Standard Enterprise Hybrid
```
On-Prem DC ──── DX (10G, Transit VIF) ────→ TGW
           ──── VPN (IPSec, fallback) ──→ TGW
                                           ├── VPC Prod
                                           ├── VPC Dev
                                           └── VPC Shared Services
BGP: DX preferred (higher LOCAL_PREF), VPN fallback
```

---

## Gotchas
1. **DX not encrypted** — add VPN over DX for IPSec or use MACsec on 10G/100G ports
2. **DX Gateway ≠ VPC-to-VPC** — only on-prem ↔ VPC connectivity
3. **TGW cross-region = static routes only** — no BGP propagation
4. **ECMP needs BGP** — static routes on VPN connections can't use ECMP
5. **TGW Appliance Mode required for NGFW** — prevents asymmetric routing
6. **VPN capped at 1.25 Gbps/tunnel** — use ECMP across multiple tunnels or DX for more

---

## Hands-On Lab

```bash
# Create Customer Gateway
aws ec2 create-customer-gateway --type ipsec.1 --public-ip YOUR_IP --bgp-asn 65000

# Create VGW and attach to VPC
aws ec2 create-vpn-gateway --type ipsec.1
aws ec2 attach-vpn-gateway --vpn-gateway-id vgw-xxx --vpc-id vpc-xxx

# Create VPN connection
aws ec2 create-vpn-connection --type ipsec.1 \
  --customer-gateway-id cgw-xxx --vpn-gateway-id vgw-xxx

# Enable route propagation on route table
aws ec2 enable-vgw-route-propagation --route-table-id rtb-xxx --gateway-id vgw-xxx

# For TGW:
aws ec2 create-transit-gateway
aws ec2 create-transit-gateway-vpc-attachment --transit-gateway-id tgw-xxx \
  --vpc-id vpc-xxx --subnet-ids subnet-xxx
```
