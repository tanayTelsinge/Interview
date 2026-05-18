# GCP & AWS Basics — Interview Cheat Sheet

---

## Service Mapping — GCP vs AWS

| Category | GCP (You know) | AWS (Equivalent) |
|----------|---------------|-----------------|
| Compute | Compute Engine | EC2 |
| Containers | GKE | EKS |
| Serverless | Cloud Run | Lambda + Fargate |
| Object Storage | Cloud Storage | S3 |
| Database | Cloud SQL | RDS |
| NoSQL | Firestore | DynamoDB |
| Message Queue | Pub/Sub | SQS / SNS |
| Streaming | Pub/Sub | Kinesis |
| Observability | Cloud Monitoring | CloudWatch |
| Logging | Cloud Logging | CloudWatch Logs |
| CI/CD | Cloud Build | CodePipeline |
| Container Registry | Artifact Registry | ECR |
| Load Balancer | Cloud Load Balancing | ALB / NLB |
| DNS | Cloud DNS | Route 53 |
| IAM | Cloud IAM | IAM |
| Secret Management | Secret Manager | Secrets Manager |

---

## GKE vs EKS

### GKE (Google Kubernetes Engine)
- Managed Kubernetes by Google
- Google manages control plane (API server, etcd, scheduler)
- You manage worker nodes (or use Autopilot — fully managed)
- Integrated with GCP services (Cloud Logging, Cloud Monitoring)

### EKS (Amazon Elastic Kubernetes Service)
- Managed Kubernetes by AWS
- AWS manages control plane
- You manage worker nodes via EC2 or use Fargate (serverless nodes)
- Integrated with AWS services (CloudWatch, IAM, ALB)

### Key Difference
```
GKE Autopilot = fully managed, Google manages nodes too
EKS Fargate   = serverless nodes, AWS manages nodes
Both run standard Kubernetes — kubectl works the same
```

### Interview Answer
> "GKE and EKS are both managed Kubernetes services — the control plane is managed by the cloud provider. The difference is in ecosystem integration — GKE integrates with GCP services like Cloud Logging and Cloud Monitoring, EKS integrates with CloudWatch and AWS IAM. Core Kubernetes concepts like pods, deployments, services, ingress are identical."

---

## Cloud Storage vs S3

### GCS (Google Cloud Storage)
- Object storage — store any file (images, logs, backups)
- Buckets contain objects
- Access control via IAM or ACLs
- Storage classes: Standard, Nearline, Coldline, Archive

### S3 (Amazon Simple Storage Service)
- Same concept — object storage
- Buckets contain objects
- Access control via IAM, Bucket Policies, ACLs
- Storage classes: Standard, Infrequent Access, Glacier (archive)

### Interview Answer
> "S3 is AWS equivalent of GCS — both are object storage services. I've worked with GCS for storing financial reports and logs. S3 follows the same bucket/object model with similar access control patterns via IAM."

---

## Cloud Monitoring vs CloudWatch

### GCP Cloud Monitoring
- Metrics, dashboards, alerting
- Integrated with GKE, Cloud Run, Cloud SQL
- Used with Grafana via data source plugin

### AWS CloudWatch
- Metrics, logs, dashboards, alerting
- Integrated with EC2, EKS, Lambda, RDS
- CloudWatch Logs — centralized log management
- CloudWatch Alarms — trigger alerts on metric thresholds

### Interview Answer
> "I've used GCP Cloud Monitoring and Grafana for observability — dashboards, alerting, log analysis. CloudWatch is AWS equivalent — same concepts of metrics, logs, and alarms. I'm comfortable with the observability mindset and can apply it to CloudWatch."

---

## Core AWS Concepts

### EC2 (Elastic Compute Cloud)
- Virtual machines in AWS
- Choose instance type (CPU, memory, GPU)
- Like GCP Compute Engine

```
t3.micro  — small, dev/test
m5.large  — general purpose, production
c5.xlarge — compute optimized
r5.large  — memory optimized
```

### IAM (Identity and Access Management)
- Same concept as GCP IAM
- Users, Groups, Roles, Policies
- Principle of least privilege

```json
// IAM Policy example
{
  "Effect": "Allow",
  "Action": ["s3:GetObject", "s3:PutObject"],
  "Resource": "arn:aws:s3:::my-bucket/*"
}
```

### VPC (Virtual Private Cloud)
- Isolated network in AWS
- Subnets — public (internet facing) vs private (internal)
- Security Groups — firewall rules for EC2
- Like GCP VPC

### Lambda
- Serverless functions
- No server management
- Triggered by events (S3 upload, API Gateway, SQS message)
- Like GCP Cloud Functions / Cloud Run

```java
// Lambda handler in Java
public class Handler implements RequestHandler<S3Event, String> {
    public String handleRequest(S3Event event, Context context) {
        // process S3 event
        return "processed";
    }
}
```

---

## Common AWS Interview Questions

### Q1: What is the difference between SQS and SNS?

| | SQS | SNS |
|--|-----|-----|
| Type | Queue | Topic/Pub-Sub |
| Pattern | Point to point | Fan out |
| Consumers | One consumer per message | Multiple subscribers |
| Use case | Task queue, decoupling | Notifications, broadcasting |
| GCP equivalent | Cloud Tasks | Pub/Sub |

**Answer:**
> "SQS is a message queue — one producer, one consumer per message. Used for decoupling services and task processing. SNS is pub/sub — one message fan out to multiple subscribers. I've used GCP Pub/Sub which combines both patterns. In AWS I'd use SQS for async job processing and SNS for broadcasting events to multiple downstream services."

---

### Q2: What is the difference between ALB and NLB?

| | ALB (Application Load Balancer) | NLB (Network Load Balancer) |
|--|--------------------------------|-----------------------------|
| Layer | Layer 7 (HTTP/HTTPS) | Layer 4 (TCP/UDP) |
| Routing | Path based, host based | IP based |
| Use case | Microservices, REST APIs | High performance, low latency |
| GCP equivalent | HTTP(S) Load Balancer | Network Load Balancer |

**Answer:**
> "ALB operates at Layer 7 — it can route based on URL path or host headers, making it ideal for microservices. NLB operates at Layer 4 — faster, lower latency, used for high throughput scenarios. For REST APIs and microservices I'd use ALB."

---

### Q3: What is Auto Scaling in AWS?

**Answer:**
> "Auto Scaling automatically adjusts the number of EC2 instances or EKS pods based on load. You define scaling policies — scale out when CPU > 70%, scale in when CPU < 30%. This ensures the application handles traffic spikes without over-provisioning. In GKE I've worked with Horizontal Pod Autoscaler — same concept."

---

### Q4: What is RDS and when would you use it vs DynamoDB?

| | RDS | DynamoDB |
|--|-----|----------|
| Type | Relational (SQL) | NoSQL (Key-Value) |
| Schema | Fixed schema | Flexible schema |
| Use case | Transactional, financial data | High throughput, simple queries |
| GCP equivalent | Cloud SQL | Firestore / Bigtable |

**Answer:**
> "RDS is managed relational DB — supports MySQL, PostgreSQL, Oracle. DynamoDB is managed NoSQL. For financial systems like at Citi, RDS/Oracle is appropriate — ACID transactions, complex joins, regulatory compliance. DynamoDB suits high throughput simple lookups — session data, caching."

---

### Q5: How does AWS handle security for microservices?

**Answer:**
> "Several layers:
> - IAM Roles — each service gets a role with least privilege permissions
> - Security Groups — network level firewall, control which services can talk to each other
> - VPC — services in private subnets, only exposed via load balancer
> - Secrets Manager — store DB credentials, API keys securely, rotated automatically
> - WAF (Web Application Firewall) — protect public APIs from attacks
>
> Same principles as GCP — defense in depth."

---

## Gap Mitigation — What to Say

When asked about AWS experience:

> "My cloud experience is primarily on GCP — I'm Google Cloud certified as Associate Cloud Developer. I've worked with GKE, Cloud Storage, and Cloud Monitoring in production. AWS follows the same fundamental cloud principles — the services map directly. EKS is GKE, S3 is Cloud Storage, CloudWatch is Cloud Monitoring. I'm confident I can ramp up quickly given my strong GCP foundation and Kubernetes experience."

---

## Docker Basics

### What is Docker?
> Containerization platform — packages application + dependencies into a portable container that runs consistently anywhere.

### Key Concepts

| Concept | Description |
|---------|-------------|
| Image | Blueprint — read only template to create containers |
| Container | Running instance of an image |
| Dockerfile | Instructions to build an image |
| Registry | Stores images — DockerHub, ECR, GCR |
| Volume | Persistent storage for containers |
| Network | Communication between containers |

### Dockerfile Example (Spring Boot)
```dockerfile
# Base image
FROM eclipse-temurin:17-jre-alpine

# Working directory
WORKDIR /app

# Copy jar
COPY target/finance-service.jar app.jar

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Common Docker Commands
```bash
# Build image
docker build -t finance-service:1.0 .

# Run container
docker run -d -p 8080:8080 finance-service:1.0

# List running containers
docker ps

# View logs
docker logs <container-id>

# Stop container
docker stop <container-id>

# Push to registry
docker push gcr.io/my-project/finance-service:1.0
```

### Docker vs VM

| | Docker Container | Virtual Machine |
|--|-----------------|-----------------|
| Size | MBs | GBs |
| Startup | Seconds | Minutes |
| Isolation | Process level | Full OS |
| Overhead | Low | High |
| Use case | Microservices | Full OS isolation |

### Interview Answer
> "Docker packages the application and its dependencies into a container — eliminates 'works on my machine' problems. Each microservice runs in its own container with its own dependencies. I've used Docker to containerize Spring Boot services, which are then deployed to GKE. The image is built in Jenkins CI pipeline and pushed to the container registry."

---

## Kubernetes Basics

### What is Kubernetes?
> Container orchestration platform — manages deployment, scaling, and operation of containerized applications across a cluster of machines.

### Core Components

#### Cluster Architecture
```
Control Plane (Master)
├── API Server      — entry point for all K8s commands
├── etcd            — distributed key-value store (cluster state)
├── Scheduler       — assigns pods to nodes
└── Controller Manager — maintains desired state

Worker Nodes
├── kubelet         — agent on each node, communicates with API server
├── kube-proxy      — network routing
└── Container Runtime — Docker/containerd runs containers
```

### Key Objects

| Object | Description | Analogy |
|--------|-------------|---------|
| Pod | Smallest unit — one or more containers | Single instance |
| Deployment | Manages pod replicas, rolling updates | Process manager |
| Service | Stable network endpoint for pods | Load balancer |
| ConfigMap | Non-sensitive configuration | Properties file |
| Secret | Sensitive data (passwords, keys) | Vault |
| Ingress | HTTP routing rules, SSL termination | API Gateway |
| Namespace | Logical isolation within cluster | Environment |
| PersistentVolume | Storage for stateful apps | Disk |

### Pod vs Deployment
```yaml
# Pod — single instance, no self healing
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: finance-service
    image: finance-service:1.0

# Deployment — manages replicas, self healing, rolling updates
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3  # always maintain 3 pods
  template:
    spec:
      containers:
      - name: finance-service
        image: finance-service:1.0
```

### Service Types

| Type | Description | Use case |
|------|-------------|----------|
| ClusterIP | Internal only | Microservice to microservice |
| NodePort | Expose on node IP | Dev/testing |
| LoadBalancer | External load balancer | Production public endpoints |
| ExternalName | DNS alias | External services |

### Common kubectl Commands
```bash
# Get resources
kubectl get pods
kubectl get deployments
kubectl get services

# Describe resource (debugging)
kubectl describe pod <pod-name>

# View logs
kubectl logs <pod-name>
kubectl logs <pod-name> -f  # follow logs

# Execute into container
kubectl exec -it <pod-name> -- /bin/sh

# Apply config
kubectl apply -f deployment.yaml

# Scale deployment
kubectl scale deployment finance-service --replicas=5

# Rolling update
kubectl set image deployment/finance-service finance-service=finance-service:2.0

# Rollback
kubectl rollout undo deployment/finance-service
```

### Deployment YAML Example
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: finance-service
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: finance-service
  template:
    metadata:
      labels:
        app: finance-service
    spec:
      containers:
      - name: finance-service
        image: gcr.io/my-project/finance-service:1.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
```

### Liveness vs Readiness Probe

| Probe | Purpose | Failure action |
|-------|---------|---------------|
| Liveness | Is container alive? | Restart container |
| Readiness | Is container ready for traffic? | Remove from service endpoints |
| Startup | Has container started? | Delay liveness check |

### HPA — Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: finance-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: finance-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Common Interview Questions

**Q: What happens when a pod crashes?**
> "Kubernetes controller detects the pod is not in desired state and automatically restarts it. If it keeps crashing (CrashLoopBackOff), Kubernetes applies exponential backoff. The liveness probe detects unhealthy containers and triggers restart."

**Q: How does rolling update work?**
> "Kubernetes gradually replaces old pods with new ones — brings up new pod, waits for readiness probe to pass, then terminates old pod. This ensures zero downtime deployment. If new pods fail readiness check, rollout stops automatically."

**Q: What is a namespace?**
> "Logical isolation within a cluster. We use namespaces to separate environments (dev, staging, production) or teams within the same cluster. Resource quotas and RBAC policies can be applied per namespace."

**Q: How do microservices communicate in Kubernetes?**
> "Via Kubernetes Services — each microservice has a ClusterIP service which provides a stable DNS name and IP. Service discovery is automatic — `finance-service.production.svc.cluster.local` resolves to the finance service pods. No hardcoded IPs needed."

**Q: How do you handle secrets in Kubernetes?**
> "Kubernetes Secrets store sensitive data base64 encoded. For production, integrate with external secret managers — GCP Secret Manager or AWS Secrets Manager — using the External Secrets Operator. This avoids storing sensitive data in cluster etcd directly."

---

## Docker + Kubernetes Flow (End to End)

```
Developer pushes code
        ↓
Jenkins CI pipeline triggered
        ↓
docker build → image created
        ↓
docker push → image pushed to registry (GCR/ECR)
        ↓
kubectl apply → Deployment updated with new image
        ↓
Kubernetes rolling update → zero downtime deployment
        ↓
Liveness/Readiness probes validate new pods
        ↓
Traffic routed to new pods
```

### Interview Answer
> "In our setup, Jenkins builds the Docker image on every merge to main, pushes to GCR, then updates the Kubernetes Deployment manifest with the new image tag. Kubernetes handles the rolling update — brings up new pods, validates via readiness probes, then terminates old pods. Zero downtime deployment."

---

## Exponential Backoff

**Definition:** Progressively increasing wait time between retries.

### Without backoff — bad:
```
Pod crashes → restart immediately
Pod crashes → restart immediately
Pod crashes → restart immediately
// hammering the system constantly
```

### With exponential backoff — smart:
```
Pod crashes → wait 10s → restart
Pod crashes → wait 20s → restart
Pod crashes → wait 40s → restart
Pod crashes → wait 80s → restart
// CrashLoopBackOff shown in kubectl
```

### Formula:
```
wait time = base × 2^attempt

attempt 1 → 10 × 2^0 = 10s
attempt 2 → 10 × 2^1 = 20s
attempt 3 → 10 × 2^2 = 40s
attempt 4 → 10 × 2^3 = 80s
```
Usually capped at a max wait time (e.g. 5 minutes in Kubernetes).

### Why it matters:
- Prevents thundering herd — all crashed pods retrying simultaneously overwhelming DB/dependencies
- Gives downstream services time to recover
- Used everywhere — Kubernetes, HTTP retries, message queue consumers, circuit breakers

### Spring Boot implementation:
```java
@Retryable(
    value = Exception.class,
    maxAttempts = 4,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void callExternalService() {
    // retries: 1s, 2s, 4s, 8s
}
```

### Interview Answer:
> "Exponential backoff progressively increases wait time between retries — prevents overwhelming a struggling service with immediate retries. Kubernetes uses it for CrashLoopBackOff — each restart waits longer than the previous one."

---

## Key Numbers to Remember

| Service | Default/Common Config |
|---------|----------------------|
| S3 max object size | 5TB |
| Lambda max timeout | 15 minutes |
| SQS message retention | 4 days default, 14 days max |
| EC2 t3.micro RAM | 1GB |
| RDS max storage | 64TB |