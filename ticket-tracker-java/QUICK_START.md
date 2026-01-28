# Quick Start Guide - Java Backend

This guide provides step-by-step instructions to build, configure, and deploy the Java backend.

## Prerequisites

- **Java JDK 8** or higher
- **Apache Maven 3.6+**
- **Apache Tomcat 8.5+**
- **Oracle Database 19c**
- **Git** (for version control)

## Step 1: Database Setup

### 1.1 Create Oracle Database User

Connect to Oracle as SYSDBA and run:

```sql
@database/01-oracle-create-user.sql
```

This creates the `ticket_tracker` user with necessary privileges.

### 1.2 Create Database Schema

Connect as the `ticket_tracker` user and run scripts in order:

```bash
sqlplus ticket_tracker/your_password@//localhost:1521/ORCL

SQL> @database/02-oracle-schema.sql
SQL> @database/03-oracle-sequences.sql
SQL> @database/04-oracle-triggers.sql
SQL> @database/05-oracle-indexes.sql
SQL> @database/06-oracle-constraints.sql
SQL> @database/07-oracle-seed-data.sql
```

Or use the convenience script:
```bash
sqlplus ticket_tracker/your_password@//localhost:1521/ORCL @database/install.sql
```

## Step 2: Configuration

### 2.1 Database Connection

Edit `src/main/resources/database.properties`:

```properties
# Database connection
db.driver=oracle.jdbc.OracleDriver
db.url=jdbc:oracle:thin:@localhost:1521:ORCL
db.username=ticket_tracker
db.password=your_password

# Connection pool settings
db.pool.initial=5
db.pool.max=20
db.pool.idle=10
db.pool.maxWaitMillis=10000

# Connection validation
db.validation.query=SELECT 1 FROM DUAL
db.testOnBorrow=true
db.testWhileIdle=true
db.timeBetweenEvictionRunsMillis=60000
db.minEvictableIdleTimeMillis=300000

# Connection leak detection
db.removeAbandonedTimeout=300
db.removeAbandonedOnBorrow=true
db.removeAbandonedOnMaintenance=true
db.logAbandoned=true
```

### 2.2 Application Settings

Edit `src/main/resources/application.properties`:

```properties
# Application settings
app.name=Ticket Tracker
app.version=1.0.0
app.environment=production

# Session configuration
session.timeout=3600

# File upload settings
upload.max.size=10485760
upload.temp.dir=/tmp/ticket-tracker-uploads
```

### 2.3 Logging Configuration

Edit `src/main/resources/log4j2.xml` as needed.

Default configuration logs to:
- Console (INFO level)
- File: `logs/ticket-tracker.log` (DEBUG level)
- Rolling daily with 30-day retention

## Step 3: Build the Application

### 3.1 Clean and Compile

```bash
cd ticket-tracker-java
mvn clean compile
```

### 3.2 Run Tests (when available)

```bash
mvn test
```

### 3.3 Package as WAR

```bash
mvn clean package
```

This creates: `target/ticket-tracker.war`

### 3.4 Verify Build

```bash
ls -lh target/ticket-tracker.war
```

## Step 4: Deploy to Tomcat

### Option A: Automatic Deployment

1. Copy WAR to Tomcat webapps directory:
```bash
cp target/ticket-tracker.war $TOMCAT_HOME/webapps/
```

2. Start Tomcat:
```bash
$TOMCAT_HOME/bin/startup.sh
```

3. Monitor deployment:
```bash
tail -f $TOMCAT_HOME/logs/catalina.out
```

### Option B: Tomcat Manager Deployment

1. Access Tomcat Manager: `http://localhost:8080/manager/html`
2. Navigate to "WAR file to deploy"
3. Click "Choose File" and select `ticket-tracker.war`
4. Click "Deploy"

### Option C: Manual Deployment

1. Stop Tomcat:
```bash
$TOMCAT_HOME/bin/shutdown.sh
```

2. Remove old deployment:
```bash
rm -rf $TOMCAT_HOME/webapps/ticket-tracker*
```

3. Copy new WAR:
```bash
cp target/ticket-tracker.war $TOMCAT_HOME/webapps/
```

4. Start Tomcat:
```bash
$TOMCAT_HOME/bin/startup.sh
```

## Step 5: Verify Deployment

### 5.1 Check Application Status

Access: `http://localhost:8080/ticket-tracker/`

You should see a basic response or landing page.

### 5.2 Test API Health

```bash
# This should return a 401 Unauthorized (expected without login)
curl http://localhost:8080/ticket-tracker/api/tickets
```

Expected response:
```json
{
  "status": 401,
  "message": "Authentication required"
}
```

### 5.3 Test Authentication

#### Register a User
```bash
curl -X POST http://localhost:8080/ticket-tracker/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123",
    "role": "employee",
    "department": "IT"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/ticket-tracker/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

Save the session cookie from the response.

#### Create a Ticket
```bash
curl -X POST http://localhost:8080/ticket-tracker/api/tickets \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "title": "Test Ticket",
    "description": "This is a test ticket",
    "moduleId": "<module-id-from-database>",
    "priority": "high"
  }'
```

## Step 6: Production Considerations

### 6.1 Security Hardening

1. **Change default passwords** in database.properties
2. **Enable HTTPS** in Tomcat (configure SSL/TLS)
3. **Configure firewall** rules
4. **Disable Tomcat Manager** in production
5. **Set appropriate file permissions**

### 6.2 Performance Tuning

#### JVM Options (catalina.sh or setenv.sh)
```bash
export CATALINA_OPTS="$CATALINA_OPTS -Xms512m -Xmx2048m"
export CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxGCPauseMillis=200"
```

#### Connection Pool Tuning
Adjust in database.properties:
- `db.pool.initial=10`
- `db.pool.max=50`
- `db.pool.idle=20`

### 6.3 Monitoring

#### Application Logs
```bash
tail -f $TOMCAT_HOME/logs/catalina.out
tail -f logs/ticket-tracker.log
```

#### Database Monitoring
```sql
-- Active connections
SELECT COUNT(*) FROM v$session WHERE username = 'TICKET_TRACKER';

-- Monitor table sizes
SELECT table_name, num_rows
FROM user_tables
ORDER BY num_rows DESC;
```

### 6.4 Backup Strategy

#### Database Backup
```bash
# Daily backup using Oracle Data Pump
expdp ticket_tracker/password \
  directory=backup_dir \
  dumpfile=ticket_tracker_$(date +%Y%m%d).dmp \
  logfile=backup_$(date +%Y%m%d).log
```

#### Application Backup
```bash
# Backup WAR file and configuration
tar -czf ticket-tracker-backup-$(date +%Y%m%d).tar.gz \
  target/ticket-tracker.war \
  src/main/resources/*.properties
```

## Step 7: Troubleshooting

### Common Issues

#### 1. Database Connection Failed
- Check Oracle service is running
- Verify connection details in database.properties
- Test connection with sqlplus
- Check firewall settings

#### 2. WAR Deployment Failed
- Check Tomcat logs: `$TOMCAT_HOME/logs/catalina.out`
- Verify Java version compatibility
- Check for port conflicts (8080)
- Ensure sufficient disk space

#### 3. 404 Not Found
- Verify WAR deployed successfully
- Check context path: `/ticket-tracker/`
- Clear browser cache
- Check Tomcat webapps directory

#### 4. 500 Internal Server Error
- Check application logs
- Verify database connection
- Check for missing dependencies
- Review exception stack traces

#### 5. Session Issues
- Clear cookies
- Check session timeout settings
- Verify JSESSIONID cookie is set

### Debug Mode

Enable debug logging in log4j2.xml:

```xml
<Logger name="com.tickettracker" level="DEBUG"/>
```

Restart Tomcat to apply changes.

## Step 8: Development Workflow

### Hot Deployment (Development)

1. Make code changes
2. Rebuild WAR: `mvn clean package`
3. Redeploy to Tomcat
4. Test changes

### IDE Integration

#### Eclipse
1. Import as Maven project
2. Configure Tomcat server in Eclipse
3. Deploy and run from IDE

#### IntelliJ IDEA
1. Open project as Maven project
2. Configure Tomcat run configuration
3. Deploy and debug from IDE

## API Endpoints Reference

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `POST /api/auth/register` - User registration
- `POST /api/auth/change-password` - Change password
- `GET /api/auth/current` - Get current user

### Tickets
- `GET /api/tickets` - List all tickets
- `GET /api/tickets/{id}` - Get specific ticket
- `POST /api/tickets` - Create ticket
- `PUT /api/tickets/{id}` - Update ticket
- `DELETE /api/tickets/{id}` - Delete ticket

Query parameters for GET /api/tickets:
- `status` - Filter by status
- `moduleId` - Filter by module
- `search` - Full-text search

## Support and Documentation

- **Full Documentation**: See `JAVA_BACKEND_IMPLEMENTATION.md`
- **Database Scripts**: See `database/` directory
- **API Reference**: See `API_REFERENCE.md` (when available)
- **Troubleshooting**: See `TROUBLESHOOTING.md` (when available)

## Next Steps

1. **Add more endpoints** for workflow steps, documents, etc.
2. **Implement file upload** functionality
3. **Add API documentation** using Swagger
4. **Write unit tests** for services and DAOs
5. **Set up CI/CD** pipeline
6. **Configure monitoring** and alerting
7. **Implement caching** for performance
8. **Add rate limiting** for security

---

**Need Help?**
Check the logs, review the documentation, and verify your configuration settings.
