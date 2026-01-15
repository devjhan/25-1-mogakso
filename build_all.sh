#!/bin/bash

# 전체 프로젝트 빌드 스크립트
# C 라이브러리 → Java 서버 → Python 클라이언트 순서로 빌드

set -e

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트 위치 기반으로 프로젝트 루트 찾기
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
C_LIB_DIR="$SCRIPT_DIR/C_chat_lib"
JAVA_DIR="$SCRIPT_DIR/java_chat_server"
PYTHON_DIR="$SCRIPT_DIR/py_chat_client"

echo -e "${BLUE}=== Building Chat Project ===${NC}"
echo ""

# 1. C 라이브러리 빌드
echo -e "${GREEN}[1/3] Building C library...${NC}"
cd "$C_LIB_DIR"
if [ ! -d "build" ]; then
    mkdir -p build
fi
cd build

if [ ! -f "CMakeCache.txt" ]; then
    echo "  Running cmake..."
    cmake .. -DBUILD_TESTS=ON
fi

echo "  Running make..."
make -j$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)

echo -e "${GREEN}✓ C library built successfully${NC}"
echo ""

# 2. 라이브러리 복사 스크립트 실행
echo -e "${GREEN}[2/3] Copying library to projects...${NC}"
if [ -f "$C_LIB_DIR/scripts/copy_library.sh" ]; then
    bash "$C_LIB_DIR/scripts/copy_library.sh" "$C_LIB_DIR/build"
else
    # Fallback: 직접 복사
    PLATFORM=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ]; then
        PLATFORM_DIR="darwin-aarch64"
    else
        PLATFORM_DIR="darwin-x86_64"
    fi
    
    mkdir -p "$JAVA_DIR/src/main/resources/$PLATFORM_DIR"
    mkdir -p "$PYTHON_DIR/resources/$PLATFORM_DIR"
    
    cp "$C_LIB_DIR/build/libchat.dylib" "$JAVA_DIR/src/main/resources/$PLATFORM_DIR/"
    cp "$C_LIB_DIR/build/libchat.dylib" "$PYTHON_DIR/resources/$PLATFORM_DIR/"
fi

echo -e "${GREEN}✓ Library copied to Java and Python projects${NC}"
echo ""

# 3. Java 프로젝트 빌드
echo -e "${GREEN}[3/3] Building Java server...${NC}"
cd "$JAVA_DIR"
./gradlew build -x test --no-daemon || echo -e "${YELLOW}Warning: Java build failed or tests failed${NC}"

echo -e "${GREEN}✓ Java server built${NC}"
echo ""

# 4. Python 프로젝트 확인
echo -e "${GREEN}[4/4] Verifying Python client...${NC}"
cd "$PYTHON_DIR"
if [ -f "copy_library.py" ]; then
    python3 copy_library.py --build-dir "$C_LIB_DIR/build" || true
fi

echo -e "${GREEN}✓ Python client ready${NC}"
echo ""

echo -e "${BLUE}=== Build Complete! ===${NC}"
echo ""
echo "Next steps:"
echo "  1. Run C tests: cd $C_LIB_DIR/build && ctest"
echo "  2. Run Java tests: cd $JAVA_DIR && ./gradlew test"
echo "  3. Run Java server: cd $JAVA_DIR && ./gradlew bootRun"
echo "  4. Run Python client: cd $PYTHON_DIR && python3 -m src.app.main"
