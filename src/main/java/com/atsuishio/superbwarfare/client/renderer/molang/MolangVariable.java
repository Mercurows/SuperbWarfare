package com.atsuishio.superbwarfare.client.renderer.molang;

import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.value.Variable;

import java.util.function.DoubleSupplier;

public class MolangVariable {
    public static void register() {
        register("sbw.system_time", System::currentTimeMillis);
    }

    private static void register(String name, DoubleSupplier supplier) {
        MathParser.registerVariable(new Variable(name, supplier));
    }
}
