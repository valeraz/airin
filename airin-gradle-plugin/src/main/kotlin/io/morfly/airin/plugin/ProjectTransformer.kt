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

package io.morfly.airin.plugin

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType
import io.morfly.airin.ComponentConflictResolution
import io.morfly.airin.ComponentId
import io.morfly.airin.ConfigurationName
import io.morfly.airin.FeatureComponent
import io.morfly.airin.ModuleComponent
import io.morfly.airin.GradleModule
import io.morfly.airin.GradleModuleDecorator
import io.morfly.airin.MissingComponentResolution
import io.morfly.airin.dsl.AirinProperties
import io.morfly.airin.label.GradleLabel
import io.morfly.airin.label.Label
import io.morfly.airin.label.MavenCoordinates
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.internal.impldep.org.eclipse.jgit.lib.RepositoryCache.FileKey.lenient
import org.gradle.kotlin.dsl.getByType
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.MapProperty
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier

data class ModuleConfiguration(
    val module: GradleModule,
    val component: ModuleComponent?
)

interface ProjectTransformer {

    fun invoke(project: Project): ModuleConfiguration
}

class DefaultProjectTransformer(
    private val components: Map<ComponentId, ModuleComponent>,
    private val properties: AirinProperties,
    private val decorator: GradleModuleDecorator,
    private val artifactCollector: ArtifactDependencyCollector
) : ProjectTransformer {
    private val cache = mutableMapOf<ProjectPath, ModuleConfiguration>()

    override fun invoke(project: Project): ModuleConfiguration {
        cache[project.path]?.let { return it }

        val isSkipped = project.path in properties.skippedProjects

        val packageComponent =
            if (!isSkipped) project.pickPackageComponent(components, properties)
            else null
        val featureComponents =
            if (packageComponent == null) emptyList()
            else project.pickFeatureComponents(packageComponent)

        val versionsFromConfigs = mutableMapOf<String, String>()
        // Not sure whether this is the right set of filters, but it works so far
        project.configurations.filter { it.name.endsWith("DependenciesMetadata") && ! it.name.contains("test") } .forEach {
            versionsFromConfigs.putAll(getIdentifiersToVersions(it))
        }

        val module = GradleModule(
            name = project.name,
            isRoot = project.rootProject.path == project.path,
            label = GradleLabel(path = project.path, name = project.name),
            dirPath = project.projectDir.path,
            relativeDirPath = project.projectDir.relativeTo(project.rootDir).path,
            moduleComponentId = packageComponent?.id,
            featureComponentIds = featureComponents.map { it.id }.toSet(),
            originalDependencies = project.prepareDependencies(versionsFromConfigs)
        )
        with(decorator) {
            module.decorate(project)
        }

        val config = ModuleConfiguration(
            module = module,
            component = module.moduleComponentId?.let(components::getValue)
        )
        cache[project.path] = config
        return config
    }

protected fun getIdentifiersToVersions(configuration: Configuration): Map<String, String> {
    return configuration.incoming
        .artifactView {
            attributes { attribute(AndroidArtifacts.ARTIFACT_TYPE, ArtifactType.AAR_OR_JAR.type) }
            lenient(true)
            // Only resolve external dependencies! Without this, all project dependencies will get
            // _compiled_.
            componentFilter { id -> id is ModuleComponentIdentifier }
        }
        .artifacts
        .resolvedArtifacts
        // We _must_ map this here, can't defer to the task action because of
        // https://github.com/gradle/gradle/issues/20785
        .map { result ->
            result
                .asSequence()
                .map { it.id }
                .filterIsInstance<ModuleComponentArtifactIdentifier>()
                .associate { component ->
                    val componentId = component.componentIdentifier
                    val identifier = "${componentId.group}:${componentId.module}"
                    identifier to componentId.version
                }
        }.get()
    }

    private fun Project.pickPackageComponent(
        components: Map<ComponentId, ModuleComponent>,
        properties: AirinProperties
    ): ModuleComponent? {
        val suitableComponents = components.values
            .filter { !it.ignored }
            .filter { it.canProcess(this) }

        return when {
            suitableComponents.isEmpty() -> when (properties.onMissingComponent) {
                MissingComponentResolution.Fail -> error("No package component found for $path")
                MissingComponentResolution.Ignore -> null
            }

            suitableComponents.size > 1 -> when (properties.onComponentConflict) {
                ComponentConflictResolution.Fail -> error("Unable to pick suitable package component for $path out of ${suitableComponents.map { it.javaClass }}")
                ComponentConflictResolution.UsePriority -> suitableComponents.maxByOrNull { it.priority }
                ComponentConflictResolution.Ignore -> null
            }

            else -> suitableComponents.first()
        }
    }

    private fun Project.pickFeatureComponents(
        component: ModuleComponent
    ): List<FeatureComponent> = component.subcomponents.values
        .filterIsInstance<FeatureComponent>()
        .filter { !it.ignored }
        .filter { it.canProcess(this) }

    // libsFromCatalog: Map<String, String>
    private fun Project.prepareDependencies(versionsFromConfiguration: Map<String, String>): Map<ConfigurationName, List<Label>> =
        artifactCollector
            .invoke(this)
            .mapValues { (_, dependencies) ->
                dependencies.mapNotNull { dep ->
                    when (dep) {
                        is ExternalDependency -> {
                            // Some dependency versions are configured by the platform-tools project and are not available via the
                            // standard gradle api.
                            val version = if (dep.version != null) dep.version else versionsFromConfiguration["${dep.group}:${dep.name}"]
                            MavenCoordinates(dep.group, dep.name, version)
                        }
                        is ProjectDependency -> with(dep.dependencyProject) {
                            GradleLabel(path = path, name = name)
                        }

                        else -> null
                    }
                }
            }
}

fun ProjectTransformer.invoke(projects: Map<ProjectPath, Project>): Map<ProjectPath, ModuleConfiguration> =
    projects.mapValues { (_, project) -> invoke(project) }
