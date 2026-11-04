import java.util.Properties

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

val pantryLocal = Properties()
val pantryLocalFile = file("local.properties")
if (pantryLocalFile.exists()) {
    pantryLocalFile.inputStream().use { pantryLocal.load(it) }
}
val externalBuild = pantryLocal.getProperty("pantry.buildDir")?.trim().orEmpty()
if (externalBuild.isNotEmpty()) {
    allprojects {
        layout.buildDirectory.set(file("$externalBuild/${project.name}"))
    }
}
