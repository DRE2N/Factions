plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2" // Adds runServer and runMojangMappedServer tasks for testing
    id("com.gradleup.shadow") version "9.3.1"
    id("maven-publish")
}

group = "de.erethon.factions"
version = "1.0-SNAPSHOT"
description = "A Factions plugin"

val papyrusVersion = "26.3-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://repo.erethon.de/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // Erethon dependencies
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    compileOnly("de.erethon.aergia:Aergia:1.0.1")
    compileOnly("de.erethon.hephaestus:Hephaestus:26.3-SNAPSHOT")
    compileOnly("de.erethon.hecate:Hecate:1.3-SNAPSHOT")
    compileOnly("de.erethon.tyche:Tyche:1.0-SNAPSHOT")
    implementation("de.erethon.rhea:Rhea:0.1.0-SNAPSHOT")
    compileOnly("de.erethon.questsxl:QuestsXL:1.0.6-SNAPSHOT")

    // External dependencies
    compileOnly("org.jetbrains:annotations:23.1.0")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    compileOnly("io.prometheus:simpleclient:0.16.0")
    compileOnly("org.popcraft:bolt-common:1.0.480")
    compileOnly("org.popcraft:bolt-bukkit:1.0.480")
    compileOnly("net.dv8tion:JDA:5.0.0-beta.20")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.0-M2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0-M2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType(Test::class) {
    useJUnitPlatform()
}

tasks.register<Copy>("deployToSharedServer") {
    doNotTrackState("")
    group = "Erethon"
    description = "Used for deploying the plugin to the shared server. runServer will do this automatically." +
            "This task is only for manual deployment when running runServer from another plugin."
    dependsOn(":shadowJar")
    from(layout.buildDirectory.file("libs/Factions-$version-all.jar"))
    into("C:\\Dev\\Erethon\\plugins")
}

tasks {
    jar {
        manifest {
            attributes(
                    "paperweight-mappings-namespace" to "mojang"
            )
        }
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    runServer { // Automatically download & update Papyrus
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        // \/ Comment this out in case you are offline, will fail to start otherwise \/
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-26.3.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        runDirectory.set(file("C:\\Dev\\Erethon"))
    }
    shadowJar {
        // Shade everything for now
        dependencies {
            include(dependency("de.erethon.lectern:.*"))
            include(dependency("de.erethon.rhea:.*"))
        }
        relocate("de.erethon.lectern", "de.erethon.factions.lectern")
        relocate("de.erethon.rhea", "de.erethon.factions.libs.rhea")
    }
}

val rheaCompilerDir = file("../Rhea/compiler")
val rheaSource = layout.projectDirectory.dir("src/main/rhea")
val rheaOutput = layout.buildDirectory.dir("rhea")

val installRheaCompiler by tasks.registering(Exec::class) {
    inputs.files(file("../Rhea/compiler/package.json"), file("../Rhea/compiler/package-lock.json"))
    outputs.dir(file("../Rhea/compiler/node_modules"))
    workingDir = rheaCompilerDir
    commandLine("npm.cmd", "ci")
}

val installRheaBrowser by tasks.registering(Exec::class) {
    dependsOn(installRheaCompiler)
    workingDir = rheaCompilerDir
    commandLine("node", "node_modules/playwright/cli.js", "install", "chromium")
}

val compileRhea by tasks.registering(Exec::class) {
    dependsOn(installRheaBrowser)
    inputs.files(file("../Rhea/compiler/compile.mjs"), file("../Rhea/compiler/json-comments.mjs"))
    inputs.dir(rheaSource)
    outputs.dir(rheaOutput)
    workingDir = rheaCompilerDir
    commandLine("node", "compile.mjs", "--source", rheaSource.asFile.absolutePath,
        "--output", rheaOutput.get().asFile.absolutePath)
}

val verifyRhea by tasks.registering(Exec::class) {
    dependsOn(compileRhea)
    workingDir = rheaCompilerDir
    commandLine("node", "verify.mjs", rheaOutput.get().asFile.absolutePath, "7")
}

tasks.processResources {
    dependsOn(verifyRhea)
    from(rheaOutput.map { it.dir("templates") }) { into("rhea/templates") }
}

val rheaPackFragment by tasks.registering(Zip::class) {
    dependsOn(compileRhea)
    archiveFileName.set("factions-economy-ui-fragment.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(rheaOutput.map { it.dir("pack") })
}

val deployRheaPackFragment by tasks.registering(Copy::class) {
    dependsOn(rheaPackFragment)
    from(rheaPackFragment.flatMap { it.archiveFile })
    into("C:/Dev/Erethon/plugins/Hecate/pack-fragments")
}

tasks.named("deployToSharedServer") { dependsOn(deployRheaPackFragment) }

publishing {
    repositories {
        maven {
            name = "erethon"
            url = uri("https://repo.erethon.de/snapshots/")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = "Factions"
            version = "${project.version}"

            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}
