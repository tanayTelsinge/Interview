# AWS IAM — SAP-C02 Deep Dive

---

## 1. The Problem

Without centralized access control, every AWS API call would either be open to everyone or require application-level secret sharing. You need to answer: **Who** is making this API call? **Are they allowed** to perform this action on this resource? How do you grant **temporary, scoped credentials** to EC2 instances, Lambda functions, and cross-account services without hardcoded passwords?

**Access control problems IAM solves:**
- Human users need different permissions (developer vs auditor vs billing)
- Applications must access AWS services without long-term keys
- Partner accounts need access to specific resources without sharing credentials
- Regulatory compliance requires least-privilege and audit trails
- Acquisitions bring multiple AWS accounts that need unified access

---

## 2. What AWS Built

AWS Identity and Access Management (IAM) is a **global, free service** that controls authentication (who you are) and authorization (what you can do) for all AWS API calls. It manages Users, Groups, Roles, and Policies.

---

## 3. How It Works

### IAM Components

```
┌─────────────────────────────────────────────────┐
│                    AWS Account                  │
│                                                 │
│  Users ──member of──> Groups                    │
│    |                    |                       │
│    └──attached to──┐    └──attached to──┐       │
│                    ▼                    ▼       │
│               IAM Policies (identity-based)     │
│                                                 │
│  Roles ──trust policy──> Principals             │
│    |                                            │
│    └──permission policy──> what they can do     │
│                                                 │
│  Resources ──resource-based policy──> who       │
└─────────────────────────────────────────────────┘
```

**User:** Long-term identity for a human or service. Has username/password (console) and/or access keys (API). Max 5,000 users per account.

**Group:** Collection of users. Attach policies to group, members inherit. Cannot nest groups. Cannot be used as a principal in policies.

**Role:** Temporary identity assumed by principals (users, services, applications). No long-term credentials. Uses STS to issue temporary tokens (15 min – 36 hrs).

**Policy:** JSON document defining permissions. Attached to identities or resources.

### Policy Types

| Policy Type | Where Attached | Who Writes | Purpose |
|---|---|---|---|
| **AWS Managed** | User/Group/Role | AWS | Common use cases (ReadOnlyAccess, AdministratorAccess) |
| **Customer Managed** | User/Group/Role | You | Custom permissions, versioned, reusable |
| **Inline** | Single User/Group/Role | You | One-to-one, deleted with identity |
| **Resource-based** | Resource (S3/SQS/KMS/Lambda) | You | Cross-account access, who can act on resource |
| **Permission Boundary** | User/Role | You | Cap maximum permissions (doesn't grant by itself) |
| **SCP (Service Control Policy)** | OU/Account in AWS Org | Org admin | Set maximum permissions for entire account |
| **Session Policy** | Temporary session | Caller | Restrict AssumeRole session permissions |

### Policy Structure

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowS3ReadInPrefix",
      "Effect": "Allow",
      "Principal": {"AWS": "arn:aws:iam::123456789:user/alice"},  // resource-based only
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/reports/*"
      ],
      "Condition": {
        "StringEquals": {"s3:prefix": ["reports/"]},
        "Bool": {"aws:MultiFactorAuthPresent": "true"},
        "IpAddress": {"aws:SourceIp": ["203.0.113.0/24"]}
      }
    }
  ]
}
```

### Policy Evaluation Logic

```
Incoming API call
        |
1. Is there an explicit DENY anywhere?
   └── YES → DENY (stops here, no exceptions*)
        |
2. Is there an SCP that denies this?
   └── YES → DENY
        |
3. Is there an SCP that allows this? (if Org member)
   └── NO → DENY (SCP implicit deny)
        |
4. Does a resource-based policy explicitly ALLOW this principal?
   └── YES → ALLOW (for cross-account: need both resource policy + identity policy)
        |
5. Is there a Permission Boundary? Does it allow this?
   └── Boundary exists but doesn't allow → DENY
        |
6. Does an identity-based policy ALLOW this?
   └── YES → ALLOW
   └── NO  → DENY (implicit deny)
```

**Key rule:** Explicit DENY always wins — even over Allow in another policy.

**Exception:** Management account is immune to SCPs (cannot be restricted by SCPs).

**Cross-account access:**
- S3/SQS/KMS/Lambda resource-based policy alone CAN grant cross-account access
- For most other services, you need BOTH: resource policy + identity policy in the calling account

### IAM Roles Deep Dive

A role has two parts:

**1. Trust Policy** (who can assume this role):
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Service": "ec2.amazonaws.com",      // EC2 instances
      "AWS": "arn:aws:iam::OTHER-ACCT:root" // Cross-account
    },
    "Action": "sts:AssumeRole",
    "Condition": {
      "StringEquals": {"sts:ExternalId": "secret-external-id"}  // confused deputy protection
    }
  }]
}
```

**2. Permission Policy** (what the role can do):
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:GetObject", "s3:PutObject"],
    "Resource": "arn:aws:s3:::my-bucket/*"
  }]
}
```

### EC2 Instance Profile

- Container that holds exactly one IAM role
- Attached to EC2 instance at launch or dynamically
- EC2 metadata service: `http://169.254.169.254/latest/meta-data/iam/security-credentials/role-name`
- SDK auto-refreshes credentials before expiry — no manual credential management needed

```bash
# Get role credentials from EC2 metadata (IMDSv2)
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/iam/security-credentials/my-ec2-role
```

### Cross-Account Access

**Method 1: Role Assumption**
```
Account A (your account)    →    Account B (partner account)
IAM User/Role                      IAM Role (trust: Account A)
      |                                       |
      └──sts:AssumeRole──────────────────────>|
                                              |
                     Returns: AccessKeyId + SecretKey + SessionToken
```

**Method 2: External ID (Confused Deputy Fix)**
```
Problem without ExternalId:
  Customer A creates role, gives ARN to SaaS vendor
  Attacker learns that ARN and tricks vendor into assuming it on their behalf
  → Confused deputy problem (vendor is the "deputy", confused about who to act for)

Solution:
  Role trust policy includes: "sts:ExternalId": "CUSTOMER-A-UNIQUE-SECRET"
  Only Customer A's ExternalId works for that role
```

### STS Token Durations

| API | Default Duration | Min | Max |
|---|---|---|---|
| `AssumeRole` | 1 hour | 15 min | 12 hours (role max session) |
| `AssumeRoleWithWebIdentity` | 1 hour | 15 min | 12 hours |
| `GetSessionToken` (MFA) | 12 hours | 15 min | 36 hours |
| `AssumeRoleWithSAML` | 1 hour | 15 min | 12 hours |

### Permission Boundaries

```
Admin grants developer this permission boundary:
{
  "Effect": "Allow",
  "Action": ["s3:*", "dynamodb:*"],
  "Resource": "*"
}

Developer's identity policy says:
{
  "Effect": "Allow",
  "Action": ["ec2:*", "s3:*"],
  "Resource": "*"
}

Effective permissions = intersection = s3:* only
(EC2 blocked by boundary; DynamoDB allowed by boundary but not identity policy)
```

Use case: Allow developers to create IAM roles but cap what those roles can do.

### ABAC — Attribute-Based Access Control

```json
// Policy using tags for access control
{
  "Effect": "Allow",
  "Action": "ec2:*",
  "Resource": "*",
  "Condition": {
    "StringEquals": {
      "ec2:ResourceTag/Project": "${aws:PrincipalTag/Project}"
    }
  }
}
// Users tagged Project=TeamA can only manage EC2 tagged Project=TeamA
```

### Key Policy Conditions

| Condition Key | Example Use |
|---|---|
| `aws:SourceIp` | Restrict API calls to corporate IP range |
| `aws:RequestedRegion` | Limit actions to specific regions |
| `aws:MultiFactorAuthPresent` | Require MFA for sensitive actions |
| `aws:PrincipalOrgID` | Only allow principals from your AWS Organization |
| `s3:prefix` | Restrict S3 operations to folder prefix |
| `kms:CallerAccount` | KMS key usable only within your account |
| `aws:CurrentTime` | Time-based access (business hours only) |
| `aws:SecureTransport` | Require HTTPS (deny HTTP) |
| `sts:ExternalId` | Cross-account confused deputy protection |

---

## 4. IAM Identity Center (SSO)

**Problem:** 100 AWS accounts × 500 employees = 50,000 IAM users to manage.

**Solution:** Centralized identity federation.

```
Corporate IdP (Okta/AD/Azure AD)
        |
    IAM Identity Center
        |──SAML 2.0 / SCIM──> AWS Accounts
        |
   Permission Sets (IAM policies) assigned to:
   ├── Users
   └── Groups
            |
        Mapped to AWS Account + role
```

**Permission Sets:** Reusable IAM policy bundles deployed as roles in each account.
Example: `ReadOnlyAccess` permission set assigned to "Auditors" group → deploys ReadOnly role in all 100 accounts.

**SCIM:** Automatic user provisioning/deprovisioning from IdP — when user leaves company, access revoked from all AWS accounts automatically.

---

## 5. Amazon Cognito

### User Pools (Authentication)

```
Mobile/Web App ──sign-in──> Cognito User Pool
                                   |
                             JWT tokens (ID/Access/Refresh)
                             MFA support
                             Social login (Google/Facebook/Apple)
                             SAML federation
```

- Stores user directory in AWS
- Returns JWT tokens (not AWS credentials)
- Handles MFA, email verification, password policies
- Up to 50 million users

### Identity Pools (Authorization — AWS Credentials)

```
App has Cognito User Pool JWT (or Google/Facebook token)
        |
   Cognito Identity Pool
        |──sts:AssumeRoleWithWebIdentity──> IAM Role
                                                |
                                       Temporary AWS credentials
                                       (S3, DynamoDB, etc.)
```

- Maps authenticated/unauthenticated users to IAM roles
- Supports social identity providers, SAML, User Pools
- Enables "login with Google → access S3 bucket"

### Combined Pattern (Most Common)

```
User → Cognito User Pool (authenticate, get JWT)
     → Cognito Identity Pool (exchange JWT for AWS creds)
     → Access DynamoDB/S3 with user-specific row-level security
```

---

## 6. Key Config & Limits

| Parameter | Value |
|---|---|
| IAM users per account | 5,000 |
| IAM groups per account | 300 |
| IAM roles per account | 1,000 |
| Managed policies per account | 1,500 |
| Managed policies attached to identity | 10 |
| Inline policy size | 2,048 characters |
| Managed policy size | 6,144 characters |
| Access keys per user | 2 |
| MFA devices per user | 8 |
| AssumeRole max session duration | 12 hours |
| IAM service | Global (not regional) |

---

## 7. Decision Tree

### Which Identity Mechanism?

```
Who/what needs AWS access?

├── Human employee, internal tool?
│   ├── < 10 accounts, small team → IAM User + MFA
│   └── Multiple accounts / enterprise / SSO needed → IAM Identity Center

├── AWS service (EC2, Lambda, ECS)?
│   └── IAM Role (instance profile / execution role)

├── External application (SaaS, partner account)?
│   └── IAM Role with trust policy + External ID

├── Web/Mobile app users need AWS access?
│   └── Cognito User Pool → Cognito Identity Pool → IAM Role

├── Federated corporate users (AD/Okta)?
│   └── IAM Identity Center (preferred) OR IAM SAML federation (older)

└── Automated CI/CD pipelines (GitHub Actions)?
    └── OIDC provider + IAM Role (no long-term keys!)
```

---

## 8. Common Patterns

### Pattern 1: Least-Privilege Developer Role

```
Developer (IAM Identity Center user)
      |
Permission Set: DeveloperAccess
      |
  ├── Allow: EC2/S3/RDS/Lambda read in dev account
  ├── Deny: Production account admin
  └── Boundary: no IAM modifications
```

### Pattern 2: Cross-Account Deployment

```
Account: CI/CD (CodePipeline)
      |──sts:AssumeRole──> Account: Production
                               Role: DeploymentRole
                               Trust: CI/CD account principal
                               Permissions: ECS/Lambda update only
```

### Pattern 3: S3 Cross-Account Access via Resource Policy

```json
// S3 Bucket policy in Account A allowing Account B to read
{
  "Effect": "Allow",
  "Principal": {"AWS": "arn:aws:iam::ACCOUNT-B:role/DataReaderRole"},
  "Action": ["s3:GetObject", "s3:ListBucket"],
  "Resource": ["arn:aws:s3:::shared-data", "arn:aws:s3:::shared-data/*"]
}
// Account B's IAM role also needs s3:GetObject permission
```

### Pattern 4: Emergency Break-Glass Access

```
Break-glass role in every account:
  Trust: root of security account only
  MFA required: "Bool": {"aws:MultiFactorAuthPresent": "true"}
  Permissions: AdministratorAccess
  CloudTrail alert on assumption → SNS → PagerDuty
```

---

## 9. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **SCPs don't grant permissions** | SCP Allow + IAM Allow = permitted. SCP alone doesn't grant. Need both |
| **Permission boundary limits, not grants** | Boundary of S3+DynamoDB and identity policy of EC2+S3 = only S3 allowed |
| **Explicit deny always wins** | Even if 10 Allow statements exist, one Deny overrides all |
| **IAM is global** | Roles, users, policies are global — not per-region |
| **Management account immune to SCPs** | Cannot restrict master/management account with SCPs |
| **Resource-based policy alone for S3/SQS/KMS** | Cross-account works with just resource policy (unlike most services) |
| **Group cannot be principal** | Groups can't be referenced in trust policies or resource policies |
| **Role max session** | Default 1 hour; max 12 hours. Must set `--duration-seconds` on AssumeRole |
| **Service-linked roles** | AWS manages trust policy; cannot edit it; deleted when service is removed |
| **EC2 metadata IMDSv1 vs IMDSv2** | Require IMDSv2 (token-required) to prevent SSRF attacks |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **Long-term access keys in code** | Use IAM roles for EC2/Lambda/ECS. Use OIDC for CI/CD |
| **Overly permissive `*:*` policies** | Use IAM Access Analyzer to find unused permissions. Scope down |
| **No MFA on root** | Always. Enable hardware MFA or virtual MFA on root immediately |
| **No CloudTrail** | Enable multi-region org trail. Alert on root login, access key creation |
| **Permission boundary drift** | Automate boundary assignment via SCP or account vending machine |

---

## 10. Hands-On Lab (Free Tier)

### Goal: Cross-account role assumption with External ID + permission boundary

**Step 1 — Create a Permission Boundary**

```json
// Save as boundary-policy.json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:*", "dynamodb:*"],
    "Resource": "*"
  }]
}
```

```bash
aws iam create-policy \
  --policy-name DevBoundary \
  --policy-document file://boundary-policy.json
```

**Step 2 — Create IAM User with Boundary**

```bash
aws iam create-user --user-name dev-alice

aws iam put-user-permissions-boundary \
  --user-name dev-alice \
  --permissions-boundary arn:aws:iam::ACCOUNT:policy/DevBoundary

# Attach broader policy (EC2+S3) - boundary caps to S3 only
aws iam attach-user-policy \
  --user-name dev-alice \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2FullAccess

aws iam attach-user-policy \
  --user-name dev-alice \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

Test: `dev-alice` can use S3 but NOT EC2 (blocked by boundary).

**Step 3 — Create Cross-Account Role**

```json
// trust-policy.json - replace CALLING-ACCOUNT
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"AWS": "arn:aws:iam::CALLING-ACCOUNT:root"},
    "Action": "sts:AssumeRole",
    "Condition": {
      "StringEquals": {"sts:ExternalId": "my-unique-external-id-12345"}
    }
  }]
}
```

```bash
aws iam create-role \
  --role-name CrossAccountReadRole \
  --assume-role-policy-document file://trust-policy.json \
  --max-session-duration 3600

aws iam attach-role-policy \
  --role-name CrossAccountReadRole \
  --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
```

**Step 4 — Assume the Role**

```bash
# From calling account
aws sts assume-role \
  --role-arn arn:aws:iam::TARGET-ACCOUNT:role/CrossAccountReadRole \
  --role-session-name my-session \
  --external-id my-unique-external-id-12345 \
  --duration-seconds 3600

# Export temporary credentials
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=...

# Test
aws s3 ls  # Works (ReadOnly)
aws s3 mb s3://test-bucket  # Fails (ReadOnly)
```

**Step 5 — Test MFA Condition**

```json
// Add MFA condition to a sensitive policy
{
  "Effect": "Deny",
  "Action": ["iam:*", "organizations:*"],
  "Resource": "*",
  "Condition": {
    "BoolIfExists": {"aws:MultiFactorAuthPresent": "false"}
  }
}
```

**Step 6 — Run IAM Access Analyzer**

Console: IAM → Access Analyzer → Create analyzer (your account or organization)
Review findings: any S3 buckets/roles/KMS keys with external access.

**Cleanup:**

```bash
aws iam detach-user-policy --user-name dev-alice --policy-arn ...
aws iam delete-user --user-name dev-alice
aws iam delete-role --role-name CrossAccountReadRole
aws iam delete-policy --policy-arn arn:aws:iam::ACCOUNT:policy/DevBoundary
```
