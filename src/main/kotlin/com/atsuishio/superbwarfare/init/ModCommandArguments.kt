package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.LowerCamelCaseEnumArgument
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.registries.Registries
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.RegistryObject

object ModCommandArguments {

    val COMMAND_ARGUMENT_TYPES: DeferredRegister<ArgumentTypeInfo<*, *>> =
        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Mod.MODID)

    @JvmField
    val LOWER_CAMEL_CASE_ENUM: RegistryObject<LowerCamelCaseEnumArgument.Info> =
        COMMAND_ARGUMENT_TYPES.register("lower_camel_case_enum") {
            ArgumentTypeInfos.registerByClass(
                LowerCamelCaseEnumArgument::class.java,
                LowerCamelCaseEnumArgument.Info()
            )
        }
}
