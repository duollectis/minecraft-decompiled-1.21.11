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
 * Базовый класс для всех пассивных существ (животных, жителей и т.д.).
 * Управляет системой возраста: детёныши имеют отрицательный {@code breedingAge},
 * взрослые — положительный (кулдаун размножения) или нулевой (готовы к размножению).
 */
public abstract class PassiveEntity extends PathAwareEntity {

	private static final TrackedData<Boolean> CHILD = DataTracker.registerData(
		PassiveEntity.class,
		TrackedDataHandlerRegistry.BOOLEAN
	);

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
			entityData = new PassiveData(true);
		}

		PassiveData passiveData = (PassiveData) entityData;
		if (passiveData.canSpawnBaby()
			&& passiveData.getSpawnedCount() > 0
			&& world.getRandom().nextFloat() <= passiveData.getBabyChance()
		) {
			setBreedingAge(BABY_AGE);
		}

		passiveData.countSpawned();
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * Создаёт детёныша при размножении двух особей.
	 *
	 * @param world  серверный мир, в котором происходит размножение
	 * @param entity второй родитель
	 * @return новый детёныш, или {@code null} если создание невозможно
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

	/**
	 * Возвращает текущий возраст существа.
	 * На клиенте возвращает упрощённое значение: {@code -1} для детёнышей, {@code 1} для взрослых.
	 * На сервере возвращает точное значение {@code breedingAge}.
	 */
	public int getBreedingAge() {
		return getEntityWorld().isClient()
			? (dataTracker.get(CHILD) ? -1 : 1)
			: breedingAge;
	}

	/**
	 * Ускоряет взросление существа на заданное количество тиков.
	 * Если {@code overGrow = true}, избыток роста накапливается в {@code forcedAge}
	 * и запускает анимацию счастья.
	 *
	 * @param age      количество секунд роста (умножается на 20 тиков)
	 * @param overGrow накапливать ли избыток в {@code forcedAge}
	 */
	public void growUp(int age, boolean overGrow) {
		int currentAge = getBreedingAge();
		int targetAge = Math.min(currentAge + age * 20, 0);
		int ageDelta = targetAge - currentAge;

		setBreedingAge(targetAge);

		if (overGrow) {
			forcedAge += ageDelta;
			if (happyTicksRemaining == 0) {
				happyTicksRemaining = HAPPY_TICKS;
			}
		}

		if (getBreedingAge() == 0) {
			setBreedingAge(forcedAge);
		}
	}

	public void growUp(int age) {
		growUp(age, false);
	}

	/**
	 * Устанавливает возраст существа и синхронизирует флаг {@code CHILD}.
	 * При пересечении нулевой границы вызывает {@link #onGrowUp()}.
	 */
	public void setBreedingAge(int age) {
		int previousAge = getBreedingAge();
		breedingAge = age;
		if (previousAge < 0 && age >= 0 || previousAge >= 0 && age < 0) {
			dataTracker.set(CHILD, age < 0);
			onGrowUp();
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Age", getBreedingAge());
		view.putInt("ForcedAge", forcedAge);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setBreedingAge(view.getInt("Age", 0));
		forcedAge = view.getInt("ForcedAge", 0);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (CHILD.equals(data)) {
			calculateDimensions();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (getEntityWorld().isClient()) {
			if (happyTicksRemaining > 0) {
				if (happyTicksRemaining % 4 == 0) {
					getEntityWorld().addParticleClient(
						ParticleTypes.HAPPY_VILLAGER,
						getParticleX(1.0),
						getRandomBodyY() + 0.5,
						getParticleZ(1.0),
						0.0,
						0.0,
						0.0
					);
				}

				happyTicksRemaining--;
			}
		} else if (isAlive()) {
			int currentAge = getBreedingAge();
			if (currentAge < 0) {
				setBreedingAge(currentAge + 1);
			} else if (currentAge > 0) {
				setBreedingAge(currentAge - 1);
			}
		}
	}

	/**
	 * Вызывается при пересечении нулевой границы возраста (взросление или превращение в детёныша).
	 * По умолчанию высаживает повзрослевшее существо из лодки, если оно туда не помещается.
	 */
	protected void onGrowUp() {
		if (isBaby() || !hasVehicle()) {
			return;
		}

		if (getVehicle() instanceof AbstractBoatEntity boat && !boat.isSmallerThanBoat(this)) {
			stopRiding();
		}
	}

	@Override
	public boolean isBaby() {
		return getBreedingAge() < 0;
	}

	@Override
	public void setBaby(boolean baby) {
		setBreedingAge(baby ? BABY_AGE : 0);
	}

	/**
	 * Конвертирует значение {@code breedingAge} в количество секунд роста для {@link #growUp}.
	 * Используется при кормлении детёнышей для ускорения взросления.
	 *
	 * @param breedingAge текущий возраст детёныша (отрицательное число)
	 * @return количество секунд, на которое нужно ускорить рост
	 */
	public static int toGrowUpAge(int breedingAge) {
		return (int) (breedingAge / 20 * 0.1F);
	}

	@VisibleForTesting
	public int getForcedAge() {
		return forcedAge;
	}

	@VisibleForTesting
	public int getHappyTicksRemaining() {
		return happyTicksRemaining;
	}

	/**
	 * Данные спавна для пассивных существ.
	 * Хранит счётчик заспавненных особей и вероятность появления детёныша.
	 */
	public static class PassiveData implements EntityData {

		private static final float DEFAULT_BABY_CHANCE = 0.05F;

		private int spawnCount;
		private final boolean babyAllowed;
		private final float babyChance;

		public PassiveData(boolean babyAllowed, float babyChance) {
			this.babyAllowed = babyAllowed;
			this.babyChance = babyChance;
		}

		public PassiveData(boolean babyAllowed) {
			this(babyAllowed, DEFAULT_BABY_CHANCE);
		}

		public PassiveData(float babyChance) {
			this(true, babyChance);
		}

		public int getSpawnedCount() {
			return spawnCount;
		}

		public void countSpawned() {
			spawnCount++;
		}

		public boolean canSpawnBaby() {
			return babyAllowed;
		}

		public float getBabyChance() {
			return babyChance;
		}
	}
}
