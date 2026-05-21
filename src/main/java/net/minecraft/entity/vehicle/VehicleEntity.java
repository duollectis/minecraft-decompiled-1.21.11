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
 * {@code VehicleEntity}.
 */
public abstract class VehicleEntity extends Entity {

	protected static final TrackedData<Integer>
			DAMAGE_WOBBLE_TICKS =
			DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
	protected static final TrackedData<Integer>
			DAMAGE_WOBBLE_SIDE =
			DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
	protected static final TrackedData<Float>
			DAMAGE_WOBBLE_STRENGTH =
			DataTracker.registerData(VehicleEntity.class, TrackedDataHandlerRegistry.FLOAT);

	public VehicleEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return true;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isRemoved()) {
			return true;
		}
		else if (this.isAlwaysInvulnerableTo(source)) {
			return false;
		}
		else {
			this.setDamageWobbleSide(-this.getDamageWobbleSide());
			this.setDamageWobbleTicks(10);
			this.scheduleVelocityUpdate();
			this.setDamageWobbleStrength(this.getDamageWobbleStrength() + amount * 10.0F);
			this.emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
			boolean
					bl =
					source.getAttacker() instanceof PlayerEntity playerEntity
							&& playerEntity.getAbilities().creativeMode;
			if ((bl || !(this.getDamageWobbleStrength() > 40.0F)) && !this.shouldAlwaysKill(source)) {
				if (bl) {
					this.discard();
				}
			}
			else {
				this.killAndDropSelf(world, source);
			}

			return true;
		}
	}

	/**
	 * Определяет, следует ли always kill.
	 *
	 * @param source source
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldAlwaysKill(DamageSource source) {
		return false;
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		return explosion.getCausingEntity() instanceof MobEntity && !explosion
				.getWorld()
				.getGameRules()
				.getValue(GameRules.DO_MOB_GRIEFING);
	}

	/**
	 * Уничтожает and drop item.
	 *
	 * @param world world
	 * @param item item
	 */
	public void killAndDropItem(ServerWorld world, Item item) {
		this.kill(world);
		if (world.getGameRules().getValue(GameRules.ENTITY_DROPS)) {
			ItemStack itemStack = new ItemStack(item);
			itemStack.set(DataComponentTypes.CUSTOM_NAME, this.getCustomName());
			this.dropStack(world, itemStack);
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(DAMAGE_WOBBLE_TICKS, 0);
		builder.add(DAMAGE_WOBBLE_SIDE, 1);
		builder.add(DAMAGE_WOBBLE_STRENGTH, 0.0F);
	}

	public void setDamageWobbleTicks(int damageWobbleTicks) {
		this.dataTracker.set(DAMAGE_WOBBLE_TICKS, damageWobbleTicks);
	}

	public void setDamageWobbleSide(int damageWobbleSide) {
		this.dataTracker.set(DAMAGE_WOBBLE_SIDE, damageWobbleSide);
	}

	public void setDamageWobbleStrength(float damageWobbleStrength) {
		this.dataTracker.set(DAMAGE_WOBBLE_STRENGTH, damageWobbleStrength);
	}

	public float getDamageWobbleStrength() {
		return this.dataTracker.get(DAMAGE_WOBBLE_STRENGTH);
	}

	public int getDamageWobbleTicks() {
		return this.dataTracker.get(DAMAGE_WOBBLE_TICKS);
	}

	public int getDamageWobbleSide() {
		return this.dataTracker.get(DAMAGE_WOBBLE_SIDE);
	}

	/**
	 * Уничтожает and drop self.
	 *
	 * @param world world
	 * @param damageSource damage source
	 */
	protected void killAndDropSelf(ServerWorld world, DamageSource damageSource) {
		this.killAndDropItem(world, this.asItem());
	}

	@Override
	public int getDefaultPortalCooldown() {
		return 10;
	}

	/**
	 * As item.
	 *
	 * @return Item — результат операции
	 */
	protected abstract Item asItem();
}
