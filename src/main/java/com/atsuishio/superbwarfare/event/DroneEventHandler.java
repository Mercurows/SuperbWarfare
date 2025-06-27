package com.atsuishio.superbwarfare.event;

/**
 * Code based on @Mafuyu404's <a href="https://github.com/Mafuyu404/DiligentStalker">DiligentStalker</a>
 */
//@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID, value = Dist.CLIENT)
public class DroneEventHandler {

//    @SubscribeEvent
//    public static void onClientTick(TickEvent.ClientTickEvent event) {
//        if (Minecraft.getInstance().screen != null) return;
//        if (event.phase != TickEvent.Phase.END) return;
//        var player = Minecraft.getInstance().player;
//        UUID entityUUID = Monitor.getDroneUUID(player);
//        if (entityUUID != null) {
//            ClientLevel level = player.clientLevel;
//            level.entitiesForRendering().forEach(entity -> {
//                if (entity.getUUID().equals(entityUUID)) {
//                    DronesTool.connect(player, entity);
//                }
//            });
//        }
//        if (!DronesTool.hasInstanceOf(player)) return;
//        DronesTool instance = DronesTool.getInstanceOf(player);
//        Entity stalker = instance.getDrone();
//        // TODO 调整角度？
//    }
//
//    @SubscribeEvent
//    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
//        if (event.getSide().isClient()) {
//            var player = event.getEntity();
//            if (player.isShiftKeyDown()) return;
//            if (event.getItemStack().is(ModItems.MONITOR.get())) {
//                DronesTool.connect(player, event.getTarget());
//                event.setCanceled(true);
//            }
//        }
//    }
//
//    @SubscribeEvent
//    public static void onInputKey(InputEvent.Key event) {
//        if (Minecraft.getInstance().screen != null) return;
//        Player player = Minecraft.getInstance().player;
//        if (!DronesTool.hasInstanceOf(player)) return;
//        if (event.getAction() == InputConstants.PRESS) {
//            if (event.getKey() == ModKeyMappings.DISMOUNT.getKey().getValue()) {
//                if (DronesTool.hasInstanceOf(player)) {
//                    DronesTool.getInstanceOf(player).disconnect();
//                }
//            }
//        }
//    }
}
