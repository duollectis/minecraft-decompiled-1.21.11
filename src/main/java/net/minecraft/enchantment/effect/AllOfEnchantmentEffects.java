package net.minecraft.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.function.Function;

/**
 * Составной эффект зачарования, последовательно применяющий список дочерних эффектов.
 * Реализует паттерн «Composite» для всех трёх типов эффектов зачарования.
 */
public interface AllOfEnchantmentEffects {

	/**
	 * Строит {@link MapCodec} для составного эффекта на основе кодека базового типа.
	 *
	 * @param baseCodec кодек базового типа эффекта
	 * @param fromList  фабрика составного эффекта из списка
	 * @param toList    геттер списка дочерних эффектов
	 */
	static <T, A extends T> MapCodec<A> buildCodec(
			Codec<T> baseCodec,
			Function<List<T>, A> fromList,
			Function<A, List<T>> toList
	) {
		return RecordCodecBuilder.mapCodec(instance -> instance
				.group(baseCodec.listOf().fieldOf("effects").forGetter(toList))
				.apply(instance, fromList));
	}

	static EntityEffects allOf(EnchantmentEntityEffect... entityEffects) {
		return new EntityEffects(List.of(entityEffects));
	}

	static LocationBasedEffects allOf(EnchantmentLocationBasedEffect... locationBasedEffects) {
		return new LocationBasedEffects(List.of(locationBasedEffects));
	}

	static ValueEffects allOf(EnchantmentValueEffect... valueEffects) {
		return new ValueEffects(List.of(valueEffects));
	}

	/**
	 * Составной эффект для сущностей: последовательно применяет все дочерние {@link EnchantmentEntityEffect}.
	 */
	record EntityEffects(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect {

		public static final MapCodec<EntityEffects> CODEC = AllOfEnchantmentEffects.buildCodec(
				EnchantmentEntityEffect.CODEC,
				EntityEffects::new,
				EntityEffects::effects
		);

		@Override
		public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
			for (EnchantmentEntityEffect effect : effects) {
				effect.apply(world, level, context, user, pos);
			}
		}

		@Override
		public MapCodec<EntityEffects> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Составной эффект, привязанный к местоположению: последовательно применяет все дочерние
	 * {@link EnchantmentLocationBasedEffect}, включая вызовы {@code remove}.
	 */
	record LocationBasedEffects(List<EnchantmentLocationBasedEffect> effects) implements EnchantmentLocationBasedEffect {

		public static final MapCodec<LocationBasedEffects> CODEC = AllOfEnchantmentEffects.buildCodec(
				EnchantmentLocationBasedEffect.CODEC,
				LocationBasedEffects::new,
				LocationBasedEffects::effects
		);

		@Override
		public void apply(
				ServerWorld world,
				int level,
				EnchantmentEffectContext context,
				Entity user,
				Vec3d pos,
				boolean newlyApplied
		) {
			for (EnchantmentLocationBasedEffect effect : effects) {
				effect.apply(world, level, context, user, pos, newlyApplied);
			}
		}

		@Override
		public void remove(EnchantmentEffectContext context, Entity user, Vec3d pos, int level) {
			for (EnchantmentLocationBasedEffect effect : effects) {
				effect.remove(context, user, pos, level);
			}
		}

		@Override
		public MapCodec<LocationBasedEffects> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Составной числовой эффект: последовательно применяет все дочерние {@link EnchantmentValueEffect},
	 * передавая результат каждого следующему (цепочка трансформаций).
	 */
	record ValueEffects(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect {

		public static final MapCodec<ValueEffects> CODEC = AllOfEnchantmentEffects.buildCodec(
				EnchantmentValueEffect.CODEC,
				ValueEffects::new,
				ValueEffects::effects
		);

		@Override
		public float apply(int level, Random random, float inputValue) {
			float result = inputValue;

			for (EnchantmentValueEffect effect : effects) {
				result = effect.apply(level, random, result);
			}

			return result;
		}

		@Override
		public MapCodec<ValueEffects> getCodec() {
			return CODEC;
		}
	}
}
