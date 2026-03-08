package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.curio.DogTagItem
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import top.theillusivec4.curios.api.CuriosApi
import kotlin.random.Random

object SpritePixelHelper {
    private val RANDOM = Random

    /**
     * 从 TextureAtlasSprite 中随机抽取一个像素，返回 RGB 颜色值
     * 
     * @param sprite 纹理精灵
     * @param frame  帧索引（动画纹理时使用，通常为 0）
     * @return RGB 颜色值，格式为 0xRRGGBB
     */
    fun getRandomPixelRGB(sprite: TextureAtlasSprite, frame: Int): Int {
        // 获取纹理尺寸
        val width = sprite.contents().width()
        val height = sprite.contents().height()

        // 生成随机坐标
        val x = RANDOM.nextInt(width)
        val y = RANDOM.nextInt(height)

        // 获取像素值
        val colors = sprite.getPixelRGBA(frame, x, y)

        // 提取分量
        val blue = (colors shr 16) and 0xFF
        val green = (colors shr 8) and 0xFF
        val red = colors and 0xFF

        // 组合为 RGB
        return (red shl 16) or (green shl 8) or blue
    }
    fun getDogTagIcon(livingEntity: LivingEntity): ResourceLocation {
        val newDogTagIcon = createDogTagImage(livingEntity)

        val newTextureLoc = Mod.loc(livingEntity.name.string.lowercase() + "_dog_tag.png")

        mc.textureManager.register(
            newTextureLoc,
            DynamicTexture(newDogTagIcon)
        )

        return newTextureLoc
    }

    private fun createDogTagImage(living: LivingEntity): NativeImage {
        val dogTag = NativeImage(16, 16, false)
        CuriosApi.getCuriosInventory(living).ifPresent { c ->
            c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                val stack = s.stack()
                val icon = DogTagItem.getColors(stack)
                for (x in 0..<16) {
                    for (y in 0..<16) {
                        if (icon[x][y].toInt() == -1) {
                            dogTag.setPixelRGBA(x, y, 0x00000000)
                        } else {
                            val color = DogTagEditorScreen.getColorByNum(icon[x][y])
                            dogTag.setPixelRGBA(x, y, argbToAbgr(color))
                        }
                    }
                }
            }
        }
        return dogTag
    }

    fun argbToAbgr(argb: Int): Int {
        val a = (argb shr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (a shl 24) or (b shl 16) or (g shl 8) or r
    }
}