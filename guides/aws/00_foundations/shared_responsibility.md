# AWS Shared Responsibility Model

## The Problem
When you move to the cloud, a fundamental question arises: **who is responsible for what?**

In traditional on-premises IT, your team owns everything — physical hardware, network, OS, hypervisor, runtime, application, and data. It's all your problem. In the cloud, AWS takes over some of those layers. But which ones? And what happens when something goes wrong — who is accountable?

Without a clear model, organizations made dangerous assumptions:
- "AWS handles security" → data breaches because S3 buckets were left public
- "We handle everything" → duplicating effort on things AWS already secures
- Compliance auditors unsure what evidence to collect

The Shared Responsibility Model solves this with a clear dividing line.

---

## What AWS Built
A contractual and operational framework that divides security responsibilities between **AWS** (security *of* the cloud) and **the customer** (security *in* the cloud).

---

## How It Works

### The Dividing Line

```
┌─────────────────────────────────────────────────────────┐
│                    CUSTOMER owns                        │
│   Data, Encryption, IAM, OS config, App security,      │
│   Network config (SGs, NACLs), Client-side encryption   │
├─────────────────────────────────────────────────────────┤
│                    AWS owns                             │
│   Hardware, Hypervisor, Physical network, AZ/Region     │
│   infrastructure, Managed service software patches      │
└─────────────────────────────────────────────────────────┘
```

**AWS is responsible for:**
- Physical security of data centers (guards, cameras, access control, biometrics)
- Hardware (servers, networking equipment, storage devices)
- Hypervisor layer (Nitro hypervisor)
- Network infrastructure between AZs and regions
- Managed service software — e.g., RDS database engine patches, Lambda runtime, S3 infrastructure

**Customer is responsible for:**
- Identity and Access Management (IAM): who can access what
- Data: encryption at rest and in transit, data classification
- OS configuration on EC2 (patches, hardening)
- Network controls: Security Groups, NACLs, routing
- Application security: code vulnerabilities, injection, XSS
- Client-side encryption
- What data you put in the cloud and who can see it

---

## How Responsibility Shifts by Service Type

The dividing line **moves up the stack** as you use more managed services.

### IaaS (EC2) — Customer does the most
```
Layer            AWS owns    Customer owns
─────────────────────────────────────────
Physical DC        ✓
Hypervisor         ✓
Network HW         ✓
Guest OS                         ✓  ← you patch Windows/Linux
Runtime/App                      ✓
Data                             ✓
IAM                              ✓
Security Groups                  ✓
```

### PaaS (RDS) — AWS takes more
```
Layer            AWS owns    Customer owns
─────────────────────────────────────────
Physical DC        ✓
Hypervisor         ✓
Network HW         ✓
Guest OS           ✓  ← AWS patches RDS host OS
DB engine patches  ✓  ← AWS patches MySQL/PostgreSQL
DB config                        ✓  ← parameter groups
Data                             ✓
IAM / who connects               ✓
Security Groups                  ✓
```

### SaaS (DynamoDB, Lambda, S3) — AWS takes almost everything
```
Layer            AWS owns    Customer owns
─────────────────────────────────────────
Physical DC        ✓
Hypervisor         ✓
Platform           ✓
Runtime            ✓
Scaling/patching   ✓
Data                             ✓
IAM                              ✓
Bucket/table policies            ✓
Encryption config                ✓
```

---

## Concrete Examples by Service

| Service | AWS Responsible For | Customer Responsible For |
|---|---|---|
| **EC2** | Hardware, hypervisor, network hardware | OS patching, AMI hardening, security groups, app code, IAM |
| **RDS** | Hardware, hypervisor, OS on host, DB engine patches | DB parameter groups, security groups, data encryption, IAM, who can connect |
| **S3** | Storage infrastructure, durability, hardware | Bucket policies, ACLs, encryption config, public access settings, IAM |
| **Lambda** | Runtime environment, underlying infra, scaling | Function code, execution role (IAM), environment variables, dependencies |
| **EKS** | Kubernetes control plane patches | Worker node OS patches, pod security, cluster IAM, network policies |
| **CloudFront** | CDN infrastructure, DDoS mitigation (Shield Standard) | Origin access policies, WAF config, cache behaviors, TLS certificates |
| **VPC** | Underlying network hardware | Subnet design, route tables, NACLs, Security Groups, VPC Flow Logs |

---

## Compliance Implications

The model directly determines what evidence YOU must provide in a compliance audit vs what AWS provides:

- **AWS provides:** SOC 1/2/3 reports, PCI DSS Attestation, ISO 27001, FedRAMP — covering their infrastructure
- **You provide:** Evidence that YOU configured services securely (IAM policies, encryption settings, access logs, patch records for EC2)

Use **AWS Artifact** to download AWS's compliance reports for your auditors.

---

## Decision Tree: Who Is Responsible for This Layer?

```
Is this a managed service (RDS, Lambda, DynamoDB, S3)?
│
├── YES → AWS owns the underlying infrastructure + software patches
│         Customer owns: data, IAM, network controls (SGs), encryption config
│         Ask: "Does the customer configure this?" → Yes = Customer; No = AWS
│
└── NO (EC2, self-managed on VMs) → Customer owns everything above hypervisor
    ├── Hypervisor? → AWS
    ├── Guest OS? → Customer (patch your Linux/Windows)
    ├── App code? → Customer
    └── Data? → Customer (always)

Is it physical hardware?
└── Always AWS

Is it IAM / who can access?
└── Always Customer (you control who has access to your account)

Is it data encryption?
└── Customer configures it; AWS provides the tooling (KMS, SSE options)
    Exception: Some services encrypt by default (DynamoDB, CloudTrail S3) — AWS does it, but customer controls key management
```

---

## Common Patterns

### Pattern 1: Shared Responsibility in a 3-tier web app
```
Layer          AWS Responsibility          Customer Responsibility
─────────────────────────────────────────────────────────────────
CloudFront     CDN infra, Shield Std       Cache rules, WAF config, origin policy
ALB            LB infrastructure           Security groups, listener rules, target health
EC2            Hardware, hypervisor        OS patches, hardening, security groups, app code
RDS            Host OS, DB engine patch    Parameter groups, SGs, backup retention, encryption
S3             Storage infra               Bucket policy, encryption, public access block
IAM            IAM service availability    All IAM policies, users, roles, permissions
VPC            Network hardware            Subnets, route tables, NACLs, SGs
```

### Pattern 2: Security incident — who investigates what?

**Scenario:** Data found exposed in S3 bucket
- AWS investigates: Was the S3 infrastructure compromised? (Almost certainly no)
- Customer investigates: Was the bucket policy misconfigured? Was public access block disabled? Which IAM principal accessed it? (CloudTrail)

---

## Gotchas

1. **RDS gotcha:** AWS patches the DB engine — but YOU are responsible for:
   - Security Groups (who can connect to RDS)
   - Parameter groups (DB configuration)
   - Who has DB credentials
   - Data encryption (you enable it — not auto for all engines)

2. **S3 gotcha:** AWS secures the storage infrastructure with 11 nines durability — but the #1 cause of data breaches is **customer misconfigured bucket policies**. Public access block is your responsibility.

3. **Lambda gotcha:** AWS manages the execution environment and runtime patching — but **your function code** vulnerabilities (SQL injection in Lambda handler, overly permissive execution role) are entirely your problem.

4. **"AWS is secure" misconception:** AWS's security certifications (ISO, SOC, PCI) cover their infrastructure. They do NOT certify YOUR configuration. You can have a perfectly secure AWS data center with completely misconfigured S3 buckets.

5. **Shared ≠ 50/50:** The split is asymmetric. As you move from EC2 → RDS → DynamoDB → SaaS, AWS takes more. But **data and IAM are always the customer's responsibility.**

---

## Hands-On Lab (Free Tier)

**Goal:** Identify your responsibilities in practice.

```bash
# 1. Check for public S3 buckets in your account (your responsibility)
aws s3api list-buckets --query 'Buckets[*].Name' --output text | \
  xargs -I {} aws s3api get-bucket-acl --bucket {}

# 2. Check for EC2 instances with public IPs (your responsibility to know)
aws ec2 describe-instances \
  --query 'Reservations[*].Instances[*].[InstanceId,PublicIpAddress,State.Name]' \
  --output table

# 3. Check for security groups with 0.0.0.0/0 on port 22 (your responsibility)
aws ec2 describe-security-groups \
  --filters "Name=ip-permission.from-port,Values=22" \
            "Name=ip-permission.cidr,Values=0.0.0.0/0" \
  --query 'SecurityGroups[*].[GroupId,GroupName]' \
  --output table

# 4. Check if CloudTrail is enabled (your responsibility)
aws cloudtrail describe-trails --output table

# 5. Download AWS compliance reports
# Console → AWS Artifact → Reports → download SOC 2 or PCI DSS report
```

**Reflection exercise:**
For each service you're running, create a table with two columns: "AWS owns" and "I own." This is exactly what a compliance auditor will ask for.
