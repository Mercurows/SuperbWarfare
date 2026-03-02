package com.atsuishio.superbwarfare.resource;

import com.atsuishio.superbwarfare.Mod;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockAnimationResourceSet;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockModelResourceSet;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class BedrockModelLoader {
    public static final ResourceLocation SENPAI_MODEL = Mod.loc("entity/senpai.geo");

    public static final ResourceLocation SENPAI_ANI = Mod.loc("senpai.animation");

    public static final RawResourceLoader COMMON_LOADER = new RawResourceLoader() {
        @Override
        public <T> T load(InputStream inputStream, Class<T> clazz) {
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                return GsonUtil.CLIENT_GSON.fromJson(reader, clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public static void onRegisterBedrockModels(RegisterBedrockModelEvent event) {
        event.register(SENPAI_MODEL, COMMON_LOADER);
    }

    public static void onRegisterBedrockAnimations(RegisterBedrockAnimationEvent event) {
        event.register(SENPAI_ANI, SENPAI_MODEL, COMMON_LOADER);
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelResourceSet.getInstance().getModel(location);
    }

    public static List<BedrockAnimation> getAnimations(ResourceLocation location) {
        return BedrockAnimationResourceSet.getInstance().getAnimations(location);
    }

}
