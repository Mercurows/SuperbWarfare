package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
public class DataLoader {

    private static final Map<ResourceLocation, GeneralData<?>> LOADED_DATA = new HashMap<>();
    private static final Map<ResourceLocation, GeneralData<?>> LOADED_RESOURCE = new HashMap<>();

    public record GeneralData<T>(
            Class<?> type, DataMap<T> proxyMap,
            HashMap<String, Object> data,
            @Nullable Consumer<Map<String, Object>> onReload
    ) {
    }

    public static final ComplexJsonResourceReloadListener SERVER_LISTENER = new ComplexJsonResourceReloadListener(LOADED_DATA);
    public static final ComplexJsonResourceReloadListener CLIENT_LISTENER = new ComplexJsonResourceReloadListener(LOADED_RESOURCE);

    @SubscribeEvent
    public static void addDataReloadListener(AddReloadListenerEvent event) {
        event.addListener(SERVER_LISTENER);
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    static class ClientReloadListener {
        @SubscribeEvent
        public static void addResourceReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(CLIENT_LISTENER);
        }
    }

    public static <T> DataMap<T> createData(String namespace, String directory, Class<T> clazz) {
        return createData(namespace, directory, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataMap<T> createData(String namespace, String directory, Class<T> clazz, @Nullable Consumer<Map<String, Object>> onReload) {
        var loc = new ResourceLocation(namespace, directory);
        if (LOADED_DATA.containsKey(loc)) {
            return (DataMap<T>) LOADED_DATA.get(loc).proxyMap;
        } else {
            var proxyMap = new DataMap<T>(new ResourceLocation(namespace, directory), LOADED_DATA);
            LOADED_DATA.put(loc, new GeneralData<>(clazz, proxyMap, new HashMap<>(), onReload));
            return proxyMap;
        }
    }

    public static <T> DataMap<T> createResource(String namespace, String directory, Class<T> clazz) {
        return createResource(namespace, directory, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataMap<T> createResource(String namespace, String directory, Class<T> clazz, @Nullable Consumer<Map<String, Object>> onReload) {
        var loc = new ResourceLocation(namespace, directory);
        if (LOADED_RESOURCE.containsKey(loc)) {
            return (DataMap<T>) LOADED_RESOURCE.get(loc).proxyMap;
        } else {
            var proxyMap = new DataMap<T>(new ResourceLocation(namespace, directory), LOADED_RESOURCE);
            LOADED_RESOURCE.put(loc, new GeneralData<>(clazz, proxyMap, new HashMap<>(), onReload));
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
                .registerTypeAdapter(Vec2.class, new Vec2Adapter())
                .registerTypeAdapter(ModColor.class, new ModColor.ModColorAdapter())
                .registerTypeAdapter(StringOrVec3.class, new StringOrVec3.StringOrVec3Adapter())
                .registerTypeAdapterFactory(new ObjectToList.AdapterFactory())
                .registerTypeAdapterFactory(new StringToObject.AdapterFactory());
    }

    public static final Gson GSON = createCommonBuilder().create();

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

    public static class DataMap<T> extends HashMap<String, T> {
        private final ResourceLocation location;
        private final Map<ResourceLocation, GeneralData<?>> loadedData;

        private DataMap(ResourceLocation location, Map<ResourceLocation, GeneralData<?>> loadedData) {
            this.location = location;
            this.loadedData = loadedData;
        }

        @Override
        public int size() {
            if (!this.loadedData.containsKey(location)) return 0;
            return this.loadedData.get(location).data.size();
        }

        @Override
        public boolean isEmpty() {
            if (!this.loadedData.containsKey(location)) return true;
            return this.loadedData.get(location).data.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(Object key) {
            if (!this.loadedData.containsKey(location)) return null;
            return (T) this.loadedData.get(location).data.get(key);
        }

        @Override
        public T getOrDefault(Object key, T defaultValue) {
            var value = get(key);
            return value == null ? defaultValue : value;
        }

        @Override
        public boolean containsKey(Object key) {
            if (!this.loadedData.containsKey(location)) return false;
            return this.loadedData.get(location).data.containsKey(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T put(String key, T value) {
            return (T) this.loadedData.get(location).data.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends T> m) {
            this.loadedData.get(location).data.putAll(m);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T remove(Object key) {
            return (T) this.loadedData.get(location).data.remove(key);
        }

        @Override
        public void clear() {
            this.loadedData.get(location).data.clear();
        }

        @Override
        public boolean containsValue(Object value) {
            if (!this.loadedData.containsKey(location)) return false;
            return this.loadedData.get(location).data.containsValue(value);
        }

        @Override
        public @NotNull Set<String> keySet() {
            if (!this.loadedData.containsKey(location)) return Set.of();
            return this.loadedData.get(location).data.keySet();
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Collection<T> values() {
            if (!this.loadedData.containsKey(location)) return Set.of();
            return this.loadedData.get(location).data.values().stream().map(v -> (T) v).toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Set<Entry<String, T>> entrySet() {
            if (!this.loadedData.containsKey(location)) return Set.of();
            return this.loadedData.get(location).data.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), (T) e.getValue()))
                    .collect(Collectors.toCollection(HashSet::new));
        }

        public ResourceLocation getLocation() {
            return location;
        }
    }
}
