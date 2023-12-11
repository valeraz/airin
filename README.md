# Airin 🎋
Airin is a tool for the automated migration of Gradle Android projects to Bazel.

- [Overview](#overview)
- [Gradle plugin](#gradle-plugin)
- [Module components](#module-components)
- [Feature components](#feature-components)
- [Shared components](#shared-components)
- [Properties](#properties)
- [Decorators](#decorators)

## Overview
To facilitate the migration of Android apps to Bazel, Airin provides a Gradle plugin that upon configuration, 
analyzes the Gradle project structure and replicates it with Bazel by generating the corresponding Bazel files.

To enable Starlark code generation in Kotlin, Airin is bundled with [Pendant](https://github.com/Morfly/pendant), 
an open-source declarative and type-safe Starlark code generator.

> You can learn more about Airin and the design behind it in a [blog post](https://morfly.medium.com/6dc79d298628) at Turo Engineering.

### Installation
Apply Airin Gradle plugin in your root `build.gradle.kts` file.
```kotlin
// root build.gradle.kts
plugins {
    id("io.morfly.airin.android") version "x.y.z"
}
```
### Configuration
Next, in the same `build.gradle.kts` file, use the `airin` extension to configure the plugin and adjust it specifically 
for your project.

```kotlin
// root build.gradle.kts
airin {
    targets += setOf(":app")

    register<AndroidLibraryModule> {
        include<JetpackComposeFeature>()
        include<HiltFeature>()
        include<ParcelizeFeature>()
    }
    register<JvmLibraryModule>()

    register<RootModule> {
        include<AndroidToolchainFeature>()
    }
}
```
A few things are happening in the script above:
- An`app` module and all its dependencies will be migrated to Bazel.
- Modules classified as Android library, JVM library and root module will be migrated to Bazel.
- If any Android library module optionally uses technologies like Jetpack Compose, Hilt or Parcelize, they will be reflected in Bazel too.

Continue reading the documentation to find more details about components and plugin configuration.

### Migration
Finally, after the plugin is configured, the migration to Bazel is triggered with a corresponding Gradle task.
```shell
./gradlew app:migrateToBazel --no-configure-on-demand
```

> Airin plugin needs to analyze the project dependency graph during the configuration phase. 
> Therefore, [configuration on demand](https://docs.gradle.org/current/userguide/multi_project_configuration_and_execution.html#sec:configuration_on_demand) must be disabled when running the migration. 

## Gradle plugin

### Configuration options
To configure Airin Gradle plugin use `airin` extension in the root `build.gradle.kts` file.

- `targets` - configure migration targets. A `migrateToBazel` task as assigned to each migration target and triggers the migration for all its dependencies as well as the root module of the project.
- `skippedProjects` - ignore these Gradle projects during the Bazel migration.
- `configurations` - specify allowed Gradle dependency configurations during the migration. All the rest dependencies will be ignored in Bazel.
- `register` - register a module component that targets a specific type of modules. See [module components](#module-components).
- `include` - include a feature component in a module component that targets specific build features included in the module. Must be applied under the specific module component. See [feature components](#feature-components).
- `onComponentConflict` - configure the behavior when Airin finds more then one module component that can migrate a module.
  - `Fail` - fail the build.
  - `UsePriority` - pick one with higher priority.
  - `Ignore` - ignore the module.
- `onMissingComponent` - configure the behavior when Airin can't find any module component to migrate a module.
  - `Fail` - fail the build.
  - `Ignore` - ignore the module.
- `decorateWith` - register a custom module decorator. See [decorators](#decorators).

### Gradle tasks
- `migrateToBazel` - registered for each migration target that is explicitly specified in `airin` plugin extension. Triggers the migration for the module, its direct and transitive dependencies and a root module.
- `migrateProjectToBazel` - registered for all dependencies of migration targets. Triggers migration only for this module.
- `migrateRootToBazelFor***` - registered for a root project and complements migration for specific migration target, where the `***` is a name of a migration target. E.g. `:migrateRootToBazelForApp`.

### Components
TODO

## Module components
Module component is responsible for generating Bazel files for specific types of modules.

Every module component is an abstract class that extends the ModuleComponent base class and implements 2 functions, `canProcess` and `onInvoke`.
```kotlin
abstract class AndroidLibraryModule : ModuleComponent {

  override fun canProcess(project: Project): Boolean {...}

  override fun ModuleContext.onInvoke(module: GradleModule) {...}
}
```

- `canProcess` is invoked during the Gradle Configuration phase and is aimed to filter Gradle modules to which this component is applicable. Only one module component can be selected for every module in the codebase.
- `onInvoke` is invoked during the Gradle Execution phase and contains the main logic of the component the purpose of which is to generate Bazel files for the module.

The easiest way to determine the module type is by examining its applied plugins. For example, an Android library module in Gradle typically relies upon the `com.android.library` plugin.

```kotlin
abstract class AndroidLibraryModule : ModuleComponent {

  override fun canProcess(project: Project): Boolean =
    project.plugins.hasPlugin("com.android.library")
}
```
### Generating Bazel files
The main responsibility of module components is generating Bazel files. 
To do this, Airin leverages Pendant, a Kotlin DSL that provides a declarative API for generating Starlark code. This is done in `onInvoke`.

```kotlin
abstract class AndroidLibraryModule : ModuleComponent {

  override fun ModuleContext.onInvoke(module: GradleModule) { 
    val file = BUILD.bazel {
        ...
    }
    generate(file)
  }
}
```
You can use various builders for different types of Bazel types.
```kotlin
val build = BUILD { ... }
val buildWithExt = BUILD.bazel { ... }
val workspace = WORKSPACE { ... }
val workspaceWithExt = WORKSPACE.bazel { ... }
val mavenDependencies = "maven_dependencies".bzl { ... }
```
To write the file in the file system, use `generate` call. By default, the file is generated in the same directory as the currently processed module. 
Additionally, you can use `relativePath` to specify a subdirectory for a generated file.
```kotlin
generate(build)
generate(mavenDependencies, relativePath = "third_party")
```

The actual content of a generated Starlark file is built using [Pendant](https://github.com/Morfly/pendant).

```kotlin
override fun ModuleContext.onInvoke(module: GradleModule) {
  val file = BUILD.bazel {
    load("@io_bazel_rules_kotlin//kotlin:android.bzl", "kt_android_library")
  
    kt_android_library {
      name = module.name
      srcs = glob("src/main/**/*.kt")
      custom_package = module.androidMetadata?.packageName
      manifest = "src/main/AndroidManifest.xml"
      resource_files = glob("src/main/res/**")
    }
  }

  generate(file)
}
```

> You can find an in-depth overview of Pendant on [GitHub](https://github.com/Morfly/pendant) and in the talk at [droidcon NYC 2022](https://www.droidcon.com/2022/09/29/advanced-techniques-for-building-kotlin-dsls/).

### Dependencies
A Bazel target can possess various types of dependencies, each represented by different function parameters.

```python
# BUILD.bazel
kt_android_library(
  ...
  deps = [...],
  exports = [...],
  plugins = [...],
)
```
A GradleModule instance provides dependencies mapped per configuration, represented with an argument name. To designate dependencies in the generated code, the `=` function (enclosed in backticks) is used to represent an argument passed to a function.

```python
kt_android_library {
  ...
  for ((config, deps) in module.dependencies) {
    config `=` deps.map { it.asBazelLabel().toString() }
  }
}
```
As a result, the following Starlark code is generated.

```python
# generated Bazel script
kt_android_library(
  ...
  plugins = [...],
  deps = [...],
  exports = [...],
)
```
## Feature components
Every feature component is an abstract class that extends the FeatureComponent base class and implements 2 functions, canProcess and onInvoke.

```kotlin
abstract class JetpackComposeFeature : FeatureComponent() {

  override fun canProcess(project: Project): Boolean {...}

  override fun FeatureContext.onInvoke(module: GradleModule) {...}
}
```
`canProcess` is invoked during the Gradle Configuration phase and is aimed to filter Gradle modules to which this component is applicable.

`onInvoke` is invoked during the Gradle Execution phase and contains the main logic of the component. Its purpose is to modify Bazel files generated by a related module component as well as manage the dependencies of the module.

### Dependency overrides
### Configuration overrides
### File modifiers
## Shared components
The purpose of shared components is to enable feature components to contribute into multiple module components.

There could be multiple types of shared components.
- **Shared module component** — receives contributions from every shared feature component, even if it is not directly included in it.
- **Shared feature component** — contributes to every shared module component.
- **Top-level feature component** — does not belong to any module component specifically and contributes to all module components, even non-shared ones.

- This is how they are declared in a Gradle plugin.

```kotlin
// root build.gradle.kts
airin {
  register<AndroidLibraryModule> {
    // shared feature component
    include<HiltFeature> { shared = true }
  }

  // shared module component
  register<RootModule> { shared = true }

  // top-level feature component
  include<AllPublicFeature>()
}
```
Let's analyze the example above.
- `HiltFeature` - configures Hilt for a Bazel module.
  - Contributes to `AndroidLibraryModule` because they are directly connected.
  - Contributes to `RootModule` because they are both shared.
- `AllPublicFeature` 
  - contributes to `AndroidLibraryModule` and `RootModule` because it's a top-level feature component.


## Properties
### Shared properties

## Decorators

## License

    Copyright 2023 Pavlo Stavytskyi.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
