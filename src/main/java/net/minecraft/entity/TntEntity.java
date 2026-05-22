package net.minecraft.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Сущность активированного ТНТ. Отсчитывает фитиль ({@code fuse} тиков),
 * после чего создаёт взрыв мощностью {@code explosionPower}. При телепортации
 * через портал Незера использует специальное поведение взрыва, не разрушающее портал.
 */
public class TntEntity extends Entity implements Ownable {

	private static final TrackedData<Integer> FUSE =
			DataTracker.registerData(TntEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<BlockState> BLOCK_STATE =
			DataTracker.registerData(TntEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE);
	private static final short DEFAULT_FUSE = 80;
	private static final float DEFAULT_EXPLOSION_POWER = 4.0F;
	private static final float MAX_EXPLOSION_POWER = 128.0F;
	private static final double SPAWN_VELOCITY_HORIZONTAL = 0.02;
	private static final double SPAWN_VELOCITY_VERTICAL = 0.2;
	private static final double TWO_PI = Math.PI * 2;
	private static final BlockState DEFAULT_BLOCK_STATE = Blocks.TNT.getDefaultState();
	private static final String BLOCK_STATE_NBT_KEY = "block_state";
	public static final String FUSE_NBT_KEY = "fuse";
	private static final String EXPLOSION_POWER_NBT_KEY = "explosion_power";
	private static final ExplosionBehavior TELEPORTED_EXPLOSION_BEHAVIOR = new ExplosionBehavior() {
		@Override
		public boolean canDestroyBlock(
				Explosion explosion,
				BlockView world,
				BlockPos pos,
				BlockState state,
				float power
		) {
			return state.isOf(Blocks.NETHER_PORTAL)
					? false
					: super.canDestroyBlock(explosion, world, pos, state, power);
		}

		@Override
		public Optional<Float> getBlastResistance(
				Explosion explosion,
				BlockView world,
				BlockPos pos,
				BlockState blockState,
				FluidState fluidState
		) {
			return blockState.isOf(Blocks.NETHER_PORTAL)
					? Optional.empty()
					: super.getBlastResistance(explosion, world, pos, blockState, fluidState);
		}
	};

	private @Nullable LazyEntityReference<LivingEntity> causingEntity;
	private boolean teleported;
	private float explosionPower = DEFAULT_EXPLOSION_POWER;

	public TntEntity(EntityType<? extends TntEntity> entityType, World world) {
		super(entityType, world);
		intersectionChecked = true;
	}

	public TntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
		this(EntityType.TNT, world);
		setPosition(x, y, z);
		double spawnAngle = world.random.nextDouble() * TWO_PI;
		setVelocity(-Math.sin(spawnAngle) * SPAWN_VELOCITY_HORIZONTAL, SPAWN_VELOCITY_VERTICAL, -Math.cos(spawnAngle) * SPAWN_VELOCITY_HORIZONTAL);
		setFuse(DEFAULT_FUSE);
		lastX = x;
		lastY = y;
		lastZ = z;
		causingEntity = LazyEntityReference.of(igniter);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(FUSE, (int) DEFAULT_FUSE);
		builder.add(BLOCK_STATE, DEFAULT_BLOCK_STATE);
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	public boolean canHit() {
		return !isRemoved();
	}

	@Override
	protected double getGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		tickPortalTeleportation();
		applyGravity();
		move(MovementType.SELF, getVelocity());
		tickBlockCollision();
		setVelocity(getVelocity().multiply(0.98));

		if (isOnGround()) {
			setVelocity(getVelocity().multiply(0.7, -0.5, 0.7));
		}

		int remainingFuse = getFuse() - 1;
		setFuse(remainingFuse);

		if (remainingFuse <= 0) {
			discard();

			if (!getEntityWorld().isClient()) {
				explode();
			}

			return;
		}

		updateWaterState();

		if (getEntityWorld().isClient()) {
			getEntityWorld().addParticleClient(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0.0, 0.0, 0.0);
		}
	}

	private void explode() {
		if (getEntityWorld() instanceof ServerWorld serverWorld
				&& serverWorld.getGameRules().getValue(GameRules.TNT_EXPLODES)
		) {
			getEntityWorld().createExplosion(
					this,
					Explosion.createDamageSource(getEntityWorld(), this),
					teleported ? TELEPORTED_EXPLOSION_BEHAVIOR : null,
					getX(),
					getBodyY(0.0625),
					getZ(),
					explosionPower,
					false,
					World.ExplosionSourceType.TNT
			);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putShort(FUSE_NBT_KEY, (short) getFuse());
		view.put(BLOCK_STATE_NBT_KEY, BlockState.CODEC, getBlockState());

		if (explosionPower != DEFAULT_EXPLOSION_POWER) {
			view.putFloat(EXPLOSION_POWER_NBT_KEY, explosionPower);
		}

		LazyEntityReference.writeData(causingEntity, view, "owner");
	}

	@Override
	protected void readCustomData(ReadView view) {
		setFuse(view.getShort(FUSE_NBT_KEY, DEFAULT_FUSE));
		setBlockState(view.<BlockState>read(BLOCK_STATE_NBT_KEY, BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE));
		explosionPower = MathHelper.clamp(view.getFloat(EXPLOSION_POWER_NBT_KEY, DEFAULT_EXPLOSION_POWER), 0.0F, MAX_EXPLOSION_POWER);
		causingEntity = LazyEntityReference.fromData(view, "owner");
	}

	@Override
	public @Nullable LivingEntity getOwner() {
		return LazyEntityReference.getLivingEntity(causingEntity, getEntityWorld());
	}

	@Override
	public void copyFrom(Entity original) {
		super.copyFrom(original);

		if (original instanceof TntEntity tntEntity) {
			causingEntity = tntEntity.causingEntity;
		}
	}

	public void setFuse(int fuse) {
		dataTracker.set(FUSE, fuse);
	}

	public int getFuse() {
		return dataTracker.get(FUSE);
	}

	public void setBlockState(BlockState state) {
		dataTracker.set(BLOCK_STATE, state);
	}

	public BlockState getBlockState() {
		return dataTracker.get(BLOCK_STATE);
	}

	private void setTeleported(boolean teleported) {
		this.teleported = teleported;
	}

	@Override
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		Entity entity = super.teleportTo(teleportTarget);

		if (entity instanceof TntEntity tntEntity) {
			tntEntity.setTeleported(true);
		}

		return entity;
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}
}
