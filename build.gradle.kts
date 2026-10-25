plugins {
    `application`
    `java`
    `eclipse`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.15.0")
    testImplementation("org.testng:testng:7.7.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

application {
    mainClass = "prologparser.Main"
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.named<Test>("test") {
    // Use TestNG for unit tests.
    useTestNG()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "prologanalyzer.Main"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
