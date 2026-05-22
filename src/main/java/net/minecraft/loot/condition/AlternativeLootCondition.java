package net.minecraft.loot.condition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Базовый класс для составных условий лута (AND/OR).
 *
 * <p>Хранит список дочерних условий и скомпилированный предикат,
 * который вычисляется один раз при создании объекта.</p>
 */
public abstract class AlternativeLootCondition implements LootCondition {

	protected final List<LootCondition> terms;
	private final Predicate<LootContext> predicate;

	protected AlternativeLootCondition(List<LootCondition> terms, Predicate<LootContext> predicate) {
		this.terms = terms;
		this.predicate = predicate;
	}

	/**
	 * Создаёт {@link MapCodec} для подкласса, сериализующий список условий в поле {@code "terms"}.
	 */
	protected static <T extends AlternativeLootCondition> MapCodec<T> createCodec(
		Function<List<LootCondition>, T> termsToCondition
	) {
		return RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(LootCondition.CODEC.listOf().fieldOf("terms").forGetter(condition -> condition.terms))
				.apply(instance, termsToCondition)
		);
	}

	/**
	 * Создаёт инлайн-кодек, сериализующий условие как простой список (без обёртки в объект).
	 */
	protected static <T extends AlternativeLootCondition> Codec<T> createInlineCodec(
		Function<List<LootCondition>, T> termsToCondition
	) {
		return LootCondition.CODEC.listOf().xmap(termsToCondition, condition -> condition.terms);
	}

	public final boolean test(LootContext lootContext) {
		return predicate.test(lootContext);
	}

	@Override
	public void validate(LootTableReporter reporter) {
		LootCondition.super.validate(reporter);

		for (int index = 0; index < terms.size(); index++) {
			terms.get(index).validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("terms", index)));
		}
	}

	/** Базовый строитель для составных условий. */
	public abstract static class Builder implements LootCondition.Builder {

		private final ImmutableList.Builder<LootCondition> terms = ImmutableList.builder();

		protected Builder(LootCondition.Builder... builders) {
			for (LootCondition.Builder builder : builders) {
				terms.add(builder.build());
			}
		}

		public void add(LootCondition.Builder builder) {
			terms.add(builder.build());
		}

		@Override
		public LootCondition build() {
			return build(terms.build());
		}

		protected abstract LootCondition build(List<LootCondition> terms);
	}
}
