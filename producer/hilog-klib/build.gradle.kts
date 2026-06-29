plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

base.archivesName.set("hilog-klib")

// Auto-register one cinterop per .def in nativeInterop/ohosArm64/.
// Each def's `depends = A B C` line is parsed: for each depended name, pass cinterop
//   -library <unpacked depended klib path>  (cinterop only resolves depends via -library,
//   not -libraryPath which is just a search path)
// and set -Xshort-module-name so the produced klib is findable by that name.
// Task ordering: cinterop<Name>OhosArm64 dependsOn cinterop<Dep>OhosArm64 for each dep.
// NOTE: defs have enableUndefinedApiProtection stripped (see memory kn-cinterop-stub-runtime-abi-incompat).

val defDir = file("$projectDir/nativeInterop/ohosArm64")
val cinteropOutDir = layout.buildDirectory.dir("classes/kotlin/ohosArm64/main/cinterop")
// Sysroots are checked into the repo (sysroot/ at repo root, parent of producer/).
val repoRoot = rootProject.projectDir.parentFile!!
val hmsSysroot = file("$repoRoot/sysroot/sysroot-hms-aarch64-6.0.2.640-02")
// HMS sysroot (additionalTargetSysRoot.ohos in cpf 0.4 konan.properties) — HarmonyOS-SDK-Only Kits
// (AppGalleryKit/CANNKit/DeviceSecurityKit/dataaugmentation/xengine etc.) whose headers the main
// ohos sysroot lacks. cinterop doesn't auto-add it, so add -I here.
val hmsInclude = file("$hmsSysroot/usr/include")

// Parse each def: name -> depends (by short_name as written in the def).
data class DefInfo(val name: String, val depends: List<String>)
val defs: List<DefInfo> = defDir.listFiles { f -> f.extension == "def" }!!
    .map { f ->
        val name = f.nameWithoutExtension
        val deps = f.readLines()
            .firstOrNull { it.startsWith("depends") }
            ?.substringAfter("=")?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
            ?: emptyList()
        DefInfo(name, deps)
    }
val defByName = defs.associateBy { it.name }

// Transitive closure of depends, restricted to defs in our set (external depends like posix/
// linux/gles3 are dist builtins resolved from the default search path, not -library).
fun transitiveClosure(name: String): List<String> {
    val seen = linkedSetOf<String>()
    fun visit(n: String) {
        defByName[n]?.depends?.forEach { d ->
            if (d in defByName && seen.add(d)) visit(d)
        }
    }
    visit(name)
    return seen.toList()
}

kotlin {
    ohosArm64 {
        compilations {
            val main by getting {
                cinterops {
                    defs.forEach { d ->
                        // cinterop name: use def name as-is (KMP accepts these identifiers).
                        create(d.name) {
                            defFile("$defDir/${d.name}.def")
                            extraOpts("-Xshort-module-name", d.name)
                            // HMS sysroot (additionalTargetSysRoot in cpf 0.4 konan.properties)
                            // holds the HarmonyOS-SDK-Only Kit headers (AppGalleryKit/CANNKit/
                            // DeviceSecurityKit/dataaugmentation/xengine etc.) that the main ohos
                            // sysroot lacks. cinterop doesn't auto-add it, so add -I here.
                            extraOpts("-compiler-option", "-I$hmsInclude")
                            // Load the full transitive closure of depends as -library (cinterop's
                            // -library only loads direct deps; a depended klib's own transitive
                            // depends aren't auto-resolved from -libraryPath, so pass them all).
                            transitiveClosure(d.name).forEach { dep ->
                                val depDir = cinteropOutDir.map { it.file("hilog-klib-cinterop-${dep}").asFile.absolutePath }.get()
                                extraOpts("-library", depDir)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Task ordering: each cinterop depends on its depended cinterops' tasks (only for deps in our
// set; external deps like posix are dist builtins with no task). KMP task name capitalizes the
// cinterop name's first letter: cinterop<Name>OhosArm64.
fun cinteropTaskName(name: String) = "cinterop${name.replaceFirstChar { it.uppercase() }}OhosArm64"
defs.forEach { d ->
    transitiveClosure(d.name).forEach { dep ->
        tasks.findByName(cinteropTaskName(d.name))?.dependsOn(cinteropTaskName(dep))
    }
}

publishing {
    publications {
        // KMP metadata + target publications share artifactId hilog-klib (consumer depends on
        // com.example:hilog-klib). The .pom overwrite warning is benign: .module carries per-target
        // variants consumers resolve on. Splitting artifactIds breaks .module variant resolution.
        withType<MavenPublication> {
            artifactId = "hilog-klib"
        }
    }
    // Publish to the repo-local Maven repo (kn_sample/m2, gitignored) instead of ~/.m2, so the
    // published klibs are inspectable alongside the source. Use `./gradlew :hilog-klib:publish`.
    repositories {
        maven { url = uri(rootProject.projectDir.parentFile.resolve("m2")) }
    }
}
