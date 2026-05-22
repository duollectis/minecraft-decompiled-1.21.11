package net.minecraft.world.border;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.List;

/**
 * Граница мира — управляет размером, положением и параметрами урона игровой зоны.
 * <p>
 * Поддерживает два режима: статичный ({@link StaticArea}) и движущийся ({@link MovingArea}).
 * Состояние сохраняется через {@link PersistentState} и восстанавливается при загрузке мира.
 * Все изменения параметров рассылаются зарегистрированным {@link WorldBorderListener}.
 */
public class WorldBorder extends PersistentState {

	/** Размер границы по умолчанию — максимально допустимый статичный радиус. */
	public static final double STATIC_AREA_SIZE = 5.999997E7;

	/** Максимально допустимое смещение центра границы от нуля по осям X и Z. */
	public static final double MAX_CENTER_COORDINATES = 2.9999984E7;

	/** Максимальный радиус границы по умолчанию в блоках. */
	private static final int DEFAULT_MAX_RADIUS = 29999984;

	/** Смещение от края границы при проверке коллизий AABB, чтобы избежать граничных артефактов. */
	private static final double BOUNDS_EPSILON = 1.0E-5;

	/** Значение tickProgress для получения текущего (не интерполированного) положения границы. */
	private static final float NO_TICK_PROGRESS = 0.0f;

	public static final Codec<WorldBorder> CODEC =
		WorldBorder.Properties.CODEC.xmap(WorldBorder::new, WorldBorder.Properties::new);

	public static final PersistentStateType<WorldBorder> TYPE = new PersistentStateType<>(
		"world_border", WorldBorder::new, CODEC, DataFixTypes.SAVED_DATA_WORLD_BORDER
	);

	private final WorldBorder.Properties properties;
	private boolean initialized;
	private final List<WorldBorderListener> listeners = new ArrayList<>();

	private double damagePerBlock = 0.2;
	private double safeZone = 5.0;
	private int warningTime = 15;
	private int warningBlocks = 5;
	private double centerX;
	private double centerZ;
	private int maxRadius = DEFAULT_MAX_RADIUS;
	private WorldBorder.Area area = new WorldBorder.StaticArea(STATIC_AREA_SIZE);

	public WorldBorder() {
		this(WorldBorder.Properties.DEFAULT);
	}

	public WorldBorder(WorldBorder.Properties properties) {
		this.properties = properties;
	}

	public boolean contains(BlockPos pos) {
		return contains(pos.getX(), pos.getZ());
	}

	public boolean contains(Vec3d pos) {
		return contains(pos.x, pos.z);
	}

	/**
	 * Проверяет, полностью ли чанк находится внутри границы мира.
	 * Проверяются обе угловые точки чанка — северо-западная и юго-восточная.
	 *
	 * @param chunkPos позиция чанка
	 * @return {@code true} если весь чанк внутри границы
	 */
	public boolean contains(ChunkPos chunkPos) {
		return contains(chunkPos.getStartX(), chunkPos.getStartZ())
			&& contains(chunkPos.getEndX(), chunkPos.getEndZ());
	}

	/**
	 * Проверяет, находится ли AABB-бокс внутри границы мира.
	 * Максимальные координаты уменьшаются на {@link #BOUNDS_EPSILON} во избежание граничных артефактов.
	 *
	 * @param box проверяемый ограничивающий прямоугольник
	 * @return {@code true} если бокс полностью внутри границы
	 */
	public boolean contains(Box box) {
		return contains(box.minX, box.minZ, box.maxX - BOUNDS_EPSILON, box.maxZ - BOUNDS_EPSILON);
	}

	private boolean contains(double minX, double minZ, double maxX, double maxZ) {
		return contains(minX, minZ) && contains(maxX, maxZ);
	}

	public boolean contains(double x, double z) {
		return contains(x, z, 0.0);
	}

	/**
	 * Проверяет, находится ли точка (x, z) внутри границы с дополнительным отступом.
	 *
	 * @param x координата X
	 * @param z координата Z
	 * @param margin дополнительный отступ, расширяющий проверяемую область
	 * @return {@code true} если точка внутри расширенной границы
	 */
	public boolean contains(double x, double z, double margin) {
		return x >= getBoundWest() - margin
			&& x < getBoundEast() + margin
			&& z >= getBoundNorth() - margin
			&& z < getBoundSouth() + margin;
	}

	public BlockPos clampFloored(BlockPos pos) {
		return clampFloored(pos.getX(), pos.getY(), pos.getZ());
	}

	public BlockPos clampFloored(Vec3d pos) {
		return clampFloored(pos.getX(), pos.getY(), pos.getZ());
	}

	public BlockPos clampFloored(double x, double y, double z) {
		return BlockPos.ofFloored(clamp(x, y, z));
	}

	public Vec3d clamp(Vec3d pos) {
		return clamp(pos.x, pos.y, pos.z);
	}

	/**
	 * Зажимает координаты (x, z) внутри текущих границ мира, оставляя Y без изменений.
	 * Максимальные координаты уменьшаются на {@link #BOUNDS_EPSILON}.
	 *
	 * @param x координата X
	 * @param y координата Y (не изменяется)
	 * @param z координата Z
	 * @return точка, гарантированно находящаяся внутри границы
	 */
	public Vec3d clamp(double x, double y, double z) {
		return new Vec3d(
			MathHelper.clamp(x, getBoundWest(), getBoundEast() - BOUNDS_EPSILON),
			y,
			MathHelper.clamp(z, getBoundNorth(), getBoundSouth() - BOUNDS_EPSILON)
		);
	}

	/**
	 * Возвращает расстояние от сущности до ближайшей стены границы.
	 * Отрицательное значение означает, что сущность находится за пределами границы.
	 *
	 * @param entity проверяемая сущность
	 * @return расстояние в блоках; отрицательное — если сущность снаружи
	 */
	public double getDistanceInsideBorder(Entity entity) {
		return getDistanceInsideBorder(entity.getX(), entity.getZ());
	}

	/**
	 * Возвращает расстояние от точки (x, z) до ближайшей стены границы.
	 * Вычисляется как минимум из четырёх расстояний до каждой из сторон.
	 *
	 * @param x координата X
	 * @param z координата Z
	 * @return расстояние в блоках; отрицательное — если точка снаружи
	 */
	public double getDistanceInsideBorder(double x, double z) {
		double distNorth = z - getBoundNorth();
		double distSouth = getBoundSouth() - z;
		double distWest = x - getBoundWest();
		double distEast = getBoundEast() - x;

		double minDist = Math.min(distWest, distEast);
		minDist = Math.min(minDist, distNorth);
		return Math.min(minDist, distSouth);
	}

	/**
	 * Проверяет, может ли AABB-бокс сущности столкнуться со стеной границы.
	 * Коллизия возможна, если сущность достаточно близко к стене и её бокс пересекает зону границы.
	 *
	 * @param entity проверяемая сущность
	 * @param box AABB-бокс сущности
	 * @return {@code true} если коллизия с границей возможна
	 */
	public boolean canCollide(Entity entity, Box box) {
		double maxSide = Math.max(MathHelper.absMax(box.getLengthX(), box.getLengthZ()), 1.0);
		return getDistanceInsideBorder(entity) < maxSide * 2.0
			&& contains(entity.getX(), entity.getZ(), maxSide);
	}

	public WorldBorderStage getStage() {
		return area.getStage();
	}

	public double getBoundWest() {
		return getBoundWest(NO_TICK_PROGRESS);
	}

	public double getBoundWest(float tickProgress) {
		return area.getBoundWest(tickProgress);
	}

	public double getBoundNorth() {
		return getBoundNorth(NO_TICK_PROGRESS);
	}

	public double getBoundNorth(float tickProgress) {
		return area.getBoundNorth(tickProgress);
	}

	public double getBoundEast() {
		return getBoundEast(NO_TICK_PROGRESS);
	}

	public double getBoundEast(float tickProgress) {
		return area.getBoundEast(tickProgress);
	}

	public double getBoundSouth() {
		return getBoundSouth(NO_TICK_PROGRESS);
	}

	public double getBoundSouth(float tickProgress) {
		return area.getBoundSouth(tickProgress);
	}

	public double getCenterX() {
		return centerX;
	}

	public double getCenterZ() {
		return centerZ;
	}

	public void setCenter(double x, double z) {
		centerX = x;
		centerZ = z;
		area.onCenterChanged();
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onCenterChanged(this, x, z);
		}
	}

	public double getSize() {
		return area.getSize();
	}

	public long getSizeLerpTime() {
		return area.getSizeLerpTime();
	}

	public double getSizeLerpTarget() {
		return area.getSizeLerpTarget();
	}

	public void setSize(double size) {
		area = new WorldBorder.StaticArea(size);
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onSizeChange(this, size);
		}
	}

	/**
	 * Запускает плавное изменение размера границы от {@code fromSize} до {@code toSize}
	 * за указанное количество тиков, начиная с момента {@code timeStart}.
	 * Если размеры совпадают — граница немедленно переходит в статичный режим.
	 *
	 * @param fromSize начальный размер границы
	 * @param toSize целевой размер границы
	 * @param timeDuration длительность перехода в тиках
	 * @param timeStart игровое время начала перехода в миллисекундах
	 */
	public void interpolateSize(double fromSize, double toSize, long timeDuration, long timeStart) {
		area = fromSize == toSize
			? new WorldBorder.StaticArea(toSize)
			: new WorldBorder.MovingArea(fromSize, toSize, timeDuration, timeStart);
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onInterpolateSize(this, fromSize, toSize, timeDuration, timeStart);
		}
	}

	protected List<WorldBorderListener> getListeners() {
		return new ArrayList<>(listeners);
	}

	public void addListener(WorldBorderListener listener) {
		listeners.add(listener);
	}

	public void removeListener(WorldBorderListener listener) {
		listeners.remove(listener);
	}

	public void setMaxRadius(int maxRadius) {
		this.maxRadius = maxRadius;
		area.onMaxRadiusChanged();
	}

	public int getMaxRadius() {
		return maxRadius;
	}

	public double getSafeZone() {
		return safeZone;
	}

	public void setSafeZone(double safeZone) {
		this.safeZone = safeZone;
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onSafeZoneChanged(this, safeZone);
		}
	}

	public double getDamagePerBlock() {
		return damagePerBlock;
	}

	public void setDamagePerBlock(double damagePerBlock) {
		this.damagePerBlock = damagePerBlock;
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onDamagePerBlockChanged(this, damagePerBlock);
		}
	}

	public double getShrinkingSpeed() {
		return area.getShrinkingSpeed();
	}

	public int getWarningTime() {
		return warningTime;
	}

	public void setWarningTime(int warningTime) {
		this.warningTime = warningTime;
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onWarningTimeChanged(this, warningTime);
		}
	}

	public int getWarningBlocks() {
		return warningBlocks;
	}

	public void setWarningBlocks(int warningBlocks) {
		this.warningBlocks = warningBlocks;
		markDirty();

		for (WorldBorderListener listener : getListeners()) {
			listener.onWarningBlocksChanged(this, warningBlocks);
		}
	}

	/**
	 * Обновляет состояние границы на один тик.
	 * Для движущейся границы уменьшает оставшееся время и пересчитывает текущий размер.
	 * При истечении времени автоматически переключается в статичный режим.
	 */
	public void tick() {
		area = area.getAreaInstance();
	}

	/**
	 * Инициализирует границу из сохранённых {@link Properties}, если она ещё не была инициализирована.
	 * Вызывается при загрузке мира с передачей текущего игрового времени для корректного
	 * восстановления движущейся границы с учётом прошедшего времени.
	 *
	 * @param time текущее игровое время в миллисекундах
	 */
	public void ensureInitialized(long time) {
		if (initialized) {
			return;
		}

		setCenter(properties.centerX(), properties.centerZ());
		setDamagePerBlock(properties.damagePerBlock());
		setSafeZone(properties.safeZone());
		setWarningBlocks(properties.warningBlocks());
		setWarningTime(properties.warningTime());

		if (properties.lerpTime() > 0L) {
			interpolateSize(properties.size(), properties.lerpTarget(), properties.lerpTime(), time);
		} else {
			setSize(properties.size());
		}

		initialized = true;
	}

	/**
	 * Возвращает VoxelShape текущей области границы для расчёта коллизий.
	 *
	 * @return форма границы как {@link VoxelShape}
	 */
	public VoxelShape asVoxelShape() {
		return area.asVoxelShape();
	}

	// -------------------------------------------------------------------------
	// Внутренние типы
	// -------------------------------------------------------------------------

	/**
	 * Внутренний контракт области границы мира.
	 * Реализуется двумя вариантами: {@link StaticArea} и {@link MovingArea}.
	 */
	interface Area {

		double getBoundWest(float tickProgress);

		double getBoundEast(float tickProgress);

		double getBoundNorth(float tickProgress);

		double getBoundSouth(float tickProgress);

		double getSize();

		double getShrinkingSpeed();

		long getSizeLerpTime();

		double getSizeLerpTarget();

		WorldBorderStage getStage();

		void onMaxRadiusChanged();

		void onCenterChanged();

		/**
		 * Обновляет состояние области на один тик и возвращает актуальный экземпляр.
		 * Движущаяся область может вернуть {@link StaticArea} при завершении анимации.
		 *
		 * @return актуальный экземпляр области после тика
		 */
		WorldBorder.Area getAreaInstance();

		VoxelShape asVoxelShape();
	}

	/**
	 * Движущаяся область границы — плавно интерполирует размер от {@code oldSize} до {@code newSize}
	 * за заданное количество тиков. При каждом тике пересчитывает текущий размер через линейную
	 * интерполяцию. По истечении времени заменяется на {@link StaticArea}.
	 */
	class MovingArea implements WorldBorder.Area {

		private final double oldSize;
		private final double newSize;
		private final long timeEnd;
		private final long timeStart;
		private final double timeDuration;
		private long remainingTimeDuration;
		private double currentSize;
		private double lastSize;

		MovingArea(double oldSize, double newSize, long timeDuration, long timeStart) {
			this.oldSize = oldSize;
			this.newSize = newSize;
			this.timeDuration = timeDuration;
			this.remainingTimeDuration = timeDuration;
			this.timeStart = timeStart;
			this.timeEnd = timeStart + timeDuration;

			double initialSize = computeCurrentSize();
			currentSize = initialSize;
			lastSize = initialSize;
		}

		@Override
		public double getBoundWest(float tickProgress) {
			return MathHelper.clamp(
				WorldBorder.this.getCenterX() - MathHelper.lerp(tickProgress, lastSize, currentSize) / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getBoundNorth(float tickProgress) {
			return MathHelper.clamp(
				WorldBorder.this.getCenterZ() - MathHelper.lerp(tickProgress, lastSize, currentSize) / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getBoundEast(float tickProgress) {
			return MathHelper.clamp(
				WorldBorder.this.getCenterX() + MathHelper.lerp(tickProgress, lastSize, currentSize) / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getBoundSouth(float tickProgress) {
			return MathHelper.clamp(
				WorldBorder.this.getCenterZ() + MathHelper.lerp(tickProgress, lastSize, currentSize) / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getSize() {
			return currentSize;
		}

		public double getLastSize() {
			return lastSize;
		}

		/**
		 * Вычисляет текущий размер границы через линейную интерполяцию на основе
		 * прошедшей доли от общего времени перехода.
		 *
		 * @return интерполированный размер; {@code newSize} если переход завершён
		 */
		private double computeCurrentSize() {
			double progress = (timeDuration - remainingTimeDuration) / timeDuration;
			return progress < 1.0 ? MathHelper.lerp(progress, oldSize, newSize) : newSize;
		}

		@Override
		public double getShrinkingSpeed() {
			return Math.abs(oldSize - newSize) / (timeEnd - timeStart);
		}

		@Override
		public long getSizeLerpTime() {
			return remainingTimeDuration;
		}

		@Override
		public double getSizeLerpTarget() {
			return newSize;
		}

		@Override
		public WorldBorderStage getStage() {
			return newSize < oldSize ? WorldBorderStage.SHRINKING : WorldBorderStage.GROWING;
		}

		@Override
		public void onCenterChanged() {
		}

		@Override
		public void onMaxRadiusChanged() {
		}

		@Override
		public WorldBorder.Area getAreaInstance() {
			remainingTimeDuration--;
			lastSize = currentSize;
			currentSize = computeCurrentSize();

			if (remainingTimeDuration <= 0L) {
				WorldBorder.this.markDirty();
				return WorldBorder.this.new StaticArea(newSize);
			}

			return this;
		}

		@Override
		public VoxelShape asVoxelShape() {
			return VoxelShapes.combineAndSimplify(
				VoxelShapes.UNBOUNDED,
				VoxelShapes.cuboid(
					Math.floor(getBoundWest(NO_TICK_PROGRESS)),
					Double.NEGATIVE_INFINITY,
					Math.floor(getBoundNorth(NO_TICK_PROGRESS)),
					Math.ceil(getBoundEast(NO_TICK_PROGRESS)),
					Double.POSITIVE_INFINITY,
					Math.ceil(getBoundSouth(NO_TICK_PROGRESS))
				),
				BooleanBiFunction.ONLY_FIRST
			);
		}
	}

	/**
	 * Статичная область границы с фиксированным размером.
	 * Кэширует вычисленные границы и VoxelShape, пересчитывая их только при изменении
	 * центра или максимального радиуса.
	 */
	class StaticArea implements WorldBorder.Area {

		private final double size;
		private double boundWest;
		private double boundNorth;
		private double boundEast;
		private double boundSouth;
		private VoxelShape shape;

		public StaticArea(double size) {
			this.size = size;
			recalculateBounds();
		}

		@Override
		public double getBoundWest(float tickProgress) {
			return boundWest;
		}

		@Override
		public double getBoundEast(float tickProgress) {
			return boundEast;
		}

		@Override
		public double getBoundNorth(float tickProgress) {
			return boundNorth;
		}

		@Override
		public double getBoundSouth(float tickProgress) {
			return boundSouth;
		}

		@Override
		public double getSize() {
			return size;
		}

		@Override
		public WorldBorderStage getStage() {
			return WorldBorderStage.STATIONARY;
		}

		@Override
		public double getShrinkingSpeed() {
			return 0.0;
		}

		@Override
		public long getSizeLerpTime() {
			return 0L;
		}

		@Override
		public double getSizeLerpTarget() {
			return size;
		}

		private void recalculateBounds() {
			boundWest = MathHelper.clamp(
				WorldBorder.this.getCenterX() - size / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
			boundNorth = MathHelper.clamp(
				WorldBorder.this.getCenterZ() - size / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
			boundEast = MathHelper.clamp(
				WorldBorder.this.getCenterX() + size / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
			boundSouth = MathHelper.clamp(
				WorldBorder.this.getCenterZ() + size / 2.0,
				-WorldBorder.this.maxRadius,
				WorldBorder.this.maxRadius
			);
			shape = VoxelShapes.combineAndSimplify(
				VoxelShapes.UNBOUNDED,
				VoxelShapes.cuboid(
					Math.floor(getBoundWest(NO_TICK_PROGRESS)),
					Double.NEGATIVE_INFINITY,
					Math.floor(getBoundNorth(NO_TICK_PROGRESS)),
					Math.ceil(getBoundEast(NO_TICK_PROGRESS)),
					Double.POSITIVE_INFINITY,
					Math.ceil(getBoundSouth(NO_TICK_PROGRESS))
				),
				BooleanBiFunction.ONLY_FIRST
			);
		}

		@Override
		public void onMaxRadiusChanged() {
			recalculateBounds();
		}

		@Override
		public void onCenterChanged() {
			recalculateBounds();
		}

		@Override
		public WorldBorder.Area getAreaInstance() {
			return this;
		}

		@Override
		public VoxelShape asVoxelShape() {
			return shape;
		}
	}

	/**
	 * Снимок параметров границы мира, используемый для сериализации и десериализации
	 * через {@link #CODEC}. Передаётся в конструктор {@link WorldBorder} при загрузке мира.
	 */
	public record Properties(
		double centerX,
		double centerZ,
		double damagePerBlock,
		double safeZone,
		int warningBlocks,
		int warningTime,
		double size,
		long lerpTime,
		double lerpTarget
	) {

		/** Параметры границы по умолчанию для нового мира. */
		public static final WorldBorder.Properties DEFAULT =
			new WorldBorder.Properties(0.0, 0.0, 0.2, 5.0, 5, 300, STATIC_AREA_SIZE, 0L, 0.0);

		public static final Codec<WorldBorder.Properties> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.doubleRange(-MAX_CENTER_COORDINATES, MAX_CENTER_COORDINATES)
					.fieldOf("center_x")
					.forGetter(WorldBorder.Properties::centerX),
				Codec.doubleRange(-MAX_CENTER_COORDINATES, MAX_CENTER_COORDINATES)
					.fieldOf("center_z")
					.forGetter(WorldBorder.Properties::centerZ),
				Codec.DOUBLE.fieldOf("damage_per_block").forGetter(WorldBorder.Properties::damagePerBlock),
				Codec.DOUBLE.fieldOf("safe_zone").forGetter(WorldBorder.Properties::safeZone),
				Codec.INT.fieldOf("warning_blocks").forGetter(WorldBorder.Properties::warningBlocks),
				Codec.INT.fieldOf("warning_time").forGetter(WorldBorder.Properties::warningTime),
				Codec.DOUBLE.fieldOf("size").forGetter(WorldBorder.Properties::size),
				Codec.LONG.fieldOf("lerp_time").forGetter(WorldBorder.Properties::lerpTime),
				Codec.DOUBLE.fieldOf("lerp_target").forGetter(WorldBorder.Properties::lerpTarget)
			).apply(instance, WorldBorder.Properties::new)
		);

		public Properties(WorldBorder worldBorder) {
			this(
				worldBorder.centerX,
				worldBorder.centerZ,
				worldBorder.damagePerBlock,
				worldBorder.safeZone,
				worldBorder.warningBlocks,
				worldBorder.warningTime,
				worldBorder.area.getSize(),
				worldBorder.area.getSizeLerpTime(),
				worldBorder.area.getSizeLerpTarget()
			);
		}
	}
}
