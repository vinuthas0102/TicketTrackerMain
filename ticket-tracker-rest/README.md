# Ticket Tracker - REST Edition

A comprehensive, production-ready ticket tracking system with React frontend and RESTful API backend integration.

## Overview

This is a **standalone, deployable package** that replaces all Supabase calls with REST API calls to your Java backend. It's completely independent and ready to download and deploy in your production environment.

## Features

- **Full REST API Integration** - All backend calls via HTTP REST APIs
- **JWT Authentication** - Secure token-based authentication
- **Role-Based Access Control** - Support for EO, DO, Employee, Vendor, Finance roles
- **Ticket Management** - Complete ticket lifecycle management
- **Workflow Steps** - Multi-level hierarchical workflow support
- **File Management** - Upload, download, and manage attachments
- **Finance Approval Workflow** - Built-in approval process
- **Audit Trail** - Complete activity tracking
- **Responsive Design** - Works on all devices
- **Production Ready** - Optimized build, ready to deploy

## Quick Start

### Prerequisites

- Node.js 18+ installed
- Java backend API running (see [API Specification](./docs/API_SPECIFICATION.md))

### Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Copy environment template
cp .env.example .env

# Edit .env and set your API URL
nano .env

# Set VITE_API_BASE_URL to your backend URL
# Example: VITE_API_BASE_URL=https://api.yourcompany.com/api
```

### Development

```bash
# Start development server
npm run dev

# Access at http://localhost:3000
```

### Production Build

```bash
# Build for production
npm run build

# Output will be in frontend/dist/
# Deploy this folder to your web server
```

## Project Structure

```
ticket-tracker-rest/
├── frontend/                   # React application
│   ├── src/
│   │   ├── components/        # React components
│   │   ├── context/           # React context providers
│   │   ├── services/          # REST API service layer
│   │   ├── lib/               # Utilities and HTTP client
│   │   ├── types/             # TypeScript type definitions
│   │   └── data/              # Mock data
│   ├── public/                # Static assets
│   ├── package.json           # Dependencies
│   └── vite.config.ts         # Build configuration
├── docs/                      # Documentation
│   ├── DEPLOYMENT.md          # Deployment guide
│   ├── API_SPECIFICATION.md   # API endpoints specification
│   └── CONFIGURATION.md       # Configuration guide
├── scripts/                   # Deployment scripts
└── docker/                    # Docker configuration
```

## Default Credentials

For testing purposes:

- **Admin**: username: `admin`, password: `admin`
- **Manager**: username: `manager`, password: `manager`
- **User**: username: `user`, password: `user`

## Documentation

- [Deployment Guide](./docs/DEPLOYMENT.md) - Step-by-step deployment instructions
- [API Specification](./docs/API_SPECIFICATION.md) - Required backend API endpoints
- [Configuration Guide](./docs/CONFIGURATION.md) - Configuration options

## Technology Stack

- **Frontend**: React 18, TypeScript, Vite
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **HTTP Client**: Fetch API
- **Build Tool**: Vite

## Key Differences from Supabase Version

- **No Supabase Dependency** - Uses standard REST APIs
- **JWT Authentication** - Token-based auth instead of Supabase Auth
- **File Uploads** - Multipart form-data uploads
- **Data Transformation** - Automatic backend/frontend data mapping
- **Error Handling** - Comprehensive HTTP error handling

## Support

For issues and questions, refer to the documentation in the `docs/` folder.

## License

MIT License - See LICENSE file for details
