plugins {
    id("com.android.application")
}

android {
    namespace = "app.scopeshift.module"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.scopeshift.module"
        minSdk = 30
        targetSdk = 37
        versionCode = 5
        versionName = "1.0.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                output.versionName.map { version ->
                    "ScopeShift-$version-${variant.name}.apk"
                },
            )
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
}
