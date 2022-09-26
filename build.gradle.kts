import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://registry.datapipeline.com/nexus/content/groups/public/")
        credentials {
            username = "dev"
            password = "datapipeline123"
        }
    }
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    implementation("io.vertx:vertx-web-client:4.3.3")
    implementation("org.apache.kafka:kafka-clients:3.1.0")
    implementation(libs.bundles.klog)
    implementation(libs.bundles.konf)
    implementation(libs.bundles.jdbc)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}