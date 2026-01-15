#!/bin/bash

# C 라이브러리 빌드 결과물을 Java/Python 프로젝트로 복사하는 스크립트
# 사용법: ./copy_library.sh [BUILD_DIR]

set -e

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 스크립트 위치 기반으로 프로젝트 루트 찾기
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
C_LIB_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
PROJECT_ROOT="$( cd "$C_LIB_DIR/.." && pwd )"

# 빌드 디렉토리 설정
BUILD_DIR="${1:-$C_LIB_DIR/build}"
LIB_NAME="libchat.dylib"

# 플랫폼 감지
OS=$(uname -s)
ARCH=$(uname -m)

# 플랫폼별 디렉토리 결정
if [ "$OS" = "Darwin" ]; then
    if [ "$ARCH" = "arm64" ] || [ "$ARCH" = "aarch64" ]; then
        PLATFORM_DIR="darwin-aarch64"
        LIB_EXT=".dylib"
    elif [ "$ARCH" = "x86_64" ]; then
        PLATFORM_DIR="darwin-x86_64"
        LIB_EXT=".dylib"
    else
        echo -e "${RED}Error: Unsupported architecture: $ARCH${NC}"
        exit 1
    fi
elif [ "$OS" = "Linux" ]; then
    if [ "$ARCH" = "x86_64" ]; then
        PLATFORM_DIR="linux-x86_64"
        LIB_EXT=".so"
    elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
        PLATFORM_DIR="linux-aarch64"
        LIB_EXT=".so"
    else
        echo -e "${RED}Error: Unsupported architecture: $ARCH${NC}"
        exit 1
    fi
else
    echo -e "${RED}Error: Unsupported OS: $OS${NC}"
    exit 1
fi

# 빌드된 라이브러리 경로
BUILT_LIB="$BUILD_DIR/libchat$LIB_EXT"

# 대상 경로
JAVA_TARGET="$PROJECT_ROOT/java_chat_server/src/main/resources/$PLATFORM_DIR/libchat$LIB_EXT"
PYTHON_TARGET="$PROJECT_ROOT/py_chat_client/resources/$PLATFORM_DIR/libchat$LIB_EXT"

# 빌드된 라이브러리 확인
if [ ! -f "$BUILT_LIB" ]; then
    echo -e "${RED}Error: Library not found at $BUILT_LIB${NC}"
    echo -e "${YELLOW}Hint: Build the C library first: cd $C_LIB_DIR && mkdir -p build && cd build && cmake .. && make${NC}"
    exit 1
fi

# 디렉토리 생성
echo -e "${GREEN}Copying library to projects...${NC}"
mkdir -p "$(dirname "$JAVA_TARGET")"
mkdir -p "$(dirname "$PYTHON_TARGET")"

# 파일 복사
cp "$BUILT_LIB" "$JAVA_TARGET"
echo -e "${GREEN}✓ Copied to Java project: $JAVA_TARGET${NC}"

cp "$BUILT_LIB" "$PYTHON_TARGET"
echo -e "${GREEN}✓ Copied to Python project: $PYTHON_TARGET${NC}"

# 파일 정보 출력
echo -e "${GREEN}Library copied successfully!${NC}"
echo -e "  Platform: $PLATFORM_DIR"
echo -e "  Source: $BUILT_LIB"
ls -lh "$JAVA_TARGET"
ls -lh "$PYTHON_TARGET"
