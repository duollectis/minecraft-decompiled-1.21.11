package net.minecraft.component.type;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * {@code AttackRangeComponent}.
 */
public record AttackRangeComponent(
		float minRange,
		float maxRange,
		float minCreativeRange,
		float maxCreativeRange,
		float hitboxMargin,
		float mobFactor
) {

	public static final Codec<AttackRangeComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codecs
							                    .rangedInclusiveFloat(0.0F, 64.0F)
							                    .optionalFieldOf("min_reach", 0.0F)
							                    .forGetter(AttackRangeComponent::minRange),
					                    Codecs
							                    .rangedInclusiveFloat(0.0F, 64.0F)
							                    .optionalFieldOf("max_reach", 3.0F)
							                    .forGetter(AttackRangeComponent::maxRange),
					                    Codecs
							                    .rangedInclusiveFloat(0.0F, 64.0F)
							                    .optionalFieldOf("min_creative_reach", 0.0F)
							                    .forGetter(AttackRangeComponent::minCreativeRange),
					                    Codecs
							                    .rangedInclusiveFloat(0.0F, 64.0F)
							                    .optionalFieldOf("max_creative_reach", 5.0F)
							                    .forGetter(AttackRangeComponent::maxCreativeRange),
					                    Codecs
							                    .rangedInclusiveFloat(0.0F, 1.0F)
							                    .optionalFieldOf("hitbox_margin", 0.3F)
							                    .forGetter(AttackRangeComponent::hitboxMargin),
					                    Codec
							                    .floatRange(0.0F, 2.0F)
							                    .optionalFieldOf("mob_factor", 1.0F)
							                    .forGetter(AttackRangeComponent::mobFactor)
			                    )
			                    .apply(instance, AttackRangeComponent::new)
	);
	public static final PacketCodec<ByteBuf, AttackRangeComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT,
			AttackRangeComponent::minRange,
			PacketCodecs.FLOAT,
			AttackRangeComponent::maxRange,
			PacketCodecs.FLOAT,
			AttackRangeComponent::minCreativeRange,
			PacketCodecs.FLOAT,
			AttackRangeComponent::maxCreativeRange,
			PacketCodecs.FLOAT,
			AttackRangeComponent::hitboxMargin,
			PacketCodecs.FLOAT,
			AttackRangeComponent::mobFactor,
			AttackRangeComponent::new
	);

	/**
	 * Default for entity.
	 *
	 * @param entity entity
	 *
	 * @return AttackRangeComponent — результат операции
	 */
	public static AttackRangeComponent defaultForEntity(LivingEntity entity) {
		return new AttackRangeComponent(
				0.0F,
				(float) entity.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE),
				0.0F,
				(float) entity.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE),
				0.0F,
				1.0F
		);
	}

	public HitResult getHitResult(Entity entity, float tickProgress, Predicate<Entity> hitPredicate) {
		Either<BlockHitResult, Collection<EntityHitResult>> either = ProjectileUtil.collectPiercingCollisions(
				entity, this, hitPredicate, RaycastContext.ShapeType.OUTLINE
		);
		if (either.left().isPresent()) {
			return (HitResult) either.left().get();
		}
		else {
			Collection<EntityHitResult> collection = (Collection<EntityHitResult>) either.right().get();
			EntityHitResult entityHitResult = null;
			Vec3d vec3d = entity.getCameraPosVec(tickProgress);
			double d = Double.MAX_VALUE;

			for (EntityHitResult entityHitResult2 : collection) {
				double e = vec3d.squaredDistanceTo(entityHitResult2.getPos());
				if (e < d) {
					d = e;
					entityHitResult = entityHitResult2;
				}
			}

			if (entityHitResult != null) {
				return entityHitResult;
			}
			else {
				Vec3d vec3d2 = entity.getHeadRotationVector();
				Vec3d vec3d3 = entity.getCameraPosVec(tickProgress).add(vec3d2);
				return BlockHitResult.createMissed(vec3d3, Direction.getFacing(vec3d2), BlockPos.ofFloored(vec3d3));
			}
		}
	}

	public float getEffectiveMinRange(Entity entity) {
		if (entity instanceof PlayerEntity playerEntity) {
			if (playerEntity.isSpectator()) {
				return 0.0F;
			}
			else {
				return playerEntity.isCreative() ? this.minCreativeRange : this.minRange;
			}
		}
		else {
			return this.minRange * this.mobFactor;
		}
	}

	public float getEffectiveMaxRange(Entity entity) {
		if (entity instanceof PlayerEntity playerEntity) {
			return playerEntity.isCreative() ? this.maxCreativeRange : this.maxRange;
		}
		else {
			return this.maxRange * this.mobFactor;
		}
	}

	public boolean isWithinRange(LivingEntity entity, Vec3d pos) {
		return this.isWithinRange(entity, pos::squaredDistanceTo, 0.0);
	}

	public boolean isWithinRange(LivingEntity entity, Box box, double extraHitboxMargin) {
		return this.isWithinRange(entity, box::squaredMagnitude, extraHitboxMargin);
	}

	private boolean isWithinRange(
			LivingEntity entity,
			ToDoubleFunction<Vec3d> squaredDistanceFunction,
			double extraHitboxMargin
	) {
		double d = Math.sqrt(squaredDistanceFunction.applyAsDouble(entity.getEyePos()));
		double e = this.getEffectiveMinRange(entity) - this.hitboxMargin - extraHitboxMargin;
		double f = this.getEffectiveMaxRange(entity) + this.hitboxMargin + extraHitboxMargin;
		return d >= e && d <= f;
	}
}
