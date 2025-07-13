package com.atsuishio.superbwarfare.data.container;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public class ContainerDataManager extends SimpleJsonResourceReloadListener {

    public static ContainerDataManager INSTANCE = new ContainerDataManager();

    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "containers";
    private final Map<ResourceLocation, List<String>> containerData = new HashMap<>();

    public ContainerDataManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        INSTANCE = new ContainerDataManager();
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager manager, ProfilerFiller profiler) {
        containerData.clear();
        pObject.forEach((id, json) -> {
            try {
                JsonObject obj = json.getAsJsonObject();
                List<String> list = new ArrayList<>();
                obj.getAsJsonArray("EntityTypes").forEach(e -> list.add(e.getAsString()));
                containerData.put(id, list);
            } catch (Exception e) {
                Mod.LOGGER.error("Failed to load container data for {}", id);
            }
        });
    }

    public Optional<List<String>> getEntityTypes(ResourceLocation id) {
        return Optional.ofNullable(containerData.get(id));
    }
}
