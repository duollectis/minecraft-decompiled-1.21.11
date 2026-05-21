package net.minecraft.world;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code EntityLookupView}.
 */
public interface EntityLookupView extends EntityView {

	ServerWorld toServerWorld();

	default @Nullable PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, LivingEntity entity) {
		return this.getClosestEntity(
				this.getPlayers(),
				targetPredicate,
				entity,
				entity.getX(),
				entity.getY(),
				entity.getZ()
		);
	}

	default @Nullable PlayerEntity getClosestPlayer(
			TargetPredicate targetPredicate,
			LivingEntity entity,
			double x,
			double y,
			double z
	) {
		return this.getClosestEntity(this.getPlayers(), targetPredicate, entity, x, y, z);
	}

	default @Nullable PlayerEntity getClosestPlayer(TargetPredicate targetPredicate, double x, double y, double z) {
		return this.getClosestEntity(this.getPlayers(), targetPredicate, null, x, y, z);
	}

	default <T extends LivingEntity> @Nullable T getClosestEntity(
			Class<? extends T> clazz,
			TargetPredicate targetPredicate,
			@Nullable LivingEntity entity,
			double x,
			double y,
			double z,
			Box box
	) {
		return this.getClosestEntity(
				this.getEntitiesByClass(clazz, box, potentialEntity -> true),
				targetPredicate,
				entity,
				x,
				y,
				z
		);
	}

	default @Nullable LivingEntity getClosestEntity(
			TagKey<EntityType<?>> type,
			TargetPredicate predicate,
			@Nullable LivingEntity target,
			double x,
			double y,
			double z,
			Box box
	) {
		double d = Double.MAX_VALUE;
		LivingEntity livingEntity = null;

		for (LivingEntity livingEntity2 : this.getEntitiesByClass(
				LivingEntity.class,
				box,
				predicatex -> predicatex.getType().isIn(type)
		)) {
			if (predicate.test(this.toServerWorld(), target, livingEntity2)) {
				double e = livingEntity2.squaredDistanceTo(x, y, z);
				if (e < d) {
					d = e;
					livingEntity = livingEntity2;
				}
			}
		}

		return livingEntity;
	}

	default <T extends LivingEntity> @Nullable T getClosestEntity(
			List<? extends T> entities,
			TargetPredicate targetPredicate,
			@Nullable LivingEntity entity,
			double x,
			double y,
			double z
	) {
		double d = -1.0;
		T livingEntity = null;

		for (T livingEntity2 : entities) {
			if (targetPredicate.test(this.toServerWorld(), entity, livingEntity2)) {
				double e = livingEntity2.squaredDistanceTo(x, y, z);
				if (d == -1.0 || e < d) {
					d = e;
					livingEntity = livingEntity2;
				}
			}
		}

		return livingEntity;
	}

	default List<PlayerEntity> getPlayers(TargetPredicate targetPredicate, LivingEntity entity, Box box) {
		List<PlayerEntity> list = new ArrayList<>();

		for (PlayerEntity playerEntity : this.getPlayers()) {
			if (box.contains(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ())
					&& targetPredicate.test(this.toServerWorld(), entity, playerEntity)) {
				list.add(playerEntity);
			}
		}

		return list;
	}

	default <T extends LivingEntity> List<T> getTargets(
			Class<T> clazz,
			TargetPredicate targetPredicate,
			LivingEntity entity,
			Box box
	) {
		List<T> list = this.getEntitiesByClass(clazz, box, entityx -> true);
		List<T> list2 = new ArrayList<>();

		for (T livingEntity : list) {
			if (targetPredicate.test(this.toServerWorld(), entity, livingEntity)) {
				list2.add(livingEntity);
			}
		}

		return list2;
	}
}
