// Root build: shared repositories only. Each module declares its own plugins so
// :core and :desktop build without needing the Android Gradle plugin / SDK.
allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases/")
    }
}
