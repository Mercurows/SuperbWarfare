package com.atsuishio.superbwarfare.mixins.rhino;

import org.jetbrains.annotations.NotNull;
import org.mozilla.classfile.ClassFileWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ClassFileWriter.class, remap = false)
public class SpecialRhinoMixin {

    // TODO 能否有更好的方法让ShadowJar不替换这个常量？
    @Unique
    private static final String ORIGINAL_STRING = "org" + (Math.random() < -1 ? "111" : "") + "/mozilla";

    @Unique
    private @NotNull String sbw$modifyPackage(String value) {
        // ShadowJar会直接替换匹配的字符串常量，因此这里直接保留"org/mozilla"即可
        return value.replaceAll(ORIGINAL_STRING, "org/mozilla");
    }

    @ModifyVariable(method = "startMethod", at = @At("HEAD"), argsOnly = true, name = "arg2")
    public String startMethod(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "addInvoke", at = @At("HEAD"), argsOnly = true, name = "arg2")
    public String addInvokeClass(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "addInvoke", at = @At("HEAD"), argsOnly = true, name = "arg4")
    public String addInvokeType(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "addInterface", at = @At("HEAD"), argsOnly = true, name = "arg1")
    public String addInterface(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "add(ILjava/lang/String;)V", at = @At("HEAD"), argsOnly = true, name = "arg2")
    public String add(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "add(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, name = "arg2")
    public String addClassName(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "add(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, name = "arg4")
    public String addFieldType(String value) {
        return sbw$modifyPackage(value);
    }

    @ModifyVariable(method = "addInvokeDynamic", at = @At("HEAD"), argsOnly = true, name = "arg2")
    public String addInvokeDynamic(String value) {
        return sbw$modifyPackage(value);
    }
}
