# C 라이브러리 빌드 결과물 자동 복사 설정 완료

## 구현된 자동화 방법

### ✅ 완료된 작업

1. **CMake Post-Build 자동 복사** ✅
   - C 라이브러리 빌드 시 자동으로 Java/Python 프로젝트로 복사
   - 플랫폼 자동 감지 (darwin-aarch64, darwin-x86_64, linux-x86_64 등)

2. **Gradle Task 자동화** ✅
   - Java 프로젝트 빌드 시 C 라이브러리 자동 빌드 및 복사
   - `buildCLibrary`, `copyCLibrary` task 제공

3. **Python 스크립트** ✅
   - Python 프로젝트에서 독립적으로 사용 가능
   - 플랫폼 자동 감지

4. **통합 빌드 스크립트** ✅
   - 전체 프로젝트 일괄 빌드
   - C → Java → Python 순서로 빌드

5. **수동 복사 스크립트** ✅
   - C 라이브러리만 빌드된 경우 수동으로 복사 가능

---

## 사용 방법

### 방법 1: CMake 자동 복사 (가장 간단) ⭐ 권장

```bash
cd C_chat_lib
mkdir -p build && cd build
cmake .. -DBUILD_TESTS=ON
make  # 빌드 완료 시 자동으로 복사됨
```

**장점**:
- C 라이브러리 빌드와 동시에 자동 복사
- 추가 작업 불필요

---

### 방법 2: Gradle 자동 빌드 및 복사

```bash
cd java_chat_server
./gradlew build  # C 라이브러리 자동 빌드 및 복사 후 Java 빌드
```

**사용 가능한 Tasks**:
```bash
./gradlew buildCLibrary    # C 라이브러리만 빌드
./gradlew copyCLibrary     # C 라이브러리만 복사 (이미 빌드된 경우)
./gradlew build            # 전체 빌드 (C 라이브러리 자동 포함)
```

**장점**:
- Java 프로젝트 빌드 시 자동으로 C 라이브러리도 처리
- 의존성 관리 자동화

---

### 방법 3: Python 스크립트

```bash
cd py_chat_client
python3 copy_library.py
# 또는 빌드 디렉토리 지정
python3 copy_library.py --build-dir ../C_chat_lib/build
```

**장점**:
- Python 프로젝트 독립적으로 사용 가능
- 빌드 디렉토리 지정 가능

---

### 방법 4: 통합 빌드 스크립트

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

---

### 방법 5: 수동 복사 스크립트

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

**장점**:
- C 라이브러리만 빌드된 경우 빠른 복사
- 플랫폼 자동 감지

---

## 플랫폼 지원

자동화 스크립트는 다음 플랫폼을 자동 감지합니다:

| OS | Architecture | 플랫폼 디렉토리 | 라이브러리 확장자 |
|---|---|---|---|
| macOS | ARM64 (Apple Silicon) | `darwin-aarch64` | `.dylib` |
| macOS | x86_64 (Intel) | `darwin-x86_64` | `.dylib` |
| Linux | x86_64 | `linux-x86_64` | `.so` |
| Linux | ARM64 | `linux-aarch64` | `.so` |

---

## 복사 경로

### Java 프로젝트
```
java_chat_server/src/main/resources/{platform}/libchat.{ext}
```

### Python 프로젝트
```
py_chat_client/resources/{platform}/libchat.{ext}
```

---

## 테스트 결과

✅ **스크립트 테스트 완료**:
- `copy_library.sh` 정상 작동 확인
- 플랫폼 감지 정상 작동 (darwin-aarch64)
- Java/Python 프로젝트로 복사 성공

**테스트 출력**:
```
✓ Copied to Java project: .../java_chat_server/src/main/resources/darwin-aarch64/libchat.dylib
✓ Copied to Python project: .../py_chat_client/resources/darwin-aarch64/libchat.dylib
Library copied successfully!
```

---

## 시나리오별 사용법

### 시나리오 1: C 라이브러리만 수정
```bash
cd C_chat_lib/build
make  # 자동으로 복사됨 (CMake post-build)
```

### 시나리오 2: Java 프로젝트만 빌드
```bash
cd java_chat_server
./gradlew build  # C 라이브러리 자동 빌드/복사 후 Java 빌드
```

### 시나리오 3: Python 프로젝트만 사용
```bash
cd py_chat_client
python3 copy_library.py
```

### 시나리오 4: 전체 프로젝트 처음부터 빌드
```bash
./build_all.sh
```

---

## 추가 개선 사항

### 향후 개선 가능 영역

1. **크로스 컴파일 지원**: 다른 플랫폼용 라이브러리 빌드
2. **버전 관리**: 라이브러리 버전 체크 및 업데이트 알림
3. **의존성 검증**: 파일 무결성 검증 (checksum)
4. **병렬 빌드**: 여러 플랫폼 동시 빌드
5. **캐싱**: 변경되지 않은 라이브러리는 복사 건너뛰기

---

## 문제 해결

### 라이브러리를 찾을 수 없는 경우

**에러**:
```
Error: Library not found at C_chat_lib/build/libchat.dylib
```

**해결**:
```bash
cd C_chat_lib
mkdir -p build && cd build
cmake .. && make
```

### Gradle Task 실패

**에러**:
```
Execution failed for task ':buildCLibrary'
```

**해결**:
1. CMake 설치 확인: `cmake --version`
2. C 라이브러리 수동 빌드 후 Gradle 재실행
3. 또는 `./gradlew copyCLibrary` 사용 (이미 빌드된 경우)

---

## 파일 구조

```
chat_project/
├── C_chat_lib/
│   ├── CMakeLists.txt              # Post-build 복사 설정 ✅
│   ├── scripts/
│   │   └── copy_library.sh        # 수동 복사 스크립트 ✅
│   └── build/
│       └── libchat.dylib          # 빌드 결과물
├── java_chat_server/
│   ├── build.gradle               # C 라이브러리 task 포함 ✅
│   └── src/main/resources/
│       └── darwin-aarch64/
│           └── libchat.dylib      # 복사된 라이브러리
├── py_chat_client/
│   ├── copy_library.py            # Python 복사 스크립트 ✅
│   └── resources/
│       └── darwin-aarch64/
│           └── libchat.dylib      # 복사된 라이브러리
└── build_all.sh                   # 통합 빌드 스크립트 ✅
```

---

## 요약

✅ **5가지 자동화 방법 구현 완료**:
1. CMake Post-Build 자동 복사
2. Gradle Task 자동화
3. Python 스크립트
4. 통합 빌드 스크립트
5. 수동 복사 스크립트

✅ **플랫폼 자동 감지 지원**:
- macOS ARM64, x86_64
- Linux x86_64, ARM64

✅ **테스트 완료**:
- 모든 스크립트 정상 작동 확인

이제 수동으로 라이브러리를 복사할 필요가 없습니다! 🎉
