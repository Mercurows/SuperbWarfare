package com.atsuishio.superbwarfare.item.container

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent
import com.atsuishio.superbwarfare.client.renderer.item.ContainerBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.AnimationState
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class ContainerBlockItem : BlockItem(ModBlocks.CONTAINER.get(), Properties().stacksTo(1).fireResistant()), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun canBeHurtBy(pDamageSource: DamageSource): Boolean {
        return super.canBeHurtBy(pDamageSource) && !pDamageSource.`is`(DamageTypeTags.IS_EXPLOSION) && !pDamageSource.`is`(
            DamageTypes.CACTUS
        )
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        return InteractionResult.PASS
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack?> {
        val playerPOVHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY)
        if (playerPOVHitResult.type == HitResult.Type.MISS) {
            return super.use(level, player, hand)
        }
        val blockHitResult = playerPOVHitResult.withPosition(playerPOVHitResult.blockPos.above())
        val result = super.useOn(UseOnContext(player, hand, blockHitResult))
        return InteractionResultHolder(result, player.getItemInHand(hand))
    }

    override fun place(pContext: BlockPlaceContext): InteractionResult {
        val stack = pContext.itemInHand
        val player = pContext.player
        val res = super.place(pContext)

        if (player != null) {
            val tag = getBlockEntityData(stack)
            if (tag != null && tag.get("Entity") != null) {
                if (player.level().isClientSide && res == InteractionResult.SUCCESS) {
                    player.inventory.removeItem(stack)
                }
                if (!player.level().isClientSide && res == InteractionResult.CONSUME) {
                    player.inventory.removeItem(stack)
                }
            }
        }
        return res
    }

    override fun getName(stack: ItemStack): Component {
        val tag = getBlockEntityData(stack)
        var args = Component.translatable("des.superbwarfare.container.empty")
        if (tag != null && tag.contains("EntityType")) {
            val type = tag.getString("EntityType")
            val entityType = EntityType.byString(type)
            if (entityType.isPresent) {
                args = Component.translatable(entityType.get().descriptionId)
            }
        }
        return Component.translatable("item.superbwarfare.container", args)
    }

    private fun predicate(event: AnimationState<ContainerBlockItem>): PlayState {
        return PlayState.CONTINUE
    }

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer: BlockEntityWithoutLevelRenderer = ContainerBlockItemRenderer()

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }
        })
    }

    override fun registerControllers(data: ControllerRegistrar) {
        data.add(
            AnimationController<ContainerBlockItem>(
                this,
                "controller",
                0
            ) { this.predicate(it) }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache

    @EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
    companion object {
        @SubscribeEvent(priority = EventPriority.HIGH)
        fun registerContainers(event: RegisterContainersEvent) {
            event.add(ModEntities.WHEEL_CHAIR)
            event.add(ModEntities.SODAYO_PICK_UP)
            event.add(ModEntities.SODAYO_PICK_UP_HMG)
            event.add(ModEntities.SODAYO_PICK_UP_TOW)
            event.add(ModEntities.SODAYO_PICK_UP_ROCKET)
            event.add(ModEntities.TRUCK)
            event.add(ModEntities.TYPE_63)
            event.add(ModEntities.TOW)
            event.add(ModEntities.MK_42)
            event.add(ModEntities.MLE_1934)
            event.add(ModEntities.BL_132)
            event.add(ModEntities.HPJ_11)
            event.add(ModEntities.LASER_TOWER)
            event.add(ModEntities.WAVEFORCE_TOWER)
            event.add(ModEntities.ANNIHILATOR)
            event.add(ModEntities.TINY_SPEEDBOAT)
            event.add(ModEntities.SPEEDBOAT)
            event.add(ModEntities.LAV_150)
            event.add(ModEntities.LAV_25)
            event.add(ModEntities.BMP_2)
            event.add(ModEntities.BRADLEY)
            event.add(ModEntities.LAV_AD)
            event.add(ModEntities.PRISM_TANK)
            event.add(ModEntities.ZTZ_99A)
            event.add(ModEntities.T_90A)
            event.add(ModEntities.M_1A_2)
            event.add(ModEntities.YX_100)
            event.add(ModEntities.PLZ_05)
            event.add(ModEntities.AH_6)
            event.add(ModEntities.MI_28)
            event.add(ModEntities.TOM_6)
            event.add(ModEntities.KV_16)
            event.add(ModEntities.JU_87)
            event.add(ModEntities.A_10A)
        }

        @JvmStatic
        fun createInstance(entity: Entity): ItemStack {
            val stack = ItemStack(ModBlocks.CONTAINER.get())
            val tag = CompoundTag()
            tag.put("Entity", entity.serializeNBT())
            tag.putString("EntityType", EntityType.getKey(entity.type).toString())
            setBlockEntityData(stack, ModBlockEntities.CONTAINER.get(), tag)
            return stack
        }

        @JvmStatic
        fun createInstance(entityType: EntityType<*>): ItemStack {
            val stack = ItemStack(ModBlocks.CONTAINER.get())
            val tag = CompoundTag()
            tag.putString("EntityType", EntityType.getKey(entityType).toString())
            setBlockEntityData(stack, ModBlockEntities.CONTAINER.get(), tag)
            return stack
        }
    }
}
