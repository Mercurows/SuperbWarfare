package com.atsuishio.superbwarfare.client.molang

import software.bernie.geckolib.core.molang.MolangParser
import java.util.function.DoubleSupplier

object MolangVariable {
    const val SBW_SYSTEM_TIME: String = "query.sbw_system_time"
    const val SBW_IS_EMPTY: String = "query.sbw_is_empty"

    fun register() {
        register(SBW_SYSTEM_TIME) { 0.0 }
        register(SBW_IS_EMPTY) { 0.0 }
    }

    private fun register(name: String, supplier: DoubleSupplier) {
        MolangParser.INSTANCE.setMemoizedValue(name, supplier)
    }
}
