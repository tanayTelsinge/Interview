# Docker Notes

## Core Mental Model
**Any service you need locally → run it in Docker. Never install databases or middleware directly on your machine.**

```
Need PostgreSQL?  → docker run postgres
Need Redis?       → docker run redis
Need Kafka?       → docker run kafka
Need MongoDB?     → docker run mongo
```

---

## Key Flags

| Flag | Purpose | Example |
|---|---|---|
| `--name` | Container name | `--name employeedb` |
| `-e` | Environment variable | `-e POSTGRES_PASSWORD=postgres` |
| `-p` | Port mapping host:container | `-p 5432:5432` |
| `-d` | Detached — runs in background | `-d` |
| `-v` | Volume — persist data | `-v postgres_data:/var/lib/postgresql/data` |

---

## Line Continuation
`\` splits a long command across multiple lines for readability — it's still one command:
```bash
# Same command, two ways
docker run --name employeedb \
  -e POSTGRES_USER=postgres \
  -d postgres:15

docker run --name employeedb -e POSTGRES_USER=postgres -d postgres:15
```

---

## Essential Commands

```bash
docker ps                    # list running containers
docker ps -a                 # list all including stopped
docker stop employeedb       # stop container
docker start employeedb      # start again
docker rm employeedb         # delete container
docker logs employeedb       # check logs
docker exec -it employeedb psql -U postgres  # shell into container
```

---

## Volumes — When and Why

**Rule: Any image that stores state needs a volume. Stateless services don't.**

Without volume → data lost when container restarts.  
With volume → data persists across restarts.

### How to find volume path
Go to `hub.docker.com/_/{imagename}` → look for **"Where to Store Data"** section.

### Common volume paths
```bash
PostgreSQL  →  /var/lib/postgresql/data
MongoDB     →  /data/db
Redis       →  /data
MySQL       →  /var/lib/mysql
```

---

## Environment Variables — How to Find Them
**Source of truth: Docker Hub official image page**

```
hub.docker.com/_/{imagename}
→ Look for "Environment Variables" section
→ Never rely on Google or Stack Overflow
```

### PostgreSQL env vars
```
POSTGRES_USER      # required — creates superuser
POSTGRES_PASSWORD  # required — superuser password
POSTGRES_DB        # optional — creates DB on startup, defaults to POSTGRES_USER
```

---

## Docker Run — PostgreSQL Example
```bash
docker run --name employeedb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=employeedb \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  -d postgres:15
```

---

## Docker Compose — Preferred for Projects

Instead of long `docker run` commands, use `docker-compose.yml`:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    container_name: employeedb
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: employeedb
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

```bash
docker-compose up -d    # start all services
docker-compose down     # stop all services
docker-compose logs     # view logs
```

✅ Repeatable  
✅ Version controlled  
✅ Easy to add more services (Redis, Kafka, etc.)  

---

## Habit for Any New Service
```
1. hub.docker.com → search official image
2. Read "Environment Variables" section
3. Read "Where to Store Data" section
4. Copy example run command from the page
5. Modify to your needs
```

---

## Stateful vs Stateless

| Type | Examples | Needs Volume? |
|---|---|---|
| Stateful | PostgreSQL, MongoDB, Redis | ✅ Yes |
| Stateless | Spring Boot app, Nginx | ❌ No |