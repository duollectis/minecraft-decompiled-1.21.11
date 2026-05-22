package net.minecraft.predicate.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Предикат молнии. Проверяет количество поджжённых блоков и поражённые сущности.
 */
public record LightningBoltPredicate(
		NumberRange.IntRange blocksSetOnFire,
		Optional<EntityPredicate> entityStruck
) implements EntitySubPredicate {

	public static final MapCodec<LightningBoltPredicate> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					NumberRange.IntRange.CODEC
							.optionalFieldOf("blocks_set_on_fire", NumberRange.IntRange.ANY)
							.forGetter(LightningBoltPredicate::blocksSetOnFire),
					EntityPredicate.CODEC
							.optionalFieldOf("entity_struck")
							.forGetter(LightningBoltPredicate::entityStruck)
			).apply(instance, LightningBoltPredicate::new)
	);

	public static LightningBoltPredicate of(NumberRange.IntRange blocksSetOnFire) {
		return new LightningBoltPredicate(blocksSetOnFire, Optional.empty());
	}

	@Override
	public MapCodec<LightningBoltPredicate> getCodec() {
		return EntitySubPredicateTypes.LIGHTNING;
	}

	@Override
	public boolean test(Entity entity, ServerWorld world, @Nullable Vec3d pos) {
		if (!(entity instanceof LightningEntity lightning)) {
			return false;
		}

		if (!blocksSetOnFire.test(lightning.getBlocksSetOnFire())) {
			return false;
		}

		return entityStruck.isEmpty()
				|| lightning.getStruckEntities().anyMatch(struck -> entityStruck.get().test(world, pos, struck));
	}
}
