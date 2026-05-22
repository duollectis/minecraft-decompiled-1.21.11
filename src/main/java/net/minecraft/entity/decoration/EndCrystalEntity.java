package net.minecraft.entity.decoration;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Кристалл Края — декоративная сущность, поддерживающая здоровье дракона.
 * При уничтожении создаёт взрыв и уведомляет {@link EnderDragonFight}.
 * Иммунен к урону от самого дракона.
 */
public class EndCrystalEntity extends Entity {

	private static final TrackedData<Optional<BlockPos>> BEAM_TARGET = DataTracker.registerData(
			EndCrystalEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS
	);
	private static final TrackedData<Boolean> SHOW_BOTTOM =
			DataTracker.registerData(EndCrystalEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private static final float EXPLOSION_RADIUS = 6.0F;

	public int endCrystalAge;

	public EndCrystalEntity(EntityType<? extends EndCrystalEntity> entityType, World world) {
		super(entityType, world);
		intersectionChecked = true;
		endCrystalAge = random.nextInt(100000);
	}

	public EndCrystalEntity(World world, double x, double y, double z) {
		this(EntityType.END_CRYSTAL, world);
		setPosition(x, y, z);
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(BEAM_TARGET, Optional.empty());
		builder.add(SHOW_BOTTOM, true);
	}

	@Override
	public void tick() {
		endCrystalAge++;
		tickBlockCollision();
		tickPortalTeleportation();
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			BlockPos blockPos = getBlockPos();
			if (serverWorld.getEnderDragonFight() != null
					&& getEntityWorld().getBlockState(blockPos).isAir()
			) {
				getEntityWorld().setBlockState(blockPos, AbstractFireBlock.getState(getEntityWorld(), blockPos));
			}
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.putNullable("beam_target", BlockPos.CODEC, getBeamTarget());
		view.putBoolean("ShowBottom", shouldShowBottom());
	}

	@Override
	protected void readCustomData(ReadView view) {
		setBeamTarget(view.<BlockPos>read("beam_target", BlockPos.CODEC).orElse(null));
		setShowBottom(view.getBoolean("ShowBottom", true));
	}

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public final boolean clientDamage(DamageSource source) {
		return isAlwaysInvulnerableTo(source)
				? false
				: !(source.getAttacker() instanceof EnderDragonEntity);
	}

	/**
	 * При уничтожении (не взрывом) создаёт взрыв с радиусом {@value #EXPLOSION_RADIUS} блоков
	 * и уведомляет активный бой с драконом о потере кристалла.
	 */
	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isAlwaysInvulnerableTo(source)) {
			return false;
		}

		if (source.getAttacker() instanceof EnderDragonEntity) {
			return false;
		}

		if (!isRemoved()) {
			remove(Entity.RemovalReason.KILLED);
			if (!source.isIn(DamageTypeTags.IS_EXPLOSION)) {
				DamageSource explosionSource = source.getAttacker() != null
						? getDamageSources().explosion(this, source.getAttacker())
						: null;
				world.createExplosion(
						this,
						explosionSource,
						null,
						getX(), getY(), getZ(),
						EXPLOSION_RADIUS,
						false,
						World.ExplosionSourceType.BLOCK
				);
			}

			crystalDestroyed(world, source);
		}

		return true;
	}

	@Override
	public void kill(ServerWorld world) {
		crystalDestroyed(world, getDamageSources().generic());
		super.kill(world);
	}

	private void crystalDestroyed(ServerWorld world, DamageSource source) {
		EnderDragonFight fight = world.getEnderDragonFight();
		if (fight != null) {
			fight.crystalDestroyed(this, source);
		}
	}

	public void setBeamTarget(@Nullable BlockPos beamTarget) {
		getDataTracker().set(BEAM_TARGET, Optional.ofNullable(beamTarget));
	}

	public @Nullable BlockPos getBeamTarget() {
		return getDataTracker().get(BEAM_TARGET).orElse(null);
	}

	public void setShowBottom(boolean showBottom) {
		getDataTracker().set(SHOW_BOTTOM, showBottom);
	}

	public boolean shouldShowBottom() {
		return getDataTracker().get(SHOW_BOTTOM);
	}

	@Override
	public boolean shouldRender(double distance) {
		return super.shouldRender(distance) || getBeamTarget() != null;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.END_CRYSTAL);
	}
}
