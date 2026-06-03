package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.LowerCamelCaseEnumArgument
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.registries.Registries
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModCommandArguments {

    @JvmStatic
    val COMMAND_ARGUMENT_TYPES: DeferredRegister<ArgumentTypeInfo<*, *>> =
        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Mod.MODID)

    @JvmStatic
    val LOWER_CAMEL_CASE_ENUM: DeferredHolder<ArgumentTypeInfo<*, *>, *> = COMMAND_ARGUMENT_TYPES.register(
        "lower_camel_case_enum",
        Supplier {
            ArgumentTypeInfos.registerByClass(
                LowerCamelCaseEnumArgument::class.java,
                LowerCamelCaseEnumArgument.Info()
            )
        })

}
