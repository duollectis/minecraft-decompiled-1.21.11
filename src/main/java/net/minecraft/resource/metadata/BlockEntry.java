package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Запись фильтра ресурсов, блокирующая идентификаторы по регулярным выражениям
 * для пространства имён и/или пути.
 *
 * <p>Оба поля опциональны: отсутствующее поле означает «совпадает с любым значением».
 * Итоговый предикат {@link #getIdentifierPredicate()} проверяет оба компонента одновременно.</p>
 */
public class BlockEntry {

	public static final Codec<BlockEntry> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codecs.REGULAR_EXPRESSION.optionalFieldOf("namespace").forGetter(entry -> entry.namespace),
					Codecs.REGULAR_EXPRESSION.optionalFieldOf("path").forGetter(entry -> entry.path)
			).apply(instance, BlockEntry::new)
	);

	private final Optional<Pattern> namespace;
	private final Predicate<String> namespacePredicate;
	private final Optional<Pattern> path;
	private final Predicate<String> pathPredicate;
	private final Predicate<Identifier> identifierPredicate;

	private BlockEntry(Optional<Pattern> namespace, Optional<Pattern> path) {
		this.namespace = namespace;
		this.namespacePredicate = namespace.map(Pattern::asPredicate).orElse(ns -> true);
		this.path = path;
		this.pathPredicate = path.map(Pattern::asPredicate).orElse(p -> true);
		this.identifierPredicate = id -> namespacePredicate.test(id.getNamespace()) && pathPredicate.test(id.getPath());
	}

	public Predicate<String> getNamespacePredicate() {
		return namespacePredicate;
	}

	public Predicate<String> getPathPredicate() {
		return pathPredicate;
	}

	public Predicate<Identifier> getIdentifierPredicate() {
		return identifierPredicate;
	}
}
