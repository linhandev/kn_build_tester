#!/bin/bash

# Demo script for Kotlin Native Partial Linkage functionality
# This script demonstrates the difference between enabled and disabled partial linkage

PROJECT_DIR="/home/runner/work/kn_samples/kn_samples"
cd "$PROJECT_DIR"

echo "========================================"
echo "Kotlin Native Partial Linkage Demo"
echo "========================================"
echo ""
echo "This demo shows how partial linkage handles binary incompatibility"
echo "between klib libraries."
echo ""

# Step 1: Ensure we have the compatible version built
echo "Step 1: Building compatible versions..."
echo "----------------------------------------"
git restore dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt 2>/dev/null || true
./gradlew clean -q
./gradlew :dep-lib:linuxX64MainKlibrary :caller-lib:linuxX64MainKlibrary -q
echo "✓ Compatible klibs built"
echo ""

# Backup compatible klibs
mkdir -p /tmp/partial-linkage-demo
cp dep-lib/build/classes/kotlin/linuxX64/main/klib/dep-lib.klib /tmp/partial-linkage-demo/dep-lib-COMPATIBLE.klib
cp -r caller-lib/build /tmp/partial-linkage-demo/caller-lib-build-backup
echo "✓ Backed up compatible versions"
echo ""

# Step 2: Build incompatible version of dep-lib
echo "Step 2: Building INCOMPATIBLE version of dep-lib..."
echo "----------------------------------------"
cp DepLibrary-incompatible.kt.template dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:clean :dep-lib:linuxX64MainKlibrary -q
cp dep-lib/build/classes/kotlin/linuxX64/main/klib/dep-lib.klib /tmp/partial-linkage-demo/dep-lib-INCOMPATIBLE.klib
echo "✓ Incompatible dep-lib built"
echo ""

echo "SCENARIO:"
echo "  - caller-lib.klib was compiled against COMPATIBLE dep-lib"
echo "  - dep-lib.klib is now INCOMPATIBLE (functions removed/changed)"
echo "  - ios-app will try to link both libraries"
echo ""
echo "Incompatibilities introduced:"
echo "  1. Function REMOVED: greetUser(name: String)"
echo "  2. Signature CHANGED: calculateSum(a, b) -> calculateSum(a, b, c)"  
echo "  3. Class REMOVED: UserData"
echo "  4. Function REMOVED: processUserData(data)"
echo "  5. Function REMOVED: ConfigHelper.getConfigValue()"
echo ""

# Restore caller-lib (compiled against old dep-lib)
rm -rf caller-lib/build
cp -r /tmp/partial-linkage-demo/caller-lib-build-backup caller-lib/build
echo "✓ Restored caller-lib (still references old dep-lib API)"
echo ""

###########################################
# TEST 1: Partial Linkage DISABLED
###########################################
echo "=========================================="
echo "TEST 1: Partial Linkage DISABLED"
echo "=========================================="
echo ""

# Create build configuration with partial linkage disabled
cat > ios-app/build.gradle.kts << 'GRADLE_EOF'
plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    sourceSets {
        val linuxX64Main by getting {
            dependencies {
                implementation(project(":dep-lib"))
                implementation(project(":caller-lib"))
            }
        }
    }
}
GRADLE_EOF

echo "Configuration: freeCompilerArgs = [\"-Xpartial-linkage=disable\"]"
echo ""
echo "Expected Behavior:"
echo "  ❌ Build should FAIL"
echo "  ❌ Compiler errors about missing/incompatible symbols"
echo "  ❌ No executable produced"
echo ""
echo "Building ios-app..."
echo ""

set +e
./gradlew :ios-app:clean :ios-app:linkDebugExecutableLinuxX64 --console=plain 2>&1 | tee /tmp/pl-disabled.log
BUILD_EXIT_CODE=$?
set -e

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "✓ Build FAILED as expected"
    echo ""
    echo "Key error messages:"
    echo "-------------------"
    grep -i "error\|unresolved\|undefined\|missing" /tmp/pl-disabled.log | grep -v "w:" | head -10 || echo "(no explicit error messages captured)"
else
    echo ""
    echo "❌ UNEXPECTED: Build succeeded (should have failed)"
fi

echo ""
echo ""

###########################################
# TEST 2: Partial Linkage ENABLED
###########################################
echo "=========================================="
echo "TEST 2: Partial Linkage ENABLED"
echo "=========================================="
echo ""

# Create build configuration with partial linkage enabled
cat > ios-app/build.gradle.kts << 'GRADLE_EOF'
plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=enable")
            }
        }
    }
    
    sourceSets {
        val linuxX64Main by getting {
            dependencies {
                implementation(project(":dep-lib"))
                implementation(project(":caller-lib"))
            }
        }
    }
}
GRADLE_EOF

echo "Configuration: freeCompilerArgs = [\"-Xpartial-linkage=enable\"]"
echo ""
echo "Expected Behavior:"
echo "  ✓ Build should SUCCEED"
echo "  ⚠️  Compiler warnings about unresolved symbols"
echo "  ✓ Executable produced (but will crash at runtime)"
echo ""
echo "Building ios-app..."
echo ""

set +e
./gradlew :ios-app:clean :ios-app:linkDebugExecutableLinuxX64 --console=plain 2>&1 | tee /tmp/pl-enabled.log
BUILD_EXIT_CODE=$?
set -e

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✓ Build SUCCEEDED as expected"
    echo ""
    echo "Checking for partial linkage warnings..."
    echo "----------------------------------------"
    if grep -qi "warning\|unresolved\|undefined" /tmp/pl-enabled.log; then
        echo "✓ Warnings found (partial linkage in action):"
        echo ""
        grep -i "warning\|unresolved\|undefined" /tmp/pl-enabled.log | grep -v "Kotlin/Native targets" | grep -v "Default Kotlin Hierarchy" | head -15
    else
        echo "⚠️  No explicit warnings found in build log"
        echo "    (Partial linkage may be working silently)"
    fi
    echo ""
    echo "Executable location:"
    ls -lh ios-app/build/bin/linuxX64/debugExecutable/ios-app.kexe 2>/dev/null || echo "  (executable not found at expected location)"
else
    echo ""
    echo "❌ UNEXPECTED: Build failed (should have succeeded with warnings)"
fi

echo ""
echo ""

###########################################
# SUMMARY
###########################################
echo "=========================================="
echo "SUMMARY"
echo "=========================================="
echo ""
echo "Partial Linkage Demo Results:"
echo ""
echo "WITH PARTIAL LINKAGE DISABLED (-Xpartial-linkage=disable):"
echo "  • Linker fails immediately when it detects missing symbols"
echo "  • Provides compile-time safety"
echo "  • No executable is produced"
echo "  • Recommended for production builds"
echo ""
echo "WITH PARTIAL LINKAGE ENABLED (-Xpartial-linkage=enable):"
echo "  • Linker succeeds with warnings about missing symbols"
echo "  • Allows build to complete despite incompatibilities"
echo "  • Executable is produced but may crash at runtime"
echo "  • Useful for gradual migration and testing"
echo ""
echo "Build logs saved to:"
echo "  /tmp/pl-disabled.log"
echo "  /tmp/pl-enabled.log"
echo ""
echo "Klib files saved to:"
echo "  /tmp/partial-linkage-demo/dep-lib-COMPATIBLE.klib"
echo "  /tmp/partial-linkage-demo/dep-lib-INCOMPATIBLE.klib"
echo ""

# Restore original files
echo "Restoring original files..."
git restore dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt 2>/dev/null || true
git restore ios-app/build.gradle.kts 2>/dev/null || true
echo "✓ Repository restored to original state"
echo ""
