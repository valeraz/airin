/*
 * Copyright 2021 Pavlo Stavytskyi
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

import org.gradle.api.Project
import org.morfly.airin.GradleStandaloneTemplateProvider
import org.morfly.airin.starlark.elements.StarlarkFile
import template.artifacts_build
import java.io.File


class Artifacts : GradleStandaloneTemplateProvider() {

    override fun provide(target: Project, relativePath: String): List<StarlarkFile> {
        addArtifactsToTheProject(target.projectDir.path)

        return listOf(
            artifacts_build(
                artifactsDir = ARTIFACTS_DIR,
                roomRuntimeTargetName = ROOM_RUNTIME_TARGET,
                roomKtxTargetName = ROOM_KTX_TARGET,
                kotlinReflectTargetName = KOTLIN_REFLECT_TARGET
            )
        )
    }

    private fun addArtifactsToTheProject(rootProjectDir: String) {
        File("$rootProjectDir/$ARTIFACTS_SOURCE_DIR").copyRecursively(
            target = File("$rootProjectDir/$ARTIFACTS_DIR"),
            overwrite = true
        )
    }

    companion object {
        const val ARTIFACTS_DIR = "artifacts"
        const val ARTIFACTS_SOURCE_DIR = "buildSrc/artifacts"

        const val ROOM_RUNTIME_TARGET = "room_runtime"
        const val ROOM_KTX_TARGET = "room_ktx"
        const val KOTLIN_REFLECT_TARGET = "kotlin_reflect"
    }
}