# AWS Global Infrastructure

## The Problem
Before cloud, running applications globally meant buying servers in multiple countries, negotiating data center contracts, managing physical hardware, and dealing with network peering agreements. A company wanting low latency for users in Asia AND Europe needed separate IT teams, hardware, and connectivity in each region — massive CapEx, months of lead time, and no elasticity.

Even after virtualization, the fundamental problem of geographic distribution and fault isolation remained: a hurricane, power failure, or network cut could take down an entire data center — and everything running in it.

---

## What AWS Built
AWS operates the world's largest cloud infrastructure across **33+ Regions**, **105+ Availability Zones**, **400+ Edge Locations**, and specialized zones (Local Zones, Wavelength Zones, Outposts). This physical footprint is the foundation every AWS service runs on.

---

## How It Works

### Regions
A **Region** is a geographic area containing multiple, isolated data center clusters. Each region is completely independent — failure in one does not affect another.

```
Examples:
  us-east-1       → Northern Virginia (oldest, most services)
  eu-west-1       → Ireland
  ap-southeast-1  → Singapore
  ap-south-1      → Mumbai
```

**Key characteristics:**
- Most AWS services are **regional** — data stays within the region unless you explicitly replicate it
- Each region has its own endpoints, capacity, and pricing
- ~33 regions as of 2025, new ones announced regularly

---

### Availability Zones (AZs)
An **AZ** is one or more discrete data centers within a region, each with:
- Independent power supplies (multiple providers + UPS + generators)
- Independent cooling
- Independent physical security
- Independent networking

AZs within a region are connected via **low-latency, high-bandwidth, redundant fiber** (sub-millisecond latency between AZs in the same region).

```
Region: us-east-1
  AZ: us-east-1a  ─┐
  AZ: us-east-1b  ─┤── all connected via redundant fiber
  AZ: us-east-1c  ─┘
  AZ: us-east-1d
  AZ: us-east-1e
  AZ: us-east-1f  (us-east-1 has 6 AZs — most regions have 3)
```

**Why multiple AZs matter:** A single AZ failure (power, cooling, flooding, network) should not take down your application. Properly architected apps span 2-3 AZs.

> **Critical gotcha:** AZ names are randomized per account. `us-east-1a` in your account is NOT the same physical data center as `us-east-1a` in another account. AWS does this to distribute load. Use AZ IDs (e.g., `use1-az1`) for cross-account coordination. Eg. For VPC Peering or Direct Connect, need to ensure both sides are in the same physical AZ, not just same AZ name.
---

### Edge Locations & Points of Presence (PoPs)

**PoP** is the umbrella term. There are two types of PoPs:

| Type | Count | Purpose |
|---|---|---|
| **Edge Location** | 400+ cities | Closest to users — serves cached content, answers DNS, runs Lambda@Edge |
| **Regional Edge Cache** | ~13 globally | Mid-tier cache between Edge Locations and your origin — larger cache, fewer misses to origin |

**Request flow:**
```
User → Edge Location → (cache miss) → Regional Edge Cache → (cache miss) → Origin (your S3/EC2)
```

Used by:
- **CloudFront** (CDN): cache static/dynamic content close to users
- **Route53** (DNS): answer DNS queries from the nearest Edge Location
- **AWS Shield**: absorb DDoS traffic at the edge before it reaches your origin
- **Lambda@Edge / CloudFront Functions**: run code at the edge

These are NOT full regions — they cannot run EC2 or most services. They exist purely for low-latency content delivery and edge compute.

---

### Local Zones
Extensions of a region placed in metro areas **not served by a full region** (e.g., Los Angeles, Boston, Chicago, Dallas).

- Run select services: EC2, EBS, ELB, RDS, ECS
- Sub-10ms latency to users in that city
- Still managed/billed under the parent region
- Use case: media production, gaming, real-time applications needing <10ms to a specific city

---

### Wavelength Zones
AWS infrastructure embedded **inside telecom 5G networks** (Verizon, Vodafone, SK Telecom, etc.).

- Traffic stays within the telecom network (doesn't traverse public internet)
- Single-digit millisecond latency to 5G devices
- Use case: connected vehicles, AR/VR, real-time gaming, IoT on 5G

---

### AWS Outposts
AWS-managed hardware **installed in your on-premises data center**.
- Same AWS APIs, console, and services but physically at your location
- Use case: data residency requirements, ultra-low latency to on-prem systems, compliance
- Outposts rack (full rack) or Outposts servers (1U/2U)

---

## Key Numbers Worth Knowing

| Component | Count (approx.) | Key Fact |
|---|---|---|
| Regions | 33+ | Independent, isolated |
| AZs | 105+ | 3-6 per region, sub-ms between |
| Edge Locations | 400+ | CloudFront/Route53 cache |
| Local Zones | 30+ | Metro city extensions |
| Wavelength Zones | 15+ | Inside 5G networks |

**Latency reference:**
```
Same AZ         → <1ms
Cross-AZ        → 1-2ms
Cross-Region    → 60-200ms (depends on geographic distance)
Edge to user    → <50ms for ~95% of global internet users (CloudFront)
```

---

## Decision Tree: How to Choose a Region

```
What drives your region choice?
│
├── COMPLIANCE / DATA RESIDENCY?
│   └── Data must stay in specific country/region
│       → Choose region in that country (e.g., eu-central-1 for Germany)
│       → Enable Service Control Policy to prevent data leaving that region
│
├── LATENCY to end users?
│   └── Where are most users located?
│       → Pick closest region
│       → For global users: CloudFront + origin in primary region
│       → For true global: multi-region active-active
│
├── SERVICE AVAILABILITY?
│   └── Does the service you need exist in that region?
│       → Check aws.amazon.com/about-aws/global-infrastructure/regional-product-services/
│       → Not all services are in all regions (e.g., some AI services only in us-east-1)
│
├── COST?
│   └── Pricing varies by region (~10-30% difference)
│       → us-east-1 typically cheapest (most mature)
│       → ap-southeast-2 (Sydney) among most expensive
│
└── DISASTER RECOVERY?
    └── Choose a DR region geographically distant from primary
        → us-east-1 (Virginia) ↔ us-west-2 (Oregon) — common pair
        → eu-west-1 (Ireland) ↔ eu-central-1 (Frankfurt)
```

---

## Global vs Regional vs AZ-Scoped Services

| Scope | Services |
|---|---|
| **Global** | IAM, Route53, CloudFront, WAF (on CloudFront), Organizations, Billing |
| **Regional** | EC2, S3, RDS, Lambda, SQS, SNS, VPC, ELB, DynamoDB |
| **AZ-Scoped** | EBS volumes, EC2 instances, subnets, RDS instances (unless Multi-AZ) |

> **Exam tip:** IAM is global. S3 bucket names are globally unique but data is regional. EC2 instances are AZ-scoped.

---

## Common Patterns

### Multi-AZ (Standard HA)
```
Region: us-east-1
  ALB (spans all AZs)
    ├── AZ-a: EC2 instances + RDS Primary
    ├── AZ-b: EC2 instances + RDS Standby
    └── AZ-c: EC2 instances
```

### Multi-Region (DR / Global)
```
Route53 Failover/Latency Routing
  ├── Primary: us-east-1 (full stack)
  └── Secondary: eu-west-1 (DR or active)
      CloudFront in front for global caching
```

---

## Gotchas

1. **AZ name randomization:** `us-east-1a` ≠ same physical AZ across accounts. Use AZ IDs for cross-account coordination.
2. **Not all services in all regions:** Always verify the service exists in your chosen region before architecting.
3. **Data transfer costs:** Moving data between regions costs money. Moving data between AZs within a region also costs money (small but adds up).
4. **Region is permanent:** Once you launch resources in a region, migrating them out requires deliberate effort (snapshots, exports, etc.).
5. **us-east-1 dependency:** Many global services (IAM, Organizations, Billing) are anchored to us-east-1. An us-east-1 API disruption can affect global service APIs even if your workload is in another region.

---

## Hands-On Lab (Free Tier)

**Goal:** Understand regions and AZs using the AWS Console and CLI.

```bash
# 1. List all available regions
aws ec2 describe-regions --output table

# 2. List AZs in your current region
aws ec2 describe-availability-zones --output table

# 3. Get AZ IDs (account-independent identifiers)
aws ec2 describe-availability-zones \
  --query 'AvailabilityZones[*].[ZoneName,ZoneId]' \
  --output table

# 4. Check which services are available in a region
# Go to: https://aws.amazon.com/about-aws/global-infrastructure/regional-product-services/

# 5. View edge locations for CloudFront
# Go to: https://aws.amazon.com/cloudfront/features/ → Infrastructure section
```

**Console exercise:**
1. Log into AWS Console → top right: click region dropdown
2. Switch to `eu-west-1` (Ireland) — notice which services appear
3. Switch to `ap-southeast-2` — compare service availability
4. Go to EC2 → Instances → Launch Instance → pick a subnet → note which AZ it's in
5. Note the AZ name vs AZ ID in the subnet details
