import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.io.FileInputStream
import java.util.Properties

// AGP 9 provides Kotlin support built-in, so `kotlin.android` is intentionally
// NOT applied here (it errors out on AGP >= 9.0). Only the Compose compiler
// plugin is added on top.
plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	// Generates serializers for our NavKey subtypes; Navigation3 writes the
	// NavBackstack to saved instance state and crashes at launch without them.
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.devtools.ksp)
}

apply(from = rootProject.file("app/PXTasks.gradle.kts"))

android {
	namespace = "sh.anirudhgupta109.pxn"
	compileSdk = 37

	defaultConfig {
		applicationId = "sh.anirudhgupta109.pxn"
		minSdk = 37
		targetSdk = 37

		versionCode = getVersionCode()
		versionName = getVersionName()

		ndk {
			// Release ships arm64-v8a only. The x86 variants exist for the
			// emulator, which installs the debug variant - they would only
			// bloat a release APK.
			abiFilters += setOf("arm64-v8a")
		}
	}

	// Uses ReleaseKey.properties when present, else the debug keystore, so a
	// bare assembleRelease still produces an installable APK.
	val keystorePropertiesFile = rootProject.file("ReleaseKey.properties")
	var releaseSigning = signingConfigs.getByName("debug")

	try {
		val keystoreProperties = Properties()
		FileInputStream(keystorePropertiesFile).use { inputStream ->
			keystoreProperties.load(inputStream)
		}

		releaseSigning = signingConfigs.create("release") {
			keyAlias = keystoreProperties.getProperty("keyAlias")
			keyPassword = keystoreProperties.getProperty("keyPassword")
			storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
			storePassword = keystoreProperties.getProperty("storePassword")
		}
	} catch (_: Exception) {
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
			signingConfig = releaseSigning
		}
		debug {
			isDebuggable = true
			isMinifyEnabled = false
			isShrinkResources = false
			signingConfig = releaseSigning
		}
	}

	buildFeatures {
		compose = true
		buildConfig = true
		aidl = true
	}

	compileOptions {
		isCoreLibraryDesugaringEnabled = true
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	packaging {
		resources {
			excludes += setOf(
				"META-INF/AL2.0",
				"META-INF/LGPL2.1",
			)
		}
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
		// Laya decision: optin_strategy = global_compiler_optin_flag.
		// (ExperimentalMaterial3ExpressiveApi is Kotlin-internal on material3 1.4.0,
		// so only the public ExperimentalMaterial3Api can be opted into globally.)
		freeCompilerArgs.addAll(
			"-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
		)
	}
}

androidComponents {
	val apkName = "PixelXpert-Next.apk"

	onVariants { variant ->
		// createZip (app/PXTasks.gradle.kts) picks the release APK up by this
		// exact name, so assembleRelease itself has to emit it. Renaming only
		// inside a bespoke build task would leave the Magisk zip empty.
		val artifactDir = variant.artifacts.get(SingleArtifact.APK)
		val versionName = getVersionName()

		variant.outputs.forEach { output ->
			output.versionName.set(versionName)
		}

		tasks.named("preBuild").get().doLast {
			try {
				artifactDir.get().asFile.listFiles()
					?.filter { it.name == apkName }
					?.forEach { it.delete() }
			} catch (_: Throwable) {
			}
		}

		tasks.whenTaskAdded {
			if (name.lowercase() == "assemble${variant.name.lowercase()}") {
				doLast {
					artifactDir.get().asFile.listFiles()
						?.filter { it.extension == "apk" && it.name != apkName }
						?.forEach { it.renameTo(File(it.parent, apkName)) }
				}
			}
		}
	}
}

dependencies {
	implementation("com.android.tools.smali:smali-dexlib2:3.0.10")
	implementation("com.google.guava:guava:31.1-android")
	// Compose / Material 3 - pinned explicitly (no BOM; see gradle/libs.versions.toml)
	implementation(libs.compose.ui)
	implementation(libs.compose.foundation)
	implementation(libs.compose.animation)
	implementation(libs.compose.material3)
	implementation(libs.compose.material3.adaptive)
	implementation(libs.compose.material.icons.extended)
	implementation(libs.compose.ui.tooling.preview)
	debugImplementation(libs.compose.ui.tooling)

	implementation(libs.androidx.activity.compose)
	// Navigation3 (Laya decision), not navigation-compose/Nav2
	implementation(libs.androidx.navigation3.runtime)
	implementation(libs.androidx.navigation3.ui)
	// IllustrationPreference headers are Lottie animations (Laya: add_lottie_compose)
	implementation(libs.lottie.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.core.splashscreen)
	// WorkManager, for the AutoUpdate periodic check (Laya:
	// auto_update_scheduler -> add_workmanager).
	implementation(libs.androidx.work.runtime)
	// The NTP query itself (Laya: ntp_client=kronos_dep over hand-rolling SNTP).
	implementation(libs.ntpClient)
	// CallbackToFutureAdapter, which TimeSyncWorker's startWork() wraps the
	// sync in (Laya: worker_style=listenable_worker). work-runtime only ships
	// concurrent-futures as a *runtime* dependency, so the class would not be
	// on the compile classpath without this line.
	implementation(libs.androidx.concurrent.futures)

	implementation(libs.koin.core)
	implementation(libs.koin.android)
	implementation(libs.koin.compose)
	implementation(libs.koin.compose.viewmodel)

	// Xposed - libxposed API (102.0.0), plus the classic Xposed reflection
	// helpers (XposedHelpers/XposedBridge) that the hook DSL is written against.
	compileOnly(libs.libxposed.api)
	compileOnly(libs.xposed.legacy.api)
	implementation(libs.libxposed.service)

	// KSP processor that emits xposed/ModPacks.kt from the @BaseModPack
	// annotations. It has to be a sibling module: KSP cannot load a processor
	// from the module it is processing.
	ksp(project(":modPackProcessor"))

	// objenesis for the reflective instantiation in ObjectTools (also used by
	// StatusbarMods and KSUInjector), constraintlayout for the ConstraintSet
	// work in KeyguardMods, and appcompat because ShyLinearLayout extends
	// LinearLayoutCompat rather than the platform LinearLayout.
	implementation(libs.objenesis)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.appcompat)

	// Persian (Solar Hijri) date formatting in StringFormatter.
	implementation(libs.persian.date.time)

	implementation(libs.remotepreferences)
	implementation(libs.libsuCore)
	implementation(libs.libsuService)
	implementation(libs.libsuNIO)
	implementation(libs.gson)
	implementation(libs.apache.commons.text)
	// Changelog card on the Update page; the parser behind ui/update/MarkdownText.
	implementation(libs.jetbrains.markdown)

	coreLibraryDesugaring(libs.desugar.jdk.libs)
}

