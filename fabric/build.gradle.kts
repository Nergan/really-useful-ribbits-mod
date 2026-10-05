import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.9.2"
    kotlin("jvm") version "2.1.20"
}

fun prop(name: String): String = project.property(name) as String

version = prop("mod_version")
group = prop("mod_group_id")

base {
    archivesName.set("reallyusefulribbits-fabric-${prop("minecraft_version")}")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

loom {
    accessWidenerPath = file("src/main/resources/reallyusefulribbits.accesswidener")
}

kotlin {
    jvmToolchain(21)
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("src/main/kotlin"))
        kotlin.exclude("**/attach/ModAttachments.kt")
        kotlin.exclude("**/config/ServerConfig.kt")
        kotlin.exclude("**/event/ModSetup.kt")
        kotlin.exclude("**/client/ClientModEvents.kt")
        kotlin.exclude("**/network/ModNetworking.kt")
        kotlin.exclude("**/loader/neoforge/**")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

sourceSets.named("main") {
    java.srcDir(rootProject.file("src/main/java"))
    resources.srcDir(rootProject.file("src/main/resources"))
    resources.exclude("data/reallyusefulribbits/recipe/ribbit_guide.json")
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        content {
            includeGroupByRegex("software\\.bernie.*")
            includeGroup("com.eliotlash.mclib")
        }
    }
    maven {
        name = "Shedaniel"
        url = uri("https://maven.shedaniel.me/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
    modImplementation("maven.modrinth:ribbits:${prop("ribbits_fabric_maven")}")
    modImplementation("software.bernie.geckolib:geckolib-fabric-${prop("minecraft_version")}:${prop("geckolib_version")}")
    modImplementation("maven.modrinth:yungs-api:${prop("yungsapi_fabric_maven")}")
    modImplementation("maven.modrinth:patchouli:${prop("patchouli_fabric_version")}")
    // Loom не подхватывает JiJ из jar Patchouli. В релизе Fiber отдельно не кладём:
    // он уже лежит внутри Patchouli-*-FABRIC.jar.
    modRuntimeOnly("me.zeroeightsix:fiber:0.23.0-2")
    modRuntimeOnly("me.shedaniel.cloth:cloth-config-fabric:${prop("cloth_config_version")}")
    modRuntimeOnly("org.reflections:reflections:0.10.2")
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        filter { line -> line.replace("\${version}", version) }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
