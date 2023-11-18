plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "com.cout970"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.antlr:antlr4:4.13.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}

val runTests by tasks.registering(JavaExec::class) {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("nitrolang.MainKt")
    args = listOf(
        "--test",
        "--output",
        "src/main/resources/output/assembly.wat",
        "--execute",
        "src/main/nitro/compiler/main.nitro",
    )
}