package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentPredicate;

/**
 * Предикат для проверки прочности предмета: текущего урона и оставшейся прочности.
 */
public record DamagePredicate(
		NumberRange.IntRange durability,
		NumberRange.IntRange damage
) implements ComponentPredicate {

	public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					NumberRange.IntRange.CODEC
							.optionalFieldOf("durability", NumberRange.IntRange.ANY)
							.forGetter(DamagePredicate::durability),
					NumberRange.IntRange.CODEC
							.optionalFieldOf("damage", NumberRange.IntRange.ANY)
							.forGetter(DamagePredicate::damage)
			)
			.apply(instance, DamagePredicate::new)
	);

	public static DamagePredicate durability(NumberRange.IntRange durability) {
		return new DamagePredicate(durability, NumberRange.IntRange.ANY);
	}

	@Override
	public boolean test(ComponentsAccess components) {
		Integer currentDamage = components.get(DataComponentTypes.DAMAGE);

		if (currentDamage == null) {
			return false;
		}

		int maxDamage = components.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);

		if (!durability.test(maxDamage - currentDamage)) {
			return false;
		}

		return damage.test(currentDamage);
	}
}
