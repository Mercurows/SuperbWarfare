package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


// 屎
// TODO 优化这一坨（至少得支持数组和嵌套对象序列化）
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
//                case List<?> l -> {
//                    buffer.writeVarInt(l.size());
//                    l.forEach(o -> serialize(o, buffer));
//                }

                default -> serialize(value);
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
//            } else if (field.getType().isAssignableFrom(List.class)) {
//                var size = buffer.readVarInt();
//                var list = new ArrayList<>();
//                for (int i = 0; i < size; i++) {
//                    list.add(readFieldByClass(object, field.getGenericType().getClass(), buffer));
//                }
//                setField(object, field, list);
            } else {
                throw new IllegalArgumentException("Non-primary Object not supported");
//                setField(object, field, deserialize(buffer, getField(object, field)));
            }
        });

        return object;
    }

    public static Object readFieldByClass(Object object, Class<?> clazz, FriendlyByteBuf buffer) {
        if (clazz.isAssignableFrom(Byte.class) || clazz.getName().equals("byte")) {
            return buffer.readByte();
        } else if (clazz.isAssignableFrom(Integer.class) || clazz.getName().equals("int")) {
            return buffer.readVarInt();
        } else if (clazz.isAssignableFrom(Long.class) || clazz.getName().equals("long")) {
            return buffer.readLong();
        } else if (clazz.isAssignableFrom(Float.class) || clazz.getName().equals("float")) {
            return buffer.readFloat();
        } else if (clazz.isAssignableFrom(Double.class) || clazz.getName().equals("double")) {
            return buffer.readDouble();
        } else if (clazz.isAssignableFrom(String.class)) {
            return buffer.readUtf();
        } else if (clazz.isAssignableFrom(Boolean.class) || clazz.getName().equals("boolean")) {
            return buffer.readByte();
//        } else if (clazz.isAssignableFrom(List.class)) {
//            var size = buffer.readVarInt();
//            var list = new ArrayList<>();
//            for (int i = 0; i < size; i++) {
//                clazz.getDeclaredConstructors()[0].newInstance()
//                list.add(deserialize(field));
//            }
//            setField(object, field, list);
        } else {
            throw new IllegalArgumentException("Non-primary Object not supported");
//            deserialize(buffer, getField(object, field));
        }
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
