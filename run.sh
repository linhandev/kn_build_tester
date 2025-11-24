#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

./gradlew --stop
./gradlew clean

echo "Publishing compatible dep-lib and caller-lib klibs to mavenLocal..."
./gradlew :dep-lib:publishToMavenLocal :caller-lib:publishToMavenLocal --console=plain --rerun-tasks

echo "Updating dep-lib to introduce ABI-incompatible change..."
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt.bk
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.broken dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:publishToMavenLocal --console=plain --rerun-tasks
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.broken
mv dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt.bk dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt

echo "Building app with partial linkage disabled (expected failure)..."
if ./gradlew -PpartialLinkMode=disable :app:clean :app:linkDebugExecutableMacosArm64 --console=plain --rerun-tasks --refresh-dependencies; then
  echo "Partial linkage disabled build unexpectedly succeeded"
  exit 1
else
  echo "Partial linkage disabled build failed as expected"
fi

echo "Building app with partial linkage enabled (expected success)..."
./gradlew -PpartialLinkMode=enable :app:clean :app:linkDebugExecutableMacosArm64 --console=plain --rerun-tasks --refresh-dependencies

echo "Running macOS kexe (will crash on missing symbols)..."
if ./app/build/bin/macosArm64/debugExecutable/app.kexe; then
  echo "❌ Application should crash at runtime yet it didn't!"
else
  echo "✅ Runtime crash confirms missing symbols - partial linkage allowed build but not execution"
fi