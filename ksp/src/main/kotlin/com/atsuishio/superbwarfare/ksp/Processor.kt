package com.atsuishio.superbwarfare.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

private const val REGISTER_PACKET_ANNOTATION =
    "com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket"
private const val SERVER_PACKET_PAYLOAD =
    "com.atsuishio.superbwarfare.network.ServerPacketPayload"
private const val CLIENT_PACKET_PAYLOAD =
    "com.atsuishio.superbwarfare.network.ClientPacketPayload"


class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processor(environment.codeGenerator, environment.logger)
    }
}

class Processor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(REGISTER_PACKET_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
            .let(::generateRegistrations)

        return emptyList()
    }

    private fun generateRegistrations(declarations: List<KSClassDeclaration>) {
        val sourceFiles = mutableSetOf<KSFile>()
        val registrations = mutableListOf<Pair<String, String>>() // qualified name to playTo* function

        declarations.forEach { declaration ->
            declaration.containingFile?.let(sourceFiles::add)

            val qualifiedName = declaration.qualifiedName?.asString()
            if (qualifiedName == null) {
                logger.error("@RegisterPacket class has no qualified name", declaration)
                return@forEach
            }

            val function = when {
                declaration.isSubtypeOf(SERVER_PACKET_PAYLOAD) -> "playToServer"
                declaration.isSubtypeOf(CLIENT_PACKET_PAYLOAD) -> "playToClient"
                else -> {
                    logger.error(
                        "@RegisterPacket class $qualifiedName must extend ServerPacketPayload or ClientPacketPayload",
                        declaration
                    )
                    return@forEach
                }
            }

            registrations += qualifiedName to function
            logger.info("@RegisterPacket -> $function<$qualifiedName>")
        }

        if (registrations.isEmpty()) return

        registrations.sortBy { it.first }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *sourceFiles.toTypedArray()),
            packageName = "com.atsuishio.superbwarfare.network",
            fileName = "GeneratedPayloadRegistrations"
        )

        val content = buildString {
            appendLine("// 自动生成文件，请勿手动更改")
            appendLine()
            appendLine("package com.atsuishio.superbwarfare.network")
            appendLine()
            registrations.forEach { (name, _) -> appendLine("import $name") }
            appendLine()
            appendLine("@Suppress(\"unused\")")
            appendLine("internal fun registerGeneratedPayloads() {")
            registrations.forEach { (name, function) ->
                appendLine("    $function<${name.substringAfterLast('.')}>()")
            }
            appendLine("}")
        }

        file.bufferedWriter().use { writer -> writer.write(content) }
    }

    private fun KSClassDeclaration.isSubtypeOf(qualifiedName: String): Boolean {
        val queue = ArrayDeque<KSClassDeclaration>()
        val visited = mutableSetOf<String>()
        queue.addLast(this)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentName = current.qualifiedName?.asString() ?: continue
            if (!visited.add(currentName)) continue
            if (currentName == qualifiedName) return true

            current.superTypes
                .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
                .forEach(queue::addLast)
        }
        return false
    }
}
