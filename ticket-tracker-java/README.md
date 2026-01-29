# Ticket Tracker - Java Web Application

A comprehensive, production-ready ticket tracking system built with Java 8, Oracle Database 19c, and deployable on Apache Tomcat 8.5.

## Overview

This is a **separate Java implementation** of the ticket tracking system. It is completely independent from the React + Supabase version and designed for enterprise environments requiring:

- Java 8 compatibility
- Oracle Database 19c integration
- Tomcat 8.5 WAR deployment
- Pure JDBC with no ORM dependencies
- Traditional servlet-based architecture
- Database-driven file upload configuration

## Technology Stack

- **Language**: Java 8
- **Build Tool**: Maven 3.6+
- **Database**: Oracle Database 19c
- **Application Server**: Apache Tomcat 8.5
- **Frontend**: React 18 (adapted from original project)
- **Logging**: Log4j2
- **JSON Processing**: Jackson
- **Connection Pooling**: Apache Commons DBCP2

## Project Structure

```
ticket-tracker-java/
├── pom.xml                          # Maven build configuration
├── README.md                        # This file
├── src/
│   ├── main/
│   │   ├── java/                    # Java source code
│   │   │   └── com/tickettracker/
│   │   │       ├── config/          # Database and app configuration
│   │   │       ├── dao/             # Data Access Objects (JDBC)
│   │   │       ├── service/         # Business logic layer
│   │   │       ├── servlet/         # HTTP request handlers
│   │   │       ├── filter/          # Servlet filters (security, CORS)
│   │   │       ├── model/           # POJOs/Domain models
│   │   │       ├── util/            # Utility classes
│   │   │       ├── exception/       # Custom exceptions
│   │   │       └── listener/        # Lifecycle listeners
│   │   ├── resources/               # Configuration files
│   │   │   ├── database.properties
│   │   │   ├── log4j2.xml
│   │   │   └── application.properties
│   │   └── webapp/                  # Web application resources
│   │       ├── WEB-INF/
│   │       │   └── web.xml          # Servlet configuration
│   │       └── [React build output]
│   └── test/                        # Unit and integration tests
├── database/                        # Oracle SQL migration scripts
├── tomcat-config/                   # Tomcat configuration templates
├── docs/                            # Comprehensive documentation
├── scripts/                         # Build and deployment scripts
├── frontend-src/                    # Adapted React source code
└── target/                          # Maven build output
    └── ticket-tracker.war          # Deployable WAR file
```

## Quick Start

### Prerequisites

1. **Java Development Kit (JDK) 8** installed
2. **Apache Maven 3.6+** installed
3. **Oracle Database 19c** running and accessible
4. **Apache Tomcat 8.5** installed
5. **Node.js 18+** (for frontend build)

### Building the Application

```bash
# Navigate to the Java project directory
cd ticket-tracker-java

# Clean and build the WAR file
mvn clean package

# The WAR file will be created at: target/ticket-tracker.war
```

### Database Setup

```bash
# Run the Oracle database setup script
sqlplus sys/password@ORCL as sysdba @database/install.sql
```

### Deployment

```bash
# Copy the WAR file to Tomcat webapps directory
cp target/ticket-tracker.war $TOMCAT_HOME/webapps/

# Start Tomcat
$TOMCAT_HOME/bin/startup.sh

# Access the application
# http://localhost:8080/ticket-tracker
```

## Key Features

### Database-Driven File Upload Configuration

File upload rules are stored in the `file_upload_config` table, allowing dynamic configuration without code changes:

- Maximum file size per field
- Allowed file extensions
- Storage paths
- Mandatory/optional validation

### Security Features

- Session-based authentication
- Role-based access control (RBAC)
- Password hashing with SHA-256 and salt
- CSRF protection
- SQL injection prevention via PreparedStatements
- Failed login attempt tracking and account lockout

### Workflow Management

- Multi-step workflow support
- Step dependencies and blocking logic
- Progress tracking with percentage completion
- Document attachments per step
- Comprehensive audit trail

### Finance Approval Module

- Integrated finance approval workflow
- Tentative cost submission
- Approval/rejection with remarks
- Document upload support
- Status tracking

## Documentation

Detailed documentation is available in the `docs/` directory:

- **INSTALLATION.md** - Complete installation guide
- **DATABASE_SETUP.md** - Oracle database configuration
- **TOMCAT_DEPLOYMENT.md** - Deployment instructions
- **API_DOCUMENTATION.md** - REST API endpoints
- **CONFIGURATION_GUIDE.md** - Application configuration
- **TROUBLESHOOTING.md** - Common issues and solutions
- **ARCHITECTURE.md** - System architecture overview
- **SECURITY.md** - Security best practices

## Configuration

### Database Connection

Edit `src/main/resources/database.properties`:

```properties
db.url=jdbc:oracle:thin:@localhost:1521:ORCL
db.username=ticket_tracker_user
db.password=your_secure_password
db.driver=oracle.jdbc.OracleDriver
```

### File Upload Settings

Edit `src/main/resources/application.properties`:

```properties
upload.directory=/opt/ticket-tracker/uploads
upload.max.size=10485760
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/session` - Get current session

### Tickets
- `GET /api/tickets` - List all tickets
- `GET /api/tickets/{id}` - Get ticket details
- `POST /api/tickets` - Create new ticket
- `PUT /api/tickets/{id}` - Update ticket
- `DELETE /api/tickets/{id}` - Delete ticket

### Workflow Steps
- `GET /api/tickets/{ticketId}/steps` - Get ticket steps
- `POST /api/steps` - Create workflow step
- `PUT /api/steps/{id}` - Update step

### File Operations
- `POST /api/files/upload` - Upload file
- `GET /api/files/download/{id}` - Download file

### Finance Approval
- `POST /api/finance/submit` - Submit for finance approval
- `PUT /api/finance/approve` - Approve request
- `PUT /api/finance/reject` - Reject request

## Development

### Running Tests

```bash
mvn test
```

### Building Without Tests

```bash
mvn clean package -DskipTests
```

### Deployment Package

```bash
mvn clean package assembly:single
```

This creates a complete deployment ZIP in `target/` containing:
- WAR file
- Database scripts
- Configuration templates
- Documentation

## License

MIT License - See LICENSE file for details

## Support

For issues, questions, or contributions, please refer to the documentation in the `docs/` directory.

## Version

Current Version: 1.0.0
