#!/bin/bash
set -ex

# Build all targets
./gradlew build

# Run Native (auto-detect platform)
if [[ $(uname -m) == "arm64" && $(uname -s) == "Darwin" ]]; then
    ./app/build/bin/macosArm/releaseExecutable/app.kexe
elif [[ $(uname -s) == "Darwin" ]]; then
    ./app/build/bin/macos/releaseExecutable/app.kexe
else
    ./app/build/bin/native/releaseExecutable/app.kexe
fi

# Run JVM
KOTLIN_STDLIB=$(find ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib -name "kotlin-stdlib-*.jar" | grep -v sources | head -1)
java -cp "app/build/classes/kotlin/jvm/main:mathlib/build/libs/mathlib-jvm.jar:stringlib/build/libs/stringlib-jvm.jar:$KOTLIN_STDLIB" MainKt