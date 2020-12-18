import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version ("1.4.21")
    id("io.spring.dependency-management") version ("1.0.10.RELEASE")
    id("org.springframework.boot") version ("2.4.1")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
}

dependencyManagement {
    imports {
        mavenBom("io.r2dbc:r2dbc-bom:${properties["r2dbc_bom_version"]}")
    }
}

dependencies {
    implementation("org.springframework.fu:spring-fu-kofu:${properties["spring_kofu_version"]}")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.data:spring-data-r2dbc")
    implementation("io.r2dbc:r2dbc-h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage", "junit-vintage-engine")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("io.projectreactor:reactor-test")
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")
    implementation("io.jsonwebtoken:jjwt-impl:${properties["jsonwebtoken_version"]}")
    implementation("io.jsonwebtoken:jjwt-jackson:${properties["jsonwebtoken_version"]}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
