package net.minecraft.entity.projectile.thrown;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.Predicate;

/**
 * Базовый класс для всех бросаемых зелий (плескательных и длительных).
 * <p>
 * Обрабатывает попадание в блок (тушение огня для водяного зелья),
 * попадание в сущность и общую логику взрыва при столкновении.
 * Конкретный эффект при взрыве делегируется в {@link #spawnAreaEffectCloud}.
 */
public abstract class PotionEntity extends ThrownItemEntity {

	public static final double POTION_EXPLOSION_RADIUS = 4.0;
	protected static final double WATER_POTION_EXPLOSION_SQUARED_RADIUS = 16.0;

	/** Предикат: сущности, на которых действует водяное зелье (горящие или уязвимые к воде). */
	public static final Predicate<LivingEntity> AFFECTED_BY_WATER =
			entity -> entity.hurtByWater() || entity.isOnFire();

	/** Код мирового события для мгновенного зелья. */
	private static final int INSTANT_EFFECT_WORLD_EVENT = 2007;

	/** Код мирового события для обычного зелья. */
	private static final int NORMAL_EFFECT_WORLD_EVENT = 2002;

	/** Расширение области поиска сущностей по вертикали. */
	private static final double ENTITY_SEARCH_Y_EXPAND = 2.0;

	public PotionEntity(EntityType<? extends PotionEntity> entityType, World world) {
		super(entityType, world);
	}

	public PotionEntity(EntityType<? extends PotionEntity> type, World world, LivingEntity owner, ItemStack stack) {
		super(type, owner, world, stack);
	}

	public PotionEntity(
			EntityType<? extends PotionEntity> type,
			World world,
			double x,
			double y,
			double z,
			ItemStack stack
	) {
		super(type, x, y, z, world, stack);
	}

	@Override
	protected double getGravity() {
		return 0.05;
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		if (getEntityWorld().isClient()) {
			return;
		}

		ItemStack itemStack = getStack();
		PotionContentsComponent contents = itemStack.getOrDefault(
				DataComponentTypes.POTION_CONTENTS,
				PotionContentsComponent.DEFAULT
		);

		if (!contents.matches(Potions.WATER)) {
			return;
		}

		Direction hitSide = blockHitResult.getSide();
		BlockPos hitPos = blockHitResult.getBlockPos();
		BlockPos offsetPos = hitPos.offset(hitSide);

		extinguishFire(offsetPos);
		extinguishFire(offsetPos.offset(hitSide.getOpposite()));

		for (Direction horizontal : Direction.Type.HORIZONTAL) {
			extinguishFire(offsetPos.offset(horizontal));
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		ItemStack itemStack = getStack();
		PotionContentsComponent contents = itemStack.getOrDefault(
				DataComponentTypes.POTION_CONTENTS,
				PotionContentsComponent.DEFAULT
		);

		if (contents.matches(Potions.WATER)) {
			explodeWaterPotion(serverWorld);
		} else if (contents.hasEffects()) {
			spawnAreaEffectCloud(serverWorld, itemStack, hitResult);
		}

		boolean isInstant = contents.potion().isPresent()
				&& contents.potion().get().value().hasInstantEffect();
		int worldEvent = isInstant ? INSTANT_EFFECT_WORLD_EVENT : NORMAL_EFFECT_WORLD_EVENT;
		serverWorld.syncWorldEvent(worldEvent, getBlockPos(), contents.getColor());
		discard();
	}

	private void explodeWaterPotion(ServerWorld world) {
		Box searchBox = getBoundingBox().expand(POTION_EXPLOSION_RADIUS, ENTITY_SEARCH_Y_EXPAND, POTION_EXPLOSION_RADIUS);

		for (LivingEntity entity : getEntityWorld().getEntitiesByClass(LivingEntity.class, searchBox, AFFECTED_BY_WATER)) {
			if (squaredDistanceTo(entity) >= WATER_POTION_EXPLOSION_SQUARED_RADIUS) {
				continue;
			}

			if (entity.hurtByWater()) {
				entity.damage(world, getDamageSources().indirectMagic(this, getOwner()), 1.0F);
			}

			if (entity.isOnFire() && entity.isAlive()) {
				entity.extinguishWithSound();
			}
		}

		for (AxolotlEntity axolotl : getEntityWorld().getNonSpectatingEntities(AxolotlEntity.class, searchBox)) {
			axolotl.hydrateFromPotion();
		}
	}

	/**
	 * Создаёт облако эффекта (или применяет эффекты напрямую) при взрыве зелья.
	 * Реализация зависит от типа зелья: плескательное применяет эффекты мгновенно,
	 * длительное создаёт {@link net.minecraft.entity.AreaEffectCloudEntity}.
	 *
	 * @param world     серверный мир
	 * @param stack     стек зелья с компонентами эффектов
	 * @param hitResult результат столкновения для определения позиции
	 */
	protected abstract void spawnAreaEffectCloud(ServerWorld world, ItemStack stack, HitResult hitResult);

	private void extinguishFire(BlockPos pos) {
		BlockState blockState = getEntityWorld().getBlockState(pos);

		if (blockState.isIn(BlockTags.FIRE)) {
			getEntityWorld().breakBlock(pos, false, this);
		} else if (AbstractCandleBlock.isLitCandle(blockState)) {
			AbstractCandleBlock.extinguish(null, blockState, getEntityWorld(), pos);
		} else if (CampfireBlock.isLitCampfire(blockState)) {
			getEntityWorld().syncWorldEvent(null, 1009, pos, 0);
			CampfireBlock.extinguish(getOwner(), getEntityWorld(), pos, blockState);
			getEntityWorld().setBlockState(pos, blockState.with(CampfireBlock.LIT, false));
		}
	}

	@Override
	public DoubleDoubleImmutablePair getKnockback(LivingEntity target, DamageSource source) {
		double deltaX = target.getEntityPos().x - getEntityPos().x;
		double deltaZ = target.getEntityPos().z - getEntityPos().z;
		return DoubleDoubleImmutablePair.of(deltaX, deltaZ);
	}
}
