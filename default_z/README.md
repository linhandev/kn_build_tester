# default_z: Default -z now vs -z lazy

**Result**: `~/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++` defaults to **-z now** (BIND_NOW).

Evidence: Built minimal exe (empty main), inspected with `llvm-readobj --dynamic-table`; DynamicSection shows:
- `FLAGS   BIND_NOW`
- `FLAGS_1 NOW PIE`

Build (copy-paste from repo root or from `experiments/default_z`):

```bash
cd "$(git rev-parse --show-toplevel)/experiments/default_z"
$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
  -target aarch64-linux-ohos \
  --sysroot=/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot \
  -resource-dir=$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19 \
  -L/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot/usr/lib/aarch64-linux-ohos \
  main.cpp -o z_default
```

Inspect: `$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/llvm-readobj --dynamic-table z_default`

**Confirm `-Wl,-z,lazy` works:** Build with lazy binding; inspect should show no BIND_NOW:

```bash
$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/clang++ \
  -target aarch64-linux-ohos \
  --sysroot=/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot \
  -resource-dir=$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/lib/clang/19 \
  -L/Applications/DevEco-Studio-6.0.2.636.app/Contents/sdk/default/openharmony/native/sysroot/usr/lib/aarch64-linux-ohos \
  -Wl,-z,lazy main.cpp -o z_lazy
```

Inspect: `$HOME/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2/bin/llvm-readobj --dynamic-table z_lazy` → FLAGS should **not** include BIND_NOW (lazy binding).
