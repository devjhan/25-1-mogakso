# 통합 테스트 가이드

이 가이드는 1개의 서버와 3개의 클라이언트를 실행하여 실제 채팅 시스템을 테스트하는 방법을 설명합니다.

## 사전 요구사항

1. **C 라이브러리 빌드**
   ```bash
   cd C_chat_lib
   mkdir -p build && cd build
   cmake .. -DBUILD_TESTS=ON
   make -j$(sysctl -n hw.ncpu)
   ```

2. **Java 서버 의존성 확인**
   - Java 17 이상 설치
   - Gradle Wrapper가 포함되어 있음 (`java_chat_server/gradlew`)

3. **Python 클라이언트 의존성 확인**
   - Python 3.8 이상 설치
   - 필요한 패키지 설치: `pip install -r py_chat_client/requirements.txt`
   - C 라이브러리가 올바른 위치에 복사되었는지 확인

## 통합 테스트 스크립트 사용법

### 기본 사용법 (전체 시작)

프로젝트 루트 디렉토리에서 실행:

```bash
./run_integration_test.sh
```

이 명령은 다음을 수행합니다:
1. 기존 프로세스 정리
2. **Java 서버를 별도 터미널 창에서 시작** (포트 9000)
3. **3개의 Python 클라이언트를 별도 터미널 창에서 시작**
   - 클라이언트 1: `client1` (자동 로그인)
   - 클라이언트 2: `client2` (자동 로그인)
   - 클라이언트 3: `client3` (자동 로그인)

**총 4개의 터미널 창이 열립니다:**
- 터미널 1: 채팅 서버 (포트 9000)
- 터미널 2: 클라이언트 1
- 터미널 3: 클라이언트 2
- 터미널 4: 클라이언트 3

### 옵션

#### 서버만 시작
```bash
./run_integration_test.sh --server-only
```

서버만 시작하고, 클라이언트는 수동으로 실행합니다.

#### 클라이언트만 시작 (서버가 이미 실행 중일 때)
```bash
./run_integration_test.sh --clients-only
```

**주의**: 서버가 실행 중이어야 합니다.

#### 모든 프로세스 종료
```bash
./run_integration_test.sh --cleanup-only
```

모든 서버 및 클라이언트 프로세스를 종료합니다.

### 클라이언트 닉네임 변경

환경 변수를 통해 클라이언트 닉네임을 변경할 수 있습니다:

```bash
export CLIENT1_NICKNAME="user1"
export CLIENT2_NICKNAME="user2"
export CLIENT3_NICKNAME="user3"
./run_integration_test.sh
```

또는 한 줄로:

```bash
CLIENT1_NICKNAME="alice" CLIENT2_NICKNAME="bob" CLIENT3_NICKNAME="charlie" ./run_integration_test.sh
```

## 로그 파일

통합 테스트 실행 시 다음 디렉토리에 로그 파일이 생성됩니다:

```
integration_test_logs/
├── server.log          # 서버 로그
├── client1.log         # 클라이언트 1 로그 (Linux)
├── client2.log         # 클라이언트 2 로그 (Linux)
├── client3.log         # 클라이언트 3 로그 (Linux)
├── server.pid          # 서버 프로세스 ID
└── clients.pid         # 클라이언트 프로세스 ID 목록
```

### 로그 확인

**macOS (터미널 창에서 실행):**
- 서버 로그: 서버 터미널 창에서 직접 확인
- 클라이언트 로그: 각 클라이언트 터미널 창에서 직접 확인

**Linux (백그라운드 실행):**
- 서버 로그 실시간 확인:
  ```bash
  tail -f integration_test_logs/server.log
  ```

- 클라이언트 로그 확인:
  ```bash
  tail -f integration_test_logs/client1.log
  tail -f integration_test_logs/client2.log
  tail -f integration_test_logs/client3.log
  ```

## 테스트 시나리오

### 기본 채팅 테스트

1. 서버 및 3개의 클라이언트 시작:
   ```bash
   ./run_integration_test.sh
   ```

2. 각 클라이언트 터미널 창에서:
   - 클라이언트는 자동으로 서버에 연결되고 로그인됩니다
   - 메시지를 입력하여 다른 클라이언트와 채팅할 수 있습니다
   - 예: 클라이언트 1에서 "Hello, everyone!" 입력

3. 다른 클라이언트에서 메시지 수신 확인

### 명령어 테스트

각 클라이언트에서 사용 가능한 명령어:

- `/quit` 또는 `/exit`: 클라이언트 종료
- `/nickname <새닉네임>`: 닉네임 변경
- `/users`: 접속 중인 사용자 목록 확인
- `/file <파일경로>`: 파일 전송

### 파일 전송 테스트

1. 클라이언트 1에서 파일 전송:
   ```
   /file /path/to/test.txt
   ```

2. 다른 클라이언트에서 파일 수신 확인

3. 서버 로그에서 파일 업로드 및 전송 과정 확인

## 문제 해결

### 서버가 시작되지 않음

1. **C 라이브러리 확인:**
   ```bash
   ls -la java_chat_server/src/main/resources/darwin-aarch64/libchat.dylib
   # 또는 해당 플랫폼의 경로
   ```

2. **포트 9000이 이미 사용 중:**
   ```bash
   lsof -i :9000
   # 프로세스 종료 또는 application.properties에서 포트 변경
   ```

3. **서버 로그 확인:**
   ```bash
   cat integration_test_logs/server.log
   ```

### 클라이언트가 연결되지 않음

1. **서버가 실행 중인지 확인:**
   ```bash
   lsof -i :9000
   ```

2. **C 라이브러리 확인 (클라이언트):**
   ```bash
   ls -la py_chat_client/resources/darwin-aarch64/libchat.dylib
   # 또는 해당 플랫폼의 경로
   ```

3. **클라이언트 로그 확인:**
   ```bash
   cat integration_test_logs/client1.log
   ```

### 클라이언트가 자동 로그인되지 않음

환경 변수가 올바르게 설정되었는지 확인:

```bash
echo $CHAT_CLIENT_NICKNAME
```

명령어 실행 전에 환경 변수를 설정했는지 확인하세요.

### 프로세스가 정상적으로 종료되지 않음

강제 종료:

```bash
# 서버 PID 확인 및 종료
cat integration_test_logs/server.pid | xargs kill -9

# 클라이언트 PID 확인 및 종료
cat integration_test_logs/clients.pid | xargs kill -9
```

또는:

```bash
./run_integration_test.sh --cleanup-only
```

## 수동 테스트 (스크립트 사용 안 함)

### 서버 수동 실행

```bash
cd java_chat_server
./gradlew bootRun
```

### 클라이언트 수동 실행

**터미널 1:**
```bash
cd py_chat_client
python3 -m src.main
```

**터미널 2:**
```bash
cd py_chat_client
CHAT_CLIENT_NICKNAME="client1" python3 -m src.main
```

**터미널 3:**
```bash
cd py_chat_client
CHAT_CLIENT_NICKNAME="client2" python3 -m src.main
```

**터미널 4:**
```bash
cd py_chat_client
CHAT_CLIENT_NICKNAME="client3" python3 -m src.main
```

## 주의사항

1. **macOS 권한**: Terminal.app에 접근 권한이 필요할 수 있습니다 (시스템 환경설정 > 보안 및 개인정보 보호)

2. **포트 충돌**: 포트 9000이 이미 사용 중이면 서버가 시작되지 않습니다

3. **C 라이브러리 버전**: Java 서버와 Python 클라이언트가 동일한 C 라이브러리 버전을 사용해야 합니다

4. **로그 파일 크기**: 장시간 실행 시 로그 파일이 커질 수 있으므로 주기적으로 정리하세요

## 추가 정보

- Java 서버 설정: `java_chat_server/src/main/resources/application.properties`
- Python 클라이언트 설정: `py_chat_client/src/configs/configuration.py` 또는 `.env` 파일
- C 라이브러리 빌드 가이드: `C_chat_lib/BUILD_GUIDE.md`
