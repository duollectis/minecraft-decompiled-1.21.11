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
	 * Компонент дальности атаки предмета. Определяет минимальный и максимальный радиус атаки
	 * как для обычного, так и для творческого режима, а также поправку на хитбокс и множитель для мобов.
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
		 * Создаёт компонент дальности атаки по умолчанию для сущности, используя её атрибут
		 * {@code ENTITY_INTERACTION_RANGE} как максимальный радиус (и для обычного, и для творческого режима).
		 *
		 * @param entity сущность, для которой создаётся компонент
		 * @return компонент с дальностью, равной текущему значению атрибута взаимодействия
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

	/**
		 * Вычисляет результат трассировки луча атаки от сущности. Среди всех попаданий по сущностям
		 * выбирает ближайшее к камере; если попаданий нет — возвращает промах по блоку.
		 *
		 * @param entity       атакующая сущность
		 * @param tickProgress интерполяция тика для позиции камеры
		 * @param hitPredicate фильтр допустимых целей
		 * @return результат трассировки (блок или сущность)
		 */
	public HitResult getHitResult(Entity entity, float tickProgress, Predicate<Entity> hitPredicate) {
		Either<BlockHitResult, Collection<EntityHitResult>> either = ProjectileUtil.collectPiercingCollisions(
				entity, this, hitPredicate, RaycastContext.ShapeType.OUTLINE
		);

		if (either.left().isPresent()) {
			return either.left().get();
		}

		Collection<EntityHitResult> hits = either.right().get();
		Vec3d cameraPos = entity.getCameraPosVec(tickProgress);
		EntityHitResult closest = null;
		double minDistSq = Double.MAX_VALUE;

		for (EntityHitResult hit : hits) {
			double distSq = cameraPos.squaredDistanceTo(hit.getPos());
			if (distSq < minDistSq) {
				minDistSq = distSq;
				closest = hit;
			}
		}

		if (closest != null) {
			return closest;
		}

		Vec3d lookVec = entity.getHeadRotationVector();
		Vec3d missPos = entity.getCameraPosVec(tickProgress).add(lookVec);
		return BlockHitResult.createMissed(missPos, Direction.getFacing(lookVec), BlockPos.ofFloored(missPos));
	}

	public float getEffectiveMinRange(Entity entity) {
		if (entity instanceof PlayerEntity playerEntity) {
			if (playerEntity.isSpectator()) {
				return 0.0F;
			}

			return playerEntity.isCreative() ? minCreativeRange : minRange;
		}

		return minRange * mobFactor;
	}

	public float getEffectiveMaxRange(Entity entity) {
		if (entity instanceof PlayerEntity playerEntity) {
			return playerEntity.isCreative() ? maxCreativeRange : maxRange;
		}

		return maxRange * mobFactor;
	}

	public boolean isWithinRange(LivingEntity entity, Vec3d pos) {
		return isWithinRange(entity, pos::squaredDistanceTo, 0.0);
	}

	public boolean isWithinRange(LivingEntity entity, Box box, double extraHitboxMargin) {
		return isWithinRange(entity, box::squaredMagnitude, extraHitboxMargin);
	}

	private boolean isWithinRange(
			LivingEntity entity,
			ToDoubleFunction<Vec3d> squaredDistanceFunction,
			double extraHitboxMargin
	) {
		double distance = Math.sqrt(squaredDistanceFunction.applyAsDouble(entity.getEyePos()));
		double minRange = getEffectiveMinRange(entity) - hitboxMargin - extraHitboxMargin;
		double maxRange = getEffectiveMaxRange(entity) + hitboxMargin + extraHitboxMargin;
		return distance >= minRange && distance <= maxRange;
	}
}
