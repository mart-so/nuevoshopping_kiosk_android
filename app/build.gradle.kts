import java.io.File
import java.nio.file.Files
import java.time.LocalDate

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.generic.kiosk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.generic.kiosk"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

val repoRoot = rootProject.projectDir.canonicalFile
fun publishDatedApk(variant: String, source: File) {
    val dist = File(repoRoot, "dist").canonicalFile
    check(dist.parentFile == repoRoot) { "APK output directory must remain inside the repository: $dist" }
    check(source.isFile) { "Expected APK was not produced: $source" }
    dist.mkdirs()

    val datedNamePattern = Regex("^generic-kiosk-browser-${Regex.escape(variant)}-\\d{4}-\\d{2}-\\d{2}\\.apk$")
    dist.listFiles().orEmpty()
        .filter { it.isFile && datedNamePattern.matches(it.name) }
        .forEach { oldApk ->
            check(oldApk.canonicalFile.parentFile == dist && !Files.isSymbolicLink(oldApk.toPath())) {
                "Refusing to remove an APK outside dist or a symbolic link: $oldApk"
            }
            check(oldApk.delete()) { "Could not remove previous dated APK: $oldApk" }
        }

    val date = LocalDate.now().toString()
    val stableName = if (variant == "debug") "generic-kiosk-browser-debug.apk" else "generic-kiosk-browser-release-unsigned.apk"
    source.copyTo(File(dist, stableName), overwrite = true)
    source.copyTo(File(dist, "generic-kiosk-browser-$variant-$date.apk"), overwrite = true)
}

tasks.configureEach {
    when (name) {
        "assembleDebug" -> doLast {
            publishDatedApk("debug", layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile)
        }
        "assembleRelease" -> doLast {
            publishDatedApk("release-unsigned", layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk").get().asFile)
        }
    }
}
