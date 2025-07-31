package com.atsuishio.superbwarfare.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 用于指令的枚举类型参数，会把枚举常量的名称转换为小驼峰形式
 * <p>
 * Code based on <a href= https://github.com/Apocalypse114/CaerulaArbor>CaerulaArbor</a>
 *
 * @author Mercurows
 */
public class LowerCamelCaseEnumArgument<T extends Enum<T>> implements ArgumentType<T> {

    private static final Dynamic2CommandExceptionType INVALID_ENUM =
            new Dynamic2CommandExceptionType((found, constants) -> Component.translatable("commands.neoforge.arguments.enum.invalid", constants, found));

    public final Function<T, String> valueMapper = e -> {
        var input = e.name();
        if (input.trim().isEmpty()) {
            return input;
        }

        String trimmed = input.replaceAll("^_+|_+$", "");
        if (trimmed.isEmpty()) {
            return input;
        }
        String[] parts = trimmed.toLowerCase(Locale.ENGLISH).split("_+");

        StringBuilder result = new StringBuilder();
        result.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                        .append(parts[i].substring(1));
            }
        }
        return result.toString();
    };

    private final Class<T> enumClass;

    private LowerCamelCaseEnumArgument(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    public static <T extends Enum<T>> LowerCamelCaseEnumArgument<T> enumArgument(Class<T> enumClass) {
        return new LowerCamelCaseEnumArgument<>(enumClass);
    }

    @Override
    public T parse(final StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();

        for (T enumValue : enumClass.getEnumConstants()) {
            if (valueMapper.apply(enumValue).equals(input)) {
                return enumValue;
            }
        }
        throw INVALID_ENUM.createWithContext(reader, input, Arrays.toString(Arrays.stream(enumClass.getEnumConstants()).map(valueMapper).toArray()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(enumClass.getEnumConstants()).map(valueMapper).toList(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(enumClass.getEnumConstants()).map(valueMapper).toList();
    }

    public static class Info<T extends Enum<T>> implements ArgumentTypeInfo<LowerCamelCaseEnumArgument<T>, Info<T>.Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
            buffer.writeUtf(template.enumClass.getName());
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            try {
                String name = buffer.readUtf();
                return new Template((Class<T>) Class.forName(name));
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("enum", template.enumClass.getName());
        }

        @Override
        public @NotNull Template unpack(LowerCamelCaseEnumArgument<T> argument) {
            return new Template(argument.enumClass);
        }

        public class Template implements ArgumentTypeInfo.Template<LowerCamelCaseEnumArgument<T>> {
            final Class<T> enumClass;

            Template(Class<T> enumClass) {
                this.enumClass = enumClass;
            }

            @Override
            public @NotNull LowerCamelCaseEnumArgument<T> instantiate(@NotNull CommandBuildContext pStructure) {
                return new LowerCamelCaseEnumArgument<>(this.enumClass);
            }

            @Override
            public @NotNull ArgumentTypeInfo<LowerCamelCaseEnumArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}
