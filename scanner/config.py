SDK_BASE = "/Applications/DevEco-Studio.app/Contents/sdk/default"
LLVM_LIB = f"{SDK_BASE}/openharmony/native/llvm/lib"
OH_SYSROOT = f"{SDK_BASE}/openharmony/native/sysroot"
HMS_SYSROOT = f"{SDK_BASE}/hms/native/sysroot"
SYSROOT_PREFIXES = (OH_SYSROOT, HMS_SYSROOT)
INCLUDE_PREFIXES = tuple(f"{s}/usr/include/" for s in SYSROOT_PREFIXES)

LLVM_CXX_INCLUDE = f"{SDK_BASE}/openharmony/native/llvm/include/c++/v1"
LLVM_CLANG_INCLUDE = f"{SDK_BASE}/openharmony/native/llvm/lib/clang/15.0.4/include"

EXCLUDED_HEADERS = frozenset([
    "IPCKit/ipc_kit.h",
    "StoreKit/module_install.h",
    "CryptoArchitectureKit/crypto_architecture_kit.h",
    "hiai_foundation/hiai_aipp_param.h",
    "hiai_foundation/hiai_helper.h",
    "hiai_foundation/hiai_options.h",
    "hiai_foundation/hiai_single_op.h",
    "hiai_foundation/hiai_tensor.h",
    "tss2/tss2_common.h",
    "tss2/tss2_mu.h",
    "tss2/tss2_sys.h",
    "tss2/tss2_tcti.h",
    "tss2/tss2_tpm2_types.h",
    "tss2/tss2_tctildr.h",
    "TEEKit/tee/tee_defines.h",
    "TEEKit/tee/tee_apm_api.h",
    "TEEKit/tee/pthread_attr.h",
    "TEEKit/tee/tee_internal_se_api.h",
    "TEEKit/tee/tee_rtc_time_api.h",
    "TEEKit/tee/tee_sharemem_ops.h",
    "TEEKit/tee/tee_mem_mgmt_api.h",
    "TEEKit/tee/tee_log.h",
    "TEEKit/tee/tee_hw_ext_api.h",
    "TEEKit/tee/tee_crypto_api.h",
    "TEEKit/tee/tee_drv_client.h",
    "TEEKit/tee/tee_get_recoverymode.h",
    "TEEKit/tee/tee_dynamic_srv.h",
    "TEEKit/tee/dstb_api.h",
    "TEEKit/tee/tee_time_api.h",
    "TEEKit/tee/tee_sharemem.h",
    "TEEKit/tee/tee_core_api.h",
    "TEEKit/tee/tee_notify_set_priority.h",
    "TEEKit/tee/tee_ext_api.h",
    "TEEKit/tee/tee_service_public.h",
    "TEEKit/tee/tee_arith_api.h",
    "TEEKit/tee/tee_trusted_storage_api.h",
    "TEEKit/tee/rpmb_driver_rw_api.h",
    "TEEKit/tee/rpmb_fcntl.h",
    "TEEKit/tee/tee_tui_gp_api.h",
    "TEEKit/tee/tee_object_api.h",
    "TEEKit/tee/tee_agent.h",
    "TEEKit/tee/tee_crypto_hal.h",
    "TEEKit/tee/tee_property_api.h",
    "TEEKit/tee/tee_hw_ext_api_legacy.h",
])

KNOWN_SKIP_KINDS = frozenset([
    "INCLUSION_DIRECTIVE", "MACRO_EXPANSION", "MACRO_INSTANTIATION",
    "TYPE_REF", "UNEXPOSED_DECL",
])
CHILD_HANDLED_KINDS = frozenset([
    "FIELD_DECL", "ENUM_CONSTANT_DECL", "PARM_DECL",
])
