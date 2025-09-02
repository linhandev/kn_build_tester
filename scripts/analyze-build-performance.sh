#!/bin/bash

# KN Build Tester - Comprehensive Build Performance Analysis Script
# This script measures and analyzes Kotlin Native build performance

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(dirname "$0")/.."
BUILD_DIR="${PROJECT_ROOT}/build"
REPORTS_DIR="${BUILD_DIR}/reports"
TIMING_DIR="${REPORTS_DIR}/timing"
LLVM_STATS_DIR="${BUILD_DIR}/llvm-stats"
PERF_DUMPS_DIR="${BUILD_DIR}/perf-dumps"

# Default scale parameters
SCALE_CLASSES=${PROJECT_SCALE_CLASSES:-100}
SCALE_FUNCTIONS=${PROJECT_SCALE_FUNCTIONS:-50}
SCALE_PROTOBUF=${PROJECT_SCALE_PROTOBUF_MESSAGES:-20}

# Ensure required directories exist
mkdir -p "${TIMING_DIR}" "${LLVM_STATS_DIR}" "${PERF_DUMPS_DIR}"

echo -e "${BLUE}🔧 KN Build Tester - Performance Analysis${NC}"
echo -e "${BLUE}=========================================${NC}"
echo "Project Scale Configuration:"
echo "  - Classes: ${SCALE_CLASSES}"
echo "  - Functions: ${SCALE_FUNCTIONS}" 
echo "  - Protobuf Messages: ${SCALE_PROTOBUF}"
echo ""

# Function to log with timestamp
log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Function to measure execution time
measure_time() {
    local command="$1"
    local description="$2"
    
    log "${YELLOW}Starting: ${description}${NC}"
    local start_time=$(date +%s.%N)
    
    eval "$command"
    local exit_code=$?
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)
    
    if [ $exit_code -eq 0 ]; then
        log "${GREEN}✓ Completed: ${description} (${duration}s)${NC}"
    else
        log "${RED}✗ Failed: ${description} (${duration}s)${NC}"
        return $exit_code
    fi
    
    echo "$duration" > "${TIMING_DIR}/${description// /_}.time"
    return 0
}

# Function to clean build artifacts
clean_build() {
    log "${YELLOW}Cleaning build artifacts...${NC}"
    cd "${PROJECT_ROOT}"
    ./gradlew clean
    rm -rf "${LLVM_STATS_DIR}"/* "${PERF_DUMPS_DIR}"/* "${TIMING_DIR}"/* 2>/dev/null || true
}

# Function to configure project scale
configure_scale() {
    log "${YELLOW}Configuring project scale...${NC}"
    
    # Set scale parameters as system properties
    export GRADLE_OPTS="${GRADLE_OPTS} -Dproject.scale.classes=${SCALE_CLASSES}"
    export GRADLE_OPTS="${GRADLE_OPTS} -Dproject.scale.functions=${SCALE_FUNCTIONS}"
    export GRADLE_OPTS="${GRADLE_OPTS} -Dproject.scale.protobuf.messages=${SCALE_PROTOBUF}"
    
    echo "Scale configuration applied to GRADLE_OPTS"
}

# Function to build specific target with timing
build_target() {
    local target="$1"
    local task="$2"
    
    cd "${PROJECT_ROOT}"
    
    # Measure frontend compilation
    measure_time "./gradlew ${task} --info --profile" "Frontend_${target}_compilation"
    
    # Measure backend/native compilation if applicable
    if [[ "$task" == *"androidNativeArm64"* || "$task" == *"iosArm64"* || "$task" == *"native"* ]]; then
        measure_time "./gradlew ${target}:linkReleaseSharedAndroidNativeArm64 --info --profile" "Backend_${target}_Android_compilation"
        measure_time "./gradlew ${target}:linkReleaseSharedIosArm64 --info --profile" "Backend_${target}_iOS_compilation"
    fi
}

# Function to analyze LLVM stats
analyze_llvm_stats() {
    log "${YELLOW}Analyzing LLVM statistics...${NC}"
    
    local llvm_report="${REPORTS_DIR}/llvm-analysis.md"
    
    echo "# LLVM Performance Analysis" > "$llvm_report"
    echo "Generated at: $(date)" >> "$llvm_report"
    echo "" >> "$llvm_report"
    
    if [ -d "${LLVM_STATS_DIR}" ] && [ "$(ls -A "${LLVM_STATS_DIR}")" ]; then
        echo "## LLVM Statistics Files Found:" >> "$llvm_report"
        
        for stats_file in "${LLVM_STATS_DIR}"/*.json; do
            if [ -f "$stats_file" ]; then
                local basename=$(basename "$stats_file" .json)
                echo "### $basename" >> "$llvm_report"
                echo "\`\`\`json" >> "$llvm_report"
                head -50 "$stats_file" >> "$llvm_report" 2>/dev/null || echo "Error reading file" >> "$llvm_report"
                echo "\`\`\`" >> "$llvm_report"
                echo "" >> "$llvm_report"
            fi
        done
    else
        echo "No LLVM statistics files found." >> "$llvm_report"
    fi
    
    # Analyze timing passes if available
    echo "## LLVM Pass Timing Analysis" >> "$llvm_report"
    
    for perf_file in "${PERF_DUMPS_DIR}"/*.txt; do
        if [ -f "$perf_file" ]; then
            local basename=$(basename "$perf_file" .txt)
            echo "### $basename" >> "$llvm_report"
            
            # Extract LLVM-specific timing information
            if grep -q "LLVM" "$perf_file" 2>/dev/null; then
                echo "\`\`\`" >> "$llvm_report"
                grep -A 10 -B 2 "LLVM\|optimization\|bitcode" "$perf_file" 2>/dev/null || echo "No LLVM timing data found" >> "$llvm_report"
                echo "\`\`\`" >> "$llvm_report"
            else
                echo "No LLVM timing data available in this file." >> "$llvm_report"
            fi
            echo "" >> "$llvm_report"
        fi
    done
    
    log "${GREEN}✓ LLVM analysis saved to: $llvm_report${NC}"
}

# Function to generate comprehensive timing report
generate_timing_report() {
    log "${YELLOW}Generating comprehensive timing report...${NC}"
    
    local timing_report="${REPORTS_DIR}/build-timing-analysis.md"
    
    echo "# Kotlin Native Build Performance Report" > "$timing_report"
    echo "Generated at: $(date)" >> "$timing_report"
    echo "" >> "$timing_report"
    
    echo "## Project Configuration" >> "$timing_report"
    echo "- Scale Classes: $SCALE_CLASSES" >> "$timing_report"
    echo "- Scale Functions: $SCALE_FUNCTIONS" >> "$timing_report"
    echo "- Scale Protobuf Messages: $SCALE_PROTOBUF" >> "$timing_report"
    echo "" >> "$timing_report"
    
    echo "## Build Timing Results" >> "$timing_report"
    echo "" >> "$timing_report"
    
    if [ -d "${TIMING_DIR}" ] && [ "$(ls -A "${TIMING_DIR}")" ]; then
        echo "| Phase | Duration (seconds) |" >> "$timing_report"
        echo "|-------|-------------------|" >> "$timing_report"
        
        for time_file in "${TIMING_DIR}"/*.time; do
            if [ -f "$time_file" ]; then
                local phase=$(basename "$time_file" .time | tr '_' ' ')
                local duration=$(cat "$time_file")
                echo "| $phase | $duration |" >> "$timing_report"
            fi
        done
    else
        echo "No timing data available." >> "$timing_report"
    fi
    
    echo "" >> "$timing_report"
    
    # Include performance dump summaries
    echo "## Performance Dumps Summary" >> "$timing_report"
    echo "" >> "$timing_report"
    
    for perf_file in "${PERF_DUMPS_DIR}"/*.txt; do
        if [ -f "$perf_file" ]; then
            local basename=$(basename "$perf_file" .txt)
            echo "### $basename" >> "$timing_report"
            echo "\`\`\`" >> "$timing_report"
            head -20 "$perf_file" >> "$timing_report" 2>/dev/null || echo "Error reading performance dump" >> "$timing_report"
            echo "\`\`\`" >> "$timing_report"
            echo "" >> "$timing_report"
        fi
    done
    
    log "${GREEN}✓ Timing report saved to: $timing_report${NC}"
}

# Function to extract specific optimization phases
analyze_optimization_phases() {
    log "${YELLOW}Analyzing optimization phases...${NC}"
    
    local opt_report="${REPORTS_DIR}/optimization-phases-analysis.md"
    
    echo "# Optimization Phases Analysis" > "$opt_report"
    echo "Generated at: $(date)" >> "$opt_report"
    echo "" >> "$opt_report"
    
    echo "## ModuleBitcodeOptimization Analysis" >> "$opt_report"
    echo "" >> "$opt_report"
    
    for perf_file in "${PERF_DUMPS_DIR}"/*.txt; do
        if [ -f "$perf_file" ] && grep -q "ModuleBitcodeOptimization\|LTOBitcodeOptimization" "$perf_file" 2>/dev/null; then
            local basename=$(basename "$perf_file" .txt)
            echo "### $basename" >> "$opt_report"
            echo "\`\`\`" >> "$opt_report"
            grep -A 5 -B 2 "ModuleBitcodeOptimization\|LTOBitcodeOptimization" "$perf_file" 2>/dev/null || echo "No optimization data found" >> "$opt_report"
            echo "\`\`\`" >> "$opt_report"
            echo "" >> "$opt_report"
        fi
    done
    
    echo "## LLVM Pass Breakdown" >> "$opt_report"
    echo "" >> "$opt_report"
    
    # Look for LLVM pass timing in stats files
    for stats_file in "${LLVM_STATS_DIR}"/*.json; do
        if [ -f "$stats_file" ]; then
            local basename=$(basename "$stats_file" .json)
            echo "### $basename LLVM Passes" >> "$opt_report"
            
            # Extract timing information from JSON if available
            if command -v jq >/dev/null 2>&1; then
                echo "\`\`\`json" >> "$opt_report"
                jq '.["time-passes"] // .passes // .timing // empty' "$stats_file" 2>/dev/null >> "$opt_report" || echo "No pass timing data in JSON format" >> "$opt_report"
                echo "\`\`\`" >> "$opt_report"
            else
                echo "\`\`\`" >> "$opt_report"
                grep -i "pass\|time\|optimization" "$stats_file" 2>/dev/null | head -20 >> "$opt_report" || echo "No pass timing data available (jq not installed)" >> "$opt_report"
                echo "\`\`\`" >> "$opt_report"
            fi
            echo "" >> "$opt_report"
        fi
    done
    
    log "${GREEN}✓ Optimization phases analysis saved to: $opt_report${NC}"
}

# Main execution function
main() {
    local start_total=$(date +%s.%N)
    
    echo -e "${BLUE}Starting comprehensive build performance analysis...${NC}"
    
    # Step 1: Clean and configure
    clean_build
    configure_scale
    
    # Step 2: Build all targets with timing
    log "${YELLOW}Building all targets with performance monitoring...${NC}"
    
    measure_time "./gradlew data-layer:compileKotlinAndroidNativeArm64 --info --profile" "Data_Layer_Android_Frontend"
    measure_time "./gradlew data-layer:compileKotlinIosArm64 --info --profile" "Data_Layer_iOS_Frontend"
    measure_time "./gradlew business-logic:compileKotlinAndroidNativeArm64 --info --profile" "Business_Logic_Android_Frontend"  
    measure_time "./gradlew business-logic:compileKotlinIosArm64 --info --profile" "Business_Logic_iOS_Frontend"
    measure_time "./gradlew shared:compileKotlinAndroidNativeArm64 --info --profile" "Shared_Android_Frontend"
    measure_time "./gradlew shared:compileKotlinIosArm64 --info --profile" "Shared_iOS_Frontend"
    measure_time "./gradlew composeApp:compileKotlinAndroidNativeArm64 --info --profile" "Compose_App_Android_Frontend"
    measure_time "./gradlew composeApp:compileKotlinIosArm64 --info --profile" "Compose_App_iOS_Frontend"
    
    # Step 3: Build native binaries with comprehensive monitoring
    log "${YELLOW}Building native binaries with LLVM monitoring...${NC}"
    
    measure_time "./gradlew data-layer:linkReleaseSharedAndroidNativeArm64 --info --profile" "Data_Layer_Android_Backend"
    measure_time "./gradlew data-layer:linkReleaseSharedIosArm64 --info --profile" "Data_Layer_iOS_Backend"
    measure_time "./gradlew business-logic:linkReleaseSharedAndroidNativeArm64 --info --profile" "Business_Logic_Android_Backend"
    measure_time "./gradlew business-logic:linkReleaseSharedIosArm64 --info --profile" "Business_Logic_iOS_Backend"
    measure_time "./gradlew shared:linkReleaseSharedAndroidNativeArm64 --info --profile" "Shared_Android_Backend"
    measure_time "./gradlew shared:linkReleaseSharedIosArm64 --info --profile" "Shared_iOS_Backend"
    measure_time "./gradlew composeApp:linkReleaseExecutableAndroidNativeArm64 --info --profile" "Compose_App_Android_Backend"
    measure_time "./gradlew composeApp:linkReleaseExecutableIosArm64 --info --profile" "Compose_App_iOS_Backend"
    
    # Step 4: Generate reports
    log "${YELLOW}Generating analysis reports...${NC}"
    
    analyze_llvm_stats
    generate_timing_report
    analyze_optimization_phases
    
    # Step 5: Generate build report
    measure_time "./gradlew generateBuildReport" "Build_Report_Generation"
    
    local end_total=$(date +%s.%N)
    local total_duration=$(echo "$end_total - $start_total" | bc)
    
    echo "" >> "${REPORTS_DIR}/build-timing-analysis.md"
    echo "## Total Analysis Duration" >> "${REPORTS_DIR}/build-timing-analysis.md"
    echo "Total time: ${total_duration} seconds" >> "${REPORTS_DIR}/build-timing-analysis.md"
    
    echo ""
    echo -e "${GREEN}🎉 Build performance analysis completed!${NC}"
    echo -e "${GREEN}Total analysis time: ${total_duration} seconds${NC}"
    echo ""
    echo "Reports generated:"
    echo "  📊 Build Timing: ${REPORTS_DIR}/build-timing-analysis.md"
    echo "  🔧 LLVM Analysis: ${REPORTS_DIR}/llvm-analysis.md"
    echo "  ⚡ Optimization Phases: ${REPORTS_DIR}/optimization-phases-analysis.md"
    echo "  📈 Build Report: ${BUILD_DIR}/reports/build-timing/build-timing-report.md"
    echo ""
    echo "Performance data locations:"
    echo "  🕒 Timing data: ${TIMING_DIR}/"
    echo "  📊 LLVM stats: ${LLVM_STATS_DIR}/"
    echo "  📋 Performance dumps: ${PERF_DUMPS_DIR}/"
}

# Handle command line arguments
case "${1:-}" in
    "clean")
        clean_build
        ;;
    "configure")
        configure_scale
        ;;
    "analyze-llvm")
        analyze_llvm_stats
        ;;
    "timing-report")
        generate_timing_report
        ;;
    "optimization")
        analyze_optimization_phases
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  clean          Clean build artifacts"
        echo "  configure      Configure project scale"
        echo "  analyze-llvm   Analyze LLVM statistics only"
        echo "  timing-report  Generate timing report only"
        echo "  optimization   Analyze optimization phases only"
        echo "  help           Show this help message"
        echo ""
        echo "Environment variables:"
        echo "  PROJECT_SCALE_CLASSES         Number of classes to generate (default: 100)"
        echo "  PROJECT_SCALE_FUNCTIONS       Number of functions to generate (default: 50)"
        echo "  PROJECT_SCALE_PROTOBUF_MESSAGES Number of protobuf messages (default: 20)"
        ;;
    "")
        main
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac