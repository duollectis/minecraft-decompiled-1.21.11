package net.minecraft.client.search;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code IdentifierSearcher}.
 */
public interface IdentifierSearcher<T> {

	static <T> IdentifierSearcher<T> of() {
		return new IdentifierSearcher<T>() {
			@Override
			public List<T> searchNamespace(String namespace) {
				return List.of();
			}

			@Override
			public List<T> searchPath(String path) {
				return List.of();
			}
		};
	}

	static <T> IdentifierSearcher<T> of(List<T> values, Function<T, Stream<Identifier>> identifiersGetter) {
		if (values.isEmpty()) {
			return of();
		}
		else {
			final SuffixArray<T> suffixArray = new SuffixArray<>();
			final SuffixArray<T> suffixArray2 = new SuffixArray<>();

			for (T object : values) {
				identifiersGetter.apply(object).forEach(id -> {
					suffixArray.add(object, id.getNamespace().toLowerCase(Locale.ROOT));
					suffixArray2.add(object, id.getPath().toLowerCase(Locale.ROOT));
				});
			}

			suffixArray.build();
			suffixArray2.build();
			return new IdentifierSearcher<T>() {
				@Override
				public List<T> searchNamespace(String namespace) {
					return suffixArray.findAll(namespace);
				}

				@Override
				public List<T> searchPath(String path) {
					return suffixArray2.findAll(path);
				}
			};
		}
	}

	List<T> searchNamespace(String namespace);

	List<T> searchPath(String path);
}
