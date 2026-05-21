package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentPredicate;

/**
 * {@code DamagePredicate}.
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

	@Override
	public boolean test(ComponentsAccess components) {
		Integer integer = components.get(DataComponentTypes.DAMAGE);
		if (integer == null) {
			return false;
		}
		else {
			int i = components.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
			return !this.durability.test(i - integer) ? false : this.damage.test(integer);
		}
	}

	public static DamagePredicate durability(NumberRange.IntRange durability) {
		return new DamagePredicate(durability, NumberRange.IntRange.ANY);
	}
}
