#!/usr/bin/env python3
"""
C 라이브러리를 Python 프로젝트로 복사하는 스크립트
"""

import os
import sys
import shutil
import platform
from pathlib import Path


def detect_platform():
    """플랫폼 감지"""
    system = platform.system()
    machine = platform.machine().lower()
    
    if system == "Darwin":
        if machine in ("arm64", "aarch64"):
            return "darwin-aarch64", ".dylib"
        elif machine == "x86_64":
            return "darwin-x86_64", ".dylib"
    elif system == "Linux":
        if machine in ("aarch64", "arm64"):
            return "linux-aarch64", ".so"
        elif machine == "x86_64":
            return "linux-x86_64", ".so"
    
    return f"unknown-{machine}", ".so"


def copy_library(build_dir=None):
    """C 라이브러리를 복사"""
    # 프로젝트 루트 찾기
    script_dir = Path(__file__).parent.absolute()
    project_root = script_dir.parent
    c_lib_dir = project_root / "C_chat_lib"
    
    # 빌드 디렉토리 설정
    if build_dir:
        build_path = Path(build_dir)
    else:
        build_path = c_lib_dir / "build"
    
    # 플랫폼 감지
    platform_dir, lib_ext = detect_platform()
    lib_name = f"libchat{lib_ext}"
    
    # 소스 라이브러리 경로
    source_lib = build_path / lib_name
    
    # 대상 경로
    target_dir = script_dir / "resources" / platform_dir
    target_lib = target_dir / lib_name
    
    # 소스 라이브러리 확인
    if not source_lib.exists():
        print(f"❌ Error: Library not found at {source_lib}")
        print(f"   Hint: Build the C library first:")
        print(f"   cd {c_lib_dir} && mkdir -p build && cd build && cmake .. && make")
        sys.exit(1)
    
    # 대상 디렉토리 생성
    target_dir.mkdir(parents=True, exist_ok=True)
    
    # 파일 복사
    shutil.copy2(source_lib, target_lib)
    
    print(f"✅ Copied library successfully!")
    print(f"   Platform: {platform_dir}")
    print(f"   Source: {source_lib}")
    print(f"   Target: {target_lib}")
    print(f"   Size: {target_lib.stat().st_size / 1024:.2f} KB")


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Copy C library to Python project")
    parser.add_argument(
        "--build-dir",
        help="C library build directory (default: ../C_chat_lib/build)"
    )
    
    args = parser.parse_args()
    copy_library(args.build_dir)
