#!/bin/bash
# KMP CAPI 自动化测试主脚本

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "🚀 KMP CAPI 自动化测试系统"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help          显示此帮助信息"
    echo "  -b, --build-only    仅编译KMP产物"
    echo "  --skip-build        跳过编译步骤"
    echo ""
    echo "示例:"
    echo "  $0                  # 完整自动化测试流程（编译+测试+报告）"
    echo "  $0 -b               # 仅编译"
    echo "  $0 --skip-build     # 跳过编译，直接运行测试"
    exit 0
}

# 检查Python环境
check_python() {
    echo "🔍 检查Python环境..."
    
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}❌ 未找到python3，请先安装Python 3${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Python环境正常${NC}"
}

# 编译KMP产物
build_kmp() {
    echo ""
    echo "🔨 步骤1/3: 编译KMP二进制产物"
    echo "========================================"
    echo -e "${YELLOW}提示: 编译通常需要 1-3 分钟（首次可能更久）${NC}"
    echo -e "${YELLOW}      如需跳过编译，请按 Ctrl+C 后运行: ./run_test.sh --skip-build${NC}"
    echo ""
    
    # 显示编译开始时间
    START_TIME=$(date +%s)
    
    # 执行编译
    echo "正在编译..."
    set +e  # 临时允许命令失败
    ./gradlew :composeApp:publishDebugBinariesToHarmonyApp
    BUILD_RESULT=$?
    set -e  # 恢复 exit on error
    
    END_TIME=$(date +%s)
    ELAPSED=$((END_TIME - START_TIME))
    
    if [ $BUILD_RESULT -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ KMP产物编译成功 (耗时: ${ELAPSED}秒)${NC}"
        return 0
    else
        echo ""
        echo -e "${RED}❌ 编译失败 (耗时: ${ELAPSED}秒)${NC}"
        echo ""
        echo -e "${YELLOW}💡 解决建议:${NC}"
        echo "  1. 使用 ./run_test.sh --skip-build 跳过编译直接测试"
        echo "  2. 在 DevEco Studio 中手动构建 harmonyApp"
        echo "  3. 检查 Gradle 配置和网络连接"
        echo "  4. 运行 ./gradlew clean 后重试"
        return 1
    fi
}

# 检查设备连接
check_device() {
    echo ""
    echo "📱 检查HarmonyOS设备连接..."
    
    if ! command -v hdc &> /dev/null; then
        echo -e "${YELLOW}⚠️  未找到hdc命令，请确认HarmonyOS工具链已安装${NC}"
        return 1
    fi
    
    DEVICES=$(hdc list targets)
    
    if [ -z "$DEVICES" ]; then
        echo -e "${RED}❌ 未检测到设备${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ 检测到设备:${NC}"
    echo "$DEVICES"
    return 0
}

# 部署应用到设备
deploy_app_to_device() {
    local step_num="2"
    if [ "$SKIP_BUILD" = "true" ]; then
        step_num="1"
    fi
    
    echo ""
    echo "📱 步骤${step_num}: 部署应用到设备"
    echo "========================================"
    
    # 尝试自动安装 hap 包
    HAP_PATH="harmonyApp/entry/build/default/outputs/default/entry-default-signed.hap"
    
    if [ -f "$HAP_PATH" ]; then
        echo "发现 hap 包，尝试自动安装..."
        if hdc install -r "$HAP_PATH" 2>/dev/null; then
            echo -e "${GREEN}✅ 应用安装成功${NC}"
            
            # 尝试启动应用
            echo "正在启动应用..."
            hdc shell aa start -a EntryAbility -b com.example.harmonyapp 2>/dev/null || true
            
            echo -e "${GREEN}✅ 应用已启动${NC}"
            echo ""
            echo -e "${YELLOW}等待3秒，确保应用完全启动...${NC}"
            sleep 3
            return 0
        else
            echo -e "${YELLOW}⚠️  自动安装失败${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  未找到 hap 包: $HAP_PATH${NC}"
    fi
    
    # 如果自动安装失败，提示手动操作
    echo ""
    echo -e "${YELLOW}请在 DevEco Studio 中手动部署应用：${NC}"
    echo "  1. 打开 harmonyApp 项目"
    echo "  2. 点击运行按钮（或按 Shift+F10）"
    echo "  3. 等待应用成功启动到设备"
    echo ""
    
    read -p "按回车键继续（确认应用已部署并启动）..." 
    echo ""
    echo -e "${GREEN}✅ 继续执行测试${NC}"
    return 0
}

# 自动化测试 - 通过Python脚本自动执行所有模块测试
run_auto_test() {
    local step_num="3"
    if [ "$SKIP_BUILD" = "true" ]; then
        step_num="2"
    fi
    
    echo ""
    echo "🤖 步骤${step_num}: 自动化测试执行"
    echo "========================================"
    
    # 创建test_reports目录
    mkdir -p test_reports
    
    echo "将自动测试以下9个模块:"
    echo "  • 模块1: RDB模块"
    echo "  • 模块2: OH_CommonEvent公共事件模块"
    echo "  • 模块3: HuksKeyApi密钥管理模块"
    echo "  • 模块4: NetConnection网络连接模块"
    echo "  • 模块5: HiAppEvent用户打点模块"
    echo "  • 模块6: HiLog日志模块"
    echo "  • 模块7: Drawing模块"
    echo "  • 失败场景模块"
    echo "  • 版本检测机制模块"
    echo ""
    
    # 调用Python脚本（自动逐模块测试）
    python3 autotest.py
}



# 主流程
main() {
    MODE="full"
    SKIP_BUILD="false"
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                ;;
            -b|--build-only)
                MODE="build"
                shift
                ;;
            --skip-build)
                SKIP_BUILD="true"
                shift
                ;;
            *)
                echo "未知选项: $1"
                show_help
                ;;
        esac
    done
    
    # 检查环境
    check_python
    
    # 如果是 full 模式且不跳过编译，给出友好提示
    if [ "$MODE" = "full" ] && [ "$SKIP_BUILD" != "true" ]; then
        echo ""
        echo -e "${YELLOW}⚡ 快速提示:${NC}"
        echo "  如果 KMP 产物已经编译好，可以使用 --skip-build 选项跳过编译步骤"
        echo "  例如: ./run_test.sh --skip-build"
        echo ""
        sleep 2
    fi
    
    case $MODE in
        build)
            build_kmp
            ;;
        full)
            # 完整自动化测试流程
            if [ "$SKIP_BUILD" != "true" ]; then
                build_kmp || exit 1
            else
                echo ""
                echo "⏩ 跳过编译步骤，使用已有的KMP产物"
            fi
            
            check_device || {
                echo -e "${YELLOW}⚠️  设备检查失败，但继续尝试运行测试...${NC}"
            }
            
            deploy_app_to_device
            
            # 运行自动化测试
            run_auto_test
            ;;
    esac
    
    echo ""
    echo "========================================"
    echo -e "${GREEN}🎉 自动化测试完成！${NC}"
    echo "========================================"
    echo ""
    echo "测试报告位置:"
    echo "  - HTML: test_reports/test_report.html"
    echo "  - CSV:  test_reports/test_report.csv"
    echo ""
}

# 执行主函数
main "$@"
