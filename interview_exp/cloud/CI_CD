# CI/CD Interview Cheat Sheet

---

## Your CI/CD Stack

```
GitHub → Tekton → Gradle → Docker → GCR → GKE
```

---

## End to End Flow

```
1. Developer pushes code to GitHub
         ↓
2. GitHub webhook triggers Tekton PipelineRun
         ↓
3. Tekton Task 1 — gradle test (unit + integration tests)
         ↓ (fail here = pipeline stops, no deployment)
4. Tekton Task 2 — gradle build → generates JAR
         ↓
5. Tekton Task 3 — docker build → creates image
         ↓
6. Tekton Task 4 — docker push → pushes to GCR
         ↓
7. Tekton Task 5 — kubectl apply → updates GKE Deployment
         ↓
8. GKE rolling update → pods replaced, zero downtime
         ↓
9. Changes live on dev environment
```

---

## GitHub Webhook

**Definition:** HTTP callback — GitHub sends a POST request to a configured URL when an event occurs (push, PR, merge).

**How it works:**
```
Developer pushes code to GitHub
        ↓
GitHub sends POST request to Tekton EventListener URL
        ↓
POST body contains — branch, commit SHA, author, changed files
        ↓
Tekton validates webhook secret (ensures request is from GitHub)
        ↓
Tekton triggers PipelineRun
```

**Configuration in GitHub:**
```
Repo Settings → Webhooks → Add Webhook
- Payload URL: http://tekton-event-listener:8080
- Content type: application/json
- Secret: shared secret (validated by Tekton)
- Events: push, pull_request
```

**Why webhook secret matters:**
> Without secret validation, anyone could trigger your pipeline by sending a POST request to the EventListener URL. Secret ensures only GitHub can trigger it.

**Interview Answer:**
> "GitHub webhook is an HTTP callback — on every code push, GitHub sends a POST request to our Tekton EventListener with commit details. Tekton validates the request using a shared webhook secret, then triggers the pipeline automatically. This is what makes CI/CD fully automated — no manual trigger needed."

---

## GitHub Webhook → Tekton Trigger

```yaml
# Tekton EventListener — listens for GitHub push events
apiVersion: triggers.tekton.dev/v1beta1
kind: EventListener
metadata:
  name: github-listener
spec:
  triggers:
  - name: github-push
    interceptors:
    - ref:
        name: github
      params:
      - name: secretRef
        value:
          secretName: github-webhook-secret
      - name: eventTypes
        value: [push]
    bindings:
    - ref: github-binding
    template:
      ref: pipeline-trigger-template
```

**How it works:**
- GitHub sends POST request to Tekton EventListener on every push
- Tekton validates webhook secret
- Triggers PipelineRun automatically

---

## Tekton Pipeline

```yaml
apiVersion: tekton.dev/v1
kind: Pipeline
metadata:
  name: finance-service-pipeline
spec:
  params:
  - name: image-tag
    type: string
  tasks:
  # Task 1 — Run tests
  - name: test
    taskRef:
      name: gradle-test
    
  # Task 2 — Build JAR
  - name: build
    taskRef:
      name: gradle-build
    runAfter: [test]
  
  # Task 3 — Build Docker image
  - name: docker-build
    taskRef:
      name: docker-build-push
    runAfter: [build]
    params:
    - name: image
      value: gcr.io/my-project/finance-service:$(params.image-tag)
  
  # Task 4 — Deploy to GKE
  - name: deploy
    taskRef:
      name: kubectl-deploy
    runAfter: [docker-build]
```

---

## Gradle in CI/CD

### Key Gradle Commands
```bash
# Run unit tests only
./gradlew test

# Run integration tests
./gradlew integrationTest

# Build JAR (skipping tests — tests already ran)
./gradlew build -x test

# Clean build
./gradlew clean build

# Run specific test class
./gradlew test --tests "com.maxxton.finance.PaymentServiceTest"
```

### Gradle vs Maven

| | Gradle | Maven |
|--|--------|-------|
| Config | Groovy/Kotlin DSL | XML (pom.xml) |
| Performance | Faster (incremental builds, cache) | Slower |
| Flexibility | High | Convention based |
| Spring Boot | Both supported | Both supported |

### build.gradle (Spring Boot)
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.0'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}
```

---

## Docker in CI/CD

### Dockerfile (Spring Boot)
```dockerfile
# Multi-stage build — smaller final image
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY build/libs/*.jar app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Multi-stage build — why?
```
Stage 1 (builder) — JDK needed to run JAR
Stage 2 (final)   — JRE only (smaller, more secure)

JDK image: ~300MB
JRE image: ~180MB
= smaller image = faster pull = less attack surface
```

### Image Tagging Strategy
```bash
# Tag with git commit SHA — immutable, traceable
docker build -t gcr.io/my-project/finance-service:${GIT_SHA} .

# Also tag as latest
docker tag gcr.io/my-project/finance-service:${GIT_SHA} \
           gcr.io/my-project/finance-service:latest
```

---

## GCR — Google Container Registry

```bash
# Authenticate Docker with GCR
gcloud auth configure-docker

# Push image
docker push gcr.io/my-project/finance-service:1.0

# Pull image
docker pull gcr.io/my-project/finance-service:1.0

# List images
gcloud container images list --repository=gcr.io/my-project
```

**GCR → Artifact Registry:**
- GCR is being deprecated
- Artifact Registry is the new standard in GCP
- Same concept, more features (supports Helm charts, npm, Maven artifacts too)

---

## GKE Deployment

### Rolling Update (Zero Downtime)
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # max extra pods during update
      maxUnavailable: 0  # never reduce below desired replicas
```

```
Before update: pod-v1, pod-v1, pod-v1
Step 1: pod-v2 created (surge)     → pod-v1, pod-v1, pod-v1, pod-v2
Step 2: pod-v2 ready, pod-v1 killed → pod-v1, pod-v1, pod-v2
Step 3: repeat until all replaced   → pod-v2, pod-v2, pod-v2
```

### Update Image via kubectl
```bash
# Update image (triggers rolling update)
kubectl set image deployment/finance-service \
  finance-service=gcr.io/my-project/finance-service:2.0

# Check rollout status
kubectl rollout status deployment/finance-service

# Rollback if something wrong
kubectl rollout undo deployment/finance-service

# Rollback to specific version
kubectl rollout undo deployment/finance-service --to-revision=2
```

---

## Jenkins vs Tekton

| | Jenkins | Tekton |
|--|---------|--------|
| Runs on | JVM (dedicated server) | Kubernetes pods |
| Config | Groovy (Jenkinsfile) | YAML (CRDs) |
| Scaling | Manual agents | Auto (K8s) |
| Cloud native | No | Yes |
| UI | Rich UI | Basic (use Tekton Dashboard) |
| Plugin ecosystem | Huge | Growing |
| Learning curve | Medium | High |
| Resource usage | High (always running) | Low (pods spin up on demand) |

### Jenkinsfile Example (your old setup)
```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Build') {
            steps {
                sh './gradlew build -x test'
            }
        }
        stage('Docker Build & Push') {
            steps {
                sh 'docker build -t gcr.io/my-project/finance-service:${BUILD_NUMBER} .'
                sh 'docker push gcr.io/my-project/finance-service:${BUILD_NUMBER}'
            }
        }
        stage('Deploy to GKE') {
            steps {
                sh 'kubectl set image deployment/finance-service finance-service=gcr.io/my-project/finance-service:${BUILD_NUMBER}'
            }
        }
    }
    post {
        failure {
            // notify team on failure
            slackSend message: "Build failed: ${env.JOB_NAME}"
        }
    }
}
```

---

## How Microservice Files & CI/CD Link Together

### Repo Structure
```
Your Code Repo (GitHub)
├── src/                          # application code
├── build.gradle                  # build config
├── Dockerfile                    # how to containerize
└── k8s/                          # kubernetes manifests
    ├── deployment.yaml           # how to deploy
    ├── service.yaml              # how to expose
    └── configmap.yaml            # environment config
```

### How They Connect
```
1. Developer writes code + updates k8s/deployment.yaml if needed
         ↓
2. Push to GitHub
         ↓
3. Tekton picks up changes via webhook
         ↓
4. Reads build.gradle → gradle build → JAR
         ↓
5. Reads Dockerfile → docker build → image
         ↓
6. Pushes image to GCR with new tag (git SHA)
         ↓
7. Updates deployment.yaml image tag → kubectl apply
         ↓
8. GKE reads deployment.yaml → rolling update
```

### deployment.yaml is the Glue
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: finance-service
        # CI/CD pipeline updates this image tag automatically
        image: gcr.io/my-project/finance-service:abc1234  ← git SHA injected by Tekton
        env:
        - name: DB_URL
          valueFrom:
            configMapKeyRef:        ← reads from configmap.yaml
              name: finance-config
              key: db-url
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:           ← reads from secret
              name: finance-secret
              key: password
```

### Complete File Relationship
```
build.gradle    → tells Gradle HOW to build JAR
Dockerfile      → tells Docker HOW to containerize JAR
deployment.yaml → tells Kubernetes HOW to run container
service.yaml    → tells Kubernetes HOW to expose container
configmap.yaml  → tells Kubernetes WHAT config to inject
Tekton pipeline → orchestrates ALL of the above in sequence
```

### Interview Answer
> "All files live in the same GitHub repo. Tekton pipeline reads them in sequence — Gradle builds the JAR using build.gradle, Docker containerizes it using Dockerfile, pushes to GCR, then kubectl applies the Kubernetes manifests in the k8s folder. The deployment.yaml ties everything together — it references the new image from GCR and pulls config from ConfigMaps and Secrets. This is GitOps — Git is the single source of truth for both application code and infrastructure."

---



### Q1: What is the difference between CI and CD?

| | CI (Continuous Integration) | CD (Continuous Delivery/Deployment) |
|--|----------------------------|--------------------------------------|
| What | Merge, build, test automatically | Deploy automatically |
| Goal | Catch bugs early | Ship faster |
| Trigger | Every code push | After CI passes |
| Output | Tested artifact (JAR/image) | Running application |

**Answer:**
> "CI automatically builds and tests every code push — catches integration issues early. CD takes the tested artifact and deploys it automatically to the target environment. In our setup, Tekton handles both — tests and build in CI phase, GKE deployment in CD phase."

---

### Q2: How do you handle a failed deployment?

**Answer:**
> "First line of defense is readiness probes — if new pods fail health check, Kubernetes stops the rolling update automatically, old pods continue serving traffic. If the issue is caught post-deployment, we use `kubectl rollout undo` to instantly rollback to previous deployment. We also monitor Grafana dashboards for error rate spikes immediately after deployment."

---

### Q3: How do you manage environment-specific configs?

**Answer:**
> "We use Kubernetes ConfigMaps for non-sensitive config and Secrets for sensitive data like DB credentials. Spring Boot profiles (dev, staging, prod) handle application-level config. Environment specific values are injected as environment variables into the pod — never hardcoded in the image. This way the same image runs in all environments."

---

### Q4: What is a blue-green deployment?

```
Blue  = current production (v1) — live traffic
Green = new version (v2)        — deployed but no traffic

Test green → switch traffic → blue becomes standby
Rollback   = switch traffic back to blue instantly
```

**vs Rolling Update:**
- Rolling = gradual replacement, some v1 + some v2 running simultaneously
- Blue-Green = instant switch, full v1 OR full v2, never mixed

**Answer:**
> "Blue-green runs two identical environments — blue is live, green is new version. Once green is validated, traffic switches instantly. Rollback is immediate — just switch back to blue. We used rolling updates in GKE, but blue-green is preferred when you need instant rollback and can't tolerate mixed versions."

---

### Q5: How do you secure your CI/CD pipeline?

**Answer:**
> "Several layers:
> - GitHub webhook secret — validates requests are genuinely from GitHub
> - GCP Service Account with least privilege — Tekton only has permission to push to GCR and deploy to specific GKE namespace
> - Kubernetes Secrets for credentials — never hardcoded in pipeline config
> - Image scanning — scan Docker images for vulnerabilities before deployment
> - Branch protection — PRs require code review approval before merge to main
> - Audit logs — all deployments logged with who triggered and which image"

---

## Exponential Backoff in CI/CD Context

**Used in:**
- Retry failed pipeline steps (network blip during docker push)
- Kubernetes CrashLoopBackOff (pod restart policy)
- Spring Boot `@Retryable` for flaky external calls

```
Attempt 1 → wait 10s → retry
Attempt 2 → wait 20s → retry
Attempt 3 → wait 40s → retry
Attempt 4 → fail pipeline, notify team
```

---

## Key Interview Talking Points

1. **Shift left** — catch bugs early in pipeline, not in production
2. **Immutable images** — same image deployed to dev, staging, prod. No rebuilding per environment
3. **GitOps** — Git is single source of truth. Infrastructure changes via PRs, not manual kubectl
4. **Fail fast** — tests run first, expensive steps (docker build) only if tests pass
5. **Observability** — every deployment monitored via Grafana, alerts on error rate spike