# Quick Start Guide - Ticket Tracker Conversion

This guide will help you get started with the Ticket Tracker Tomcat/Oracle conversion project.

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17 or higher**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Verify installation: `java -version`

2. **Maven 3.8 or higher**
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **Oracle Database 19c or higher**
   - Oracle Express Edition (free): https://www.oracle.com/database/technologies/xe-downloads.html
   - Or Oracle Cloud Always Free Tier
   - Or Oracle Database Docker image

4. **IDE (Recommended)**
   - IntelliJ IDEA Community Edition (free) or Ultimate
   - Eclipse IDE for Enterprise Java Developers
   - Visual Studio Code with Java extensions

5. **Apache Tomcat 10+ (for deployment)**
   - Download from: https://tomcat.apache.org/download-10.cgi
   - Or use embedded Tomcat from Spring Boot (for development)

6. **Web Browser**
   - Chrome, Firefox, Edge, or Safari (latest version)

### Optional Software

- Oracle SQL Developer or DBeaver (database management)
- Postman or Insomnia (API testing)
- Git (version control)

## Step 1: Database Setup (30 minutes)

### 1.1 Connect to Oracle Database

Using SQL*Plus:
```bash
sqlplus username/password@localhost:1521/ORCL
```

Using SQL Developer:
- Create a new connection
- Connection Name: TicketTrackerDB
- Username: your_username
- Password: your_password
- Connection Type: Basic
- Hostname: localhost
- Port: 1521
- SID: ORCL

### 1.2 Create Database Schema

```sql
-- Run the schema creation script
@/path/to/database/01_CREATE_ORACLE_SCHEMA.sql

-- Verify tables were created
SELECT table_name FROM user_tables;
-- Should show 14 tables
```

### 1.3 Insert Seed Data

```sql
-- Run the seed data script
@/path/to/database/02_INSERT_SEED_DATA.sql

-- Verify data insertion
SELECT COUNT(*) FROM users;
-- Should return 8

SELECT COUNT(*) FROM modules;
-- Should return 5
```

### 1.4 Test Database Connection

```sql
-- Test query
SELECT name, email, role FROM users;

-- Should return all 8 users including admin@company.com
```

## Step 2: Backend Setup (1 hour)

### 2.1 Open Project in IDE

1. Open IntelliJ IDEA or your preferred IDE
2. File → Open → Navigate to `backend/` directory
3. Wait for Maven to download dependencies (first time takes 5-10 minutes)

### 2.2 Configure Database Connection

Edit `backend/src/main/resources/application.properties`:

```properties
# Update these values for your Oracle database
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:ORCL
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### 2.3 Configure File Storage

Create the upload directory:

```bash
# Linux/Mac
mkdir -p /var/ticket-tracker/uploads

# Windows (create manually or use PowerShell)
New-Item -Path "C:\ticket-tracker\uploads" -ItemType Directory
```

Update application.properties:
```properties
# Linux/Mac
file.storage.location=/var/ticket-tracker/uploads

# Windows
file.storage.location=C:/ticket-tracker/uploads
```

### 2.4 Build the Project

```bash
cd backend
mvn clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
```

### 2.5 Run the Application

**Option A: Using Maven (Development)**
```bash
mvn spring-boot:run
```

**Option B: Using IDE**
- Right-click on `TicketTrackerApplication.java`
- Select "Run 'TicketTrackerApplication'"

**Option C: Using JAR file**
```bash
java -jar target/ticket-tracker.war
```

### 2.6 Verify Backend is Running

Open browser to: http://localhost:8080/ticket-tracker/api/users

Expected response: JSON array of users

If you see a 404 error, wait a few seconds for the application to fully start.

## Step 3: Frontend Setup (15 minutes)

### 3.1 Navigate to Frontend Directory

```bash
cd frontend
```

### 3.2 Update API Base URL (if needed)

Edit `frontend/js/main.js`:

```javascript
// Line 22: Update if backend is on different port/host
this.apiClient.setBaseUrl('http://localhost:8080/ticket-tracker/api');
```

### 3.3 Serve Frontend Files

**Option A: Using Python HTTP Server**
```bash
# Python 3
python -m http.server 3000

# Python 2
python -m SimpleHTTPServer 3000
```

**Option B: Using Node.js http-server**
```bash
npx http-server -p 3000
```

**Option C: Using VS Code Live Server Extension**
- Install "Live Server" extension
- Right-click on `index.html`
- Select "Open with Live Server"

### 3.4 Open Application

Open browser to: http://localhost:3000

You should see the login page.

## Step 4: Test the Application (10 minutes)

### 4.1 Login with Test Credentials

Use one of these test accounts:

**Admin User:**
- Email: `admin@company.com`
- Password: `admin`
- Role: Executive Officer (EO) - Full access

**Manager User:**
- Email: `manager@company.com`
- Password: `manager`
- Role: Department Officer (DO) - Department access

**Employee User:**
- Email: `john@company.com`
- Password: `user`
- Role: Employee - Limited access

### 4.2 Explore the Dashboard

After login, you should see:
- Header with app name and user info
- Sidebar with navigation
- Main content area with dashboard
- Status cards (currently showing 0 tickets)

### 4.3 Test API Endpoints

Open Postman or use curl to test:

**Get all users:**
```bash
curl http://localhost:8080/ticket-tracker/api/users
```

**Get user by ID:**
```bash
curl http://localhost:8080/ticket-tracker/api/users/550e8400-e29b-41d4-a716-446655440001
```

**Get user by email:**
```bash
curl http://localhost:8080/ticket-tracker/api/users/email/admin@company.com
```

## Step 5: Development Workflow

### Backend Development

1. Make changes to Java code
2. Build project: `mvn clean install`
3. Restart application
4. Test changes via API or frontend

### Frontend Development

1. Make changes to JavaScript/HTML/CSS files
2. Refresh browser (no build step needed)
3. Check browser console for errors
4. Test functionality

### Database Changes

1. Write migration SQL script
2. Test on development database
3. Apply to production when ready

## Common Issues and Solutions

### Issue: Database connection error

**Symptom:** `Unable to connect to Oracle database`

**Solution:**
1. Verify Oracle database is running
2. Check connection details in application.properties
3. Test connection using SQL*Plus or SQL Developer
4. Verify firewall is not blocking port 1521

### Issue: Maven build fails

**Symptom:** `BUILD FAILURE` during `mvn clean install`

**Solution:**
1. Check Java version: `java -version` (must be 17+)
2. Check Maven version: `mvn -version` (must be 3.8+)
3. Delete `~/.m2/repository` and rebuild
4. Check internet connection (Maven downloads dependencies)

### Issue: Frontend shows blank page

**Symptom:** White/blank page when opening http://localhost:3000

**Solution:**
1. Check browser console for JavaScript errors (F12)
2. Verify all JavaScript files exist in correct locations
3. Check browser supports ES6 modules (use modern browser)
4. Ensure HTTP server is serving from frontend directory

### Issue: CORS errors in browser console

**Symptom:** `Access to fetch has been blocked by CORS policy`

**Solution:**
1. Verify backend CORS configuration in application.properties
2. Add frontend URL to `cors.allowed-origins`
3. Restart backend application
4. Clear browser cache

### Issue: 404 Not Found for API endpoints

**Symptom:** API calls return 404 error

**Solution:**
1. Verify backend is running: Check console output
2. Check URL includes context path: `/ticket-tracker/api/...`
3. Verify controller mapping matches URL
4. Check Spring Boot logs for startup errors

## Next Steps

1. **Complete Backend Implementation**
   - Review `docs/BACKEND_DEVELOPMENT_GUIDE.md`
   - Implement remaining entities (Ticket, WorkflowStep, etc.)
   - Create service layer for business logic
   - Add remaining REST controllers

2. **Complete Frontend Implementation**
   - Review `docs/FRONTEND_DEVELOPMENT_GUIDE.md`
   - Build ticket management UI
   - Implement workflow step components
   - Add file upload functionality

3. **Implement Security**
   - Review `docs/SECURITY_GUIDE.md`
   - Add Spring Security configuration
   - Implement session-based authentication
   - Add role-based access control

4. **Deploy to Tomcat**
   - Review `docs/DEPLOYMENT_GUIDE.md`
   - Build WAR file: `mvn clean package`
   - Deploy to Tomcat webapps directory
   - Configure production database

## Getting Help

1. **Check Documentation**
   - See all guides in `docs/` directory
   - Review original React code for business logic
   - Check example code in `backend/src/` and `frontend/js/`

2. **Verify Environment**
   - Java version 17+
   - Maven 3.8+
   - Oracle Database running
   - All dependencies installed

3. **Review Logs**
   - Backend logs: Console output or `logs/ticket-tracker.log`
   - Frontend errors: Browser console (F12)
   - Database logs: Oracle alert logs

4. **Compare with Original**
   - Original React app in parent directory (`../`)
   - Reference for business logic and UI/UX
   - Compare database queries and data flow

## Useful Commands Reference

```bash
# Backend
mvn clean install          # Build project
mvn spring-boot:run        # Run application
mvn test                   # Run tests
mvn package               # Create WAR file

# Database
sqlplus user/pass@db      # Connect to Oracle
@script.sql               # Run SQL script
DESC table_name           # Describe table structure
SELECT * FROM users;      # Query data

# Frontend
python -m http.server 3000    # Serve frontend (Python 3)
npx http-server -p 3000       # Serve frontend (Node.js)

# Git (if using version control)
git status                # Check changes
git add .                 # Stage all changes
git commit -m "message"   # Commit changes
git pull                  # Get latest code
git push                  # Push to remote
```

## Project Structure Quick Reference

```
tomcat-oracle-conversion/
├── backend/                          # Spring Boot backend
│   ├── src/main/java/               # Java source code
│   │   └── com/tickettracker/
│   │       ├── entity/              # JPA entities
│   │       ├── repository/          # Data repositories
│   │       ├── service/             # Business logic
│   │       ├── controller/          # REST controllers
│   │       ├── config/              # Configuration classes
│   │       └── security/            # Security classes
│   ├── src/main/resources/          # Configuration files
│   │   └── application.properties   # App configuration
│   └── pom.xml                      # Maven dependencies
│
├── frontend/                         # Vanilla JavaScript frontend
│   ├── index.html                   # Main HTML file
│   ├── js/                          # JavaScript modules
│   │   ├── main.js                  # Application entry point
│   │   ├── api/                     # API client
│   │   ├── auth/                    # Authentication
│   │   ├── core/                    # Core utilities
│   │   └── components/              # UI components
│   ├── css/                         # Stylesheets
│   └── assets/                      # Images, icons, etc.
│
├── database/                         # Oracle database scripts
│   ├── 01_CREATE_ORACLE_SCHEMA.sql  # Schema creation
│   └── 02_INSERT_SEED_DATA.sql      # Sample data
│
└── docs/                            # Documentation
    ├── QUICK_START_GUIDE.md         # This file
    ├── BACKEND_DEVELOPMENT_GUIDE.md # Backend guide
    ├── FRONTEND_DEVELOPMENT_GUIDE.md# Frontend guide
    ├── DEPLOYMENT_GUIDE.md          # Deployment guide
    └── API_SPECIFICATION.md         # API documentation
```

## Success Checklist

- [ ] Oracle database running and accessible
- [ ] Schema created successfully (14 tables)
- [ ] Seed data inserted (8 users, 5 modules)
- [ ] Backend builds without errors
- [ ] Backend starts and responds to API calls
- [ ] Frontend serves without errors
- [ ] Can login with test credentials
- [ ] Dashboard displays after login
- [ ] No errors in browser console
- [ ] No errors in backend logs

Once all checkboxes are complete, you're ready to start development!

## Resources

- Spring Boot Documentation: https://spring.boot.io/docs
- Oracle Database Documentation: https://docs.oracle.com/database/
- Modern JavaScript Guide: https://javascript.info/
- Maven Documentation: https://maven.apache.org/guides/
- Apache Tomcat Documentation: https://tomcat.apache.org/tomcat-10.1-doc/

---

**Next:** Read `BACKEND_DEVELOPMENT_GUIDE.md` to start implementing remaining features.
