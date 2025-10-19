package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
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

    // 务必在所有需要序列化GSON数据的地方调用，避免报错
    public static GsonBuilder createCommonBuilder() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setLenient()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapter(Vec3.class, new Vec3Adapter())
                .registerTypeAdapter(StringOrVec3.class, new StringOrVec3.StringOrVec3Adapter())
                .registerTypeAdapterFactory(new ObjectToList.AdapterFactory())
                .registerTypeAdapterFactory(new StringToObject.AdapterFactory());
    }

    public static final Gson GSON = createCommonBuilder().create();

    private static void reloadAllData(ResourceManager manager) {
        loadedData.forEach((name, value) -> {
            var map = value.data;
            map.clear();

            for (var entry : manager.listResources(name, location -> location.getNamespace().equals(Mod.MODID) && location.getPath().endsWith(".json")).entrySet()) {
                var attribute = entry.getValue();
                try {
                    var data = (IDBasedData) GSON.fromJson(new InputStreamReader(attribute.open()), value.type);

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

    /**
     * 将StringToObject和ObjectToList转换为原始值
     */
    public static Object processValue(Object value) {
        if (value instanceof ObjectToList<?> otl) {
            return otl.list.stream().map(DataLoader::processValue).toList();
        } else if (value instanceof StringToObject<?> sto) {
            return processValue(sto.value);
        }
        return value;
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
        @SuppressWarnings("unchecked")
        public T put(String key, T value) {
            return (T) loadedData.get(name).data.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends T> m) {
            loadedData.get(name).data.putAll(m);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T remove(Object key) {
            return (T) loadedData.get(name).data.remove(key);
        }

        @Override
        public void clear() {
            loadedData.get(name).data.clear();
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

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Collection<T> values() {
            if (!loadedData.containsKey(name)) return Set.of();
            return loadedData.get(name).data.values().stream().map(v -> (T) v).toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Set<Entry<String, T>> entrySet() {
            if (!loadedData.containsKey(name)) return Set.of();
            return loadedData.get(name).data.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), (T) e.getValue()))
                    .collect(Collectors.toCollection(HashSet::new));
        }
    }
}
