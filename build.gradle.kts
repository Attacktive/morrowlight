import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.compose) apply false
}

tasks.wrapper {
	gradleVersion = "9.8.1"
	distributionType = Wrapper.DistributionType.BIN
	distributionSha256Sum = "dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f"
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
	languageVersion = JavaLanguageVersion.of(21)
	toolchainDownloadUrls.empty()
}
