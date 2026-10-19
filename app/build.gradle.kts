import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "xyz.attacktive.morrowlight"
	compileSdk {
		version = release(37) {
			minorApiLevel = 0
		}
	}

	defaultConfig {
		applicationId = "xyz.attacktive.morrowlight"
		minSdk = 26
		targetSdk = 37
		versionCode = 1
		versionName = "1.0.0"
	}

	buildFeatures {
		compose = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
		}
	}

	packaging {
		resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
	}
}

// AGP 9 supplies Kotlin for Android; do not apply org.jetbrains.kotlin.android.
kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_21)
	}
}

dependencies {
	implementation(project(":core"))
	implementation(libs.activity.compose)
	implementation(libs.lifecycle.runtime.compose)
	implementation(libs.lifecycle.viewmodel)
	implementation(libs.coroutines.android)
	implementation(libs.compose.ui)
	implementation(libs.compose.foundation)
	implementation(libs.compose.animation)
	implementation(libs.compose.preview)
	implementation(libs.material3)
	debugImplementation(libs.compose.tooling)
}
