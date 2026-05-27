plugins {
    kotlin("multiplatform")
}

group = "com.example.cexport"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "cexport_demo"

                // Scenario 5: Re-export the :lib module's public API into the generated C header.
                // Without this, only :app's own public API appears in the header.
                export(project(":lib"))

                // Scenario 6 (uncomment to activate): whitelist which modules appear in the header.
                // When set, ONLY matching modules are exported — everything else is stripped from the header.
                // freeCompilerArgs += "-Xbinary=moduleIncludeOnly=lib"

                // Scenario 11 (uncomment to suppress header entirely):
                // freeCompilerArgs += "-Xbinary=cInterfaceMode=none"
            }
        }
    }
}
