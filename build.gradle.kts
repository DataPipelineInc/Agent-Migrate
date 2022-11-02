import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "com.datapipeline"
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
    implementation("com.oracle:ojdbc8:12.2.0.1")
    implementation("io.vertx:vertx-web-client:4.3.3")
    implementation("org.apache.kafka:kafka-clients:3.1.0")
    implementation(libs.bundles.klog)
    implementation(libs.bundles.konf)
    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.datapipeline.agent.ConfigMigrationKt")
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}

tasks.test {
    useJUnitPlatform()
    environment("NEW_SRCID" to 1)
    environment("NEW_MIGRATETOPICFORMAT" to "string")
    environment("NEW_HOST" to "192.168.0.14")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}