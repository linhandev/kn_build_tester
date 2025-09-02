#!/bin/bash

# KN Build Tester - Demo Application Runner
# This script demonstrates the working KMP project for build efficiency analysis

set -e

PROJECT_ROOT="$(dirname "$0")/.."
cd "$PROJECT_ROOT"

echo "🚀 KN Build Tester Demo"
echo "======================="
echo ""

echo "📋 Project Structure:"
echo "  ├── data-layer/        # Repository pattern with complex algorithms"
echo "  ├── business-logic/    # Business processing with scalable complexity"
echo "  ├── shared/           # Unified facade API"
echo "  └── composeApp/       # Demo application"
echo ""

echo "🔧 Building project..."
./gradlew build --info --no-daemon > build-log.txt 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    echo ""
    echo "📊 Build Timing Analysis:"
    echo "Build log saved to: build-log.txt"
    
    # Extract key timing information
    echo ""
    echo "🕒 Compilation Times:"
    grep -E "Task.*compile.*took" build-log.txt | head -10 || echo "  Detailed timing info not available in this run"
    
    echo ""
    echo "📦 Generated Artifacts:"
    find build -name "*.jar" 2>/dev/null | head -5 || echo "  JAR files in build directories"
    
    echo ""
    echo "🎯 Key Features Demonstrated:"
    echo "  ✓ Multi-module KMP project structure"
    echo "  ✓ Complex algorithmic code for compilation stress testing"
    echo "  ✓ Scalable project configuration"
    echo "  ✓ Repository pattern with realistic business logic"
    echo "  ✓ Cross-module dependencies and API exports"
    
    echo ""
    echo "🧪 Running Demo Application..."
    ./gradlew composeApp:desktopRun --no-daemon
    
else
    echo "❌ Build failed. Check build-log.txt for details."
    echo ""
    echo "Last few lines of build log:"
    tail -20 build-log.txt
    exit 1
fi

echo ""
echo "✨ Demo completed successfully!"
echo ""
echo "📝 For build performance analysis:"
echo "  - Review build-log.txt for detailed timing"
echo "  - Use ./scripts/analyze-build-performance.sh for native analysis"
echo "  - Adjust scale parameters in gradle.properties"