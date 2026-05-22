package net.minecraft.client.gl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Набор препроцессорных директив GLSL: именованные значения ({@code #define KEY VALUE})
 * и флаги ({@code #define FLAG}). Используется при компиляции шейдеров для передачи
 * конфигурационных параметров.
 */
@Environment(EnvType.CLIENT)
public record Defines(Map<String, String> values, Set<String> flags) {

	public static final Defines EMPTY = new Defines(Map.of(), Set.of());
	public static final Codec<Defines> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Codec.STRING)
				.optionalFieldOf("values", Map.of())
				.forGetter(Defines::values),
			Codec.STRING
				.listOf()
				.xmap(Set::copyOf, List::copyOf)
				.optionalFieldOf("flags", Set.of())
				.forGetter(Defines::flags)
		).apply(instance, Defines::new)
	);

	public static Defines.Builder builder() {
		return new Defines.Builder();
	}

	/**
	 * Объединяет текущий набор директив с {@code other}, возвращая новый экземпляр.
	 * При конфликте ключей побеждает значение из {@code other} (keepingLast).
	 */
	public Defines withMerged(Defines other) {
		if (isEmpty()) {
			return other;
		}

		if (other.isEmpty()) {
			return this;
		}

		ImmutableMap.Builder<String, String> mergedValues =
			ImmutableMap.builderWithExpectedSize(values.size() + other.values.size());
		mergedValues.putAll(values);
		mergedValues.putAll(other.values);

		ImmutableSet.Builder<String> mergedFlags =
			ImmutableSet.builderWithExpectedSize(flags.size() + other.flags.size());
		mergedFlags.addAll(flags);
		mergedFlags.addAll(other.flags);

		return new Defines(mergedValues.buildKeepingLast(), mergedFlags.build());
	}

	/**
	 * Сериализует все директивы в строку GLSL-препроцессора.
	 * Именованные значения выводятся как {@code #define KEY VALUE\n},
	 * флаги — как {@code #define FLAG\n}.
	 */
	public String toSource() {
		StringBuilder source = new StringBuilder();

		for (Entry<String, String> entry : values.entrySet()) {
			source.append("#define ").append(entry.getKey()).append(" ").append(entry.getValue()).append('\n');
		}

		for (String flag : flags) {
			source.append("#define ").append(flag).append('\n');
		}

		return source.toString();
	}

	public boolean isEmpty() {
		return values.isEmpty() && flags.isEmpty();
	}

	/**
	 * Строитель для пошагового формирования набора директив {@link Defines}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
		private final ImmutableSet.Builder<String> flags = ImmutableSet.builder();

		Builder() {
		}

		public Defines.Builder define(String key, String value) {
			if (value.isBlank()) {
				throw new IllegalArgumentException("Cannot define empty string");
			}

			values.put(key, escapeLinebreak(value));
			return this;
		}

		public Defines.Builder define(String key, float value) {
			values.put(key, String.valueOf(value));
			return this;
		}

		public Defines.Builder define(String key, int value) {
			values.put(key, String.valueOf(value));
			return this;
		}

		public Defines.Builder flag(String flag) {
			flags.add(flag);
			return this;
		}

		public Defines build() {
			return new Defines(values.build(), flags.build());
		}

		private static String escapeLinebreak(String value) {
			return value.replaceAll("\n", "\\\\\n");
		}
	}
}
