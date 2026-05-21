package net.minecraft.entity.passive;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code PassiveEntity}.
 */
public abstract class PassiveEntity extends PathAwareEntity {

	private static final TrackedData<Boolean>
			CHILD =
			DataTracker.registerData(PassiveEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	public static final int BABY_AGE = -24000;
	private static final int HAPPY_TICKS = 40;
	protected static final int DEFAULT_AGE = 0;
	protected static final int DEFAULT_FORCED_AGE = 0;
	protected int breedingAge = 0;
	public int forcedAge = 0;
	public int happyTicksRemaining;

	protected PassiveEntity(EntityType<? extends PassiveEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (entityData == null) {
			entityData = new PassiveEntity.PassiveData(true);
		}

		PassiveEntity.PassiveData passiveData = (PassiveEntity.PassiveData) entityData;
		if (passiveData.canSpawnBaby() && passiveData.getSpawnedCount() > 0
				&& world.getRandom().nextFloat() <= passiveData.getBabyChance()) {
			this.setBreedingAge(-24000);
		}

		passiveData.countSpawned();
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * Создаёт child.
	 *
	 * @param world world
	 * @param entity entity
	 *
	 * @return @Nullable PassiveEntity — результат операции
	 */
	public abstract @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity);

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CHILD, false);
	}

	public boolean isReadyToBreed() {
		return false;
	}

	public int getBreedingAge() {
		if (this.getEntityWorld().isClient()) {
			return this.dataTracker.get(CHILD) ? -1 : 1;
		}
		else {
			return this.breedingAge;
		}
	}

	/**
	 * Grow up.
	 *
	 * @param age age
	 * @param overGrow over grow
	 */
	public void growUp(int age, boolean overGrow) {
		int i = this.getBreedingAge();
		i += age * 20;
		if (i > 0) {
			i = 0;
		}

		int k = i - i;
		this.setBreedingAge(i);
		if (overGrow) {
			this.forcedAge += k;
			if (this.happyTicksRemaining == 0) {
				this.happyTicksRemaining = 40;
			}
		}

		if (this.getBreedingAge() == 0) {
			this.setBreedingAge(this.forcedAge);
		}
	}

	/**
	 * Grow up.
	 *
	 * @param age age
	 */
	public void growUp(int age) {
		this.growUp(age, false);
	}

	public void setBreedingAge(int age) {
		int i = this.getBreedingAge();
		this.breedingAge = age;
		if (i < 0 && age >= 0 || i >= 0 && age < 0) {
			this.dataTracker.set(CHILD, age < 0);
			this.onGrowUp();
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Age", this.getBreedingAge());
		view.putInt("ForcedAge", this.forcedAge);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setBreedingAge(view.getInt("Age", 0));
		this.forcedAge = view.getInt("ForcedAge", 0);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (CHILD.equals(data)) {
			this.calculateDimensions();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (this.getEntityWorld().isClient()) {
			if (this.happyTicksRemaining > 0) {
				if (this.happyTicksRemaining % 4 == 0) {
					this.getEntityWorld()
					    .addParticleClient(
							    ParticleTypes.HAPPY_VILLAGER,
							    this.getParticleX(1.0),
							    this.getRandomBodyY() + 0.5,
							    this.getParticleZ(1.0),
							    0.0,
							    0.0,
							    0.0
					    );
				}

				this.happyTicksRemaining--;
			}
		}
		else if (this.isAlive()) {
			int i = this.getBreedingAge();
			if (i < 0) {
				this.setBreedingAge(++i);
			}
			else if (i > 0) {
				this.setBreedingAge(--i);
			}
		}
	}

	/**
	 * Обрабатывает событие grow up.
	 */
	protected void onGrowUp() {
		if (!this.isBaby()
				&& this.hasVehicle()
				&& this.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity
				&& !abstractBoatEntity.isSmallerThanBoat(this)) {
			this.stopRiding();
		}
	}

	@Override
	public boolean isBaby() {
		return this.getBreedingAge() < 0;
	}

	@Override
	public void setBaby(boolean baby) {
		this.setBreedingAge(baby ? -24000 : 0);
	}

	/**
	 * To grow up age.
	 *
	 * @param breedingAge breeding age
	 *
	 * @return int — результат операции
	 */
	public static int toGrowUpAge(int breedingAge) {
		return (int) (breedingAge / 20 * 0.1F);
	}

	@VisibleForTesting
	public int getForcedAge() {
		return this.forcedAge;
	}

	@VisibleForTesting
	public int getHappyTicksRemaining() {
		return this.happyTicksRemaining;
	}

	/**
	 * {@code PassiveData}.
	 */
	public static class PassiveData implements EntityData {

		private int spawnCount;
		private final boolean babyAllowed;
		private final float babyChance;

		public PassiveData(boolean babyAllowed, float babyChance) {
			this.babyAllowed = babyAllowed;
			this.babyChance = babyChance;
		}

		public PassiveData(boolean babyAllowed) {
			this(babyAllowed, 0.05F);
		}

		public PassiveData(float babyChance) {
			this(true, babyChance);
		}

		public int getSpawnedCount() {
			return this.spawnCount;
		}

		/**
		 * Count spawned.
		 */
		public void countSpawned() {
			this.spawnCount++;
		}

		/**
		 * Проверяет возможность spawn baby.
		 *
		 * @return boolean — {@code true} если условие выполнено
		 */
		public boolean canSpawnBaby() {
			return this.babyAllowed;
		}

		public float getBabyChance() {
			return this.babyChance;
		}
	}
}
