package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Предикат для проверки состояния рейдера: участвует ли в рейде и является ли капитаном.
 */
public record RaiderPredicate(boolean hasRaid, boolean isCaptain) implements EntitySubPredicate {

	public static final MapCodec<RaiderPredicate> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.BOOL.optionalFieldOf("has_raid", false).forGetter(RaiderPredicate::hasRaid),
					Codec.BOOL.optionalFieldOf("is_captain", false).forGetter(RaiderPredicate::isCaptain)
			)
			.apply(instance, RaiderPredicate::new)
	);

	public static final RaiderPredicate CAPTAIN_WITHOUT_RAID = new RaiderPredicate(false, true);

	@Override
	public MapCodec<RaiderPredicate> getCodec() {
		return EntitySubPredicateTypes.RAIDER;
	}

	@Override
	public boolean test(Entity entity, ServerWorld world, @Nullable Vec3d pos) {
		if (!(entity instanceof RaiderEntity raider)) {
			return false;
		}

		return raider.hasRaid() == hasRaid && raider.isCaptain() == isCaptain;
	}
}
