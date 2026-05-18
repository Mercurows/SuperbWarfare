package com.atsuishio.superbwarfare.resource

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.google.gson.Gson
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.SimplePreparableReloadListener

abstract class BedrockModelReloadListener<T>(val path: String) :
    SimplePreparableReloadListener<Map<ResourceLocation, BedrockModelPOJO>>() {
    val gson: Gson = GsonUtil.CLIENT_GSON
    val data: MutableMap<ResourceLocation, T> = hashMapOf()
}