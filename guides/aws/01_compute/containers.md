# Containers on AWS — ECS, EKS, Fargate, ECR

## The Problem
"It works on my machine."

Before containers, deploying software meant:
- Different OS versions, library versions, and configs between dev/staging/production
- "Dependency hell" — App A needs Python 3.8, App B needs Python 3.11, can't coexist on same server
- Snowflake servers — each production server slightly different, manual drift over years
- Long VM startup times (minutes), heavy OS overhead for small services

The solution was **containerization**: package the application + its entire runtime dependencies into a single portable unit that runs identically everywhere.

---

## What AWS Built
A full container ecosystem:
- **ECR** — private container registry (store and scan images)
- **ECS** — AWS-native container orchestration
- **EKS** — managed Kubernetes for teams that need the full K8s ecosystem
- **Fargate** — serverless container runtime (works with both ECS and EKS)
- **App Runner** — fully managed, code → running container with zero config

---

## How It Works

### Docker Basics (Foundation)
```dockerfile
# Dockerfile — blueprint for a container image
FROM python:3.12-slim          # base image
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt   # installed into image layer
COPY . .
EXPOSE 8080
CMD ["python", "app.py"]       # runs when container starts
```

```bash
docker build -t myapp:1.0 .       # build image
docker run -p 8080:8080 myapp:1.0  # run container
docker push myrepo/myapp:1.0       # push to registry
```

**Image vs Container:**
- **Image:** Immutable template (like an AMI)
- **Container:** Running instance of an image (like an EC2 instance)

---

## ECR — Elastic Container Registry

### The Problem
You need a private place to store container images close to where they run (fast pull, low latency, secure).

### What It Provides
- Private Docker-compatible registry in AWS
- Integrated with ECS, EKS, Lambda, CodeBuild
- **Image Scanning:** Basic (CVE scan on push) and Enhanced (Inspector integration, continuous scanning)
- **Lifecycle Policies:** Auto-delete old/untagged images (keep last 10 production images)
- **Cross-region/cross-account replication**
- **Immutable tags:** Prevent overwriting `latest` tag (best practice for prod)

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag myapp:1.0 123456789.dkr.ecr.us-east-1.amazonaws.com/myapp:1.0
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/myapp:1.0
```

---

## ECS — Elastic Container Service

### Core Concepts

**Task Definition** — Blueprint for running containers (like a docker-compose.yml):
```json
{
  "family": "my-web-app",
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [{
    "name": "web",
    "image": "123456789.dkr.ecr.us-east-1.amazonaws.com/myapp:1.0",
    "portMappings": [{"containerPort": 8080}],
    "environment": [{"name": "ENV", "value": "production"}],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {"awslogs-group": "/ecs/my-web-app", "awslogs-region": "us-east-1"}
    }
  }],
  "executionRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskRole"
}
```

**Critical: Two IAM Roles in ECS:**
| Role | Purpose | What it does |
|---|---|---|
| **Task Execution Role** | ECS infrastructure | Pull image from ECR, write logs to CloudWatch, get secrets from Secrets Manager |
| **Task Role** | Your application code | What your app can call: S3, DynamoDB, SQS, etc. |

**Service** — Long-running tasks (like a deployment):
- Maintains desired count of tasks
- Integrates with ALB/NLB for load balancing
- Supports rolling updates and blue/green deployments
- Auto-scaling based on CPU/memory/ALB request count

**Task** — Short-lived (run-to-completion), like a batch job

---

### ECS Launch Types

#### EC2 Launch Type
- You manage an **ECS Cluster** of EC2 instances
- ECS agent runs on each EC2, registers with cluster, accepts tasks
- You patch the EC2s, manage capacity, choose instance types
- More control, can use Spot Instances, GPU, specific instance families
- ECS container agent manages task placement

```
ECS Cluster
├── EC2 Instance (t3.large)   ← you manage OS, patches, capacity
│   ├── Task: web-container
│   └── Task: worker-container
└── EC2 Instance (t3.large)
    └── Task: web-container
```

#### Fargate Launch Type
- AWS manages the underlying compute
- You specify CPU + memory per task, AWS finds the hardware
- No EC2 clusters to manage, patch, or scale
- Pay per task (vCPU-seconds + memory-seconds)
- Can still use Spot for Fargate (Fargate Spot — up to 70% discount)

```
Fargate (AWS-managed)
├── Task: web-container (0.5 vCPU, 1GB) ← AWS handles the server
├── Task: web-container (0.5 vCPU, 1GB)
└── Task: worker-container (1 vCPU, 2GB)
```

---

## EKS — Elastic Kubernetes Service

### When to Choose EKS over ECS
- Your team has Kubernetes expertise and existing K8s manifests/Helm charts
- You need the Kubernetes ecosystem: Istio, ArgoCD, Prometheus, Grafana, Crossplane
- Compliance/portability: vendor-neutral, same tooling on-prem + cloud (EKS Anywhere)
- Complex multi-cluster federation
- Migrating existing K8s workloads from on-prem

### EKS Architecture
```
EKS Control Plane (AWS-managed, you don't touch)
├── API Server (kubectl talks to this)
├── etcd (cluster state)
└── Scheduler, Controller Manager

Data Plane (you choose):
├── Managed Node Groups (EC2, AWS handles node upgrades)
├── Self-Managed Nodes (you manage EC2 nodes fully)
└── Fargate Profiles (serverless pods, no nodes)
```

### Key EKS Concepts
- **Managed Node Groups:** AWS provisions and patches EC2 worker nodes, handles upgrades
- **EKS Fargate Profiles:** Run pods on Fargate (no nodes needed) — great for variable workloads
- **EKS Anywhere:** Deploy EKS control plane on your on-prem hardware (VMware or bare metal)
- **EKS Distro:** The open-source K8s distribution AWS uses (for self-managed)

**Essential Add-ons:**
| Add-on | Purpose |
|---|---|
| VPC CNI | AWS-native pod networking (pods get VPC IPs) |
| CoreDNS | DNS resolution within cluster |
| kube-proxy | Network rules for service routing |
| EBS CSI Driver | Dynamic EBS volume provisioning for pods |
| EFS CSI Driver | Dynamic EFS volume provisioning |
| AWS Load Balancer Controller | Create ALB/NLB from K8s Ingress/Service |

---

## Fargate — Serverless Containers

Works with **both ECS and EKS**. The key difference:

| | ECS Fargate | EKS Fargate |
|---|---|---|
| Orchestration | ECS | Kubernetes |
| Networking | awsvpc mode (ENI per task) | Kubernetes VPC CNI |
| Pricing | vCPU + memory per second | Same |
| Complexity | Lower | Higher (need K8s knowledge) |

**Fargate pricing:**
```
vCPU: $0.04048/vCPU-hour
Memory: $0.004445/GB-hour

Example: 1 vCPU + 2GB, running 1 hour = $0.04048 + (2 × $0.004445) = ~$0.049/hr
Compare EC2 t3.small: $0.0208/hr (but includes server management)
```

**Fargate Spot:** Up to 70% cheaper. Can be interrupted. Same 2-min warning. Good for stateless, fault-tolerant tasks.

---

## App Runner

### When to Use
Simplest path from code/container to running HTTPS service:
- No VPC, no task definitions, no cluster — just point to container image or source repo
- AWS handles: build, deploy, SSL certificate, load balancing, auto-scaling, health checks
- Scales to zero automatically

```
Use App Runner when:
  - Small team / individual developer
  - Simple containerized web service or API
  - Don't need VPC-level network control
  - Want deployment as simple as Heroku/Railway but on AWS

Don't use when:
  - Need VPC integration (private subnets, RDS in VPC)
  - Need sidecar containers
  - Need complex deployment strategies (blue/green, canary)
  - Large, complex microservices mesh
```

---

## Decision Tree: Which Container Service?

```
Do you need full Kubernetes ecosystem (existing K8s, Istio, ArgoCD, multi-cluster)?
│
├── YES → EKS
│         ├── Want to manage EC2 nodes? → EKS Managed Node Groups
│         └── Want serverless nodes? → EKS Fargate Profiles
│
└── NO → ECS
      ├── Want to manage EC2 clusters (GPU, Spot, specific instance types)?
      │   └── ECS on EC2 Launch Type
      │
      └── Don't want to manage servers?
          ├── Complex microservices, VPC integration, custom networking?
          │   └── ECS Fargate
          └── Simple web service / API, minimal config?
              └── App Runner

For batch/scheduled tasks:
  - Stateless containers that run and complete → ECS Tasks (one-off)
  - Complex job dependencies, multi-node → AWS Batch
```

---

## Key Config & Limits

| Parameter | ECS | EKS |
|---|---|---|
| Max containers per task def | 10 | N/A (K8s pods) |
| Max tasks per service | Soft: 1000, hard: 5000 | N/A |
| Fargate min CPU | 256 (.25 vCPU) | 0.25 vCPU |
| Fargate max CPU | 16 vCPU | 4 vCPU |
| Fargate max memory | 120 GB | 30 GB |
| EKS control plane cost | $0.10/hr ($72/month) | — |

---

## Common Patterns

### Microservices on ECS Fargate
```
Route53 → ALB
  ├── /api/* → Target Group → ECS Service (API, 2 tasks, 0.5vCPU/1GB)
  ├── /auth/* → Target Group → ECS Service (Auth, 2 tasks)
  └── /admin/* → Target Group → ECS Service (Admin, 1 task)

Auto Scaling on all services based on ALB RequestCount
ElastiCache for sessions, RDS for data
```

### ECS + Blue/Green Deployment (CodeDeploy)
```
CodeDeploy Controller
├── Blue Target Group (current production)
└── Green Target Group (new version)
  1. Deploy new version to Green
  2. Test Green via test listener
  3. Shift 10% traffic → Green
  4. Monitor alarms (5 min)
  5. Shift 100% → Green
  6. Terminate Blue
```

### ML Inference on EKS with GPU
```
EKS Cluster
├── CPU Node Group (t3.xlarge) → web/API pods
└── GPU Node Group (g4dn.xlarge) → inference pods
    ↑ Node selector: k8s.amazonaws.com/accelerator: nvidia-tesla-t4
```

---

## Gotchas

1. **ECS Task Role ≠ Execution Role:** Task Execution Role = ECS agent needs it (pull image, write logs). Task Role = your app code uses it (call S3, DynamoDB). Confusing these causes `AccessDenied` errors that seem mysterious.

2. **Fargate doesn't support privileged containers:** If your container needs `--privileged` (e.g., running Docker-in-Docker, or some security tools), you need ECS on EC2.

3. **EKS costs $0.10/hour for control plane:** ~$72/month minimum, even with no workloads. Don't use EKS for trivial or dev workloads — use ECS or App Runner.

4. **Fargate has no persistent local storage:** Unlike EC2 where you attach EBS, Fargate containers have ephemeral storage only (up to 200GB). For persistent storage, use EFS mounts.

5. **ECS awsvpc mode = 1 ENI per task:** Each Fargate task gets its own ENI. In large deployments (hundreds of tasks), this can hit VPC ENI limits. Plan your CIDR blocks accordingly.

6. **ECR image pull requires execution role:** `ecsTaskExecutionRole` must have `ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`. Missing these = container fails to start.

---

## Hands-On Lab (Free Tier)

**Goal:** Deploy a containerized app on ECS Fargate.

```bash
# 1. Create ECR repository
aws ecr create-repository --repository-name my-web-app --region us-east-1

# 2. Build and push image
docker build -t my-web-app .
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com
docker tag my-web-app:latest ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/my-web-app:latest
docker push ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/my-web-app:latest

# 3. Create ECS cluster
aws ecs create-cluster --cluster-name my-cluster

# 4. Register task definition (save as task-def.json, then:)
aws ecs register-task-definition --cli-input-json file://task-def.json

# 5. Create service
aws ecs create-service \
  --cluster my-cluster \
  --service-name my-web-service \
  --task-definition my-web-app:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={
    subnets=[subnet-xxx,subnet-yyy],
    securityGroups=[sg-xxx],
    assignPublicIp=ENABLED}"

# 6. Check service status
aws ecs describe-services --cluster my-cluster --services my-web-service

# 7. View logs
aws logs tail /ecs/my-web-app --follow

# 8. Clean up (avoid charges)
aws ecs update-service --cluster my-cluster --service my-web-service --desired-count 0
aws ecs delete-service --cluster my-cluster --service my-web-service
aws ecs delete-cluster --cluster my-cluster
```
