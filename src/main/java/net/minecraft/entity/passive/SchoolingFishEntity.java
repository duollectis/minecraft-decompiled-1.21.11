package net.minecraft.entity.passive;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FollowGroupLeaderGoal;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Базовый класс для стайных рыб (треска, лосось, тропические рыбы).
 * Реализует логику лидерства в стае: одна рыба становится лидером,
 * остальные следуют за ней через {@link net.minecraft.entity.ai.goal.FollowGroupLeaderGoal}.
 */
public abstract class SchoolingFishEntity extends FishEntity {

	private @Nullable SchoolingFishEntity leader;
	private int groupSize = 1;

	public SchoolingFishEntity(EntityType<? extends SchoolingFishEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		this.goalSelector.add(5, new FollowGroupLeaderGoal(this));
	}

	@Override
	public int getLimitPerChunk() {
		return this.getMaxGroupSize();
	}

	public int getMaxGroupSize() {
		return super.getLimitPerChunk();
	}

	@Override
	protected boolean hasSelfControl() {
		return !this.hasLeader();
	}

	public boolean hasLeader() {
		return this.leader != null && this.leader.isAlive();
	}

	/**
	 * Join group of.
	 *
	 * @param groupLeader group leader
	 *
	 * @return SchoolingFishEntity — результат операции
	 */
	public SchoolingFishEntity joinGroupOf(SchoolingFishEntity groupLeader) {
		this.leader = groupLeader;
		groupLeader.increaseGroupSize();
		return groupLeader;
	}

	/**
	 * Leave group.
	 */
	public void leaveGroup() {
		this.leader.decreaseGroupSize();
		this.leader = null;
	}

	private void increaseGroupSize() {
		this.groupSize++;
	}

	private void decreaseGroupSize() {
		this.groupSize--;
	}

	/**
	 * Проверяет возможность have more fish in group.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canHaveMoreFishInGroup() {
		return this.hasOtherFishInGroup() && this.groupSize < this.getMaxGroupSize();
	}

	@Override
	public void tick() {
		super.tick();
		if (this.hasOtherFishInGroup() && this.getEntityWorld().random.nextInt(200) == 1) {
			List<? extends FishEntity> list = this.getEntityWorld()
			                                      .getNonSpectatingEntities(
					                                      (Class<? extends FishEntity>) this.getClass(),
					                                      this.getBoundingBox().expand(8.0, 8.0, 8.0)
			                                      );
			if (list.size() <= 1) {
				this.groupSize = 1;
			}
		}
	}

	public boolean hasOtherFishInGroup() {
		return this.groupSize > 1;
	}

	public boolean isCloseEnoughToLeader() {
		return this.squaredDistanceTo(this.leader) <= 121.0;
	}

	/**
	 * Перемещает toward leader.
	 */
	public void moveTowardLeader() {
		if (this.hasLeader()) {
			this.getNavigation().startMovingTo(this.leader, 1.0);
		}
	}

	/**
	 * Pull in other fish.
	 *
	 * @param fish fish
	 */
	public void pullInOtherFish(Stream<? extends SchoolingFishEntity> fish) {
		fish
				.limit(this.getMaxGroupSize() - this.groupSize)
				.filter(fishx -> fishx != this)
				.forEach(fishx -> fishx.joinGroupOf(this));
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		super.initialize(world, difficulty, spawnReason, entityData);
		if (entityData == null) {
			entityData = new SchoolingFishEntity.FishData(this);
		}
		else {
			this.joinGroupOf(((SchoolingFishEntity.FishData) entityData).leader);
		}

		return entityData;
	}

	/**
 * Данные спавна стайных рыб: хранит лидера стаи.
 */
	public static class FishData implements EntityData {

		public final SchoolingFishEntity leader;

		public FishData(SchoolingFishEntity leader) {
			this.leader = leader;
		}
	}
}
