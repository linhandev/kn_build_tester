#!/bin/bash

# Test script for Kotlin Native Partial Linkage functionality
# This script demonstrates the difference between enabled and disabled partial linkage

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "========================================"
echo "Kotlin Native Partial Linkage Demo"
echo "========================================"
echo ""

# Step 1: Build initial compatible version
echo "Step 1: Building initial compatible version..."
echo "----------------------------------------"
./gradlew clean --console=plain -q
./gradlew :dep-lib:linuxX64MainKlibrary :caller-lib:linuxX64MainKlibrary --console=plain -q
echo "✓ Initial klibs built successfully"
echo ""

# Backup everything
mkdir -p /tmp/klib-backup
cp -r dep-lib/build/classes/kotlin/linuxX64/main/klib /tmp/klib-backup/dep-lib-klib
cp -r caller-lib/build/classes/kotlin/linuxX64/main/klib /tmp/klib-backup/caller-lib-klib
cp dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt /tmp/klib-backup/DepLibrary-original.kt
echo "✓ Backed up compatible versions"
echo ""

# Step 2: Create incompatible version of dep-lib
echo "Step 2: Creating incompatible version of dep-lib..."
echo "----------------------------------------"
rm dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
cp DepLibrary-incompatible.kt.template dep-lib/src/commonMain/kotlin/com/example/dep/DepLibrary.kt
echo "✓ Switched to incompatible dep-lib source"
echo ""

# Build incompatible dep-lib in isolation
echo "Step 3: Building incompatible dep-lib..."
echo "----------------------------------------"
./gradlew :dep-lib:clean :dep-lib:linuxX64MainKlibrary --console=plain -q
cp dep-lib/build/classes/kotlin/linuxX64/main/klib/dep-lib.klib /tmp/klib-backup/dep-lib-incompatible.klib
echo "✓ Incompatible dep-lib.klib built"
echo ""

# Restore the caller-lib from backup (it was built against compatible dep-lib)
echo "Step 4: Restoring caller-lib (compiled against compatible dep-lib)..."
echo "----------------------------------------"
rm -rf caller-lib/build
mkdir -p caller-lib/build/classes/kotlin/linuxX64/main/klib
cp /tmp/klib-backup/caller-lib-klib/* caller-lib/build/classes/kotlin/linuxX64/main/klib/
echo "✓ Caller-lib restored (still references old API)"
echo ""
echo "IMPORTANT: caller-lib was compiled against the ORIGINAL dep-lib API"
echo "           but dep-lib.klib now contains the INCOMPATIBLE version"
echo "           This simulates a real-world scenario where libraries are updated"
echo ""

# Step 4: Try building ios-app WITHOUT partial linkage (should fail)
echo "=========================================="
echo "TEST 1: Building with Partial Linkage DISABLED"
echo "=========================================="
echo ""

# Modify build.gradle.kts to disable partial linkage
cat > ios-app/build.gradle.kts << 'EOF'
plugins {
    kotlin("multiplatform")
}

kotlin {
    // iOS targets (for macOS hosts)
    iosArm64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    iosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    // Linux target (for testing on Linux hosts)
    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation(project(":dep-lib"))
                implementation(project(":caller-lib"))
            }
        }
        
        val iosMain by creating {
            dependsOn(nativeMain)
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
EOF

echo "Configuration: -Xpartial-linkage=disable"
echo ""
echo "Expected: Build should FAIL with errors about missing symbols"
echo ""
echo "Building..."
if ./gradlew :ios-app:linkDebugExecutableLinuxX64 --console=plain 2>&1 | tee /tmp/build-disabled.log; then
    echo ""
    echo "❌ UNEXPECTED: Build succeeded (should have failed)"
    echo ""
else
    echo ""
    echo "✓ EXPECTED: Build failed as expected"
    echo ""
    echo "Error messages from the build:"
    echo "----------------------------------------"
    grep -E "(error|Error|ERROR|undefined|missing|unresolved|Unbound)" /tmp/build-disabled.log | head -20 || echo "No explicit error markers found, but build failed"
    echo "----------------------------------------"
fi
echo ""

# Step 5: Try building ios-app WITH partial linkage (should succeed with warnings)
echo "=========================================="
echo "TEST 2: Building with Partial Linkage ENABLED"
echo "=========================================="
echo ""

# Modify build.gradle.kts to enable partial linkage
cat > ios-app/build.gradle.kts << 'EOF'
plugins {
    kotlin("multiplatform")
}

kotlin {
    // iOS targets (for macOS hosts)
    iosArm64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=enable")
            }
        }
    }
    
    iosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=enable")
            }
        }
    }
    
    // Linux target (for testing on Linux hosts)
    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                freeCompilerArgs += listOf("-Xpartial-linkage=enable")
            }
        }
    }
    
    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation(project(":dep-lib"))
                implementation(project(":caller-lib"))
            }
        }
        
        val iosMain by creating {
            dependsOn(nativeMain)
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
EOF

echo "Configuration: -Xpartial-linkage=enable"
echo ""
echo "Expected: Build should SUCCEED with warnings about missing symbols"
echo ""
echo "Building..."
if ./gradlew :ios-app:clean :ios-app:linkDebugExecutableLinuxX64 --console=plain 2>&1 | tee /tmp/build-enabled.log; then
    echo ""
    echo "✓ EXPECTED: Build succeeded with partial linkage enabled"
    echo ""
    echo "Warning messages from the build:"
    echo "----------------------------------------"
    grep -E "(warning|Warning|WARN|w:|unresolved|Unbound)" /tmp/build-enabled.log | grep -v "Kotlin/Native targets cannot be built" | grep -v "Default Kotlin Hierarchy" | head -20 || echo "No explicit warnings found in build log"
    echo "----------------------------------------"
    echo ""
    echo "Note: If you run the executable, it will crash at runtime when trying to call missing functions"
else
    echo ""
    echo "❌ UNEXPECTED: Build failed (should have succeeded with warnings)"
fi
echo ""

echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "Incompatibilities injected into dep-lib:"
echo "  1. Removed function: greetUser(name: String)"
echo "  2. Changed signature: calculateSum(a, b) -> calculateSum(a, b, c)"
echo "  3. Removed class: UserData"
echo "  4. Removed function: processUserData(data)"
echo "  5. Removed function: ConfigHelper.getConfigValue()"
echo ""
echo "Behavior with partial linkage DISABLED:"
echo "  - Build FAILS immediately"
echo "  - Compiler reports errors about missing/incompatible symbols"
echo "  - No binary is produced"
echo ""
echo "Behavior with partial linkage ENABLED:"
echo "  - Build SUCCEEDS with warnings"
echo "  - Compiler logs warnings about unresolved symbols"
echo "  - Binary is produced but will crash at runtime if missing symbols are used"
echo ""
echo "Build logs saved to:"
echo "  /tmp/build-disabled.log (partial linkage disabled)"
echo "  /tmp/build-enabled.log (partial linkage enabled)"
echo ""
