package com.atsuishio.superbwarfare.client.model.entity

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import java.util.regex.Pattern

open class BedrockVehicleModel(pojo: BedrockModelPOJO) : BedrockModel(pojo) {
    companion object {
        @JvmField
        val WHEEL_PATTERN: Pattern = Pattern.compile("^wheel(?<direction>[LR]).*$")
        val SHELL_PATTERN: Pattern = Pattern.compile("^shell(?<id>\\d+)$")
    }

    lateinit var leftWheels: List<BedrockBone>
    lateinit var rightWheels: List<BedrockBone>

    lateinit var leftWheelsTurn: List<BedrockBone>
    lateinit var rightWheelsTurn: List<BedrockBone>

    lateinit var shell: List<BedrockBone>

    open fun init() {
        val map = this.boneMap

        val leftWheels = mutableListOf<BedrockBone>()
        val rightWheels = mutableListOf<BedrockBone>()
        val leftWheelsTurn = mutableListOf<BedrockBone>()
        val rightWheelsTurn = mutableListOf<BedrockBone>()

        val shell = mutableListOf<BedrockBone>()

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

            val matcherShell = SHELL_PATTERN.matcher(name)

            if (matcherShell.matches()) {
                shell += bone
            }
        }

        this.leftWheels = leftWheels
        this.rightWheels = rightWheels
        this.leftWheelsTurn = leftWheelsTurn
        this.rightWheelsTurn = rightWheelsTurn

        this.shell = shell
    }
}