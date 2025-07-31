package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.command.LowerCamelCaseEnumArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCommandArguments {

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Mod.MODID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ?> LOWER_CAMEL_CASE_ENUM =
            COMMAND_ARGUMENT_TYPES.register("lower_camel_case_enum", () -> ArgumentTypeInfos.registerByClass(LowerCamelCaseEnumArgument.class, new LowerCamelCaseEnumArgument.Info()));
}
