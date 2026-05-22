package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.predicate.component.ComponentSubPredicate;

import java.util.Optional;

/**
 * Предикат для проверки компонента взрыва фейерверка.
 */
public record FireworkExplosionPredicate(
		FireworkExplosionPredicate.Predicate predicate
) implements ComponentSubPredicate<FireworkExplosionComponent> {

	public static final Codec<FireworkExplosionPredicate> CODEC = FireworkExplosionPredicate.Predicate.CODEC
			.xmap(FireworkExplosionPredicate::new, FireworkExplosionPredicate::predicate);

	@Override
	public ComponentType<FireworkExplosionComponent> getComponentType() {
		return DataComponentTypes.FIREWORK_EXPLOSION;
	}

	public boolean test(FireworkExplosionComponent component) {
		return predicate.test(component);
	}

	/**
	 * Внутренний предикат с параметрами взрыва: форма, мерцание, след.
	 */
	public record Predicate(
			Optional<FireworkExplosionComponent.Type> shape,
			Optional<Boolean> twinkle,
			Optional<Boolean> trail
	) implements java.util.function.Predicate<FireworkExplosionComponent> {

		public static final Codec<FireworkExplosionPredicate.Predicate> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						FireworkExplosionComponent.Type.CODEC
								.optionalFieldOf("shape")
								.forGetter(FireworkExplosionPredicate.Predicate::shape),
						Codec.BOOL
								.optionalFieldOf("has_twinkle")
								.forGetter(FireworkExplosionPredicate.Predicate::twinkle),
						Codec.BOOL
								.optionalFieldOf("has_trail")
								.forGetter(FireworkExplosionPredicate.Predicate::trail)
				)
				.apply(instance, FireworkExplosionPredicate.Predicate::new)
		);

		public boolean test(FireworkExplosionComponent component) {
			if (shape.isPresent() && shape.get() != component.shape()) {
				return false;
			}

			if (twinkle.isPresent() && twinkle.get() != component.hasTwinkle()) {
				return false;
			}

			return trail.isEmpty() || trail.get() == component.hasTrail();
		}
	}
}
