# Amazon VPC — SAP-C02 Deep Dive

---

## 1. The Problem

Before VPC, AWS ran EC2 instances in a flat, shared network (EC2-Classic). Every instance got a public IP, shared the same broadcast domain, and had no logical isolation from other customers' workloads. Running a multi-tier application meant exposing your database servers to the internet unless you layered complex host-based firewalls everywhere. There was no way to extend your corporate network into AWS or enforce network-level segmentation between environments.

**Core pain points VPC was designed to solve:**
- No tenant isolation at the network layer
- No support for RFC 1918 private address space in a controlled manner
- No way to mirror on-premises network topology in the cloud
- No gateway-level traffic filtering

---

## 2. What AWS Built

**Amazon Virtual Private Cloud (VPC)** is a logically isolated section of the AWS cloud where you launch AWS resources in a virtual network that you define. You have complete control over the IP address range, subnet creation, route tables, network gateways, and security settings.

Key facts:
- Region-scoped (spans all AZs in a region)
- Up to **5 VPCs per region** by default (soft limit, can request increase)
- Up to **200 subnets per VPC** by default
- One **default VPC** per region (pre-created, all public subnets, CIDR `172.31.0.0/16`)
- IPv4 CIDR block required; IPv6 optional (dual-stack)
- CIDR sizes: **/16** (65,536 IPs) to **/28** (16 IPs)

---

## 3. How It Works

### 3.1 CIDR Blocks

When you create a VPC you assign a primary IPv4 CIDR. You can add up to **4 secondary CIDRs** (5 total). AWS reserves 5 IPs from every subnet:

| Reserved IP | Purpose |
|---|---|
| `.0` | Network address |
| `.1` | VPC router |
| `.2` | AWS DNS |
| `.3` | Future use |
| `.255` | Broadcast (not supported, reserved) |

So a `/28` (16 addresses) gives you only **11 usable** host addresses.

### 3.2 Subnet Types

Subnets live in a single AZ. The "type" is determined entirely by the **route table** attached to the subnet — there is no inherent type flag.

| Subnet Type | Route Table Has | Use Case |
|---|---|---|
| **Public** | Route to Internet Gateway (IGW) | Web servers, load balancers, NAT Gateways |
| **Private** | Route to NAT Gateway/Instance | App servers, databases needing outbound internet |
| **Isolated** | No internet route at all | RDS, internal-only services, maximum isolation |

Instances in a public subnet also need a **public IP or Elastic IP** to be reachable from the internet — the IGW alone is not enough.

### 3.3 Internet Gateway (IGW)

- Horizontally scaled, redundant, highly available AWS-managed component
- **1 per VPC** (you cannot attach multiple)
- **Stateful** — tracks connections, allows return traffic automatically
- Performs **1:1 NAT** between an instance's private IP and its public/Elastic IP
- No bandwidth limits, no single point of failure

```
Internet ──► IGW ──► Route Table (0.0.0.0/0 → igw-xxx) ──► Public Subnet ──► Instance (Private IP + Public IP)
```

### 3.4 NAT Gateway vs NAT Instance

Used to give private subnet instances **outbound** internet access without exposing them to inbound connections.

| Feature | NAT Gateway (Managed) | NAT Instance (EC2-based) |
|---|---|---|
| **Management** | Fully managed by AWS | You manage OS, patches, failover |
| **Availability** | Highly available within AZ | Single instance (needs scripts/ASG for HA) |
| **Bandwidth** | Up to 100 Gbps (scales automatically) | Depends on instance type |
| **Cost** | ~$0.045/hr + $0.045/GB data | EC2 instance cost + data transfer |
| **Bastion host** | Cannot be used as one | Can be used as bastion + NAT |
| **Port forwarding** | Not supported | Supported (iptables) |
| **Security Groups** | Cannot be associated | Can associate Security Groups |
| **Source/dest check** | Disabled automatically | Must disable manually |
| **Performance** | 5 Gbps initially, scales to 100 Gbps | Limited by instance size |
| **Multi-AZ HA** | Deploy one per AZ | Requires automation |

**Cost note:** A NAT Gateway in AZ-A serving traffic from subnets in AZ-B incurs cross-AZ data transfer charges. Best practice: **one NAT Gateway per AZ**.

### 3.5 Route Tables

- Every VPC has a **main route table** (default for subnets without explicit association)
- Subnets can have explicit route table associations
- **Local route is always present and cannot be removed**: e.g., `10.0.0.0/16 → local`
- Route priority: **most specific prefix wins** (longest prefix match)
- A route table can be associated with multiple subnets
- A subnet can only be associated with **one route table** at a time

```
Route Table Example (Public Subnet):
Destination     Target
10.0.0.0/16     local           ← always present, cannot delete
0.0.0.0/0       igw-abc123      ← makes this subnet "public"

Route Table Example (Private Subnet):
Destination     Target
10.0.0.0/16     local
0.0.0.0/0       nat-abc123
```

### 3.6 Security Groups

- Operate at the **ENI (Elastic Network Interface)** level
- **Stateful** — if you allow inbound, the return outbound traffic is automatically allowed
- **Allow rules only** — you cannot write deny rules
- Default SG: allows all outbound, allows inbound from same SG
- Custom SG: no inbound by default, all outbound allowed
- Can reference **other Security Groups** as sources/destinations (instead of CIDR blocks)
- Up to **5 SGs per ENI**, up to **60 inbound + 60 outbound rules per SG**
- Changes take effect **immediately**

```
# Example: Allow app tier to reach DB tier (same VPC)
DB SG Inbound Rule:
  Protocol: TCP
  Port: 3306
  Source: sg-app-tier-id   ← references another SG, not a CIDR
```

### 3.7 Network ACLs (NACLs)

- Operate at the **subnet** level — applies to all traffic entering/leaving the subnet
- **Stateless** — you must explicitly allow both inbound AND outbound for bidirectional traffic
- Support both **Allow and Deny** rules
- Rules evaluated in **ascending numerical order** (lowest number first); first match wins
- Each subnet has exactly **one NACL** (default NACL allows all traffic)
- Changes may take a moment to propagate

```
NACL Example (Public Subnet):
Inbound Rules:
Rule#  Protocol  Port Range   Source          Action
100    TCP       80           0.0.0.0/0       ALLOW
110    TCP       443          0.0.0.0/0       ALLOW
120    TCP       1024-65535   0.0.0.0/0       ALLOW    ← ephemeral ports for return traffic
*      ALL       ALL          0.0.0.0/0       DENY     ← implicit deny

Outbound Rules:
Rule#  Protocol  Port Range   Destination     Action
100    TCP       80           0.0.0.0/0       ALLOW
110    TCP       443          0.0.0.0/0       ALLOW
120    TCP       1024-65535   0.0.0.0/0       ALLOW    ← ephemeral ports
*      ALL       ALL          0.0.0.0/0       DENY
```

**Ephemeral ports:** When a client initiates a connection, the OS assigns a random source port in range **1024–65535**. NACLs must allow this range for response traffic to return to the client.

### 3.8 Security Groups vs NACLs

| Feature | Security Group | NACL |
|---|---|---|
| **Level** | ENI (instance) | Subnet |
| **State** | Stateful | Stateless |
| **Rules** | Allow only | Allow + Deny |
| **Evaluation** | All rules evaluated | Rules evaluated in order (first match wins) |
| **Return traffic** | Automatically allowed | Must explicitly allow |
| **Scope** | Per instance/ENI | All resources in subnet |
| **Default** | Deny all inbound, allow all outbound | Allow all (default NACL) |
| **Use case** | Fine-grained instance control | Subnet-wide blocking (e.g., block specific IPs) |

### 3.9 VPC Flow Logs

Captures IP traffic metadata going to/from network interfaces in your VPC. Can be enabled at **VPC, subnet, or ENI** level. Delivered to CloudWatch Logs or S3.

**What IS captured:**
- Accepted and rejected traffic
- Source/destination IP, port, protocol
- Bytes and packets
- Action (ACCEPT/REJECT)
- Log status

**What is NOT captured (exam-critical):**
- Traffic to/from the **EC2 metadata service** (169.254.169.254)
- **DHCP** traffic
- Traffic to the **Amazon DNS resolver** (169.254.169.253 or the VPC+2 address)
- Traffic for **Windows license activation**
- Traffic between a **VPC endpoint** and the service (only traffic to/from ENI is logged)
- **Mirror traffic** (use Traffic Mirroring for packet capture)

Flow Log fields (default format):
```
version account-id interface-id srcaddr dstaddr srcport dstport protocol packets bytes windowstart windowend action log-status
```

### 3.10 DNS in VPC

Two VPC-level flags control DNS behavior:

| Setting | Default | Meaning |
|---|---|---|
| `enableDnsSupport` | true | VPC uses AWS DNS resolver at VPC+2 address |
| `enableDnsHostnames` | false (true for default VPC) | Public instances get DNS hostnames |

Both must be `true` for instances to get public DNS names and for Route53 private hosted zones to work with the VPC.

**Route53 Resolver Endpoints** (for hybrid DNS):

| Type | Direction | Use Case |
|---|---|---|
| **Inbound Endpoint** | On-prem → AWS | On-prem DNS servers forward queries for AWS resources to Route53 |
| **Outbound Endpoint** | AWS → On-prem | EC2 instances can resolve on-prem DNS names via forwarding rules |

### 3.11 VPC Endpoints

Allow private connectivity to AWS services **without traversing the internet**.

#### Gateway Endpoints
- Supported services: **S3 and DynamoDB only**
- **Free** (no hourly charge, no data processing charge)
- Implemented via a **prefix list entry in route tables**
- Not an ENI — no IP address assigned
- Subnet-level route table entry required
- Cannot be accessed outside the VPC (no cross-region, no on-prem over DX)

```
Route Table with Gateway Endpoint:
Destination              Target
10.0.0.0/16              local
0.0.0.0/0                nat-abc123
pl-xxxxxx (S3 prefix)    vpce-xxxxxxxxx    ← traffic to S3 goes via endpoint, not internet
```

#### Interface Endpoints (PrivateLink)
- Supported services: **Most AWS services** (SSM, EC2 API, KMS, SNS, SQS, etc.) + third-party services
- Creates an **ENI** with a private IP in your subnet
- **Cost:** ~$0.01/hr per AZ + $0.01/GB data processed
- DNS resolution: creates **private DNS names** for the service
- Enable `Private DNS` to override the service's public endpoint DNS to resolve to the private ENI IP

### 3.12 VPC Peering

- Direct network connection between **two VPCs** using AWS backbone
- Supports **same account, cross-account, cross-region**
- Traffic is **encrypted** (never traverses the public internet)
- CIDR blocks **must not overlap**
- **Non-transitive:** A peered with B, B peered with C does NOT mean A can reach C
- Must update **route tables on both sides** and update Security Groups if needed
- No bandwidth bottleneck, no single point of failure

```
                    VPC A ──── Peer ──── VPC B
                                           |
                                         Peer
                                           |
                                         VPC C
A cannot reach C through B. A must peer directly with C.
```

**Limits:** Up to 125 peering connections per VPC (default).

### 3.13 AWS PrivateLink

Allows you to expose a service in your VPC to other VPCs **without VPC peering, IGW, NAT, or VPN**.

- **Producer side:** Deploy your service behind a **Network Load Balancer**
- **Consumer side:** Create a **VPC Endpoint (Interface type)** in their VPC
- Traffic stays on AWS network
- One-directional: consumer can reach producer, not vice versa
- Supports **cross-account and cross-region** (with additional configuration)
- Used by AWS itself for all Interface VPC Endpoints

### 3.14 IPv6

- VPC can be **dual-stack** (IPv4 + IPv6)
- IPv6 CIDRs are `/56` per VPC, `/64` per subnet
- All IPv6 addresses are **publicly routable** (no private IPv6 in AWS)
- No NAT for IPv6 — instead use **Egress-Only Internet Gateway (EIGW)**
  - Like an IGW but only allows **outbound** IPv6 traffic
  - Stateful — blocks unsolicited inbound IPv6
- Subnet auto-assign IPv6 addresses can be enabled
- Not all services support IPv6 yet

### 3.15 Elastic IP (EIP)

- Static public IPv4 address you allocate to your account
- Can be associated with an EC2 instance or ENI
- **Free while associated with a running instance**
- **Charged (~$0.005/hr) when not associated** or when associated with stopped instance
- Up to **5 EIPs per region** (soft limit)
- Remapping takes seconds — useful for failover scenarios

### 3.16 Elastic Network Interface (ENI)

- Virtual network card that can be attached to EC2 instances
- Attributes: primary private IP, secondary private IPs, EIP, MAC address, SGs
- **eth0** is the primary ENI and cannot be detached
- Additional ENIs can be attached/detached while running
- ENI attributes follow the ENI, not the instance — useful for license-bound software
- Used for: dual-homing, management networks, PrivateLink endpoints

---

## 4. Key Config & Limits

| Parameter | Default Limit | Hard Limit |
|---|---|---|
| VPCs per region | 5 | Adjustable |
| Subnets per VPC | 200 | Adjustable |
| IPv4 CIDR blocks per VPC | 5 | 5 |
| Route tables per VPC | 200 | Adjustable |
| Routes per route table | 50 | 1000 |
| Security Groups per ENI | 5 | 16 |
| Rules per Security Group | 60 inbound + 60 outbound | 60 |
| NACLs per VPC | 200 | Adjustable |
| Rules per NACL | 20 | 40 |
| Peering connections per VPC | 125 | Adjustable |
| EIPs per region | 5 | Adjustable |
| NAT Gateways per AZ | 5 | Adjustable |
| Interface Endpoints per VPC | 50 | Adjustable |

---

## 5. Decision Tree

### Where to Place a Resource

```
Does it need inbound traffic from the internet?
├── YES → Public Subnet (IGW route required + public IP)
└── NO → Does it need outbound internet access?
         ├── YES → Private Subnet (route via NAT Gateway)
         └── NO → Does it need to reach other AWS services?
                  ├── YES (S3/DynamoDB) → Isolated Subnet + Gateway Endpoint
                  ├── YES (other services) → Isolated Subnet + Interface Endpoint
                  └── NO → Isolated Subnet (no internet route at all)
```

### Which VPC Endpoint Type?

```
Which AWS service?
├── S3 or DynamoDB → Gateway Endpoint (free, route-table based)
└── Any other AWS service → Interface Endpoint (PrivateLink, ENI, has cost)
    └── Do you need access from on-premises or other VPCs?
        ├── YES → Interface Endpoint + DNS forwarding
        └── NO → Interface Endpoint with private DNS enabled
```

### Security Group vs NACL?

```
Need to DENY specific IPs/ranges?
├── YES → Use NACL (SGs cannot deny)
└── NO → Use Security Group
    └── Need to restrict access within a subnet between instances?
        ├── YES → Security Group (NACLs are subnet-level only)
        └── Either works → Security Group preferred (stateful, simpler)
```

### VPC Peering vs PrivateLink?

```
Are you exposing a service to many consumers?
├── YES → PrivateLink (one NLB, many consumers, no CIDR conflicts)
└── NO → Are CIDRs non-overlapping?
         ├── YES → VPC Peering (full mesh routing, simpler for small scale)
         └── NO → PrivateLink (works regardless of CIDRs)
```

---

## 6. Common Patterns

### Pattern 1: Three-Tier Web Application

```
                          Internet
                             |
                           [IGW]
                             |
              ┌──────────────┼──────────────┐
              │          Public Subnet       │
              │    [ALB]  [NAT GW]  [Bastion]│
              └──────────────┼──────────────┘
                             |
              ┌──────────────┼──────────────┐
              │          Private Subnet      │
              │         [EC2 App Tier]       │
              └──────────────┼──────────────┘
                             |
              ┌──────────────┼──────────────┐
              │         Isolated Subnet      │
              │          [RDS MySQL]         │
              └─────────────────────────────┘
```

### Pattern 2: Multi-Account with VPC Endpoints for S3

```
VPC (App Account)
├── Private Subnet → EC2 instances
├── Route Table: pl-s3-prefix → vpce-gateway (free)
└── No NAT Gateway needed for S3 access

Benefits: No data egress charges for S3, traffic stays private
```

### Pattern 3: Restricting S3 Access to VPC Only

```json
// S3 Bucket Policy: deny if not from VPC endpoint
{
  "Effect": "Deny",
  "Principal": "*",
  "Action": "s3:*",
  "Resource": ["arn:aws:s3:::my-bucket", "arn:aws:s3:::my-bucket/*"],
  "Condition": {
    "StringNotEquals": {
      "aws:sourceVpce": "vpce-xxxxxxxx"
    }
  }
}
```

### Pattern 4: Hub-and-Spoke with Shared Services VPC

```
                     Shared Services VPC
                     (DNS, AD, Monitoring)
                            |
             ┌──────────────┼──────────────┐
             |              |              |
          VPC Peer       VPC Peer       VPC Peer
           Dev VPC       Test VPC       Prod VPC

Each app VPC peers with Shared Services VPC only.
App VPCs do NOT peer with each other (non-transitive peering).
For full-mesh, use Transit Gateway instead.
```

---

## 7. Gotchas

### NACL Stateless Trap
**Problem:** You add an inbound ALLOW for TCP 443 in your NACL but traffic still fails.
**Why:** NACLs are stateless. You must also add an OUTBOUND ALLOW for ephemeral ports 1024–65535. The client's return traffic comes back on a random high port, not 443.

### Default VPC Risk
The default VPC has all subnets configured as **public** (routes to IGW) and `enableDnsHostnames` is true. Launching an EC2 instance here without thinking gives it a public IP by default. Never use the default VPC for sensitive workloads — create a dedicated VPC with properly designed public/private/isolated tiers.

### VPC Peering is Non-Transitive
A → B and B → C does NOT give A access to C. This is a fundamental property. Trying to route through an intermediate VPC will not work. Use Transit Gateway for hub-and-spoke or full-mesh routing between many VPCs.

### Security Group Cross-Account References
You CAN reference security groups from another account in your SG rules, but only if:
1. The VPCs are peered
2. You use the format `account-id/sg-id` as the source

This is limited to **peered VPCs** and does not work across Transit Gateway connections.

### Flow Logs Miss Internal AWS Traffic
Flow logs DO NOT capture: traffic to 169.254.169.254 (metadata), DHCP, or DNS to the VPC resolver. If you're debugging why an instance can't reach the metadata service, flow logs won't help.

### Gateway Endpoint Scope
Gateway endpoints are **VPC-scoped** only. Traffic from on-premises (via DX or VPN) cannot use a Gateway Endpoint to reach S3. For on-premises S3 access via private connectivity, use Interface Endpoints.

### ENI Limits per Instance Type
The number of ENIs and secondary IPs per ENI is limited per instance type. For example, a `t3.small` supports only 3 ENIs. This affects container networking (EKS/ECS) where each pod/task may consume an IP.

### CIDR Can't Shrink
Once assigned, a VPC primary CIDR cannot be changed or shrunk. You can only add secondary CIDRs. Plan your IP space carefully upfront.

### Security Group Rules Are Evaluated as a Set
Unlike NACLs, ALL SG rules are evaluated together — there is no ordering. If any rule allows traffic, it is allowed. You cannot "override" an allow with a deny in SGs.

---

## 8. Hands-On Lab (Free Tier)

### Objective: Build a two-tier VPC with public/private subnets, NAT Gateway, and VPC endpoint for S3

**Estimated cost:** ~$0.045/hr for NAT Gateway while running. Stop after lab.

#### Step 1: Create the VPC

1. Go to VPC Console → **Your VPCs** → **Create VPC**
2. Name: `lab-vpc`
3. IPv4 CIDR: `10.0.0.0/16`
4. Click Create

#### Step 2: Create Subnets

Create 4 subnets (2 AZs × 2 tiers):

| Subnet Name | AZ | CIDR |
|---|---|---|
| `public-1a` | us-east-1a | `10.0.0.0/24` |
| `public-1b` | us-east-1b | `10.0.1.0/24` |
| `private-1a` | us-east-1a | `10.0.10.0/24` |
| `private-1b` | us-east-1b | `10.0.11.0/24` |

For public subnets: check **Enable auto-assign public IPv4 address**.

#### Step 3: Create and Attach Internet Gateway

1. VPC Console → **Internet Gateways** → Create
2. Name: `lab-igw`
3. Select → **Actions → Attach to VPC** → select `lab-vpc`

#### Step 4: Create Route Tables

**Public Route Table:**
1. Create route table, name `public-rt`, attach to `lab-vpc`
2. Routes tab → Edit routes → Add: `0.0.0.0/0` → Target: `lab-igw`
3. Subnet associations → Edit → select `public-1a`, `public-1b`

**Private Route Table:**
1. Create route table, name `private-rt`
2. Subnet associations → `private-1a`, `private-1b`
   (we'll add NAT route after creating the NAT GW)

#### Step 5: Create NAT Gateway

1. VPC Console → **NAT Gateways** → Create
2. Subnet: `public-1a` (NAT Gateway goes in public subnet)
3. Allocate Elastic IP → click Allocate
4. Create NAT Gateway (wait for status = Available, ~1-2 min)
5. Go to `private-rt` → Edit routes → Add: `0.0.0.0/0` → Target: NAT Gateway

#### Step 6: Launch Test Instances

**Public instance:**
- AMI: Amazon Linux 2023
- Subnet: `public-1a`
- Auto-assign public IP: enabled
- SG: allow SSH (22) from your IP, allow all outbound

**Private instance:**
- Subnet: `private-1a`
- No public IP
- SG: allow SSH (22) from public instance SG

SSH to public instance, then SSH to private instance using private IP as a jump host:
```bash
# From your machine
ssh -A ec2-user@<public-instance-ip>    # -A for agent forwarding

# From public instance
ssh ec2-user@10.0.10.x                  # private instance IP
```

#### Step 7: Create S3 Gateway Endpoint

1. VPC Console → **Endpoints** → Create Endpoint
2. Service category: AWS services
3. Search: `com.amazonaws.us-east-1.s3` → select **Gateway** type
4. VPC: `lab-vpc`
5. Route tables: select `private-rt` (this adds the prefix list route)
6. Policy: Full access
7. Create

Verify from private instance:
```bash
# Should work even without internet (NAT Gateway) if you remove the 0.0.0.0/0 route
aws s3 ls --region us-east-1
```

#### Step 8: Test Security Group vs NACL Behavior

Add a NACL deny rule to block traffic from your home IP:
1. VPC → **Network ACLs** → select the NACL on your public subnet
2. Edit inbound rules → Add rule #90 (before 100): DENY TCP 22 from `<your-ip>/32`
3. Attempt SSH — it should be blocked even if the SG allows it
4. Then delete the NACL rule and observe SG alone controls access

#### Cleanup

```
1. Delete NAT Gateway (wait for deletion)
2. Release Elastic IP
3. Delete VPC endpoints
4. Terminate EC2 instances
5. Delete VPC (this removes subnets, route tables, IGW automatically)
```

---

*File last updated for SAP-C02 exam objectives. Always verify current AWS limits in official documentation.*
