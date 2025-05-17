package com.atsuishio.superbwarfare.client.renderer.molang;

import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.value.Variable;

public class MolangVariable {
    public static void register() {
        MathParser.registerVariable(new Variable("sbw.system_time", System::currentTimeMillis));
    }
}
