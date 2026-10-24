plugins { id("com.android.application") }
android {
    namespace = "io.github.hankaviator.gappuccino"
    compileSdk = 37
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "io.github.hankaviator.gappuccino"
        minSdk = 32
        targetSdk = 37
        versionCode = 20
        versionName = "0.2.4"
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
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
    testImplementation("io.github.libxposed:api:102.0.0")
    implementation("org.luckypray:dexkit:2.3.0")
    implementation("org.smali:dexlib2:2.5.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.guava:guava:32.0.1-jre")
}
