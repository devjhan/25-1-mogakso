# C 라이브러리 빌드 가이드

## 사전 요구사항

### 1. CMake 설치 (필수)

CMake가 설치되어 있지 않은 경우:

#### macOS에서 설치
```bash
# Homebrew 사용
brew install cmake

# 또는 MacPorts 사용
sudo port install cmake

# 설치 확인
cmake --version  # 버전 3.28 이상 권장
```

#### Linux에서 설치
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install cmake build-essential

# Fedora/CentOS
sudo yum install cmake gcc make
```

### 2. C 컴파일러 (GCC 또는 Clang)

```bash
# 설치 확인
gcc --version
# 또는
clang --version
```

### 3. Threads 라이브러리 (pthread)

일반적으로 시스템에 기본 설치되어 있습니다.

---

## 빌드 과정 상세 설명

### 단계 1: 빌드 디렉토리 생성

```bash
cd C_chat_lib
mkdir -p build
cd build
```

**설명**: 
- `build` 디렉토리는 CMake가 생성하는 빌드 파일들을 저장하는 곳입니다
- 소스 코드와 빌드 산출물을 분리하여 깔끔하게 관리할 수 있습니다

### 단계 2: CMake 설정 (Configure)

```bash
cmake .. -DBUILD_TESTS=ON
```

**이 단계에서 수행되는 작업**:

1. **프로젝트 구조 파악**
   ```
   CMakeLists.txt (루트) 읽기
   ├── socket_lib/CMakeLists.txt 분석
   ├── common/CMakeLists.txt 분석
   ├── client_lib/CMakeLists.txt 분석
   ├── server_lib/CMakeLists.txt 분석
   └── test/CMakeLists.txt 분석 (BUILD_TESTS=ON인 경우)
   ```

2. **의존성 확인**
   - Threads 라이브러리 찾기 (`find_package(Threads REQUIRED)`)
   - C 컴파일러 확인
   - 필요한 헤더 파일 경로 설정

3. **타겟 정의**
   - **라이브러리 타겟들**:
     - `socket_lib` (SHARED 라이브러리)
     - `common` (SHARED 라이브러리)
     - `client_lib` (SHARED 라이브러리)
     - `server_lib` (SHARED 라이브러리)
     - `chat` (메인 SHARED 라이브러리, 모든 모듈 통합)
   
   - **테스트 타겟들** (BUILD_TESTS=ON인 경우):
     - `test_framework` (STATIC 라이브러리)
     - `protocol_test` (실행 파일)
     - `command_queue_test` (실행 파일)
     - `command_test` (실행 파일)
     - `echo_test` (통합 테스트 실행 파일)

4. **빌드 시스템 파일 생성**
   - `CMakeCache.txt`: 설정 캐시
   - `build.ninja` 또는 `Makefile`: 실제 빌드 파일
   - `cmake_install.cmake`: 설치 스크립트

**출력 예시**:
```
-- The C compiler identification is AppleClang 15.0.0
-- Detecting C compiler ABI info
-- Detecting C compiler ABI info - done
-- Check for working C compiler: /usr/bin/cc - skipped
-- Detecting C compile features
-- Detecting C compile features - done
-- Looking for pthread.h
-- Looking for pthread.h - found
-- Found Threads: TRUE
-- Configuring done
-- Generating done
-- Build files have been written to: /path/to/build
```

### 단계 3: 실제 컴파일 (Build)

```bash
make
# 또는
cmake --build .
# 또는 (Ninja 빌드 시스템 사용 시)
ninja
```

**이 단계에서 수행되는 작업**:

#### 3.1 의존성 순서에 따른 라이브러리 빌드

1. **socket_lib 빌드** (최우선, 다른 모든 모듈의 기반)
   ```
   컴파일: socket_utils.c → socket_utils.o
   링크: socket_utils.o → libsocket_lib.dylib (macOS) / libsocket_lib.so (Linux)
   ```
   - **소스 파일**: `socket_lib/src/socket_utils.c`
   - **헤더 파일**: `socket_lib/include/socket_utils.h`
   - **출력**: `socket_lib/libsocket_lib.dylib`

2. **common 빌드** (socket_lib에 의존)
   ```
   컴파일: 
     - protocol.c → protocol.o
     - command_queue.c → command_queue.o
     - command.c → command.o
   링크: [object files] + libsocket_lib.dylib + pthread → libcommon.dylib
   ```
   - **소스 파일들**:
     - `common/src/protocol.c`
     - `common/src/command_queue.c`
     - `common/src/command.c`
   - **헤더 파일들**:
     - `common/include/protocol.h`
     - `common/include/command_queue.h`
     - `common/include/command.h`
   - **의존성**: `socket_lib`, `Threads::Threads`
   - **출력**: `common/libcommon.dylib`

3. **client_lib 빌드** (common, socket_lib에 의존)
   ```
   컴파일: chat_client.c → chat_client.o
   링크: chat_client.o + libcommon.dylib + libsocket_lib.dylib → libclient_lib.dylib
   ```
   - **소스 파일**: `client_lib/src/chat_client.c`
   - **헤더 파일**: `client_lib/include/chat_client.h`
   - **의존성**: `common`, `socket_lib`
   - **출력**: `client_lib/libclient_lib.dylib`

4. **server_lib 빌드** (common, socket_lib에 의존)
   ```
   컴파일: chat_server.c → chat_server.o
   링크: chat_server.o + libcommon.dylib + libsocket_lib.dylib → libserver_lib.dylib
   ```
   - **소스 파일**: `server_lib/src/chat_server.c`
   - **헤더 파일**: `server_lib/include/chat_server.h`
   - **의존성**: `common`, `socket_lib`
   - **출력**: `server_lib/libserver_lib.dylib`

5. **메인 chat 라이브러리 빌드** (모든 모듈 통합)
   ```
   컴파일 (통합):
     - client_lib/src/chat_client.c
     - server_lib/src/chat_server.c
     - common/src/protocol.c
     - common/src/command_queue.c
     - common/src/command.c
     - socket_lib/src/socket_utils.c
   → 각각의 object 파일들
   
   링크: 모든 object 파일들 + pthread → libchat.dylib
   ```
   - **출력**: `libchat.dylib` (메인 공유 라이브러리)
   - **용도**: Java/Python 바인딩에서 사용

#### 3.2 테스트 빌드 (BUILD_TESTS=ON인 경우)

1. **test_framework 빌드**
   ```
   컴파일: test/src/test_framework.c → test_framework.o
   링크: test_framework.o → libtest_framework.a (정적 라이브러리)
   ```
   - **출력**: `test/libtest_framework.a`

2. **단위 테스트 빌드**

   a. **protocol_test**
   ```
   컴파일: test/protocol_test.c → protocol_test.o
   링크: protocol_test.o + libcommon.dylib + libsocket_lib.dylib + libtest_framework.a 
         → protocol_test (실행 파일)
   ```

   b. **command_queue_test**
   ```
   컴파일: test/command_queue_test.c → command_queue_test.o
   링크: command_queue_test.o + libcommon.dylib + libtest_framework.a + pthread
         → command_queue_test (실행 파일)
   ```

   c. **command_test**
   ```
   컴파일: test/command_test.c → command_test.o
   링크: command_test.o + libcommon.dylib + libtest_framework.a
         → command_test (실행 파일)
   ```

3. **통합 테스트 빌드 (echo_test)**
   ```
   컴파일: test/echo_test.c → echo_test.o
   링크: echo_test.o + libclient_lib.dylib + libserver_lib.dylib + 
         libcommon.dylib + libsocket_lib.dylib + pthread
         → echo_test (실행 파일)
   ```

**출력 예시**:
```
[  5%] Building C object socket_lib/CMakeFiles/socket_lib.dir/src/socket_utils.c.o
[ 10%] Linking C shared library socket_lib/libsocket_lib.dylib
[ 15%] Building C object common/CMakeFiles/common.dir/src/protocol.c.o
[ 20%] Building C object common/CMakeFiles/common.dir/src/command_queue.c.o
[ 25%] Building C object common/CMakeFiles/common.dir/src/command.c.o
[ 30%] Linking C shared library common/libcommon.dylib
...
[100%] Built target echo_test
```

### 단계 4: 빌드 결과 확인

```bash
# 라이브러리 파일 확인
ls -lh *.dylib
ls -lh */lib*.dylib

# 테스트 실행 파일 확인
ls -lh test/*_test
```

**예상 출력 파일들**:
```
build/
├── libchat.dylib                    # 메인 통합 라이브러리
├── socket_lib/
│   └── libsocket_lib.dylib
├── common/
│   └── libcommon.dylib
├── client_lib/
│   └── libclient_lib.dylib
├── server_lib/
│   └── libserver_lib.dylib
└── test/
    ├── protocol_test                # 단위 테스트 실행 파일
    ├── command_queue_test
    ├── command_test
    └── echo_test                    # 통합 테스트 실행 파일
```

---

## 빌드 옵션

### 테스트 없이 빌드

```bash
cmake .. -DBUILD_TESTS=OFF
make
```

### 디버그 모드로 빌드

```bash
cmake .. -DCMAKE_BUILD_TYPE=Debug -DBUILD_TESTS=ON
make
```

### 릴리즈 모드로 빌드

```bash
cmake .. -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTS=OFF
make
```

### 병렬 빌드 (빠른 빌드)

```bash
make -j4  # 4개 코어 사용
# 또는
cmake --build . --parallel 4
```

---

## 빌드 후 작업

### 1. 테스트 실행

```bash
# 개별 테스트 실행
./test/protocol_test
./test/command_queue_test
./test/command_test
./test/echo_test

# CTest를 통한 모든 테스트 실행
ctest --output-on-failure

# 또는 커스텀 타겟 사용
make run_all_tests
```

### 2. 라이브러리 설치 (선택사항)

```bash
sudo make install
```

기본 설치 경로: `/usr/local/lib` (라이브러리), `/usr/local/include` (헤더)

### 3. 정리

```bash
# 빌드 파일만 삭제 (소스는 유지)
cd build
rm -rf *

# 또는 완전히 삭제
cd ..
rm -rf build
```

---

## 문제 해결

### CMake를 찾을 수 없는 경우

```bash
# macOS에서 Homebrew로 설치
brew install cmake

# PATH 확인
which cmake
echo $PATH

# 직접 경로 사용 (설치된 경우)
/usr/local/bin/cmake --version
```

### 컴파일 오류 발생 시

1. **의존성 확인**
   ```bash
   # macOS
   xcode-select --install
   
   # Linux
   sudo apt-get install build-essential
   ```

2. **깨끗한 빌드 시도**
   ```bash
   cd build
   rm -rf *
   cmake .. -DBUILD_TESTS=ON
   make clean
   make
   ```

3. **상세 빌드 로그 확인**
   ```bash
   make VERBOSE=1
   ```

### 링크 오류 발생 시

- 라이브러리 경로 확인
- 의존성 순서 확인
- pthread 라이브러리 링크 확인

---

## 빌드 타임라인 요약

```
1. cmake .. (설정 단계)
   └─ 약 5-10초

2. make (컴파일 및 링크 단계)
   └─ 약 30초-2분 (코어 수에 따라 다름)
   └─ 테스트 포함 시 약 1-3분

3. ctest (테스트 실행 단계)
   └─ 약 1-5초
```

**전체 빌드 시간**: 약 1-5분 (시스템 성능에 따라 다름)
