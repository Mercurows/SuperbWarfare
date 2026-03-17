package com.atsuishio.superbwarfare.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter


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
        resolver.getSymbolsWithAnnotation("com.atsuishio.superbwarfare.ksp.annotation.GenerateMapCodec")
            .filterIsInstance<KSClassDeclaration>()
            .forEach(::processClass)

        return emptyList()
    }

    private fun processClass(classDeclaration: KSClassDeclaration) {
        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()

        logger.info("Processing class $packageName.$className")

        generateFancyExtension(classDeclaration, packageName, className)
    }

    private fun generateFancyExtension(
        classDeclaration: KSClassDeclaration,
        packageName: String,
        className: String
    ) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = "${className}GeneratedCodec"
        )

        val parameters = classDeclaration.primaryConstructor?.parameters.orEmpty()

        if (parameters.size !in 1..8) {
            logger.error(
                "@GenerateMapCodec class $className must have 1 to 8 primary constructor parameters!",
                classDeclaration
            )
            return
        }

        val parameterCodecs = parameters.joinToString(
            prefix = "\n                        ",
            separator = ",\n                        ",
            postfix = "\n                    "
        ) {
            generateCodec(it)
        }

        file.bufferedWriter().use { writer ->
            writer.write(
                """
                // 自动生成文件，请勿手动更改
                
                package $packageName

                import com.mojang.serialization.MapCodec
                import com.mojang.serialization.codecs.RecordCodecBuilder

                val $className.Companion.CODEC: MapCodec<$className> get() = RecordCodecBuilder.mapCodec { builder ->
                    builder.group($parameterCodecs).apply(builder, ::$className)
                }

                """.trimIndent()
            )
        }
    }

    private fun generateCodec(parameter: KSValueParameter): String {
        val name = parameter.name!!.asString()
        return when (val type = parameter.type.resolve().declaration.qualifiedName?.asString()) {
            "kotlin.Byte" -> """com.mojang.serialization.Codec.BYTE.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.Short" -> """com.mojang.serialization.Codec.SHORT.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.Int" -> """com.mojang.serialization.Codec.INT.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.Long" -> """com.mojang.serialization.Codec.LONG.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.Float" -> """com.mojang.serialization.Codec.FLOAT.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.Double" -> """com.mojang.serialization.Codec.DOUBLE.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.Boolean" -> """com.mojang.serialization.Codec.BOOL.fieldOf("$name").forGetter { it.$name }"""
            "kotlin.String" -> """com.mojang.serialization.Codec.STRING.fieldOf("$name").forGetter { it.$name }"""
            "net.minecraft.core.BlockPos" -> """com.mojang.serialization.Codec.LONG.fieldOf("$name").forGetter { it.$name.asLong() }"""
            "net.minecraft.world.item.crafting.Ingredient" -> """net.minecraft.world.item.crafting.Ingredient.CODEC.optionalFieldOf("$name", Ingredient.EMPTY).forGetter { it.$name }"""
            else -> error("don't know how to generate codec for $type!")
        }
    }
}