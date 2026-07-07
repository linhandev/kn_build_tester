// Minimal source so the ohosArm64 main compilation produces a klib that consumers can depend on.
// The HMS extension Kit C bindings come from the cinterop libraries (platform.*Kit.*), pulled in
// transitively via this module's cinterops + the POM dependency on ohos-capi (for the 5 hms defs
// that depend on ohos-side defs like NeuralNetworkRuntime/vulkan/gles3).
@Suppress("unused")
object HmsAccess
