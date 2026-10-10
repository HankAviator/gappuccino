plugins { id("com.android.application") }
android {
    namespace = "io.github.hankaviator.gappuccino"
    compileSdk = 35
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "io.github.hankaviator.gappuccino"
        minSdk = 32
        targetSdk = 35
        versionCode = 11
        versionName = "0.1.10"
    }
    buildTypes {
        release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("com.google.android.material:material:1.14.0")
    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:dexkit:2.3.0")
    implementation("org.smali:dexlib2:2.5.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.guava:guava:32.0.1-jre")
}
