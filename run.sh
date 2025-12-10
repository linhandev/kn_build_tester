#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

./gradlew --stop
./gradlew clean

echo "👀 Publishing compatible dep-lib and caller-lib klibs to mavenLocal..."
./gradlew :dep-lib:publishToMavenLocal :caller-lib:publishToMavenLocal --console=plain --rerun-tasks

echo "👀 Updating dep-lib to introduce ABI-incompatible change..."
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt.bk
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.broken dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:publishToMavenLocal --console=plain --rerun-tasks
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.broken
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt.bk dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt

# now caller-lib has call to non-existent symbols in dep-lib
# testing the default, pl = enable, pl = disable behaviors

echo "👀 Building app without any partial linkage setting, expecting success..."
./gradlew :app:clean :app:linkDebugSharedMacosArm64 --console=plain --rerun-tasks --refresh-dependencies

echo "👀 Building app with partial linkage disabled (expected failure)..."
if ./gradlew -PpartialLinkMode=disable :app:clean :app:linkDebugSharedMacosArm64 --console=plain --rerun-tasks --refresh-dependencies; then
  echo "❌ Partial linkage disabled build unexpectedly succeeded"
  exit 1
else
  echo "✅ Partial linkage disabled build failed as expected"
fi

echo "👀 Building app with partial linkage enabled (expected success)..."
./gradlew -PpartialLinkMode=enable :app:clean :app:linkDebugSharedMacosArm64 --console=plain --rerun-tasks --refresh-dependencies

echo "👀 Compiling and running C caller (will crash on missing symbols)..."
clang -o runner c-caller/main.c -I app/build/bin/macosArm64/debugShared -L app/build/bin/macosArm64/debugShared -lapp -rpath app/build/bin/macosArm64/debugShared

if ./runner; then
  echo "❌ Application should crash at runtime yet it didn't!"
else
  echo "✅ Runtime crash confirms missing symbols - partial linkage allowed build but not execution"
fi