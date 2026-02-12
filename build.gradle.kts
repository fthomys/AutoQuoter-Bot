import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.lang.Closure
import org.gradle.kotlin.dsl.invoke

plugins {
    kotlin("jvm") version "2.3.10"
    java
    kotlin("plugin.serialization") version "2.3.10"
    application
    id("com.palantir.git-version") version "4.3.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("com.google.cloud.tools.jib") version "3.5.3"
}

application.mainClass.set("me.fabichan.autoquoter.Main")
group = "me.fabichan"
val gitVersion: Closure<String> by extra
version = gitVersion()

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")
    implementation("ch.qos.logback:logback-classic:1.5.29")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql:42.7.10")
    implementation("net.dv8tion:JDA:6.3.0")
    implementation("io.github.freya022:BotCommands:3.0.0-beta.8")
    implementation("org.flywaydb:flyway-core:12.0.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-jvm:2.6.1")
    implementation("club.minnced:jda-ktx:0.14.1")
    implementation("io.github.freya022:BotCommands-jda-ktx:3.0.0-beta.8")
    implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-common:2.6.1")
    implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-jvm:2.6.1")
    implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-runtime:2.4.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.3")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:12.0.1")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()

    archiveFileName.set("AutoQuoter.jar")

}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.release.set(21)
}

kotlin {
    jvmToolchain(21)
}

jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
    }
    container {
        mainClass = "me.fabichan.autoquoter.Main"
        jvmFlags = listOf("-XX:+UseContainerSupport")
        workingDirectory = "/app"
        environment = mapOf("BOT_DATA_DIR" to "/app")
    }
}