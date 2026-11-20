plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}
android {
  namespace = "kr.jion.day"
  compileSdk = 34
  defaultConfig {
    applicationId = "kr.jion.day"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }
  signingConfigs {
    create("jion") {
      storeFile = file("jion.jks")
      storePassword = "TlJjzhvWKl-9PCqb3jHw3ZBZ"
      keyAlias = "jionday"
      keyPassword = "TlJjzhvWKl-9PCqb3jHw3ZBZ"
    }
  }
  buildTypes {
    getByName("debug") { signingConfig = signingConfigs.getByName("jion") }
    getByName("release") {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("jion")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}
dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.work:work-runtime-ktx:2.9.1")
}
