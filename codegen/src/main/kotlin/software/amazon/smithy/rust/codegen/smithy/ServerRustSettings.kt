package software.amazon.smithy.rust.codegen.smithy

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ShapeId

/**
 * [ServerRustSettings] and [ServerCodegenConfig] classes.
 *
 * These classes are entirely analogous to [ClientRustSettings] and [ClientCodegenConfig]. Refer to the documentation
 * for those.
 *
 * These classes have to live in the `codegen` subproject because they are referenced in [ServerCodegenContext],
 * which is used in common generators to both client and server.
 */

/**
 * Settings used by [RustCodegenServerPlugin].
 */
data class ServerRustSettings(
    override val service: ShapeId,
    override val moduleName: String,
    override val moduleVersion: String,
    override val moduleAuthors: List<String>,
    override val moduleDescription: String?,
    override val moduleRepository: String?,
    override val runtimeConfig: RuntimeConfig,
    override val coreCodegenConfig: ServerCoreCodegenConfig,
    override val license: String?,
    override val examplesUri: String? = null,
    override val customizationConfig: ObjectNode? = null
): RustSettings(
    service, moduleName, moduleVersion, moduleAuthors, moduleDescription, moduleRepository, runtimeConfig, coreCodegenConfig, license
) {
    companion object {
        fun from(model: Model, config: ObjectNode): ServerRustSettings {
            val rustSettings = RustSettings.from(model, config)
            val codegenSettings = config.getObjectMember(CODEGEN_SETTINGS)
            val coreCodegenConfig = CoreCodegenConfig.fromNode(codegenSettings)
            return ServerRustSettings(
                service = rustSettings.service,
                moduleName = rustSettings.moduleName,
                moduleVersion = rustSettings.moduleVersion,
                moduleAuthors = rustSettings.moduleAuthors,
                moduleDescription = rustSettings.moduleDescription,
                moduleRepository = rustSettings.moduleRepository,
                runtimeConfig = rustSettings.runtimeConfig,
                coreCodegenConfig = ServerCoreCodegenConfig.fromCodegenConfigAndNode(coreCodegenConfig, config),
                license = rustSettings.license,
                examplesUri = rustSettings.examplesUri,
                customizationConfig = rustSettings.customizationConfig
            )
        }
    }
}

data class ServerCoreCodegenConfig(
    override val formatTimeoutSeconds: Int = 20,
    override val debugMode: Boolean = false,
    override val eventStreamAllowList: Set<String> = emptySet(),
): CoreCodegenConfig(
    formatTimeoutSeconds, debugMode, eventStreamAllowList
) {
    companion object {
        // Note `node` is unused, because at the moment `ServerCodegenConfig` has the same properties as
        // `CodegenConfig`. In the future, the server will have server-specific codegen options just like the client
        // does.
        fun fromCodegenConfigAndNode(coreCodegenConfig: CoreCodegenConfig, node: ObjectNode) =
            ServerCoreCodegenConfig(
                formatTimeoutSeconds = coreCodegenConfig.formatTimeoutSeconds,
                debugMode = coreCodegenConfig.debugMode,
                eventStreamAllowList = coreCodegenConfig.eventStreamAllowList,
            )
    }
}