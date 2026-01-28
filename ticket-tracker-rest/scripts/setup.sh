#!/bin/bash

# Setup script for Ticket Tracker REST Edition
# This script performs initial setup and configuration

set -e

echo "================================================"
echo "Ticket Tracker - Setup Script"
echo "================================================"
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed"
    echo ""
    echo "Please install Node.js 18 or later:"
    echo "  Ubuntu/Debian: curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash - && sudo apt-get install -y nodejs"
    echo "  CentOS/RHEL: curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash - && sudo yum install -y nodejs"
    echo "  macOS: brew install node"
    echo "  Or download from: https://nodejs.org/"
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "ERROR: Node.js version 18 or higher is required"
    echo "Current version: $(node -v)"
    echo "Please upgrade Node.js"
    exit 1
fi

echo "✓ Node.js version: $(node -v)"
echo "✓ npm version: $(npm -v)"
echo ""

# Navigate to frontend directory
cd "$(dirname "$0")/../frontend" || exit 1

# Install dependencies
echo "Installing dependencies..."
npm install

if [ $? -eq 0 ]; then
    echo "✓ Dependencies installed successfully"
else
    echo "✗ Failed to install dependencies"
    exit 1
fi

echo ""

# Setup environment file
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp .env.example .env
    echo "✓ .env file created"
    echo ""
    echo "IMPORTANT: Please configure your environment variables"
    echo ""

    # Prompt for API URL
    read -p "Enter your backend API URL (e.g., https://api.yourcompany.com/api): " API_URL
    if [ ! -z "$API_URL" ]; then
        sed -i.bak "s|VITE_API_BASE_URL=.*|VITE_API_BASE_URL=$API_URL|" .env
        rm -f .env.bak
        echo "✓ API URL configured: $API_URL"
    fi
    echo ""
else
    echo "✓ .env file already exists"
    echo ""
fi

# Display current configuration
echo "Current Configuration:"
echo "────────────────────────────────────────────────"
grep "^VITE_" .env || echo "No configuration found"
echo "────────────────────────────────────────────────"
echo ""

# Test build
echo "Running test build..."
npm run build

if [ $? -eq 0 ]; then
    echo "✓ Test build successful"
    rm -rf dist
else
    echo "✗ Test build failed"
    echo "Please check for errors above"
    exit 1
fi

echo ""
echo "================================================"
echo "Setup completed successfully!"
echo "================================================"
echo ""
echo "Next steps:"
echo ""
echo "1. Review configuration:"
echo "   nano .env"
echo ""
echo "2. Start development server:"
echo "   cd frontend && npm run dev"
echo ""
echo "3. Build for production:"
echo "   ./scripts/build.sh"
echo ""
echo "4. Deploy to server:"
echo "   sudo ./scripts/deploy.sh"
echo ""
echo "For detailed instructions, see:"
echo "  - docs/DEPLOYMENT.md"
echo "  - docs/CONFIGURATION.md"
echo "  - docs/API_SPECIFICATION.md"
echo ""
