set -ex

./gradlew linkDebugExecutableNative --rerun-tasks
hdc file send build/bin/native/debugExecutable/kn-sample.kexe /data/
hdc shell chmod 777 /data/kn-sample.kexe
hdc file send build/libhello.so /data/
hdc shell LD_LIBRARY_PATH=/data/ /data/kn-sample.kexe
