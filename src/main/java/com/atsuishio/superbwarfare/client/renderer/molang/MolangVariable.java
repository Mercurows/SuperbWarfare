package com.atsuishio.superbwarfare.client.renderer.molang;

import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.value.Variable;

import java.util.function.DoubleSupplier;

public class MolangVariable {

    public static final String SBW_SYSTEM_TIME = "sbw.system_time";

    public static void register() {
        register(SBW_SYSTEM_TIME, () -> 0);
    }

    private static void register(String name, DoubleSupplier supplier) {
        MathParser.registerVariable(new Variable(name, supplier));
    }
}
