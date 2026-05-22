package net.minecraft.loot.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/** Условие лута: проверяет текущее состояние погоды в мире (дождь и/или гроза). */
public record WeatherCheckLootCondition(
	Optional<Boolean> raining,
	Optional<Boolean> thundering
) implements LootCondition {

	public static final MapCodec<WeatherCheckLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.BOOL.optionalFieldOf("raining").forGetter(WeatherCheckLootCondition::raining),
			Codec.BOOL.optionalFieldOf("thundering").forGetter(WeatherCheckLootCondition::thundering)
		).apply(instance, WeatherCheckLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.WEATHER_CHECK;
	}

	@Override
	public boolean test(LootContext lootContext) {
		ServerWorld world = lootContext.getWorld();

		if (raining.isPresent() && raining.get() != world.isRaining()) {
			return false;
		}

		return thundering.isEmpty() || thundering.get() == world.isThundering();
	}

	public static WeatherCheckLootCondition.Builder create() {
		return new WeatherCheckLootCondition.Builder();
	}

	/** Строитель условия проверки погоды. */
	public static class Builder implements LootCondition.Builder {

		private Optional<Boolean> raining = Optional.empty();
		private Optional<Boolean> thundering = Optional.empty();

		public WeatherCheckLootCondition.Builder raining(boolean raining) {
			this.raining = Optional.of(raining);
			return this;
		}

		public WeatherCheckLootCondition.Builder thundering(boolean thundering) {
			this.thundering = Optional.of(thundering);
			return this;
		}

		@Override
		public WeatherCheckLootCondition build() {
			return new WeatherCheckLootCondition(raining, thundering);
		}
	}
}
