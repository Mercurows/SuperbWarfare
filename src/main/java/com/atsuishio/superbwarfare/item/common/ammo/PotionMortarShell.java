package com.atsuishio.superbwarfare.item.common.ammo;

//@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PotionMortarShell extends MortarShell {

    public PotionMortarShell() {
        super();
    }

    // TODO default instance
//    @Override
//    public ItemStack getDefaultInstance() {
//        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.POISON);
//    }

//    @Override
//    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
//        PotionUtils.addPotionTooltip(pStack, pTooltip, 0.125F);
//    }

//    @SubscribeEvent
//    public static void onRegisterColorHandlers(final RegisterColorHandlersEvent.Item event) {
//        event.register((stack, layer) -> layer == 1 ? PotionUtils.getColor(stack) : -1, ModItems.POTION_MORTAR_SHELL.get());
//    }
}
