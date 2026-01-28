# Docker Deployment Guide

Deploy Ticket Tracker using Docker containers.

## Prerequisites

- Docker installed (version 20.10+)
- Docker Compose installed (version 2.0+)

## Quick Start

### 1. Configure Environment

Create `.env` file in the docker directory:

```bash
cd docker
cp ../.env.example .env
```

Edit `.env`:
```bash
VITE_API_BASE_URL=https://your-backend-api.com/api
VITE_API_TIMEOUT=30000
VITE_ENABLE_LOGGING=false
```

### 2. Build and Run

```bash
# Build image
docker-compose build

# Start container
docker-compose up -d

# View logs
docker-compose logs -f

# Stop container
docker-compose down
```

### 3. Access Application

Open browser: `http://localhost`

## Production Deployment

### Using Docker Image

Build production image:

```bash
# Build
docker build -t ticket-tracker:latest -f docker/Dockerfile .

# Run
docker run -d \
  -p 80:80 \
  --name ticket-tracker \
  --restart unless-stopped \
  ticket-tracker:latest
```

### With Environment Variables

```bash
docker run -d \
  -p 80:80 \
  --name ticket-tracker \
  -e VITE_API_BASE_URL=https://api.yourcompany.com/api \
  -e VITE_ENABLE_LOGGING=false \
  --restart unless-stopped \
  ticket-tracker:latest
```

### Behind Reverse Proxy

If running behind reverse proxy (Nginx/Apache):

```bash
docker run -d \
  -p 8080:80 \
  --name ticket-tracker \
  --restart unless-stopped \
  ticket-tracker:latest
```

Then configure your reverse proxy to forward to `localhost:8080`.

## Health Check

The container includes a health check endpoint:

```bash
# Check container health
docker ps

# Manual health check
curl http://localhost/health
```

## Logs

View logs:

```bash
# All logs
docker-compose logs

# Follow logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100

# Specific service
docker logs ticket-tracker-frontend
```

## Updating

To update the application:

```bash
# Pull latest code
git pull

# Rebuild image
docker-compose build --no-cache

# Restart container
docker-compose up -d
```

## Troubleshooting

### Container Won't Start

Check logs:
```bash
docker-compose logs frontend
```

### Cannot Access Application

1. Check container is running:
   ```bash
   docker ps
   ```

2. Check port binding:
   ```bash
   docker port ticket-tracker-frontend
   ```

3. Check firewall:
   ```bash
   sudo ufw status
   ```

### API Connection Issues

1. Check environment variables:
   ```bash
   docker exec ticket-tracker-frontend env | grep VITE
   ```

2. Check backend accessibility from container:
   ```bash
   docker exec ticket-tracker-frontend wget -O- http://your-backend:8080/api/health
   ```

## Advanced Configuration

### Custom Nginx Configuration

1. Modify `docker/nginx.conf`
2. Rebuild image:
   ```bash
   docker-compose build --no-cache
   ```

### Resource Limits

Add to `docker-compose.yml`:

```yaml
services:
  frontend:
    # ... existing config ...
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

### Volume Mounts

For development with hot reload:

```yaml
services:
  frontend:
    # ... existing config ...
    volumes:
      - ../frontend/src:/app/src:ro
```

## Production Best Practices

1. **Use specific tags** instead of `latest`:
   ```bash
   docker build -t ticket-tracker:v1.0.0 .
   ```

2. **Enable automatic restarts:**
   ```yaml
   restart: always
   ```

3. **Set resource limits** to prevent resource exhaustion

4. **Use Docker secrets** for sensitive data

5. **Regular backups** of important data

6. **Monitor container health** and logs

7. **Update images regularly** for security patches

## Docker Registry

### Push to Registry

```bash
# Tag image
docker tag ticket-tracker:latest registry.yourcompany.com/ticket-tracker:v1.0.0

# Push to registry
docker push registry.yourcompany.com/ticket-tracker:v1.0.0
```

### Pull from Registry

```bash
docker pull registry.yourcompany.com/ticket-tracker:v1.0.0
```

## Kubernetes Deployment

For Kubernetes deployment, see example manifests:

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ticket-tracker
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ticket-tracker
  template:
    metadata:
      labels:
        app: ticket-tracker
    spec:
      containers:
      - name: frontend
        image: ticket-tracker:latest
        ports:
        - containerPort: 80
        env:
        - name: VITE_API_BASE_URL
          value: "https://api.yourcompany.com/api"
        resources:
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: ticket-tracker
spec:
  selector:
    app: ticket-tracker
  ports:
  - port: 80
    targetPort: 80
  type: LoadBalancer
```

## Support

For issues with Docker deployment:
1. Check container logs
2. Verify environment variables
3. Test backend connectivity
4. Review nginx configuration
5. Check resource availability
