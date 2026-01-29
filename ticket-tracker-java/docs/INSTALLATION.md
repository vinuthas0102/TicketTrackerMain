# Ticket Tracker - Installation Guide

Complete guide for installing and deploying the Java-based Ticket Tracker application.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Application Configuration](#application-configuration)
4. [Building the Application](#building-the-application)
5. [Deployment](#deployment)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- **Java Development Kit (JDK) 8** or higher
  - Download from: https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html
  - Verify installation: `java -version`

- **Apache Maven 3.6+**
  - Download from: https://maven.apache.org/download.cgi
  - Verify installation: `mvn --version`

- **Oracle Database 19c**
  - Must be installed and running
  - Access to create users and schemas

- **Apache Tomcat 8.5**
  - Download from: https://tomcat.apache.org/download-80.cgi
  - Recommended: Latest 8.5.x version

### System Requirements
- **Memory**: Minimum 2GB RAM (4GB+ recommended)
- **Disk Space**: 500MB for application + database
- **Operating System**: Windows, Linux, or macOS

## Database Setup

### Step 1: Create Database User

Connect to Oracle as SYSDBA:

```bash
sqlplus sys/password@ORCL as sysdba
```

Run the user creation script:

```sql
@database/01-oracle-create-user.sql
```

This creates the `ticket_tracker` user with password `ticket_pass_2024`.

**IMPORTANT**: Change the default password in production!

### Step 2: Run Database Migrations

Connect as the ticket_tracker user:

```bash
sqlplus ticket_tracker/ticket_pass_2024@ORCL
```

Run the master installation script:

```sql
@database/install.sql
```

This will:
- Create all 20 database tables
- Create sequences and triggers
- Add indexes and constraints
- Load seed data (modules, users, configurations)

### Step 3: Verify Database Installation

Check that tables were created:

```sql
SELECT COUNT(*) FROM user_tables;
```

Expected result: 20 tables

Check seed data:

```sql
SELECT COUNT(*) FROM modules;  -- Should return 5
SELECT COUNT(*) FROM users;    -- Should return 9+
```

## Application Configuration

### Step 1: Configure Database Connection

Edit `src/main/resources/database.properties`:

```properties
db.url=jdbc:oracle:thin:@your-host:1521:ORCL
db.username=ticket_tracker
db.password=your_secure_password
```

### Step 2: Configure Application Settings

Edit `src/main/resources/application.properties`:

```properties
# File upload directory (must exist and be writable)
upload.directory=/opt/ticket-tracker/uploads

# Maximum file size (10MB default)
upload.max.size=10485760
```

### Step 3: Create Upload Directory

```bash
# Linux/macOS
sudo mkdir -p /opt/ticket-tracker/uploads
sudo chown tomcat:tomcat /opt/ticket-tracker/uploads

# Windows
mkdir C:\ticket-tracker\uploads
```

## Building the Application

### Using Build Scripts

**Linux/macOS:**
```bash
cd ticket-tracker-java
./build.sh
```

**Windows:**
```cmd
cd ticket-tracker-java
build.bat
```

### Using Maven Directly

```bash
# Clean build with tests
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests
```

### Build Output

Successful build creates:
- `target/ticket-tracker.war` - Deployable WAR file
- `target/ticket-tracker/` - Exploded WAR directory

## Deployment

### Option 1: Deploy to Tomcat (Recommended)

1. **Copy WAR to Tomcat webapps:**

```bash
# Linux/macOS
cp target/ticket-tracker.war $TOMCAT_HOME/webapps/

# Windows
copy target\ticket-tracker.war %TOMCAT_HOME%\webapps\
```

2. **Start Tomcat:**

```bash
# Linux/macOS
$TOMCAT_HOME/bin/startup.sh

# Windows
%TOMCAT_HOME%\bin\startup.bat
```

3. **Access Application:**

Open browser: http://localhost:8080/ticket-tracker

### Option 2: Deploy via Tomcat Manager

1. Access Tomcat Manager: http://localhost:8080/manager
2. Scroll to "WAR file to deploy"
3. Choose `ticket-tracker.war`
4. Click "Deploy"

### Option 3: Manual Deployment

1. Stop Tomcat
2. Extract WAR to `$TOMCAT_HOME/webapps/ticket-tracker/`
3. Start Tomcat

## Verification

### 1. Check Tomcat Logs

```bash
# Linux/macOS
tail -f $TOMCAT_HOME/logs/catalina.out

# Windows
type %TOMCAT_HOME%\logs\catalina.out
```

Look for:
- "Database connection pool initialized successfully"
- Application startup messages
- No ERROR logs

### 2. Health Check Endpoint

```bash
curl http://localhost:8080/ticket-tracker/health
```

Expected response:
```json
{
  "status": "healthy",
  "database": "connected",
  "version": "1.0.0"
}
```

### 3. Test Login

Navigate to: http://localhost:8080/ticket-tracker

Use default credentials:
- **Email**: admin@company.com
- **Password**: changeme

**Change default passwords immediately!**

## Troubleshooting

### Database Connection Issues

**Problem**: Cannot connect to Oracle database

**Solutions**:
1. Verify Oracle is running:
   ```bash
   lsnrctl status
   ```

2. Check database.properties has correct URL, username, password

3. Test connection manually:
   ```bash
   sqlplus ticket_tracker/password@ORCL
   ```

4. Check Oracle listener is accepting connections

### Build Failures

**Problem**: Maven build fails

**Solutions**:
1. Check Java version: `java -version` (must be 8+)
2. Check Maven version: `mvn --version` (must be 3.6+)
3. Clear Maven cache: `mvn clean install -U`
4. Check internet connection for dependency downloads

### Deployment Issues

**Problem**: WAR not deploying to Tomcat

**Solutions**:
1. Check Tomcat logs in `$TOMCAT_HOME/logs/catalina.out`
2. Verify Tomcat version is 8.5.x
3. Ensure port 8080 is not in use
4. Check file permissions on WAR file
5. Remove old deployment:
   ```bash
   rm -rf $TOMCAT_HOME/webapps/ticket-tracker*
   ```

### Application Errors

**Problem**: 500 Internal Server Error

**Solutions**:
1. Check application logs in `logs/application.log`
2. Check database connection in `logs/sql.log`
3. Verify all database migrations ran successfully
4. Check upload directory exists and is writable

### File Upload Issues

**Problem**: Cannot upload files

**Solutions**:
1. Verify upload directory exists:
   ```bash
   ls -la /opt/ticket-tracker/uploads
   ```

2. Check directory permissions:
   ```bash
   chmod 755 /opt/ticket-tracker/uploads
   chown tomcat:tomcat /opt/ticket-tracker/uploads
   ```

3. Verify max file size in application.properties

## Post-Installation Security

### 1. Change Default Passwords

Update all default user passwords in database:

```sql
-- Update password hashes (use application to hash passwords properly)
UPDATE users SET password_hash = 'new_hash', password_salt = 'new_salt'
WHERE email = 'admin@company.com';
```

### 2. Secure Database Access

```sql
-- Change database user password
ALTER USER ticket_tracker IDENTIFIED BY new_secure_password;
```

Update `database.properties` with new password.

### 3. Configure HTTPS

Edit `$TOMCAT_HOME/conf/server.xml` to enable SSL/TLS.

Update web.xml:
```xml
<cookie-config>
    <secure>true</secure>
</cookie-config>
```

### 4. Restrict File Uploads

Edit `application.properties`:
```properties
upload.allowed.extensions=pdf,doc,docx
upload.max.size=5242880
```

## Next Steps

- [Configure field configurations](ADMIN_GUIDE.md)
- [Set up user management](USER_MANAGEMENT.md)
- [Review security settings](SECURITY.md)
- [Monitor application](MONITORING.md)

## Support

For issues and questions:
- Check logs in `logs/` directory
- Review [Troubleshooting Guide](TROUBLESHOOTING.md)
- Consult [API Documentation](API_DOCUMENTATION.md)
