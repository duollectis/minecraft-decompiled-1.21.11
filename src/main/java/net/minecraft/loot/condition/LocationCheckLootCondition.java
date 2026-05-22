package net.minecraft.loot.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;
import java.util.Set;

/**
 * Условие, проверяющее местоположение через {@link LocationPredicate}.
 *
 * <p>Поддерживает смещение позиции через {@code offset}, что позволяет проверять
 * соседние блоки относительно источника лута.</p>
 */
public record LocationCheckLootCondition(
	Optional<LocationPredicate> predicate,
	BlockPos offset
) implements LootCondition {

	private static final MapCodec<BlockPos> OFFSET_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.INT.optionalFieldOf("offsetX", 0).forGetter(Vec3i::getX),
			Codec.INT.optionalFieldOf("offsetY", 0).forGetter(Vec3i::getY),
			Codec.INT.optionalFieldOf("offsetZ", 0).forGetter(Vec3i::getZ)
		)
		.apply(instance, BlockPos::new)
	);

	public static final MapCodec<LocationCheckLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			LocationPredicate.CODEC.optionalFieldOf("predicate").forGetter(LocationCheckLootCondition::predicate),
			OFFSET_CODEC.forGetter(LocationCheckLootCondition::offset)
		)
		.apply(instance, LocationCheckLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.LOCATION_CHECK;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.ORIGIN);
	}

	public boolean test(LootContext lootContext) {
		Vec3d origin = lootContext.get(LootContextParameters.ORIGIN);

		if (origin == null) {
			return false;
		}

		return predicate.isEmpty() || predicate.get().test(
			lootContext.getWorld(),
			origin.getX() + offset.getX(),
			origin.getY() + offset.getY(),
			origin.getZ() + offset.getZ()
		);
	}

	public static LootCondition.Builder builder(LocationPredicate.Builder predicateBuilder) {
		return () -> new LocationCheckLootCondition(Optional.of(predicateBuilder.build()), BlockPos.ORIGIN);
	}

	public static LootCondition.Builder builder(LocationPredicate.Builder predicateBuilder, BlockPos pos) {
		return () -> new LocationCheckLootCondition(Optional.of(predicateBuilder.build()), pos);
	}
}
