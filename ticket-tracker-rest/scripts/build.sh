#!/bin/bash

# Build script for Ticket Tracker REST Edition
# This script builds the production-ready frontend application

set -e

echo "================================================"
echo "Ticket Tracker - Production Build Script"
echo "================================================"
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed"
    echo "Please install Node.js 18 or later from https://nodejs.org/"
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "ERROR: Node.js version 18 or higher is required"
    echo "Current version: $(node -v)"
    exit 1
fi

echo "Node.js version: $(node -v)"
echo "npm version: $(npm -v)"
echo ""

# Navigate to frontend directory
cd "$(dirname "$0")/../frontend" || exit 1

# Check if .env file exists
if [ ! -f .env ]; then
    echo "WARNING: .env file not found"
    echo "Copying .env.example to .env"
    cp .env.example .env
    echo ""
    echo "IMPORTANT: Please edit .env file and set your API URL before deploying"
    echo "Required: VITE_API_BASE_URL=https://your-backend-domain.com/api"
    echo ""
    read -p "Press Enter to continue or Ctrl+C to abort..."
fi

# Install dependencies
echo "Installing dependencies..."
npm install

# Run linting (optional, comment out if not needed)
echo ""
echo "Running linter..."
npm run lint || echo "WARNING: Linting found issues (continuing anyway)"

# Build the application
echo ""
echo "Building production bundle..."
npm run build

# Check if build was successful
if [ -d "dist" ]; then
    echo ""
    echo "================================================"
    echo "Build completed successfully!"
    echo "================================================"
    echo ""
    echo "Build output location: frontend/dist/"
    echo ""
    echo "Build statistics:"
    du -sh dist
    echo "Files: $(find dist -type f | wc -l)"
    echo ""
    echo "Next steps:"
    echo "1. Review the build output in frontend/dist/"
    echo "2. Test the build locally: npm run preview"
    echo "3. Deploy to your web server (see docs/DEPLOYMENT.md)"
    echo ""
else
    echo ""
    echo "ERROR: Build failed - dist directory not found"
    exit 1
fi
