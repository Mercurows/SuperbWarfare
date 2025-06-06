package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BufferSerializer {
    public static List<Field> sortedFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(ServerOnly.class)
                        && !Modifier.isTransient(f.getModifiers())
                        && !f.getType().isAssignableFrom(Annotation.class)
                )
                .sorted(Comparator.comparing(Field::getName))
                .toList();
    }

    public static List<Field> sortedFields(Object object) {
        return sortedFields(object.getClass());
    }

    public static List<Object> fieldValuesList(Object object) {
        var fields = new ArrayList<>();

        for (var field : sortedFields(object)) {
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                fields.add(value);
            } catch (IllegalAccessException e) {
                Mod.LOGGER.error("BufferSerializer read error: {}", e.getMessage());
            }
        }
        return fields;
    }

    private static final Gson gson = new Gson();

    public static FriendlyByteBuf serialize(Object object) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var fields = fieldValuesList(object);

        fields.forEach(value -> {
            switch (value) {
                case Byte b -> buffer.writeByte(b);
                case Integer i -> buffer.writeVarInt(i);
                case Long l -> buffer.writeLong(l);
                case Float f -> buffer.writeFloat(f);
                case Double d -> buffer.writeDouble(d);
                case String s -> buffer.writeUtf(s);
                case Boolean b -> buffer.writeBoolean(b);

                default -> buffer.writeUtf(gson.toJson(value));
            }
        });

        return buffer;
    }

    public static <T> T deserialize(FriendlyByteBuf buffer, T object) {
        sortedFields(object).forEach(field -> {
            if (field.getType().isAssignableFrom(Byte.class) || field.getType().getName().equals("byte")) {
                setField(object, field, buffer.readByte());
            } else if (field.getType().isAssignableFrom(Integer.class) || field.getType().getName().equals("int")) {
                setField(object, field, buffer.readVarInt());
            } else if (field.getType().isAssignableFrom(Long.class) || field.getType().getName().equals("long")) {
                setField(object, field, buffer.readLong());
            } else if (field.getType().isAssignableFrom(Float.class) || field.getType().getName().equals("float")) {
                setField(object, field, buffer.readFloat());
            } else if (field.getType().isAssignableFrom(Double.class) || field.getType().getName().equals("double")) {
                setField(object, field, buffer.readDouble());
            } else if (field.getType().isAssignableFrom(String.class)) {
                setField(object, field, buffer.readUtf());
            } else if (field.getType().isAssignableFrom(Boolean.class) || field.getType().getName().equals("boolean")) {
                setField(object, field, buffer.readBoolean());
            } else {
                setField(object, field, gson.fromJson(buffer.readUtf(), field.getGenericType()));
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
