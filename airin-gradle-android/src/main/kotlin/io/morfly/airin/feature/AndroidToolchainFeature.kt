/*
 * Copyright 2023 Pavlo Stavytskyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.morfly.airin.feature

import io.morfly.airin.FeatureContext
import io.morfly.airin.FeatureComponent
import io.morfly.airin.GradleModule
import io.morfly.airin.module.RootModule
import io.morfly.airin.property
import io.morfly.pendant.starlark.android_sdk_repository
import io.morfly.pendant.starlark.define_kt_toolchain
import io.morfly.pendant.starlark.http_archive
import io.morfly.pendant.starlark.kotlin_repositories
import io.morfly.pendant.starlark.kotlinc_version
import io.morfly.pendant.starlark.lang.context.BuildContext
import io.morfly.pendant.starlark.lang.context.WorkspaceContext
import io.morfly.pendant.starlark.lang.onContext
import io.morfly.pendant.starlark.lang.type.ListType
import io.morfly.pendant.starlark.lang.type.StringType
import io.morfly.pendant.starlark.maven_install
import io.morfly.pendant.starlark.register_toolchains
import io.morfly.pendant.starlark.rules_java_dependencies
import io.morfly.pendant.starlark.rules_java_toolchains
import io.morfly.pendant.starlark.rules_jvm_external_deps
import io.morfly.pendant.starlark.rules_jvm_external_setup
import org.gradle.api.Project

abstract class AndroidToolchainFeature : FeatureComponent() {

    var kotlinToolchainVersion by property("1.9")
    var kotlinJvmTarget by property("21")

    var androidApiVersion by property(34)
    var androidBuildToolsVersion by property("34.0.0")

    var kotlincVersion by property("1.9.22")
    var kotlincSha by property("88b39213506532c816ff56348c07bbeefe0c8d18943bffbad11063cf97cac3e6")

    var rulesJavaVersion by property("7.4.0")
    var rulesJavaSha by property("976ef08b49c929741f201790e59e3807c72ad81f428c8bc953cdbeff5fed15eb")

    var rulesJvmExternalVersion by property("4.5")
    var rulesJvmExternalSha by property("b17d7388feb9bfa7f2fa09031b32707df529f26c91ab9e5d909eb1676badd9a6")

    var rulesKotlinVersion by property("1.9.0")
    var rulesKotlinSha by property("5766f1e599acf551aa56f49dab9ab9108269b03c557496c54acaf41f98e2b8d6")

    var rulesAndroidVersion by property("0.1.1")
    var rulesAndroidSha by property("b75a673a66c157138ab53f4d8612a6e655d38b69bb14207c1a6675f0e10afa61")

    var allowedRepositories by property(
        mutableListOf(
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        )
    )

    override fun canProcess(project: Project): Boolean =
        project.plugins.hasPlugin("io.morfly.airin.android")

    override fun FeatureContext.onInvoke(module: GradleModule) {

        onContext<BuildContext>(id = RootModule.ID_BUILD) {
            load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

            val KOTLIN_VERSION by kotlinToolchainVersion
            val JAVA_VERSION by kotlinJvmTarget

            define_kt_toolchain(
                name = "kotlin_toolchain",
                api_version = KOTLIN_VERSION,
                jvm_target = JAVA_VERSION,
                language_version = KOTLIN_VERSION,
            )
        }

        onContext<BuildContext>(id = RootModule.ID_THIRD_PARTY_BUILD) {

        }

        onContext<WorkspaceContext>(id = RootModule.ID_WORKSPACE) {

            comment { "Java" }
            val RULES_JAVA_VERSION by rulesJavaVersion
            val RULES_JAVA_SHA by rulesJavaSha

            http_archive(
                name = "rules_java",
                sha256 = RULES_JAVA_SHA,
                urls = list[
                    "https://github.com/bazelbuild/rules_java/releases/download/{v}/rules_java-{v}.tar.gz".format { "v" `=` RULES_JAVA_VERSION }
                ],
            )
            load(
                "@rules_java//java:repositories.bzl",
                "rules_java_dependencies",
                "rules_java_toolchains"
            )
            rules_java_dependencies()
            rules_java_toolchains()

            comment { "Kotlin" }
            val RULES_KOTLIN_VERSION by rulesKotlinVersion
            val RULES_KOTLIN_SHA by rulesKotlinSha

            http_archive(
                name = "rules_kotlin",
                sha256 = RULES_KOTLIN_SHA,
                urls = list["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin_release.tgz".format(RULES_KOTLIN_VERSION)],
            )

            load(
                "@rules_kotlin//kotlin:repositories.bzl",
                "kotlin_repositories",
                "kotlinc_version",
            )

            kotlin_repositories(
                compiler_release = kotlinc_version(
                    release = kotlincVersion,
                    sha256 = kotlincSha,
                ),
            )

            register_toolchains("//:kotlin_toolchain")

            comment { "Android" }
            val RULES_ANDROID_VERSION by rulesAndroidVersion
            val RULES_ANDROID_SHA by rulesAndroidSha

            http_archive(
                name = "rules_android",
                sha256 = RULES_ANDROID_SHA,
                strip_prefix = "rules_android-%s" `%` RULES_ANDROID_VERSION,
                urls = list["https://github.com/bazelbuild/rules_android/archive/v%s.zip" `%` RULES_ANDROID_VERSION],
            )

            load("@rules_android//android:rules.bzl", "android_sdk_repository")

            android_sdk_repository(
                name = "androidsdk",
                api_level = androidApiVersion,
                build_tools_version = androidBuildToolsVersion,
            )

            _checkpoint(CHECKPOINT_BEFORE_JVM_EXTERNAL)

            comment { "JVM External" }
            val RULES_JVM_EXTERNAL_VERSION by rulesJvmExternalVersion
            val RULES_JVM_EXTERNAL_SHA by rulesJvmExternalSha

            http_archive(
                name = "rules_jvm_external",
                sha256 = RULES_JVM_EXTERNAL_SHA,
                strip_prefix = "rules_jvm_external-%s" `%` RULES_JVM_EXTERNAL_VERSION,
                url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" `%` RULES_JVM_EXTERNAL_VERSION,
            )

            load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
            rules_jvm_external_deps()

            load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")
            rules_jvm_external_setup()

            val MAVEN_ARTIFACTS = load(
                "//third_party:maven_dependencies.bzl", "MAVEN_ARTIFACTS"
            ).of<ListType<StringType>>()

            load("@rules_jvm_external//:defs.bzl", "maven_install")
            maven_install {
                _id = ID_MAVEN_INSTALL

                artifacts = MAVEN_ARTIFACTS
                repositories = allowedRepositories
                version_conflict_policy = "pinned"
            }
        }
    }

    companion object {
        const val ID_MAVEN_INSTALL = "android_tools_maven_install"
        const val CHECKPOINT_BEFORE_JVM_EXTERNAL = "checkpoint_before_jvm_external"
    }
}
