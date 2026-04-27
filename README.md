# Kotlin/Native Exception Demo (Minimal)

Keep `bare` branch a starting point for doing a demo, impl demos on another branch.

Full build command.

```shell
clear
hdc uninstall com.kotlin.demo || true
KN_ACTION_BUILD_REPO_ABS=/Users/ohoskt/git/agent/kotlin/build/repo ./gradlew clean
./gradlew --stop
KN_ACTION_BUILD_REPO_ABS=/Users/ohoskt/git/agent/kotlin/build/repo ./gradlew startHarmonyAppDebug --rerun-tasks
```

`KN_ACTION_BUILD_REPO_ABS` forces Gradle plugin and dependency resolution to use the freshly published Kotlin build instead of a remote repository.

## LibBacktrace demo

The app calls `runLibbacktraceDemo()` on startup and logs a caught Kotlin exception stack trace through hilog. Tap the main text to refresh the caught stack trace without crashing.

Tap **Crash for fault log** to call `runLibbacktraceCrash()`. This intentionally lets a Kotlin exception terminate the process so OHOS writes a native fault log for `com.kotlin.demo`.

## Bundle name (from project)

The installed app’s **bundle name** is **`app.bundleName`** in **`harmonyApp/AppScope/app.json5`** (for this sample it is `com.kotlin.demo`). Use the same value for `hdc uninstall`, `aa start`, and filtering crash logs.

Read it from the repo (from the project root):

```shell
grep bundleName harmonyApp/AppScope/app.json5
```

## Pull the latest crash / fault log for this app

Fault dumps for apps usually land under **`/data/log/faultlog/faultlogger/`** (freeze-related dumps often under **`/data/log/faultlog/freeze_ext/`**). Filenames typically include the **bundle name**, so you can take the newest matching file.

From the project root (macOS/Linux; strips a trailing CR from `hdc` output):

```shell
bundle=$(sed -n 's/.*"bundleName"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' harmonyApp/AppScope/app.json5)
latest=$(hdc shell "ls -t /data/log/faultlog/faultlogger/" | tr -d '\r' | awk -v bundle="$bundle" 'index($0, bundle) { print; exit }')
hdc file recv "/data/log/faultlog/faultlogger/$latest" ./
rg 'libbacktrace|helloworld\.kt|kn_libbacktrace|kfun:|\.kt:' "$latest"
```

Freeze logs for the same app (same idea, different directory):

```shell
latest=$(hdc shell "ls -t /data/log/faultlog/freeze_ext/" | tr -d '\r' | awk -v bundle="$bundle" 'index($0, bundle) { print; exit }')
hdc file recv "/data/log/faultlog/freeze_ext/$latest" ./
```

If `latest` is empty, list recent files and pick the one whose name matches your bundle: `hdc shell "ls -lt /data/log/faultlog/faultlogger/ | head -n 20"`.
