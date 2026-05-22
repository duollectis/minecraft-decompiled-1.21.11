package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.context.ContextType;

import java.util.Optional;

/**
 * Запись об эффекте зачарования с опциональным условием применения.
 * Условие ({@code requirements}) проверяется через {@link LootCondition} в контексте лута —
 * если условие не задано, эффект применяется всегда.
 *
 * @param <T> тип эффекта зачарования
 */
public record EnchantmentEffectEntry<T>(T effect, Optional<LootCondition> requirements) {

	/**
	 * Создаёт кодек для валидации условий применения эффекта.
	 * Условие проверяется на корректность относительно указанного типа loot-контекста —
	 * если валидация выявляет ошибки, кодек возвращает {@link DataResult#error}.
	 */
	public static Codec<LootCondition> createRequirementsCodec(ContextType lootContextType) {
		return LootCondition.CODEC
				.validate(condition -> {
					ErrorReporter.Impl reporter = new ErrorReporter.Impl();
					LootTableReporter tableReporter = new LootTableReporter(reporter, lootContextType);
					condition.validate(tableReporter);

					return reporter.isEmpty()
						? DataResult.success(condition)
						: DataResult.error(() -> "Validation error in enchantment effect condition: "
							+ reporter.getErrorsAsString());
				});
	}

	/**
	 * Создаёт кодек для записи эффекта зачарования с опциональным условием.
	 *
	 * @param effectCodec     кодек конкретного типа эффекта
	 * @param lootContextType тип loot-контекста для валидации условий
	 */
	public static <T> Codec<EnchantmentEffectEntry<T>> createCodec(
			Codec<T> effectCodec,
			ContextType lootContextType
	) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						effectCodec.fieldOf("effect").forGetter(EnchantmentEffectEntry::effect),
						createRequirementsCodec(lootContextType)
								.optionalFieldOf("requirements")
								.forGetter(EnchantmentEffectEntry::requirements)
				).apply(instance, EnchantmentEffectEntry::new)
		);
	}

	/**
	 * Проверяет, выполнено ли условие применения эффекта в данном loot-контексте.
	 * Если условие не задано — всегда возвращает {@code true}.
	 */
	public boolean test(LootContext context) {
		return requirements.isEmpty() || requirements.get().test(context);
	}
}
