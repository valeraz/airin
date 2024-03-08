package io.morfly.airin.module

import include
import io.morfly.airin.GradleModule
import io.morfly.airin.ModuleComponent
import io.morfly.airin.ModuleContext
import io.morfly.airin.feature.ArtifactMappingFeature
import io.morfly.pendant.starlark.glob
import io.morfly.pendant.starlark.kt_jvm_library
import io.morfly.pendant.starlark.lang.context.BUILD
import io.morfly.pendant.starlark.lang.context.bazel
import org.gradle.api.Project


abstract class KotlinJvmLibraryModule : ModuleComponent() {

    init {
        include<ArtifactMappingFeature>()
    }
    override fun canProcess(project: Project): Boolean =
        with(project.plugins) { hasPlugin("org.jetbrains.kotlin.jvm") }

    override fun ModuleContext.onInvoke(module: GradleModule) {
        val build =
            BUILD.bazel {
                _id = ID_BUILD

                load("//bazel/macros:module.bzl", "kt_jvm_library")

                kt_jvm_library {
                    _id = ID_BUILD_TARGET_CALL

                    name = module.name

                    for ((config, deps) in module.dependencies) {
                        config `=` deps.map { it.asBazelLabel().toString() }.distinct()
                    }
                }
            }
        generate(build)
    }

    companion object {
        const val ID_BUILD = "kt_jvm_library_build"
        const val ID_BUILD_TARGET_CALL = "kt_jvm_target_call"
    }
}