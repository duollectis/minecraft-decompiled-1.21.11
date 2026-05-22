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
 * Базовый класс для сущностей, прикреплённых к конкретному блоку мира.
 * Автоматически уничтожается, если блок-опора исчезает (проверка каждые 100 тиков).
 */
public abstract class BlockAttachedEntity extends Entity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int ATTACH_CHECK_INTERVAL = 100;

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

	/** Обновляет позицию и хитбокс сущности относительно блока-опоры. */
	protected abstract void updateAttachmentPosition();

	@Override
	public void tick() {
		if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		attemptTickInVoid();
		if (attachCheckTimer++ == ATTACH_CHECK_INTERVAL) {
			attachCheckTimer = 0;
			if (!isRemoved() && !canStayAttached()) {
				discard();
				onBreak(serverWorld, null);
			}
		}
	}

	/** Проверяет, может ли сущность оставаться прикреплённой к текущему блоку. */
	public abstract boolean canStayAttached();

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public boolean handleAttack(Entity attacker) {
		if (!(attacker instanceof PlayerEntity playerEntity)) {
			return false;
		}

		return !getEntityWorld().canEntityModifyAt(playerEntity, attachedBlockPos)
			? true
			: sidedDamage(getDamageSources().playerAttack(playerEntity), 0.0F);
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return !isAlwaysInvulnerableTo(source);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isAlwaysInvulnerableTo(source)) {
			return false;
		}

		if (!world.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
				&& source.getAttacker() instanceof MobEntity) {
			return false;
		}

		if (!isRemoved()) {
			kill(world);
			scheduleVelocityUpdate();
			onBreak(world, source.getAttacker());
		}

		return true;
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		Entity explosionEntity = explosion.getEntity();
		if (explosionEntity != null && explosionEntity.isTouchingWater()) {
			return true;
		}

		return explosion.preservesDecorativeEntities() ? super.isImmuneToExplosion(explosion) : true;
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		if (getEntityWorld() instanceof ServerWorld serverWorld
				&& !isRemoved()
				&& movement.lengthSquared() > 0.0) {
			kill(serverWorld);
			onBreak(serverWorld, null);
		}
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (getEntityWorld() instanceof ServerWorld serverWorld
				&& !isRemoved()
				&& deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0) {
			kill(serverWorld);
			onBreak(serverWorld, null);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		view.put("block_pos", BlockPos.CODEC, getAttachedBlockPos());
	}

	@Override
	protected void readCustomData(ReadView view) {
		BlockPos blockPos = view.<BlockPos>read("block_pos", BlockPos.CODEC).orElse(null);
		if (blockPos == null || !blockPos.isWithinDistance(getBlockPos(), 16.0)) {
			LOGGER.error("Block-attached entity at invalid position: {}", blockPos);
			return;
		}

		attachedBlockPos = blockPos;
	}

	/**
	 * Вызывается при разрушении сущности — для дропа предметов и звуков.
	 *
	 * @param breaker сущность, разрушившая объект, или {@code null} если причина — мир
	 */
	public abstract void onBreak(ServerWorld world, @Nullable Entity breaker);

	@Override
	protected boolean shouldSetPositionOnLoad() {
		return false;
	}

	@Override
	public void setPosition(double x, double y, double z) {
		attachedBlockPos = BlockPos.ofFloored(x, y, z);
		updateAttachmentPosition();
		velocityDirty = true;
	}

	public BlockPos getAttachedBlockPos() {
		return attachedBlockPos;
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
	}

	@Override
	public void calculateDimensions() {
	}
}
