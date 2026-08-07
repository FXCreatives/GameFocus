plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.android.library") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
