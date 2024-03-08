/**
 */
plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.5.11"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5" // Use fork until shadow has updated to Java 21
    id("maven-publish")
}

group = "de.erethon.factions"
version = "1.0-SNAPSHOT"
description = "A Factions plugin"

val papyrusVersion = "1.20.4-R0.1-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://erethon.de/repo")
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    implementation("de.erethon:bedrock:1.3.1")
    //implementation("de.erethon.lectern:Lectern:1.0-SNAPSHOT") //not ready yet
    compileOnly("de.erethon.aergia:Aergia:1.0.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.1.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.41")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = "Factions"
            version = "${project.version}"

            from(components["java"])
        }
    }
}

tasks {
    // Run reobfJar on build
    build {
        dependsOn(reobfJar)
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        options.compilerArgs.add("--enable-preview")
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
    runServer { // Automatically download & update Papyrus
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        // \/ Comment this out in case you are offline, will fail to start otherwise \/
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-reobf.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        jvmArgs("--enable-preview")
    }
    shadowJar {
        // Shade everything for now
        dependencies {
            include(dependency("de.erethon:bedrock:.*"))
            include(dependency("de.erethon.lectern:.*"))
        }
        relocate("de.erethon.bedrock", "de.erethon.factions.bedrock")
        relocate("de.erethon.lectern", "de.erethon.factions.lectern")
    }
}