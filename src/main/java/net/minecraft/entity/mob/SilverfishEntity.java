package net.minecraft.entity.mob;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.InfestedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Чешуйница — маленький моб, прячущийся в камне.
 */
public class SilverfishEntity extends HostileEntity {

	private SilverfishEntity.@Nullable CallForHelpGoal callForHelpGoal;

	public SilverfishEntity(EntityType<? extends SilverfishEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		callForHelpGoal = new SilverfishEntity.CallForHelpGoal(this);
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(1, new PowderSnowJumpGoal(this, getEntityWorld()));
		goalSelector.add(3, callForHelpGoal);
		goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.add(5, new SilverfishEntity.WanderAndInfestGoal(this));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	public static DefaultAttributeContainer.Builder createSilverfishAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 8.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 1.0);
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SILVERFISH_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_SILVERFISH_STEP, 0.15F, 1.0F);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		if ((source.getAttacker() != null || source.isIn(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH))
				&& callForHelpGoal != null) {
			callForHelpGoal.onHurt();
		}

		return super.damage(world, source, amount);
	}

	@Override
	public void tick() {
		bodyYaw = getYaw();
		super.tick();
	}

	@Override
	public void setBodyYaw(float bodyYaw) {
		setYaw(bodyYaw);
		super.setBodyYaw(bodyYaw);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return InfestedBlock.isInfestable(world.getBlockState(pos.down()))
				? 10.0F
				: super.getPathfindingFavor(pos, world);
	}

	public static boolean canSpawn(
			EntityType<SilverfishEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		if (!canSpawnIgnoreLightLevel(type, world, spawnReason, pos, random)) {
			return false;
		}

		if (SpawnReason.isAnySpawner(spawnReason)) {
			return true;
		}

		PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5.0, true);
		return nearestPlayer == null;
	}

	static class CallForHelpGoal extends Goal {

		private final SilverfishEntity silverfish;
		private int delay;

		public CallForHelpGoal(SilverfishEntity silverfish) {
			this.silverfish = silverfish;
		}

		private static final int CALL_DELAY_TICKS = 20;
		private static final int SEARCH_RADIUS_Y = 5;
		private static final int SEARCH_RADIUS_XZ = 10;
		private static final int BLOCK_UPDATE_FLAGS = 3;

		public void onHurt() {
			if (delay == 0) {
				delay = getTickCount(CALL_DELAY_TICKS);
			}
		}

		@Override
		public boolean canStart() {
			return delay > 0;
		}

		@Override
		public void tick() {
			delay--;
			if (delay > 0) {
				return;
			}

			World world = silverfish.getEntityWorld();
			Random random = silverfish.getRandom();
			BlockPos origin = silverfish.getBlockPos();

			for (int dy = 0; dy <= SEARCH_RADIUS_Y && dy >= -SEARCH_RADIUS_Y; dy = (dy <= 0 ? 1 : 0) - dy) {
				for (int dx = 0; dx <= SEARCH_RADIUS_XZ && dx >= -SEARCH_RADIUS_XZ; dx = (dx <= 0 ? 1 : 0) - dx) {
					for (int dz = 0; dz <= SEARCH_RADIUS_XZ && dz >= -SEARCH_RADIUS_XZ; dz = (dz <= 0 ? 1 : 0) - dz) {
						BlockPos candidate = origin.add(dx, dy, dz);
						BlockState blockState = world.getBlockState(candidate);
						Block block = blockState.getBlock();

						if (block instanceof InfestedBlock infestedBlock) {
							if (castToServerWorld(world).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
								world.breakBlock(candidate, true, silverfish);
							}
							else {
								world.setBlockState(
										candidate,
										infestedBlock.toRegularState(world.getBlockState(candidate)),
										BLOCK_UPDATE_FLAGS
								);
							}

							if (random.nextBoolean()) {
								return;
							}
						}
					}
				}
			}
		}
	}

	static class WanderAndInfestGoal extends WanderAroundGoal {

		private @Nullable Direction direction;
		private boolean canInfest;

		public WanderAndInfestGoal(SilverfishEntity silverfish) {
			super(silverfish, 1.0, 10);
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		private static final int INFEST_CHANCE_TICKS = 10;
		private static final int BLOCK_UPDATE_FLAGS = 3;

		@Override
		public boolean canStart() {
			if (mob.getTarget() != null) {
				return false;
			}

			if (!mob.getNavigation().isIdle()) {
				return false;
			}

			Random random = mob.getRandom();
			if (getServerWorld(mob).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)
					&& random.nextInt(toGoalTicks(INFEST_CHANCE_TICKS)) == 0) {
				direction = Direction.random(random);
				BlockPos target = BlockPos
						.ofFloored(mob.getX(), mob.getY() + 0.5, mob.getZ())
						.offset(direction);
				BlockState blockState = mob.getEntityWorld().getBlockState(target);

				if (InfestedBlock.isInfestable(blockState)) {
					canInfest = true;
					return true;
				}
			}

			canInfest = false;
			return super.canStart();
		}

		@Override
		public boolean shouldContinue() {
			if (canInfest) {
				return false;
			}

			return super.shouldContinue();
		}

		@Override
		public void start() {
			if (!canInfest) {
				super.start();
				return;
			}

			WorldAccess worldAccess = mob.getEntityWorld();
			BlockPos target = BlockPos
					.ofFloored(mob.getX(), mob.getY() + 0.5, mob.getZ())
					.offset(direction);
			BlockState blockState = worldAccess.getBlockState(target);

			if (InfestedBlock.isInfestable(blockState)) {
				worldAccess.setBlockState(target, InfestedBlock.fromRegularState(blockState), BLOCK_UPDATE_FLAGS);
				mob.playSpawnEffects();
				mob.discard();
			}
		}
	}
}
