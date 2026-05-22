package net.minecraft.world.explosion;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Серверная реализация взрыва. Выполняет полный цикл взрыва:
 * трассировку лучей для определения разрушаемых блоков, нанесение урона
 * и отбрасывание сущностей, разрушение блоков с выпадением предметов
 * и опциональное создание огня.
 */
public class ExplosionImpl implements Explosion {

	private static final ExplosionBehavior DEFAULT_BEHAVIOR = new ExplosionBehavior();

	/** Количество точек сетки по каждой оси при трассировке лучей взрыва. */
	private static final int RAY_GRID_SIZE = 16;

	/** Последний индекс сетки (RAY_GRID_SIZE - 1), используется для определения граней куба. */
	private static final int RAY_GRID_LAST = RAY_GRID_SIZE - 1;

	/** Шаг продвижения луча взрыва в блоках за одну итерацию. */
	private static final float RAY_STEP = 0.3F;

	/**
	 * Затухание мощности луча за один шаг без учёта сопротивления блоков.
	 * Равно {@code RAY_STEP * 0.75F = 0.225F}. Значение {@code 0.22500001F}
	 * — артефакт точности float в оригинальном коде.
	 */
	private static final float RAY_ATTENUATION_PER_STEP = 0.22500001F;

	/** Случайный диапазон начальной мощности луча: {@code power * (0.7 + random * 0.6)}. */
	private static final float RAY_POWER_RANDOM_MIN = 0.7F;
	private static final float RAY_POWER_RANDOM_RANGE = 0.6F;

	/** Минимальная мощность взрыва, ниже которой урон сущностям не наносится. */
	private static final float MIN_DAMAGE_POWER = 1.0E-5F;

	private final boolean createFire;
	private final Explosion.DestructionType destructionType;
	private final ServerWorld world;
	private final Vec3d pos;
	private final @Nullable Entity entity;
	private final float power;
	private final DamageSource damageSource;
	private final ExplosionBehavior behavior;
	private final Map<PlayerEntity, Vec3d> knockbackByPlayer = new HashMap<>();

	public ExplosionImpl(
			ServerWorld world,
			@Nullable Entity entity,
			@Nullable DamageSource damageSource,
			@Nullable ExplosionBehavior behavior,
			Vec3d pos,
			float power,
			boolean createFire,
			Explosion.DestructionType destructionType
	) {
		this.world = world;
		this.entity = entity;
		this.power = power;
		this.pos = pos;
		this.createFire = createFire;
		this.destructionType = destructionType;
		this.damageSource = damageSource == null ? world.getDamageSources().explosion(this) : damageSource;
		this.behavior = behavior == null ? resolveBehavior(entity) : behavior;
	}

	/**
	 * Вычисляет долю лучей, достигающих сущности из центра взрыва без препятствий.
	 * Используется как коэффициент воздействия (exposure) при расчёте урона и отбрасывания.
	 * <p>
	 * Алгоритм: равномерная сетка точек на AABB сущности, из каждой точки
	 * пускается рейкаст к центру взрыва. Возвращает долю незаблокированных лучей.
	 *
	 * @param explosionPos позиция центра взрыва
	 * @param entity       сущность, для которой вычисляется воздействие
	 * @return коэффициент воздействия от 0.0 (полностью заблокирован) до 1.0 (открыт)
	 */
	public static float calculateReceivedDamage(Vec3d explosionPos, Entity entity) {
		Box box = entity.getBoundingBox();
		double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
		double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
		double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

		if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) {
			return 0.0F;
		}

		double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
		double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
		int hitCount = 0;
		int totalCount = 0;

		for (double tx = 0.0; tx <= 1.0; tx += stepX) {
			for (double ty = 0.0; ty <= 1.0; ty += stepY) {
				for (double tz = 0.0; tz <= 1.0; tz += stepZ) {
					double sampleX = MathHelper.lerp(tx, box.minX, box.maxX);
					double sampleY = MathHelper.lerp(ty, box.minY, box.maxY);
					double sampleZ = MathHelper.lerp(tz, box.minZ, box.maxZ);
					Vec3d samplePoint = new Vec3d(sampleX + offsetX, sampleY, sampleZ + offsetZ);

					boolean isVisible = entity.getEntityWorld()
						.raycast(new RaycastContext(
							samplePoint,
							explosionPos,
							RaycastContext.ShapeType.COLLIDER,
							RaycastContext.FluidHandling.NONE,
							entity
						))
						.getType() == HitResult.Type.MISS;

					if (isVisible) {
						hitCount++;
					}

					totalCount++;
				}
			}
		}

		return (float) hitCount / totalCount;
	}

	@Override
	public float getPower() {
		return power;
	}

	@Override
	public Vec3d getPosition() {
		return pos;
	}

	/**
	 * Запускает полный цикл взрыва: испускает игровое событие, определяет
	 * разрушаемые блоки, наносит урон сущностям, разрушает блоки и создаёт огонь.
	 *
	 * @return количество блоков, затронутых взрывом
	 */
	public int explode() {
		world.emitGameEvent(entity, GameEvent.EXPLODE, pos);
		List<BlockPos> affectedBlocks = getBlocksToDestroy();
		damageEntities();

		if (shouldDestroyBlocks()) {
			Profiler profiler = Profilers.get();
			profiler.push("explosion_blocks");
			destroyBlocks(affectedBlocks);
			profiler.pop();
		}

		if (createFire) {
			createFire(affectedBlocks);
		}

		return affectedBlocks.size();
	}

	public Map<PlayerEntity, Vec3d> getKnockbackByPlayer() {
		return knockbackByPlayer;
	}

	@Override
	public ServerWorld getWorld() {
		return world;
	}

	@Override
	public @Nullable LivingEntity getCausingEntity() {
		return Explosion.getCausingEntity(entity);
	}

	@Override
	public @Nullable Entity getEntity() {
		return entity;
	}

	public DamageSource getDamageSource() {
		return damageSource;
	}

	@Override
	public Explosion.DestructionType getDestructionType() {
		return destructionType;
	}

	/**
	 * Взрыв типа {@link Explosion.DestructionType#TRIGGER_BLOCK} может активировать блоки
	 * только если это не заряд ветра бриза, либо если включено правило {@code mobGriefing}.
	 */
	@Override
	public boolean canTriggerBlocks() {
		if (destructionType != Explosion.DestructionType.TRIGGER_BLOCK) {
			return false;
		}

		boolean isBreezeWindCharge = entity != null && entity.getType() == EntityType.BREEZE_WIND_CHARGE;
		return !isBreezeWindCharge || world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
	}

	/**
	 * Декоративные сущности (рамки, картины и т.п.) сохраняются при взрыве,
	 * если источник — заряд ветра (бриза или обычный), либо если {@code mobGriefing} отключён
	 * и взрыв не разрушает блоки.
	 */
	@Override
	public boolean preservesDecorativeEntities() {
		boolean mobGriefingEnabled = world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
		boolean isWindCharge = entity != null
			&& (entity.getType() == EntityType.BREEZE_WIND_CHARGE
				|| entity.getType() == EntityType.WIND_CHARGE);

		return mobGriefingEnabled
			? !isWindCharge
			: destructionType.destroysBlocks() && !isWindCharge;
	}

	/**
	 * Взрыв считается малым, если его мощность меньше 2 или он не разрушает блоки.
	 * Используется для оптимизации клиентских эффектов.
	 */
	public boolean isSmall() {
		return power < 2.0F || !shouldDestroyBlocks();
	}

	/**
	 * Трассирует лучи из центра взрыва по всем направлениям от граней куба
	 * {@code RAY_GRID_SIZE × RAY_GRID_SIZE × RAY_GRID_SIZE} и собирает блоки,
	 * которые должны быть разрушены.
	 * <p>
	 * Каждый луч начинается с мощностью {@code power * (0.7 + random * 0.6)}.
	 * На каждом шаге мощность уменьшается на {@link #RAY_ATTENUATION_PER_STEP}
	 * плюс сопротивление блока. Блок добавляется в список, если луч ещё имеет
	 * положительную мощность после прохождения через него.
	 */
	private List<BlockPos> getBlocksToDestroy() {
		Set<BlockPos> affectedBlocks = new HashSet<>();

		for (int gx = 0; gx < RAY_GRID_SIZE; gx++) {
			for (int gy = 0; gy < RAY_GRID_SIZE; gy++) {
				for (int gz = 0; gz < RAY_GRID_SIZE; gz++) {
					if (gx != 0 && gx != RAY_GRID_LAST
							&& gy != 0 && gy != RAY_GRID_LAST
							&& gz != 0 && gz != RAY_GRID_LAST) {
						continue;
					}

					double dirX = gx / (float) RAY_GRID_LAST * 2.0F - 1.0F;
					double dirY = gy / (float) RAY_GRID_LAST * 2.0F - 1.0F;
					double dirZ = gz / (float) RAY_GRID_LAST * 2.0F - 1.0F;
					double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
					dirX /= length;
					dirY /= length;
					dirZ /= length;

					float rayPower = power * (RAY_POWER_RANDOM_MIN + world.random.nextFloat() * RAY_POWER_RANDOM_RANGE);
					double rayX = pos.x;
					double rayY = pos.y;
					double rayZ = pos.z;

					for (; rayPower > 0.0F; rayPower -= RAY_ATTENUATION_PER_STEP) {
						BlockPos blockPos = BlockPos.ofFloored(rayX, rayY, rayZ);
						BlockState blockState = world.getBlockState(blockPos);
						FluidState fluidState = world.getFluidState(blockPos);

						if (!world.isInBuildLimit(blockPos)) {
							break;
						}

						Optional<Float> resistance = behavior.getBlastResistance(
							this,
							world,
							blockPos,
							blockState,
							fluidState
						);

						if (resistance.isPresent()) {
							rayPower -= (resistance.get() + RAY_STEP) * RAY_STEP;
						}

						if (rayPower > 0.0F && behavior.canDestroyBlock(this, world, blockPos, blockState, rayPower)) {
							affectedBlocks.add(blockPos);
						}

						rayX += dirX * RAY_STEP;
						rayY += dirY * RAY_STEP;
						rayZ += dirZ * RAY_STEP;
					}
				}
			}
		}

		return new ObjectArrayList<>(affectedBlocks);
	}

	/**
	 * Наносит урон и отбрасывает все сущности в радиусе взрыва.
	 * Урон масштабируется по расстоянию и коэффициенту воздействия (exposure).
	 * Отбрасывание уменьшается атрибутом {@link EntityAttributes#EXPLOSION_KNOCKBACK_RESISTANCE}.
	 * Перенаправляемые снаряды меняют владельца на атакующего.
	 * Позиции игроков для отбрасывания сохраняются в {@link #knockbackByPlayer}.
	 */
	private void damageEntities() {
		if (power < MIN_DAMAGE_POWER) {
			return;
		}

		float diameter = power * 2.0F;
		int minX = MathHelper.floor(pos.x - diameter - 1.0);
		int maxX = MathHelper.floor(pos.x + diameter + 1.0);
		int minY = MathHelper.floor(pos.y - diameter - 1.0);
		int maxY = MathHelper.floor(pos.y + diameter + 1.0);
		int minZ = MathHelper.floor(pos.z - diameter - 1.0);
		int maxZ = MathHelper.floor(pos.z + diameter + 1.0);

		for (Entity target : world.getOtherEntities(entity, new Box(minX, minY, minZ, maxX, maxY, maxZ))) {
			if (target.isImmuneToExplosion(this)) {
				continue;
			}

			double distance = Math.sqrt(target.squaredDistanceTo(pos)) / diameter;
			if (distance > 1.0) {
				continue;
			}

			Vec3d eyePos = target instanceof TntEntity ? target.getEntityPos() : target.getEyePos();
			Vec3d knockbackDir = eyePos.subtract(pos).normalize();
			boolean shouldDamage = behavior.shouldDamage(this, target);
			float knockbackMod = behavior.getKnockbackModifier(target);
			float exposure = (!shouldDamage && knockbackMod == 0.0F)
				? 0.0F
				: calculateReceivedDamage(pos, target);

			if (shouldDamage) {
				target.damage(world, damageSource, behavior.calculateDamage(this, target, exposure));
			}

			double knockbackResistance = target instanceof LivingEntity living
				? living.getAttributeValue(EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE)
				: 0.0;
			double knockbackStrength = (1.0 - distance) * exposure * knockbackMod * (1.0 - knockbackResistance);
			Vec3d knockbackVelocity = knockbackDir.multiply(knockbackStrength);
			target.addVelocity(knockbackVelocity);

			if (target.getType().isIn(EntityTypeTags.REDIRECTABLE_PROJECTILE)
					&& target instanceof ProjectileEntity projectile) {
				projectile.setOwner(damageSource.getAttacker());
			} else if (target instanceof PlayerEntity player
					&& !player.isSpectator()
					&& (!player.isCreative() || !player.getAbilities().flying)) {
				knockbackByPlayer.put(player, knockbackVelocity);
			}

			target.onExplodedBy(entity);
		}
	}

	/**
	 * Разрушает блоки из переданного списка в случайном порядке.
	 * Выпавшие предметы накапливаются и объединяются перед спавном в мире.
	 */
	private void destroyBlocks(List<BlockPos> positions) {
		List<DroppedItem> droppedItems = new ArrayList<>();
		Util.shuffle(positions, world.random);

		for (BlockPos blockPos : positions) {
			world.getBlockState(blockPos)
				.onExploded(world, blockPos, this, (item, dropPos) -> addDroppedItem(droppedItems, item, dropPos));
		}

		for (DroppedItem droppedItem : droppedItems) {
			Block.dropStack(world, droppedItem.pos, droppedItem.item);
		}
	}

	/**
	 * Случайно расставляет огонь на воздушных блоках поверх непрозрачных,
	 * затронутых взрывом (вероятность 1/3 для каждого блока).
	 */
	private void createFire(List<BlockPos> positions) {
		for (BlockPos blockPos : positions) {
			if (world.random.nextInt(3) != 0) {
				continue;
			}

			if (!world.getBlockState(blockPos).isAir()) {
				continue;
			}

			if (!world.getBlockState(blockPos.down()).isOpaqueFullCube()) {
				continue;
			}

			world.setBlockState(blockPos, AbstractFireBlock.getState(world, blockPos));
		}
	}

	/**
	 * Добавляет предмет в список выпавших, объединяя со стеком того же типа,
	 * если это возможно. Если стек полностью поглощён — возврат без добавления.
	 */
	private static void addDroppedItem(List<DroppedItem> droppedItems, ItemStack item, BlockPos pos) {
		for (DroppedItem existing : droppedItems) {
			existing.merge(item);
			if (item.isEmpty()) {
				return;
			}
		}

		droppedItems.add(new DroppedItem(pos, item));
	}

	private boolean shouldDestroyBlocks() {
		return destructionType != Explosion.DestructionType.KEEP;
	}

	private static ExplosionBehavior resolveBehavior(@Nullable Entity entity) {
		return entity == null ? DEFAULT_BEHAVIOR : new EntityExplosionBehavior(entity);
	}

	/**
	 * Накопленный выпавший предмет с позицией спавна.
	 * Объединяет стеки одного типа для уменьшения количества сущностей в мире.
	 */
	static class DroppedItem {

		final BlockPos pos;
		ItemStack item;

		DroppedItem(BlockPos pos, ItemStack item) {
			this.pos = pos;
			this.item = item;
		}

		/**
		 * Пытается объединить переданный стек с текущим.
		 * Если типы совместимы — поглощает часть или весь стек {@code other}.
		 *
		 * @param other стек для объединения (изменяется in-place при поглощении)
		 */
		public void merge(ItemStack other) {
			if (ItemEntity.canMerge(item, other)) {
				item = ItemEntity.merge(item, other, Item.DEFAULT_MAX_COUNT);
			}
		}
	}
}
