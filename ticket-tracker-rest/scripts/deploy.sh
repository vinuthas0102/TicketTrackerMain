#!/bin/bash

# Deployment script for Ticket Tracker REST Edition
# This script deploys the built application to a web server

set -e

echo "================================================"
echo "Ticket Tracker - Deployment Script"
echo "================================================"
echo ""

# Configuration - Modify these for your environment
DEPLOY_USER="${DEPLOY_USER:-www-data}"
DEPLOY_PATH="${DEPLOY_PATH:-/var/www/ticket-tracker}"
WEB_SERVER="${WEB_SERVER:-nginx}"  # nginx or apache2
BACKUP_PATH="${BACKUP_PATH:-/var/www/backups}"

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "This script should be run as root or with sudo"
    echo "Usage: sudo ./deploy.sh"
    exit 1
fi

# Check if build exists
if [ ! -d "$(dirname "$0")/../frontend/dist" ]; then
    echo "ERROR: Build not found"
    echo "Please run ./scripts/build.sh first"
    exit 1
fi

echo "Deployment Configuration:"
echo "  Deploy Path: $DEPLOY_PATH"
echo "  Deploy User: $DEPLOY_USER"
echo "  Web Server: $WEB_SERVER"
echo ""
read -p "Continue with deployment? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled"
    exit 0
fi

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_PATH"

# Backup existing deployment
if [ -d "$DEPLOY_PATH" ]; then
    BACKUP_NAME="ticket-tracker-$(date +%Y%m%d-%H%M%S).tar.gz"
    echo "Creating backup: $BACKUP_NAME"
    tar -czf "$BACKUP_PATH/$BACKUP_NAME" -C "$DEPLOY_PATH" . 2>/dev/null || echo "No previous deployment to backup"
fi

# Create deployment directory
echo "Creating deployment directory..."
mkdir -p "$DEPLOY_PATH"

# Copy files
echo "Deploying files..."
cp -r "$(dirname "$0")/../frontend/dist/"* "$DEPLOY_PATH/"

# Set permissions
echo "Setting permissions..."
chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH"
chmod -R 755 "$DEPLOY_PATH"

# Restart web server
echo "Restarting web server..."
if [ "$WEB_SERVER" = "nginx" ]; then
    if command -v nginx &> /dev/null; then
        nginx -t && systemctl restart nginx
        echo "Nginx restarted successfully"
    else
        echo "WARNING: nginx not found, skipping restart"
    fi
elif [ "$WEB_SERVER" = "apache2" ]; then
    if command -v apache2 &> /dev/null; then
        apache2ctl configtest && systemctl restart apache2
        echo "Apache restarted successfully"
    elif command -v httpd &> /dev/null; then
        httpd -t && systemctl restart httpd
        echo "Apache (httpd) restarted successfully"
    else
        echo "WARNING: apache2 not found, skipping restart"
    fi
fi

echo ""
echo "================================================"
echo "Deployment completed successfully!"
echo "================================================"
echo ""
echo "Deployment location: $DEPLOY_PATH"
echo "Backup location: $BACKUP_PATH/$BACKUP_NAME"
echo ""
echo "Next steps:"
echo "1. Test the application in your browser"
echo "2. Check web server logs for any errors"
echo "3. Monitor application performance"
echo ""
