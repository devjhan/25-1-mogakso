# C 라이브러리 테스트 가이드

## 개요

이 디렉토리는 C 채팅 라이브러리의 체계적인 테스트를 포함합니다.

## 테스트 구조

### 단위 테스트 (Unit Tests)

- **protocol_test.c**: 프로토콜 프레임 생성 및 파싱 테스트
- **command_queue_test.c**: 명령 큐 동작 테스트 (스레드 안전성 포함)
- **command_test.c**: 명령 생성 및 파괴 테스트

### 통합 테스트 (Integration Tests)

- **echo_test.c**: 클라이언트-서버 통신 통합 테스트

## 테스트 프레임워크

간단한 자체 테스트 프레임워크를 사용합니다:
- `TEST(name)`: 테스트 함수 정의
- `RUN_TEST(name)`: 테스트 실행
- `ASSERT_*` 매크로: 다양한 검증 매크로

## 빌드 및 실행

### 전체 테스트 빌드

```bash
cd C_chat_lib
mkdir -p build
cd build
cmake .. -DBUILD_TESTS=ON
make
```

### 단위 테스트 실행

```bash
# 개별 테스트 실행
./test/protocol_test
./test/command_queue_test
./test/command_test

# 통합 테스트 실행
./test/echo_test
```

### CTest를 통한 테스트 실행

```bash
# 모든 테스트 실행
ctest --output-on-failure

# 특정 테스트만 실행
ctest -R ProtocolTest
ctest -R CommandQueueTest
ctest -R CommandTest
ctest -R EchoTest

# 상세 출력과 함께 실행
ctest --verbose
```

### 모든 테스트 한 번에 실행

```bash
make run_all_tests
```

## 테스트 커버리지

현재 테스트 커버리지:
- **Protocol 모듈**: 프레임 생성, 파싱, 빈 페이로드, 분할 전송 등
- **Command Queue 모듈**: 생성/파괴, push/pop, FIFO 순서, 스레드 안전성
- **Command 모듈**: 명령 생성, 파괴, 대용량 페이로드 처리

## 테스트 추가 방법

새 테스트를 추가하려면:

1. 새로운 테스트 파일 생성 (예: `new_module_test.c`)
2. `test_framework.h` 포함
3. `TEST(test_name)` 매크로로 테스트 함수 정의
4. `ASSERT_*` 매크로로 검증
5. `main()` 함수에서 `test_init()`, `RUN_TEST()`, `test_finish()` 호출
6. `CMakeLists.txt`에 테스트 추가:

```cmake
add_executable(new_module_test new_module_test.c)
target_link_libraries(new_module_test PRIVATE
    module_name
    test_framework
)

add_test(NAME NewModuleTest COMMAND new_module_test)
```

## 디버깅

### Valgrind로 메모리 검사

```bash
valgrind --leak-check=full --show-leak-kinds=all ./test/protocol_test
valgrind --leak-check=full --show-leak-kinds=all ./test/command_queue_test
```

### GDB로 디버깅

```bash
gdb ./test/protocol_test
(gdb) run
```

## 문제 해결

### 테스트가 빌드되지 않는 경우

1. `BUILD_TESTS=ON` 옵션이 설정되어 있는지 확인
2. 모든 의존성 라이브러리가 빌드되었는지 확인

### 테스트 실행 실패

1. 각 테스트를 개별적으로 실행하여 실패 원인 확인
2. `ctest --verbose`로 상세 출력 확인
3. 포트 충돌 시 (echo_test): 다른 포트로 변경
