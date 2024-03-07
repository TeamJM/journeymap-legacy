import com.matthewprenger.cursegradle.CurseExtension
import com.modrinth.minotaur.ModrinthExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
  id("com.gtnewhorizons.gtnhconvention")
}

val configFile = file("project.properties")
val srcWebDir = "src/main/resources/assets/journeymap/web/"
val implSrc = "build/resources/java/journeymap/feature/impl/"

val config = configFile.reader(Charsets.UTF_8).use {
  val prop = Properties()
  prop.load(it)
  return@use prop
}

val forge_version = config.getProperty("forge_version")!!
val minecraft_version = config.getProperty("minecraft_version")!!
val group_id = config.getProperty("group_id")!!
val mod_id = config.getProperty("mod_id")!!
val fairPlay = config.getProperty("fairPlay")!!
val unlimited = config.getProperty("unlimited")!!
val noRadar = config.getProperty("noRadar")!!
val curse_release_type = config.getProperty("curse_release_type")!!
val dateFormat = config.getProperty("dateFormat")!!

val jmVersionComponents = project.version.toString().split(".")
val jm_major = jmVersionComponents.getOrNull(0) ?: "5"
val jm_minor = jmVersionComponents.getOrNull(1) ?: "1"
val jm_micro = jmVersionComponents.getOrNull(2) ?: "4"
val jm_patch = jmVersionComponents.getOrNull(3) ?: "p8"

minecraft {
  injectedTags.put("JM_VERSION", project.version.toString())
  injectedTags.put("MC_VERSION", minecraft_version)
  injectedTags.put("FORGE_VERSION", forge_version)
  injectedTags.put("MAJOR", jm_major)
  injectedTags.put("MINOR", jm_minor)
  injectedTags.put("MICRO", jm_micro)
  injectedTags.put("PATCH", jm_patch)
}

fun getDate(): String {
  val date = LocalDateTime.now()
  return date.format(DateTimeFormatter.ofPattern(dateFormat))
}

// Web processing

val yuiProcessorConfiguration =
  configurations.detachedConfiguration(dependencies.create("com.yahoo.platform.yui:yuicompressor:2.4.8"))

abstract class YuiMinifyTask : DefaultTask() {
  @get:Classpath
  abstract val yuiCompressor: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val inputFile: RegularFileProperty

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @get:Inject
  abstract val execOperations: ExecOperations

  @TaskAction
  fun minify() {
    val inputFile = this.inputFile.get().asFile.absoluteFile
    val outputFile = this.outputFile.get().asFile.absoluteFile
    val outputDir = outputFile.parentFile
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    execOperations.javaexec {
      classpath = yuiCompressor
      mainClass = "com.yahoo.platform.yui.compressor.Bootstrap"
      args = listOf("$inputFile", "-o", "$outputFile")
    }
  }
}

val processDocs by tasks.registering(Copy::class) {
  group = "JourneyMap"
  description = "Substitute templates in docs"

  from(file("doc/")) {
    include("*.html")
    expand(
      "version" to project.version.toString(),
      "mcversion" to minecraft_version,
      "forgeversion" to forge_version,
      "date" to getDate()
    )
  }
  into("build/doc")

  doFirst {
    file("build/doc").mkdirs()
  }
}

val minifyJs by tasks.registering(YuiMinifyTask::class) {
  group = "JourneyMap"
  description = "Minify webmap JS"

  yuiCompressor = yuiProcessorConfiguration
  inputFile = file("src/main/resources/assets/journeymap/web/js/journeymap.js")
  outputFile = file("build/minified/main/assets/journeymap/web/js/journeymap.min.js")
}

val minifyCss by tasks.registering(YuiMinifyTask::class) {
  group = "JourneyMap"
  description = "Minify webmap CSS"

  yuiCompressor = yuiProcessorConfiguration
  inputFile = file("src/main/resources/assets/journeymap/web/css/journeymap.css")
  outputFile = file("build/minified/main/assets/journeymap/web/css/journeymap.min.css")
}

// Resources processing

with(gtnhGradle.toolchainModule) {
  mcmodInfoProperties.put("version", project.version.toString())
  mcmodInfoProperties.put("mcversion", minecraft_version)
  mcmodInfoProperties.put("forgeversion", forge_version)
  // Keep as a template for the final jar builds
  mcmodInfoProperties.put("jmedition", "\${jm_edition}")
}

tasks.processResources {
  dependsOn(minifyJs, minifyCss)
  filesMatching("license.txt") {
    expand("version" to project.version.toString(), "mcversion" to minecraft_version, "date" to getDate())
  }
  exclude("assets/journeymap/web/js/journeymap.js", "assets/journeymap/web/css/journeymap.css")
  from("build/minified/main") {
    rename("(.*)\\.min\\.js", "$1.js")
    rename("(.*)\\.min\\.css", "$1.css")
  }
}

// -dev jar
tasks.jar {
  dependsOn(processDocs)
  archiveVersion = "${version}-${unlimited}".lowercase()
  exclude("journeymap/client/feature/impl/NoRadar.class")
  filesMatching("mcmod.info") {
    expand("jm_edition" to unlimited)
  }
}

// Main artifact
tasks.reobfJar {
  archiveVersion = "${version}-${unlimited}".lowercase()
}

tasks.sourcesJar {
  archiveVersion = "${version}-${unlimited}".lowercase()
}

// Additional fairPlay jar
val fairPlayJar by tasks.registering(Jar::class) {
  dependsOn(tasks.reobfJar)
  archiveVersion = "${version}-${fairPlay}".lowercase()
  from(tasks.reobfJar.map { zipTree(it.archiveFile.get().asFile) }) {
    exclude("META-INF/MANIFEST.MF")
    exclude("mcmod.info")
  }
  manifest.from(tasks.jar.get().manifest)
  exclude("journeymap/client/feature/impl/Unlimited.class")
  from(tasks.processResources) {
    include("mcmod.info")
    expand("jm_edition" to fairPlay)
  }
}

tasks.assemble {
  dependsOn(fairPlayJar)
}

// Add fairPlay to publishing
publishing.publications.withType<MavenPublication> {
  this.artifact(fairPlayJar) {
    classifier = "fairPlay"
  }
}

pluginManager.withPlugin("com.modrinth.minotaur") {
  val modrinth = project.extensions.getByType<ModrinthExtension>()
  modrinth.additionalFiles.add(fairPlayJar)
  modrinth.changelog = project.file("build/doc/changelog.html").readText(Charsets.UTF_8)
}

pluginManager.withPlugin("com.matthewprenger.cursegradle") {
  val curse = project.extensions.getByType<CurseExtension>()
  curse.curseProjects.forEach {
    it.releaseType = curse_release_type
    it.changelogType = "html"
    it.addArtifact(fairPlayJar.get().archiveFile)
    it.mainArtifact.changelog = project.file("build/doc/changelog.html")
    it.additionalArtifacts.forEach { it.changelog = project.file("build/doc/changelog.html") }
  }
}
