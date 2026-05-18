package com.atsuishio.superbwarfare.client.model.entity

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import java.util.regex.Pattern

open class BedrockVehicleModel(pojo: BedrockModelPOJO) : BedrockModel(pojo) {
    companion object {
        @JvmField
        val WHEEL_PATTERN: Pattern = Pattern.compile("^wheel(?<direction>[LR]).*$")
    }

    lateinit var leftWheels: List<BedrockBone>
    lateinit var rightWheels: List<BedrockBone>

    lateinit var leftWheelsTurn: List<BedrockBone>
    lateinit var rightWheelsTurn: List<BedrockBone>

    open fun init() {
        val map = this.boneMap

        for ((name, bone) in map.entries) {
            val matcher = WHEEL_PATTERN.matcher(name)
            if (matcher.matches()) {
                val left = matcher.group("direction") == "L"
                val turn = name.endsWith("Turn")

                if (left) {
                    if (turn) {
                        leftWheelsTurn += bone
                    } else {
                        leftWheels += bone
                    }
                } else {
                    if (turn) {
                        rightWheelsTurn += bone
                    } else {
                        rightWheels += bone
                    }
                }
            }
        }
    }
}