# CloudTrail + AWS Config + Systems Manager

## 1. The Problem

Operational visibility and compliance have two distinct needs:

**Audit:** "Who changed this security group at 2 PM on Tuesday? Which IAM user made this API call? Why did this resource suddenly have public access?"
→ You need an immutable log of every API call.

**Compliance drift:** "Is our AWS environment still compliant with CIS benchmarks? Did someone enable S3 public access after we disabled it? Are all EC2 instances using encrypted volumes?"
→ You need continuous configuration assessment.

**Operations at scale:** "How do I patch 500 EC2 instances without SSH? How do I securely log into a Windows server without RDP port open? How do I manage secrets without hardcoding them?"
→ You need a unified operations platform.

Three services solve these problems: **CloudTrail** (audit), **AWS Config** (drift detection), **Systems Manager** (ops automation).

---

## 2. What AWS Built

| Service | Core Function |
|---------|--------------|
| **CloudTrail** | Record every AWS API call — who, what, when, from where |
| **AWS Config** | Record configuration snapshots + evaluate compliance rules |
| **Systems Manager** | Operational automation — patching, secrets, remote access, runbooks |

---

## 3. How It Works

### AWS CloudTrail

**Event Types:**
| Type | What it captures | Default logged? | Cost |
|------|-----------------|-----------------|------|
| **Management events** | API calls that manage AWS resources (CreateEC2, DeleteS3Bucket, PutBucketPolicy) | ✓ Yes | First copy to S3 free |
| **Data events** | Object-level operations (S3 GetObject/PutObject, Lambda Invoke, DynamoDB GetItem) | ✗ No | Extra cost — opt-in |
| **Insights events** | Anomalous API activity (unusual spike in DeleteBucket calls, unusual IAM actions) | ✗ No | Extra cost — opt-in |

**Single-Region vs Multi-Region vs Organization Trail:**
```
Single-region trail:  captures events in one region only
Multi-region trail:   captures events in ALL regions → required for complete audit
Organization trail:   captures events from ALL accounts in AWS Organization
                      management account creates it, applies to all member accounts

Best practice: Organization trail (multi-region) → single central S3 bucket
```

**Trail Storage Options:**
```
Amazon S3:
  - Primary storage (required for all trails)
  - Log files delivered every ~5 minutes
  - Separate file per region, per hour
  - Format: JSON gzipped

CloudWatch Logs:
  - Real-time streaming to CloudWatch log group
  - Use for: CloudWatch Alarms on specific API calls, Logs Insights queries

Amazon EventBridge:
  - Route CloudTrail events to Lambda, SQS, SNS in near-real-time
  - Use for: automated response to specific API actions
  Example: CloudTrail records "AuthorizeSecurityGroupIngress port 22 open to 0.0.0.0/0"
           → EventBridge → Lambda → reverts the change
```

**Log File Integrity:**
```
CloudTrail validates logs haven't been tampered with:
  Method: SHA-256 hash of each log file, RSA-2048 signature of digest files
  Digest files: created every hour, reference all log files in that hour

Validate from CLI:
  aws cloudtrail validate-logs \
    --trail-arn arn:aws:cloudtrail:us-east-1:123:trail/my-trail \
    --start-time 2024-01-15T00:00:00Z

Result: "No log files were modified, deleted, or forged"
```

**Protecting the CloudTrail S3 Bucket:**
```
1. MFA Delete — require MFA to delete bucket or objects
2. Versioning — recover deleted log files
3. S3 Object Lock — WORM, can't modify/delete even with root account
4. Bucket policy — deny DeleteObject, deny disabling logging
5. CloudTrail log file integrity validation — detect tampering

Bucket policy to prevent deletion:
{
  "Effect": "Deny",
  "Principal": "*",
  "Action": ["s3:DeleteObject", "s3:DeleteObjectVersion"],
  "Resource": "arn:aws:s3:::cloudtrail-bucket/*"
}
```

**CloudTrail Lake:**
```
New capability (2022+):
  - SQL-based event data store, retain up to 7 years
  - Query with SQL (no need to set up Athena separately)
  - Costs more than S3 storage but far easier to query

vs S3 + Athena:
  S3+Athena: cheaper, flexible, requires setup (partitioning, Glue catalog)
  Lake:      ready to query immediately, higher storage cost

Example query:
SELECT eventName, userIdentity.principalId, eventTime
FROM my_event_data_store
WHERE eventName = 'DeleteBucket'
  AND eventTime > '2024-01-01 00:00:00'
ORDER BY eventTime DESC
LIMIT 100;
```

**90-Day Free Event History:**
```
Without creating a trail, CloudTrail still shows the last 90 days of management events
in the console (Event History). This is free.
Creating a trail = persistent storage beyond 90 days = costs storage
```

---

### AWS Config

**Core Concept:** Config records the configuration of every AWS resource over time — like a "version control system" for your infrastructure.

**Configuration Item (CI):**
```
A snapshot of a resource's configuration at a point in time:
  - Resource attributes (type, ID, ARN, creation date)
  - Relationships (this SG is attached to these EC2 instances)
  - Configuration (all attribute values)
  - Metadata (when recorded, version number)

Config records CI whenever a resource changes
Config also records snapshot of ALL resources periodically (configurable)
```

**Config Rules:**
```
AWS Managed Rules (150+):
  - s3-bucket-public-read-prohibited
  - encrypted-volumes (all EBS must be encrypted)
  - required-tags (resources must have specific tags)
  - restricted-ssh (SG rules must not allow port 22 from 0.0.0.0/0)
  - iam-password-policy (password policy requirements)
  - rds-instance-public-access-check

Custom Rules (Lambda):
  Write your own Lambda function to evaluate any custom logic
  Triggered: on configuration change or periodically

Proactive Rules (CloudFormation Guard):
  Evaluate CloudFormation templates BEFORE deployment
  Prevent non-compliant resources from being created
```

**Rule Evaluation Triggers:**
```
Change-triggered: evaluates when resource configuration changes
  → Near-real-time (seconds after change)
  → Cost-efficient (only evaluates on actual changes)

Periodic: evaluates on schedule (1hr, 3hr, 6hr, 12hr, 24hr)
  → Good for: checks that don't depend on config changes (e.g., check external state)
  → Higher cost for frequent schedules (evaluates all matching resources)
```

**Remediation:**
```
Manual remediation:
  Config marks resource as NON_COMPLIANT
  Operator sees in Config console → manually fixes

Automatic remediation:
  Config rule → linked SSM Automation document
  When NON_COMPLIANT → automatically trigger SSM Automation
  Example: Bucket public access → trigger SSM runbook → disable public access

Example: restricted-ssh rule + auto-remediation:
  Rule detects: SG allows port 22 from 0.0.0.0/0
  Remediation: SSM Automation runs "AWS-RevokeSecurityGroupIngress"
  Result: port 22 rule removed without human intervention
```

**Conformance Packs:**
```
Pre-built collections of Config rules for specific frameworks:
  - CIS AWS Foundations Benchmark
  - PCI DSS
  - HIPAA
  - NIST 800-53
  - AWS Security Best Practices

Deploy to single account or across Organization via StackSets
One conformance pack = dozens of Config rules + remediation actions
```

**Config Aggregator:**
```
Multi-account, multi-region Config view:
  Create aggregator in management/security account
  Collects Config data from all member accounts + all regions
  Single console: view compliance across entire organization

Use: compliance dashboards, reporting, executive scorecards
Cost: Config charges per configuration item recorded + rule evaluation
     Aggregator itself is free (uses source account's Config data)
```

**CloudTrail vs Config Comparison:**
| | CloudTrail | AWS Config |
|-|------------|-----------|
| What it records | API calls (actions) | Resource configuration (state) |
| Question answered | Who did what, when? | What does this look like right now / over time? |
| Example | "Who created this SG?" | "Is this SG compliant? What did it look like last week?" |
| Compliance | API audit trail | Configuration compliance checking |
| Storage | S3 (event logs) | S3 (configuration history) |
| Evaluation | No rules | Config rules evaluate compliance |
| Retention | Configurable | 7 years (config history in S3) |

---

### AWS Systems Manager (SSM)

A collection of operational capabilities, not a single service:

**Parameter Store:**
```
Hierarchical secret/config storage:

/myapp/prod/database/password    → SecureString (KMS-encrypted)
/myapp/prod/database/host        → String
/myapp/prod/app/max-connections  → String

Tiers:
  Standard (free): 10,000 parameters, 4 KB max, no auto-expiry
  Advanced:        >10,000 parameters, 8 KB max, parameter policies (auto-expiry, notify on access)

SecureString: encrypted with KMS key (default or custom CMK)
Version history: every change creates new version, previous versions accessible

vs Secrets Manager:
  Parameter Store: cheaper, good for config + non-rotating secrets
  Secrets Manager: automatic rotation, $0.40/secret/month, built-in rotation for RDS/Redshift/DocumentDB
```

**Session Manager:**
```
Replaces SSH/RDP — no port 22/3389 needed, no bastion hosts

How it works:
  1. SSM Agent on EC2 (pre-installed on Amazon Linux 2, Amazon Linux 2023, Windows)
  2. Agent connects TO SSM (outbound HTTPS 443) — no inbound ports needed
  3. Browser-based terminal in AWS console OR CLI:
     aws ssm start-session --target i-1234567890abcdef0

Requirements:
  - EC2 IAM role: AmazonSSMManagedInstanceCore policy
  - SSM Agent: installed and running
  - VPC endpoint for SSM (if no internet access): com.amazonaws.region.ssm

Audit: every session logged to CloudTrail + optionally S3 and CloudWatch Logs
Security: no SSH keys to manage, no port 22 in security groups
```

**Patch Manager:**
```
Automated OS patching for EC2 and on-premises:

Patch Baselines:
  - Define: which patches to approve/reject
  - AWS provides pre-defined baselines per OS (Windows, Amazon Linux, Ubuntu, etc.)
  - Custom baselines: approve by severity (Critical, Important), product, classification

Maintenance Windows:
  - Define: when to patch (e.g., Sunday 2-4 AM)
  - Targets: EC2 instances by tag, resource group, or manual list
  - Max concurrency: % or count of instances to patch simultaneously
  - Max error threshold: abort if X% of patches fail

Patch compliance:
  Reports on: compliant, non-compliant, missing patches per instance
  Integrates with: Config (non-compliant = Config rule violation)
  Integrates with: Security Hub (patch compliance in security posture)
```

**Run Command:**
```
Execute commands across fleet without SSH:

aws ssm send-command \
  --document-name "AWS-RunShellScript" \
  --parameters '{"commands":["df -h","free -m","ps aux | head -20"]}' \
  --targets '[{"Key":"tag:Environment","Values":["prod"]}]' \
  --output text

→ Runs on all instances tagged Environment=prod simultaneously
→ Output available in SSM console or CloudWatch Logs
→ Completely logged to CloudTrail

Pre-built documents:
  AWS-RunShellScript       → run bash on Linux
  AWS-RunPowerShellScript  → run PowerShell on Windows
  AWS-ConfigureAWSPackage  → install/uninstall AWS packages
  AWS-ApplyPatchBaseline   → apply patches
```

**State Manager:**
```
Enforce desired state on EC2 instances:
  - Ensure CloudWatch Agent is always installed
  - Ensure specific software version is always present
  - Apply hardening configuration continuously

Creates: SSM Association (resource + document + schedule)
Re-applies if configuration drifts
Similar to Chef/Puppet but AWS-native
```

**Inventory:**
```
Collect software inventory from EC2:
  - Installed applications (name, version, install date)
  - AWS components
  - Network configuration
  - Windows updates
  - Custom inventory (you define what to collect)

Stored in: SSM Inventory data + optionally S3
Query with: Systems Manager → Inventory → Custom query (SQL-like)
```

**Automation:**
```
Runbooks for common operations:
  AWS-StopEC2Instance
  AWS-StartEC2Instance
  AWS-RestartEC2Instance
  AWS-CreateImage (AMI)
  AWS-UpdateLinuxAmi

Custom runbooks: YAML/JSON defining steps
Multi-step: Step 1: stop, Step 2: snapshot, Step 3: start
Integrates with: EventBridge, Config remediation, Lambda
```

**OpsCenter:**
```
Central issue tracking for operational problems:
  OpsItem: a record of an operational issue
  Created by: CloudWatch Alarms, Config rules, Security Hub, PagerDuty

Aggregates: related metrics, logs, resource config in one view
Runbooks: link SSM Automation runbooks directly to OpsItems
Use: operational dashboard during incidents
```

---

## 4. Key Config & Limits

| Service | Parameter | Value |
|---------|-----------|-------|
| CloudTrail | Event history (free) | 90 days, management events only |
| CloudTrail | Multi-region trail | Required for complete coverage |
| CloudTrail Lake | Retention | Up to 7 years |
| CloudTrail | Log delivery lag | ~5 minutes to S3 |
| Config | Managed rules available | 150+ |
| Config | Max rules per region | 500 (soft limit) |
| Config | Retention period | 7 years (config history) |
| Config | Charge | Per config item + per rule evaluation |
| SSM Parameter Store Standard | Max parameters | 10,000 |
| SSM Parameter Store Standard | Max value size | 4 KB |
| SSM Parameter Store Advanced | Max value size | 8 KB |
| SSM Session Manager | Session timeout | Configurable (idle timeout) |
| Patch Manager | Patch scan frequency | Configurable (minimum 30 min) |

---

## 5. Decision Tree

```
AUDIT: Who made this change?
└─ CloudTrail (API call log: who, what, when, from where)

COMPLIANCE: Is this resource configured correctly?
└─ AWS Config (configuration snapshot + rule evaluation)

ONGOING DRIFT: Alert when config changes from compliant state
└─ AWS Config rules (change-triggered) + remediation

INVESTIGATE SECURITY INCIDENT: What API calls happened?
└─ CloudTrail Event History or CloudTrail Lake

PATCH MANAGEMENT: Keep EC2 fleet up to date
└─ SSM Patch Manager + Maintenance Windows

REMOTE ACCESS: Access EC2 without SSH
└─ SSM Session Manager

SECRET STORAGE: Store app config and secrets
└─ SSM Parameter Store (simple) or Secrets Manager (rotation needed)

FLEET COMMANDS: Run command on 500 instances
└─ SSM Run Command

CloudTrail vs Config vs Security Hub vs GuardDuty:
  CloudTrail:   WHO did WHAT (API audit)
  Config:       WHAT is the configuration (resource state compliance)
  Security Hub: AGGREGATOR of findings from Config, GuardDuty, Inspector, Macie
  GuardDuty:    ML-based THREAT DETECTION (anomalous behavior, malware indicators)
```

---

## 6. Common Patterns

### Pattern 1: Detect and Auto-Remediate Public S3 Buckets
```
Config Rule: s3-bucket-public-read-prohibited
  Trigger: Configuration change on S3 buckets
  Evaluation: Check if bucket has public ACL or bucket policy allowing * principal

Auto-Remediation:
  SSM Automation: AWS-DisablePublicAccessToS3Bucket
  Action: Put Public Access Block, remove public ACL

Result:
  Developer accidentally makes bucket public (10:03 AM)
  Config detects change (10:03 AM, change-triggered)
  Auto-remediation fires: public access removed (10:04 AM)
  CloudTrail records both the original change and the remediation
```

### Pattern 2: CloudTrail + EventBridge Security Automation
```
CloudTrail captures: "AuthorizeSecurityGroupIngress" API call

EventBridge rule:
{
  "source": ["aws.ec2"],
  "detail-type": ["AWS API Call via CloudTrail"],
  "detail": {
    "eventName": ["AuthorizeSecurityGroupIngress"],
    "requestParameters": {
      "ipPermissions": {
        "items": {
          "ipRanges": {
            "items": {"cidrIp": ["0.0.0.0/0"]}
          }
        }
      }
    }
  }
}

Target: Lambda function
Action: Revoke the SG rule, send Slack alert, create Security Hub finding
```

### Pattern 3: SSM Parameter Store for Application Config
```python
# Application reads config from Parameter Store at startup:
import boto3

ssm = boto3.client('ssm')

def get_config():
    params = ssm.get_parameters_by_path(
        Path='/myapp/prod/',
        Recursive=True,
        WithDecryption=True  # decrypt SecureString values
    )
    config = {}
    for param in params['Parameters']:
        key = param['Name'].split('/')[-1]  # get last path component
        config[key] = param['Value']
    return config

config = get_config()
db_password = config['database_password']  # decrypted SecureString
db_host = config['database_host']           # plain String
```

### Pattern 4: Compliance Reporting with Config Aggregator
```
Org-level Config Aggregator (in Security account):

Dashboard shows:
  Total resources: 45,234
  Compliant:       43,891 (97.0%)
  Non-compliant:   1,343  (3.0%)

Non-compliant breakdown by rule:
  encrypted-volumes:              423 instances
  s3-bucket-versioning-enabled:   198 buckets
  required-tags:                  512 resources
  restricted-ssh:                 89 security groups
  iam-password-policy:            121 accounts

Export: generate PDF report for auditors
Link: each non-compliant resource linked to auto-remediation
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Config charges per item + rule evaluation** | Every configuration change generates a billable config item. In a busy environment with hundreds of auto-scaling events, Config costs add up. Use org aggregator to avoid running Config in every account independently. |
| **CloudTrail data events are expensive at scale** | S3 data events log EVERY GetObject/PutObject. For an S3 bucket with millions of requests/day, data events cost can exceed $100/month. Enable selectively on sensitive buckets only. |
| **SSM Session Manager needs agent + role + (VPC endpoint)** | Three requirements: (1) SSM agent installed and running, (2) EC2 IAM role with AmazonSSMManagedInstanceCore, (3) if private subnet with no internet, VPC endpoint for SSM. Miss any one = Session Manager fails. |
| **Config rules: periodic vs change-triggered** | Change-triggered rules are cheaper — only evaluate on actual resource changes. Periodic rules evaluate ALL matching resources on schedule (expensive for large environments). |
| **Multi-region trail is not default** | A single-region trail misses events in other regions (including global service events in us-east-1). Always create multi-region trail for real audit coverage. |
| **CloudTrail log delivery lag** | Logs arrive in S3 ~5-15 minutes after the API call. For real-time response, use EventBridge (delivers in seconds). |
| **SSM Parameter Store SecureString requires KMS decrypt permission** | The IAM role accessing SecureString parameters must have `kms:Decrypt` permission on the KMS key used for encryption. Easy to miss. |
| **Config does not record all resource types** | Not every AWS resource type is supported by Config. Check supported resource types before relying on Config for compliance coverage of newer services. |
| **CloudTrail is regional but global services log to us-east-1** | IAM, STS, CloudFront, Route 53 — these are global services, and their CloudTrail events are logged in us-east-1. A trail in us-west-2 misses IAM changes without the "Include global service events" setting. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Enable CloudTrail, create a Config rule, and use SSM Session Manager.

### Step 1: Create a Multi-Region CloudTrail
```
CloudTrail Console → Create trail
  Trail name: org-trail-lab
  Storage location: New S3 bucket (auto-created)
  CloudWatch Logs: Enable (create new log group)
  Log file SSE-KMS encryption: Enable (create new KMS key)
  Enable for all regions: Yes (critical!)
  Management events: All (Read + Write)
  Data events: None (cost control for lab)
  Insights: None (lab)

→ Create trail → Status: Logging
```

### Step 2: Generate Some CloudTrail Events
```bash
# These AWS API calls will appear in CloudTrail:
aws s3 mb s3://cloudtrail-lab-test-$(date +%s)
aws s3 ls
aws iam list-users
aws ec2 describe-instances

# Wait 10-15 minutes, then check:
CloudTrail Console → Event history
  Filter by: User name = your IAM user
  → See all the API calls above
```

### Step 3: Enable AWS Config
```
Config Console → Get started
  Recording: All resource types (or select specific for cost control)
  S3 bucket: Create new
  SNS topic: Create new → email subscription

→ Confirm SNS email → Config starts recording
```

### Step 4: Create a Config Rule
```
Config Console → Rules → Add rule
  AWS managed rule: restricted-ssh
  Description: Check no SG allows port 22 from 0.0.0.0/0
  Trigger: Configuration changes
  → Save

Wait for evaluation → Check: compliant vs non-compliant resources
```

### Step 5: Create a Deliberately Non-Compliant Resource
```bash
# Create SG with port 22 open (will trigger Config rule):
aws ec2 create-security-group \
  --group-name "lab-bad-sg" \
  --description "Deliberately non-compliant SG"

SG_ID=$(aws ec2 describe-security-groups \
  --filters Name=group-name,Values=lab-bad-sg \
  --query 'SecurityGroups[0].GroupId' --output text)

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp --port 22 --cidr 0.0.0.0/0

# Wait 2-5 minutes → Config console → restricted-ssh rule
# → Shows lab-bad-sg as NON_COMPLIANT
```

### Step 6: Use SSM Session Manager
```
EC2: Launch t2.micro with Amazon Linux 2023
  IAM Role: attach AmazonSSMManagedInstanceCore policy

Wait 2-3 minutes for SSM agent to register

Systems Manager Console → Session Manager → Start session
  Select: your EC2 instance → Start session
  → Browser-based terminal, no SSH needed
  → Run: whoami, df -h, free -m

Check CloudTrail: should see "StartSession" event
```

### Step 7: Clean Up
```bash
# Delete Config rule → Disable Config recording
# Delete CloudTrail trail → Delete S3 bucket
aws ec2 delete-security-group --group-id $SG_ID
# Terminate EC2 instance
```

---

## Summary Reference Card

```
CLOUDTRAIL:
  Records: every AWS API call (who, what, when, from where)
  Event types: Management (free first copy) | Data (extra cost) | Insights (extra cost)
  Always use: multi-region trail + org trail for complete coverage
  Protect: S3 MFA delete + versioning + bucket policy deny delete
  90-day free: Event History in console (no trail needed)

AWS CONFIG:
  Records: resource configuration over time (version control for infra)
  Rules: 150+ managed + custom Lambda + proactive CloudFormation Guard
  Remediation: manual or automatic (SSM Automation)
  Aggregator: multi-account/region compliance dashboard
  Cost: per config item + per rule evaluation

SYSTEMS MANAGER:
  Parameter Store: hierarchical config/secrets, free Standard (10K, 4KB)
  Session Manager: browser terminal, no port 22 needed
  Patch Manager: automated patching, patch baselines, maintenance windows
  Run Command: fleet commands without SSH
  State Manager: enforce desired state continuously

DECISION:
  Who changed it? → CloudTrail
  Is it compliant? → AWS Config
  Patch/access/automate? → Systems Manager
```
