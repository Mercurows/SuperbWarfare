package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class Prop<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD> {
    public static final List<Prop<?, ?, ?>> props = new ArrayList<>();

    public final Class<DEFAULT_DATA> rawDataType;
    public final String name;
    protected final Field field;

    protected Prop(Class<DEFAULT_DATA> rawDataType, String name) {
        this.rawDataType = rawDataType;
        this.name = name;

        try {
            this.field = Arrays.stream(this.rawDataType.getFields())
                    .filter(f -> {
                        var annotation = f.getAnnotation(SerializedName.class);
                        return annotation != null && annotation.value().equals(this.name);
                    })
                    .findFirst()
                    .orElseThrow();
            this.field.setAccessible(true);
        } catch (Exception exception) {
            Mod.LOGGER.error("Could not find field {} in RAW_DATA!", name);
            throw new RuntimeException(exception);
        }

        props.add(this);
    }

    public Function<DEFAULT_DATA, FIELD> specialSupplier;
    public PropModifyContext<DATA, FIELD> limiter;

    public Type getFieldType() {
        return this.field.getGenericType();
    }

    @SuppressWarnings("unchecked")
    protected <T extends Prop<DATA, DEFAULT_DATA, FIELD>> T withSupplier(Function<DEFAULT_DATA, FIELD> supplier) {
        this.specialSupplier = supplier;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Prop<DATA, DEFAULT_DATA, FIELD>> T withLimiter(Prop.PropModifyContext<DATA, FIELD> limiter) {
        this.limiter = limiter;
        return (T) this;
    }

    protected <T extends Prop<DATA, DEFAULT_DATA, FIELD>> T withLimiter(Function<FIELD, FIELD> limiter) {
        return withLimiter((data, value) -> limiter.apply(value));
    }

    @SuppressWarnings("unchecked")
    public FIELD getDefault(DEFAULT_DATA data) {
        if (this.specialSupplier != null) {
            return specialSupplier.apply(data);
        }

        try {
            return (FIELD) DataLoader.processValue(field.get(data));
        } catch (Exception exception) {
            Mod.LOGGER.error("Could not get field {} in RAW_DATA!", name);
            throw new RuntimeException(exception);
        }
    }

    public PropModifier asModifier(DATA data) {
        return new PropModifier(data, this.getDefault(data.getDefault()), limiter);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Prop<?, ?, ?>> @Nullable T getByName(String name) {
        return (T) props.stream().filter(p -> p.name.equals(name)).findFirst().orElse(null);
    }


    @FunctionalInterface
    public interface PropModifyContext<DATA, FIELD> {
        FIELD apply(@NotNull DATA data, @NotNull FIELD value);
    }

    public class PropModifier {
        private final DATA data;
        private final FIELD value;
        private final PropModifyContext<DATA, FIELD> limiter;

        private final List<PropModifyContext<DATA, FIELD>> modifiers = new ArrayList<>();

        private PropModifier(DATA data, FIELD value, @Nullable PropModifyContext<DATA, FIELD> limiter) {
            this.data = data;
            this.value = value;
            this.limiter = limiter;
        }

        public PropModifier apply(@Nullable List<PropModifyContext<DATA, FIELD>> modifiers) {
            if (modifiers == null) return this;

            for (var modifier : modifiers) {
                apply(modifier);
            }
            return this;
        }

        public PropModifier apply(@Nullable PropModifyContext<DATA, FIELD> modifier) {
            if (modifier == null) return this;

            modifiers.add(modifier);
            return this;
        }

        public PropModifier override(@Nullable FIELD value) {
            if (value == null) return this;

            modifiers.add((data, v) -> value);
            return this;
        }

        @SuppressWarnings("unchecked")
        public FIELD compute() {
            var result = value;

            for (var modifier : modifiers) {
                result = modifier.apply(data, result);
            }

            if (limiter != null) {
                result = limiter.apply(data, result);
            }

            return (FIELD) DataLoader.processValue(result);
        }
    }
}
