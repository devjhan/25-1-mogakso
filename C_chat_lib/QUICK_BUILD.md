# 빠른 빌드 가이드

## 1. CMake 설치 (첫 실행 시)

```bash
# Homebrew로 CMake 설치
brew install cmake

# 설치 확인
cmake --version
# CMake version 3.28.0 이상이 나와야 합니다
```

## 2. 빌드 실행 (전체 프로세스)

```bash
# 프로젝트 루트로 이동
cd ~/projects/chat_project/C_chat_lib

# 빌드 디렉토리 생성 및 이동
mkdir -p build && cd build

# CMake 설정 (테스트 포함)
cmake .. -DBUILD_TESTS=ON

# 컴파일 및 빌드
make -j4  # -j4는 4개 코어를 병렬로 사용 (옵션)

# 테스트 실행 (선택사항)
ctest --output-on-failure

# 또는 모든 테스트 한 번에 실행
make run_all_tests
```

## 3. 빌드 결과 확인

```bash
# 라이브러리 파일 확인
find . -name "*.dylib" -o -name "*.so"

# 테스트 실행 파일 확인
find test -type f -perm +111 2>/dev/null | grep test

# 메인 라이브러리 위치
ls -lh libchat.dylib
```

## 4. 테스트 실행

```bash
# 개별 테스트 실행
./test/protocol_test
./test/command_queue_test
./test/command_test
./test/echo_test

# CTest를 통한 전체 테스트
ctest -V
```

## 5. 문제 발생 시

### CMake 명령어를 찾을 수 없는 경우

```bash
# Homebrew로 설치한 CMake 경로 확인
ls -la /opt/homebrew/bin/cmake
# 또는
ls -la /usr/local/bin/cmake

# PATH에 추가 (필요한 경우)
export PATH="/opt/homebrew/bin:$PATH"  # Apple Silicon Mac
# 또는
export PATH="/usr/local/bin:$PATH"     # Intel Mac
```

### 빌드 오류 발생 시

```bash
# 깨끗한 빌드 시도
cd build
rm -rf *
cmake .. -DBUILD_TESTS=ON
make VERBOSE=1  # 상세 출력으로 오류 확인
```
