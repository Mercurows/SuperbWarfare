package com.atsuishio.superbwarfare.data.container;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class ContainerDataManager extends SimpleJsonResourceReloadListener {

    public static ContainerDataManager INSTANCE = new ContainerDataManager();

    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "sbw/containers";
    private final Map<ResourceLocation, List<Pair<String, Integer>>> containerData = new HashMap<>();

    public ContainerDataManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        INSTANCE = new ContainerDataManager();
        event.addListener(INSTANCE);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager manager, ProfilerFiller profiler) {
        containerData.clear();
        pObject.forEach((id, json) -> {
            try {
                JsonObject obj = json.getAsJsonObject();
                List<Pair<String, Integer>> list = new ArrayList<>();
                var array = obj.getAsJsonArray("List");
                for (var arr : array) {
                    if (arr.isJsonObject()) {
                        JsonObject obj2 = arr.getAsJsonObject();
                        String type = obj2.get("Type").getAsString();
                        int weight = obj2.get("Weight").getAsInt();
                        list.add(Pair.of(type, weight));
                    } else {
                        list.add(Pair.of(arr.getAsString(), 1));
                    }
                }
                containerData.put(id, list);
            } catch (Exception e) {
                Mod.LOGGER.error("Failed to load container data for {}", id);
            }
        });
    }

    public Optional<List<Pair<String, Integer>>> getEntityTypes(ResourceLocation id) {
        return Optional.ofNullable(containerData.get(id));
    }
}
