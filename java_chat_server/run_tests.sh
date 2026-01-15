#!/bin/bash

# Java Chat Server 테스트 실행 스크립트
# 사용법: ./run_tests.sh [옵션]

set -e

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트 위치
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${BLUE}=== Java Chat Server Test Runner ===${NC}"
echo ""

# 옵션 파싱
VERBOSE=false
SPECIFIC_TEST=""
CLEAN=false
COVERAGE=false

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
        -c|--clean)
            CLEAN=true
            shift
            ;;
        --coverage)
            COVERAGE=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -v, --verbose        Verbose output"
            echo "  -t, --test CLASS     Run specific test class"
            echo "  -c, --clean          Clean build before testing"
            echo "  --coverage           Generate coverage report"
            echo "  -h, --help           Show this help"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Clean if requested
if [ "$CLEAN" = true ]; then
    echo -e "${YELLOW}Cleaning build...${NC}"
    ./gradlew clean --no-daemon
    echo ""
fi

# C 라이브러리 확인 및 복사
echo -e "${YELLOW}Checking C library...${NC}"
if [ ! -f "src/main/resources/darwin-aarch64/libchat.dylib" ] && 
   [ ! -f "src/main/resources/darwin-x86_64/libchat.dylib" ] &&
   [ ! -f "src/main/resources/linux-x86_64/libchat.so" ]; then
    echo -e "${YELLOW}Warning: C library not found. Attempting to build and copy...${NC}"
    ./gradlew copyCLibrary --no-daemon || echo -e "${RED}Warning: Failed to copy C library${NC}"
fi
echo ""

# 테스트 실행
echo -e "${GREEN}Running tests...${NC}"
echo ""

if [ -n "$SPECIFIC_TEST" ]; then
    echo -e "${BLUE}Running specific test: $SPECIFIC_TEST${NC}"
    if [ "$VERBOSE" = true ]; then
        ./gradlew test --tests "$SPECIFIC_TEST" --no-daemon --info 2>&1 | tee test_output.log
    else
        ./gradlew test --tests "$SPECIFIC_TEST" --no-daemon 2>&1 | tee test_output.log
    fi
else
    if [ "$VERBOSE" = true ]; then
        ./gradlew test --no-daemon --info 2>&1 | tee test_output.log
    else
        ./gradlew test --no-daemon 2>&1 | tee test_output.log
    fi
fi

TEST_EXIT_CODE=${PIPESTATUS[0]}

echo ""
echo -e "${BLUE}=== Test Results Summary ===${NC}"

# 테스트 결과 파싱
TEST_RESULT_FILES=$(find build/test-results/test -name "TEST-*.xml" 2>/dev/null)
if [ -n "$TEST_RESULT_FILES" ]; then
    TOTAL_TESTS=0
    FAILED_TESTS=0
    ERROR_TESTS=0
    
    for file in $TEST_RESULT_FILES; do
        TESTS=$(grep -o 'tests="[0-9]*"' "$file" 2>/dev/null | grep -o '[0-9]*' | head -1)
        FAILURES=$(grep -o 'failures="[0-9]*"' "$file" 2>/dev/null | grep -o '[0-9]*' | head -1)
        ERRORS=$(grep -o 'errors="[0-9]*"' "$file" 2>/dev/null | grep -o '[0-9]*' | head -1)
        
        TOTAL_TESTS=$((TOTAL_TESTS + ${TESTS:-0}))
        FAILED_TESTS=$((FAILED_TESTS + ${FAILURES:-0}))
        ERROR_TESTS=$((ERROR_TESTS + ${ERRORS:-0}))
    done
    
    PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS - ERROR_TESTS))
    
    echo -e "Total:   ${TOTAL_TESTS}"
    echo -e "Passed:  ${GREEN}${PASSED_TESTS}${NC}"
    
    if [ "${FAILED_TESTS:-0}" -gt 0 ]; then
        echo -e "Failed:  ${RED}${FAILED_TESTS}${NC}"
    else
        echo -e "Failed:  ${FAILED_TESTS:-0}"
    fi
    
    if [ "${ERROR_TESTS:-0}" -gt 0 ]; then
        echo -e "Errors:  ${RED}${ERROR_TESTS}${NC}"
    else
        echo -e "Errors:  ${ERROR_TESTS:-0}"
    fi
else
    echo -e "${YELLOW}Warning: Could not find test result files${NC}"
fi

# 실패한 테스트 출력
if [ "$FAILED_TESTS" -gt 0 ] || [ "$ERROR_TESTS" -gt 0 ]; then
    echo ""
    echo -e "${RED}=== Failed Tests ===${NC}"
    grep -h "<failure" build/test-results/test/TEST-*.xml 2>/dev/null | head -20 || true
    grep -h "<error" build/test-results/test/TEST-*.xml 2>/dev/null | head -20 || true
fi

# Coverage 리포트 생성
if [ "$COVERAGE" = true ]; then
    echo ""
    echo -e "${BLUE}Generating coverage report...${NC}"
    ./gradlew jacocoTestReport --no-daemon 2>&1 | tail -10
    echo -e "${GREEN}Coverage report: build/reports/jacoco/test/html/index.html${NC}"
fi

# HTML 리포트 위치 알림
echo ""
echo -e "${BLUE}Test report: build/reports/tests/test/index.html${NC}"

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ All tests passed!${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Some tests failed${NC}"
    exit $TEST_EXIT_CODE
fi
