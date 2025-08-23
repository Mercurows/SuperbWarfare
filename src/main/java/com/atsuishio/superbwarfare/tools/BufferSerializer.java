package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BufferSerializer {
    public static List<Field> sortedFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(ServerOnly.class) && !f.getType().isAssignableFrom(Annotation.class))
                .sorted(Comparator.comparing(Field::getName))
                .toList();
    }

    public static List<Field> sortedFields(Object object) {
        return sortedFields(object.getClass());
    }

    public static List<Pair<Object, Field>> fieldValuesList(Object object) {
        var fields = new ArrayList<Pair<Object, Field>>();

        for (var field : sortedFields(object)) {
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                fields.add(new Pair<>(value, field));
            } catch (IllegalAccessException e) {
                Mod.LOGGER.error("BufferSerializer read error: {}", e.getMessage());
            }
        }
        return fields;
    }

    private static final Gson GSON = DataLoader.createCommonBuilder()
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getAnnotation(ServerOnly.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();

    public static FriendlyByteBuf serialize(Object object) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());

        fieldValuesList(object).forEach(fieldValue -> {
            var value = fieldValue.getFirst();
            var field = fieldValue.getSecond();

            if (value instanceof Byte b) {
                buffer.writeByte(b);
            } else if (value instanceof Integer i) {
                buffer.writeVarInt(i);
            } else if (value instanceof Long l) {
                buffer.writeLong(l);
            } else if (value instanceof Float f) {
                buffer.writeFloat(f);
            } else if (value instanceof Double d) {
                buffer.writeDouble(d);
            } else if (value instanceof String s) {
                buffer.writeUtf(s);
            } else if (value instanceof Boolean b) {
                buffer.writeBoolean(b);
            } else {
                buffer.writeUtf(GSON.toJson(value, field.getGenericType()));
            }
        });

        return buffer;
    }

    public static <T> T deserialize(FriendlyByteBuf buffer, T object) {
        sortedFields(object).forEach(field -> {
            var classType = field.getType();
            if (byte.class.isAssignableFrom(classType) || Byte.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readByte());
            } else if (int.class.isAssignableFrom(classType) || Integer.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readVarInt());
            } else if (long.class.isAssignableFrom(classType) || Long.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readLong());
            } else if (float.class.isAssignableFrom(classType) || Float.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readFloat());
            } else if (double.class.isAssignableFrom(classType) || Double.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readDouble());
            } else if (boolean.class.isAssignableFrom(classType) || Boolean.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readBoolean());
            } else if (String.class.isAssignableFrom(classType)) {
                setField(object, field, buffer.readUtf());
            } else {
                setField(object, field, GSON.fromJson(buffer.readUtf(), field.getGenericType()));
            }
        });

        return object;
    }

    public static void setField(Object object, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException e) {
            Mod.LOGGER.error("BufferSerializer write error: {}", e.getMessage());
        }
    }
}
