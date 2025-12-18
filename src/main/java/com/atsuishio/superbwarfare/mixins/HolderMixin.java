package com.atsuishio.superbwarfare.mixins;

import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;

// TODO 能否正确实现这个？
@Mixin(Holder.Reference.class)
public class HolderMixin {

//    @Inject(at = @At("HEAD"), method = "bindTags(Ljava/util/Collection;)V")
//    public void bindTags(Collection<TagKey<?>> tags, CallbackInfo ci) {
//        var ref = (Holder.Reference<?>) (Object) this;
//        if (ref.value() instanceof GunItem gunItem) {
//            tags.add(ModTags.Items.GUN);
//
//            var tag = switch (GunData.getDefault(gunItem).gunType) {
//                case SMG -> ModTags.Items.SMG;
//                case RIFLE -> ModTags.Items.RIFLE;
//                case SNIPER -> ModTags.Items.SNIPER_RIFLE;
//                case SHOTGUN -> ModTags.Items.SHOTGUN;
//                case MACHINE_GUN -> ModTags.Items.MACHINE_GUN;
//                case DIRECT_LAUNCHER, CURVED_LAUNCHER -> ModTags.Items.LAUNCHER;
//                default -> null;
//            };
//
//            if (tag != null) {
//                tags.add(tag);
//            }
//        }
//    }
}