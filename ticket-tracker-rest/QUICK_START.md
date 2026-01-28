# Quick Start Guide

Get up and running with Ticket Tracker REST Edition in minutes.

## For Developers (Local Testing)

### 1. Install and Setup (2 minutes)

```bash
# Run setup script
./scripts/setup.sh

# Or manually:
cd frontend
npm install
cp .env.example .env
```

### 2. Configure API URL (1 minute)

Edit `frontend/.env`:
```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

### 3. Start Development Server (1 minute)

```bash
cd frontend
npm run dev
```

Open: `http://localhost:3000`

### 4. Login

Default credentials:
- Username: `admin`
- Password: `admin`

## For Production Deployment (12 minutes)

### Method 1: Traditional Deployment

```bash
# 1. Setup (2 min)
./scripts/setup.sh

# 2. Configure production API URL (1 min)
nano frontend/.env
# Set: VITE_API_BASE_URL=https://your-api.com/api

# 3. Build (2 min)
./scripts/build.sh

# 4. Deploy to web server (5 min)
sudo ./scripts/deploy.sh

# 5. Verify (2 min)
# Open https://your-domain.com
```

### Method 2: Docker Deployment

```bash
# 1. Configure (1 min)
cd docker
cp ../.env.example .env
nano .env  # Set API URL

# 2. Build and run (3 min)
docker-compose up -d

# 3. Verify
# Open http://localhost
```

## Minimum Backend Requirements

Your Java backend must implement these endpoints:

1. **Authentication:**
   - `POST /api/auth/login` - Returns JWT token

2. **Users:**
   - `GET /api/users` - List users

3. **Modules:**
   - `GET /api/modules` - List modules

4. **Tickets:**
   - `GET /api/tickets?moduleId={id}` - List tickets
   - `POST /api/tickets` - Create ticket
   - `PUT /api/tickets/{id}` - Update ticket

5. **Workflow Steps:**
   - `POST /api/tickets/{ticketId}/steps` - Create step
   - `PUT /api/steps/{id}` - Update step

See [API_SPECIFICATION.md](./docs/API_SPECIFICATION.md) for complete details.

## Testing Without Backend

The application includes fallback mock data for testing the UI without a backend.

## Common Issues

### "Cannot connect to server"

**Problem:** Frontend can't reach backend API

**Solutions:**
1. Verify backend is running
2. Check `VITE_API_BASE_URL` in `.env`
3. Check CORS is enabled on backend
4. Check firewall/network settings

### "Session expired" on every login

**Problem:** JWT tokens not working

**Solutions:**
1. Verify backend generates valid JWT tokens
2. Check token expiration time
3. Verify `Authorization` header format

### Files won't upload

**Problem:** File upload failing

**Solutions:**
1. Check backend accepts multipart/form-data
2. Verify `VITE_MAX_FILE_SIZE` setting
3. Check backend file size limit
4. Verify file type restrictions

## Next Steps

1. **Customize** the application for your needs
2. **Configure** user roles and permissions
3. **Set up** SSL/TLS for production
4. **Monitor** application performance
5. **Backup** configuration regularly

## Getting Help

- [Full Documentation](./README.md)
- [Deployment Guide](./docs/DEPLOYMENT.md)
- [API Specification](./docs/API_SPECIFICATION.md)
- [Configuration Guide](./docs/CONFIGURATION.md)

## Support

For issues:
1. Check documentation in `docs/` folder
2. Review error logs
3. Verify backend API is working
4. Check environment configuration

## Development Workflow

```bash
# Make changes to code
nano frontend/src/...

# Test locally
npm run dev

# Build for production
npm run build

# Deploy
sudo ./scripts/deploy.sh
```

## Production Checklist

- [ ] Backend API is running and accessible
- [ ] All required endpoints implemented
- [ ] CORS configured on backend
- [ ] JWT authentication working
- [ ] `VITE_API_BASE_URL` set to production URL
- [ ] `VITE_ENABLE_LOGGING` set to `false`
- [ ] SSL/TLS configured
- [ ] Web server configured (Nginx/Apache)
- [ ] Firewall configured
- [ ] Backup strategy in place
- [ ] Monitoring configured

## Estimated Times

- **Initial setup:** 5 minutes
- **Development build:** 2 minutes
- **Production build:** 3 minutes
- **First deployment:** 15 minutes
- **Subsequent deployments:** 5 minutes

## Success!

You should now have a fully functional ticket tracking system running in your environment.

Happy tracking!
