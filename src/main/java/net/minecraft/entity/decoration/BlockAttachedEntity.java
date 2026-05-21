package net.minecraft.entity.decoration;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * {@code BlockAttachedEntity}.
 */
public abstract class BlockAttachedEntity extends Entity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private int attachCheckTimer;
	protected BlockPos attachedBlockPos;

	protected BlockAttachedEntity(EntityType<? extends BlockAttachedEntity> entityType, World world) {
		super(entityType, world);
	}

	protected BlockAttachedEntity(
			EntityType<? extends BlockAttachedEntity> type,
			World world,
			BlockPos attachedBlockPos
	) {
		this(type, world);
		this.attachedBlockPos = attachedBlockPos;
	}

	protected abstract void updateAttachmentPosition();

	@Override
	public void tick() {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.attemptTickInVoid();
			if (this.attachCheckTimer++ == 100) {
				this.attachCheckTimer = 0;
				if (!this.isRemoved() && !this.canStayAttached()) {
					this.discard();
					this.onBreak(serverWorld, null);
				}
			}
		}
	}

	public abstract boolean canStayAttached();

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public boolean handleAttack(Entity attacker) {
		if (attacker instanceof PlayerEntity playerEntity) {
			return !this.getEntityWorld().canEntityModifyAt(playerEntity, this.attachedBlockPos)
			       ? true
			       : this.sidedDamage(this.getDamageSources().playerAttack(playerEntity), 0.0F);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return !this.isAlwaysInvulnerableTo(source);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isAlwaysInvulnerableTo(source)) {
			return false;
		}
		else if (!world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				&& source.getAttacker() instanceof MobEntity) {
			return false;
		}
		else {
			if (!this.isRemoved()) {
				this.kill(world);
				this.scheduleVelocityUpdate();
				this.onBreak(world, source.getAttacker());
			}

			return true;
		}
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		Entity entity = explosion.getEntity();
		if (entity != null && entity.isTouchingWater()) {
			return true;
		}
		else {
			return explosion.preservesDecorativeEntities() ? super.isImmuneToExplosion(explosion) : true;
		}
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld && !this.isRemoved()
				&& movement.lengthSquared() > 0.0) {
			this.kill(serverWorld);
			this.onBreak(serverWorld, null);
		}
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld && !this.isRemoved()
				&& deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0) {
			this.kill(serverWorld);
			this.onBreak(serverWorld, null);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.put("block_pos", BlockPos.CODEC, this.getAttachedBlockPos());
	}

	@Override
	protected void readCustomData(ReadView view) {
		BlockPos blockPos = view.<BlockPos>read("block_pos", BlockPos.CODEC).orElse(null);
		if (blockPos != null && blockPos.isWithinDistance(this.getBlockPos(), 16.0)) {
			this.attachedBlockPos = blockPos;
		}
		else {
			LOGGER.error("Block-attached entity at invalid position: {}", blockPos);
		}
	}

	public abstract void onBreak(ServerWorld world, @Nullable Entity breaker);

	@Override
	protected boolean shouldSetPositionOnLoad() {
		return false;
	}

	@Override
	public void setPosition(double x, double y, double z) {
		this.attachedBlockPos = BlockPos.ofFloored(x, y, z);
		this.updateAttachmentPosition();
		this.velocityDirty = true;
	}

	public BlockPos getAttachedBlockPos() {
		return this.attachedBlockPos;
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
	}

	@Override
	public void calculateDimensions() {
	}
}
