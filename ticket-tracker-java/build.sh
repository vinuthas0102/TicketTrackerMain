#!/bin/bash
##############################################################################
# Build Script for Ticket Tracker Java Application (Unix/Linux)
##############################################################################

set -e  # Exit on error

echo "==========================================================================="
echo "Ticket Tracker - Java Build Script"
echo "==========================================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven 3.6+ and try again."
    exit 1
fi

print_info "Maven found: $(mvn --version | head -n 1)"
echo ""

# Check Java version
if ! command -v java &> /dev/null; then
    print_error "Java is not installed. Please install JDK 8 and try again."
    exit 1
fi

print_info "Java found: $(java -version 2>&1 | head -n 1)"
echo ""

# Parse command line arguments
SKIP_TESTS=false
CLEAN=true

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --no-clean)
            CLEAN=false
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Usage: $0 [--skip-tests] [--no-clean]"
            exit 1
            ;;
    esac
done

# Clean project
if [ "$CLEAN" = true ]; then
    print_info "Cleaning project..."
    mvn clean
    echo ""
fi

# Build project
print_info "Building project..."
if [ "$SKIP_TESTS" = true ]; then
    print_warn "Skipping tests"
    mvn package -DskipTests
else
    mvn package
fi

echo ""

# Check if WAR was created
WAR_FILE="target/ticket-tracker.war"
if [ -f "$WAR_FILE" ]; then
    print_info "Build successful!"
    echo ""
    print_info "WAR file created: $WAR_FILE"
    print_info "WAR file size: $(du -h $WAR_FILE | cut -f1)"
    echo ""
    print_info "Next steps:"
    echo "  1. Deploy WAR to Tomcat: cp $WAR_FILE \$TOMCAT_HOME/webapps/"
    echo "  2. Start Tomcat: \$TOMCAT_HOME/bin/startup.sh"
    echo "  3. Access application: http://localhost:8080/ticket-tracker"
    echo ""
else
    print_error "Build failed! WAR file not found."
    exit 1
fi

echo "==========================================================================="
