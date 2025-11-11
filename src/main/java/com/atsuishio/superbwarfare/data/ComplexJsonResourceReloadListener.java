package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.JsonParseException;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.Map;

public class ComplexJsonResourceReloadListener extends SimplePreparableReloadListener<Object> {

    private final Map<String, DataLoader.GeneralData<?>> data;

    public ComplexJsonResourceReloadListener(Map<String, DataLoader.GeneralData<?>> data) {
        this.data = data;
    }

    private static final Object NULL = new Object();

    @ParametersAreNonnullByDefault
    protected @NotNull Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        this.data.forEach((name, value) -> {
            var map = value.data();
            map.clear();

            var converter = FileToIdConverter.json(name);
            for (var entry : converter.listMatchingResources(resourceManager).entrySet()) {
                var resourcelocation = entry.getKey();
                var pathLocation = converter.fileToId(resourcelocation);

                try (var reader = entry.getValue().openAsReader()) {
                    var data = DataLoader.GSON.fromJson(reader, value.type());

                    String id;
                    if (data instanceof IDBasedData<?> IDData && !IDData.getId().isEmpty()) {
                        id = IDData.getId();
                    } else {
                        id = pathLocation.toString();
                        Mod.LOGGER.warn("{} ID for {} is empty, try using {} as id", name, id, pathLocation);
                    }

                    map.put(id, data);
                } catch (IllegalArgumentException | IOException | JsonParseException exception) {
                    Mod.LOGGER.error("Couldn't parse data file {} from {}", pathLocation, resourcelocation, exception);
                }
            }

            if (value.onReload() != null) {
                value.onReload().accept(map);
            }
        });

        return NULL;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void apply(Object obj, ResourceManager resourceManager, ProfilerFiller profiler) {
    }

}
