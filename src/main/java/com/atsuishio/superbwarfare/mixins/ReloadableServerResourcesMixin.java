package com.atsuishio.superbwarfare.mixins;

import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;

// TODO 能否正确实现这个？
@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {

//    @Final
//    @Shadow
//    private TagManager tagManager;
//
//    @SuppressWarnings({"unchecked", "deprecated"})
//    @Inject(method = "updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V", at = @At("HEAD"))
//    public void onUpdateRegistryTags(CallbackInfo ci) {
//        var itemTagOptional = this.tagManager.getResult().stream()
//                .filter(r -> r.key().equals(Registries.ITEM))
//                .findFirst();
//        if (itemTagOptional.isEmpty()) return;
//
//        var itemTags = (Map<ResourceLocation, Collection<Holder<Item>>>) (Object) itemTagOptional.get().tags();
//
//        for (var gunItem : ForgeRegistries.ITEMS.getEntries().stream()
//                .map(Map.Entry::getValue)
//                .filter(i -> i instanceof GunItem)
//                .toList()
//        ) {
//
//            var tagsToAdd = new HashSet<TagKey<Item>>();
//            tagsToAdd.add(ModTags.Items.GUN);
//            tagsToAdd.add(ModTags.Items.MACHINE_GUN);
//
//            var tagKey = switch (GunData.getDefault(gunItem).gunType) {
//                case SMG -> ModTags.Items.SMG;
//                case RIFLE -> ModTags.Items.RIFLE;
//                case SNIPER -> ModTags.Items.SNIPER_RIFLE;
//                case SHOTGUN -> ModTags.Items.SHOTGUN;
//                case MACHINE_GUN -> ModTags.Items.MACHINE_GUN;
//                case DIRECT_LAUNCHER, CURVED_LAUNCHER -> ModTags.Items.LAUNCHER;
//                default -> null;
//            };
//
//            if (tagKey != null) {
//                tagsToAdd.add(tagKey);
//            }
//
//            for (var tagToAdd : tagsToAdd) {
//                var tag = tagToAdd.location();
//
//                if (itemTags.containsKey(tag)) {
//                    itemTags.computeIfPresent(tag, (k, items) -> new ImmutableSet.Builder<Holder<Item>>()
//                            .addAll(items)
//                            .add(ForgeRegistries.ITEMS.getDelegateOrThrow(gunItem))
//                            .build()
//                    );
//                } else {
//                    itemTags.put(tag, new ImmutableSet.Builder<Holder<Item>>()
//                            .add(ForgeRegistries.ITEMS.getDelegateOrThrow(gunItem))
//                            .build()
//                    );
//                }
//            }
//        }
//    }
}
