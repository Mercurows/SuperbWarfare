package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.vehicle.subdata.CollisionLevel;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
public class DataLoader {

    public static final Gson GSON = createCommonBuilder().create();

    public static final LoadingCache<Object, JsonObject> JSON_OBJECT_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<>() {
                public @NotNull JsonObject load(@NotNull Object object) {
                    return DataLoader.GSON.toJsonTree(object).getAsJsonObject();
                }
            });

    private static final Map<String, GeneralData<?>> LOADED_DATA = new HashMap<>();
    private static final Map<String, GeneralData<?>> LOADED_RESOURCE = new HashMap<>();

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

    public static <T> DataMap<T> createData(String directory, Class<T> clazz) {
        return createData(directory, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataMap<T> createData(String directory, Class<T> clazz, @Nullable Consumer<Map<String, Object>> onReload) {
        if (LOADED_DATA.containsKey(directory)) {
            return (DataMap<T>) LOADED_DATA.get(directory).proxyMap;
        } else {
            var proxyMap = new DataMap<T>(directory, LOADED_DATA);
            LOADED_DATA.put(directory, new GeneralData<>(clazz, proxyMap, new HashMap<>(), onReload));
            return proxyMap;
        }
    }

    public static <T> DataMap<T> createResource(String directory, Class<T> clazz) {
        return createResource(directory, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataMap<T> createResource(String directory, Class<T> clazz, @Nullable Consumer<Map<String, Object>> onReload) {
        if (LOADED_RESOURCE.containsKey(directory)) {
            return (DataMap<T>) LOADED_RESOURCE.get(directory).proxyMap;
        } else {
            var proxyMap = new DataMap<T>(directory, LOADED_RESOURCE);
            LOADED_RESOURCE.put(directory, new GeneralData<>(clazz, proxyMap, new HashMap<>(), onReload));
            return proxyMap;
        }
    }

    // 务必在所有需要序列化GSON数据的地方调用，避免报错
    public static GsonBuilder createCommonBuilder() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setLenient()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapter(Vec2.class, new Vec2Adapter())
                .registerTypeAdapter(Vec3.class, new Vec3Adapter())
                .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
                .registerTypeAdapter(SoundEvent.class, new SoundEventAdapter())
                .registerTypeAdapter(ModColor.class, new ModColor.ModColorAdapter())
                .registerTypeAdapter(StringOrVec3.class, new StringOrVec3.StringOrVec3Adapter())
                .registerTypeAdapter(CollisionLevel.Limit.class, new CollisionLevel.LimitAdapter())
                .registerTypeAdapterFactory(new ObjectToList.AdapterFactory())
                .registerTypeAdapterFactory(new StringToObject.AdapterFactory());
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

    public static class DataMap<T> extends HashMap<String, T> {
        private final String directory;
        private final Map<String, GeneralData<?>> loadedData;

        private DataMap(String directory, Map<String, GeneralData<?>> loadedData) {
            this.directory = directory;
            this.loadedData = loadedData;
        }

        @Override
        public int size() {
            if (!this.loadedData.containsKey(directory)) return 0;
            return this.loadedData.get(directory).data.size();
        }

        @Override
        public boolean isEmpty() {
            if (!this.loadedData.containsKey(directory)) return true;
            return this.loadedData.get(directory).data.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(Object key) {
            if (!this.loadedData.containsKey(directory)) return null;
            return (T) this.loadedData.get(directory).data.get(key);
        }

        @Override
        public T getOrDefault(Object key, T defaultValue) {
            var value = get(key);
            return value == null ? defaultValue : value;
        }

        public T getOrElseGet(Object key, Supplier<T> supplier) {
            var value = get(key);
            return value == null ? supplier.get() : value;
        }

        @Override
        public boolean containsKey(Object key) {
            if (!this.loadedData.containsKey(directory)) return false;
            return this.loadedData.get(directory).data.containsKey(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T put(String key, T value) {
            return (T) this.loadedData.get(directory).data.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ? extends T> m) {
            this.loadedData.get(directory).data.putAll(m);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T remove(Object key) {
            return (T) this.loadedData.get(directory).data.remove(key);
        }

        @Override
        public void clear() {
            this.loadedData.get(directory).data.clear();
        }

        @Override
        public boolean containsValue(Object value) {
            if (!this.loadedData.containsKey(directory)) return false;
            return this.loadedData.get(directory).data.containsValue(value);
        }

        @Override
        public @NotNull Set<String> keySet() {
            if (!this.loadedData.containsKey(directory)) return Set.of();
            return this.loadedData.get(directory).data.keySet();
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Collection<T> values() {
            if (!this.loadedData.containsKey(directory)) return Set.of();
            return this.loadedData.get(directory).data.values().stream().map(v -> (T) v).toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Set<Entry<String, T>> entrySet() {
            if (!this.loadedData.containsKey(directory)) return Set.of();
            return this.loadedData.get(directory).data.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), (T) e.getValue()))
                    .collect(Collectors.toCollection(HashSet::new));
        }

        public String getDirectory() {
            return directory;
        }
    }
}
