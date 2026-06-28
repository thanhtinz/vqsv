plugins { kotlin("jvm") version "1.9.24" }
group = "com.vqsv"; version = "1.0.0"
val gdxVersion = "1.12.1"
repositories { mavenCentral(); maven("https://oss.sonatype.org/content/repositories/releases/") }
dependencies {
  api("com.badlogicgames.gdx:gdx:$gdxVersion")
  api("com.squareup.okhttp3:okhttp:4.12.0")
  api("com.google.code.gson:gson:2.10.1")
}
kotlin { jvmToolchain(17) }
// Game assets live under src/main/resources/game/** (standard resources layout),
// so they are bundled into core.jar automatically. LibGDX's Gdx.files.internal()
// falls back to the classpath, so the desktop client finds them for both
// `:desktop:run` and the fat JAR. (Android loads them via its assets srcDir.)
