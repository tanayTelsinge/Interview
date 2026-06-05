# Network Security — WAF, Shield, Network Firewall — SAP-C02 Deep Dive

---

## 1. The Problem

Internet-facing applications face a spectrum of attacks:

- **Layer 3/4 volumetric attacks (DDoS):** UDP floods, SYN floods, reflection attacks that overwhelm network infrastructure
- **Layer 7 application attacks:** SQL injection, XSS, credential stuffing, bad bots, web scraping
- **East-west threats inside VPC:** Malware lateral movement, compromised workload communicating outbound to C2 servers
- **Third-party appliance integration:** Existing investment in Palo Alto, Check Point, or Fortinet firewalls that must remain inline

No single service covers all these attack vectors. AWS provides a layered defense-in-depth stack.

---

## 2. What AWS Built

| Service | Layer | Protects Against | Scope |
|---|---|---|---|
| **Shield Standard** | L3/L4 | Volumetric DDoS (automatic) | All AWS resources, free |
| **Shield Advanced** | L3/L4/L7 | Advanced DDoS + L7 with WAF | Specific resource types, $3K/month |
| **WAF** | L7 (HTTP/S) | OWASP, bots, rate abuse, SQLi, XSS | CloudFront/ALB/API GW/AppSync |
| **Network Firewall** | L3/L4/L7 | VPC-level stateful/stateless, IDS/IPS | VPC traffic (all protocols) |
| **GWLB + Appliances** | L3–L7 | Third-party inline inspection | VPC traffic via appliance |
| **Firewall Manager** | Management | Centralized policy enforcement | AWS Organization-wide |
| **Security Groups** | L3/L4 | Instance-level stateful rules | ENI-level |
| **NACLs** | L3/L4 | Subnet-level stateless rules | Subnet-level |

---

## 3. AWS WAF

### What It Inspects

WAF operates at **Layer 7 (HTTP/HTTPS)** — it can inspect:
- HTTP method, URI, query strings, headers, cookies, body (first 8 KB)
- IP addresses, country of origin
- Request rate per IP

### Supported Attachment Points

```
Internet
   |
CloudFront Distribution ← WAF Web ACL
   |
ALB (Application Load Balancer) ← WAF Web ACL
   |
API Gateway (REST) ← WAF Web ACL
   |
AppSync GraphQL ← WAF Web ACL
   |
Cognito User Pool ← WAF Web ACL
```

**Critical:** WAF at CloudFront = global (us-east-1 Web ACL); WAF at ALB/API GW = regional.

### Web ACL Rule Types

| Rule Type | Description | Example |
|---|---|---|
| **AWS Managed Rules** | Pre-built rule groups by AWS | AWSManagedRulesCommonRuleSet (OWASP Top 10) |
| **IP Set Rule** | Allow/block specific IPs or CIDRs | Block competitor scrapers |
| **Geo Match Rule** | Allow/block countries | Block all except US, EU |
| **Rate-based Rule** | Limit requests per IP per 5 minutes | Block IP after 1,000 req/5min |
| **Regex Pattern Set** | Match URI/header/body against regex | Block `/admin/` from external |
| **Label Match** | Match on labels set by other rules | Chain rules conditionally |

### AWS Managed Rule Groups

| Group | Protects Against |
|---|---|
| `AWSManagedRulesCommonRuleSet` | OWASP Top 10 (SQLi, XSS, LFI, RFI) |
| `AWSManagedRulesBotControlRuleSet` | Bot traffic detection and mitigation |
| `AWSManagedRulesATPRuleSet` | Account takeover (credential stuffing) |
| `AWSManagedRulesACFPRuleSet` | Account creation fraud prevention |
| `AWSManagedRulesKnownBadInputsRuleSet` | Log4Shell, Spring4Shell, SSRF attempts |
| `AWSManagedRulesAmazonIpReputationList` | Known malicious IPs (botnets, TOR) |
| `AWSManagedRulesSQLiRuleSet` | SQL injection specifically |

### Rule Actions

| Action | Effect |
|---|---|
| **Allow** | Forward request |
| **Block** | Return 403 Forbidden |
| **Count** | Log match only (no block) — use for testing |
| **CAPTCHA** | Present CAPTCHA challenge |
| **Challenge** | Silent JS browser challenge (bot detection) |

### WAF Logging

```
WAF Web ACL
    |
    └── Logs → S3 bucket (Kinesis Firehose → S3)
             OR CloudWatch Logs
             OR Kinesis Firehose (for real-time analysis)

Log content: timestamp, IP, country, URI, action, rule matched, headers
```

**WAF Capacity Units (WCU):** Each rule consumes WCU. Web ACL max: 1,500 WCU.

---

## 4. AWS Shield

### Shield Standard

- **Free, automatic, always-on**
- Protects against: SYN/UDP floods, reflection attacks, Layer 3/4 volumetric DDoS
- Applied automatically to all AWS resources
- No configuration required

### Shield Advanced

| Feature | Value |
|---|---|
| Cost | **$3,000/month/organization** (one fee covers org) |
| L7 protection | Yes — automatic WAF rule creation during DDoS |
| DDoS Response Team (DRT) | 24/7 access for active attacks |
| Cost protection | AWS credits if scaling due to DDoS attack |
| Enhanced visibility | CloudWatch metrics, attack diagnostics |
| Protected resource types | ALB, CLB, NLB, Elastic IP, CloudFront, Route 53, Global Accelerator |

**Supported resource types (exam critical):**
- Application Load Balancer
- Classic Load Balancer
- Network Load Balancer
- Elastic IP addresses (EC2, NAT Gateway)
- Amazon CloudFront distributions
- Amazon Route 53 hosted zones
- AWS Global Accelerator

**NOT supported:** Direct protection of EC2 instances (protect via EIP or ALB in front).

### Shield Advanced L7 Protection

```
DDoS attack detected at L7 (e.g., HTTP flood)
        |
Shield Advanced + WAF (if both enabled)
        |
Automatic rule creation in WAF Web ACL to block attack pattern
        |
DRT can manually tune rules during attack (if you grant IAM access)
```

---

## 5. AWS Network Firewall

### What It Does

A **stateful, managed network firewall** deployed inside your VPC. Inspects all traffic flowing through designated subnets at layers 3–7.

### Architecture — Requires Route Table Changes

```
Internet Gateway
       |
  Public Subnet (Web Tier)
       |
  [Route table: 0.0.0.0/0 → Network Firewall endpoint]
       |
  Network Firewall Endpoint (dedicated subnet)
       |
  [Route table: 0.0.0.0/0 → IGW, VPC-local → VPC CIDR]
       |
  Private Subnet (App/DB Tier)
```

**Critical:** Network Firewall requires:
1. A **dedicated subnet** per AZ for the firewall endpoint
2. **Route table modifications** to send traffic through the firewall endpoint
3. If multiple AZs, one firewall endpoint per AZ (traffic cannot cross AZs in firewall)

### Rule Types

| Rule Type | What It Does |
|---|---|
| **Stateless rules** | Simple packet filtering (5-tuple: src IP/port, dst IP/port, protocol). Evaluated first. No connection tracking. |
| **Stateful rules** | Connection-aware inspection. Suricata-compatible IDS/IPS rules. |
| **Domain list** | Allow/block outbound DNS queries by domain (*.malicious.com) |
| **Suricata IDS/IPS rules** | Full Suricata rule syntax for deep packet inspection |

### Suricata Rule Example

```
# Block outbound C2 communication
drop tcp $HOME_NET any -> $EXTERNAL_NET [4444,8080,8443] \
  (msg:"Possible C2 callback"; flow:established,to_server; \
   content:"cmd.exe"; nocase; sid:1000001;)

# Alert on SQL injection attempt
alert http $EXTERNAL_NET any -> $HTTP_SERVERS $HTTP_PORTS \
  (msg:"SQL injection attempt"; content:"' OR '1'='1"; sid:1000002;)
```

### Network Firewall vs Security Group vs NACL

| Feature | Security Group | NACL | Network Firewall |
|---|---|---|---|
| Level | ENI (instance) | Subnet | VPC-wide traffic |
| State | **Stateful** | **Stateless** | Both (stateful + stateless) |
| Protocol support | L4 | L4 | L3–L7 |
| Rules | Allow only | Allow + Deny | Allow + Deny + IDS/IPS |
| Deep inspection | No | No | Yes (payload inspection) |
| Domain filtering | No | No | Yes |
| Cost | Free | Free | ~$0.395/endpoint/hr + data |
| Route changes | No | No | Required |

---

## 6. Security Group vs NACL Deep Comparison

| Feature | Security Group | NACL |
|---|---|---|
| **Applies to** | EC2 instances / ENIs | Subnets |
| **State** | **Stateful** — return traffic auto-allowed | **Stateless** — return traffic needs explicit rule |
| **Rule types** | Allow only (implicit deny) | Allow AND Deny |
| **Evaluation** | All rules evaluated before decision | Rules evaluated in **number order** (lowest first); first match wins |
| **Scope** | Same subnet or cross-subnet within VPC | Only affects traffic entering/leaving subnet |
| **Changes apply** | Immediately | Immediately |
| **Default behavior** | Default SG: all inbound denied, outbound allowed | Default NACL: all traffic allowed; custom NACL: all denied |
| **Source/Dest** | IP, CIDR, another SG, prefix list | IP and CIDR only |
| **Ephemeral ports** | Auto-handled (stateful) | **Must explicitly allow 1024–65535** inbound for return traffic |

### NACL Ephemeral Port Trap

```
Client (on internet) → ALB → EC2 in private subnet
                                  |
NACL on EC2 subnet must allow:
  Inbound: TCP 80/443 from 0.0.0.0/0 (web traffic)
  Outbound: TCP 1024-65535 to 0.0.0.0/0 (EPHEMERAL PORTS for return traffic)

Without the outbound ephemeral rule:
  Request arrives ✓
  Response blocked by NACL ✗ (NACL is stateless!)
```

---

## 7. AWS Firewall Manager

### What It Does

Centrally manage and enforce security policies across all accounts in an AWS Organization.

```
AWS Organizations (Management Account)
        |
  Firewall Manager (Security account)
        |
  Policies deployed to member accounts:
  ├── WAF Web ACLs (auto-attach to new ALBs/CloudFront)
  ├── Shield Advanced protections
  ├── Security Group policies (required SGs, unused SG cleanup)
  ├── Network Firewall policies
  └── Route 53 Resolver DNS Firewall
```

**Prerequisites:**
- AWS Organizations with all features enabled
- Firewall Manager administrator account designated
- AWS Config enabled in all member accounts

---

## 8. GWLB — Gateway Load Balancer for Third-Party Appliances

### Architecture

```
Traffic enters VPC
        |
  Route table → GWLB Endpoint
        |
  GWLB (Layer 3 transparent, GENEVE encapsulation)
        |
  Appliance Fleet (Palo Alto / Check Point / Fortinet)
  (auto-scaled ASG in inspection VPC)
        |
  GWLB (return traffic)
        |
  Back to original path → destination
```

**Key point:** GWLB is transparent at Layer 3 — original source/destination IPs preserved via GENEVE tunnel.
Use with **VPC endpoint service** to share appliance fleet across accounts.

---

## 9. Key Config & Limits

| Parameter | Value |
|---|---|
| WAF Web ACL max WCU | 1,500 |
| WAF rules per Web ACL | 100 |
| WAF IP set max CIDRs | 10,000 |
| WAF rate-based rule evaluation window | 1–5 minutes |
| WAF body inspection size | First **8 KB** |
| Shield Advanced cost | **$3,000/month** per organization |
| Shield Standard | **Free** |
| Network Firewall endpoint cost | ~$0.395/endpoint/hr |
| Network Firewall data processing | ~$0.065/GB |
| SG max inbound/outbound rules | 60 each (default), up to 1,000 |
| NACLs per VPC | 200 |
| NACL rules per NACL | 20 (default), up to 40 |

---

## 10. Decision Tree

### Which Service for Which Threat?

```
What is the threat?

├── Volumetric DDoS (network floods, SYN floods)?
│   ├── Automatic free protection → Shield Standard (always on)
│   └── Need advanced visibility/DRT/cost protection → Shield Advanced

├── Layer 7 attacks (SQLi, XSS, bot abuse, rate abuse)?
│   └── WAF (Web ACL on CloudFront/ALB/API Gateway)

├── VPC-level east-west lateral movement?
│   └── Network Firewall (stateful IDS/IPS inside VPC)

├── Outbound exfiltration / C2 callbacks?
│   └── Network Firewall (domain filtering + Suricata rules)

├── Simple allow/deny at subnet boundary?
│   └── NACL (remember: stateless, number-order evaluation)

├── Instance/ENI-level access control?
│   └── Security Group

├── Third-party appliance (Palo Alto/Fortinet) inline?
│   └── GWLB + appliance fleet

├── Centralize policies across org?
│   └── Firewall Manager
```

---

## 11. Common Patterns

### Pattern 1: Multi-Layer Internet-Facing Defense

```
Internet
    |
Route 53 (DDoS resilient DNS, Shield Standard)
    |
CloudFront + WAF Web ACL
  ├── AWS Managed Rules (OWASP, Bot Control)
  ├── Rate-based rule: 2000 req/5min per IP
  ├── Geo block: CN, RU (if applicable)
  └── ACM certificate (free TLS)
    |
ALB + WAF Web ACL (defense in depth, different rules)
    |
ECS/EC2 (Security Group: only ALB SG allowed)
    |
RDS (Security Group: only app tier SG allowed, no internet)
```

### Pattern 2: Network Firewall for Egress Control

```
Private EC2 instances
    |
Route: 0.0.0.0/0 → Network Firewall endpoint
    |
Network Firewall
  ├── Domain list: allow *.amazonaws.com, *.ubuntu.com
  ├── Domain list: block *.suspicious-domain.com
  └── Suricata rule: alert on crypto mining ports
    |
NAT Gateway → Internet (only allowed domains)
```

### Pattern 3: Shield Advanced + WAF Coordinated Defense

```
CloudFront + Shield Advanced + WAF
    |
DDoS detected (L7 HTTP flood from 500 IPs)
    |
Shield Advanced → automatically creates WAF rate-based rule
    + notifies you via CloudWatch alarm
    |
Optional: DRT logs in → reviews → creates custom WAF rules
    |
Attack traffic blocked at CloudFront edge (before reaching origin)
```

### Pattern 4: Firewall Manager Organization-Wide WAF

```
Security Account (Firewall Manager admin)
    |
Create policy: "All ALBs must have WAF Web ACL with CommonRuleSet"
    |
New ALB created in any member account
    |
Firewall Manager automatically attaches the Web ACL
(non-compliant resource becomes compliant automatically)
```

---

## 12. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **Shield Advanced only specific resource types** | ALB, NLB, CLB, EIP, CloudFront, Route 53, Global Accelerator. NOT direct EC2 |
| **Network Firewall needs dedicated subnet** | And requires route table changes. Not plug-and-play |
| **WAF doesn't stop volumetric DDoS** | WAF is L7; volumetric DDoS is L3/L4 → use Shield Standard/Advanced |
| **NACL rules in number order** | Rule 100 evaluated before rule 200. First match wins — different from SG (all rules evaluated) |
| **Ephemeral ports in NACLs** | NACL stateless: must explicitly allow outbound 1024–65535 for return traffic |
| **WAF CloudFront requires us-east-1** | Web ACL for CloudFront must be created in us-east-1 (global service) |
| **WAF body inspection only first 8 KB** | Large request bodies partially inspected only |
| **Network Firewall cross-AZ** | Traffic can't cross AZs within the firewall — one endpoint per AZ required |
| **GWLB GENEVE encapsulation** | Appliances must support GENEVE protocol (not traditional VXLAN) |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **WAF blocks legitimate traffic** | Start with Count mode, analyze logs 1-2 weeks, then switch to Block |
| **NACL ephemeral port blocking** | Always allow outbound 1024-65535 from private subnets |
| **Security group sprawl** | Use SG referencing (allow from SG instead of CIDR); audit with IAM Access Analyzer |
| **Shield Advanced over-counting** | One $3K/month bill per org regardless of resources protected — worth it for large fleets |
| **Network Firewall route tables wrong** | Missing return route or AZ mismatch silently drops traffic or causes asymmetric routing |

---

## 13. Hands-On Lab (Free Tier)

### Goal: Create WAF Web ACL + attach to ALB + test IP block + rate limiting

**Step 1 — Create WAF Web ACL**

Console: WAF & Shield → Web ACLs → Create web ACL
- Region: your region (NOT us-east-1 unless for CloudFront)
- Name: `my-app-web-acl`
- Default action: Allow

**Step 2 — Add Rules**

Add rule → Add managed rule group:
- AWS managed rules → AWSManagedRulesCommonRuleSet (700 WCU) → Override to Count (for now)
- AWS managed rules → AWSManagedRulesAmazonIpReputationList (25 WCU)

Add rule → Rule builder → Rate-based rule:
- Name: `RateLimitRule`
- Rate limit: 1000 requests / 5 minutes
- Scope of inspection: Source IP address only
- Action: Block

**Step 3 — Add IP Block Rule**

```bash
# Create IP set
aws wafv2 create-ip-set \
  --name blocked-ips \
  --scope REGIONAL \
  --ip-address-version IPV4 \
  --addresses "198.51.100.0/24"

# Note the IPSetId and IPSetArn
```

Console: Add rule → IP set rule → Select `blocked-ips` → Action: Block

**Step 4 — Attach to ALB**

Console: Web ACL → Associated AWS resources → Add ALB → Select your ALB.

Or via CLI:
```bash
ALB_ARN=$(aws elbv2 describe-load-balancers --names my-alb \
  --query 'LoadBalancers[0].LoadBalancerArn' --output text)

aws wafv2 associate-web-acl \
  --web-acl-arn arn:aws:wafv2:REGION:ACCOUNT:regional/webacl/my-app-web-acl/ID \
  --resource-arn $ALB_ARN
```

**Step 5 — Enable Logging**

```bash
# Create S3 bucket for WAF logs (must start with aws-waf-logs-)
aws s3 mb s3://aws-waf-logs-myapp-ACCOUNT

aws wafv2 put-logging-configuration \
  --logging-configuration '{
    "ResourceArn": "arn:aws:wafv2:REGION:ACCOUNT:regional/webacl/my-app-web-acl/ID",
    "LogDestinationConfigs": ["arn:aws:s3:::aws-waf-logs-myapp-ACCOUNT"]
  }'
```

**Step 6 — Test Rules (Count Mode)**

```bash
# Test from your IP - should be counted but not blocked (Count mode)
for i in {1..5}; do
  curl -I https://your-alb-domain.com/ \
    -H "User-Agent: sqlmap" 2>/dev/null | head -1
done

# Check WAF sampled requests (console) or logs in S3
```

**Step 7 — Check Security Group / NACL comparison**

```bash
# List security group rules
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=my-app-sg" \
  --query 'SecurityGroups[0].IpPermissions'

# List NACL rules
aws ec2 describe-network-acls \
  --filters "Name=association.subnet-id,Values=subnet-XXXXX" \
  --query 'NetworkAcls[0].Entries'
```

**Cleanup:**

```bash
aws wafv2 disassociate-web-acl --resource-arn $ALB_ARN
aws wafv2 delete-web-acl --name my-app-web-acl --scope REGIONAL --id WEB-ACL-ID --lock-token LOCK-TOKEN
aws s3 rb s3://aws-waf-logs-myapp-ACCOUNT --force
```
