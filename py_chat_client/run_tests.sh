#!/bin/bash

# Python Chat Client Test Runner
# Usage: ./run_tests.sh [OPTIONS]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${BLUE}=== Python Chat Client Test Runner ===${NC}"
echo ""

# Parse options
VERBOSE=false
SPECIFIC_TEST=""
COVERAGE=false
MARKERS=""
COLLECT_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -t|--test)
            SPECIFIC_TEST="$2"
            shift 2
            ;;
        -c|--coverage)
            COVERAGE=true
            shift
            ;;
        -m|--marker)
            MARKERS="$2"
            shift 2
            ;;
        --collect-only)
            COLLECT_ONLY=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -v, --verbose        Verbose output"
            echo "  -t, --test PATH      Run specific test file or test"
            echo "  -c, --coverage       Generate coverage report"
            echo "  -m, --marker MARKER  Run tests with specific marker (e.g., unit, integration)"
            echo "  --collect-only       Collect tests without running"
            echo "  -h, --help           Show this help"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Check if pytest is installed
if ! command -v pytest &> /dev/null; then
    echo -e "${YELLOW}Warning: pytest not found. Installing dependencies...${NC}"
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt
    else
        echo -e "${RED}Error: requirements.txt not found${NC}"
        exit 1
    fi
fi

# Check C library
echo -e "${YELLOW}Checking C library...${NC}"
if [ ! -f "resources/darwin-aarch64/libchat.dylib" ] && 
   [ ! -f "resources/darwin-x86_64/libchat.dylib" ] &&
   [ ! -f "resources/linux-x86_64/libchat.so" ]; then
    echo -e "${YELLOW}Warning: C library not found. Attempting to copy...${NC}"
    if [ -f "copy_library.py" ]; then
        python3 copy_library.py || echo -e "${RED}Warning: Failed to copy C library${NC}"
    fi
fi
echo ""

# Build pytest command
PYTEST_CMD="pytest"

if [ "$VERBOSE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD -v"
else
    PYTEST_CMD="$PYTEST_CMD -v"
fi

if [ -n "$SPECIFIC_TEST" ]; then
    PYTEST_CMD="$PYTEST_CMD $SPECIFIC_TEST"
else
    PYTEST_CMD="$PYTEST_CMD tests/"
fi

if [ -n "$MARKERS" ]; then
    PYTEST_CMD="$PYTEST_CMD -m $MARKERS"
fi

if [ "$COVERAGE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD --cov=src --cov-report=term-missing --cov-report=html --cov-report=xml"
fi

if [ "$COLLECT_ONLY" = true ]; then
    PYTEST_CMD="$PYTEST_CMD --collect-only"
fi

# Run tests
echo -e "${GREEN}Running tests...${NC}"
echo ""

if [ "$VERBOSE" = true ]; then
    $PYTEST_CMD 2>&1 | tee test_output.log
else
    $PYTEST_CMD 2>&1 | tee test_output.log
fi

TEST_EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo -e "${BLUE}=== Test Results Summary ===${NC}"

# Parse test results
if [ -f "test_output.log" ]; then
    TOTAL_TESTS=$(grep -E "test session starts|collected" test_output.log | grep -oE "[0-9]+ test" | grep -oE "[0-9]+" | head -1 || echo "0")
    PASSED=$(grep -E "passed" test_output.log | tail -1 | grep -oE "[0-9]+ passed" | grep -oE "[0-9]+" || echo "0")
    FAILED=$(grep -E "failed" test_output.log | tail -1 | grep -oE "[0-9]+ failed" | grep -oE "[0-9]+" || echo "0")
    ERROR=$(grep -E "error" test_output.log | tail -1 | grep -oE "[0-9]+ error" | grep -oE "[0-9]+" || echo "0")
    SKIPPED=$(grep -E "skipped" test_output.log | tail -1 | grep -oE "[0-9]+ skipped" | grep -oE "[0-9]+" || echo "0")
    
    if [ -z "$TOTAL_TESTS" ] || [ "$TOTAL_TESTS" = "0" ]; then
        TOTAL_TESTS=$(grep -E "collected [0-9]+ item" test_output.log | grep -oE "[0-9]+" | head -1 || echo "0")
    fi
    
    echo -e "Total:   ${TOTAL_TESTS}"
    echo -e "Passed:  ${GREEN}${PASSED}${NC}"
    
    if [ "${FAILED:-0}" -gt 0 ]; then
        echo -e "Failed:  ${RED}${FAILED}${NC}"
    else
        echo -e "Failed:  ${FAILED:-0}"
    fi
    
    if [ "${ERROR:-0}" -gt 0 ]; then
        echo -e "Errors:  ${RED}${ERROR}${NC}"
    else
        echo -e "Errors:  ${ERROR:-0}"
    fi
    
    if [ "${SKIPPED:-0}" -gt 0 ]; then
        echo -e "Skipped: ${YELLOW}${SKIPPED}${NC}"
    else
        echo -e "Skipped: ${SKIPPED:-0}"
    fi
fi

# Show failed tests
if [ "${FAILED:-0}" -gt 0 ] || [ "${ERROR:-0}" -gt 0 ]; then
    echo ""
    echo -e "${RED}=== Failed Tests ===${NC}"
    grep -E "FAILED|ERROR" test_output.log | head -20 || true
fi

# Coverage report location
if [ "$COVERAGE" = true ]; then
    echo ""
    echo -e "${GREEN}Coverage report: htmlcov/index.html${NC}"
fi

# Test report location
if [ -d ".pytest_cache" ] || [ -d "htmlcov" ]; then
    echo ""
    echo -e "${BLUE}Test artifacts created${NC}"
fi

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ All tests passed!${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Some tests failed${NC}"
    exit $TEST_EXIT_CODE
fi
