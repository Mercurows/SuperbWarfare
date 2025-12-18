package com.atsuishio.superbwarfare.compat.tacz;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class TACZGunEventHandler {

    public static void entityHurtByTACZGun(EntityHurtByGunEvent.Pre event) {
        if (event.getHurtEntity() instanceof VehicleEntity) {
            event.setHeadshot(false);
        }
    }

    public static boolean hasMod() {
        return ModList.get().isLoaded("tacz");
    }

    public static boolean compatCondition() {
        var modFile = LoadingModList.get().getModFileById("tacz");
        if (modFile == null) return false;
        DefaultArtifactVersion modVersion = new DefaultArtifactVersion(modFile.versionString());
        return modVersion.compareTo(new DefaultArtifactVersion("1.1.4")) >= 0;
    }

    public static ResourceLocation getTaczCompatIcon(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            ResourceLocation gunId = iGun.getGunId(stack);
            GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
            GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
            if (gunData != null && display != null) {
                return display.getHUDTexture();
            }
        }
        return null;
    }
}
