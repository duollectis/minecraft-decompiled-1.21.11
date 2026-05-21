package net.minecraft.client.search;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * {@code SearchProvider}.
 */
public interface SearchProvider<T> {

	static <T> SearchProvider<T> empty() {
		return string -> List.of();
	}

	static <T> SearchProvider<T> plainText(List<T> list, Function<T, Stream<String>> function) {
		if (list.isEmpty()) {
			return empty();
		}
		else {
			SuffixArray<T> suffixArray = new SuffixArray<>();

			for (T object : list) {
				function.apply(object).forEach(string -> suffixArray.add(object, string.toLowerCase(Locale.ROOT)));
			}

			suffixArray.build();
			return suffixArray::findAll;
		}
	}

	List<T> findAll(String text);
}
