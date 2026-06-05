# Detective Controls — GuardDuty, Macie, Inspector, Security Hub, CloudTrail, Config — SAP-C02 Deep Dive

---

## 1. The Problem

You can deploy perfect preventive controls — WAF, NACLs, SCPs — and still be breached. Attack techniques evolve, misconfigurations slip in, insiders act maliciously, and credentials get compromised. Without detective controls, breaches go undetected for an average of 200+ days.

**Visibility gaps that detective controls address:**
- A developer accidentally made an S3 bucket public
- A compromised EC2 instance is talking to a known command-and-control server
- An IAM access key is being used from an unusual country at 3 AM
- A Lambda function has an unpatched CVE with network reachability
- Someone modified a critical security group rule last Thursday
- Your RDS instance contains 50,000 credit card numbers in plaintext

Each of these requires a different detection mechanism.

---

## 2. Overview — Which Service Detects What

| Service | What It Detects | Data Sources | Response |
|---|---|---|---|
| **GuardDuty** | Active threats, anomalous behavior | VPC Flow Logs, CloudTrail, DNS, EKS, RDS, Lambda, S3 | EventBridge → Lambda |
| **Macie** | PII/sensitive data in S3 | S3 object content | EventBridge → Lambda |
| **Inspector** | CVEs, software vulnerabilities | EC2 packages, ECR images, Lambda code | Findings → Security Hub |
| **Security Hub** | Aggregated findings + compliance | All above + Config + IAM Access Analyzer | Cross-account dashboard |
| **CloudTrail** | All API call history | Management/data plane events | Logs → S3/CloudWatch |
| **Config** | Configuration history + compliance | AWS resource configuration state | Remediation via SSM |
| **IAM Access Analyzer** | External/unintended resource sharing | IAM, S3, KMS, SQS, Lambda, Secrets Manager | Findings dashboard |

---

## 3. Amazon GuardDuty

### What It Is

ML-powered **threat detection** service. Continuously monitors for malicious or unauthorized behavior across your AWS environment. You do not configure what to detect — AWS ML models and threat intelligence do that.

### Data Sources

| Source | What It Detects |
|---|---|
| **VPC Flow Logs** | Unusual traffic patterns, port scanning, crypto mining |
| **CloudTrail Management Events** | Unusual API calls, credential abuse |
| **CloudTrail S3 Data Events** | Suspicious S3 access patterns |
| **DNS Logs** | Queries to known malicious domains, DNS exfiltration |
| **EKS Audit Logs** | Kubernetes attack patterns (privileged containers, etc.) |
| **RDS Login Events** | Brute force, unusual login locations |
| **Lambda Network Activity** | Lambda calling unusual external endpoints |
| **EBS Malware Scan** | Known malware signatures in EBS volumes |
| **Runtime Monitoring (EKS/EC2/ECS)** | Process-level behavioral detection |

**Important:** GuardDuty does NOT require you to enable VPC Flow Logs or CloudTrail separately — it accesses these independently. However, enabling them is still recommended for your own use.

### Finding Categories and Examples

| Finding Category | Example Finding |
|---|---|
| **Backdoor** | `Backdoor:EC2/C&CActivity.B` — EC2 communicating with known C2 |
| **Behavior** | `Behavior:EC2/TrafficVolumeUnusual` — unusual outbound traffic |
| **Credential Access** | `CredentialAccess:IAMUser/AnomalousBehavior` |
| **Exfiltration** | `Exfiltration:S3/ObjectRead.Unusual` |
| **Impact** | `Impact:EC2/BitcoinDomainRequest.Reputation` — crypto mining |
| **Initial Access** | `UnauthorizedAccess:EC2/SSHBruteForce` |
| **Discovery** | `Discovery:S3/MaliciousIPCaller` |
| **Recon** | `Recon:EC2/PortProbeUnprotectedPort` |

### Severity Levels

| Severity | Range | Meaning |
|---|---|---|
| Critical | 9.0–10.0 | Immediate action required |
| High | 7.0–8.9 | Likely malicious |
| Medium | 4.0–6.9 | Suspicious, investigate |
| Low | 1.0–3.9 | Informational |

### Multi-Account (Organization) Setup

```
Organizations Management Account or delegated admin
        |
  GuardDuty delegated administrator account (security account)
        |
  Auto-enable new accounts
        |
  Centralized findings across all member accounts
  (can be suppressed/filtered per account)
```

- Must enable in **each region separately** (not global)
- 30-day free trial per account per region
- Per-finding pricing after trial

### EventBridge Automated Remediation

```
GuardDuty Finding
      |
EventBridge Rule (pattern: source=aws.guardduty, detail-type=GuardDuty Finding)
      |
Lambda Function:
  if finding.type == "UnauthorizedAccess:EC2/SSHBruteForce":
      block source IP in Security Group
  if finding.type == "Backdoor:EC2/C&CActivity":
      isolate instance (remove from SG, add quarantine SG)
      snapshot EBS volumes for forensics
      notify via SNS
```

---

## 4. Amazon Macie

### What It Is

ML-powered **sensitive data discovery** for Amazon S3. Identifies PII, financial data, credentials, and custom-defined patterns in S3 objects.

### How It Works

```
Macie → scans S3 buckets
     → uses ML pattern matching + custom identifiers
     → generates findings:
        - SensitiveData:S3Object/Personal (SSN, passports, DOB)
        - SensitiveData:S3Object/Financial (credit card numbers, IBAN)
        - SensitiveData:S3Object/Credentials (API keys, passwords)
        - SensitiveData:S3Object/CustomIdentifier
     → EventBridge → Lambda/SNS for automated response
```

### Managed vs Custom Identifiers

| Type | Description |
|---|---|
| **Managed identifiers** | AWS-defined: SSN, credit cards, passport numbers, AWS credentials, etc. |
| **Custom identifiers** | Your regex + keywords (e.g., your internal employee ID format) |

### Multi-Account

- Delegated administrator account sees all member account S3 findings
- Auto-enable for new accounts in org

### Pricing

- Per S3 bucket inventory: $0.10/bucket/month
- Per GB data scanned (on-demand or automated): varies by region

---

## 5. Amazon Inspector

### What It Is

Automated **vulnerability scanning** service for:
- EC2 instances (OS packages, software CVEs)
- Amazon ECR container images (package CVEs)
- AWS Lambda functions (code dependencies CVEs)
- Network reachability (open ports, security group paths)

### How It Works

```
EC2 Instance
    |
SSM Agent (required!) → Inspector Agent-less scan
    |
Inspector scans:
  - Installed packages against CVE database
  - Network configuration (reachability from internet)
  → Risk Score = CVSS score × reachability factor

ECR Registry → Image push trigger → Inspector scans image layers
Lambda Function → Inspector scans code dependencies
```

**Critical:** Inspector requires **SSM Agent** on EC2 instances. Instances without SSM Agent cannot be scanned.

### Finding Prioritization

```
Inspector Risk Score = CVSS Base Score × Context Modifiers
Context: Is port 22 reachable from internet? Is there public IP?
         Higher score = CVE is reachable = more urgent

vs. CVSS 9.8 CVE on instance with no network reachability → lower actual risk
```

### Inspector vs GuardDuty

| Feature | Inspector | GuardDuty |
|---|---|---|
| Type | Vulnerability assessment (static) | Threat detection (behavioral/active) |
| Timing | Continuous scanning of software | Real-time event analysis |
| Detects | Known CVEs, config weaknesses | Active attacks, anomalous behavior |
| Source | Package inventory, code | VPC Flow Logs, CloudTrail, DNS |
| SSM Agent | Required for EC2 | Not required |

---

## 6. AWS Security Hub

### What It Is

**Centralized security posture management** — aggregates findings from multiple services and checks compliance against security standards.

### Input Sources

```
GuardDuty findings ─────────┐
Macie findings ─────────────┤
Inspector findings ──────────┤
IAM Access Analyzer ─────────┤──> Security Hub ──> Dashboard + Automation
AWS Config rules ────────────┤        |
Firewall Manager ────────────┤    Security Standards Checks
3rd party tools (crowdstrike)┘    (CIS, PCI DSS, NIST, AWS Foundational)
```

### Security Standards

| Standard | Description |
|---|---|
| **AWS Foundational Security Best Practices** | 200+ controls across all services |
| **CIS AWS Foundations Benchmark** | Center for Internet Security Level 1 & 2 |
| **PCI DSS** | Payment Card Industry Data Security Standard |
| **NIST SP 800-53** | US Government security framework |
| **SOC 2** | Service Organization Control 2 |

Each standard runs **Config rules** under the hood — requires Config enabled.

### Cross-Account + Cross-Region

```
Member accounts → aggregate findings to Security Hub admin account
Member regions → aggregate to home region via finding aggregator
```

One console view of all findings across 100 accounts × 5 regions.

### Automated Remediation

```
Security Hub Finding
      |
EventBridge (Security Hub finding) → Step Functions or Lambda
      |
Example: "S3 bucket public access blocked = FAILED"
      → Lambda: enable S3 Block Public Access
      → Close finding as RESOLVED
```

### Pricing

- Per security check: $0.0010/check/account/month (first 100K free)
- Per finding ingested: $0.00003/finding
- Per region separately

---

## 7. AWS CloudTrail

### What It Is

The **complete audit log** of every AWS API call made in your account. Who made it, what they did, when, from where, and what happened.

### Event Types

| Type | Default | Cost | Examples |
|---|---|---|---|
| **Management events** | Yes, enabled by default | Free (first copy) | CreateBucket, RunInstances, AttachPolicy |
| **Data events** | No — opt-in | Extra cost (~$0.10/100K events) | S3:GetObject, S3:PutObject, Lambda:Invoke, DynamoDB GetItem |
| **Insights events** | No — opt-in | Extra cost | Anomalous API activity detection |

### CloudTrail Record Structure

```json
{
  "eventVersion": "1.08",
  "userIdentity": {
    "type": "IAMUser",
    "principalId": "AIDAEXAMPLE",
    "arn": "arn:aws:iam::123456789:user/alice",
    "accountId": "123456789"
  },
  "eventTime": "2024-01-15T14:30:00Z",
  "eventSource": "s3.amazonaws.com",
  "eventName": "DeleteBucket",
  "sourceIPAddress": "203.0.113.10",
  "userAgent": "aws-cli/2.15.0",
  "requestParameters": {"bucketName": "my-critical-bucket"},
  "responseElements": null,
  "errorCode": "AccessDenied",      // present if failed
  "errorMessage": "Access Denied",
  "awsRegion": "us-east-1",
  "requestID": "EXAMPLE123"
}
```

### Multi-Region Trail vs Organization Trail

```
Multi-Region Trail:
  - Single trail captures events from ALL regions
  - Stored in one S3 bucket
  - Required for complete coverage

Organization Trail:
  - Created in management account
  - Automatically applies to all member accounts
  - Member accounts cannot delete or modify it
  - Stored in management account's S3 bucket
```

### CloudTrail Log Integrity

- **SHA-256 hash** of each log file
- **RSA-SHA256 digital signature** of digest files
- Digest files stored separately in S3
- Validate with: `aws cloudtrail validate-logs`
- Detects: deletion, modification, or tampering of log files

### CloudTrail Insights

Detects **unusual API activity** patterns:
- Unusual volume of API calls (e.g., 1,000 `DeleteSecurityGroup` calls in 10 minutes)
- Unusual error rate patterns
- Generates Insight events → stored in S3/CloudWatch → EventBridge

### CloudTrail Lake

- **Managed data lake** for CloudTrail events
- SQL-queryable (no need to set up Athena + Glue)
- Retention: 7 years
- Query across accounts/regions in one place
- More expensive than S3+Athena but simpler

```sql
-- CloudTrail Lake SQL query: find all root account logins in 30 days
SELECT eventTime, sourceIPAddress, userAgent
FROM aws_cloudtrail_events
WHERE userIdentity.type = 'Root'
  AND eventName = 'ConsoleLogin'
  AND eventTime > DATE_ADD('day', -30, NOW())
ORDER BY eventTime DESC
```

---

## 8. AWS Config

### What It Is

Continuous **configuration recording and compliance checking** for AWS resources. Answers: "What did this resource look like at any point in time?" and "Is it compliant with my rules?"

### How It Works

```
AWS Resource change detected
        |
Config Recorder captures configuration snapshot
        |
Config Rule evaluated (Was this change compliant?)
        |
Config Item stored in S3 (history of all config states)
        |
Non-compliant → SSM Automation (auto-remediation) OR manual
```

### Config Rules

| Type | Description | Example |
|---|---|---|
| **AWS Managed Rules** | 200+ pre-built rules | `s3-bucket-public-read-prohibited` |
| **Custom Lambda Rules** | Your compliance logic | Check your own tagging standard |
| **Proactive rules** | Check before resource creation (CloudFormation hooks) | Block non-compliant resource creation |

### Common Managed Config Rules (Exam Favorites)

| Rule | Checks |
|---|---|
| `s3-bucket-public-read-prohibited` | No S3 public read |
| `encrypted-volumes` | EBS volumes encrypted |
| `restricted-ssh` | No SG allowing SSH from 0.0.0.0/0 |
| `iam-root-access-key-check` | Root has no access keys |
| `mfa-enabled-for-iam-console-access` | MFA on IAM users with console access |
| `cloudtrail-enabled` | CloudTrail active |
| `rds-storage-encrypted` | RDS encrypted at rest |
| `vpc-flow-logs-enabled` | VPC flow logs active |

### Remediation

```
Config Rule: encrypted-volumes → NON_COMPLIANT
        |
Remediation Action:
  Type: AUTOMATIC (SSM Automation document)
  Document: AWS-EncryptEC2Volume
  Parameters: KMSKeyId=alias/my-key
  MaximumAutomaticAttempts: 3
        |
SSM executes encryption snapshot + re-attach
```

### Conformance Packs

A collection of Config rules + remediation actions deployed as a unit.

```
Conformance Pack: "OperationalBestPractices-PCI-DSS"
  Contains 30+ Config rules for PCI DSS compliance
  Deployed to all accounts in org via Config aggregator
```

### Config Aggregator

```
Config Aggregator (in security account):
  └── Aggregates Config data from all accounts in org
  └── Single view of compliance status
  └── Cost: per configuration item recorded in each account
```

### Config vs CloudTrail

| Feature | Config | CloudTrail |
|---|---|---|
| Tracks | Resource configuration state | API calls / actions |
| Answers | What did it look like? Is it compliant? | Who did what, when? |
| Granularity | Config changes | Every API call |
| Cost | Per config item | Per event |
| Retention | S3 (your control) | S3 (your control), Lake (7yr) |

---

## 9. IAM Access Analyzer

### What It Does

Finds resources that are **accessible from outside your AWS account or organization** — "unintended external sharing".

### Analyzed Resource Types

| Resource | Detects |
|---|---|
| S3 buckets | Public access, cross-account access |
| IAM roles | Cross-account trust relationships |
| KMS keys | Cross-account key usage |
| Lambda functions | Cross-account resource-based policy |
| SQS queues | Cross-account access |
| Secrets Manager secrets | Cross-account access |

### Zone of Trust

- **Account analyzer:** Findings for any access from outside the account
- **Organization analyzer:** Findings only for access from outside the organization (no alerts for within-org)

### Policy Validation + Generation

```
Policy Validation:
  Paste IAM policy → Access Analyzer checks for:
  - Errors (invalid JSON, invalid actions)
  - Warnings (overly permissive, security suggestions)
  - General recommendations

Policy Generation:
  Enable CloudTrail → Access Analyzer analyzes actual API calls →
  Generates least-privilege policy based on what was actually used
  (great for tightening policies in production)
```

---

## 10. Key Config & Limits

| Parameter | Value |
|---|---|
| GuardDuty: free trial | 30 days per account per region |
| GuardDuty: multi-account | Delegated admin via Organizations |
| GuardDuty: regions | **Per-region — must enable in each region** |
| CloudTrail: management events | First copy free; additional copies charged |
| CloudTrail: data events cost | ~$0.10/100K events |
| CloudTrail: log retention in S3 | Your S3 lifecycle policy |
| CloudTrail Lake retention | Up to 7 years |
| Config: pricing | $0.003/configuration item recorded |
| Config: rules | $0.001/rule evaluation |
| Security Hub: checks | $0.0010/check/month |
| Inspector: EC2 | Requires SSM Agent |
| Macie: bucket inventory | $0.10/bucket/month |

---

## 11. Decision Tree — Which Detective Service?

```
What are you trying to detect?

├── Active threat / ongoing attack / behavioral anomaly?
│   └── GuardDuty

├── Sensitive data (PII, credit cards) in S3?
│   └── Macie

├── Known CVEs / software vulnerabilities in EC2/ECR/Lambda?
│   └── Inspector

├── Resource sharing outside account/org (S3 public, role with external trust)?
│   └── IAM Access Analyzer

├── Who made this API call? What happened? Audit trail?
│   └── CloudTrail

├── What did this resource look like last Tuesday? Is it compliant?
│   └── Config

├── Unified view of all findings + compliance score across 100 accounts?
│   └── Security Hub (aggregates all of the above)

├── Anomalous API activity patterns (bulk deletions)?
│   └── CloudTrail Insights
```

---

## 12. Common Patterns

### Pattern 1: Automated Incident Response

```
GuardDuty: EC2 instance communicating with C2 server
      |
EventBridge rule: GuardDuty finding type = Backdoor:EC2/C&CActivity
      |
Step Functions state machine:
  1. Snapshot EBS volume (forensics)
  2. Add instance to quarantine security group (no egress)
  3. Remove instance from load balancer
  4. Create SNS notification to security team
  5. Create Jira ticket via Lambda + API
```

### Pattern 2: S3 Compliance Pipeline

```
S3 bucket created / modified
      |
Config rule: s3-bucket-public-read-prohibited
      |
NON_COMPLIANT triggered
      |
Config Remediation → Lambda → enable S3 block public access
      |
Macie: scheduled scan → find any PII already uploaded
      |
Security Hub: consolidated finding with severity
```

### Pattern 3: GuardDuty + Macie Multi-Account

```
AWS Organizations
      |
Security Account (Delegated Admin for GuardDuty + Macie)
      |
GuardDuty:
  - Centralizes findings from 100 member accounts
  - Suppression rules filter known-good findings
  - EventBridge in security account routes all findings to SIEM

Macie:
  - Centralized sensitive data findings across all accounts
  - S3 bucket inventory across organization
```

### Pattern 4: CloudTrail + Athena Investigation

```
Incident: S3 bucket made public
      |
CloudTrail S3 data events enabled
      |
Athena table over s3://cloudtrail-logs/
      |
SQL query:
SELECT eventTime, userIdentity.arn, sourceIPAddress, eventName
FROM cloudtrail_logs
WHERE bucketName = 'production-data'
  AND eventName IN ('PutBucketAcl', 'PutBucketPolicy', 'DeleteBucketPolicy')
ORDER BY eventTime DESC
LIMIT 50
```

---

## 13. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **GuardDuty per-region** | Disabled in one region = blind spot. Enable org-wide with auto-enable |
| **CloudTrail data events cost extra** | Management events are default/free. S3:GetObject, Lambda:Invoke = extra charge |
| **Config charges per item** | $0.003/config item × 1,000 resources × 30 days = real cost. Plan accordingly |
| **Security Hub per-region** | Like GuardDuty — must enable in each region. Use aggregator for cross-region |
| **Inspector needs SSM Agent** | EC2 without SSM Agent = not scanned. Ensure SSM Agent in AMI baseline |
| **CloudTrail not real-time** | Log delivery to S3 up to 15 minutes. For real-time, use CloudWatch Logs from CloudTrail |
| **Macie doesn't scan all file types** | Structured data (CSV, JSON, Parquet) scanned; some binary formats not analyzed |
| **IAM Access Analyzer org vs account** | Org analyzer: only external-to-org findings. Account analyzer: any external account = finding |
| **GuardDuty findings don't auto-remediate** | Findings are informational. You must wire EventBridge → Lambda for auto-remediation |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **Alert fatigue from GuardDuty** | Use suppression rules for known-good IPs/roles. Tune severity thresholds |
| **No CloudTrail on new accounts** | Organization trail covers new accounts automatically |
| **Config recording disabled** | Config rules don't evaluate without recorder running |
| **Security Hub findings duplicated** | Filter by AWS account and source to avoid double-counting |
| **Inspector CVEs without prioritization** | Use Inspector risk score (reachability × CVSS), not just CVSS |

---

## 14. Hands-On Lab (Free Tier)

### Goal: Enable GuardDuty + CloudTrail + Config + Security Hub, trigger test finding

**Step 1 — Enable GuardDuty**

```bash
# Enable in current region
aws guardduty create-detector --enable

# Note the DetectorId
DETECTOR_ID=$(aws guardduty list-detectors --query DetectorIds[0] --output text)
```

**Step 2 — Enable Multi-Region CloudTrail**

```bash
aws s3 mb s3://cloudtrail-logs-ACCOUNT-REGION

# Create multi-region trail
aws cloudtrail create-trail \
  --name org-trail \
  --s3-bucket-name cloudtrail-logs-ACCOUNT-REGION \
  --is-multi-region-trail \
  --enable-log-file-validation

aws cloudtrail start-logging --name org-trail

# Enable data events for S3 (extra cost — disable after lab)
aws cloudtrail put-event-selectors \
  --trail-name org-trail \
  --event-selectors '[{
    "ReadWriteType": "All",
    "IncludeManagementEvents": true,
    "DataResources": [{"Type": "AWS::S3::Object","Values": ["arn:aws:s3:::"]}]
  }]'
```

**Step 3 — Enable AWS Config**

```bash
# Create S3 bucket for Config
aws s3 mb s3://config-recordings-ACCOUNT-REGION

# Create Config recorder
aws configservice put-configuration-recorder \
  --configuration-recorder name=default,roleARN=arn:aws:iam::ACCOUNT:role/AWSConfigRole

# Create delivery channel
aws configservice put-delivery-channel \
  --delivery-channel name=default,s3BucketName=config-recordings-ACCOUNT-REGION

# Start recording
aws configservice start-configuration-recorder --configuration-recorder-name default

# Add a Config rule
aws configservice put-config-rule \
  --config-rule '{
    "ConfigRuleName": "s3-bucket-public-read-prohibited",
    "Source": {
      "Owner": "AWS",
      "SourceIdentifier": "S3_BUCKET_PUBLIC_READ_PROHIBITED"
    }
  }'
```

**Step 4 — Enable Security Hub**

```bash
aws securityhub enable-security-hub \
  --enable-default-standards  # Enables AWS Foundational Security Best Practices
```

**Step 5 — Trigger GuardDuty Test Finding**

```bash
# Generate sample findings (safe — doesn't create real threat)
aws guardduty create-sample-findings \
  --detector-id $DETECTOR_ID \
  --finding-types "UnauthorizedAccess:EC2/SSHBruteForce" "Backdoor:EC2/C&CActivity.B"

# List findings
aws guardduty list-findings \
  --detector-id $DETECTOR_ID \
  --query FindingIds

# Get finding details
aws guardduty get-findings \
  --detector-id $DETECTOR_ID \
  --finding-ids FINDING-ID
```

**Step 6 — Create EventBridge Rule for GuardDuty**

```bash
aws events put-rule \
  --name guardduty-high-severity \
  --event-pattern '{
    "source": ["aws.guardduty"],
    "detail-type": ["GuardDuty Finding"],
    "detail": {
      "severity": [{"numeric": [">=", 7]}]
    }
  }' \
  --state ENABLED

# Add SNS target for notifications
aws events put-targets \
  --rule guardduty-high-severity \
  --targets 'Id=1,Arn=arn:aws:sns:REGION:ACCOUNT:security-alerts'
```

**Step 7 — Query CloudTrail**

```bash
# Look up recent events for your user
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=Username,AttributeValue=your-username \
  --max-results 10 \
  --query 'Events[].{Time:EventTime,Event:EventName,Source:EventSource}'
```

**Cleanup:**

```bash
aws guardduty delete-detector --detector-id $DETECTOR_ID
aws cloudtrail stop-logging --name org-trail
aws cloudtrail delete-trail --name org-trail
aws configservice stop-configuration-recorder --configuration-recorder-name default
aws securityhub disable-security-hub
aws s3 rb s3://cloudtrail-logs-ACCOUNT-REGION --force
aws s3 rb s3://config-recordings-ACCOUNT-REGION --force
```
