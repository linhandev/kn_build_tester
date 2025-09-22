set -ex

./gradlew linkDebugExecutableNative --rerun-tasks

hdc file send build/bin/native/debugExecutable/kn-sample.kexe /data/local/tmp
hdc shell chmod a+x /data/local/tmp/kn-sample.kexe
hdc shell LD_PRELOAD=/data/app/el1/bundle/public/com.huawei.hmos.location/libs/arm64/libc++_shared.so /data/local/tmp/kn-sample.kexe
