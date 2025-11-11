set -ex

rm -rf build/bin/

BUILD_MODE=release
BUILD_MODE_CAPITALIZED=$(echo ${BUILD_MODE} | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
KONAN_DATA_DIR=${KONAN_DATA_DIR:-$(realpath ~/.konan)}

./gradlew link${BUILD_MODE_CAPITALIZED}SharedOhosArm64 --rerun-tasks

cd znow-test
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang++ \
    --target=aarch64-linux-ohos \
    --shared \
    -I. \
    -o libznow.so \
    znow_test.cpp
hdc file send libznow.so /data/local/tmp/
file libznow.so
cd -

cd c-caller
/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang++ \
    --sysroot /Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/sysroot/ \
    --target=aarch64-linux-ohos \
    -fPIC -pthread \
    -Wall -Wextra -std=c++17 \
    -I../build/bin/ohosArm64/${BUILD_MODE}Shared \
    -o main main.cpp \
    -L../build/bin/ohosArm64/${BUILD_MODE}Shared \
    -lc2k \
    -L../znow-test \
    -lznow

hdc file send main /data/local/tmp/
file main
cd -


hdc file send build/bin/ohosArm64/${BUILD_MODE}Shared/libc2k.so /data/local/tmp/
hdc shell chmod 777 /data/local/tmp/main
hdc shell LD_LIBRARY_PATH=/data/local/tmp/ /data/local/tmp/main