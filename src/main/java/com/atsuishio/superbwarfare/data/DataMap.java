package com.atsuishio.superbwarfare.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// read-only custom data map
public class DataMap<T> extends HashMap<String, T> {
    private final String directory;
    private final Map<String, DataLoader.GeneralData<?>> loadedData;

    DataMap(String directory, Map<String, DataLoader.GeneralData<?>> loadedData) {
        this.directory = directory;
        this.loadedData = loadedData;
    }

    @Override
    public int size() {
        if (!this.loadedData.containsKey(directory)) return 0;
        return this.loadedData.get(directory).getDataMap().size();
    }

    @Override
    public boolean isEmpty() {
        if (!this.loadedData.containsKey(directory)) return true;
        return this.loadedData.get(directory).getDataMap().isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(Object key) {
        if (!this.loadedData.containsKey(directory)) return null;
        return (T) this.loadedData.get(directory).getDataMap().get(key);
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
        return this.loadedData.get(directory).getDataMap().containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T put(String key, T value) {
        return (T) this.loadedData.get(directory).getDataMap().put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> m) {
        this.loadedData.get(directory).getDataMap().putAll(m);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(Object key) {
        return (T) this.loadedData.get(directory).getDataMap().remove(key);
    }

    @Override
    public void clear() {
        this.loadedData.get(directory).getDataMap().clear();
    }

    @Override
    public boolean containsValue(Object value) {
        if (!this.loadedData.containsKey(directory)) return false;
        return this.loadedData.get(directory).getDataMap().containsValue(value);
    }

    @Override
    public @NotNull Set<String> keySet() {
        if (!this.loadedData.containsKey(directory)) return Set.of();
        return this.loadedData.get(directory).getDataMap().keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Collection<T> values() {
        if (!this.loadedData.containsKey(directory)) return Set.of();
        return this.loadedData.get(directory).getDataMap().values().stream().map(v -> (T) v).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Set<Entry<String, T>> entrySet() {
        if (!this.loadedData.containsKey(directory)) return Set.of();
        return this.loadedData.get(directory).getDataMap().entrySet().stream()
                .map(e -> new SimpleImmutableEntry<>(e.getKey(), (T) e.getValue()))
                .collect(Collectors.toCollection(HashSet::new));
    }

}
