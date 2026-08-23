allprojects {
    configurations.all {
        resolutionStrategy {
            // android
            force("androidx.lifecycle:lifecycle-livedata-core:2.6.1")
            force("androidx.vectordrawable:vectordrawable-animated:1.0.0")
            force("androidx.vectordrawable:vectordrawable:1.0.0")
            force("androidx.appcompat:appcompat:1.3.1")
            force("androidx.appcompat:appcompat-resources:1.3.1")
            force("androidx.compose.ui:ui:${libs.versions.android.compose.get()}")
            force("androidx.compose.foundation:foundation:${libs.versions.android.compose.get()}")
            force("androidx.compose.foundation:foundation-layout:${libs.versions.android.compose.get()}")
            force("androidx.compose.material:material:${libs.versions.android.compose.get()}")
            force("androidx.compose.runtime:runtime:${libs.versions.android.compose.get()}")
            force("androidx.compose.runtime:runtime-android:${libs.versions.android.compose.get()}")
            force("androidx.compose.runtime:runtime-runtime-saveable:${libs.versions.android.compose.get()}")
            force("androidx.compose.animation:animation:${libs.versions.android.compose.get()}")

            // Core & Kotlin
            force(libs.atomic.fu.get())
            force(libs.kotlinx.coroutines.get())
            force(libs.kotlinx.datetime.get())
            force(libs.kotlinx.serialization.json.get())

            // Ohos
            force(libs.ohos.ffi.annotation.get())
            force(libs.ohos.ffi.library.get())
            force(libs.compose.ohos.annotation.get())

            // iOS
            force(libs.disko.get())

            // Others
            force(libs.spi.asProvider().get())
        }
    }
}
