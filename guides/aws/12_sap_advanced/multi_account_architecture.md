# SAP — Multi-Account Architecture at Scale

## 1. The Problem

A growing enterprise reaches 100+ AWS accounts. Manual account management doesn't scale:

- Creating a new account takes a week of tickets: billing setup, security baseline, CloudTrail, VPC, IAM password policy, default VPC deletion, SCPs
- Each team has a different interpretation of "secure baseline" — inconsistency creates compliance gaps
- Security teams can't see what's happening across 200 accounts without a centralized view
- Network topology becomes ad-hoc — some accounts peer directly, others route through NAT, no consistent model
- Cost attribution is impossible: which account belongs to which team/project/environment?

At scale, **everything must be automated**: account creation, baseline application, security monitoring, network connectivity, and cost management.

---

## 2. What AWS Built

No single service solves this — it's an architecture:

| Layer | Services |
|-------|---------|
| Account Vending | Control Tower + Account Factory / AFT / Organizations API |
| Baseline Automation | EventBridge + Lambda + CloudFormation StackSets |
| Security | Security Hub + GuardDuty + Macie + Config + CloudTrail (all with delegated admin) |
| Networking | Transit Gateway + VPC sharing + Route53 Resolver + RAM |
| Cost | Consolidated billing + Savings Plans (org-level) + Tag Policies + Budgets |
| Identity | IAM Identity Center + SCIM + ABAC |

---

## 3. How It Works

### Account Vending Machine Patterns

**Option 1: Control Tower + Account Factory (UI/API)**
```
Self-service via Service Catalog:
  1. Developer fills form: account name, email, OU, tags
  2. Service Catalog product creates the account
  3. Control Tower applies: OU enrollment + all guardrails
  4. Baseline applied: Control Tower provisions Audit + Log Archive integration
  5. Account ready in ~20 minutes

Programmatic via API:
  aws servicecatalog provision-product \
    --product-name "AWS Control Tower Account Factory" \
    --provisioning-artifact-name "latest" \
    --provisioned-product-name "payments-prod" \
    --provisioning-parameters file://account-params.json
```

**Option 2: Account Factory for Terraform (AFT)**
```
GitOps pipeline for account vending:

  Developer PR to account-request repo:
  /account-requests/payments-prod.tf
  ─────────────────────────────────
  module "payments_prod" {
    source = "./modules/account-request"
    account_name   = "payments-prod"
    account_email  = "payments-prod@company.com"
    ou             = "Workloads/Production"
    tags = {
      Team        = "payments"
      Environment = "prod"
      CostCenter  = "CC-1234"
    }
  }

  PR approval → CodePipeline triggers:
    Step 1: Organizations API → create account
    Step 2: Apply account customizations (Terraform)
    Step 3: Apply global customizations (org-wide)
    Step 4: Apply OU customizations (Workloads/Production)
    Step 5: Apply account-specific customizations
    → Account ready with all baselines applied
```

**Option 3: Custom Organizations API + StackSets**
```
EventBridge catches "CreateAccountResult" event from Organizations API
→ Lambda runs account baseline:
   1. AssumeRole into new account
   2. Delete default VPCs (all regions)
   3. Enable Config (all regions)
   4. Enable GuardDuty (delegate to security account)
   5. Enable Security Hub (delegate to security account)
   6. Set IAM password policy
   7. Create mandatory CloudWatch alarms (root login)
   8. Apply organization SCPs

StackSets deployed by management account:
  → Create Config rules in all accounts
  → Create CloudTrail in all accounts
  → Create IAM roles for cross-account access
```

---

### Account Baseline Automation

**EventBridge-driven baseline Lambda:**
```python
import boto3

def lambda_handler(event, context):
    # Triggered by: Organizations CreateAccount success event
    account_id = event['detail']['serviceEventDetails']['createAccountStatus']['accountId']
    account_name = event['detail']['serviceEventDetails']['createAccountStatus']['accountName']

    # Assume role into new account
    sts = boto3.client('sts')
    creds = sts.assume_role(
        RoleArn=f"arn:aws:iam::{account_id}:role/OrganizationAccountAccessRole",
        RoleSession=f"baseline-{account_id}"
    )['Credentials']

    session = boto3.Session(
        aws_access_key_id=creds['AccessKeyId'],
        aws_secret_access_key=creds['SecretAccessKey'],
        aws_session_token=creds['SessionToken']
    )

    # Delete default VPC in all regions
    ec2 = session.client('ec2', region_name='us-east-1')
    vpcs = ec2.describe_vpcs(Filters=[{'Name':'isDefault','Values':['true']}])
    for vpc in vpcs['Vpcs']:
        delete_default_vpc(ec2, vpc['VpcId'])

    # Enable Config
    enable_config(session, account_id)

    # Set IAM password policy
    iam = session.client('iam')
    iam.update_account_password_policy(
        MinimumPasswordLength=14,
        RequireUppercaseCharacters=True,
        RequireLowercaseCharacters=True,
        RequireNumbers=True,
        RequireSymbols=True,
        MaxPasswordAge=90,
        PasswordReusePrevention=24
    )

    print(f"Baseline applied to account {account_id} ({account_name})")
```

**CloudFormation StackSets for Cross-Account Resources:**
```yaml
# StackSet deployed from management account to all accounts in Workloads OU
# Creates mandatory Config rules in every account/region

Resources:
  EncryptedVolumesRule:
    Type: AWS::Config::ConfigRule
    Properties:
      ConfigRuleName: encrypted-volumes
      Source:
        Owner: AWS
        SourceIdentifier: ENCRYPTED_VOLUMES

  RestrictedSSHRule:
    Type: AWS::Config::ConfigRule
    Properties:
      ConfigRuleName: restricted-ssh
      Source:
        Owner: AWS
        SourceIdentifier: INCOMING_SSH_DISABLED

  RequiredTagsRule:
    Type: AWS::Config::ConfigRule
    Properties:
      ConfigRuleName: required-tags
      Source:
        Owner: AWS
        SourceIdentifier: REQUIRED_TAGS
      InputParameters:
        tag1Key: Environment
        tag2Key: Team
        tag3Key: CostCenter
```

---

### Centralized Security Architecture

```
Security OU
  ├── Audit Account
  │     ├── Config Aggregator (all accounts, all regions)
  │     ├── Security Hub Aggregator (all findings)
  │     ├── GuardDuty Administrator (all member accounts enrolled)
  │     └── Inspector (delegated admin)
  │
  └── Log Archive Account
        ├── S3: CloudTrail logs (from ALL accounts via org trail)
        ├── S3: Config history/snapshots (from ALL accounts)
        ├── S3: VPC Flow Logs (from ALL accounts)
        └── S3 Object Lock: WORM protection on all log buckets
```

**CloudTrail Organization Trail:**
```
Management account creates org trail:
  - Multi-region: Yes
  - All accounts: Yes (organization trail)
  - Target S3: log-archive-account/cloudtrail/
  - Management events: All
  - S3 bucket policy: allows CloudTrail service from all org accounts

Result: Every API call from every account → centralized S3
Bucket policy in Log Archive prevents deletion:
  - S3 Object Lock (governance mode): 7 years
  - Only Log Archive account owner + Management can break lock (compliance mode = no one)
```

**GuardDuty Organization Setup:**
```
Management account → GuardDuty console:
  1. Designate delegated administrator: Security/Audit account
  2. Auto-enable GuardDuty for new accounts: Yes
  3. Data sources: VPC Flow Logs + DNS Logs + CloudTrail + S3 Protection + Malware Protection

Audit account:
  → Sees findings from ALL member accounts
  → Suppression rules: reduce noise (known IPs, scanner traffic)
  → EventBridge: high-severity finding → PagerDuty/Slack
  → Automated remediation: isolate compromised instance (remove from SG)
```

**Security Hub Aggregation:**
```
Audit account = aggregation region (us-east-1)
All member accounts: auto-enabled via Organizations

Findings flow into Security Hub from:
  GuardDuty    → threat detections
  Config       → compliance violations
  Inspector    → EC2/container/Lambda vulnerabilities
  Macie        → S3 data classification findings
  IAM Access Analyzer → overly permissive policies
  Firewall Manager → WAF/SG policy violations

Security Hub scores each account with Security Score (0-100%)
ASFF (Amazon Security Finding Format) normalizes all findings
```

---

### Centralized Networking — Hub-and-Spoke

```
Networking Account (hub):
  ├── Transit Gateway (TGW) — central router
  │     ├── TGW Route Table: Production (isolated from Dev)
  │     ├── TGW Route Table: Development (isolated from Prod)
  │     └── TGW Route Table: Shared Services (accessible from all)
  │
  ├── Egress VPC
  │     ├── NAT Gateway per AZ → internet egress for all spoke VPCs
  │     └── TGW attachment
  │
  ├── Ingress VPC
  │     ├── ALB / API Gateway → internet ingress
  │     ├── WAF
  │     └── TGW attachment
  │
  └── VPN/DX connections → on-premises

Spoke Accounts (connected via TGW):
  PaymentsApp-Prod:
    └── App VPC → TGW attachment → TGW route table: Production
  InventoryApp-Prod:
    └── App VPC → TGW attachment → TGW route table: Production
  Dev accounts:
    └── Dev VPCs → TGW attachment → TGW route table: Development
```

**TGW Route Table Segmentation:**
```
Production Route Table:
  10.0.0.0/8 → TGW attachment (all VPCs, allows intra-prod communication)
  0.0.0.0/0  → Egress VPC attachment (internet through centralized NAT)

Development Route Table:
  10.0.0.0/8 → TGW attachment (all dev VPCs)
  0.0.0.0/0  → Egress VPC attachment

Prod and Dev are in different route tables:
  → Production traffic can't reach Development VPCs and vice versa
  → Both can reach Shared Services and Egress
```

**Centralized DNS with Route53 Resolver:**
```
SharedServices VPC:
  Route53 Inbound Resolver endpoint (receives DNS queries from on-prem)
  Route53 Outbound Resolver endpoint (forwards queries to on-prem DNS)

Route53 Resolver Rules (shared via RAM to all accounts):
  "payments.internal" → on-premises DNS server IP
  "*.corp.company.com" → on-premises DNS
  All other → Route53 (AWS public/private DNS)

Each spoke VPC configures Resolver rules (received via RAM sharing)
→ All internal DNS resolution works consistently across accounts
```

**VPC Sharing vs TGW Tradeoffs:**
| | VPC Sharing (RAM) | Transit Gateway |
|-|------------------|----------------|
| Architecture | Single VPC, multiple accounts | Separate VPCs, routed |
| Blast radius | Higher (same VPC) | Lower (separate VPCs) |
| Cost | Lower (no TGW per-attachment cost) | Higher ($0.05/attachment-hr + $0.02/GB) |
| Network isolation | Limited (same subnet is shared) | Strong (separate VPCs, route table control) |
| Latency | Lower (same VPC, no router hop) | Slightly higher (TGW hop) |
| Use case | Dev environments, cost sensitivity | Production, strict isolation requirements |

---

### Cost Management at Scale

**Consolidated Billing:**
```
All accounts pay through management account
Volume discount tiers (S3, data transfer) computed across ALL accounts combined

Example:
  Account A: 50 TB S3 data → $0.023/GB tier
  Account B: 40 TB S3 data → $0.023/GB tier
  Combined: 90 TB → reaches $0.022/GB tier → ALL accounts benefit

RI/Savings Plans sharing:
  Compute Savings Plans purchased in any account
  → Automatically shared across ALL accounts in organization
  → AWS applies discounts where most beneficial (highest On-Demand usage)
```

**Tag Policies:**
```
Tag Policy (Organizations): enforce tag standardization

Example policy (enforces "Environment" tag has specific values):
{
  "tags": {
    "Environment": {
      "tag_key": {
        "@@assign": "Environment"
      },
      "tag_value": {
        "@@assign": ["prod", "staging", "dev", "sandbox"]
      },
      "enforced_for": {
        "@@assign": ["ec2:instance", "rds:db", "s3:bucket"]
      }
    }
  }
}

Non-compliant resources flagged in Tag Policies compliance view
Combine with Config "required-tags" rule for enforcement
```

**Budget Actions for Cost Control:**
```
Scenario: Each team gets a monthly budget. If exceeded:
  1. AWS Budgets alerts at 80% → Slack notification
  2. AWS Budgets alerts at 100% → Apply SCP via Budget Action
     SCP blocks: new EC2 launches, new RDS instances
  3. AWS Budgets alerts at 120% → Stop existing EC2 instances

Budget Action types:
  Apply IAM policy (restrict permissions)
  Apply SCP (org-level restriction)
  Target EC2/RDS instances (stop them)
```

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| TGW attachments per VPC | 5 maximum |
| TGW route tables | No hard limit (thousands) |
| TGW route entries per table | 10,000 |
| VPC CIDR blocks per VPC | 5 (IPv4) |
| StackSets — max accounts per operation | 1,500 |
| StackSets — max concurrent deployments | Configurable (rate limit) |
| Config aggregator — max accounts | No limit |
| RAM shared resources | Stay in owner account |
| GuardDuty — threat intel refresh | Near real-time |
| Security Hub — finding retention | 90 days |
| AFT — account creation time | 20-45 minutes |

---

## 5. Decision Tree

### Hub-and-Spoke Topology Choices
```
How many accounts/VPCs need connectivity?
│
├─ < 5 VPCs, low cost priority?
│   └─ VPC Peering (no TGW cost, simple)
│
├─ 5-50 VPCs, need segmentation?
│   └─ Transit Gateway (centralized routing, route table segmentation)
│
└─ Cost-sensitive dev environment?
    └─ VPC Sharing via RAM (multiple accounts, one VPC)
```

### Centralized vs Distributed Security
```
Always centralize: CloudTrail (org trail → Log Archive)
                   GuardDuty (delegated admin in Security account)
                   Security Hub (aggregation region)

Distribute but aggregate: Config (each account's Config aggregates to Audit)
                           VPC Flow Logs (per account → S3 → centralized SIEM)
```

### When to Use StackSets vs AFT Customizations
```
StackSets:
  - Ongoing compliance (Config rules deployed to all accounts)
  - New service availability (IAM roles for a new service)
  - Applied to existing accounts immediately
  - Use: CloudFormation StackSets with service-managed permissions

AFT Customizations:
  - Account-creation-time baseline (things applied once when account is new)
  - Account-specific configuration
  - Terraform-native teams
```

---

## 6. Common Patterns

### Pattern 1: Account Vending Machine (Complete Flow)
```
Developer → Jira ticket: "Need production account for payments microservice"

Approval → Merge to account-requests/ repo

AFT Pipeline (CodePipeline):
  Stage 1: Account Creation
    → Organizations CreateAccount API
    → Email: payments-prod@company.com
    → OU: Workloads/Production

  Stage 2: Global Customizations (all accounts)
    → Terraform: delete default VPCs, IAM password policy,
                 CloudWatch billing alert, Config enable

  Stage 3: OU Customizations (Production OU)
    → Terraform: strict SCPs, Network firewall policy,
                 mandatory encryption at rest

  Stage 4: Account Customizations (payments-prod specific)
    → Terraform: TGW attachment, Route53 hosted zone,
                 baseline IAM roles, S3 encryption policy

  Stage 5: Notification
    → Slack message: "payments-prod account ready: 123456789012"

Total time: 25-40 minutes, zero human intervention
```

### Pattern 2: Centralized Egress with Security Inspection
```
Spoke VPC (PaymentsApp-Prod)
  → Default route 0.0.0.0/0 → TGW
  → TGW routes to Egress-Inspection VPC
  → AWS Network Firewall (stateful inspection)
    → Allow: *.amazonaws.com, api.stripe.com
    → Block: everything else
  → NAT Gateway → Internet

Benefits:
  - All egress through one chokepoint
  - Centralized firewall rules (not per-VPC)
  - Network Firewall logs to S3 → SIEM
  - One set of Elastic IPs for external allowlists

Requires: TGW Appliance Mode enabled (prevents asymmetric routing)
```

### Pattern 3: Read Access for Security Team Across All Accounts
```
IAM Identity Center:
  Permission Set: SecurityAuditAccess
    → AWS managed policy: SecurityAudit + ReadOnlyAccess
    → Session duration: 4 hours

  Assignment: SecurityAuditAccess → ALL accounts → security-team group

Security team member:
  → Logs into SSO portal
  → Sees all 200 accounts
  → Clicks into any account → Read-only console access
  → No long-term credentials, no IAM users per account
  → All access logged to CloudTrail
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Control Tower can't enroll non-compliant accounts** | Accounts with resources that violate mandatory guardrails fail enrollment. Common cause: existing CloudTrail trails, Config recorders with different settings. Fix: clean up first or use manual baseline instead of CT enrollment. |
| **StackSets SELF_MANAGED requires trust roles** | StackSets with self-managed permissions need `AWSCloudFormationStackSetAdministrationRole` in admin account AND `AWSCloudFormationStackSetExecutionRole` in every target account. Service-managed permissions (requires Organizations) handle this automatically. |
| **TGW attachment limit 5 per VPC** | A VPC can attach to at most 5 Transit Gateways. In complex multi-region architectures, this can be a constraint. Plan TGW topology upfront. |
| **Centralized NAT Gateway becomes a bottleneck** | All spoke VPCs routing internet traffic through one NAT Gateway in Egress VPC creates a single point of congestion. Size the NAT GW for aggregate bandwidth; scale horizontally with multiple NAT GWs per AZ. |
| **RAM sharing resources stay in owner account** | When Networking account shares a subnet, application accounts deploy EC2 instances into that subnet, but the subnet (and VPC, and any NAT GW) remain in and are billed to the Networking account. This is often unexpected. |
| **GuardDuty auto-enable doesn't retroactively enroll** | When you enable "auto-enable" for new accounts in GuardDuty, existing accounts before you enabled it are NOT automatically enrolled. Run explicit enrollment for existing accounts. |
| **AFT pipeline can fail silently** | AFT CodePipeline stages can fail partway through, leaving an account partially baselined. Implement Lambda-based completion notifications and validate all baseline checks after account creation. |
| **StackSets deployments can be slow** | Deploying a StackSet change to 500 accounts with max concurrent = 10% = 50 accounts at a time. A full deployment can take 30+ minutes. Plan maintenance windows. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Set up a minimal centralized security architecture and practice StackSets.

### Step 1: Create a Two-Account Organization
```
See organizations.md lab for org setup
Have: management account + one member account
```

### Step 2: Deploy Config Rule via StackSets
```
CloudFormation Console (management account) → StackSets → Create StackSet

Template (upload):
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  RestrictedSSH:
    Type: AWS::Config::ConfigRule
    Properties:
      ConfigRuleName: restricted-ssh-stackset
      Source:
        Owner: AWS
        SourceIdentifier: INCOMING_SSH_DISABLED

StackSet settings:
  Permissions: Service-managed (Organizations)
  Deployment targets: Deploy to Organization OU (select your OU)
  Specify regions: us-east-1
  Deployment options:
    Max concurrent: 1 account (safe for lab)
    Failure tolerance: 0
```

### Step 3: Verify in Member Account
```
Assume role in member account:
  aws sts assume-role \
    --role-arn arn:aws:iam::MEMBER-ACCOUNT-ID:role/OrganizationAccountAccessRole \
    --role-session-name lab-verify

In member account:
  AWS Config Console → Rules
  → Should see: "restricted-ssh-stackset" rule created by StackSets
  → Not modifiable by member account user (managed by StackSets)
```

### Step 4: Set Up a Delegated Administrator (GuardDuty)
```
Management account → GuardDuty Console
  Settings → Accounts → Designate delegated administrator
  Enter: member account ID
  → Confirm

Member account → GuardDuty Console
  → Now shows as administrator
  → Can see/manage GuardDuty for entire organization
```

### Step 5: Create a Cross-Account Monitoring Dashboard
```
CloudWatch Console (management account) → Dashboards → Create dashboard
  Name: cross-account-overview

Add widget → Explorer (cross-account/cross-region):
  Select metric: AWS/EC2 → CPUUtilization
  Account: All accounts in organization
  Region: All regions

→ Dashboard now shows EC2 CPU across ALL accounts
```

---

## Summary Reference Card

```
ACCOUNT VENDING:
  Control Tower + Account Factory: UI/API-driven, managed guardrails
  AFT: GitOps/Terraform pipeline, most flexible
  Custom Lambda: Organizations API + EventBridge + StackSets

CENTRALIZED SECURITY:
  Log Archive account: CloudTrail + Config + VPC Flow Logs (all accounts → central S3)
  Audit account: Config aggregator + Security Hub aggregator + GuardDuty admin
  Protection: S3 Object Lock WORM on log buckets

CENTRALIZED NETWORKING:
  Transit Gateway: hub-and-spoke, route table segmentation (Prod/Dev/Shared)
  RAM sharing: VPC subnets, TGW, Route53 Resolver rules
  Egress centralized: NAT Gateway in Networking account
  DNS centralized: Route53 Resolver + rules shared via RAM

COST GOVERNANCE:
  Consolidated billing: volume discounts, Savings Plans shared org-wide
  Tag Policies: enforce tag key/value standardization
  Budget Actions: apply SCP or stop instances when budget breached

LIMITS:
  TGW: 5 attachments per VPC
  StackSets: service-managed = automatic trust roles via Organizations
  RAM: resources stay in owner account (important for billing attribution)
```
