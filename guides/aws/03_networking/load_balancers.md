# Elastic Load Balancing & Route53 — SAP-C02 Deep Dive

---

## 1. The Problem

As applications scaled horizontally, a single entry point became a bottleneck and single point of failure. Users needed:
- **Traffic distribution** across multiple healthy targets
- **Automatic failover** when individual instances become unhealthy
- **SSL/TLS termination** to offload crypto from application servers
- **Layer 7 routing** to direct traffic to different microservices based on URL path
- **Global routing intelligence** to send users to the nearest or healthiest region

Two separate but tightly coupled AWS services address this: **Elastic Load Balancing (ELB)** for within-region distribution and **Route53** for global DNS-based routing.

---

## 2. What AWS Built

**Elastic Load Balancing** automatically distributes incoming traffic across multiple targets (EC2, containers, IPs, Lambda functions) in one or more AZs. Four distinct load balancer types exist, each solving a different problem at a different network layer.

**Amazon Route53** is a highly available, scalable DNS service that also provides domain registration and health-based routing for global traffic management.

---

## 3. How It Works

### 3.1 Application Load Balancer (ALB)

**Layer 7 (HTTP/HTTPS/gRPC/WebSocket)** — understands the application protocol.

#### Routing Rules

ALB routes requests based on **listener rules**, evaluated in priority order (lowest number first). Each rule has **conditions** and an **action**.

| Condition Type | Example |
|---|---|
| **Host header** | `api.example.com` vs `www.example.com` |
| **Path pattern** | `/api/*` vs `/images/*` |
| **HTTP header** | `X-Custom-Header: value` |
| **HTTP method** | `GET`, `POST` |
| **Query string** | `?platform=mobile` |
| **Source IP** | CIDR-based |

```
Listener: HTTPS 443
├── Rule 1 (priority 10): IF path=/api/* THEN forward → api-target-group
├── Rule 2 (priority 20): IF host=admin.example.com THEN forward → admin-tg
├── Rule 3 (priority 30): IF query=version:2 THEN forward → v2-target-group
│                          WEIGHT: v2-tg (80%), v1-tg (20%)   ← weighted routing
└── Default Rule: forward → main-target-group
```

#### Weighted Target Groups
Assign a weight (0–999) to different target groups in the same action. Useful for:
- **Canary deployments** (5% to new version, 95% to old)
- **Blue/green switching** (gradually shift weight)
- **A/B testing**

#### Target Types

| Target Type | Details |
|---|---|
| **EC2 Instance** | Register by instance ID; ALB uses instance's primary IP |
| **IP** | Register by IP; supports any IPv4 including on-prem over DX/VPN |
| **Lambda** | ALB invokes Lambda synchronously; request/response in JSON |
| **ALB** | An NLB can have ALB targets (used with AWS PrivateLink scenarios) |

#### Sticky Sessions

Two modes, both prevent requests from the same client from going to different targets:

| Mode | Cookie Name | Set By | Duration |
|---|---|---|---|
| **Duration-based** | `AWSALB` (ALB-generated) | ALB | 1 second – 7 days |
| **Application-based** | Custom name (app-generated) | Your application | App controls |

Sticky sessions break even distribution — use only when required for session state. Prefer stateless design + ElastiCache.

#### Other ALB Features
- **WAF integration:** Attach AWS WAF Web ACL directly to ALB
- **Deregistration delay:** Default 300 seconds (0–3600). Time ALB waits for in-flight connections to complete before removing a target. Reduce this for faster deployments.
- **Connection draining** is the old CLB term; same concept
- **Idle timeout:** Default 60 seconds (1–4000). How long to keep idle connections open
- **gRPC support:** ALB can route gRPC traffic and perform health checks on gRPC endpoints
- **WebSocket:** Supported natively; connection is upgraded and kept open

#### ALB Access Logs
Detailed logs per request to S3. Includes: timestamp, client IP, target, request processing time, HTTP method, URL, user-agent, SSL cipher. Not real-time.

---

### 3.2 Network Load Balancer (NLB)

**Layer 4 (TCP/UDP/TLS)** — does not inspect application content.

#### Key Characteristics

| Feature | Detail |
|---|---|
| **Protocol** | TCP, UDP, TCP_UDP, TLS |
| **Performance** | Millions of requests per second, ultra-low latency |
| **Static IP** | One static IP per AZ (allocated at creation) |
| **Elastic IP** | Can bring your own EIP per AZ |
| **Source IP preservation** | Passes client IP to target (no X-Forwarded-For needed) |
| **TLS termination** | Supported; NLB decrypts TLS and forwards TCP to target |
| **Health checks** | TCP, HTTP, HTTPS |

#### Source IP Preservation

NLB passes the client's original IP directly to the backend — the target sees the actual client IP in its connection. This is different from ALB where you need `X-Forwarded-For` header. If using NLB in **IP target type**, ensure your security groups allow the client CIDRs (not the NLB IPs).

#### When NLB is Required (vs ALB)
- **PrivateLink:** NLB is required to expose a service via PrivateLink (ALB cannot be the producer)
- **Static IP requirements:** Whitelisting by IP address (e.g., partner APIs that require known IPs)
- **Non-HTTP protocols:** MQTT, SMTP, FTP, custom TCP protocols
- **Extreme performance:** When ALB latency or TPS is insufficient
- **UDP traffic:** ALB does not support UDP

#### Cross-Zone Load Balancing

| Load Balancer | Cross-Zone Default | Cost for Cross-Zone |
|---|---|---|
| **ALB** | ON (always) | Free |
| **NLB** | OFF | $0.01/GB across AZs |
| **GWLB** | OFF | $0.01/GB across AZs |
| **CLB** | OFF (can enable free) | Free when enabled |

**NLB gotcha:** Cross-zone LB is disabled by default on NLB. If AZ-A has 2 targets and AZ-B has 8 targets, and cross-zone is off, AZ-A targets will handle 50% of traffic (not 20%). Enable cross-zone for even distribution.

---

### 3.3 Classic Load Balancer (CLB)

**Legacy — do not use for new deployments.** Layer 4 and basic Layer 7. No path-based routing, no SNI, limited features. AWS recommends migrating to ALB or NLB. Included on exam only in legacy migration contexts.

---

### 3.4 Gateway Load Balancer (GWLB)

**Layer 3/4 — for virtual network appliances** (firewalls, IDS/IPS, deep packet inspection).

#### The Problem It Solves

You want to route ALL traffic through a third-party firewall (Palo Alto, Fortinet, etc.) before it reaches your application — without modifying your network topology.

#### How It Works: Bump-in-the-Wire

```
Internet → [GWLB Endpoint in App VPC]
                ↓
         [GWLB in Security VPC] ← GENEVE tunnel (port 6081)
                ↓
         [Firewall EC2 Appliances] ← inspect/filter
                ↑
         [GWLB in Security VPC]
                ↓
         [GWLB Endpoint in App VPC]
                ↓
         Application
```

#### Key Points
- Uses **GENEVE protocol** (port 6081) to encapsulate packets — preserves original source/destination IPs
- Appliances see original L3/L4 headers
- If appliance **drops** the packet → traffic is dropped (inline)
- If appliance **forwards** the packet → GWLB sends to destination
- Scales appliance fleet automatically
- Integrates with **PrivateLink** for cross-account appliance sharing
- Target type: EC2 instances only (the appliances)

---

### 3.5 Target Groups

Targets can be registered in multiple target groups. Health check settings are per target group.

| Setting | Default | Notes |
|---|---|---|
| Health check protocol | Same as target group protocol | Can override |
| Health check path | `/` | Change to actual health endpoint |
| Healthy threshold | 3 consecutive successes | |
| Unhealthy threshold | 3 consecutive failures | Reduce for faster detection |
| Timeout | 5 seconds | |
| Interval | 30 seconds | Reduce to 10s for faster detection |
| Success codes | 200 | Can specify range: 200-299 |

**Deregistration delay:** When a target is deregistered (instance stopping, deployment), ALB waits this long for existing connections to complete. Default 300s — reduce to 30-60s for faster rolling deployments.

---

### 3.6 Health Checks

| Health Check Type | Used With | Details |
|---|---|---|
| **ELB health checks** | Target groups | HTTP/HTTPS/TCP; ALB/NLB marks targets healthy/unhealthy |
| **EC2 status checks** | ASG integration | System and instance checks by EC2 |
| **Custom health checks** | ASG | Application-level checks, post results via API |

ELB health checks are separate from ASG health checks. You need to configure the ASG to use ELB health checks explicitly (`HealthCheckType: ELB`) otherwise ASG only uses EC2 system/instance checks.

---

## Route53

### 3.7 Record Types

| Record Type | Purpose | Notes |
|---|---|---|
| **A** | Maps hostname to IPv4 address | `example.com → 1.2.3.4` |
| **AAAA** | Maps hostname to IPv6 address | |
| **CNAME** | Maps hostname to another hostname | Cannot be used at **zone apex** (naked domain) |
| **NS** | Name server records | Delegated by registrar |
| **MX** | Mail exchange | Priority-based |
| **TXT** | Text records | Domain verification, SPF, DKIM |
| **PTR** | Reverse DNS lookup | IP → hostname |
| **CAA** | Certificate Authority Authorization | |
| **SRV** | Service location records | Used by some protocols |
| **Alias** | AWS extension; maps to AWS resources | At apex, points to AWS resources, **free queries**, auto-tracks IP changes |

#### Alias vs CNAME

| | CNAME | Alias |
|---|---|---|
| **Zone apex** | Not allowed | Allowed |
| **Points to** | Any hostname | AWS resources only (ALB, NLB, CloudFront, API GW, S3 website, Elastic Beanstalk, Global Accelerator) |
| **Query charges** | Yes | No (Alias queries are free) |
| **TTL** | You set it | AWS manages it |
| **Health checks** | Cannot associate directly | Can associate with Route53 health check |

```
# WRONG — CNAME at apex
example.com.    CNAME   myalb-123.us-east-1.elb.amazonaws.com.   ← NOT ALLOWED

# CORRECT — Alias at apex
example.com.    A       ALIAS myalb-123.us-east-1.elb.amazonaws.com.   ← WORKS
```

---

### 3.8 Routing Policies

| Policy | Use Case | Health Checks | Notes |
|---|---|---|---|
| **Simple** | Single resource, no routing logic | No | Returns all values in random order if multiple |
| **Failover** | Active/passive HA between two endpoints | Yes (required for primary) | Primary + Secondary; routes to secondary when primary fails |
| **Weighted** | A/B testing, gradual migrations, traffic splitting | Optional | Weight 0 stops traffic; all 0 = equal distribution |
| **Latency** | Route to lowest-latency AWS region | Optional | Based on measured AWS network latency to region, not geographic distance |
| **Geolocation** | Route based on user's **geographic location** | Optional | Continent, country, or US state; default record required for unmatched locations |
| **Geoproximity** | Route based on proximity with **bias adjustment** | Optional | Uses Traffic Flow; bias expands/shrinks routing area; requires Traffic Flow |
| **Multi-Value** | Return multiple healthy IPs (up to 8) | Yes | NOT a substitute for a load balancer — DNS-level only |
| **IP-based** | Route based on client IP/CIDR | Optional | You define CIDR blocks mapped to locations |

#### Routing Policy Details

**Weighted:**
```
Record 1: api.example.com  A  1.2.3.4   Weight: 70  (→ sends 70% of traffic)
Record 2: api.example.com  A  5.6.7.8   Weight: 30  (→ sends 30% of traffic)
```
To blue/green switch: set old version weight to 0 (no traffic) while keeping it available for manual testing.

**Failover:**
```
Primary: api.example.com  A  <active-alb>   Type: PRIMARY    Health check: required
Secondary: api.example.com  A  <dr-alb>     Type: SECONDARY  Health check: optional
```
Route53 monitors primary health. When primary fails X consecutive checks → routes to secondary.

**Geolocation vs Geoproximity:**

| | Geolocation | Geoproximity |
|---|---|---|
| **Based on** | User's actual geographic location | Distance between user and resource + bias |
| **Bias** | Cannot adjust | Can expand (+1 to +99) or shrink (-1 to -99) routing area |
| **Use case** | Regulatory compliance (data sovereignty) | Shift traffic gradually between regions |
| **Requires Traffic Flow** | No | Yes |
| **Default record** | Required for unmatched locations | N/A |

**Multi-Value:**
- Returns up to 8 healthy records
- Clients pick one at random (client-side load balancing)
- NOT a replacement for an ELB — DNS caching and lack of connection draining mean it's not suitable for production load balancing
- Health checks make unresponsive endpoints invisible to DNS responses

---

### 3.9 Health Checks

| Type | What It Checks |
|---|---|
| **Endpoint** | HTTP/HTTPS/TCP health check against a specific endpoint |
| **Calculated** | Aggregate health of up to 256 child health checks (AND/OR logic) |
| **CloudWatch Alarm** | Monitors a CloudWatch alarm state (useful for private resources) |

Key settings:
- **Evaluation period:** 3 data points (default) = 30-second TTL before failover
- **Request interval:** 30s (standard) or 10s (fast, higher cost)
- **String matching:** HTTP checks can match response body content (first 5120 bytes)
- **SNI:** HTTPS checks support SNI for virtual-hosted SSL
- **Health checkers:** Use 18+ global health checkers; healthy = majority agreement

**Private resource health checks:**
Route53 health checkers are public — they cannot reach private VPC resources. Solution:
1. Create a CloudWatch metric/alarm for the private resource
2. Create a Route53 health check that monitors the CloudWatch alarm

---

### 3.10 Private Hosted Zones

- DNS zone that resolves only within associated VPCs
- Associate multiple VPCs (same or different accounts) with one private hosted zone
- VPC must have `enableDnsHostnames` and `enableDnsSupport` = true
- For cross-account: enable VPC association authorization in hosting account

```
Private Hosted Zone: internal.example.com
├── db.internal.example.com → 10.0.10.50
├── api.internal.example.com → 10.0.20.100
└── cache.internal.example.com → 10.0.30.25

These names resolve ONLY from associated VPCs.
```

---

### 3.11 Route53 Resolver for Hybrid DNS

| Component | Direction | Use Case |
|---|---|---|
| **Inbound Endpoint** | On-prem → AWS | On-prem resolvers forward queries for AWS-hosted domains to Route53 |
| **Outbound Endpoint** | AWS → On-prem | EC2/Lambda resolves on-prem domain names via forwarding rules |
| **Resolver Rules** | Configuration | Define which domains get forwarded where |

```
On-prem scenario:
EC2 needs to resolve corp.internal → On-prem DNS (192.168.1.53)

Setup:
1. Create Outbound Endpoint in VPC (has ENI with private IP in each AZ)
2. Create Forwarding Rule: corp.internal → 192.168.1.53
3. Associate rule with VPC
4. EC2 queries → Route53 Resolver → Outbound Endpoint → On-prem DNS → Response
```

---

## 4. Key Config & Limits

### ELB Limits

| Parameter | ALB | NLB | GWLB |
|---|---|---|---|
| Listeners per LB | 50 | 50 | 6 |
| Target groups per LB | 100 | 100 | 6 |
| Targets per target group | 1,000 | 1,000 | 300 |
| Rules per listener | 100 | N/A | N/A |
| Conditions per rule | 5 | N/A | N/A |
| Certificates per ALB | 25 (SNI) | 25 (SNI) | N/A |
| Idle timeout (ALB) | 1–4000s (default 60s) | Not configurable | N/A |
| Deregistration delay | 0–3600s (default 300s) | 0–3600s | 0–3600s |

### Route53 Limits

| Parameter | Limit |
|---|---|
| Hosted zones per account | 500 (soft) |
| Records per hosted zone | 10,000 (soft) |
| Health checks per account | 200 (default) |
| Traffic policies per account | 50 |
| Routing policies per record set | 1 |
| Max values in multi-value policy | 8 |

---

## 5. Decision Tree

### Which Load Balancer?

```
What protocol?
├── HTTP / HTTPS / gRPC / WebSocket → ALB
│   └── Need path-based routing, host-based routing, Lambda targets? → ALB
│
├── TCP / UDP / TLS → NLB
│   ├── Need static IP or Elastic IP? → NLB
│   ├── Need to preserve source IP? → NLB
│   ├── Need PrivateLink producer? → NLB (required)
│   └── Need millions RPS / ultra-low latency? → NLB
│
└── Need to route through inline virtual appliances (firewalls, IDS)?
    └── GWLB (with GENEVE encapsulation)
```

### Which Route53 Routing Policy?

```
Single endpoint, no logic needed?
└── Simple

Need failover to backup endpoint?
└── Failover (with health checks on primary)

Need to split traffic by percentage?
└── Weighted (specify weights per record)

Route to lowest latency AWS region?
└── Latency

Route based on user's country/continent?
└── Geolocation (need default record for unmatched)

Route based on proximity + want to fine-tune boundaries?
└── Geoproximity (requires Traffic Flow, use bias)

Return multiple healthy IPs for DNS-level balancing?
└── Multi-Value (not a true LB, client picks)

Route based on client IP CIDR block?
└── IP-based
```

### ALB vs Route53 for Multi-Region?

```
Traffic from multiple global regions?
├── Need intelligent routing (latency, geo, failover)? → Route53 routing policies
│   └── Each region has its own ALB → Route53 points to correct ALB
│
└── Single-region with multiple targets? → ALB routing rules only

Need both? → Route53 latency/failover policy → per-region ALB
```

---

## 6. Common Patterns

### Pattern 1: Multi-Region Active-Active with Route53 Latency

```
User (Tokyo)
     ↓
Route53 Latency Policy
├── us-east-1: ALB → EC2 fleet (weight: latency-based)
├── eu-west-1: ALB → EC2 fleet
└── ap-northeast-1: ALB → EC2 fleet (← Tokyo user routed here)

Each ALB has health checks.
If ap-northeast-1 ALB fails health check → Route53 routes to next lowest latency.
```

### Pattern 2: Blue/Green Deployment with Weighted Records

```
Deploy new version:
1. Deploy "green" stack (new ALB + ASG)
2. Create weighted Route53 record: green=0, blue=100
3. Test green stack via direct ALB URL
4. Shift: green=10, blue=90 → monitor error rates
5. Shift: green=50, blue=50
6. Shift: green=100, blue=0
7. Decommission blue stack

Zero downtime. Instant rollback: flip weights back.
```

### Pattern 3: ALB Path-Based Routing for Microservices

```
ALB Listener Rules:
/api/users/*     → user-service target group    (ECS service)
/api/orders/*    → order-service target group   (ECS service)
/api/products/*  → product-service target group (ECS service)
/static/*        → Forward 301 → CloudFront distribution
/* (default)     → frontend-tg                  (React SPA on EC2)
```

### Pattern 4: NLB + PrivateLink for Cross-Account Service Exposure

```
Account A (Service Provider):
  Service EC2 → NLB (lab-nlb) → PrivateLink Service Endpoint

Account B (Consumer 1):
  Interface VPC Endpoint → connects to lab-nlb via PrivateLink
  DNS: service.vpce-dns → resolves to ENI in Account B's VPC

Account C (Consumer 2):
  Same pattern — no peering needed between B and C
  Service provider never sees consumer network topology
```

### Pattern 5: Geolocation for Data Residency

```
European users → eu-west-1 ALB (data stored in EU S3/RDS)
US users       → us-east-1 ALB (data stored in US)
APAC users     → ap-southeast-1 ALB
Default        → us-east-1 (fallback for unmatched)

NACL/SG + S3 bucket policies enforce this at the data layer.
Route53 Geolocation is the routing layer.
```

---

## 7. Gotchas

### CNAME Cannot Be at Zone Apex
`example.com` (no subdomain) is the zone apex. You CANNOT use a CNAME record here — DNS spec prohibits it. Use an Alias record (Route53 extension) which resolves to the AWS resource's IP(s) transparently and is free.

### NLB Cross-Zone Load Balancing is Off by Default and Charged
ALB has cross-zone LB always on and free. NLB has it **off by default** and charges **$0.01/GB** if enabled. If your targets are unevenly distributed across AZs and cross-zone is off, you'll get uneven load distribution. Enable it explicitly for NLB but be aware of the cost.

### NLB Preserves Source IP — Update Your Security Groups
ALB replaces source IP with its own IP (use `X-Forwarded-For`). NLB passes the original client IP. Your EC2 security groups must allow the client IP ranges, not just the NLB IP range. If you restrict SGs to only the NLB subnet CIDR, traffic will work only when the client is in that CIDR.

### Geolocation ≠ Geoproximity
- **Geolocation**: routes based on actual geographic location (country, continent). Hard boundaries.
- **Geoproximity**: routes based on distance, and you can shift boundaries with bias. Requires Traffic Flow (additional cost ~$50/month per policy).
- Exam trick: "route EU users to EU, US users to US" → Geolocation. "Gradually shift APAC traffic from Singapore to Sydney" → Geoproximity.

### Health Check TTL Must Be Low for Fast Failover
If you're using Route53 Failover policy with a 5-minute TTL, it takes up to 5 minutes (TTL expiry) + health check evaluation time before clients stop using the failed endpoint. Use 60-second TTL or less for fast failover. For critical applications, use 30-second evaluation periods (fast health checks, extra cost).

### ALB Deregistration Delay
Default is 300 seconds (5 minutes). If your deployment pipeline replaces targets frequently, this causes 5 minutes of waiting per deployment. Reduce to 30-60 seconds for faster CI/CD pipelines. Set to 0 to disable drain entirely (risky — in-flight requests will fail).

### ALB WAF Integration is Regional
AWS WAF on ALB is regional. CloudFront + WAF is global. If your ALB is public, users bypass CloudFront WAF by hitting the ALB directly. Use ALB security groups to only allow traffic from CloudFront IP ranges (or AWS-managed prefix list for CloudFront).

### Route53 Health Checks Cannot Reach Private Resources
Route53 health checkers are AWS's global network — they cannot reach your private VPC resources. For private endpoint monitoring: publish a CloudWatch metric → create a CloudWatch alarm → create Route53 health check on that alarm.

### Multi-Value Answer is Not a Load Balancer
Multi-Value returns up to 8 healthy IPs. DNS TTL and client-side caching mean requests are not evenly distributed. There is no connection draining, sticky sessions, or advanced routing. Use ELB for actual load balancing.

---

## 8. Hands-On Lab (Free Tier)

### Objective: ALB with path-based routing + Route53 Alias record

**Estimated cost:** Free tier (EC2 t3.micro) — ALB has no free tier, ~$0.008/hr.

#### Step 1: Launch Two EC2 Instances

Launch 2 t3.micro Amazon Linux 2023 instances in **public subnets** (use your lab VPC or default VPC):

**Instance 1 — "blue" server:**
```bash
#!/bin/bash
# User data script
yum install -y httpd
systemctl start httpd
echo "<h1>Blue Server - /blue path</h1>" > /var/www/html/index.html
mkdir /var/www/html/blue
echo "<h1>Blue Service</h1>" > /var/www/html/blue/index.html
```

**Instance 2 — "green" server:**
```bash
#!/bin/bash
yum install -y httpd
systemctl start httpd
echo "<h1>Green Server - /green path</h1>" > /var/www/html/index.html
mkdir /var/www/html/green
echo "<h1>Green Service</h1>" > /var/www/html/green/index.html
```

SG for both instances: Allow TCP 80 from the ALB security group (create ALB SG first).

#### Step 2: Create Target Groups

**Target Group 1:**
- Name: `blue-tg`
- Target type: Instance
- Protocol: HTTP, Port: 80
- Health check path: `/blue/`
- Register: Instance 1

**Target Group 2:**
- Name: `green-tg`
- Target type: Instance
- Protocol: HTTP, Port: 80
- Health check path: `/green/`
- Register: Instance 2

#### Step 3: Create ALB

1. EC2 Console → Load Balancers → Create → Application Load Balancer
2. Name: `lab-alb`
3. Scheme: Internet-facing
4. Select 2+ AZs and public subnets
5. Security Group: Create new → allow HTTP 80 from 0.0.0.0/0
6. Listener HTTP:80 → Default action: forward to `blue-tg`
7. Create

#### Step 4: Add Path-Based Routing Rules

1. Select `lab-alb` → Listeners tab → HTTP:80 → View/edit rules
2. Add rule (priority 10):
   - Condition: Path is `/blue/*`
   - Action: Forward to `blue-tg`
3. Add rule (priority 20):
   - Condition: Path is `/green/*`
   - Action: Forward to `green-tg`
4. Save

Test:
```bash
curl http://<alb-dns>/blue/       # → Blue Service
curl http://<alb-dns>/green/      # → Green Service
curl http://<alb-dns>/           # → Blue Server (default rule)
```

#### Step 5: Add Weighted Target Group (Canary Test)

1. Edit the default rule
2. Change action to: Forward with weights
   - `blue-tg`: weight 90
   - `green-tg`: weight 10
3. Save

Refresh the root URL multiple times — ~10% should hit the green server.

#### Step 6: Route53 Alias Record (if you own a domain)

1. Route53 → Hosted Zones → your zone
2. Create Record:
   - Name: `lab.yourdomain.com`
   - Type: A
   - Alias: Yes
   - Alias target: your ALB (select from dropdown)
   - Routing policy: Simple
3. Wait for DNS propagation (~1-2 min for low TTL)

```bash
nslookup lab.yourdomain.com
curl http://lab.yourdomain.com/blue/
```

#### Step 7: Route53 Failover (Simulate)

1. Create two Route53 records with Failover routing:
   - Primary: `lab.yourdomain.com` → ALB (with health check)
   - Secondary: `lab.yourdomain.com` → static S3 website ("maintenance page")
2. Stop the EC2 instances to cause the ALB health check to fail
3. Observe Route53 switching to the secondary (S3 website)

#### Cleanup

```
1. Delete Route53 records
2. Delete ALB
3. Delete Target Groups
4. Terminate EC2 instances
```

---

*File last updated for SAP-C02 exam objectives. Always verify current AWS limits in official documentation.*
