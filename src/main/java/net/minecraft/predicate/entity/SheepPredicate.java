package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Предикат для проверки состояния овцы: острижена или нет.
 */
public record SheepPredicate(Optional<Boolean> sheared) implements EntitySubPredicate {

	public static final MapCodec<SheepPredicate> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Codec.BOOL.optionalFieldOf("sheared").forGetter(SheepPredicate::sheared))
					.apply(instance, SheepPredicate::new)
	);

	public static SheepPredicate unsheared() {
		return new SheepPredicate(Optional.of(false));
	}

	@Override
	public MapCodec<SheepPredicate> getCodec() {
		return EntitySubPredicateTypes.SHEEP;
	}

	@Override
	public boolean test(Entity entity, ServerWorld world, @Nullable Vec3d pos) {
		if (!(entity instanceof SheepEntity sheep)) {
			return false;
		}

		return sheared.isEmpty() || sheep.isSheared() == sheared.get();
	}
}
