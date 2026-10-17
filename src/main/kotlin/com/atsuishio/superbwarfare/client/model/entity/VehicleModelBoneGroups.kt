package com.atsuishio.superbwarfare.client.model.entity

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import java.util.regex.Pattern

data class VehicleModelBoneGroups(
    val leftWheels: List<BoneState>,
    val rightWheels: List<BoneState>,
    val leftWheelsTurn: List<BoneState>,
    val rightWheelsTurn: List<BoneState>,
    val leftTrackMove: List<BoneState>,
    val leftTrackRot: List<BoneState>,
    val rightTrackMove: List<BoneState>,
    val rightTrackRot: List<BoneState>,
    val shell: List<BoneState>,
    val flareBones: List<BoneState>,
    val laserBones: List<BoneState>,
    val dogTagBones: List<BoneState>,
) {
    /**
     * Phase spacing that distributes the authored track bones evenly across the
     * vehicle's track animation loop. This reproduces the previously hand-tuned
     * per-vehicle constants (for example `100 / 37 ≈ 2.70` for the ZTZ-99A and
     * `80 / 41 ≈ 1.95` for the YX-100) directly from the model, so a track
     * profile no longer has to be duplicated in every renderer.
     */
    fun derivedTrackDistance(wrapLength: Int): Float {
        val count = maxOf(leftTrackMove.size, rightTrackMove.size)
        if (count <= 1 || wrapLength <= 0) return 2f
        return wrapLength.toFloat() / count
    }

    companion object {
        val WHEEL_PATTERN: Pattern = Pattern.compile("^wheel(?<direction>[LR]).*$")
        val W_PATTERN: Pattern = Pattern.compile("^w_(?<direction>[lLrR]).*$")
        val SHELL_PATTERN: Pattern = Pattern.compile("^shell(?<id>\\d+)$")
        val TRACK_PATTERN: Pattern = Pattern.compile("^track(?<type>Mov|Rot)(?<direction>[LR])(?<id>\\d+)$")
        val FLARE_PATTERN: Pattern = Pattern.compile("^flare.*")
        val LASER_PATTERN: Pattern = Pattern.compile("^laser.*")
        val DOG_TAG_PATTERN: Pattern = Pattern.compile("^.*_dogTag_(?<w>\\d+)x(?<h>\\d+)$")

        /**
         * Parses the dog tag quad size from a bone name like "body_back_dogTag_10x10".
         * Returns a pair of (width, height) in block units (Bedrock units / 16),
         * or null if the name doesn't match.
         */
        @JvmStatic
        fun parseDogTagSize(boneName: String): Pair<Float, Float>? {
            val matcher = DOG_TAG_PATTERN.matcher(boneName)
            if (!matcher.matches()) return null
            val w = matcher.group("w")?.toFloatOrNull() ?: return null
            val h = matcher.group("h")?.toFloatOrNull() ?: return null
            return Pair(w / 16f, h / 16f)
        }

        @JvmStatic
        fun compute(instance: BakedModelInstance): VehicleModelBoneGroups {
            val leftWheels = mutableListOf<BoneState>()
            val rightWheels = mutableListOf<BoneState>()
            val leftWheelsTurn = mutableListOf<BoneState>()
            val rightWheelsTurn = mutableListOf<BoneState>()

            val tempShell = hashMapOf<Int, BoneState>()

            val leftTrackMove = hashMapOf<Int, BoneState>()
            val leftTrackRot = hashMapOf<Int, BoneState>()
            val rightTrackMove = hashMapOf<Int, BoneState>()
            val rightTrackRot = hashMapOf<Int, BoneState>()

            val flareBones = mutableListOf<BoneState>()
            val laserBones = mutableListOf<BoneState>()

            val dogTagBones = mutableListOf<BoneState>()

            for (bone in instance.boneIndexes) {
                val name = bone.name()

                val wheelMatcher = WHEEL_PATTERN.matcher(name)
                if (wheelMatcher.matches()) {
                    val left = wheelMatcher.group("direction") == "L"
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

                // Also match w_ prefix wheel bones (e.g. w_lb, w_rb, w_lr, w_rr)
                val wMatcher = W_PATTERN.matcher(name)
                if (wMatcher.matches()) {
                    val left = wMatcher.group("direction").lowercase() == "l"
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

                val shellMatcher = SHELL_PATTERN.matcher(name)
                if (shellMatcher.matches()) {
                    val index = shellMatcher.group("id").toInt()
                    tempShell[index] = bone
                }

                val trackMatcher = TRACK_PATTERN.matcher(name)
                if (trackMatcher.matches()) {
                    val isRot = trackMatcher.group("type") == "Rot"
                    val isL = trackMatcher.group("direction") == "L"
                    val index = trackMatcher.group("id").toInt()

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

                val flareMatcher = FLARE_PATTERN.matcher(name)
                if (flareMatcher.matches()) {
                    flareBones += bone
                }

                val laserMatcher = LASER_PATTERN.matcher(name)
                if (laserMatcher.matches()) {
                    laserBones += bone
                }

                val dogTagMatcher = DOG_TAG_PATTERN.matcher(name)
                if (dogTagMatcher.matches()) {
                    dogTagBones += bone
                }
            }

            fun wheelOrder(bone: BoneState): Pair<Int, String> {
                val match = Pattern.compile("(?:wheel[LR]|w_[lLrR])(?<id>\\d+).*").matcher(bone.name())
                return (if (match.matches()) match.group("id").toInt() else Int.MAX_VALUE) to bone.name()
            }

            val wheelComparator = Comparator<BoneState> { a, b ->
                val orderA = wheelOrder(a)
                val orderB = wheelOrder(b)
                val indexCompare = orderA.first.compareTo(orderB.first)
                if (indexCompare != 0) indexCompare else orderA.second.compareTo(orderB.second)
            }

            return VehicleModelBoneGroups(
                leftWheels = leftWheels.sortedWith(wheelComparator),
                rightWheels = rightWheels.sortedWith(wheelComparator),
                leftWheelsTurn = leftWheelsTurn.sortedWith(wheelComparator),
                rightWheelsTurn = rightWheelsTurn.sortedWith(wheelComparator),
                leftTrackMove = leftTrackMove.toSortedMap().values.toMutableList(),
                leftTrackRot = leftTrackRot.toSortedMap().values.toMutableList(),
                rightTrackMove = rightTrackMove.toSortedMap().values.toMutableList(),
                rightTrackRot = rightTrackRot.toSortedMap().values.toMutableList(),
                shell = tempShell.toSortedMap().values.toMutableList(),
                flareBones = flareBones,
                laserBones = laserBones,
                dogTagBones = dogTagBones,
            )
        }
    }
}
