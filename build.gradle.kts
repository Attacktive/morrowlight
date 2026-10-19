import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.compose) apply false
}

tasks.wrapper {
	gradleVersion = "9.7.0"
	distributionType = Wrapper.DistributionType.BIN
	distributionSha256Sum = "84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae"
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
	languageVersion = JavaLanguageVersion.of(21)
	toolchainDownloadUrls.empty()
}
