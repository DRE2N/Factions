/**
 */
plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.5.3"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5" // Use fork until shadow has updated to Java 21
    id("maven-publish")
}

group = "de.erethon.factions"
version = "1.0-SNAPSHOT"
description = "A Factions plugin"

val mavenLocalPath = "${System.getProperties()["user.home"]}/.m2/repository/" + "${project.group}".replace(".","/") + "/${project.name}/${project.version}"
val correctJarName = "${project.name}-${project.version}.jar"
val correctAllJarName = "${project.name}-${project.version}-all.jar"
val wrongJarName = "${project.name}-${project.version}-dev.jar"
val wrongAllJarName = "${project.name}-${project.version}-dev-all.jar"
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
    implementation("de.erethon.lectern:Lectern:1.0-SNAPSHOT") //not ready yet
    compileOnly("de.erethon.aergia:Aergia:1.0.0-SNAPSHOT")
    implementation("org.jetbrains:annotations:23.1.0")
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
    jar {
        archiveFileName.set(correctJarName)
    }
    reobfJar {
        outputJar.set(layout.buildDirectory.file("libs/$correctJarName"))
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
        //archiveFileName.set(correctJarName)
        // Shade everything for now
        dependencies {
            include(dependency("de.erethon:bedrock:.*"))
            include(dependency("de.erethon.lectern:.*"))
        }
        relocate("de.erethon.bedrock", "de.erethon.factions.bedrock")
        relocate("de.erethon.lectern", "de.erethon.factions.lectern")
    }
    publishToMavenLocal {
        dependsOn(getTasksByName("deleteJarFiles", false))
    }
    /*register<Delete>("deleteJarFiles") {
        group = "publishing-fix"
        delete("$mavenLocalPath/$correctJarName")
        delete("$mavenLocalPath/$correctAllJarName")
    }
    register<Copy>("renameJarFiles") {
        group = "publishing-fix"
        mustRunAfter(publishToMavenLocal)
        from(mavenLocalPath)
        include("*.jar")
        into(mavenLocalPath)

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        rename(wrongJarName, correctJarName)
        rename(wrongAllJarName, correctAllJarName)
    }
    register("publishToMavenLocalFixed") { // paperweight appends a "-dev" to every jar file name, which u can't disable. So this is a workaround
        group = "publishing-fix"
        dependsOn(publishToMavenLocal)
        dependsOn(getTasksByName("renameJarFiles", false))
    }
    register<Copy>("deployToServer") {
        group = "publishing-fix"
        from(mavenLocalPath)
        include(correctJarName)
        into("${System.getProperties()["user.home"]}/OneDrive/Development/Server 1.19.4/plugins") // this path is individual for each user
    }*/
}