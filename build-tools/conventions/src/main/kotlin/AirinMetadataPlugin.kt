import io.morfly.airin.buildtools.AirinConventionPlugin

class AirinMetadataPlugin : AirinConventionPlugin({})

object AirinMetadata {
    const val JVM_TOOLCHAIN_VERSION = 21
    const val KOTLIN_LANGUAGE_VERSION = "1.9"

    const val ARTIFACT_GROUP = "io.morfly.airin"
}