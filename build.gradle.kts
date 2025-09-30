plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5"
    id("maven-publish")
}

group = "de.erethon.factions"
version = "1.0-SNAPSHOT"
description = "A Factions plugin"

val papyrusVersion = "1.21.9-R0.1-SNAPSHOT"

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
    paperweight.devBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    //implementation("de.erethon.lectern:Lectern:1.0-SNAPSHOT") //not ready yet
    compileOnly("de.erethon.aergia:Aergia:1.0.1")
    compileOnly("de.erethon.hephaestus:Hephaestus:1.0.3-SNAPSHOT")
    compileOnly("de.erethon.hecate:Hecate:1.2-SNAPSHOT")
    compileOnly("de.erethon.tyche:Tyche:1.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.1.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") { isTransitive = false }
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.0-M2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0-M2")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    compileOnly("io.prometheus:simpleclient:0.16.0")
    compileOnly("org.popcraft:bolt-common:1.0.480")
    compileOnly("org.popcraft:bolt-bukkit:1.0.480")
    compileOnly("net.dv8tion:JDA:5.0.0-beta.20")
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
        options.release.set(21)
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
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-mojmap.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        runDirectory.set(file("C:\\Dev\\Erethon"))
    }
    shadowJar {
        // Shade everything for now
        dependencies {
            include(dependency("de.erethon.lectern:.*"))
        }
        relocate("de.erethon.lectern", "de.erethon.factions.lectern")
    }
}

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
