package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.context.ContextType;

import java.util.Optional;

/**
 * Эффект зачарования с явным указанием ролей: кто является носителем зачарования ({@code enchanted})
 * и кто получает эффект ({@code affected}). Используется в компонентах {@code POST_ATTACK}
 * и {@code EQUIPMENT_DROPS} для разграничения атакующего, жертвы и источника урона.
 *
 * @param <T> тип применяемого эффекта
 */
public record TargetedEnchantmentEffect<T>(
		EnchantmentEffectTarget enchanted,
		EnchantmentEffectTarget affected,
		T effect,
		Optional<LootCondition> requirements
) {

	/**
	 * Создаёт кодек для эффектов типа POST_ATTACK, где допустимы все три роли:
	 * {@code ATTACKER}, {@code VICTIM} и {@code DAMAGING_ENTITY}.
	 */
	public static <S> Codec<TargetedEnchantmentEffect<S>> createPostAttackCodec(
			Codec<S> effectCodec,
			ContextType lootContextType
	) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						EnchantmentEffectTarget.CODEC
								.fieldOf("enchanted")
								.forGetter(TargetedEnchantmentEffect::enchanted),
						EnchantmentEffectTarget.CODEC
								.fieldOf("affected")
								.forGetter(TargetedEnchantmentEffect::affected),
						effectCodec.fieldOf("effect").forGetter(TargetedEnchantmentEffect::effect),
						EnchantmentEffectEntry.createRequirementsCodec(lootContextType)
								.optionalFieldOf("requirements")
								.forGetter(TargetedEnchantmentEffect::requirements)
				).apply(instance, TargetedEnchantmentEffect::new)
		);
	}

	/**
	 * Создаёт кодек для эффектов типа EQUIPMENT_DROPS, где роль {@code enchanted} ограничена
	 * только {@code ATTACKER} или {@code VICTIM} (не {@code DAMAGING_ENTITY}).
	 * Роль {@code affected} всегда фиксирована как {@code VICTIM}.
	 */
	public static <S> Codec<TargetedEnchantmentEffect<S>> createEquipmentDropsCodec(
			Codec<S> effectCodec,
			ContextType lootContextType
	) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						EnchantmentEffectTarget.CODEC
								.validate(enchanted -> enchanted != EnchantmentEffectTarget.DAMAGING_ENTITY
									? DataResult.success(enchanted)
									: DataResult.error(() -> "enchanted must be attacker or victim")
								)
								.fieldOf("enchanted")
								.forGetter(TargetedEnchantmentEffect::enchanted),
						effectCodec.fieldOf("effect").forGetter(TargetedEnchantmentEffect::effect),
						EnchantmentEffectEntry.createRequirementsCodec(lootContextType)
								.optionalFieldOf("requirements")
								.forGetter(TargetedEnchantmentEffect::requirements)
				).apply(
						instance,
						(enchanted, effect, requirements) -> new TargetedEnchantmentEffect<>(
								enchanted,
								EnchantmentEffectTarget.VICTIM,
								effect,
								requirements
						)
				)
		);
	}

	/**
	 * Проверяет, выполнено ли условие применения эффекта в данном loot-контексте.
	 * Если условие не задано — всегда возвращает {@code true}.
	 */
	public boolean test(LootContext lootContext) {
		return requirements.isEmpty() || requirements.get().test(lootContext);
	}
}
