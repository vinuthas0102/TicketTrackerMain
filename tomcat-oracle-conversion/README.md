# Ticket Tracker - Tomcat/Oracle Version

This is a converted version of the original React/TypeScript ticket tracking application, redesigned for deployment on Apache Tomcat with Oracle database.

## Important Notice

This project is a **complete conversion** of the original React/TypeScript/Supabase application located in the parent directory. The original codebase remains **completely untouched** and fully functional.

## Project Structure

```
tomcat-oracle-conversion/
├── backend/           # Java Spring Boot REST API
├── frontend/          # Vanilla JavaScript/HTML/CSS frontend
├── database/          # Oracle database scripts and schema
├── docs/             # Comprehensive documentation
└── README.md         # This file
```

## Technology Stack

### Backend
- Java 17+
- Spring Boot 3.x
- Spring Security (Session-based authentication)
- Spring Data JPA
- Oracle JDBC Driver
- Maven for dependency management

### Frontend
- Vanilla JavaScript (ES6+)
- HTML5
- CSS3 (No frameworks, responsive design)
- SVG icons

### Database
- Oracle Database 19c or higher
- File storage: Filesystem with paths in database

## Key Differences from Original

| Feature | Original | This Version |
|---------|----------|--------------|
| Frontend Framework | React + TypeScript | Vanilla JavaScript |
| Backend | Supabase | Spring Boot REST API |
| Database | PostgreSQL (Supabase) | Oracle Database |
| Authentication | Supabase Auth | Session-based (Java Servlets) |
| File Storage | Supabase Storage | Filesystem + Oracle paths |
| Build Tool | Vite | Maven |
| Deployment | Static hosting | Apache Tomcat WAR |

## Features

All features from the original application are preserved:

- User authentication with role-based access control (EMPLOYEE, DO, EO, VENDOR)
- Module-based workflow management
- Ticket lifecycle management with status transitions
- Hierarchical workflow steps with dependencies
- File attachments and document management
- Comprehensive audit trail
- Dynamic field configuration system
- Bulk ticket and step creation
- Post-tender module with template wizard
- Search and filtering capabilities
- Responsive design for mobile and desktop

## Quick Start

See the `docs/` directory for detailed installation and configuration guides:

1. **Database Setup**: `docs/DATABASE_INSTALLATION.md`
2. **Backend Deployment**: `docs/BACKEND_DEPLOYMENT.md`
3. **Frontend Deployment**: `docs/FRONTEND_DEPLOYMENT.md`
4. **Configuration Guide**: `docs/CONFIGURATION_GUIDE.md`

## Development

This project was converted from the original React/TypeScript application and maintains the same business logic and user experience while adapting to enterprise Java/Oracle architecture.

## Version

Version: 1.0.0
Converted: 2025-11-05
Original Project: Ticket Tracker (React/TypeScript/Supabase)

## License

Same as original project
