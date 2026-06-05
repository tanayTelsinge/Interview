# AWS Organizations + Multi-Account Governance

## 1. The Problem

Running all workloads in a single AWS account creates a "blast radius" problem:

- A compromised IAM user can access **everything** — dev, test, prod, billing, security
- A runaway Terraform script can delete production resources
- Developers need broad access for experimentation but that conflicts with production security
- Billing is impossible to attribute — "which team spent $50K last month?"
- Compliance requires segregation: PCI systems must not share infrastructure with non-PCI

The solution: **multiple AWS accounts** with organizational governance. But managing 50–500 accounts individually is chaos without an automation framework.

---

## 2. What AWS Built

| Service | Purpose |
|---------|---------|
| **AWS Organizations** | Hierarchical account management, consolidated billing, policy framework |
| **Service Control Policies (SCPs)** | Guardrails — restrict what's possible across accounts |
| **AWS Control Tower** | Landing zone setup + automated account provisioning |
| **IAM Identity Center** | Centralized SSO across all accounts |
| **AWS Resource Access Manager (RAM)** | Share AWS resources across accounts without copying |

---

## 3. How It Works

### AWS Organizations

**Structure:**
```
Root (the organization)
  └── OU: Security
        ├── Account: Audit (CloudTrail, Config aggregator)
        └── Account: LogArchive (central log storage)
  └── OU: Infrastructure
        ├── Account: Networking (TGW, VPN, DX)
        └── Account: SharedServices (AD, DNS, monitoring)
  └── OU: Workloads
        ├── OU: Production
        │     ├── Account: PaymentsApp-Prod
        │     └── Account: InventoryApp-Prod
        └── OU: Non-Production
              ├── Account: PaymentsApp-Dev
              └── Account: PaymentsApp-Staging
  └── OU: Sandbox
        └── Account: Developer-Experiments
```

**Key Concepts:**
```
Management Account (formerly Master):
  - Payer for all accounts (consolidated billing)
  - Creates and manages the organization
  - IMMUNE to SCPs (SCPs do NOT apply to management account)
  - Best practice: use management account ONLY for billing and org management
                   do NOT deploy workloads here

Member Accounts:
  - Receive billing via management account
  - Subject to SCPs from parent OUs
  - Can be moved between OUs

OUs (Organizational Units):
  - Logical grouping of accounts
  - SCPs applied to OU affect all accounts under it
  - Nested OUs: policies accumulate from root → OU → child OU → account
```

**Policy Types in Organizations:**
| Policy Type | What it controls |
|-------------|-----------------|
| **SCPs** | What IAM actions are ALLOWED (permission guardrails) |
| **Tag Policies** | Enforce tag standardization (key names, allowed values, case) |
| **Backup Policies** | Enforce backup plans across accounts |
| **AI Services Opt-Out Policies** | Control AI service data usage |

---

### Service Control Policies (SCPs)

**Critical Concept: SCPs don't GRANT permissions.**

```
IAM user/role permission = SCP ∩ IAM policy
                         = what BOTH allow

Example:
  SCP: Allow s3:*, ec2:* (deny everything else)
  IAM policy: Allow s3:*, rds:*

  Effective permissions: s3:* only (intersection)
  → RDS allowed by IAM but blocked by SCP
  → EC2 allowed by SCP but blocked by IAM
```

**Management Account is SCP-immune:**
```
SCPs NEVER apply to the management account
Even if you apply a "Deny *" SCP to Root, management account is unaffected
This is why: run no workloads in management account
```

**Allow-List vs Deny-List Strategy:**
```
ALLOW-LIST (whitelist):
  Default SCP: {Effect: Deny, Action: *, Resource: *}
  Add explicit allows per OU/account
  Pros: Safest, explicit about what's permitted
  Cons: Easy to accidentally over-restrict, high maintenance
  Use for: highly regulated environments (government, financial)

DENY-LIST (blacklist) — AWS default approach:
  Default SCP: FullAWSAccess (Allow *)
  Add explicit denies for specific restrictions
  Pros: Easier to manage, don't break things accidentally
  Cons: Anything not explicitly denied is allowed
  Use for: most enterprise environments
```

**SCP Examples:**
```json
// Prevent disabling CloudTrail:
{
  "Effect": "Deny",
  "Action": [
    "cloudtrail:StopLogging",
    "cloudtrail:DeleteTrail",
    "cloudtrail:UpdateTrail"
  ],
  "Resource": "*"
}

// Restrict operations to approved regions only:
{
  "Effect": "Deny",
  "Action": "*",
  "Resource": "*",
  "Condition": {
    "StringNotEquals": {
      "aws:RequestedRegion": ["us-east-1", "us-west-2", "eu-west-1"]
    }
  }
}

// Prevent accounts from leaving the organization:
{
  "Effect": "Deny",
  "Action": ["organizations:LeaveOrganization"],
  "Resource": "*"
}

// Require MFA for sensitive actions:
{
  "Effect": "Deny",
  "Action": ["iam:DeleteVirtualMFADevice", "iam:DeactivateMFADevice"],
  "Resource": "*",
  "Condition": {
    "BoolIfExists": {"aws:MultiFactorAuthPresent": "false"}
  }
}

// Prevent disabling default EBS encryption:
{
  "Effect": "Deny",
  "Action": ["ec2:DisableEbsEncryptionByDefault"],
  "Resource": "*"
}
```

**SCPs and Service-Linked Roles:**
```
SCPs DO NOT restrict service-linked roles
Service-linked roles are used by AWS services themselves
Example: EC2 Auto Scaling uses a service-linked role — SCPs can't block it

This means: even with a restrictive SCP, AWS services can still perform
their managed actions (creating ENIs, writing CloudWatch metrics, etc.)
```

---

### AWS Control Tower

**What it is:** An opinionated, automated way to set up a well-architected multi-account environment (Landing Zone).

**Components:**
```
Landing Zone:
  Automated setup of recommended OU structure + accounts + guardrails
  Created accounts: Audit, Log Archive (automatically)
  OU structure: Security OU, Sandbox OU (customizable)

Account Factory:
  Standardized template for new account provisioning
  Via: Service Catalog product (self-service) or API
  Creates: new account + applies OU + applies baselines

Guardrails (Controls):
  Mandatory:         Always enabled, can't be disabled
  Strongly Recommended: Enabled by default, can disable
  Elective:          Optional, disabled by default

Preventive guardrails:  SCPs (block non-compliant actions)
Detective guardrails:   Config rules (detect and report non-compliance)
Proactive guardrails:   CloudFormation hooks (prevent non-compliant resources)
```

**Control Tower - What it auto-creates:**
```
OUs:
  Security OU
  Sandbox OU

Accounts:
  Log Archive account → receives: CloudTrail logs, Config history from all accounts
  Audit account → receives: Config findings, Security Hub findings, SNS notifications

Guardrails applied automatically:
  - CloudTrail enabled in all accounts
  - CloudTrail logs sent to Log Archive S3
  - Config enabled in all accounts
  - Config aggregated in Audit account
  - No root account access keys
  - MFA on root account
  - S3 versioning on logging buckets
```

**Account Factory for Terraform (AFT):**
```
GitOps approach to account vending:
  Git repo → PR to add new account → approval → AFT pipeline runs
  → Organizations API creates account
  → AFT applies baseline customizations
  → Account ready for use

Pipeline components:
  CodePipeline + CodeBuild + Terraform
  Customizations: IAM roles, Config rules, CloudTrail, VPC setup, SCPs
```

---

### Cross-Account Patterns

**Pattern 1: Role Assumption (Standard)**
```
Developer in Account A wants to access S3 in Account B:

Account B: Create role "CrossAccountS3Role"
  Trust policy:
  {
    "Principal": {"AWS": "arn:aws:iam::ACCOUNT-A-ID:root"}
  }
  Permission policy: S3 read access

Developer in Account A:
  aws sts assume-role \
    --role-arn arn:aws:iam::ACCOUNT-B-ID:role/CrossAccountS3Role \
    --role-session-name my-session

  Use returned temp credentials to access Account B's S3
```

**Pattern 2: Resource-Based Policies (No AssumeRole)**
```
S3, SQS, Lambda, KMS, SNS support resource-based policies
Principal can be another account's IAM entity directly

S3 bucket in Account B:
{
  "Effect": "Allow",
  "Principal": {"AWS": "arn:aws:iam::ACCOUNT-A-ID:role/AppRole"},
  "Action": ["s3:GetObject"],
  "Resource": "arn:aws:s3:::bucket-name/*"
}

Account A's AppRole can read from Account B's S3 without AssumeRole
```

**Pattern 3: AWS RAM (Resource Access Manager)**
```
Share resources across accounts in the same Organization:
  VPC Subnets (share subnets, resources deployed in owner account's VPC)
  Transit Gateway (TGW attachments from other accounts)
  Route 53 Resolver rules
  License Manager licenses
  Glue Data Catalog
  CodeBuild projects

RAM sharing:
  - Resource stays in OWNER account (no copy)
  - Recipient account sees the resource, can use it
  - No cost for RAM itself

Common pattern: Networking account owns VPC → RAM shares subnets → App accounts
deploy resources into shared subnets (centralized network management)
```

**Pattern 4: Service Catalog**
```
IT team creates approved CloudFormation templates → publishes in Service Catalog
Developers consume: "Deploy a compliant EC2 with approved AMI and config"

Governance without blocking:
  - Developers get self-service (no tickets)
  - IT enforces: approved AMIs, mandatory tags, security groups, instance types
  - Audit: all deployments tracked

Cross-account: share portfolio from management account to member accounts
```

---

### IAM Identity Center (formerly AWS SSO)

**What it replaces:** Separate IAM users in every account (unmanageable at scale).

**Architecture:**
```
Identity Source:
  IAM Identity Center built-in users/groups
  External: SAML 2.0 (Azure AD, Okta, Google Workspace, Ping Identity)
  Active Directory: AWS Managed AD or AD Connector

SCIM Provisioning:
  Automatic user/group sync from IdP to Identity Center
  When user added to "AWS-Admins" AD group → automatically gets permissions

Permission Sets:
  Define permissions (like IAM policies) once
  Assign to: account + user/group combination

  Examples:
    PermissionSet: ReadOnlyAccess → OU: Production → Group: Developers
    PermissionSet: AdministratorAccess → Account: Dev → Group: DevTeam
    PermissionSet: SecurityAuditAccess → OU: all accounts → Group: SecurityTeam
```

**ABAC with IAM Identity Center:**
```
Attribute-Based Access Control scales better than RBAC:
  Instead of: one permission set per team per account
  Use: tags (attributes) on resources + identity attributes

Example:
  Developer John has attribute: CostCenter=Payments
  EC2 instances tagged: CostCenter=Payments

  IAM condition:
  {
    "StringEquals": {
      "aws:ResourceTag/CostCenter": "${aws:PrincipalTag/CostCenter}"
    }
  }

  → John can only access EC2s tagged with his CostCenter
  → One permission set works for all teams
  → No permission set changes when new team member joins
```

**Delegated Administration:**
```
Delegate security service management without sharing management account access:

Service → Delegated Admin Account
GuardDuty       → Security account (see findings from all accounts)
Security Hub    → Security account (aggregate all findings)
Macie           → Security account (manage data classification)
AWS Config      → Audit account (aggregate compliance)
Firewall Manager→ Security account (manage WAF, SG policies centrally)

Management account: registers delegated admin
Delegated account: performs admin actions for the service
```

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| Max accounts per organization | 10,000 (can request increase) |
| Max OUs per organization | 1,000 |
| Max OU nesting depth | 5 levels |
| SCPs apply to management account? | NO — immune |
| Max SCPs per target (account/OU/root) | 5 |
| Max SCP size | 5,120 characters |
| Control Tower — managed regions | All commercial regions |
| IAM Identity Center permission sets per account | 20 (soft limit) |
| RAM — resource types shareable | 50+ resource types |

---

## 5. Decision Tree

### Single vs Multi-Account
```
Which environments/workloads need isolation?
│
├─ Prod must be isolated from dev (blast radius, compliance)?
│   └─ Separate accounts per environment
│
├─ Regulatory: PCI, HIPAA need isolated network + billing?
│   └─ Separate accounts for regulated workloads
│
├─ Multiple teams with independent billing?
│   └─ Account per team OR per team per environment
│
└─ Just getting started, <5 people, no compliance requirements?
    └─ 2-3 accounts (prod, dev, sandbox) is fine to start
```

### OU Structure
```
What determines your OU hierarchy?
  Security posture → Security OU, Workloads OU, Sandbox OU
  Business units → OU per BU, sub-OU per environment
  Compliance → OU per compliance level (PCI, non-PCI)
  Stage → OU per environment (Prod, Staging, Dev)

Most common: mix of environment + function:
  Root → Security OU → Infrastructure OU → Workloads OU (Dev/Staging/Prod) → Sandbox OU
```

### SCP vs IAM Permission Boundary
```
Want to restrict ENTIRE ACCOUNT regardless of who creates IAM policies?
└─ SCP (applies to everyone in the account, org-level control)

Want to restrict what a specific IAM principal can grant to others?
└─ IAM Permission Boundary (per IAM user/role, account-level control)

Use both: SCP = org guardrail, Permission Boundary = per-role guardrail
```

---

## 6. Common Patterns

### Pattern 1: Typical Enterprise OU Structure
```
Root
  ├── Security OU [SCP: restrict deleting security resources]
  │     ├── Audit Account (Config aggregator, Security Hub aggregator)
  │     └── LogArchive Account (centralized CloudTrail + Config logs S3)
  │
  ├── Infrastructure OU [SCP: networking team only can modify VPC/TGW]
  │     ├── Networking Account (TGW, DX, VPN, NAT, Route53)
  │     └── SharedServices Account (AD, monitoring, CI/CD tooling)
  │
  ├── Workloads OU
  │     ├── Production OU [SCP: strict, no direct root, no public S3, approved regions]
  │     │     ├── PaymentsApp-Prod
  │     │     └── InventoryApp-Prod
  │     ├── Staging OU [SCP: moderate]
  │     └── Development OU [SCP: relaxed but no prod impact possible]
  │
  └── Sandbox OU [SCP: block only destructive org actions, cost limits]
        └── Individual developer accounts (auto-expire after 90 days)
```

### Pattern 2: Preventing Log Tampering at Org Level
```
SCP applied to all accounts (including Security OU):
{
  "Effect": "Deny",
  "Action": [
    "cloudtrail:StopLogging",
    "cloudtrail:DeleteTrail",
    "cloudtrail:UpdateTrail",
    "s3:DeleteObject",    ← on logging bucket specifically
    "s3:PutBucketPolicy"  ← prevent policy changes on log bucket
  ],
  "Resource": "*"
}

Management account retains ability to make emergency changes
(SCPs don't apply to management account)
```

### Pattern 3: IAM Identity Center + ABAC Enterprise Setup
```
1. Connect Azure AD as SAML 2.0 IdP to IAM Identity Center
2. Enable SCIM: sync AD groups automatically
3. AD Groups → Identity Center Groups:
     aws-prod-readonly  → map to ReadOnly permission set
     aws-prod-admin     → map to AdministratorAccess permission set
     aws-security-audit → map to SecurityAudit permission set (all accounts)
4. User logs in: portal.sso.aws.amazon.com
   → Sees accounts they have access to
   → Selects account + role → short-lived temp credentials
5. ABAC tagging: add Team tag from AD attributes → resources
```

### Pattern 4: Account Baseline Automation
```
EventBridge rule: "AWS account created" event
  → Lambda (account baseline)
    → Enable Config
    → Enable GuardDuty (delegate to security account)
    → Enable Security Hub
    → Delete default VPC (prevent accidental usage)
    → Create mandatory CloudWatch alarms (root login, console signin without MFA)
    → Set IAM password policy
    → Apply mandatory resource tags via Tag Policies
    → Enroll in Control Tower (if using AFT)

All of this runs automatically for every new account
No manual baseline steps
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Management account is immune to SCPs** | This is the most tested SAP-C02 trap. No matter how restrictive your SCP, management account users can still do anything. Never deploy workloads in management account. |
| **Control Tower can't enroll non-compliant accounts** | Accounts with existing resources that violate Control Tower guardrails can't be enrolled. Clean up first or use AFT with custom remediation. |
| **RAM sharing — resources stay in owner account** | When you share a VPC subnet via RAM, the subnet is NOT copied to recipient account. It stays in the Networking account. Resources deployed in the subnet count against the Networking account's limits. |
| **SCPs don't affect service-linked roles** | Services like EC2 Auto Scaling, ECS, RDS use service-linked roles that operate outside SCP restrictions. Can't block AWS service operations with SCPs. |
| **Enabling Config in all accounts is expensive** | Every resource change generates a billable Config item. In a 100-account org with active ASGs, cost can be $1,000+/month for Config alone. Use org aggregator to consolidate. |
| **SCP policy evaluation order** | SCPs evaluate from Root → OU → Account. An explicit Deny at any level blocks, even if Allow exists closer to the account. Check the full policy path. |
| **IAM Identity Center home region can't change** | Once set, the home region for IAM Identity Center is permanent. Choose carefully (us-east-1 or us-west-2 recommended). |
| **Control Tower email verification requirement** | Creating accounts via Account Factory requires a unique email per account AND email confirmation from that address. Plan email alias strategy upfront. |
| **RAM doesn't support all resource types** | Not every resource can be shared. Check the RAM docs for the supported resource types list. VPC subnets, TGW, Route53 Resolver rules are the most commonly shared. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Create an organization, apply an SCP, and configure IAM Identity Center.

### Step 1: Create an AWS Organization
```
AWS Organizations Console → Create organization
  Choose: All features (not consolidated billing only)

Your current account becomes the management account
Note: cannot undo "all features" without dissolving the org
```

### Step 2: Create OU Structure
```
Organizations Console → Root
  → Actions → Create organizational unit
    Name: Sandbox

  → Actions → Create organizational unit
    Name: Workloads

  → Under Workloads → Create OU: Production
  → Under Workloads → Create OU: Development
```

### Step 3: Invite or Create a Member Account
```
Organizations Console → Add account → Invite existing account
  (use a second AWS account email if you have one)

OR: Create account (new account, needs unique email)
  Account name: lab-sandbox-01
  Email: your+sandbox@gmail.com (Gmail alias trick)
```

### Step 4: Create and Apply an SCP
```
Organizations Console → Policies → Service control policies → Create policy
  Name: DenyRegionsExceptUsEast1
  Policy:
  {
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Deny",
      "Action": "*",
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "aws:RequestedRegion": ["us-east-1"]
        },
        "ArnNotLike": {
          "aws:PrincipalARN": ["arn:aws:iam::*:role/OrganizationAccountAccessRole"]
        }
      }
    }]
  }

Attach SCP → Sandbox OU
```

### Step 5: Test SCP
```bash
# Assume role in the sandbox account:
aws sts assume-role \
  --role-arn arn:aws:iam::SANDBOX-ACCOUNT-ID:role/OrganizationAccountAccessRole \
  --role-session-name scp-test

# Use returned credentials:
export AWS_ACCESS_KEY_ID=xxx
export AWS_SECRET_ACCESS_KEY=xxx
export AWS_SESSION_TOKEN=xxx

# Try to list S3 in us-east-1 (should succeed):
aws s3 ls --region us-east-1

# Try to list EC2 in us-west-2 (should be denied by SCP):
aws ec2 describe-instances --region us-west-2
# Expected: AccessDenied error
```

### Step 6: Enable IAM Identity Center
```
IAM Identity Center Console → Enable
  (automatically uses management account's region as home region)

Identity source: Identity Center directory (built-in for lab)

Create user:
  Username: lab-user
  Email: your+labuser@email.com
  Confirm email invitation

Create group: lab-admins
  Add lab-user to group
```

### Step 7: Assign Permission Set
```
IAM Identity Center → Permission sets → Create permission set
  Use predefined: ReadOnlyAccess
  Name: ReadOnly

Accounts → (select your sandbox account) → Assign users and groups
  Group: lab-admins
  Permission set: ReadOnly
  → Submit assignment
```

### Step 8: Test SSO Access
```
Identity Center Console → Settings → User portal URL
  Copy the portal URL

Open incognito browser → navigate to portal URL
  Sign in with lab-user credentials
  → Should see: Sandbox account with ReadOnly access
  → Click "ReadOnly" → "Management console"
  → Opens sandbox account console with limited permissions
```

### Step 9: Clean Up
```
IAM Identity Center: Delete user, group, permission set
Organizations: Detach SCP from Sandbox OU → Delete SCP
Organizations: Remove member account (if created)
Organizations: Delete organization (careful — irreversible process)
```

---

## Summary Reference Card

```
ORGANIZATIONS:
  Management account: payer, org admin, IMMUNE to SCPs
  Member accounts: subject to SCPs from parent OUs
  OUs: logical grouping, policies accumulate Root→OU→Account

SCPs:
  Don't GRANT — only RESTRICT. Effective = SCP ∩ IAM
  Management account: immune
  Service-linked roles: immune
  Strategy: Deny-list (default Allow *) for most, Allow-list for high-security

CONTROL TOWER:
  Landing Zone: auto-creates OU structure, Audit + Log Archive accounts
  Guardrails: Mandatory/Recommended/Elective × Preventive(SCP)/Detective(Config)/Proactive
  Account Factory: template provisioning via Service Catalog
  AFT: GitOps pipeline for Terraform-based account provisioning

IAM IDENTITY CENTER:
  SSO across all accounts — one login, many accounts
  Permission sets: IAM policies assigned to account+user/group
  ABAC: tag-based access scales better than per-team permission sets
  SCIM: auto-sync from Azure AD/Okta/Google

CROSS-ACCOUNT:
  Role assumption: standard for app-to-app
  Resource policies: S3/SQS/Lambda — no AssumeRole needed
  RAM: share VPC subnets, TGW, Route53 rules (resource stays in owner account)
```
