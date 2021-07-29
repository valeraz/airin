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
import template.root_build_template

class RootBuild : GradleStandaloneTemplateProvider() {

    override fun provide(target: Project, relativePath: String): List<StarlarkFile> = listOf(
        root_build_template(
            toolsDir = ToolsBuild.TOOLS_DIR,
            artifactsDir = ThirdPartyBuild.ARTIFACTS_DIR,
            javaToolchainTarget = ToolsBuild.JAVA_TOOLCHAIN_TARGET,
            kotlinToolchainTarget = ToolsBuild.KOTLIN_TOOLCHAIN_TARGET,
            roomRuntimeTarget = ThirdPartyBuild.ROOM_RUNTIME_TARGET,
            roomKtxTarget = ThirdPartyBuild.ROOM_KTX_TARGET,
            kotlinReflectTarget = ThirdPartyBuild.KOTLIN_REFLECT_TARGET,
            composePluginTarget = ToolsBuild.COMPOSE_PLUGIN_TARGET,
            roomPluginLibraryTarget = ToolsBuild.ROOM_PLUGIN_LIBRARY_TARGET,
            debugKeystoreFile = Workspace.DEBUG_KEYSTORE_FILE_NAME
        )
    )
}