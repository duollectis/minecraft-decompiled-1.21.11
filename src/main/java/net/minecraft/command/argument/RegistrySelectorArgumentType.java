package net.minecraft.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code RegistrySelectorArgumentType}.
 */
public class RegistrySelectorArgumentType<T> implements ArgumentType<Collection<RegistryEntry.Reference<T>>> {

	private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
	public static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType(
			(selector, registryRef) -> Text.stringifiedTranslatable(
					"argument.resource_selector.not_found",
					selector,
					registryRef
			)
	);
	final RegistryKey<? extends Registry<T>> registryRef;
	private final RegistryWrapper<T> registry;

	RegistrySelectorArgumentType(CommandRegistryAccess registries, RegistryKey<? extends Registry<T>> registryRef) {
		this.registryRef = registryRef;
		this.registry = registries.getOrThrow(registryRef);
	}

	public Collection<RegistryEntry.Reference<T>> parse(StringReader stringReader) throws CommandSyntaxException {
		String string = addNamespace(read(stringReader));
		List<RegistryEntry.Reference<T>>
				list =
				this.registry.streamEntries().filter(entry -> matches(string, entry.registryKey().getValue())).toList();
		if (list.isEmpty()) {
			throw NOT_FOUND_EXCEPTION.createWithContext(stringReader, string, this.registryRef.getValue());
		}
		else {
			return list;
		}
	}

	public static <T> Collection<RegistryEntry.Reference<T>> select(StringReader reader, RegistryWrapper<T> registry) {
		String string = addNamespace(read(reader));
		return registry.streamEntries().filter(entry -> matches(string, entry.registryKey().getValue())).toList();
	}

	private static String read(StringReader reader) {
		int i = reader.getCursor();

		while (reader.canRead() && isSelectorChar(reader.peek())) {
			reader.skip();
		}

		return reader.getString().substring(i, reader.getCursor());
	}

	private static boolean isSelectorChar(char c) {
		return Identifier.isCharValid(c) || c == '*' || c == '?';
	}

	private static String addNamespace(String path) {
		return !path.contains(":") ? "minecraft:" + path : path;
	}

	private static boolean matches(String selector, Identifier id) {
		return FilenameUtils.wildcardMatch(id.toString(), selector);
	}

	public static <T> RegistrySelectorArgumentType<T> selector(
			CommandRegistryAccess registries,
			RegistryKey<? extends Registry<T>> registryRef
	) {
		return new RegistrySelectorArgumentType<>(registries, registryRef);
	}

	public static <T> Collection<RegistryEntry.Reference<T>> getEntries(
			CommandContext<ServerCommandSource> context,
			String name
	) {
		return (Collection<RegistryEntry.Reference<T>>) context.getArgument(name, Collection.class);
	}

	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.listSuggestions(
				context,
				builder,
				this.registryRef,
				CommandSource.SuggestedIdType.ELEMENTS
		);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/**
	 * {@code Serializer}.
	 */
	public static class Serializer<T> implements ArgumentSerializer<RegistrySelectorArgumentType<T>, RegistrySelectorArgumentType.Serializer<T>.Properties> {

		public void writePacket(
				RegistrySelectorArgumentType.Serializer<T>.Properties properties,
				PacketByteBuf packetByteBuf
		) {
			packetByteBuf.writeRegistryKey(properties.registryRef);
		}

		public RegistrySelectorArgumentType.Serializer<T>.Properties fromPacket(PacketByteBuf packetByteBuf) {
			return new RegistrySelectorArgumentType.Serializer.Properties(packetByteBuf.readRegistryRefKey());
		}

		public void writeJson(RegistrySelectorArgumentType.Serializer<T>.Properties properties, JsonObject jsonObject) {
			jsonObject.addProperty("registry", properties.registryRef.getValue().toString());
		}

		public RegistrySelectorArgumentType.Serializer<T>.Properties getArgumentTypeProperties(
				RegistrySelectorArgumentType<T> registrySelectorArgumentType
		) {
			return new RegistrySelectorArgumentType.Serializer.Properties(registrySelectorArgumentType.registryRef);
		}

		/**
		 * {@code Properties}.
		 */
		public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<RegistrySelectorArgumentType<T>> {

			final RegistryKey<? extends Registry<T>> registryRef;

			Properties(final RegistryKey<? extends Registry<T>> registryRef) {
				this.registryRef = registryRef;
			}

			public RegistrySelectorArgumentType<T> createType(CommandRegistryAccess commandRegistryAccess) {
				return new RegistrySelectorArgumentType<>(commandRegistryAccess, this.registryRef);
			}

			@Override
			public ArgumentSerializer<RegistrySelectorArgumentType<T>, ?> getSerializer() {
				return Serializer.this;
			}
		}
	}
}
