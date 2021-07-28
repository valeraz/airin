buildscript {
    val kotlinVersion by extra("1.5.10")

    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    id("org.morfly.airin")
}

airin {
    templates {
        register<AndroidModuleBuild>()
        register<RootBuild>()
        register<Workspace>()
    }

    artifacts {
        ignored = listOf(
            "com.google.dagger:dagger",
            "com.google.dagger:dagger-compiler",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android",
            "org.jetbrains.kotlin:kotlin-stdlib"
        )
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}