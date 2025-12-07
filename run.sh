#!/bin/bash

# set -ex

echo "Cleaning..."
./gradlew clean

echo "----------------------------------------------------------------"
echo "Building WITHOUT caches (kotlin.native.cacheKind=none)..."
echo "----------------------------------------------------------------"
./gradlew :app:linkDebugExecutableOhosArm64 -Pkotlin.native.cacheKind=none

if [ $? -eq 0 ]; then
    echo "Build WITHOUT caches SUCCEEDED."
    echo "Running on device..."
    hdc file send app/build/bin/ohosArm64/debugExecutable/app.kexe /data/local/tmp/
    hdc shell chmod 777 /data/local/tmp/app.kexe
    hdc shell /data/local/tmp/app.kexe
else
    echo "Build WITHOUT caches FAILED."
fi

echo "----------------------------------------------------------------"
echo "Building WITH static caches (kotlin.native.cacheKind=static)..."
echo "----------------------------------------------------------------"
./gradlew clean
./gradlew :app:linkDebugExecutableOhosArm64 -Pkotlin.native.cacheKind=static

if [ $? -eq 0 ]; then
    echo "Build WITH static caches SUCCEEDED."
    echo "Running on device..."
    hdc file send app/build/bin/ohosArm64/debugExecutable/app.kexe /data/local/tmp/
    hdc shell chmod 777 /data/local/tmp/app.kexe
    hdc shell /data/local/tmp/app.kexe
else
    echo "Build WITH static caches FAILED."
fi
