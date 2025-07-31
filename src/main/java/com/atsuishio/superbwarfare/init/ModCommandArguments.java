package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.command.LowerCamelCaseEnumArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCommandArguments {

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Mod.MODID);

    public static final RegistryObject<LowerCamelCaseEnumArgument.Info> LOWER_CAMEL_CASE_ENUM =
            COMMAND_ARGUMENT_TYPES.register("lower_camel_case_enum", () -> ArgumentTypeInfos.registerByClass(LowerCamelCaseEnumArgument.class, new LowerCamelCaseEnumArgument.Info()));
}
