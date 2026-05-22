package net.minecraft.predicate.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Предикат активных эффектов сущности. Проверяет наличие и параметры
 * каждого из заданных эффектов статуса.
 */
public record EntityEffectPredicate(Map<RegistryEntry<StatusEffect>, EntityEffectPredicate.EffectData> effects) {

	public static final Codec<EntityEffectPredicate> CODEC =
			Codec.unboundedMap(StatusEffect.ENTRY_CODEC, EntityEffectPredicate.EffectData.CODEC)
					.xmap(EntityEffectPredicate::new, EntityEffectPredicate::effects);

	public boolean test(Entity entity) {
		return entity instanceof LivingEntity livingEntity && test(livingEntity.getActiveStatusEffects());
	}

	public boolean test(LivingEntity livingEntity) {
		return test(livingEntity.getActiveStatusEffects());
	}

	public boolean test(Map<RegistryEntry<StatusEffect>, StatusEffectInstance> activeEffects) {
		for (Entry<RegistryEntry<StatusEffect>, EntityEffectPredicate.EffectData> entry : effects.entrySet()) {
			StatusEffectInstance instance = activeEffects.get(entry.getKey());

			if (!entry.getValue().test(instance)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Строитель для составления {@link EntityEffectPredicate} с набором проверяемых статус-эффектов.
	 */
	public static class Builder {

		private final ImmutableMap.Builder<RegistryEntry<StatusEffect>, EntityEffectPredicate.EffectData> effects =
				ImmutableMap.builder();

		public static EntityEffectPredicate.Builder create() {
			return new EntityEffectPredicate.Builder();
		}

		public EntityEffectPredicate.Builder addEffect(RegistryEntry<StatusEffect> effect) {
			effects.put(effect, new EntityEffectPredicate.EffectData());
			return this;
		}

		public EntityEffectPredicate.Builder addEffect(
				RegistryEntry<StatusEffect> effect,
				EntityEffectPredicate.EffectData effectData
		) {
			effects.put(effect, effectData);
			return this;
		}

		public Optional<EntityEffectPredicate> build() {
			return Optional.of(new EntityEffectPredicate(effects.build()));
		}
	}

	/**
	 * Данные конкретного эффекта: диапазоны усилителя, длительности и флаги ambient/visible.
	 */
	public record EffectData(
			NumberRange.IntRange amplifier,
			NumberRange.IntRange duration,
			Optional<Boolean> ambient,
			Optional<Boolean> visible
	) {

		public static final Codec<EntityEffectPredicate.EffectData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						NumberRange.IntRange.CODEC
								.optionalFieldOf("amplifier", NumberRange.IntRange.ANY)
								.forGetter(EntityEffectPredicate.EffectData::amplifier),
						NumberRange.IntRange.CODEC
								.optionalFieldOf("duration", NumberRange.IntRange.ANY)
								.forGetter(EntityEffectPredicate.EffectData::duration),
						Codec.BOOL.optionalFieldOf("ambient").forGetter(EntityEffectPredicate.EffectData::ambient),
						Codec.BOOL.optionalFieldOf("visible").forGetter(EntityEffectPredicate.EffectData::visible)
				).apply(instance, EntityEffectPredicate.EffectData::new)
		);

		public EffectData() {
			this(NumberRange.IntRange.ANY, NumberRange.IntRange.ANY, Optional.empty(), Optional.empty());
		}

		public boolean test(@Nullable StatusEffectInstance statusEffectInstance) {
			if (statusEffectInstance == null) {
				return false;
			}

			if (!amplifier.test(statusEffectInstance.getAmplifier())) {
				return false;
			}

			if (!duration.test(statusEffectInstance.getDuration())) {
				return false;
			}

			if (ambient.isPresent() && ambient.get() != statusEffectInstance.isAmbient()) {
				return false;
			}

			return visible.isEmpty() || visible.get() == statusEffectInstance.shouldShowParticles();
		}
	}
}
