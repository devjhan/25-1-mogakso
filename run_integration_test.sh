#!/bin/bash

# 통합 테스트 스크립트: 1개 서버 + 3개 클라이언트
# 사용법: ./run_integration_test.sh [--cleanup-only]
#        ./run_integration_test.sh [--server-only | --clients-only]

set -e

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 스크립트 위치
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# 디렉토리 경로
JAVA_SERVER_DIR="$SCRIPT_DIR/java_chat_server"
PY_CLIENT_DIR="$SCRIPT_DIR/py_chat_client"
C_LIB_DIR="$SCRIPT_DIR/C_chat_lib"

# 로그 및 PID 디렉토리
TEST_LOG_DIR="$SCRIPT_DIR/integration_test_logs"
SERVER_PID_FILE="$TEST_LOG_DIR/server.pid"
CLIENT_PID_FILE="$TEST_LOG_DIR/clients.pid"
SERVER_LOG="$TEST_LOG_DIR/server.log"
CLIENT1_LOG="$TEST_LOG_DIR/client1.log"
CLIENT2_LOG="$TEST_LOG_DIR/client2.log"
CLIENT3_LOG="$TEST_LOG_DIR/client3.log"

# 서버 설정
SERVER_PORT=9000
SERVER_IP="127.0.0.1"

# 클라이언트 닉네임 (자동 로그인을 위해 환경 변수로 전달)
CLIENT1_NICKNAME="${CLIENT1_NICKNAME:-client1}"
CLIENT2_NICKNAME="${CLIENT2_NICKNAME:-client2}"
CLIENT3_NICKNAME="${CLIENT3_NICKNAME:-client3}"

# 함수: 로그 출력
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 함수: 정리 작업
cleanup() {
    log_info "정리 작업을 시작합니다..."
    
    # 클라이언트 종료
    if [ -f "$CLIENT_PID_FILE" ]; then
        while read pid; do
            if ps -p "$pid" > /dev/null 2>&1; then
                log_info "클라이언트 프로세스 종료: PID $pid"
                kill "$pid" 2>/dev/null || true
            fi
        done < "$CLIENT_PID_FILE"
        rm -f "$CLIENT_PID_FILE"
    fi
    
    # 서버 종료
    if [ -f "$SERVER_PID_FILE" ]; then
        SERVER_PID=$(cat "$SERVER_PID_FILE")
        if ps -p "$SERVER_PID" > /dev/null 2>&1; then
            log_info "서버 프로세스 종료: PID $SERVER_PID"
            kill "$SERVER_PID" 2>/dev/null || true
            # Spring Boot 애플리케이션이므로 조금 기다림
            sleep 2
            # 여전히 살아있으면 강제 종료
            if ps -p "$SERVER_PID" > /dev/null 2>&1; then
                kill -9 "$SERVER_PID" 2>/dev/null || true
            fi
        fi
        rm -f "$SERVER_PID_FILE"
    fi
    
    # macOS에서는 Gradle 프로세스도 찾아서 종료
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # Gradle bootRun 프로세스 찾기 및 종료
        GRADLE_PIDS=$(ps aux | grep "[g]radle.*bootRun" | awk '{print $2}')
        if [ -n "$GRADLE_PIDS" ]; then
            log_info "Gradle 서버 프로세스 종료 중..."
            echo "$GRADLE_PIDS" | while read pid; do
                kill "$pid" 2>/dev/null || true
            done
            sleep 2
            # 강제 종료
            echo "$GRADLE_PIDS" | while read pid; do
                if ps -p "$pid" > /dev/null 2>&1; then
                    kill -9 "$pid" 2>/dev/null || true
                fi
            done
        fi
        
        # Java Spring Boot 애플리케이션 프로세스 찾기 및 종료
        JAVA_PIDS=$(ps aux | grep "[j]ava.*JavaChatServerApplication" | awk '{print $2}')
        if [ -n "$JAVA_PIDS" ]; then
            log_info "Java 서버 프로세스 종료 중..."
            echo "$JAVA_PIDS" | while read pid; do
                kill "$pid" 2>/dev/null || true
            done
            sleep 2
            # 강제 종료
            echo "$JAVA_PIDS" | while read pid; do
                if ps -p "$pid" > /dev/null 2>&1; then
                    kill -9 "$pid" 2>/dev/null || true
                fi
            done
        fi
    fi
    
    log_success "정리 작업 완료"
}

# 함수: 서버 시작
start_server() {
    log_info "서버를 시작합니다..."
    
    # 로그 디렉토리 생성
    mkdir -p "$TEST_LOG_DIR"
    
    # C 라이브러리 확인
    log_info "C 라이브러리 확인 중..."
    cd "$JAVA_SERVER_DIR"
    
    # C 라이브러리 빌드 및 복사 (필요시)
    if ! ./gradlew copyCLibrary --no-daemon > /dev/null 2>&1; then
        log_warning "C 라이브러리 자동 복사 실패. 수동으로 확인하세요."
    fi
    
    # macOS에서 새 터미널 창에서 서버 시작
    if [[ "$OSTYPE" == "darwin"* ]]; then
        log_info "Java 서버를 새 터미널 창에서 시작합니다 (포트: $SERVER_PORT)..."
        # 새 터미널 창에서 서버 실행
        osascript -e "tell application \"Terminal\" to do script \"cd '$JAVA_SERVER_DIR' && echo '=== 채팅 서버 (포트: $SERVER_PORT) ===' && ./gradlew bootRun --no-daemon\""
        
        log_success "서버가 새 터미널 창에서 시작되었습니다."
        log_info "서버 터미널 창에서 로그를 확인할 수 있습니다."
    else
        # Linux에서는 백그라운드 실행 (로그 파일에 출력)
        log_info "Java 서버를 시작합니다 (포트: $SERVER_PORT)..."
        cd "$JAVA_SERVER_DIR"
        nohup ./gradlew bootRun --no-daemon > "$SERVER_LOG" 2>&1 &
        SERVER_PID=$!
        echo "$SERVER_PID" > "$SERVER_PID_FILE"
        
        log_success "서버가 시작되었습니다 (PID: $SERVER_PID)"
        log_info "서버 로그: $SERVER_LOG"
    fi
    
    # 서버가 완전히 시작될 때까지 대기
    log_info "서버가 완전히 시작될 때까지 대기 중... (최대 30초)"
    for i in {1..30}; do
        # 포트 확인 (서버가 시작되었는지 확인)
        if lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
            log_success "서버가 성공적으로 시작되었습니다! (포트: $SERVER_PORT)"
            return 0
        fi
        # macOS의 경우 로그 파일 확인도 시도 (터미널 창이라 로그 파일이 없을 수 있음)
        if [[ "$OSTYPE" != "darwin"* ]] && [ -f "$SERVER_LOG" ]; then
            if grep -q "네이티브 채팅 서버가 포트 $SERVER_PORT에서 성공적으로 시작되었습니다" "$SERVER_LOG" 2>/dev/null; then
                log_success "서버가 성공적으로 시작되었습니다!"
                return 0
            fi
        fi
        sleep 1
        echo -n "."
    done
    echo ""
    
    # 포트 확인 (로그가 없어도 포트가 열려있으면 성공으로 간주)
    if lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        log_success "서버 포트가 열려있습니다 (포트: $SERVER_PORT)"
        return 0
    else
        log_error "서버 시작 실패. 서버 터미널 창 또는 로그를 확인하세요: $SERVER_LOG"
        return 1
    fi
}

# 함수: 클라이언트 시작 (인터랙티브 모드)
start_clients_interactive() {
    log_info "클라이언트를 시작합니다 (인터랙티브 모드)..."
    
    # 클라이언트 PID 리스트 초기화
    > "$CLIENT_PID_FILE"
    
    # Python 의존성 확인
    cd "$PY_CLIENT_DIR"
    if [ ! -d "venv" ] && [ ! -f ".python-version" ]; then
        log_warning "Python 가상환경이 설정되지 않았습니다. 시스템 Python을 사용합니다."
    fi
    
    # C 라이브러리 확인
    log_info "Python 클라이언트 C 라이브러리 확인 중..."
    OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    # Architecture 변환
    case "$ARCH" in
        arm64) ARCH="aarch64" ;;
        x86_64) ARCH="x86_64" ;;
    esac
    
    # Platform 결정
    if [[ "$OS_NAME" == "darwin" ]]; then
        PLATFORM="darwin-$ARCH"
        LIB_EXT="dylib"
    elif [[ "$OS_NAME" == "linux" ]]; then
        PLATFORM="linux-$ARCH"
        LIB_EXT="so"
    else
        log_error "지원하지 않는 운영체제: $OS_NAME"
        return 1
    fi
    
    CLIENT_LIB="$PY_CLIENT_DIR/resources/$PLATFORM/libchat.$LIB_EXT"
    
    if [ ! -f "$CLIENT_LIB" ]; then
        log_error "C 라이브러리를 찾을 수 없습니다: $CLIENT_LIB"
        log_info "C 라이브러리를 먼저 빌드하고 복사하세요."
        log_info "예: cd C_chat_lib && mkdir -p build && cd build && cmake .. && make"
        return 1
    fi
    
    log_success "C 라이브러리 확인 완료: $CLIENT_LIB"
    
    log_info "클라이언트를 별도 터미널 창에서 시작합니다..."
    log_info "각 클라이언트 창에서 닉네임을 입력하세요."
    
    # macOS에서 새 터미널 창 열기
    if [[ "$OSTYPE" == "darwin"* ]]; then
        log_info "3개의 클라이언트를 새 터미널 창에서 시작합니다..."
        
        # 클라이언트 1 (자동 로그인)
        osascript -e "tell application \"Terminal\" to do script \"cd '$PY_CLIENT_DIR' && export PYTHONPATH='$PY_CLIENT_DIR' && export CHAT_CLIENT_NICKNAME='$CLIENT1_NICKNAME' && echo '=== 클라이언트 1 (닉네임: $CLIENT1_NICKNAME) ===' && python3 -m src.main\""
        
        sleep 2
        
        # 클라이언트 2 (자동 로그인)
        osascript -e "tell application \"Terminal\" to do script \"cd '$PY_CLIENT_DIR' && export PYTHONPATH='$PY_CLIENT_DIR' && export CHAT_CLIENT_NICKNAME='$CLIENT2_NICKNAME' && echo '=== 클라이언트 2 (닉네임: $CLIENT2_NICKNAME) ===' && python3 -m src.main\""
        
        sleep 2
        
        # 클라이언트 3 (자동 로그인)
        osascript -e "tell application \"Terminal\" to do script \"cd '$PY_CLIENT_DIR' && export PYTHONPATH='$PY_CLIENT_DIR' && export CHAT_CLIENT_NICKNAME='$CLIENT3_NICKNAME' && echo '=== 클라이언트 3 (닉네임: $CLIENT3_NICKNAME) ===' && python3 -m src.main\""
        
        log_success "3개의 클라이언트 터미널 창이 열렸습니다."
        log_info "각 클라이언트는 자동으로 로그인됩니다: $CLIENT1_NICKNAME, $CLIENT2_NICKNAME, $CLIENT3_NICKNAME"
        log_info "각 터미널 창에서 메시지를 주고받을 수 있습니다."
    else
        # Linux나 다른 OS에서는 백그라운드로 실행 (로그 파일에 출력)
        log_warning "macOS가 아닙니다. 클라이언트를 백그라운드로 실행합니다."
        log_info "클라이언트 출력은 로그 파일에 저장됩니다:"
        log_info "  - 클라이언트 1: $CLIENT1_LOG"
        log_info "  - 클라이언트 2: $CLIENT2_LOG"
        log_info "  - 클라이언트 3: $CLIENT3_LOG"
        
        # 클라이언트 1 (자동 로그인)
        cd "$PY_CLIENT_DIR"
        export PYTHONPATH="$PY_CLIENT_DIR:$PYTHONPATH"
        CHAT_CLIENT_NICKNAME="$CLIENT1_NICKNAME" python3 -m src.main > "$CLIENT1_LOG" 2>&1 &
        CLIENT1_PID=$!
        echo "$CLIENT1_PID" >> "$CLIENT_PID_FILE"
        
        sleep 3
        
        # 클라이언트 2 (자동 로그인)
        export PYTHONPATH="$PY_CLIENT_DIR:$PYTHONPATH"
        CHAT_CLIENT_NICKNAME="$CLIENT2_NICKNAME" python3 -m src.main > "$CLIENT2_LOG" 2>&1 &
        CLIENT2_PID=$!
        echo "$CLIENT2_PID" >> "$CLIENT_PID_FILE"
        
        sleep 3
        
        # 클라이언트 3 (자동 로그인)
        export PYTHONPATH="$PY_CLIENT_DIR:$PYTHONPATH"
        CHAT_CLIENT_NICKNAME="$CLIENT3_NICKNAME" python3 -m src.main > "$CLIENT3_LOG" 2>&1 &
        CLIENT3_PID=$!
        echo "$CLIENT3_PID" >> "$CLIENT_PID_FILE"
        
        log_success "3개의 클라이언트가 시작되었습니다 (자동 로그인: $CLIENT1_NICKNAME, $CLIENT2_NICKNAME, $CLIENT3_NICKNAME)"
        log_info "클라이언트 PID: $CLIENT1_PID, $CLIENT2_PID, $CLIENT3_PID"
        log_info "로그 파일에서 출력을 확인하세요."
    fi
}

# 함수: 상태 확인
check_status() {
    log_info "현재 상태 확인:"
    
    if [ -f "$SERVER_PID_FILE" ]; then
        SERVER_PID=$(cat "$SERVER_PID_FILE")
        if ps -p "$SERVER_PID" > /dev/null 2>&1; then
            log_success "서버 실행 중 (PID: $SERVER_PID)"
            if lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
                log_success "서버 포트 $SERVER_PORT 열림"
            else
                log_warning "서버 포트 $SERVER_PORT가 열려있지 않습니다"
            fi
        else
            log_error "서버가 실행되지 않습니다 (PID 파일만 존재)"
        fi
    else
        log_info "서버가 시작되지 않았습니다"
    fi
    
    if [ -f "$CLIENT_PID_FILE" ]; then
        CLIENT_COUNT=0
        while read pid; do
            if ps -p "$pid" > /dev/null 2>&1; then
                CLIENT_COUNT=$((CLIENT_COUNT + 1))
            fi
        done < "$CLIENT_PID_FILE"
        log_info "클라이언트 실행 중: $CLIENT_COUNT 개"
    else
        log_info "클라이언트가 시작되지 않았습니다"
    fi
}

# 함수: 도움말 출력
show_help() {
    echo -e "${CYAN}=== 통합 테스트 스크립트 도움말 ===${NC}"
    echo ""
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  (없음)              서버와 3개의 클라이언트 모두 시작"
    echo "  --server-only       서버만 시작"
    echo "  --clients-only      클라이언트만 시작 (서버가 이미 실행 중이어야 함)"
    echo "  --cleanup-only      모든 프로세스 종료 및 정리"
    echo "  --help, -h          이 도움말 표시"
    echo ""
    echo "환경 변수:"
    echo "  CLIENT1_NICKNAME    클라이언트 1 닉네임 (기본값: client1)"
    echo "  CLIENT2_NICKNAME    클라이언트 2 닉네임 (기본값: client2)"
    echo "  CLIENT3_NICKNAME    클라이언트 3 닉네임 (기본값: client3)"
    echo ""
    echo "예제:"
    echo "  # 기본 실행"
    echo "  $0"
    echo ""
    echo "  # 사용자 정의 닉네임으로 실행"
    echo "  CLIENT1_NICKNAME=alice CLIENT2_NICKNAME=bob CLIENT3_NICKNAME=charlie $0"
    echo ""
    echo "  # 서버만 시작"
    echo "  $0 --server-only"
    echo ""
    echo "  # 모든 프로세스 종료"
    echo "  $0 --cleanup-only"
    echo ""
    echo "로그 파일:"
    echo "  서버: $TEST_LOG_DIR/server.log"
    echo "  클라이언트 (Linux): $TEST_LOG_DIR/client*.log"
    echo ""
    echo "자세한 내용은 INTEGRATION_TEST_GUIDE.md를 참조하세요."
    echo ""
}

# 메인 로직
main() {
    # 도움말 표시
    if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
        show_help
        exit 0
    fi
    
    # 정리만 수행
    if [ "$1" == "--cleanup-only" ]; then
        cleanup
        exit 0
    fi
    
    # 서버만 시작
    if [ "$1" == "--server-only" ]; then
        start_server
        log_info "서버만 시작되었습니다. 클라이언트는 수동으로 실행하세요."
        log_info "종료하려면: $0 --cleanup-only"
        exit 0
    fi
    
    # 클라이언트만 시작 (서버가 이미 실행 중이어야 함)
    if [ "$1" == "--clients-only" ]; then
        if ! lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
            log_error "서버가 실행되지 않았습니다. 먼저 서버를 시작하세요."
            exit 1
        fi
        start_clients_interactive
        log_info "클라이언트가 시작되었습니다."
        exit 0
    fi
    
    # 전체 시작
    echo -e "${CYAN}=== 통합 테스트 시작 ===${NC}"
    echo ""
    
    # 기존 프로세스 정리
    cleanup
    
    # 서버 시작
    if ! start_server; then
        log_error "서버 시작 실패"
        exit 1
    fi
    
    echo ""
    
    # 잠시 대기 (서버 안정화)
    sleep 3
    
    # 클라이언트 시작
    start_clients_interactive
    
    echo ""
    log_success "통합 테스트 환경이 준비되었습니다!"
    echo ""
    echo -e "${CYAN}총 4개의 터미널 창이 열렸습니다:${NC}"
    echo "  1. 서버 터미널 - 채팅 서버 (포트: $SERVER_PORT)"
    echo "  2. 클라이언트 1 터미널 - 닉네임: $CLIENT1_NICKNAME"
    echo "  3. 클라이언트 2 터미널 - 닉네임: $CLIENT2_NICKNAME"
    echo "  4. 클라이언트 3 터미널 - 닉네임: $CLIENT3_NICKNAME"
    echo ""
    echo -e "${CYAN}사용 방법:${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "  - 서버 로그: 서버 터미널 창에서 확인"
        echo "  - 클라이언트 로그: 각 클라이언트 터미널 창에서 확인"
    else
        echo "  - 서버 로그 확인: tail -f $SERVER_LOG"
        echo "  - 클라이언트 로그 확인: tail -f $CLIENT1_LOG (또는 client2.log, client3.log)"
    fi
    echo "  - 종료: $0 --cleanup-only"
    echo ""
    echo -e "${YELLOW}종료하려면 각 터미널 창을 닫거나 다음 명령어를 실행하세요:${NC}"
    echo "  $0 --cleanup-only"
    echo ""
    
    # 종료 신호 대기
    trap cleanup EXIT INT TERM
    
    # 대기 (사용자가 수동으로 종료할 때까지)
    log_info "통합 테스트가 실행 중입니다."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        log_info "각 터미널 창에서 서버와 클라이언트를 직접 제어할 수 있습니다."
        log_info "종료하려면: $0 --cleanup-only"
        log_info ""
        log_info "현재 스크립트는 대기 상태입니다. 종료하려면 Ctrl+C를 누르세요."
    else
        log_info "종료하려면 Ctrl+C를 누르세요..."
    fi
    
    # macOS에서는 터미널 창에서 실행되므로, 포트만 확인
    while true; do
        sleep 10
        # 서버 포트가 열려있는지 확인 (macOS)
        if ! lsof -Pi :$SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
            log_warning "서버 포트 $SERVER_PORT가 닫혔습니다. 서버가 종료되었을 수 있습니다."
            log_info "프로세스 정리 후 종료합니다..."
            cleanup
            exit 0
        fi
    done
}

# 스크립트 실행
main "$@"
