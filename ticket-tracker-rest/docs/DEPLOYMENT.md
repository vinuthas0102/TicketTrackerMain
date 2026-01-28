# Deployment Guide

Complete guide to deploy Ticket Tracker REST Edition to production.

## Prerequisites

### Server Requirements

- **Web Server**: Nginx or Apache
- **Node.js**: Version 18+ (for building)
- **RAM**: Minimum 512MB
- **Disk**: Minimum 500MB free space
- **OS**: Linux (Ubuntu/CentOS/Debian recommended)

### Backend Requirements

- Java backend API must be running and accessible
- All required API endpoints implemented (see API_SPECIFICATION.md)
- CORS enabled for your frontend domain

## Deployment Steps

### Step 1: Download and Extract Package

```bash
# Extract the package
unzip ticket-tracker-rest.zip
cd ticket-tracker-rest/frontend
```

### Step 2: Install Dependencies

```bash
npm install
```

This will install all required Node.js dependencies.

### Step 3: Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit environment file
nano .env
```

Update these critical values:

```bash
VITE_API_BASE_URL=https://your-backend-domain.com/api
VITE_API_TIMEOUT=30000
VITE_ENABLE_LOGGING=false
```

### Step 4: Build for Production

```bash
npm run build
```

This creates an optimized production build in `frontend/dist/`.

### Step 5: Deploy to Web Server

## Option A: Nginx Deployment

#### 1. Install Nginx

```bash
sudo apt update
sudo apt install nginx
```

#### 2. Copy Build Files

```bash
sudo mkdir -p /var/www/ticket-tracker
sudo cp -r frontend/dist/* /var/www/ticket-tracker/
sudo chown -R www-data:www-data /var/www/ticket-tracker
```

#### 3. Configure Nginx

Create configuration file:

```bash
sudo nano /etc/nginx/sites-available/ticket-tracker
```

Add this configuration:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/ticket-tracker;
    index index.html;

    # Enable gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/css text/javascript application/javascript application/json image/svg+xml;

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # SPA routing - serve index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to Java backend
    location /api {
        proxy_pass http://your-java-backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Increase timeout for long-running requests
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

#### 4. Enable Site and Restart Nginx

```bash
sudo ln -s /etc/nginx/sites-available/ticket-tracker /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Option B: Apache Deployment

#### 1. Install Apache

```bash
sudo apt update
sudo apt install apache2
```

#### 2. Enable Required Modules

```bash
sudo a2enmod rewrite
sudo a2enmod proxy
sudo a2enmod proxy_http
sudo a2enmod headers
```

#### 3. Copy Build Files

```bash
sudo mkdir -p /var/www/html/ticket-tracker
sudo cp -r frontend/dist/* /var/www/html/ticket-tracker/
sudo chown -R www-data:www-data /var/www/html/ticket-tracker
```

#### 4. Configure Apache

Create configuration file:

```bash
sudo nano /etc/apache2/sites-available/ticket-tracker.conf
```

Add this configuration:

```apache
<VirtualHost *:80>
    ServerName your-domain.com
    DocumentRoot /var/www/html/ticket-tracker

    <Directory /var/www/html/ticket-tracker>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted

        # Enable SPA routing
        RewriteEngine On
        RewriteBase /
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule ^ index.html [L]
    </Directory>

    # Proxy API requests to Java backend
    ProxyPass /api http://your-java-backend:8080/api
    ProxyPassReverse /api http://your-java-backend:8080/api
    ProxyPreserveHost On

    # Cache static assets
    <FilesMatch "\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$">
        Header set Cache-Control "max-age=31536000, public"
    </FilesMatch>

    # Security headers
    Header always set X-Frame-Options "SAMEORIGIN"
    Header always set X-Content-Type-Options "nosniff"
    Header always set X-XSS-Protection "1; mode=block"
</VirtualHost>
```

#### 5. Enable Site and Restart Apache

```bash
sudo a2ensite ticket-tracker
sudo apache2ctl configtest
sudo systemctl restart apache2
```

## Option C: Docker Deployment

See `docker/README.md` for Docker deployment instructions.

### Step 6: Configure SSL (Highly Recommended)

#### Using Let's Encrypt (Free SSL)

For Nginx:

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Get SSL certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal is configured automatically
```

For Apache:

```bash
# Install Certbot
sudo apt install certbot python3-certbot-apache

# Get SSL certificate
sudo certbot --apache -d your-domain.com

# Auto-renewal is configured automatically
```

### Step 7: Verify Deployment

1. Open browser: `https://your-domain.com`
2. Test login with credentials
3. Create a test ticket
4. Upload a file
5. Check audit logs
6. Verify all features work

## Post-Deployment

### Monitoring

Monitor these key metrics:

- Application response time
- API error rates
- File upload success rates
- User session duration

### Backup Strategy

Regularly backup:

- Environment configuration files
- Web server configuration
- SSL certificates

### Updates

To update the application:

```bash
# Build new version
npm run build

# Backup current deployment
sudo cp -r /var/www/ticket-tracker /var/www/ticket-tracker.backup

# Deploy new version
sudo cp -r frontend/dist/* /var/www/ticket-tracker/

# Clear browser cache or use cache busting
```

## Troubleshooting

### Application Won't Load

1. Check web server is running:
   ```bash
   sudo systemctl status nginx  # or apache2
   ```

2. Check file permissions:
   ```bash
   ls -la /var/www/ticket-tracker
   ```

3. Check web server error logs:
   ```bash
   sudo tail -f /var/log/nginx/error.log  # or /var/log/apache2/error.log
   ```

### API Requests Failing

1. Check backend is running
2. Verify CORS configuration on backend
3. Check network connectivity between frontend and backend
4. Review browser console for errors

### 404 Errors on Page Refresh

- Ensure SPA routing is configured correctly in web server
- Check `try_files` (Nginx) or `RewriteRule` (Apache) configuration

### SSL Certificate Issues

```bash
# Check certificate status
sudo certbot certificates

# Renew certificates manually
sudo certbot renew
```

## Performance Optimization

### Enable Compression

Already included in Nginx/Apache configurations above.

### CDN Integration

For better global performance, consider using a CDN:

1. Upload `dist/` contents to CDN
2. Update `index.html` to load assets from CDN
3. Keep API proxying on your domain

### Cache Configuration

Adjust cache headers based on your update frequency:

- Static assets: 1 year
- index.html: No cache
- API responses: As appropriate for your data

## Security Best Practices

1. **Always use HTTPS** in production
2. **Keep dependencies updated**: Run `npm audit` regularly
3. **Use strong passwords** for all accounts
4. **Enable rate limiting** on your backend API
5. **Regular security audits** of your deployment
6. **Firewall configuration**: Only allow necessary ports
7. **Regular backups**: Automated and tested

## Support

If you encounter issues not covered in this guide, check:

- [Configuration Guide](./CONFIGURATION.md)
- [API Specification](./API_SPECIFICATION.md)
- Web server documentation
