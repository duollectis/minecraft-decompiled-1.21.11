package net.minecraft.entity.projectile;

import com.mojang.datafixers.util.Either;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * {@code ProjectileUtil}.
 */
public final class ProjectileUtil {

	public static final float DEFAULT_MARGIN = 0.3F;

	public static HitResult getCollision(Entity entity, Predicate<Entity> predicate) {
		Vec3d vec3d = entity.getVelocity();
		World world = entity.getEntityWorld();
		Vec3d vec3d2 = entity.getEntityPos();
		return getCollision(
				vec3d2,
				entity,
				predicate,
				vec3d,
				world,
				getToleranceMargin(entity),
				RaycastContext.ShapeType.COLLIDER
		);
	}

	public static Either<BlockHitResult, Collection<EntityHitResult>> collectPiercingCollisions(
			Entity entity,
			AttackRangeComponent attackRange,
			Predicate<Entity> hitPredicate,
			RaycastContext.ShapeType shapeType
	) {
		Vec3d vec3d = entity.getHeadRotationVector();
		Vec3d vec3d2 = entity.getEyePos();
		Vec3d vec3d3 = vec3d2.add(vec3d.multiply(attackRange.getEffectiveMinRange(entity)));
		double d = entity.getMovement().dotProduct(vec3d);
		Vec3d vec3d4 = vec3d2.add(vec3d.multiply(attackRange.getEffectiveMaxRange(entity) + Math.max(0.0, d)));
		return collectPiercingCollisions(
				entity,
				vec3d2,
				vec3d3,
				hitPredicate,
				vec3d4,
				attackRange.hitboxMargin(),
				shapeType
		);
	}

	public static HitResult getCollision(
			Entity entity,
			Predicate<Entity> predicate,
			RaycastContext.ShapeType raycastShapeType
	) {
		Vec3d vec3d = entity.getVelocity();
		World world = entity.getEntityWorld();
		Vec3d vec3d2 = entity.getEntityPos();
		return getCollision(vec3d2, entity, predicate, vec3d, world, getToleranceMargin(entity), raycastShapeType);
	}

	public static HitResult getCollision(Entity entity, Predicate<Entity> predicate, double range) {
		Vec3d vec3d = entity.getRotationVec(0.0F).multiply(range);
		World world = entity.getEntityWorld();
		Vec3d vec3d2 = entity.getEyePos();
		return getCollision(vec3d2, entity, predicate, vec3d, world, 0.0F, RaycastContext.ShapeType.COLLIDER);
	}

	private static HitResult getCollision(
			Vec3d pos,
			Entity entity,
			Predicate<Entity> predicate,
			Vec3d velocity,
			World world,
			float margin,
			RaycastContext.ShapeType raycastShapeType
	) {
		Vec3d vec3d = pos.add(velocity);
		HitResult
				hitResult =
				world.getCollisionsIncludingWorldBorder(new RaycastContext(
						pos,
						vec3d,
						raycastShapeType,
						RaycastContext.FluidHandling.NONE,
						entity
				));
		if (hitResult.getType() != HitResult.Type.MISS) {
			vec3d = hitResult.getPos();
		}

		HitResult
				hitResult2 =
				getEntityCollision(
						world,
						entity,
						pos,
						vec3d,
						entity.getBoundingBox().stretch(velocity).expand(1.0),
						predicate,
						margin
				);
		if (hitResult2 != null) {
			hitResult = hitResult2;
		}

		return hitResult;
	}

	private static Either<BlockHitResult, Collection<EntityHitResult>> collectPiercingCollisions(
			Entity entity,
			Vec3d pos,
			Vec3d minReach,
			Predicate<Entity> hitPredicate,
			Vec3d maxReach,
			float hitboxMargin,
			RaycastContext.ShapeType shapeType
	) {
		World world = entity.getEntityWorld();
		BlockHitResult blockHitResult = world.getCollisionsIncludingWorldBorder(
				new RaycastContext(pos, maxReach, shapeType, RaycastContext.FluidHandling.NONE, entity)
		);
		if (blockHitResult.getType() != HitResult.Type.MISS) {
			maxReach = blockHitResult.getPos();
			if (pos.squaredDistanceTo(maxReach) < pos.squaredDistanceTo(minReach)) {
				return Either.left(blockHitResult);
			}
		}

		Box
				box =
				Box
						.of(minReach, hitboxMargin, hitboxMargin, hitboxMargin)
						.stretch(maxReach.subtract(minReach))
						.expand(1.0);
		Collection<EntityHitResult>
				collection =
				collectPiercingCollisions(
						world,
						entity,
						minReach,
						maxReach,
						box,
						hitPredicate,
						hitboxMargin,
						shapeType,
						true
				);
		return !collection.isEmpty() ? Either.right(collection) : Either.left(blockHitResult);
	}

	public static @Nullable EntityHitResult raycast(
			Entity entity,
			Vec3d min,
			Vec3d max,
			Box box,
			Predicate<Entity> predicate,
			double maxDistance
	) {
		World world = entity.getEntityWorld();
		double d = maxDistance;
		Entity entity2 = null;
		Vec3d vec3d = null;

		for (Entity entity3 : world.getOtherEntities(entity, box, predicate)) {
			Box box2 = entity3.getBoundingBox().expand(entity3.getTargetingMargin());
			Optional<Vec3d> optional = box2.raycast(min, max);
			if (box2.contains(min)) {
				if (d >= 0.0) {
					entity2 = entity3;
					vec3d = optional.orElse(min);
					d = 0.0;
				}
			}
			else if (optional.isPresent()) {
				Vec3d vec3d2 = optional.get();
				double e = min.squaredDistanceTo(vec3d2);
				if (e < d || d == 0.0) {
					if (entity3.getRootVehicle() == entity.getRootVehicle()) {
						if (d == 0.0) {
							entity2 = entity3;
							vec3d = vec3d2;
						}
					}
					else {
						entity2 = entity3;
						vec3d = vec3d2;
						d = e;
					}
				}
			}
		}

		return entity2 == null ? null : new EntityHitResult(entity2, vec3d);
	}

	public static @Nullable EntityHitResult getEntityCollision(
			World world, ProjectileEntity projectile, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate
	) {
		return getEntityCollision(world, projectile, min, max, box, predicate, getToleranceMargin(projectile));
	}

	public static float getToleranceMargin(Entity entity) {
		return Math.max(0.0F, Math.min(0.3F, (entity.age - 2) / 20.0F));
	}

	public static @Nullable EntityHitResult getEntityCollision(
			World world, Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, float margin
	) {
		double d = Double.MAX_VALUE;
		Optional<Vec3d> optional = Optional.empty();
		Entity entity2 = null;

		for (Entity entity3 : world.getOtherEntities(entity, box, predicate)) {
			Box box2 = entity3.getBoundingBox().expand(margin);
			Optional<Vec3d> optional2 = box2.raycast(min, max);
			if (optional2.isPresent()) {
				double e = min.squaredDistanceTo(optional2.get());
				if (e < d) {
					entity2 = entity3;
					d = e;
					optional = optional2;
				}
			}
		}

		return entity2 == null ? null : new EntityHitResult(entity2, optional.get());
	}

	public static Collection<EntityHitResult> collectPiercingCollisions(
			World world,
			Entity entity,
			Vec3d from,
			Vec3d to,
			Box box,
			Predicate<Entity> hitPredicate,
			boolean skipRaycast
	) {
		return collectPiercingCollisions(
				world,
				entity,
				from,
				to,
				box,
				hitPredicate,
				getToleranceMargin(entity),
				RaycastContext.ShapeType.COLLIDER,
				skipRaycast
		);
	}

	public static Collection<EntityHitResult> collectPiercingCollisions(
			World world,
			Entity entity,
			Vec3d from,
			Vec3d to,
			Box box,
			Predicate<Entity> hitPredicate,
			float hitboxMargin,
			RaycastContext.ShapeType shapeType,
			boolean bl
	) {
		List<EntityHitResult> list = new ArrayList<>();

		for (Entity entity2 : world.getOtherEntities(entity, box, hitPredicate)) {
			Box box2 = entity2.getBoundingBox();
			if (bl && box2.contains(from)) {
				list.add(new EntityHitResult(entity2, from));
			}
			else {
				Optional<Vec3d> optional = box2.raycast(from, to);
				if (optional.isPresent()) {
					list.add(new EntityHitResult(entity2, optional.get()));
				}
				else if (!(hitboxMargin <= 0.0)) {
					Optional<Vec3d> optional2 = box2.expand(hitboxMargin).raycast(from, to);
					if (!optional2.isEmpty()) {
						Vec3d vec3d = optional2.get();
						Vec3d vec3d2 = box2.getCenter();
						BlockHitResult blockHitResult = world.getCollisionsIncludingWorldBorder(
								new RaycastContext(vec3d, vec3d2, shapeType, RaycastContext.FluidHandling.NONE, entity)
						);
						if (blockHitResult.getType() != HitResult.Type.MISS) {
							vec3d2 = blockHitResult.getPos();
						}

						Optional<Vec3d> optional3 = entity2.getBoundingBox().raycast(vec3d, vec3d2);
						if (optional3.isPresent()) {
							list.add(new EntityHitResult(entity2, optional3.get()));
						}
					}
				}
			}
		}

		return list;
	}

	public static void setRotationFromVelocity(Entity entity, float tickProgress) {
		Vec3d vec3d = entity.getVelocity();
		if (vec3d.lengthSquared() != 0.0) {
			double d = vec3d.horizontalLength();
			entity.setYaw((float) (MathHelper.atan2(vec3d.z, vec3d.x) * 180.0F / (float) Math.PI) + 90.0F);
			entity.setPitch((float) (MathHelper.atan2(d, vec3d.y) * 180.0F / (float) Math.PI) - 90.0F);

			while (entity.getPitch() - entity.lastPitch < -180.0F) {
				entity.lastPitch -= 360.0F;
			}

			while (entity.getPitch() - entity.lastPitch >= 180.0F) {
				entity.lastPitch += 360.0F;
			}

			while (entity.getYaw() - entity.lastYaw < -180.0F) {
				entity.lastYaw -= 360.0F;
			}

			while (entity.getYaw() - entity.lastYaw >= 180.0F) {
				entity.lastYaw += 360.0F;
			}

			entity.setPitch(MathHelper.lerp(tickProgress, entity.lastPitch, entity.getPitch()));
			entity.setYaw(MathHelper.lerp(tickProgress, entity.lastYaw, entity.getYaw()));
		}
	}

	public static Hand getHandPossiblyHolding(LivingEntity entity, Item item) {
		return entity.getMainHandStack().isOf(item) ? Hand.MAIN_HAND : Hand.OFF_HAND;
	}

	public static PersistentProjectileEntity createArrowProjectile(
			LivingEntity entity,
			ItemStack stack,
			float damageModifier,
			@Nullable ItemStack bow
	) {
		ArrowItem arrowItem = (ArrowItem) (stack.getItem() instanceof ArrowItem ? stack.getItem() : Items.ARROW);
		PersistentProjectileEntity
				persistentProjectileEntity =
				arrowItem.createArrow(entity.getEntityWorld(), stack, entity, bow);
		persistentProjectileEntity.applyDamageModifier(damageModifier);
		return persistentProjectileEntity;
	}
}
