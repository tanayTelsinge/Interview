# Encryption on AWS — KMS, CloudHSM, ACM, Secrets Manager — SAP-C02 Deep Dive

---

## 1. The Problem

Encrypting data sounds simple, but at scale it creates hard problems:

- **Key distribution:** How do you securely give each application its encryption key?
- **Key rotation:** If you rotate keys, you must re-encrypt all existing data — impractical for petabytes
- **Key access control:** Who can decrypt? How do you audit key usage?
- **Hardware compliance:** Some regulations require keys never touch software HSMs (FIPS 140-2 Level 3)
- **Secret sprawl:** Database passwords hardcoded in repos, never rotated, shared across teams

AWS built a layered answer: envelope encryption for efficiency, KMS for key management, CloudHSM for hardware compliance, ACM for TLS automation, and Secrets Manager for credential lifecycle management.

---

## 2. Envelope Encryption — The Core Concept

Encrypting large datasets directly with a KMS key is slow (KMS has throughput limits) and requires sending all your data to AWS. Envelope encryption solves this:

```
┌─────────────────────────────────────────────────────────┐
│                  Envelope Encryption                    │
│                                                         │
│  1. GenerateDataKey(CMK) → returns:                     │
│     - Plaintext data key (use to encrypt your data)     │
│     - Encrypted data key (encrypted with CMK)           │
│                                                         │
│  2. Encrypt data locally with plaintext data key        │
│                                                         │
│  3. Store encrypted data + encrypted data key together  │
│     (discard plaintext data key from memory)            │
│                                                         │
│  To decrypt:                                            │
│  1. Call Decrypt(encrypted data key) → plaintext key    │
│  2. Decrypt data locally with plaintext key             │
└─────────────────────────────────────────────────────────┘
```

**Why envelope encryption?**
- Only the small data key is sent to KMS — not your data
- CMK never leaves KMS (for KMS-generated keys)
- Key rotation only requires re-encrypting the data key (not the data itself)
- High throughput: local symmetric encryption is fast

---

## 3. AWS KMS — Key Management Service

### Key Types

| Key Type | Cost | Rotation | Control | Use Case |
|---|---|---|---|---|
| **AWS Managed Key** | **Free** | Automatic every 3 years (was 1yr pre-2023) | None — AWS manages | Default for AWS services (S3, EBS, RDS) |
| **Customer Managed CMK** | **$1/month/key** | Automatic annual (opt-in) or manual | Full control | Custom key policies, cross-account, grants |
| **AWS Owned Key** | Free | AWS-managed | None | Used internally by some services (invisible to you) |

### Key Material Sources

| Source | Notes |
|---|---|
| **KMS-generated** | AWS generates in HSMs; key material never exported |
| **Imported** | You provide key material; you manage rotation manually; reimport to rotate |
| **CloudHSM-backed** | Key material in your CloudHSM cluster; KMS acts as proxy |

### Key Policy (Primary Access Control)

Every KMS key has a **key policy** — without it, no IAM policy can grant access.

**Root account statement (required):**
```json
{
  "Effect": "Allow",
  "Principal": {"AWS": "arn:aws:iam::ACCOUNT:root"},
  "Action": "kms:*",
  "Resource": "*"
}
```
This statement allows IAM policies in the account to work. Without it, only key policy controls access.

**Separation of duties — Manage vs Use:**
```json
// Key administrators (manage the key, cannot use it to encrypt)
{
  "Effect": "Allow",
  "Principal": {"AWS": "arn:aws:iam::ACCOUNT:role/KeyAdmins"},
  "Action": ["kms:Create*", "kms:Describe*", "kms:Enable*", "kms:List*",
             "kms:Put*", "kms:Update*", "kms:Revoke*", "kms:Disable*",
             "kms:Get*", "kms:Delete*", "kms:ScheduleKeyDeletion", "kms:CancelKeyDeletion"],
  "Resource": "*"
},
// Key users (use the key to encrypt/decrypt, cannot manage)
{
  "Effect": "Allow",
  "Principal": {"AWS": "arn:aws:iam::ACCOUNT:role/AppRole"},
  "Action": ["kms:Encrypt", "kms:Decrypt", "kms:ReEncrypt*",
             "kms:GenerateDataKey*", "kms:DescribeKey"],
  "Resource": "*"
}
```

### KMS Core APIs

| API | What It Does | Returns |
|---|---|---|
| `Encrypt` | Encrypt small data (max 4KB) with CMK directly | CiphertextBlob |
| `Decrypt` | Decrypt CiphertextBlob | Plaintext |
| `GenerateDataKey` | Envelope encryption — get plaintext + encrypted data key | Both keys |
| `GenerateDataKeyWithoutPlaintext` | Get only encrypted data key (defer decryption) | Encrypted key only |
| `ReEncrypt` | Re-encrypt ciphertext under different CMK (never exposes plaintext) | New CiphertextBlob |
| `DescribeKey` | Key metadata, rotation status, key state | Key metadata |
| `CreateKey` | Create new CMK | Key ARN |

### Automatic Key Rotation

- **Symmetric CMKs:** Automatic rotation once per year (opt-in)
- Rotation creates new key material, keeps old material for decrypting old ciphertexts
- Key ARN and alias stay the same — applications don't change
- **Does NOT work for:** asymmetric keys, HMAC keys, imported key material, CloudHSM-backed

### KMS Grants

Temporary, programmatic delegation of key permissions:
```bash
aws kms create-grant \
  --key-id arn:aws:kms:... \
  --grantee-principal arn:aws:iam::ACCOUNT:role/ServiceRole \
  --operations Decrypt GenerateDataKey \
  --retiring-principal arn:aws:iam::ACCOUNT:role/GrantAdmin
```

Use case: Allow a specific Lambda invocation to use a key without modifying key policy.

### Multi-Region Keys

```
Primary key: arn:aws:kms:us-east-1:ACCOUNT:key/mrk-XXXXX
Replica key: arn:aws:kms:eu-west-1:ACCOUNT:key/mrk-XXXXX (same key ID prefix mrk-)

Use case: Encrypt in us-east-1, decrypt in eu-west-1 (disaster recovery, global apps)
```

- Same key material in all regions
- Independent key policies per region
- Replicas can be promoted to primary

### Cross-Account KMS Access

Two things BOTH required:
1. Key policy allows the external account/role
2. IAM policy in external account allows `kms:*` actions on the key ARN

```json
// Key policy (in key owner's account)
{
  "Effect": "Allow",
  "Principal": {"AWS": "arn:aws:iam::OTHER-ACCOUNT:role/ConsumerRole"},
  "Action": ["kms:Decrypt", "kms:GenerateDataKey"],
  "Resource": "*"
}
// ConsumerRole's IAM policy also needs kms:Decrypt on the key ARN
```

---

## 4. AWS CloudHSM

### What It Is

A **dedicated Hardware Security Module** in your VPC. You own and manage the cryptographic keys — AWS has no access.

```
Your VPC
  ├── App Server → CloudHSM Client → CloudHSM Cluster
  │                                        ├── HSM (AZ-a) ──┐
  │                                        └── HSM (AZ-b) ──┘ synchronized
  └── (Keys never leave hardware)
```

### CloudHSM vs KMS

| Feature | KMS CMK | CloudHSM |
|---|---|---|
| Key custody | AWS manages HSMs | You own keys, AWS no access |
| FIPS compliance | FIPS 140-2 Level 2 | **FIPS 140-2 Level 3** |
| Cost | $1/key/month | **~$1.60/HSM/hour** (~$1,150/month per HSM) |
| HA setup | Built-in | Requires cluster of 2+ HSMs |
| Integration | Native AWS service integration | Via PKCS#11, JCE, Microsoft CNG |
| Performance | Managed | Higher throughput for crypto ops |
| Key types | Symmetric + Asymmetric | Full crypto library |
| Use case | General key management | Compliance, BYOK, code signing, CA |

### CloudHSM as KMS Custom Key Store

```
KMS API calls → Custom Key Store → CloudHSM Cluster
                (key material lives in CloudHSM, not KMS)
```

Benefits: AWS service integration (S3, EBS) + key material in your HSM.

### When to Use CloudHSM

- Regulatory requirement: FIPS 140-2 Level 3 (PCI HSM, HIPAA custom)
- Must own key material (BYOK at hardware level)
- Need offload TLS/asymmetric operations at high performance
- Oracle TDE (Transparent Data Encryption) with CloudHSM
- CA (Certificate Authority) private key protection

---

## 5. AWS Certificate Manager (ACM)

### What It Is

Free public SSL/TLS certificate provisioning and auto-renewal for AWS services.

```
Public internet traffic
         |
      HTTPS (443)
         |
     CloudFront / ALB / API Gateway / AppSync
         |
    ACM Certificate (auto-renewed 60 days before expiry)
```

### Key Characteristics

| Feature | Value |
|---|---|
| Cost | **Free** for public certificates |
| Renewal | Automatic (DNS or email validation) |
| Export | **Cannot export ACM certificates** (private key never leaves ACM) |
| Region | Certificates are regional; **CloudFront requires us-east-1** |
| Supported services | ALB, NLB, CloudFront, API Gateway, AppSync, Elastic Beanstalk |
| Validation methods | DNS (preferred, auto-renew) or Email |
| Wildcard certs | Yes (*.example.com) |
| SANs | Yes (multiple domains in one cert) |

### ACM Private CA (PCA)

- Internal CA for private certificates (non-public-trusted)
- Issue certs for internal services, IoT devices, VPN clients
- Cost: ~$400/month per CA + $0.75/cert
- Can chain to your existing enterprise PKI

### ACM vs CloudHSM for TLS

- Use **ACM** for: load balancers, CloudFront, API Gateway TLS termination
- Use **CloudHSM** for: custom TLS termination on EC2 with FIPS Level 3, code signing, CA key storage

---

## 6. AWS Secrets Manager

### What It Is

Managed service for **storing, rotating, and retrieving** secrets (DB passwords, API keys, OAuth tokens).

### How It Works

```
Application
    |
    1. GetSecretValue(SecretId)
    |
Secrets Manager (returns current secret value)
    |
    2. Auto-rotation (Lambda function):
       - Creates new credential in target DB
       - Updates secret in Secrets Manager
       - Tests the new credential
       - Invalidates old credential
```

### Supported Auto-Rotation Targets

- Amazon RDS (all engines)
- Amazon Redshift
- Amazon DocumentDB
- Amazon Aurora
- Custom rotation via your own Lambda

### Secrets Manager vs Parameter Store

| Feature | Secrets Manager | SSM Parameter Store (SecureString) |
|---|---|---|
| **Auto rotation** | Yes (built-in Lambda integration) | No (manual rotation via Lambda) |
| **Cost** | **$0.40/secret/month** + $0.05/10K API calls | Free (standard), $0.05/10K API calls (standard), $0.10/10K (advanced) |
| **Max secret size** | 65,536 bytes | Standard: 4KB, Advanced: 8KB |
| **Cross-account** | Yes | Limited |
| **Cross-region replication** | Yes (replica secrets) | No |
| **Versioning** | Yes (AWSCURRENT/AWSPREVIOUS/AWSPENDING) | Yes (up to 100 versions) |
| **Encryption** | KMS (mandatory) | KMS (optional for Standard) |
| **Best for** | DB credentials, API keys needing rotation | Config values, non-secrets, hierarchical config |

### Secrets Manager Integration with RDS

```python
import boto3
import json

def get_db_connection():
    client = boto3.client('secretsmanager')
    response = client.get_secret_value(SecretId='prod/myapp/rds')
    secret = json.loads(response['SecretString'])
    # secret = {"username": "admin", "password": "rotated-pw", "host": "..."}
    return connect(host=secret['host'],
                   user=secret['username'],
                   password=secret['password'])
```

### Secret Versions

```
AWSCURRENT  → active secret (most applications use this)
AWSPREVIOUS → previous secret (during rotation grace period, both work)
AWSPENDING  → new secret being tested during rotation
```

---

## 7. S3 Encryption Options

| Option | Key Mgmt | Notes |
|---|---|---|
| **SSE-S3** | S3 manages keys (AES-256) | Free, default since Jan 2023, no KMS cost |
| **SSE-KMS** | Your CMK or AWS Managed key | Audit trail in CloudTrail; KMS API costs; throttling possible |
| **DSSE-KMS** | Two-layer KMS encryption | Strongest at-rest option; dual encryption |
| **SSE-C** | You provide key with each request | AWS never stores your key; HTTPS only; no key rotation by AWS |
| **Client-side** | You encrypt before upload | Full control; complex; key management on you |

### Enforce HTTPS for S3

```json
// S3 Bucket Policy — deny all non-HTTPS
{
  "Effect": "Deny",
  "Principal": "*",
  "Action": "s3:*",
  "Resource": ["arn:aws:s3:::my-bucket", "arn:aws:s3:::my-bucket/*"],
  "Condition": {
    "Bool": {"aws:SecureTransport": "false"}
  }
}
```

### Enforce SSE-KMS for S3 Uploads

```json
{
  "Effect": "Deny",
  "Principal": "*",
  "Action": "s3:PutObject",
  "Resource": "arn:aws:s3:::my-bucket/*",
  "Condition": {
    "StringNotEquals": {
      "s3:x-amz-server-side-encryption": "aws:kms"
    }
  }
}
```

---

## 8. Key Config & Limits

| Parameter | Value |
|---|---|
| KMS CMK cost | $1/month/key |
| KMS API rate | Default 5,500–30,000 requests/sec per region (varies by region) |
| KMS max data size for direct Encrypt | **4 KB** |
| CloudHSM cost | ~$1.60/HSM/hour |
| CloudHSM FIPS level | **140-2 Level 3** |
| KMS FIPS level | 140-2 Level 2 |
| ACM public cert cost | **Free** |
| ACM cert for CloudFront | Must be in **us-east-1** |
| Secrets Manager cost | $0.40/secret/month + API calls |
| Secret max size | 65,536 bytes |
| KMS key deletion waiting period | 7–30 days (cannot be immediate) |
| KMS key alias | Must start with `alias/` |
| Multi-Region key prefix | `mrk-` |

---

## 9. Decision Tree

### Which Key Type?

```
Do you need hardware key protection (FIPS 140-2 Level 3)?
├── YES → CloudHSM (or CloudHSM-backed KMS custom key store)
└── NO  → KMS

KMS: Who manages rotation and key material?
├── AWS handles it, just need encryption → AWS Managed Key (free)
├── Need control, cross-account, custom policy → Customer Managed CMK ($1/mo)
└── Importing your own key material → CMK with imported material

Need to decrypt in multiple regions?
├── YES → KMS Multi-Region Keys
└── NO  → Standard regional CMK
```

### SSE Options for S3

```
Need maximum simplicity, no extra cost?
└── SSE-S3

Need audit trail (who decrypted what, when)?
└── SSE-KMS (CMK)

Need dual-layer encryption for compliance?
└── DSSE-KMS

You manage keys outside AWS?
└── SSE-C (bring key per request) or Client-side encryption

Need cross-account access to encrypted objects?
└── SSE-KMS with CMK (bucket owner + object owner + key policy)
```

### Secrets Manager vs Parameter Store

```
Does the secret need automatic rotation?
├── YES → Secrets Manager (built-in rotation Lambda)
└── NO  → Is it a secret (password/key)?
          ├── YES, cost matters → Parameter Store SecureString ($0 standard tier)
          └── NO, it's config → Parameter Store String (free)
```

### ACM vs CloudHSM for TLS

```
TLS termination at ALB/CloudFront/API GW?
└── ACM (free, auto-renewal)

TLS on EC2/custom server, need FIPS Level 3?
└── CloudHSM with ACM PCA or bring your own PKI
```

---

## 10. Common Patterns

### Pattern 1: EBS Encryption with CMK

```
EC2 Instance → EBS Volume (encrypted with CMK)
                   |
              KMS GenerateDataKey
                   |
              Data key encrypts volume blocks locally
              Encrypted data key stored with volume metadata
```

All EBS snapshots inherit encryption. Sharing encrypted snapshot cross-account requires sharing CMK access.

### Pattern 2: RDS Encryption at Rest + Secrets Manager Rotation

```
RDS Aurora cluster ── encrypted with KMS CMK
      |
Secrets Manager: prod/aurora/admin
      |── auto-rotation every 30 days
      |── Lambda: create new pw in RDS → store in SM → test → retire old
      |
App ── GetSecretValue → connect with current password
```

### Pattern 3: S3 Cross-Account Encrypted Access

```
Account A: S3 bucket + SSE-KMS with CMK-A
Account B: User needs to read S3 objects

Requirements:
1. S3 bucket policy: allow Account B role s3:GetObject
2. KMS key policy (CMK-A): allow Account B role kms:Decrypt
3. Account B IAM policy: allow s3:GetObject + kms:Decrypt on specific resources
```

### Pattern 4: Secrets Manager + Lambda + RDS

```python
# Lambda function with automatic secret rotation
import boto3, json, pymysql

def rotate(event, context):
    sm = boto3.client('secretsmanager')
    step = event['Step']
    secret_id = event['SecretId']
    token = event['ClientRequestToken']

    if step == 'createSecret':
        # Generate new password, store as AWSPENDING
        new_password = generate_password()
        sm.put_secret_value(SecretId=secret_id, ClientRequestToken=token,
                           SecretString=json.dumps({**current, 'password': new_password}),
                           VersionStages=['AWSPENDING'])

    elif step == 'setSecret':
        # Set new password in RDS
        pending = json.loads(sm.get_secret_value(SecretId=secret_id,
                             VersionStage='AWSPENDING')['SecretString'])
        # ALTER USER in RDS...

    elif step == 'testSecret':
        # Test connection with new password
        pass

    elif step == 'finishSecret':
        # Move AWSPENDING → AWSCURRENT
        sm.update_secret_version_stage(SecretId=secret_id,
            VersionStage='AWSCURRENT', MoveToVersionId=token,
            RemoveFromVersionId=current_version)
```

---

## 11. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **KMS keys are regional** | A CMK in us-east-1 cannot decrypt data encrypted in eu-west-1 (unless Multi-Region) |
| **CloudHSM expensive** | $1.60/hr = ~$1,150/month per HSM × 2 for HA. Justify with compliance need |
| **ACM certs not exportable** | Cannot download private key. If you need it for non-AWS server, use self-signed or public CA |
| **CloudFront ACM must be us-east-1** | Regardless of CloudFront distribution origin region |
| **Secrets Manager $0.40/secret/month** | 1,000 secrets = $400/month. Use Parameter Store for simple config values |
| **KMS deletion waiting period** | Minimum 7 days, default 30 days. Plan ahead for key retirement |
| **SSE-KMS S3 throttling** | High-throughput S3 can exhaust KMS API rate. Use S3 Bucket Keys to reduce KMS calls by 99% |
| **Parameter Store free tier limit** | Standard parameters free, but > 10,000 parameters or advanced tier has costs |
| **Rotation doesn't happen on creation** | Secrets Manager rotation starts at the configured interval from creation, not immediately |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **KMS throttling in S3 workloads** | Enable S3 Bucket Keys — reduces KMS API calls per object to once per bucket key lifetime |
| **Accidental key deletion** | Set key deletion waiting period to 30 days; use CloudTrail alert on ScheduleKeyDeletion |
| **Hardcoded secrets in Lambda env vars** | Use Secrets Manager or Parameter Store SecureString + Lambda extension for caching |
| **No key policy root statement** | Key becomes unmanageable; must contact AWS support to recover |
| **Cross-account KMS only key policy** | KMS resource policy alone not sufficient — external account also needs IAM policy |

---

## 12. Hands-On Lab (Free Tier)

### Goal: Create CMK → Encrypt/Decrypt → Enable S3 SSE-KMS → Secrets Manager rotation

**Step 1 — Create Customer Managed CMK**

```bash
aws kms create-key \
  --description "My app encryption key" \
  --key-usage ENCRYPT_DECRYPT \
  --origin AWS_KMS

# Note the KeyId, create alias
aws kms create-alias \
  --alias-name alias/my-app-key \
  --target-key-id KEY-ID
```

**Step 2 — Enable Auto-Rotation**

```bash
aws kms enable-key-rotation --key-id KEY-ID
aws kms get-key-rotation-status --key-id KEY-ID
# {"KeyRotationEnabled": true}
```

**Step 3 — Envelope Encryption Demo**

```python
import boto3, base64

kms = boto3.client('kms')

# Generate data key
response = kms.generate_data_key(
    KeyId='alias/my-app-key',
    KeySpec='AES_256'
)
plaintext_key = response['Plaintext']     # use to encrypt, then discard
encrypted_key = response['CiphertextBlob']  # store this

# Simulate local encryption
from cryptography.fernet import Fernet
import base64
# (simplified — real impl uses AES-GCM with the plaintext_key)
print(f"Encrypted data key length: {len(encrypted_key)} bytes")
print("Store encrypted_key alongside your encrypted data")
del plaintext_key  # discard from memory

# To decrypt later
decrypt_response = kms.decrypt(CiphertextBlob=encrypted_key)
recovered_key = decrypt_response['Plaintext']
print("Key recovered successfully")
```

**Step 4 — S3 Bucket with SSE-KMS + Bucket Keys**

```bash
# Create bucket
aws s3 mb s3://my-encrypted-bucket-ACCOUNT

# Enable default SSE-KMS encryption with Bucket Key
aws s3api put-bucket-encryption \
  --bucket my-encrypted-bucket-ACCOUNT \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "aws:kms",
        "KMSMasterKeyID": "alias/my-app-key"
      },
      "BucketKeyEnabled": true
    }]
  }'

# Upload file - automatically encrypted
aws s3 cp /tmp/test.txt s3://my-encrypted-bucket-ACCOUNT/test.txt

# Verify encryption
aws s3api head-object \
  --bucket my-encrypted-bucket-ACCOUNT \
  --key test.txt
# Look for: "ServerSideEncryption": "aws:kms"
```

**Step 5 — Create a Secret in Secrets Manager**

```bash
aws secretsmanager create-secret \
  --name prod/myapp/database \
  --description "RDS database credentials" \
  --secret-string '{"username":"admin","password":"InitialPassword123!","host":"db.example.com"}' \
  --kms-key-id alias/my-app-key

# Retrieve secret
aws secretsmanager get-secret-value \
  --secret-id prod/myapp/database \
  --query SecretString \
  --output text | python3 -m json.tool
```

**Step 6 — Generate ACM Certificate (for existing domain)**

```bash
aws acm request-certificate \
  --domain-name example.com \
  --subject-alternative-names www.example.com \
  --validation-method DNS \
  --region us-east-1  # Required for CloudFront!

# Add the CNAME records shown in console to your DNS
# Certificate auto-renews 60 days before expiry
```

**Cleanup:**

```bash
aws secretsmanager delete-secret \
  --secret-id prod/myapp/database \
  --force-delete-without-recovery

aws s3 rb s3://my-encrypted-bucket-ACCOUNT --force

# Schedule key deletion (minimum 7 days)
aws kms schedule-key-deletion \
  --key-id KEY-ID \
  --pending-window-in-days 7
```
