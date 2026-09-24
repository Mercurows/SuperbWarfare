package com.atsuishio.superbwarfare.item.attachment

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition

/**
 * 「能作为配件安装的物品」。
 *
 * 只声明一个 id：配件数据本身由 [definition] 这个**扩展函数**去查，而不是接口的默认实现——
 * Kotlin 接口的默认实现无法被 Java 实现类继承（见 `data/DataCodec.kt` 的 KDoc 里记录的同一个坑），
 * 而仓库里已经有不少 Java 侧的实现类。
 *
 * 用接口而不是 `is BasicAttachmentItem` 表达"配件身份"：将来必须存在"既是 `GunItem`、又是配件"的物品
 * （`GunData.item` 要求副武器物品是枪，而一个物品只能继承一个类），所以"配件身份"只能是接口。
 */
interface AttachmentProvider {
    /** 该物品对应的配件 id（`namespace:path`），例如 `superbwarfare:bayonet_knife`。 */
    val attachmentId: String
}

/** 该物品的配件数据；id 没有对应的数据文件时返回 `null`。 */
fun AttachmentProvider.definition(): AttachmentDefinition? = AttachmentDefinition.from(attachmentId)
