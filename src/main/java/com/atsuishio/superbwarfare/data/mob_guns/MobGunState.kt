package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.data.mob_guns.MobGunState.ammo
import com.atsuishio.superbwarfare.data.mob_guns.MobGunState.selectionKey
import net.minecraft.world.entity.Entity

/**
 * 持枪生物自身的状态，写在实体 persistentData 上，**不随枪械物品走**。
 *
 * 两项数据的存在理由：
 * - [selectionKey]：生成时抽中的 [GunSpawnData] 的稳定标识。goal 本身不参与序列化，读档 / 换维度 /
 *   区块重载后只能靠这个键从**当前**数据包重新解析出参数并重建 goal——存的是身份而不是参数，
 *   所以 `/reload` 依然生效（见 [MobGunData.onDataReload]）。
 * - [ammo]：生物的备弹池。它只存在于生物身上，死亡即消失，这是「掉落枪械不会连备弹一起掉出来」的根据
 *   （枪的物品 NBT 里只剩弹匣内的弹药）。它作为备弹参与 `GunData.countBackupAmmo` 的
 *   求和与消耗，因此**不需要**改动枪械本身的弹种声明——掉出去的枪在玩家手里依然按原弹种工作。
 *
 * 注意这两项都是服务端数据（persistentData 不参与网络同步）；客户端读到的恒为默认值，
 * 而生物的开火逻辑只在服务端跑，因此没有影响。
 */
object MobGunState {

    private const val SELECTION_KEY_TAG = "SbwMobGunSelection"
    private const val AMMO_TAG = "SbwMobAmmo"

    /** 生成时抽中的配置键；未登记过（普通生物）返回 `null` */
    fun selectionKey(entity: Entity): String? {
        val data = entity.persistentData
        if (!data.contains(SELECTION_KEY_TAG)) return null
        return data.getString(SELECTION_KEY_TAG).ifEmpty { null }
    }

    fun setSelectionKey(entity: Entity, key: String) {
        entity.persistentData.putString(SELECTION_KEY_TAG, key)
    }

    fun clearSelectionKey(entity: Entity) {
        entity.persistentData.remove(SELECTION_KEY_TAG)
    }

    /** 生物备弹池剩余数量 */
    fun ammo(entity: Entity): Int = entity.persistentData.getInt(AMMO_TAG).coerceAtLeast(0)

    fun setAmmo(entity: Entity, count: Int) {
        val value = count.coerceAtLeast(0)
        if (value == 0) {
            entity.persistentData.remove(AMMO_TAG)
        } else {
            entity.persistentData.putInt(AMMO_TAG, value)
        }
    }

    fun addAmmo(entity: Entity, delta: Int) {
        setAmmo(entity, ammo(entity) + delta)
    }
}
