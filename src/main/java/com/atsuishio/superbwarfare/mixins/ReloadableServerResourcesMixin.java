package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {

    @Final
    @Shadow
    private TagManager tagManager;

    @SuppressWarnings("unchecked")
    @Inject(method = "updateRegistryTags()V", at = @At("HEAD"))
    public void onUpdateRegistryTags(CallbackInfo ci) {
        var itemTagOptional = this.tagManager.getResult().stream()
                .filter(r -> r.key().equals(Registries.ITEM))
                .findFirst();
        if (itemTagOptional.isEmpty()) return;

        var itemTags = (Map<ResourceLocation, Collection<Holder<Item>>>) (Object) itemTagOptional.get().tags();

        for (var gunItem : BuiltInRegistries.ITEM.stream().filter(i -> i instanceof GunItem).toList()) {

            var tagsToAdd = new HashSet<TagKey<Item>>();
            tagsToAdd.add(ModTags.Items.GUN);
//            tagsToAdd.add(ModTags.Items.MACHINE_GUN);

            var tagKey = switch (GunData.getDefault(gunItem).gunType) {
                case SMG -> ModTags.Items.SMG;
                case RIFLE -> ModTags.Items.RIFLE;
                case SNIPER -> ModTags.Items.SNIPER_RIFLE;
                case SHOTGUN -> ModTags.Items.SHOTGUN;
                case MACHINE_GUN -> ModTags.Items.MACHINE_GUN;
                case DIRECT_LAUNCHER, CURVED_LAUNCHER -> ModTags.Items.LAUNCHER;
                default -> null;
            };

            if (tagKey != null) {
                tagsToAdd.add(tagKey);
            }

            for (var tagToAdd : tagsToAdd) {
                var tag = tagToAdd.location();

                if (itemTags.containsKey(tag)) {
                    itemTags.computeIfPresent(tag, (k, items) -> new ImmutableSet.Builder<Holder<Item>>()
                            .addAll(items)
                            .add(BuiltInRegistries.ITEM.wrapAsHolder(gunItem))
                            .build());
                } else {
                    itemTags.put(tag, new ImmutableSet.Builder<Holder<Item>>()
                            .add(BuiltInRegistries.ITEM.wrapAsHolder(gunItem))
                            .build()
                    );
                }
            }
        }
    }
}
