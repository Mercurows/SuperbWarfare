package com.atsuishio.superbwarfare.client.model.entity

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import java.util.regex.Pattern

open class BedrockVehicleModel(pojo: BedrockModelPOJO) : BedrockModel(pojo) {
    companion object {
        @JvmField
        val WHEEL_PATTERN: Pattern = Pattern.compile("^wheel(?<direction>[LR]).*$")

        @JvmField
        val SHELL_PATTERN: Pattern = Pattern.compile("^shell(?<id>\\d+)$")

        @JvmField
        val TRACK_PATTERN: Pattern = Pattern.compile("^track(?<type>Mov|Rot)(?<direction>[LR])(?<id>\\d+)$")

        @JvmField
        val FLARE_PATTERN: Pattern = Pattern.compile("^flare.*")

        @JvmField
        val DOG_TAG_PATTERN: Pattern = Pattern.compile("^.*_dogTag$")
    }

    lateinit var leftWheels: List<BedrockBone>
    lateinit var rightWheels: List<BedrockBone>

    lateinit var leftWheelsTurn: List<BedrockBone>
    lateinit var rightWheelsTurn: List<BedrockBone>

    lateinit var shell: List<BedrockBone>

    lateinit var leftTrackMove: List<BedrockBone>
    lateinit var leftTrackRot: List<BedrockBone>

    lateinit var rightTrackMove: List<BedrockBone>
    lateinit var rightTrackRot: List<BedrockBone>

    lateinit var flareBones: List<BedrockBone>

    lateinit var dogTagBones: List<BedrockBone>

    open fun init() {
        val map = this.boneMap

        val leftWheels = mutableListOf<BedrockBone>()
        val rightWheels = mutableListOf<BedrockBone>()
        val leftWheelsTurn = mutableListOf<BedrockBone>()
        val rightWheelsTurn = mutableListOf<BedrockBone>()

        val tempShell = hashMapOf<Int, BedrockBone>()

        val leftTrackMove = hashMapOf<Int, BedrockBone>()
        val leftTrackRot = hashMapOf<Int, BedrockBone>()
        val rightTrackMove = hashMapOf<Int, BedrockBone>()
        val rightTrackRot = hashMapOf<Int, BedrockBone>()

        val flareBones = mutableListOf<BedrockBone>()

        val dogTagBones = mutableListOf<BedrockBone>()

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
                val index = matcherShell.group("id").toInt()
                tempShell[index] = bone
            }

            val matcherTrackPart = TRACK_PATTERN.matcher(name)
            if (matcherTrackPart.matches()) {
                val isRot = matcherTrackPart.group("type") == "Rot"
                val isL = matcherTrackPart.group("direction") == "L"
                val index = matcherTrackPart.group("id").toInt()

                if (isRot) {
                    if (isL) {
                        leftTrackRot[index] = bone
                    } else {
                        rightTrackRot[index] = bone
                    }
                } else {
                    if (isL) {
                        leftTrackMove[index] = bone
                    } else {
                        rightTrackMove[index] = bone
                    }
                }
            }

            val matcherFlare = FLARE_PATTERN.matcher(name)
            if (matcherFlare.matches()) {
                flareBones += bone
            }

            val matcherDogTag = DOG_TAG_PATTERN.matcher(name)
            if (matcherDogTag.matches()) {
                dogTagBones += bone
            }
        }

        this.leftWheels = leftWheels
        this.rightWheels = rightWheels
        this.leftWheelsTurn = leftWheelsTurn
        this.rightWheelsTurn = rightWheelsTurn

        this.shell = tempShell.toSortedMap().values.toMutableList()

        this.leftTrackMove = leftTrackMove.toSortedMap().values.toMutableList()
        this.leftTrackRot = leftTrackRot.toSortedMap().values.toMutableList()
        this.rightTrackMove = rightTrackMove.toSortedMap().values.toMutableList()
        this.rightTrackRot = rightTrackRot.toSortedMap().values.toMutableList()

        this.flareBones = flareBones

        this.dogTagBones = dogTagBones
    }
}