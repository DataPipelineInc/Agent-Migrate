rootProject.name = "agent-migrate"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("klogging", "io.github.microutils:kotlin-logging:2.1.21")
            library("slf4j-api", "org.slf4j:slf4j-api:1.7.36")
            library("slf4j-log4j", "org.slf4j:slf4j-log4j12:1.7.36")
            library("log4j", "log4j:log4j:1.2.17")
            bundle("klog", listOf("klogging", "slf4j-api", "slf4j-log4j", "log4j"))

            library("konf-base", "com.uchuhimo:konf:1.1.2")
            library("konf-core", "com.uchuhimo:konf-core:1.1.2")
            library("konf-yaml", "com.uchuhimo:konf-yaml:1.1.2")
            bundle("konf", listOf("konf-base", "konf-core", "konf-yaml"))
        }
    }
}

