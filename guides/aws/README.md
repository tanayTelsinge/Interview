# AWS Study Guide — SAA-C03 → SAP-C02

> Comprehensive, industry-level reference. Every topic follows:
> **WHY** (problem) → **WHAT** (the service) → **HOW** (internals) → **WHEN** (decision tree) → **GOTCHAS** → **HANDS-ON**

---

## Certification Path

```
No cert → SAA-C03 (Solutions Architect Associate) → SAP-C02 (Solutions Architect Professional)
              ~6 weeks                                    ~12 weeks
              $150 exam                                   $300 exam
```

---

## Study Sections

| # | Section | SAA-C03 | SAP-C02 | Files |
|---|---|---|---|---|
| 00 | [Foundations](./00_foundations/) | Must Know | Must Know | 3 |
| 01 | [Compute](./01_compute/) | Core | Core | 4 |
| 02 | [Storage](./02_storage/) | Core | Core | 3 |
| 03 | [Networking](./03_networking/) | Core | Heavy | 4 |
| 04 | [Database](./04_database/) | Core | Core | 4 |
| 05 | [Messaging](./05_messaging/) | Core | Core | 3 |
| 06 | [Security](./06_security/) | Core | Heavy | 4 |
| 07 | [HA & DR](./07_ha_and_dr/) | Core | Heavy | 2 |
| 08 | [Cost Optimization](./08_cost_optimization/) | Medium | Medium | 1 |
| 09 | [Migration](./09_migration/) | Light | Heavy | 3 |
| 10 | [Data & Analytics](./10_data_analytics/) | Light | Medium | 2 |
| 11 | [Monitoring & Governance](./11_monitoring_governance/) | Medium | Heavy | 3 |
| 12 | [SAP Advanced](./12_sap_advanced/) | Skip | Core | 4 |

---

## Recommended Study Order

### Phase 1 — SAA-C03 (Weeks 1–6)
```
Week 1: 00_foundations → 01_compute (EC2 + Lambda)
Week 2: 01_compute (containers) → 02_storage
Week 3: 03_networking (VPC + Load Balancers)
Week 4: 04_database → 05_messaging
Week 5: 06_security → 07_ha_and_dr
Week 6: 08_cost → Practice exams (Tutorials Dojo, score 80%+ before booking)
```

### Phase 2 — SAP-C02 (Weeks 7–18)
```
Week 7-8:  Revisit all Phase 1 at deeper level
Week 9-10: 03_networking advanced + 09_migration
Week 11-12: 10_data_analytics + 11_monitoring_governance
Week 13-14: 12_sap_advanced (multi-account, BGP, resilience)
Week 15-16: SAP-specific practice exams (Tutorials Dojo SAP pack)
Week 17-18: Weak area revision + final mock exams (80%+ to book)
```

---

## How to Use Each File

Every file is structured as:
1. **The Problem** — Why this service was needed
2. **What AWS Built** — Core definition
3. **How It Works** — Internals, architecture, components
4. **Key Config & Limits** — Numbers that matter for exam + prod
5. **Decision Tree** — When to use this vs alternatives (where applicable)
6. **Common Patterns** — Real industry architectures
7. **Gotchas** — Exam tricks + production pitfalls
8. **Hands-On Lab** — Free tier exercise

---

## Exam Resources

| Resource | Purpose | Cost |
|---|---|---|
| Stephane Maarek (Udemy) | Primary course — SAA-C03 + SAP-C02 | ~$15 on sale |
| Adrian Cantrill (learn.cantrill.io) | Deep dive, visual learner | $40 one-time |
| Tutorials Dojo practice exams | Closest to real exam | $15-20 |
| AWS Skill Builder | Official labs + courses | Free tier |
| AWS Whitepapers | SAP must-read | Free |

### Must-Read Whitepapers for SAP-C02
- AWS Well-Architected Framework
- Disaster Recovery of Workloads on AWS
- AWS Security Best Practices
- Migrating to AWS: Best Practices and Strategies
- AWS Multiple Account Security Strategy

---

## Free Hands-On Setup

```bash
# 1. Create AWS Free Tier account at aws.amazon.com/free
# 2. Set billing alarm IMMEDIATELY after account creation
#    Console → CloudWatch → Alarms → Billing → $1 threshold

# 3. Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install
aws configure  # enter your Access Key, Secret, region, output format

# 4. LocalStack (run AWS locally, zero cost)
docker run --rm -it -p 4566:4566 localstack/localstack
aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket
```

---

## Key Exam Mindset

> AWS exam questions are always scenario-based. The answer is almost never "the most powerful service" — it is the **most appropriate** given constraints of cost, operational overhead, RTO/RPO, or compliance.

**Elimination strategy:**
1. Remove answers that introduce unnecessary operational overhead
2. Remove answers that don't meet the stated RTO/RPO or compliance requirement
3. Between the remaining 2, pick the one that is more **managed** (less you operate)
4. When cost is mentioned — Spot > Reserved > On-Demand; Serverless > EC2 for variable workloads
