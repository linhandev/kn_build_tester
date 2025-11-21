#!/bin/bash

# Comprehensive Partial Linkage Demo
# This script demonstrates the actual behavior of Kotlin Native partial linkage

set -e

PROJECT_DIR="/home/runner/work/kn_samples/kn_samples"
cd "$PROJECT_DIR"

echo "╔══════════════════════════════════════════════════════════╗"
echo "║   Kotlin Native Partial Linkage Demonstration           ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "This demo shows how partial linkage affects compilation when"
echo "binary dependencies change in incompatible ways."
echo ""

# Clean start
echo "🧹 Cleaning previous builds..."
./gradlew clean -q
echo ""

# Step 1: Build everything with COMPATIBLE version
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "STEP 1: Building with COMPATIBLE dependencies"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
git restore . 2>/dev/null || true
echo "Building dep-lib (compatible version)..."
./gradlew :dep-lib:linuxX64MainKlibrary -q
echo "✅ dep-lib built"
echo ""
echo "Building caller-lib (uses dep-lib API)..."
./gradlew :caller-lib:linuxX64MainKlibrary -q
echo "✅ caller-lib built"
echo ""
echo "Building ios-app..."
./gradlew :ios-app:linkDebugExecutableLinuxX64 -q
echo "✅ ios-app built"
echo ""
echo "Running application:"
echo "────────────────────"
./ios-app/build/bin/linuxX64/debugExecutable/ios-app.kexe
echo ""

# Step 2: Make dep-lib INCOMPATIBLE
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "STEP 2: Introducing BINARY INCOMPATIBILITIES"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Changing dep-lib to incompatible version..."
echo ""
echo "Changes being made:"
echo "  ❌ REMOVED: greetUser(name: String)"
echo "  ❌ CHANGED: calculateSum(a, b) → calculateSum(a, b, c)"
echo "  ❌ REMOVED: UserData class"
echo "  ❌ REMOVED: processUserData(data: UserData)"
echo "  ❌ REMOVED: ConfigHelper.getConfigValue()"
echo ""
cp DepLibrary-incompatible.kt.template dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
./gradlew :dep-lib:clean :dep-lib:linuxX64MainKlibrary -q
echo "✅ Incompatible dep-lib rebuilt"
echo ""
echo "NOTE: caller-lib source code still references the OLD API,"
echo "      but dep-lib.klib now contains the NEW incompatible API."
echo ""

# Step 3: Try with Partial Linkage DISABLED
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "STEP 3: Compiling with PARTIAL LINKAGE DISABLED"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Configuration: -Xpartial-linkage=disable"
echo ""

cat > caller-lib/build.gradle.kts << 'EOF'
plugins {
    kotlin("multiplatform")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    
    linuxX64 {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=disable")
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":dep-lib"))
            }
        }
    }
}
EOF

echo "Attempting to compile caller-lib..."
echo ""
if ./gradlew :caller-lib:clean :caller-lib:compileKotlinLinuxX64 --console=plain 2>&1 | tee /tmp/disabled-output.log | grep -A 20 "Task :caller-lib:compileKotlinLinuxX64"; then
    echo ""
    echo "❌ UNEXPECTED: Build succeeded"
else
    echo ""
    echo "✅ EXPECTED: Build FAILED"
    echo ""
    echo "Errors detected:"
    echo "────────────────"
    grep "^e:" /tmp/disabled-output.log | head -10 | sed 's/^/  /'
fi
echo ""

# Step 4: Try with Partial Linkage ENABLED
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "STEP 4: Compiling with PARTIAL LINKAGE ENABLED"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Configuration: -Xpartial-linkage=enable"
echo ""

cat > caller-lib/build.gradle.kts << 'EOF'
plugins {
    kotlin("multiplatform")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    
    linuxX64 {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=enable")
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":dep-lib"))
            }
        }
    }
}
EOF

echo "Attempting to compile caller-lib..."
echo ""
if ./gradlew :caller-lib:clean :caller-lib:compileKotlinLinuxX64 --console=plain 2>&1 | tee /tmp/enabled-output.log | grep -A 20 "Task :caller-lib:compileKotlinLinuxX64"; then
    echo ""
    echo "✅ EXPECTED: Build succeeded (with partial linkage)"
    echo ""
    echo "Note: With current Kotlin version, errors may still appear during"
    echo "      source compilation. Partial linkage is most effective with"
    echo "      pre-compiled binary klib distributions."
else
    echo ""
    echo "⚠️  Build failed (expected in source-level compilation)"
    echo ""
    echo "Errors shown as compiler tries to resolve references:"
    echo "──────────────────────────────────────────────────────"
    grep "^e:" /tmp/enabled-output.log | head -10 | sed 's/^/  /'
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Demonstrated 5 types of binary incompatibility:"
echo "   1. Removed function (greetUser)"
echo "   2. Changed function signature (calculateSum)"
echo "   3. Removed class (UserData)"
echo "   4. Removed dependent function (processUserData)"
echo "   5. Removed object method (ConfigHelper.getConfigValue)"
echo ""
echo "📊 Partial Linkage Behavior:"
echo ""
echo "   WITH -Xpartial-linkage=disable:"
echo "   • Compilation fails immediately with errors"
echo "   • Prevents binary incompatibility at build time"
echo "   • Recommended for production"
echo ""
echo "   WITH -Xpartial-linkage=enable:"
echo "   • Allows compilation to proceed (in binary klib scenarios)"
echo "   • Useful for gradual migration"
echo "   • May defer errors to runtime"
echo ""
echo "🔍 Key Insight:"
echo "   Partial linkage is most effective when working with pre-compiled"
echo "   binary klib distributions where source code isn't recompiled."
echo "   In Gradle projects that compile from source, the compiler still"
echo "   needs to resolve all references in the source code."
echo ""
echo "📝 Logs saved to:"
echo "   /tmp/disabled-output.log"
echo "   /tmp/enabled-output.log"
echo ""

# Cleanup
echo "🧹 Restoring project to original state..."
git restore . 2>/dev/null || true
echo "✅ Project restored"
echo ""
echo "Demo complete!"
