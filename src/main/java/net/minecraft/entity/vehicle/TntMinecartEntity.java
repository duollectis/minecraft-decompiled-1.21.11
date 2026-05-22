package net.minecraft.entity.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * Вагонетка с ТНТ. Взрывается при столкновении с достаточной скоростью, падении с высоты,
 * попадании горящего снаряда или при активации рельсом-активатором.
 * Мощность взрыва масштабируется от скорости столкновения.
 */
public class TntMinecartEntity extends AbstractMinecartEntity {

	private static final byte PRIME_TNT_STATUS = 10;
	private static final String NBT_EXPLOSION_POWER = "explosion_power";
	private static final String NBT_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
	private static final String NBT_FUSE = "fuse";

	private static final float DEFAULT_EXPLOSION_POWER = 4.0F;
	private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0F;
	private static final int DEFAULT_FUSE_TICKS = -1;
	private static final int FUSE_ON_PRIME = 80;
	private static final int FUSE_RANDOM_RANGE = 20;
	private static final double MIN_COLLISION_SPEED_SQ = 0.01F;
	private static final double MIN_FALL_DISTANCE = 3.0;
	private static final double FALL_POWER_DIVISOR = 10.0;
	private static final double MAX_SPEED_CONTRIBUTION = 5.0;
	private static final float MAX_EXPLOSION_POWER = 128.0F;
	private static final float EXPLOSION_RANDOM_SCALE = 1.5F;
	private static final float SOUND_VOLUME = 1.0F;
	private static final float SOUND_PITCH = 1.0F;

	private @Nullable DamageSource damageSource;
	private int fuseTicks = DEFAULT_FUSE_TICKS;
	private float explosionPower = DEFAULT_EXPLOSION_POWER;
	private float explosionSpeedFactor = DEFAULT_EXPLOSION_SPEED_FACTOR;

	public TntMinecartEntity(EntityType<? extends TntMinecartEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public BlockState getDefaultContainedBlock() {
		return Blocks.TNT.getDefaultState();
	}

	@Override
	public void tick() {
		super.tick();

		if (fuseTicks > 0) {
			fuseTicks--;
			getEntityWorld().addParticleClient(
					ParticleTypes.SMOKE,
					getX(), getY() + 0.5, getZ(),
					0.0, 0.0, 0.0
			);
		} else if (fuseTicks == 0) {
			explode(damageSource, getVelocity().horizontalLengthSquared());
		}

		if (horizontalCollision) {
			double speedSq = getVelocity().horizontalLengthSquared();

			if (speedSq >= MIN_COLLISION_SPEED_SQ) {
				explode(damageSource, speedSq);
			}
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (source.getSource() instanceof PersistentProjectileEntity arrow && arrow.isOnFire()) {
			DamageSource explosionSource = getDamageSources().explosion(this, source.getAttacker());
			explode(explosionSource, arrow.getVelocity().lengthSquared());
		}

		return super.damage(world, source, amount);
	}

	/**
	 * При уничтожении: если источник урона требует детонации или скорость достаточна —
	 * поджигает фитиль; иначе дропает предмет.
	 */
	@Override
	public void killAndDropSelf(ServerWorld world, DamageSource damageSource) {
		double speedSq = getVelocity().horizontalLengthSquared();

		if (!shouldDetonate(damageSource) && speedSq < MIN_COLLISION_SPEED_SQ) {
			killAndDropItem(world, asItem());
			return;
		}

		if (fuseTicks < 0) {
			prime(damageSource);
			fuseTicks = random.nextInt(FUSE_RANDOM_RANGE) + random.nextInt(FUSE_RANDOM_RANGE);
		}
	}

	@Override
	protected Item asItem() {
		return Items.TNT_MINECART;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.TNT_MINECART);
	}

	/**
	 * Создаёт взрыв с мощностью, масштабированной от скорости столкновения.
	 * Взрыв не создаётся, если правило {@code tntExplodes} отключено.
	 *
	 * @param damageSource источник урона для атрибуции взрыва
	 * @param speedSquared квадрат горизонтальной скорости в момент взрыва
	 */
	protected void explode(@Nullable DamageSource damageSource, double speedSquared) {
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		if (!serverWorld.getGameRules().getValue(GameRules.TNT_EXPLODES)) {
			if (isPrimed()) {
				discard();
			}

			return;
		}

		double speedContribution = Math.min(Math.sqrt(speedSquared), MAX_SPEED_CONTRIBUTION);
		float finalPower = (float) (explosionPower
				+ explosionSpeedFactor * random.nextDouble() * EXPLOSION_RANDOM_SCALE * speedContribution);

		serverWorld.createExplosion(
				this, damageSource, null,
				getX(), getY(), getZ(),
				finalPower, false,
				World.ExplosionSourceType.TNT
		);
		discard();
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (fallDistance >= MIN_FALL_DISTANCE) {
			double power = fallDistance / FALL_POWER_DIVISOR;
			explode(this.damageSource, power * power);
		}

		return super.handleFallDamage(fallDistance, damagePerDistance, damageSource);
	}

	@Override
	public void onActivatorRail(ServerWorld serverWorld, int x, int y, int z, boolean powered) {
		if (powered && fuseTicks < 0) {
			prime(null);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == PRIME_TNT_STATUS) {
			prime(null);
		} else {
			super.handleStatus(status);
		}
	}

	/**
	 * Поджигает фитиль вагонетки с ТНТ на {@code FUSE_ON_PRIME} тиков.
	 * Не действует, если правило {@code tntExplodes} отключено.
	 *
	 * @param source источник урона, инициировавший поджиг (может быть {@code null})
	 */
	public void prime(@Nullable DamageSource source) {
		if (getEntityWorld() instanceof ServerWorld serverWorld
				&& !serverWorld.getGameRules().getValue(GameRules.TNT_EXPLODES)) {
			return;
		}

		fuseTicks = FUSE_ON_PRIME;

		if (getEntityWorld().isClient()) {
			return;
		}

		if (source != null && damageSource == null) {
			damageSource = getDamageSources().explosion(this, source.getAttacker());
		}

		getEntityWorld().sendEntityStatus(this, PRIME_TNT_STATUS);

		if (!isSilent()) {
			getEntityWorld().playSound(
					null,
					getX(), getY(), getZ(),
					SoundEvents.ENTITY_TNT_PRIMED,
					SoundCategory.BLOCKS,
					SOUND_VOLUME, SOUND_PITCH
			);
		}
	}

	public int getFuseTicks() {
		return fuseTicks;
	}

	public boolean isPrimed() {
		return fuseTicks > DEFAULT_FUSE_TICKS;
	}

	@Override
	public float getEffectiveExplosionResistance(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState blockState,
			FluidState fluidState,
			float max
	) {
		if (!isPrimed()) {
			return super.getEffectiveExplosionResistance(explosion, world, pos, blockState, fluidState, max);
		}

		boolean isRailOrAboveRail = blockState.isIn(BlockTags.RAILS)
				|| world.getBlockState(pos.up()).isIn(BlockTags.RAILS);
		return isRailOrAboveRail
				? 0.0F
				: super.getEffectiveExplosionResistance(explosion, world, pos, blockState, fluidState, max);
	}

	@Override
	public boolean canExplosionDestroyBlock(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState state,
			float power
	) {
		if (!isPrimed()) {
			return super.canExplosionDestroyBlock(explosion, world, pos, state, power);
		}

		boolean isRailOrAboveRail = state.isIn(BlockTags.RAILS)
				|| world.getBlockState(pos.up()).isIn(BlockTags.RAILS);
		return !isRailOrAboveRail && super.canExplosionDestroyBlock(explosion, world, pos, state, power);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		fuseTicks = view.getInt(NBT_FUSE, DEFAULT_FUSE_TICKS);
		explosionPower = MathHelper.clamp(view.getFloat(NBT_EXPLOSION_POWER, DEFAULT_EXPLOSION_POWER), 0.0F, MAX_EXPLOSION_POWER);
		explosionSpeedFactor = MathHelper.clamp(view.getFloat(NBT_EXPLOSION_SPEED_FACTOR, DEFAULT_EXPLOSION_SPEED_FACTOR), 0.0F, MAX_EXPLOSION_POWER);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt(NBT_FUSE, fuseTicks);

		if (explosionPower != DEFAULT_EXPLOSION_POWER) {
			view.putFloat(NBT_EXPLOSION_POWER, explosionPower);
		}

		if (explosionSpeedFactor != DEFAULT_EXPLOSION_SPEED_FACTOR) {
			view.putFloat(NBT_EXPLOSION_SPEED_FACTOR, explosionSpeedFactor);
		}
	}

	@Override
	protected boolean shouldAlwaysKill(DamageSource source) {
		return shouldDetonate(source);
	}

	private static boolean shouldDetonate(DamageSource source) {
		return source.getSource() instanceof ProjectileEntity projectile
				? projectile.isOnFire()
				: source.isIn(DamageTypeTags.IS_FIRE) || source.isIn(DamageTypeTags.IS_EXPLOSION);
	}
}
