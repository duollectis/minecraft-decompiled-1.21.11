package net.minecraft.entity.vehicle;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;

/**
 * Базовый класс для всех транспортных средств (лодки, вагонетки).
 * Управляет общей логикой получения урона, анимацией тряски при ударе
 * и дропом предмета при уничтожении.
 */
public abstract class VehicleEntity extends Entity {

	private static final int DAMAGE_WOBBLE_TICKS_ON_HIT = 10;
	private static final float DAMAGE_WOBBLE_STRENGTH_MULTIPLIER = 10.0F;
	private static final float DAMAGE_WOBBLE_KILL_THRESHOLD = 40.0F;

	protected static final TrackedData<Integer> DAMAGE_WOBBLE_TICKS =
			DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
	protected static final TrackedData<Integer> DAMAGE_WOBBLE_SIDE =
			DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
	protected static final TrackedData<Float> DAMAGE_WOBBLE_STRENGTH =
			DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.FLOAT);

	public VehicleEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return true;
	}

	/**
	 * Обрабатывает получение урона транспортным средством.
	 * Накапливает силу тряски; при превышении порога или атаке в режиме творчества — уничтожает.
	 */
	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isRemoved()) {
			return true;
		}

		if (isAlwaysInvulnerableTo(source)) {
			return false;
		}

		setDamageWobbleSide(-getDamageWobbleSide());
		setDamageWobbleTicks(DAMAGE_WOBBLE_TICKS_ON_HIT);
		scheduleVelocityUpdate();
		setDamageWobbleStrength(getDamageWobbleStrength() + amount * DAMAGE_WOBBLE_STRENGTH_MULTIPLIER);
		emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());

		boolean isCreativeAttack = source.getAttacker() instanceof PlayerEntity player
				&& player.getAbilities().creativeMode;
		boolean shouldKill = getDamageWobbleStrength() > DAMAGE_WOBBLE_KILL_THRESHOLD || shouldAlwaysKill(source);

		if (isCreativeAttack) {
			discard();
		} else if (shouldKill) {
			killAndDropSelf(world, source);
		}

		return true;
	}

	/**
	 * Переопределяется подклассами, которые должны уничтожаться при определённых источниках урона
	 * (например, {@link TntMinecartEntity} при огне или взрыве).
	 */
	protected boolean shouldAlwaysKill(DamageSource source) {
		return false;
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		return explosion.getCausingEntity() instanceof MobEntity
				&& !explosion.getWorld().getGameRules().getValue(GameRules.DO_MOB_GRIEFING);
	}

	/**
	 * Уничтожает транспортное средство и дропает указанный предмет с сохранением кастомного имени.
	 */
	public void killAndDropItem(ServerWorld world, Item item) {
		kill(world);

		if (world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
			ItemStack stack = new ItemStack(item);
			stack.set(DataComponentTypes.CUSTOM_NAME, getCustomName());
			dropStack(world, stack);
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(DAMAGE_WOBBLE_TICKS, 0);
		builder.add(DAMAGE_WOBBLE_SIDE, 1);
		builder.add(DAMAGE_WOBBLE_STRENGTH, 0.0F);
	}

	public void setDamageWobbleTicks(int ticks) {
		dataTracker.set(DAMAGE_WOBBLE_TICKS, ticks);
	}

	public void setDamageWobbleSide(int side) {
		dataTracker.set(DAMAGE_WOBBLE_SIDE, side);
	}

	public void setDamageWobbleStrength(float strength) {
		dataTracker.set(DAMAGE_WOBBLE_STRENGTH, strength);
	}

	public float getDamageWobbleStrength() {
		return dataTracker.get(DAMAGE_WOBBLE_STRENGTH);
	}

	public int getDamageWobbleTicks() {
		return dataTracker.get(DAMAGE_WOBBLE_TICKS);
	}

	public int getDamageWobbleSide() {
		return dataTracker.get(DAMAGE_WOBBLE_SIDE);
	}

	/**
	 * Уничтожает транспортное средство и дропает соответствующий предмет.
	 * Переопределяется в подклассах с инвентарём для дополнительного дропа содержимого.
	 */
	protected void killAndDropSelf(ServerWorld world, DamageSource damageSource) {
		killAndDropItem(world, asItem());
	}

	@Override
	public int getDefaultPortalCooldown() {
		return 10;
	}

	/** Возвращает предмет, соответствующий данному транспортному средству. */
	protected abstract Item asItem();
}
