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

// Bundle the original-game assets (assets/game/**) into the core jar as classpath
// resources. LibGDX's Gdx.files.internal() falls back to the classpath, so the
// desktop client picks them up for both `:desktop:run` and the fat JAR without an
// extra copy step. (Android loads them via its own assets srcDir — see android/.)
sourceSets {
  main {
    resources.srcDir("assets")
  }
}
