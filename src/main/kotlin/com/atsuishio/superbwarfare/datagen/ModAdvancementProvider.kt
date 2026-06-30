package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.advancement.criteria.OttoSprintTrigger
import com.atsuishio.superbwarfare.advancement.criteria.RPGMeleeExplosionTrigger
import com.atsuishio.superbwarfare.advancement.criteria.VehicleHurtTrigger
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.critereon.DamagePredicate
import net.minecraft.advancements.critereon.DamageSourcePredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.TagPredicate
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.server.packs.PackType
import net.minecraftforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.UnaryOperator

class ModAdvancementProvider(private val packOutput: PackOutput, private val existingFileHelper: ExistingFileHelper) :
    DataProvider {
    val advancements: MutableList<ModAdvancement> = arrayListOf()

    @Suppress("UnusedVariable", "unused")
    fun generate() {
        val mainRoot = advancement("root") {
            it.icon(ModItems.TASER.get()).type(ModAdvancement.Type.SILENT)
                .awardedForFree()
                .rewardLootTable(loc("grant_manual"))
        }
        val bestFriend = advancement("best_friend") {
            it.icon(ModItems.CLAYMORE_MINE.get())
                .whenIconCollected()
                .type(ModAdvancement.Type.SECRET)
                .parent(mainRoot)
        }
        val banzai = advancement("banzai") {
            it.icon(ModItems.LUNGE_MINE.get())
                .whenIconCollected()
                .parent(mainRoot)
        }
        val hammer = advancement("hammer") {
            it.icon(ModItems.HAMMER.get())
                .whenItemCollected(ModTags.Items.HAMMER)
                .parent(mainRoot)
        }
        val physicsExcalibur = advancement("physics_excalibur") {
            it.icon(ModItems.CROWBAR.get())
                .whenIconCollected()
                .parent(mainRoot)
        }
        val vehicleAssembling = advancement("vehicle_assembling") {
            it.icon(ModItems.VEHICLE_ASSEMBLING_TABLE.get())
                .whenIconCollected()
                .parent(physicsExcalibur)
        }
        val cleanEnergy = advancement("clean_energy") {
            it.icon(ModItems.CHARGING_STATION.get())
                .whenIconCollected()
                .parent(physicsExcalibur)
        }
        val superContainer = advancement("super_container") {
            it.icon(ModItems.CONTAINER.get())
                .whenIconCollected()
                .parent(vehicleAssembling)
        }

        // 蓝图
        val blueprint = advancement("blueprint") {
            it.icon(ModItems.AK_47_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.BLUEPRINT)
                .parent(mainRoot)
        }
        val commonBlueprint = advancement("common_blueprint") {
            it.icon(ModItems.M_1911_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.COMMON_BLUEPRINT)
                .parent(blueprint)
        }
        val rareBlueprint = advancement("rare_blueprint") {
            it.icon(ModItems.MP_5_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.RARE_BLUEPRINT)
                .parent(commonBlueprint)
        }
        val epicBlueprint = advancement("epic_blueprint") {
            it.icon(ModItems.QBZ_191_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.EPIC_BLUEPRINT)
                .parent(rareBlueprint)
        }
        val legendaryBlueprint = advancement("legendary_blueprint") {
            it.icon(ModItems.AA_12_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.LEGENDARY_BLUEPRINT)
                .parent(epicBlueprint)
        }
        val superbBlueprint = advancement("superb_blueprint") {
            it.icon(ModItems.SUPER_STAR_SHOOTER_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.SUPERB_BLUEPRINT)
                .parent(legendaryBlueprint)
        }
        val virtualBlueprint = advancement("virtual_blueprint") {
            it.icon(ModItems.TRACHELIUM_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.VIRTUAL_BLUEPRINT)
                .parent(superbBlueprint)
        }
        val cannonBlueprint = advancement("cannon_blueprint") {
            it.icon(ModItems.MK_42_BLUEPRINT.get())
                .whenItemCollected(ModTags.Items.CANNON_BLUEPRINT)
                .parent(blueprint)
        }
        val blueprintResearching = advancement("blueprint_researching") {
            it.icon(ModItems.BLUEPRINT_RESEARCH_TABLE.get())
                .whenIconCollected()
                .parent(blueprint)
        }

        // 古代芯片
        val ancientTechnology = advancement("ancient_technology") {
            it.icon(ModItems.ANCIENT_CPU.get())
                .whenIconCollected()
                .type(ModAdvancement.Type.GOAL)
                .parent(mainRoot)
        }
        val enclave = advancement("enclave") {
            it.icon(ModItems.REFORGING_TABLE.get())
                .whenIconCollected()
                .type(ModAdvancement.Type.GOAL)
                .parent(ancientTechnology)
        }

        val handsomeFrame = advancement("handsome_frame") {
            it.icon(ModItems.INTELLIGENT_CHIP!!.get())
                .whenIconCollected()
                .type(ModAdvancement.Type.GOAL)
                .parent(enclave)
        }
        val powerfulCooler = advancement("powerful_cooler") {
            it.icon(ModItems.PERK_ITEMS[ModPerks.POWERFUL_COOLER]!!.get())
                .whenIconCollected()
                .parent(enclave)
        }

        // 哑弹棒（？）
        val boomstickMelee = advancement("boomstick_melee") {
            it.icon(ModItems.RPG_ROCKET_TBG.get())
                .externalTrigger(RPGMeleeExplosionTrigger.TriggerInstance.get())
                .type(ModAdvancement.Type.SECRET_CHALLENGE)
                .parent(mainRoot)
        }

        val rushRushRun = advancement("rush_rush_run") {
            it.icon(ModItems.ELECTRIC_BATON.get())
                .externalTrigger(OttoSprintTrigger.TriggerInstance.get())
                .type(ModAdvancement.Type.SECRET_CHALLENGE)
                .parent(mainRoot)
        }

        val deleteYourGun = advancement("delete_your_gun") {
            it.icon(ModItems.MARLIN.get())
                .externalTrigger(
                    VehicleHurtTrigger.TriggerInstance.vehicleHurt(
                        DamagePredicate.Builder.damageInstance()
                            .type(
                                DamageSourcePredicate.Builder.damageType().tag(
                                    TagPredicate.`is`(ModTags.DamageTypes.SBW_GUN_FIRE_DAMAGE)
                                )
                            )
                            .dealtDamage(MinMaxBounds.Doubles.atMost(0.1))
                    )
                )
                .type(ModAdvancement.Type.SECRET_CHALLENGE)
                .parent(superContainer)
        }
        val criticalHit = advancement("critical_hit") {
            it.icon(ModItems.NTW_20.get())
                .externalTrigger(
                    VehicleHurtTrigger.TriggerInstance.vehicleHurt(
                        DamagePredicate.Builder.damageInstance()
                            .type(
                                DamageSourcePredicate.Builder.damageType().tag(
                                    TagPredicate.`is`(ModTags.DamageTypes.SBW_GUN_FIRE_DAMAGE)
                                )
                            )
                            .dealtDamage(MinMaxBounds.Doubles.atLeast(514.0))
                    )
                )
                .type(ModAdvancement.Type.SECRET_CHALLENGE)
                .parent(deleteYourGun)
        }
    }

    private fun advancement(id: String, b: UnaryOperator<ModAdvancement.Builder>): ModAdvancement {
        val advancement = ModAdvancement(id, b)
        this.advancements.add(advancement)
        return advancement
    }

    override fun run(pOutput: CachedOutput): CompletableFuture<*> {
        this.generate()

        val futures = arrayListOf<CompletableFuture<*>>()
        val pathProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "advancements")

        val mainConsumer = Consumer { advancement: Advancement ->
            val id = advancement.id
            check(
                !existingFileHelper.exists(
                    id,
                    PackType.SERVER_DATA,
                    ".json",
                    "advancements"
                )
            ) { "Duplicate advancement $id" }
            val path = pathProvider.json(id)
            futures.add(DataProvider.saveStable(pOutput, advancement.deconstruct().serializeToJson(), path))
        }

        for (advancement in this.advancements) {
            advancement.save(mainConsumer)
        }

        return CompletableFuture.allOf(*futures.toTypedArray())
    }

    override fun getName(): String {
        return "Superb Warfare Advancements"
    }
}
