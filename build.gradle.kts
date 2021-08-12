plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.32"
}

version = "1.0-SNAPSHOT"
group = "org.twostack.bitcoin4j"

val kotlinVersion=project.properties.get("kotlinVersion")

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.twostack:bitcoin4j:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.4")


    testImplementation ("io.kotest:kotest-runner-junit5:4.6.1")
    testImplementation ("io.kotest:kotest-assertions-core:4.6.1")
    testImplementation ("io.kotest:kotest-property:4.6.1")
    testImplementation("ch.qos.logback:logback-classic:1.2.5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")


}

java {
    sourceCompatibility = JavaVersion.toVersion("1.8")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }


}

tasks.withType<Test>{
    useJUnitPlatform()
}
