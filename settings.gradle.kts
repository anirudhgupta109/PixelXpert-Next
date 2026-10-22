pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		maven { setUrl("https://jitpack.io") }
		// Official Xposed repository - hosts de.robv.android.xposed:api:82, the
		// reflection/hook helpers (XposedHelpers, XposedBridge) the hook DSL is
		// written against. Declared compileOnly, so it is never packaged into
		// the APK.
		maven { setUrl("https://api.xposed.info/") }
	}
}

rootProject.name = "PixelXpert-Next"

// :modPackProcessor is a sibling module because KSP cannot load a processor
// from the module it processes.
include(":app")
include(":modPackProcessor")
