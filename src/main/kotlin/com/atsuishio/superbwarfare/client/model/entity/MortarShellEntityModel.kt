package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import software.bernie.geckolib.model.GeoModel

class MortarShellEntityModel : GeoModel<MortarShellEntity>() {
    override fun getAnimationResource(entity: MortarShellEntity) = null

    override fun getModelResource(entity: MortarShellEntity) = loc("geo/mortar_shell.geo.json")

    override fun getTextureResource(entity: MortarShellEntity) = loc("textures/entity/mortar.png")
}
