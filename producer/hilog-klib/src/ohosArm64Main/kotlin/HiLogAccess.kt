// Minimal source so the ohosArm64 main compilation produces a klib that consumers can depend on.
// The HiLog C bindings come from the cinterop library (platform.PerformanceAnalysisKit.HiLog),
// pulled in transitively via this module's dependency on the cinterop klib.
@Suppress("unused")
object HiLogAccess
