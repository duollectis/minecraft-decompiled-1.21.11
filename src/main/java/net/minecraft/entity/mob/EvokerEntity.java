package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Эвокер — иллагер-заклинатель, вызывающий вексов и клыки эвокера.
 * Может перекрашивать синих овец в красных (Wololo). Считает вексов
 * своего призыва союзниками. Получает 10 очков опыта при смерти.
 */
public class EvokerEntity extends SpellcastingIllagerEntity {

	private @Nullable SheepEntity wololoTarget;

	public EvokerEntity(EntityType<? extends EvokerEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 10;
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EvokerEntity.LookAtTargetOrWololoTarget());
		goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0F, 0.6, 1.0));
		goalSelector.add(3, new FleeEntityGoal<>(this, CreakingEntity.class, 8.0F, 0.6, 1.0));
		goalSelector.add(4, new EvokerEntity.SummonVexGoal());
		goalSelector.add(5, new EvokerEntity.ConjureFangsGoal());
		goalSelector.add(6, new EvokerEntity.WololoGoal());
		goalSelector.add(8, new WanderAroundGoal(this, 0.6));
		goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0F, 1.0F));
		goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
		targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
		targetSelector.add(
				2,
				new ActiveTargetGoal<>(this, PlayerEntity.class, true).setMaxTimeWithoutVisibility(300)
		);
		targetSelector.add(
				3,
				new ActiveTargetGoal<>(this, MerchantEntity.class, false).setMaxTimeWithoutVisibility(300)
		);
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, false));
	}

	public static DefaultAttributeContainer.Builder createEvokerAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.5)
		                    .add(EntityAttributes.FOLLOW_RANGE, 12.0)
		                    .add(EntityAttributes.MAX_HEALTH, 24.0);
	}

	@Override
	public SoundEvent getCelebratingSound() {
		return SoundEvents.ENTITY_EVOKER_CELEBRATE;
	}

	@Override
	public boolean isInSameTeam(Entity other) {
		if (other == this) {
			return true;
		}

		if (super.isInSameTeam(other)) {
			return true;
		}

		return other instanceof VexEntity vexEntity && vexEntity.getOwner() != null
				&& isInSameTeam(vexEntity.getOwner());
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_EVOKER_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_EVOKER_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_EVOKER_HURT;
	}

	void setWololoTarget(@Nullable SheepEntity wololoTarget) {
		this.wololoTarget = wololoTarget;
	}

	@Nullable
	SheepEntity getWololoTarget() {
		return wololoTarget;
	}

	@Override
	protected SoundEvent getCastSpellSound() {
		return SoundEvents.ENTITY_EVOKER_CAST_SPELL;
	}

	@Override
	public void addBonusForWave(ServerWorld world, int wave, boolean unused) {
	}

	class ConjureFangsGoal extends SpellcastingIllagerEntity.CastSpellGoal {

		@Override
		protected int getSpellTicks() {
			return 40;
		}

		@Override
		protected int startTimeDelay() {
			return 100;
		}

		@Override
		protected void castSpell() {
			LivingEntity target = EvokerEntity.this.getTarget();
			double minY = Math.min(target.getY(), EvokerEntity.this.getY());
			double maxY = Math.max(target.getY(), EvokerEntity.this.getY()) + 1.0;
			float yaw = (float) MathHelper.atan2(
					target.getZ() - EvokerEntity.this.getZ(),
					target.getX() - EvokerEntity.this.getX()
			);

			if (EvokerEntity.this.squaredDistanceTo(target) < 9.0) {
				for (int ringIndex = 0; ringIndex < 5; ringIndex++) {
					float angle = yaw + ringIndex * (float) Math.PI * 0.4F;
					conjureFangs(
							EvokerEntity.this.getX() + MathHelper.cos(angle) * 1.5,
							EvokerEntity.this.getZ() + MathHelper.sin(angle) * 1.5,
							minY, maxY, angle, 0
					);
				}

				for (int outerIndex = 0; outerIndex < 8; outerIndex++) {
					float angle = yaw + outerIndex * (float) Math.PI * 2.0F / 8.0F + (float) (Math.PI * 2.0 / 5.0);
					conjureFangs(
							EvokerEntity.this.getX() + MathHelper.cos(angle) * 2.5,
							EvokerEntity.this.getZ() + MathHelper.sin(angle) * 2.5,
							minY, maxY, angle, 3
					);
				}
			} else {
				for (int lineIndex = 0; lineIndex < 16; lineIndex++) {
					double dist = 1.25 * (lineIndex + 1);
					int warmup = lineIndex;
					conjureFangs(
							EvokerEntity.this.getX() + MathHelper.cos(yaw) * dist,
							EvokerEntity.this.getZ() + MathHelper.sin(yaw) * dist,
							minY, maxY, yaw, warmup
					);
				}
			}
		}

		private void conjureFangs(double x, double z, double maxY, double y, float yaw, int warmup) {
			BlockPos spawnPos = BlockPos.ofFloored(x, y, z);
			boolean foundGround = false;
			double heightOffset = 0.0;

			do {
				BlockPos below = spawnPos.down();
				BlockState belowState = EvokerEntity.this.getEntityWorld().getBlockState(below);
				if (belowState.isSideSolidFullSquare(EvokerEntity.this.getEntityWorld(), below, Direction.UP)) {
					if (!EvokerEntity.this.getEntityWorld().isAir(spawnPos)) {
						BlockState atPos = EvokerEntity.this.getEntityWorld().getBlockState(spawnPos);
						VoxelShape shape = atPos.getCollisionShape(EvokerEntity.this.getEntityWorld(), spawnPos);
						if (!shape.isEmpty()) {
							heightOffset = shape.getMax(Direction.Axis.Y);
						}
					}

					foundGround = true;
					break;
				}

				spawnPos = spawnPos.down();
			} while (spawnPos.getY() >= MathHelper.floor(maxY) - 1);

			if (foundGround) {
				double spawnY = spawnPos.getY() + heightOffset;
				EvokerEntity.this.getEntityWorld().spawnEntity(
						new EvokerFangsEntity(EvokerEntity.this.getEntityWorld(), x, spawnY, z, yaw, warmup, EvokerEntity.this)
				);
				EvokerEntity.this.getEntityWorld().emitGameEvent(
						GameEvent.ENTITY_PLACE,
						new Vec3d(x, spawnY, z),
						GameEvent.Emitter.of(EvokerEntity.this)
				);
			}
		}

		@Override
		protected SoundEvent getSoundPrepare() {
			return SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK;
		}

		@Override
		protected SpellcastingIllagerEntity.Spell getSpell() {
			return SpellcastingIllagerEntity.Spell.FANGS;
		}
	}

	class LookAtTargetOrWololoTarget extends SpellcastingIllagerEntity.LookAtTargetGoal {

		@Override
		public void tick() {
			if (EvokerEntity.this.getTarget() != null) {
				EvokerEntity.this.getLookControl()
				                 .lookAt(
						                 EvokerEntity.this.getTarget(),
						                 EvokerEntity.this.getMaxHeadRotation(),
						                 EvokerEntity.this.getMaxLookPitchChange()
				                 );
			}
			else if (EvokerEntity.this.getWololoTarget() != null) {
				EvokerEntity.this.getLookControl()
				                 .lookAt(
						                 EvokerEntity.this.getWololoTarget(),
						                 EvokerEntity.this.getMaxHeadRotation(),
						                 EvokerEntity.this.getMaxLookPitchChange()
				                 );
			}
		}
	}

	class SummonVexGoal extends SpellcastingIllagerEntity.CastSpellGoal {

		private final TargetPredicate closeVexPredicate = TargetPredicate.createNonAttackable()
		                                                                 .setBaseMaxDistance(16.0)
		                                                                 .ignoreVisibility()
		                                                                 .ignoreDistanceScalingFactor();

		@Override
		public boolean canStart() {
			if (!super.canStart()) {
				return false;
			}

			int nearbyVexCount = castToServerWorld(EvokerEntity.this.getEntityWorld())
					.getTargets(
							VexEntity.class,
							closeVexPredicate,
							EvokerEntity.this,
							EvokerEntity.this.getBoundingBox().expand(16.0)
					)
					.size();
			return EvokerEntity.this.random.nextInt(8) + 1 > nearbyVexCount;
		}

		@Override
		protected int getSpellTicks() {
			return 100;
		}

		@Override
		protected int startTimeDelay() {
			return 340;
		}

		@Override
		protected void castSpell() {
			ServerWorld serverWorld = (ServerWorld) EvokerEntity.this.getEntityWorld();
			Team team = EvokerEntity.this.getScoreboardTeam();

			for (int vexIndex = 0; vexIndex < 3; vexIndex++) {
				BlockPos
						blockPos =
						EvokerEntity.this
								.getBlockPos()
								.add(
										-2 + EvokerEntity.this.random.nextInt(5),
										1,
										-2 + EvokerEntity.this.random.nextInt(5)
								);
				VexEntity
						vexEntity =
						EntityType.VEX.create(EvokerEntity.this.getEntityWorld(), SpawnReason.MOB_SUMMONED);
				if (vexEntity != null) {
					vexEntity.refreshPositionAndAngles(blockPos, 0.0F, 0.0F);
					vexEntity.initialize(
							serverWorld,
							serverWorld.getLocalDifficulty(blockPos),
							SpawnReason.MOB_SUMMONED,
							null
					);
					vexEntity.setOwner(EvokerEntity.this);
					vexEntity.setBounds(blockPos);
					vexEntity.setLifeTicks(20 * (30 + EvokerEntity.this.random.nextInt(90)));
					if (team != null) {
						serverWorld.getScoreboard().addScoreHolderToTeam(vexEntity.getNameForScoreboard(), team);
					}

					serverWorld.spawnEntityAndPassengers(vexEntity);
					serverWorld.emitGameEvent(
							GameEvent.ENTITY_PLACE,
							blockPos,
							GameEvent.Emitter.of(EvokerEntity.this)
					);
				}
			}
		}

		@Override
		protected SoundEvent getSoundPrepare() {
			return SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON;
		}

		@Override
		protected SpellcastingIllagerEntity.Spell getSpell() {
			return SpellcastingIllagerEntity.Spell.SUMMON_VEX;
		}
	}

	public class WololoGoal extends SpellcastingIllagerEntity.CastSpellGoal {

		private final TargetPredicate convertibleSheepPredicate = TargetPredicate.createNonAttackable()
		                                                                         .setBaseMaxDistance(16.0)
		                                                                         .setPredicate((sheep, world) ->
				                                                                         ((SheepEntity) sheep).getColor()
						                                                                         == DyeColor.BLUE);

		@Override
		public boolean canStart() {
			if (EvokerEntity.this.getTarget() != null) {
				return false;
			}

			if (EvokerEntity.this.isSpellcasting()) {
				return false;
			}

			if (EvokerEntity.this.age < startTime) {
				return false;
			}

			ServerWorld serverWorld = castToServerWorld(EvokerEntity.this.getEntityWorld());
			if (!serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
				return false;
			}

			List<SheepEntity> blueSheep = serverWorld.getTargets(
					SheepEntity.class,
					convertibleSheepPredicate,
					EvokerEntity.this,
					EvokerEntity.this.getBoundingBox().expand(16.0, 4.0, 16.0)
			);
			if (blueSheep.isEmpty()) {
				return false;
			}

			EvokerEntity.this.setWololoTarget(blueSheep.get(EvokerEntity.this.random.nextInt(blueSheep.size())));
			return true;
		}

		@Override
		public boolean shouldContinue() {
			return EvokerEntity.this.getWololoTarget() != null && this.spellCooldown > 0;
		}

		@Override
		public void stop() {
			super.stop();
			EvokerEntity.this.setWololoTarget(null);
		}

		@Override
		protected void castSpell() {
			SheepEntity sheepEntity = EvokerEntity.this.getWololoTarget();
			if (sheepEntity != null && sheepEntity.isAlive()) {
				sheepEntity.setColor(DyeColor.RED);
			}
		}

		@Override
		protected int getInitialCooldown() {
			return 40;
		}

		@Override
		protected int getSpellTicks() {
			return 60;
		}

		@Override
		protected int startTimeDelay() {
			return 140;
		}

		@Override
		protected SoundEvent getSoundPrepare() {
			return SoundEvents.ENTITY_EVOKER_PREPARE_WOLOLO;
		}

		@Override
		protected SpellcastingIllagerEntity.Spell getSpell() {
			return SpellcastingIllagerEntity.Spell.WOLOLO;
		}
	}
}
