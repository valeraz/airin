package io.morfly.airin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.NodeChild
import org.gradle.api.Project
import java.io.File

private val xmlParser = XmlSlurper()

open class AndroidProjectDecorator : GradleProjectDecorator {

    override fun GradleProject.decorate(target: Project) {
        with(target.plugins) {
            if (!hasPlugin("com.android.application") && !hasPlugin("com.android.library")) {
                return
            }
        }

        androidMetadata = AndroidMetadata(
            applicationId = target.applicationId,
            packageName = target.namespace ?: target.manifestPackageName,
            composeEnabled = target.composeEnabled,
            minSdkVersion = target.minSdkVersion,
            compileSdkVersion = target.compileSdkVersion,
            targetSdkVersion = target.targetSdkVersion,
            versionCode = target.versionCode,
            versionName = target.versionName
        )
    }
}

val Project.namespace: String?
    get() = extensions.findByType(CommonExtension::class.java)?.namespace

val Project.composeEnabled: Boolean
    get() = extensions.findByType(CommonExtension::class.java)?.buildFeatures?.compose ?: false

val Project.applicationId: String?
    get() = extensions.findByType(ApplicationExtension::class.java)?.defaultConfig?.applicationId

val Project.minSdkVersion: Int?
    get() = extensions.findByType(CommonExtension::class.java)?.defaultConfig?.minSdk

val Project.targetSdkVersion: Int?
    get() = extensions.findByType(ApplicationExtension::class.java)?.defaultConfig?.targetSdk

val Project.compileSdkVersion: Int?
    get() = extensions.findByType(CommonExtension::class.java)?.compileSdk

val Project.versionCode: Int?
    get() = extensions.findByType(ApplicationExtension::class.java)?.defaultConfig?.versionCode

val Project.versionName: String?
    get() = extensions.findByType(ApplicationExtension::class.java)?.defaultConfig?.versionName

val Project.androidManifestFile: File?
    get() = extensions.findByType(BaseExtension::class.java)
        ?.sourceSets
        ?.map { it.manifest.srcFile }
        ?.firstOrNull(File::exists)

val Project.manifestPackageName: String?
    get() = androidManifestFile
        ?.let(xmlParser::parse)
        ?.list()
        ?.filterIsInstance<NodeChild>()
        ?.firstOrNull { it.name() == "manifest" }
        ?.attributes()?.get("package")?.toString()
