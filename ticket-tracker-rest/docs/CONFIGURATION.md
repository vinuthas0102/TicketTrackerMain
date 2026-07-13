# Configuration Guide

Complete guide to configuring the Ticket Tracker REST Edition.

## Environment Variables

All configuration is done through environment variables in the `.env` file.

### Backend API Configuration

#### VITE_API_BASE_URL

**Required:** Yes
**Default:** `http://localhost:8080/api`
**Description:** Base URL of your Java backend API

**Examples:**
```bash
# Development
VITE_API_BASE_URL=http://localhost:8080/api

# Production
VITE_API_BASE_URL=https://api.yourcompany.com/api

# Staging
VITE_API_BASE_URL=https://staging-api.yourcompany.com/api
```

#### VITE_API_TIMEOUT

**Required:** No
**Default:** `30000` (30 seconds)
**Description:** Request timeout in milliseconds

**Examples:**
```bash
# Default (30 seconds)
VITE_API_TIMEOUT=30000

# Long timeout for slow connections (60 seconds)
VITE_API_TIMEOUT=60000

# Short timeout for fast networks (15 seconds)
VITE_API_TIMEOUT=15000
```

### Application Configuration

#### VITE_APP_NAME

**Required:** No
**Default:** `Ticket Tracker`
**Description:** Application name displayed in UI

```bash
VITE_APP_NAME=My Company Ticket System
```

#### VITE_ENABLE_LOGGING

**Required:** No
**Default:** `false`
**Description:** Enable detailed API logging to browser console

```bash
# Development - Enable logging
VITE_ENABLE_LOGGING=true

# Production - Disable logging
VITE_ENABLE_LOGGING=false
```

**Warning:** Never enable logging in production as it may expose sensitive data.

### File Upload Configuration

#### VITE_MAX_FILE_SIZE

**Required:** No
**Default:** `10485760` (10MB)
**Description:** Maximum file size in bytes

**Examples:**
```bash
# 10 MB
VITE_MAX_FILE_SIZE=10485760

# 50 MB
VITE_MAX_FILE_SIZE=52428800

# 100 MB
VITE_MAX_FILE_SIZE=104857600
```

#### VITE_ALLOWED_FILE_TYPES

**Required:** No
**Default:** `.pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png`
**Description:** Comma-separated list of allowed file extensions

**Examples:**
```bash
# Documents only
VITE_ALLOWED_FILE_TYPES=.pdf,.doc,.docx

# Documents and images
VITE_ALLOWED_FILE_TYPES=.pdf,.doc,.docx,.jpg,.jpeg,.png

# All common types
VITE_ALLOWED_FILE_TYPES=.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.gif,.zip
```

### Feature Flags

#### VITE_ENABLE_BULK_OPERATIONS

**Required:** No
**Default:** `true`
**Description:** Enable/disable bulk ticket and step creation

```bash
# Enable bulk operations
VITE_ENABLE_BULK_OPERATIONS=true

# Disable bulk operations
VITE_ENABLE_BULK_OPERATIONS=false
```

#### VITE_ENABLE_FILE_REFERENCES

**Required:** No
**Default:** `true`
**Description:** Enable/disable file reference templates feature

```bash
VITE_ENABLE_FILE_REFERENCES=true
```

#### VITE_ENABLE_FINANCE_APPROVAL

**Required:** No
**Default:** `true`
**Description:** Enable/disable finance approval workflow

```bash
VITE_ENABLE_FINANCE_APPROVAL=true
```

## Environment-Specific Configurations

### Development Environment

`.env.development`:
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_API_TIMEOUT=30000
VITE_ENABLE_LOGGING=true
VITE_MAX_FILE_SIZE=104857600
```

### Staging Environment

`.env.staging`:
```bash
VITE_API_BASE_URL=https://staging-api.yourcompany.com/api
VITE_API_TIMEOUT=30000
VITE_ENABLE_LOGGING=false
VITE_MAX_FILE_SIZE=52428800
```

### Production Environment

`.env.production`:
```bash
VITE_API_BASE_URL=https://api.yourcompany.com/api
VITE_API_TIMEOUT=30000
VITE_ENABLE_LOGGING=false
VITE_MAX_FILE_SIZE=10485760
VITE_ENABLE_BULK_OPERATIONS=true
VITE_ENABLE_FILE_REFERENCES=true
VITE_ENABLE_FINANCE_APPROVAL=true
```

## Build Configuration

### Vite Configuration

File: `vite.config.ts`

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: false,        // Disable in production
    minify: 'terser',        // Minification
    chunkSizeWarningLimit: 1000,
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### TypeScript Configuration

File: `tsconfig.json`

Key settings:
- `strict: true` - Enable strict type checking
- `esModuleInterop: true` - Enable ES module interop
- Target ES2020 or later

## Web Server Configuration

### Nginx Configuration Tips

1. **Enable Gzip Compression:**
   ```nginx
   gzip on;
   gzip_types text/css text/javascript application/javascript application/json;
   ```

2. **Set Cache Headers:**
   ```nginx
   location ~* \.(js|css|png|jpg)$ {
       expires 1y;
       add_header Cache-Control "public, immutable";
   }
   ```

3. **Increase Upload Size:**
   ```nginx
   client_max_body_size 100M;
   ```

4. **Timeout Settings:**
   ```nginx
   proxy_connect_timeout 60s;
   proxy_send_timeout 60s;
   proxy_read_timeout 60s;
   ```

### Apache Configuration Tips

1. **Enable Compression:**
   ```apache
   <IfModule mod_deflate.c>
       AddOutputFilterByType DEFLATE text/html text/css application/javascript
   </IfModule>
   ```

2. **Set Cache Headers:**
   ```apache
   <FilesMatch "\.(js|css|png|jpg)$">
       Header set Cache-Control "max-age=31536000, public"
   </FilesMatch>
   ```

3. **Increase Upload Size:**
   ```apache
   LimitRequestBody 104857600
   ```

## Backend Configuration Requirements

Your Java backend must be configured to:

### 1. Accept CORS Requests

```java
// Example Spring Boot CORS configuration
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("https://your-frontend-domain.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
```

### 2. Handle File Uploads

- Accept multipart/form-data
- Support file size up to your configured limit
- Return file ID in response

### 3. Generate JWT Tokens

- Use HS256 or RS256 algorithm
- Include user information in payload
- Set appropriate expiration time (e.g., 24 hours)

### 4. Implement Request Logging

Log all incoming requests for debugging and auditing.

## Security Configuration

### JWT Configuration

**Token Expiration:**
- Access Token: 24 hours
- Refresh Token: 7 days

**Token Storage:**
- Client stores tokens in localStorage
- Tokens cleared on logout
- Auto-logout on token expiration

### Password Policy

Enforce on backend:
- Minimum 8 characters
- Mix of uppercase, lowercase, numbers
- Special characters recommended
- No common passwords

### Rate Limiting

Implement on backend:
- Login: 5 attempts per minute
- API calls: 100 requests per minute per user
- File uploads: 10 per minute per user

### Session Management

- Single session per user (optional)
- Session timeout: 30 minutes of inactivity
- Remember me: 30 days

## Performance Tuning

### Browser Caching

Configure appropriate cache headers:

**index.html:** No cache
```
Cache-Control: no-cache, no-store, must-revalidate
```

**Static assets:** Long cache
```
Cache-Control: public, max-age=31536000, immutable
```

**API responses:** Short cache (if applicable)
```
Cache-Control: private, max-age=300
```

### Build Optimization

1. **Code Splitting:**
   Vite automatically splits code into chunks

2. **Tree Shaking:**
   Remove unused code automatically

3. **Minification:**
   Terser minification enabled

4. **Compression:**
   Enable gzip/brotli on web server

### Network Optimization

1. **HTTP/2:**
   Enable HTTP/2 on web server for better performance

2. **CDN Integration:**
   Use CDN for static assets in production

3. **API Response Compression:**
   Enable gzip compression on API responses

## Monitoring Configuration

### Application Monitoring

Monitor these metrics:

1. **API Response Times:**
   - Target: < 500ms
   - Alert: > 2000ms

2. **Error Rates:**
   - Target: < 1%
   - Alert: > 5%

3. **File Upload Success Rate:**
   - Target: > 95%
   - Alert: < 90%

### Infrastructure Monitoring

1. **Server Resources:**
   - CPU usage
   - Memory usage
   - Disk space

2. **Network:**
   - Bandwidth usage
   - Connection count
   - Request rate

### Logging Configuration

**Log Levels:**
- Development: DEBUG
- Staging: INFO
- Production: WARN

**Log Rotation:**
- Daily rotation
- Keep 30 days
- Compress old logs

## Troubleshooting Configuration Issues

### API Connection Issues

Check:
1. `VITE_API_BASE_URL` is correct
2. Backend is running and accessible
3. CORS is properly configured
4. Network connectivity between frontend and backend

### Build Issues

Check:
1. Node.js version (18+)
2. All dependencies installed (`npm install`)
3. No TypeScript errors (`npm run lint`)
4. Environment variables are set

### Authentication Issues

Check:
1. JWT token is being generated correctly
2. Token expiration is reasonable
3. Token is being sent in Authorization header
4. Backend is validating tokens correctly

### File Upload Issues

Check:
1. `VITE_MAX_FILE_SIZE` is appropriate
2. Backend accepts multipart/form-data
3. Backend file size limit matches or exceeds frontend limit
4. File type restrictions match between frontend and backend

## Best Practices

1. **Never commit `.env` files to version control**
2. **Use different API URLs for different environments**
3. **Disable logging in production**
4. **Set appropriate timeout values based on network conditions**
5. **Regularly review and update feature flags**
6. **Monitor application performance and adjust configuration**
7. **Keep dependencies up to date**
8. **Test configuration changes in staging before production**

## Configuration Checklist

Before deployment, verify:

- [ ] `VITE_API_BASE_URL` points to production API
- [ ] `VITE_ENABLE_LOGGING` is `false`
- [ ] File size limits are appropriate
- [ ] Feature flags are correctly set
- [ ] CORS is configured on backend
- [ ] JWT authentication is working
- [ ] File uploads are working
- [ ] All required API endpoints are implemented
- [ ] SSL/TLS is configured
- [ ] Web server compression is enabled
- [ ] Cache headers are set correctly
- [ ] Monitoring is configured

## Support

For configuration issues not covered here, refer to:
- [Deployment Guide](./DEPLOYMENT.md)
- [API Specification](./API_SPECIFICATION.md)
