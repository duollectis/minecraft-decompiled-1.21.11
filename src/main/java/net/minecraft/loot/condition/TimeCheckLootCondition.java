package net.minecraft.loot.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.context.ContextParameter;

import java.util.Optional;
import java.util.Set;

/** Условие лута: проверяет текущее игровое время суток, опционально применяя модуль периода. */
public record TimeCheckLootCondition(Optional<Long> period, BoundedIntUnaryOperator value) implements LootCondition {

	public static final MapCodec<TimeCheckLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.LONG.optionalFieldOf("period").forGetter(TimeCheckLootCondition::period),
			BoundedIntUnaryOperator.CODEC.fieldOf("value").forGetter(TimeCheckLootCondition::value)
		).apply(instance, TimeCheckLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.TIME_CHECK;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return value.getRequiredParameters();
	}

	@Override
	public boolean test(LootContext lootContext) {
		ServerWorld world = lootContext.getWorld();
		long timeOfDay = world.getTimeOfDay();

		if (period.isPresent()) {
			timeOfDay %= period.get();
		}

		return value.test(lootContext, (int) timeOfDay);
	}

	public static TimeCheckLootCondition.Builder create(BoundedIntUnaryOperator value) {
		return new TimeCheckLootCondition.Builder(value);
	}

	/** Строитель условия проверки времени суток. */
	public static class Builder implements LootCondition.Builder {

		private Optional<Long> period = Optional.empty();
		private final BoundedIntUnaryOperator value;

		public Builder(BoundedIntUnaryOperator value) {
			this.value = value;
		}

		public TimeCheckLootCondition.Builder period(long period) {
			this.period = Optional.of(period);
			return this;
		}

		@Override
		public TimeCheckLootCondition build() {
			return new TimeCheckLootCondition(period, value);
		}
	}
}
