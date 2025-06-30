package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.Gson;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Mod.MODID)
public class DataLoader {

    private static final HashMap<String, GeneralData<?>> loadedData = new HashMap<>();

    private record GeneralData<T extends IDBasedData>(
            Class<?> type, DataMap<T> proxyMap,
            HashMap<String, Object> data,
            @Nullable Consumer<Map<String, Object>> onReload
    ) {
    }

    public static <T extends IDBasedData> DataMap<T> createData(String name, Class<T> clazz) {
        return createData(name, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IDBasedData> DataMap<T> createData(String name, Class<T> clazz, @Nullable Consumer<Map<String, Object>> onReload) {
        if (loadedData.containsKey(name)) {
            return (DataMap<T>) loadedData.get(name).proxyMap;
        } else {
            var proxyMap = new DataMap<T>(name);
            loadedData.put(name, new GeneralData<>(clazz, proxyMap, new HashMap<>(), onReload));
            return proxyMap;
        }
    }

    private static void reloadAllData(ResourceManager manager) {
        loadedData.forEach((name, value) -> {
            var map = value.data;
            map.clear();

            for (var entry : manager.listResources(name, file -> file.getPath().endsWith(".json")).entrySet()) {
                var attribute = entry.getValue();
                try {
                    Gson gson = new Gson();
                    var data = (IDBasedData) gson.fromJson(new InputStreamReader(attribute.open()), value.type);

                    String id;
                    if (!data.getId().isEmpty()) {
                        id = data.getId();
                    } else {
                        var path = entry.getKey().getPath();
                        id = Mod.MODID + ":" + path.substring(name.length() + 1, path.length() - name.length() - 1);
                        Mod.LOGGER.warn("{} ID for {} is empty, try using {} as id", name, id, path);
                    }

                    map.put(id, data);
                } catch (Exception e) {
                    Mod.LOGGER.error(e.getMessage());
                }
            }

            if (value.onReload != null) {
                value.onReload.accept(map);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void serverStarted(ServerStartedEvent event) {
        reloadAllData(event.getServer().getResourceManager());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        reloadAllData(event.getPlayerList().getServer().getResourceManager());
    }

    // read-only custom data map

    public static class DataMap<T extends IDBasedData> extends HashMap<String, T> {
        private final String name;

        private DataMap(String name) {
            this.name = name;
        }

        @Override
        public int size() {
            if (!loadedData.containsKey(name)) return 0;
            return loadedData.get(name).data.size();
        }

        @Override
        public boolean isEmpty() {
            if (!loadedData.containsKey(name)) return true;
            return loadedData.get(name).data.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(Object key) {
            if (!loadedData.containsKey(name)) return null;
            return (T) loadedData.get(name).data.get(key);
        }

        @Override
        public T getOrDefault(Object key, T defaultValue) {
            var value = get(key);
            return value == null ? defaultValue : value;
        }

        @Override
        public boolean containsKey(Object key) {
            if (!loadedData.containsKey(name)) return false;
            return loadedData.get(name).data.containsKey(key);
        }

        @Override
        public T put(String key, T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ? extends T> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            if (!loadedData.containsKey(name)) return false;
            return loadedData.get(name).data.containsValue(value);
        }

        @Override
        public @NotNull Set<String> keySet() {
            if (!loadedData.containsKey(name)) return Set.of();
            return loadedData.get(name).data.keySet();
        }
    }
}
