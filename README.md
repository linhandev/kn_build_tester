# Kotlin Native Build Efficiency Tester

A comprehensive Kotlin Multiplatform project designed to analyze and benchmark Kotlin Native build performance, specifically targeting compilation bottlenecks in large-scale applications.

## Overview

This project creates a realistic, large-scale KMP application with multiple modules to stress-test the Kotlin Native compiler and measure performance across different compilation phases.

## Architecture

### Modules

1. **data-layer** - Repository pattern implementation with protobuf code generation
2. **business-logic** - Complex business logic with algorithmic processing  
3. **shared** - Unified API facade exposing all functionality
4. **composeApp** - Compose Multiplatform application

### Key Features

- **Scalable Code Generation**: Configurable complexity via gradle properties
- **Protobuf Integration**: Uses @streem/pbandk for protocol buffer code generation
- **Multi-target Support**: Android, iOS, Desktop, and mingwX64 (substitute for ohosArm64)
- **Performance Monitoring**: Comprehensive build timing and LLVM optimization analysis
- **Real-world Complexity**: Mimics production application patterns

## Performance Analysis

### Kotlin Native Compiler Monitoring

The project is configured with advanced performance profiling:

```kotlin
compilerOptions.configure {
    freeCompilerArgs.add("-Xreport-perf")
    freeCompilerArgs.add("-Xdump-perf=${project.buildDir}/perf-dumps/...")
    freeCompilerArgs.add("-Xprofile-phases")
}
```

### LLVM Backend Analysis

Detailed LLVM pass timing and optimization analysis:

```kotlin
freeCompilerArgs += listOf(
    "-Xllvm-args=-time-passes",
    "-Xllvm-args=-stats",
    "-Xllvm-args=-print-stats-json=...",
    "-Xllvm-args=-print-module-scope"
)
```

## Quick Start

### Prerequisites

- JDK 17+
- Kotlin 2.2.10+
- Gradle 9.0+
- Native development tools for your platform

### Running the Analysis

1. **Configure project scale** (optional):
   ```bash
   export PROJECT_SCALE_CLASSES=200
   export PROJECT_SCALE_FUNCTIONS=100
   export PROJECT_SCALE_PROTOBUF_MESSAGES=30
   ```

2. **Run comprehensive build analysis**:
   ```bash
   ./scripts/analyze-build-performance.sh
   ```

3. **Run specific analysis**:
   ```bash
   # Clean build only
   ./scripts/analyze-build-performance.sh clean
   
   # LLVM analysis only
   ./scripts/analyze-build-performance.sh analyze-llvm
   
   # Timing report only
   ./scripts/analyze-build-performance.sh timing-report
   ```

### Manual Build Commands

```bash
# Build all targets with performance monitoring
./gradlew build --info --profile

# Build specific native target
./gradlew shared:linkReleaseSharedMingwX64

# Generate build reports
./gradlew generateBuildReport
```

## Configuration

### Project Scale Parameters

Set these in `gradle.properties` or as environment variables:

- `project.scale.classes` - Number of generated classes (default: 100)
- `project.scale.functions` - Number of generated functions (default: 50)  
- `project.scale.protobuf.messages` - Number of protobuf messages (default: 20)

### Performance Monitoring

The build generates several types of performance data:

- **Frontend/Backend timing**: `build/perf-dumps/`
- **LLVM statistics**: `build/llvm-stats/`
- **Build reports**: `build/reports/`
- **Gradle profiles**: `build/reports/profile/`

## Analysis Reports

After running the analysis script, you'll find:

1. **Build Timing Analysis** (`build/reports/build-timing-analysis.md`)
   - Frontend vs backend compilation times
   - Module-by-module breakdown
   - Total build duration analysis

2. **LLVM Analysis** (`build/reports/llvm-analysis.md`)
   - LLVM pass timing breakdown
   - Optimization statistics
   - Bitcode optimization phases

3. **Optimization Phases** (`build/reports/optimization-phases-analysis.md`)
   - ModuleBitcodeOptimization timing
   - LTOBitcodeOptimization analysis
   - Individual LLVM pass performance

## Key Metrics to Monitor

### Frontend Compilation
- Kotlin source processing time
- Type checking and inference
- IR generation

### Backend Compilation  
- IR to LLVM IR translation
- LLVM optimization passes
- Code generation and linking

### Critical Backend Phases
- **ModuleBitcodeOptimization**: Per-module LLVM optimizations
- **LTOBitcodeOptimization**: Link-time optimizations
- **Individual LLVM passes**: Function inlining, dead code elimination, etc.

## Troubleshooting

### Build Issues

1. **Out of memory**: Increase heap size in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx8g
   ```

2. **Missing native tools**: Install platform-specific development tools
3. **Protobuf issues**: Ensure protoc is installed and accessible

### Performance Issues

1. **Enable incremental compilation**:
   ```properties
   kotlin.incremental=true
   kotlin.incremental.multiplatform=true
   ```

2. **Use build cache**:
   ```properties
   org.gradle.caching=true
   ```

## Contributing

To extend the project for different analysis scenarios:

1. Modify scale parameters in `gradle.properties`
2. Add new modules following existing patterns
3. Update performance monitoring configuration
4. Extend the analysis script for new metrics

## License

This project is intended for performance analysis and benchmarking purposes.