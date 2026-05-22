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
 * Утилитарный класс для работы со снарядами: трассировка лучей, поиск столкновений,
 * вычисление допуска попадания и вспомогательные методы создания снарядов.
 */
public final class ProjectileUtil {

	/** Стандартный допуск (margin) для расширения хитбокса при трассировке снарядов. */
	public static final float DEFAULT_MARGIN = 0.3F;

	private static final float ROTATION_OFFSET_YAW = 90.0F;
	private static final float ROTATION_OFFSET_PITCH = 90.0F;
	private static final float HALF_ROTATION = 180.0F;
	private static final float FULL_ROTATION = 360.0F;
	private static final float TOLERANCE_RAMP_TICKS = 20.0F;
	private static final int TOLERANCE_RAMP_START_AGE = 2;

	/**
	 * Вычисляет столкновение снаряда с блоками и сущностями на основе его текущей позиции и скорости.
	 * Использует форму коллайдера и стандартный допуск.
	 *
	 * @param entity    снаряд
	 * @param predicate фильтр сущностей для попадания
	 * @return ближайший результат столкновения
	 */
	public static HitResult getCollision(Entity entity, Predicate<Entity> predicate) {
		Vec3d velocity = entity.getVelocity();
		World world = entity.getEntityWorld();
		Vec3d pos = entity.getEntityPos();
		return getCollision(
			pos,
			entity,
			predicate,
			velocity,
			world,
			getToleranceMargin(entity),
			RaycastContext.ShapeType.COLLIDER
		);
	}

	/**
	 * Собирает все столкновения снаряда с пробиванием (piercing) в заданном диапазоне атаки.
	 * Возвращает либо первый блок на пути, либо коллекцию поражённых сущностей.
	 *
	 * @param entity       снаряд
	 * @param attackRange  компонент дальности атаки
	 * @param hitPredicate фильтр сущностей
	 * @param shapeType    тип формы для трассировки блоков
	 * @return Either с блоком (Left) или коллекцией сущностей (Right)
	 */
	public static Either<BlockHitResult, Collection<EntityHitResult>> collectPiercingCollisions(
		Entity entity,
		AttackRangeComponent attackRange,
		Predicate<Entity> hitPredicate,
		RaycastContext.ShapeType shapeType
	) {
		Vec3d lookVec = entity.getHeadRotationVector();
		Vec3d eyePos = entity.getEyePos();
		Vec3d minReach = eyePos.add(lookVec.multiply(attackRange.getEffectiveMinRange(entity)));
		double movementDot = entity.getMovement().dotProduct(lookVec);
		Vec3d maxReach = eyePos.add(lookVec.multiply(attackRange.getEffectiveMaxRange(entity) + Math.max(0.0, movementDot)));
		return collectPiercingCollisions(
			entity,
			eyePos,
			minReach,
			hitPredicate,
			maxReach,
			attackRange.hitboxMargin(),
			shapeType
		);
	}

	/**
	 * Вычисляет столкновение снаряда с заданным типом формы трассировки.
	 *
	 * @param entity           снаряд
	 * @param predicate        фильтр сущностей
	 * @param raycastShapeType тип формы для трассировки блоков
	 * @return ближайший результат столкновения
	 */
	public static HitResult getCollision(
		Entity entity,
		Predicate<Entity> predicate,
		RaycastContext.ShapeType raycastShapeType
	) {
		Vec3d velocity = entity.getVelocity();
		World world = entity.getEntityWorld();
		Vec3d pos = entity.getEntityPos();
		return getCollision(pos, entity, predicate, velocity, world, getToleranceMargin(entity), raycastShapeType);
	}

	/**
	 * Вычисляет столкновение снаряда в заданном диапазоне от глаз сущности.
	 * Используется для атак ближнего боя с трассировкой луча.
	 *
	 * @param entity    сущность
	 * @param predicate фильтр сущностей
	 * @param range     дальность трассировки
	 * @return ближайший результат столкновения
	 */
	public static HitResult getCollision(Entity entity, Predicate<Entity> predicate, double range) {
		Vec3d velocity = entity.getRotationVec(0.0F).multiply(range);
		World world = entity.getEntityWorld();
		Vec3d eyePos = entity.getEyePos();
		return getCollision(eyePos, entity, predicate, velocity, world, 0.0F, RaycastContext.ShapeType.COLLIDER);
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
		Vec3d endPos = pos.add(velocity);
		HitResult blockHit = world.getCollisionsIncludingWorldBorder(
			new RaycastContext(pos, endPos, raycastShapeType, RaycastContext.FluidHandling.NONE, entity)
		);
		if (blockHit.getType() != HitResult.Type.MISS) {
			endPos = blockHit.getPos();
		}

		HitResult entityHit = getEntityCollision(
			world,
			entity,
			pos,
			endPos,
			entity.getBoundingBox().stretch(velocity).expand(1.0),
			predicate,
			margin
		);
		if (entityHit != null) {
			return entityHit;
		}

		return blockHit;
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
		BlockHitResult blockHit = world.getCollisionsIncludingWorldBorder(
			new RaycastContext(pos, maxReach, shapeType, RaycastContext.FluidHandling.NONE, entity)
		);
		if (blockHit.getType() != HitResult.Type.MISS) {
			maxReach = blockHit.getPos();
			if (pos.squaredDistanceTo(maxReach) < pos.squaredDistanceTo(minReach)) {
				return Either.left(blockHit);
			}
		}

		Box searchBox = Box.of(minReach, hitboxMargin, hitboxMargin, hitboxMargin)
			.stretch(maxReach.subtract(minReach))
			.expand(1.0);
		Collection<EntityHitResult> hits = collectPiercingCollisions(
			world,
			entity,
			minReach,
			maxReach,
			searchBox,
			hitPredicate,
			hitboxMargin,
			shapeType,
			true
		);
		return hits.isEmpty() ? Either.left(blockHit) : Either.right(hits);
	}

	/**
	 * Выполняет трассировку луча для поиска ближайшей сущности в заданном объёме.
	 * Если начальная точка находится внутри хитбокса — сущность считается поражённой немедленно.
	 *
	 * @param entity      снаряд (исключается из поиска)
	 * @param min         начальная точка луча
	 * @param max         конечная точка луча
	 * @param box         ограничивающий объём для поиска сущностей
	 * @param predicate   фильтр сущностей
	 * @param maxDistance максимальное расстояние до цели
	 * @return результат попадания или null
	 */
	public static @Nullable EntityHitResult raycast(
		Entity entity,
		Vec3d min,
		Vec3d max,
		Box box,
		Predicate<Entity> predicate,
		double maxDistance
	) {
		World world = entity.getEntityWorld();
		double closestDist = maxDistance;
		Entity closestEntity = null;
		Vec3d hitPos = null;

		for (Entity candidate : world.getOtherEntities(entity, box, predicate)) {
			Box expandedBox = candidate.getBoundingBox().expand(candidate.getTargetingMargin());
			Optional<Vec3d> intersection = expandedBox.raycast(min, max);
			if (expandedBox.contains(min)) {
				if (closestDist >= 0.0) {
					closestEntity = candidate;
					hitPos = intersection.orElse(min);
					closestDist = 0.0;
				}
			} else if (intersection.isPresent()) {
				Vec3d intersectPos = intersection.get();
				double distSq = min.squaredDistanceTo(intersectPos);
				if (distSq < closestDist || closestDist == 0.0) {
					if (candidate.getRootVehicle() == entity.getRootVehicle()) {
						if (closestDist == 0.0) {
							closestEntity = candidate;
							hitPos = intersectPos;
						}
					} else {
						closestEntity = candidate;
						hitPos = intersectPos;
						closestDist = distSq;
					}
				}
			}
		}

		return closestEntity == null ? null : new EntityHitResult(closestEntity, hitPos);
	}

	public static @Nullable EntityHitResult getEntityCollision(
		World world,
		ProjectileEntity projectile,
		Vec3d min,
		Vec3d max,
		Box box,
		Predicate<Entity> predicate
	) {
		return getEntityCollision(world, projectile, min, max, box, predicate, getToleranceMargin(projectile));
	}

	/**
	 * Вычисляет допуск (margin) для расширения хитбокса при трассировке снаряда.
	 * Допуск плавно нарастает от 0 до {@link #DEFAULT_MARGIN} в течение первых 20 тиков после спавна.
	 *
	 * @param entity снаряд
	 * @return значение допуска в диапазоне [0, DEFAULT_MARGIN]
	 */
	public static float getToleranceMargin(Entity entity) {
		return Math.max(0.0F, Math.min(DEFAULT_MARGIN, (entity.age - TOLERANCE_RAMP_START_AGE) / TOLERANCE_RAMP_TICKS));
	}

	/**
	 * Находит ближайшую сущность на пути луча в заданном объёме с учётом допуска хитбокса.
	 *
	 * @param world     мир
	 * @param entity    снаряд (исключается из поиска)
	 * @param min       начальная точка луча
	 * @param max       конечная точка луча
	 * @param box       ограничивающий объём для поиска
	 * @param predicate фильтр сущностей
	 * @param margin    допуск расширения хитбокса
	 * @return результат попадания или null
	 */
	public static @Nullable EntityHitResult getEntityCollision(
		World world,
		Entity entity,
		Vec3d min,
		Vec3d max,
		Box box,
		Predicate<Entity> predicate,
		float margin
	) {
		double closestDistSq = Double.MAX_VALUE;
		Optional<Vec3d> closestHitPos = Optional.empty();
		Entity closestEntity = null;

		for (Entity candidate : world.getOtherEntities(entity, box, predicate)) {
			Box expandedBox = candidate.getBoundingBox().expand(margin);
			Optional<Vec3d> intersection = expandedBox.raycast(min, max);
			if (intersection.isPresent()) {
				double distSq = min.squaredDistanceTo(intersection.get());
				if (distSq < closestDistSq) {
					closestEntity = candidate;
					closestDistSq = distSq;
					closestHitPos = intersection;
				}
			}
		}

		return closestEntity == null ? null : new EntityHitResult(closestEntity, closestHitPos.get());
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

	/**
	 * Собирает все сущности, пронизанные лучом снаряда (для пробивающих снарядов).
	 * При ненулевом допуске дополнительно проверяет расширенный хитбокс и трассирует
	 * луч к центру сущности для исключения ложных попаданий через стены.
	 *
	 * @param world        мир
	 * @param entity       снаряд
	 * @param from         начальная точка
	 * @param to           конечная точка
	 * @param box          ограничивающий объём
	 * @param hitPredicate фильтр сущностей
	 * @param hitboxMargin допуск расширения хитбокса
	 * @param shapeType    тип формы для трассировки блоков
	 * @param skipRaycast  если true — сущности внутри начальной точки добавляются без трассировки
	 * @return коллекция результатов попаданий
	 */
	public static Collection<EntityHitResult> collectPiercingCollisions(
		World world,
		Entity entity,
		Vec3d from,
		Vec3d to,
		Box box,
		Predicate<Entity> hitPredicate,
		float hitboxMargin,
		RaycastContext.ShapeType shapeType,
		boolean skipRaycast
	) {
		List<EntityHitResult> hits = new ArrayList<>();

		for (Entity candidate : world.getOtherEntities(entity, box, hitPredicate)) {
			Box candidateBox = candidate.getBoundingBox();
			if (skipRaycast && candidateBox.contains(from)) {
				hits.add(new EntityHitResult(candidate, from));
				continue;
			}

			Optional<Vec3d> directHit = candidateBox.raycast(from, to);
			if (directHit.isPresent()) {
				hits.add(new EntityHitResult(candidate, directHit.get()));
				continue;
			}

			if (hitboxMargin <= 0.0) {
				continue;
			}

			Optional<Vec3d> expandedHit = candidateBox.expand(hitboxMargin).raycast(from, to);
			if (expandedHit.isEmpty()) {
				continue;
			}

			// Проверяем, не загорожена ли цель блоком между точкой попадания и центром хитбокса
			Vec3d hitPoint = expandedHit.get();
			Vec3d boxCenter = candidateBox.getCenter();
			BlockHitResult blockCheck = world.getCollisionsIncludingWorldBorder(
				new RaycastContext(hitPoint, boxCenter, shapeType, RaycastContext.FluidHandling.NONE, entity)
			);
			if (blockCheck.getType() != HitResult.Type.MISS) {
				boxCenter = blockCheck.getPos();
			}

			Optional<Vec3d> confirmedHit = candidate.getBoundingBox().raycast(hitPoint, boxCenter);
			if (confirmedHit.isPresent()) {
				hits.add(new EntityHitResult(candidate, confirmedHit.get()));
			}
		}

		return hits;
	}

	/**
	 * Устанавливает углы поворота сущности на основе вектора скорости с интерполяцией.
	 * Используется для плавного вращения снарядов типа шулкерской пули.
	 *
	 * @param entity       сущность
	 * @param tickProgress прогресс интерполяции (0.0 — предыдущий тик, 1.0 — текущий)
	 */
	public static void setRotationFromVelocity(Entity entity, float tickProgress) {
		Vec3d velocity = entity.getVelocity();
		if (velocity.lengthSquared() == 0.0) {
			return;
		}

		double horizontalLen = velocity.horizontalLength();
		entity.setYaw(
			(float) (MathHelper.atan2(velocity.z, velocity.x) * HALF_ROTATION / (float) Math.PI) + ROTATION_OFFSET_YAW
		);
		entity.setPitch(
			(float) (MathHelper.atan2(horizontalLen, velocity.y) * HALF_ROTATION / (float) Math.PI) - ROTATION_OFFSET_PITCH
		);

		while (entity.getPitch() - entity.lastPitch < -HALF_ROTATION) {
			entity.lastPitch -= FULL_ROTATION;
		}

		while (entity.getPitch() - entity.lastPitch >= HALF_ROTATION) {
			entity.lastPitch += FULL_ROTATION;
		}

		while (entity.getYaw() - entity.lastYaw < -HALF_ROTATION) {
			entity.lastYaw -= FULL_ROTATION;
		}

		while (entity.getYaw() - entity.lastYaw >= HALF_ROTATION) {
			entity.lastYaw += FULL_ROTATION;
		}

		entity.setPitch(MathHelper.lerp(tickProgress, entity.lastPitch, entity.getPitch()));
		entity.setYaw(MathHelper.lerp(tickProgress, entity.lastYaw, entity.getYaw()));
	}

	/**
	 * Определяет руку, в которой сущность держит указанный предмет.
	 * Приоритет — основная рука.
	 *
	 * @param entity сущность
	 * @param item   искомый предмет
	 * @return рука, держащая предмет
	 */
	public static Hand getHandPossiblyHolding(LivingEntity entity, Item item) {
		return entity.getMainHandStack().isOf(item) ? Hand.MAIN_HAND : Hand.OFF_HAND;
	}

	/**
	 * Создаёт снаряд-стрелу из стека предмета с применением модификатора урона.
	 * Если предмет не является {@link ArrowItem} — используется обычная стрела.
	 *
	 * @param entity         стрелок
	 * @param stack          стек предмета-стрелы
	 * @param damageModifier множитель базового урона
	 * @param bow            стек лука (может быть null)
	 * @return созданный снаряд-стрела
	 */
	public static PersistentProjectileEntity createArrowProjectile(
		LivingEntity entity,
		ItemStack stack,
		float damageModifier,
		@Nullable ItemStack bow
	) {
		ArrowItem arrowItem = stack.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem) Items.ARROW.asItem();
		PersistentProjectileEntity arrow = arrowItem.createArrow(entity.getEntityWorld(), stack, entity, bow);
		arrow.applyDamageModifier(damageModifier);
		return arrow;
	}
}
