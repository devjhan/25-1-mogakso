# 빌드 자동화 가이드

C 라이브러리 빌드 결과물을 Java와 Python 프로젝트로 자동 복사하는 방법을 설명합니다.

## 자동화 방법

### 1. CMake Post-Build 자동 복사 (권장)

C 라이브러리를 빌드하면 자동으로 Java와 Python 프로젝트로 복사됩니다.

```bash
cd C_chat_lib
mkdir -p build
cd build
cmake .. -DBUILD_TESTS=ON
make  # 이 시점에 자동으로 복사됨
```

**장점**:
- C 라이브러리 빌드 시 자동으로 복사
- 플랫폼 감지 자동 처리
- CMake 빌드 시스템과 통합

**대상 경로**:
- Java: `java_chat_server/src/main/resources/{platform}/libchat.{ext}`
- Python: `py_chat_client/resources/{platform}/libchat.{ext}`

### 2. Gradle Task로 자동 빌드 및 복사

Java 프로젝트 빌드 시 C 라이브러리를 먼저 빌드하고 복사합니다.

```bash
cd java_chat_server
./gradlew build  # C 라이브러리 자동 빌드 및 복사 후 Java 빌드
```

**장점**:
- Java 프로젝트 빌드 시 자동으로 C 라이브러리도 빌드
- 의존성 관리 자동화

**사용 가능한 Gradle Tasks**:
```bash
./gradlew buildCLibrary     # C 라이브러리만 빌드
./gradlew copyCLibrary      # C 라이브러리만 복사 (이미 빌드된 경우)
./gradlew build             # 전체 빌드 (C 라이브러리 포함)
```

### 3. Python 스크립트 사용

Python 프로젝트에서 직접 실행할 수 있는 스크립트입니다.

```bash
cd py_chat_client
python3 copy_library.py
# 또는 빌드 디렉토리 지정
python3 copy_library.py --build-dir ../C_chat_lib/build
```

**장점**:
- Python 프로젝트 독립적으로 사용 가능
- 빌드 디렉토리 지정 가능

### 4. 통합 빌드 스크립트

모든 프로젝트를 순서대로 빌드하는 통합 스크립트입니다.

```bash
./build_all.sh
```

**실행 순서**:
1. C 라이브러리 빌드
2. 라이브러리 복사 (Java, Python)
3. Java 서버 빌드
4. Python 클라이언트 확인

**장점**:
- 전체 프로젝트 일괄 빌드
- 의존성 순서 자동 처리

### 5. 수동 복사 스크립트

C 라이브러리만 빌드된 경우 수동으로 복사할 수 있는 스크립트입니다.

```bash
cd C_chat_lib/scripts
./copy_library.sh [BUILD_DIR]
```

**예시**:
```bash
# 기본 빌드 디렉토리 사용 (C_chat_lib/build)
./copy_library.sh

# 사용자 정의 빌드 디렉토리
./copy_library.sh ../cmake-build-debug
```

## 플랫폼 지원

자동화 스크립트는 다음 플랫폼을 자동 감지합니다:

- **macOS ARM64 (Apple Silicon)**: `darwin-aarch64/libchat.dylib`
- **macOS x86_64 (Intel)**: `darwin-x86_64/libchat.dylib`
- **Linux x86_64**: `linux-x86_64/libchat.so`
- **Linux ARM64**: `linux-aarch64/libchat.so`

## 사용 시나리오

### 시나리오 1: C 라이브러리만 수정한 경우

```bash
cd C_chat_lib/build
make
# 자동으로 복사됨 (CMake post-build)
```

### 시나리오 2: Java 프로젝트 빌드만 필요한 경우

```bash
cd java_chat_server
./gradlew build
# C 라이브러리 자동 빌드 및 복사 후 Java 빌드
```

### 시나리오 3: Python 프로젝트만 사용하는 경우

```bash
cd py_chat_client
python3 copy_library.py
```

### 시나리오 4: 전체 프로젝트 처음부터 빌드

```bash
./build_all.sh
```

## 문제 해결

### 라이브러리를 찾을 수 없는 경우

**에러 메시지**:
```
Error: Library not found at C_chat_lib/build/libchat.dylib
```

**해결 방법**:
1. C 라이브러리를 먼저 빌드:
   ```bash
   cd C_chat_lib
   mkdir -p build
   cd build
   cmake ..
   make
   ```

2. 빌드 디렉토리 확인:
   ```bash
   ls -la C_chat_lib/build/libchat.*
   ```

### 플랫폼 감지 오류

**에러 메시지**:
```
Error: Unsupported OS/Architecture
```

**해결 방법**:
1. 수동으로 플랫폼 디렉토리 생성:
   ```bash
   mkdir -p java_chat_server/src/main/resources/darwin-aarch64
   mkdir -p py_chat_client/resources/darwin-aarch64
   ```

2. 수동으로 복사:
   ```bash
   cp C_chat_lib/build/libchat.dylib java_chat_server/src/main/resources/darwin-aarch64/
   cp C_chat_lib/build/libchat.dylib py_chat_client/resources/darwin-aarch64/
   ```

### Gradle Task 실패

**에러 메시지**:
```
Execution failed for task ':buildCLibrary'
```

**해결 방법**:
1. CMake가 설치되어 있는지 확인:
   ```bash
   cmake --version
   ```

2. C 라이브러리 디렉토리 확인:
   ```bash
   ls -la C_chat_lib/CMakeLists.txt
   ```

3. 수동으로 C 라이브러리 빌드 후 Gradle 재실행:
   ```bash
   cd C_chat_lib/build && cmake .. && make
   cd ../../java_chat_server
   ./gradlew build
   ```

## 권장 워크플로우

### 개발 중

1. **C 라이브러리 수정**:
   ```bash
   cd C_chat_lib/build
   make  # 자동 복사됨
   ```

2. **Java 서버 테스트**:
   ```bash
   cd java_chat_server
   ./gradlew test  # C 라이브러리 자동 빌드/복사 후 테스트
   ```

3. **Python 클라이언트 테스트**:
   ```bash
   cd py_chat_client
   python3 copy_library.py  # 필요시에만 실행
   python3 -m pytest  # 또는 테스트 실행
   ```

### CI/CD 파이프라인

```yaml
# 예시: GitHub Actions
- name: Build C Library
  run: |
    cd C_chat_lib
    mkdir -p build && cd build
    cmake .. -DBUILD_TESTS=ON
    make -j4

- name: Copy Library
  run: |
    bash C_chat_lib/scripts/copy_library.sh C_chat_lib/build

- name: Build Java Server
  run: |
    cd java_chat_server
    ./gradlew build

- name: Verify Python Client
  run: |
    cd py_chat_client
    python3 copy_library.py --build-dir ../C_chat_lib/build
```

## 파일 구조

```
chat_project/
├── C_chat_lib/
│   ├── CMakeLists.txt              # Post-build 복사 설정 포함
│   ├── scripts/
│   │   └── copy_library.sh        # 수동 복사 스크립트
│   └── build/
│       └── libchat.dylib          # 빌드 결과물
├── java_chat_server/
│   ├── build.gradle               # C 라이브러리 빌드/복사 task 포함
│   └── src/main/resources/
│       └── darwin-aarch64/
│           └── libchat.dylib      # 복사된 라이브러리
├── py_chat_client/
│   ├── copy_library.py            # Python 복사 스크립트
│   └── resources/
│       └── darwin-aarch64/
│           └── libchat.dylib      # 복사된 라이브러리
└── build_all.sh                   # 통합 빌드 스크립트
```

## 추가 개선 사항

### 향후 개선 가능 영역

1. **크로스 컴파일 지원**: 다른 플랫폼용 라이브러리 빌드 및 복사
2. **버전 관리**: 라이브러리 버전 체크 및 업데이트 알림
3. **의존성 검증**: 라이브러리 파일 무결성 검증 (checksum)
4. **병렬 빌드**: 여러 플랫폼 동시 빌드
5. **캐싱**: 변경되지 않은 라이브러리는 복사 건너뛰기
