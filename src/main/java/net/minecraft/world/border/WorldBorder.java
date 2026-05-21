package net.minecraft.world.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.List;

/**
 * {@code WorldBorder}.
 */
public class WorldBorder extends PersistentState {

	public static final double STATIC_AREA_SIZE = 5.999997E7F;
	public static final double MAX_CENTER_COORDINATES = 2.9999984E7;
	public static final Codec<WorldBorder>
			CODEC =
			WorldBorder.Properties.CODEC.xmap(WorldBorder::new, WorldBorder.Properties::new);
	public static final PersistentStateType<WorldBorder> TYPE = new PersistentStateType<>(
			"world_border", WorldBorder::new, CODEC, DataFixTypes.SAVED_DATA_WORLD_BORDER
	);
	private final WorldBorder.Properties properties;
	private boolean initialized;
	private final List<WorldBorderListener> listeners = Lists.newArrayList();
	double damagePerBlock = 0.2;
	double safeZone = 5.0;
	int warningTime = 15;
	int warningBlocks = 5;
	double centerX;
	double centerZ;
	int maxRadius = 29999984;
	WorldBorder.Area area = new WorldBorder.StaticArea(5.999997E7F);

	public WorldBorder() {
		this(WorldBorder.Properties.DEFAULT);
	}

	public WorldBorder(WorldBorder.Properties properties) {
		this.properties = properties;
	}

	/**
	 * Contains.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(BlockPos pos) {
		return this.contains(pos.getX(), pos.getZ());
	}

	/**
	 * Contains.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(Vec3d pos) {
		return this.contains(pos.x, pos.z);
	}

	/**
	 * Contains.
	 *
	 * @param chunkPos chunk pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(ChunkPos chunkPos) {
		return this.contains(chunkPos.getStartX(), chunkPos.getStartZ()) && this.contains(
				chunkPos.getEndX(),
				chunkPos.getEndZ()
		);
	}

	/**
	 * Contains.
	 *
	 * @param box box
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(Box box) {
		return this.contains(box.minX, box.minZ, box.maxX - 1.0E-5F, box.maxZ - 1.0E-5F);
	}

	private boolean contains(double minX, double minZ, double maxX, double maxZ) {
		return this.contains(minX, minZ) && this.contains(maxX, maxZ);
	}

	/**
	 * Contains.
	 *
	 * @param x x
	 * @param z z
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(double x, double z) {
		return this.contains(x, z, 0.0);
	}

	/**
	 * Contains.
	 *
	 * @param x x
	 * @param z z
	 * @param margin margin
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(double x, double z, double margin) {
		return x >= this.getBoundWest() - margin && x < this.getBoundEast() + margin
				&& z >= this.getBoundNorth() - margin && z < this.getBoundSouth() + margin;
	}

	/**
	 * Clamp floored.
	 *
	 * @param pos pos
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos clampFloored(BlockPos pos) {
		return this.clampFloored(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Clamp floored.
	 *
	 * @param pos pos
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos clampFloored(Vec3d pos) {
		return this.clampFloored(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Clamp floored.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return BlockPos — результат операции
	 */
	public BlockPos clampFloored(double x, double y, double z) {
		return BlockPos.ofFloored(this.clamp(x, y, z));
	}

	/**
	 * Clamp.
	 *
	 * @param pos pos
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d clamp(Vec3d pos) {
		return this.clamp(pos.x, pos.y, pos.z);
	}

	/**
	 * Clamp.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d clamp(double x, double y, double z) {
		return new Vec3d(
				MathHelper.clamp(x, this.getBoundWest(), this.getBoundEast() - 1.0E-5F),
				y,
				MathHelper.clamp(z, this.getBoundNorth(), this.getBoundSouth() - 1.0E-5F)
		);
	}

	public double getDistanceInsideBorder(Entity entity) {
		return this.getDistanceInsideBorder(entity.getX(), entity.getZ());
	}

	/**
	 * As voxel shape.
	 *
	 * @return VoxelShape — результат операции
	 */
	public VoxelShape asVoxelShape() {
		return this.area.asVoxelShape();
	}

	public double getDistanceInsideBorder(double x, double z) {
		double d = z - this.getBoundNorth();
		double e = this.getBoundSouth() - z;
		double f = x - this.getBoundWest();
		double g = this.getBoundEast() - x;
		double h = Math.min(f, g);
		h = Math.min(h, d);
		return Math.min(h, e);
	}

	/**
	 * Проверяет возможность collide.
	 *
	 * @param entity entity
	 * @param box box
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canCollide(Entity entity, Box box) {
		double d = Math.max(MathHelper.absMax(box.getLengthX(), box.getLengthZ()), 1.0);
		return this.getDistanceInsideBorder(entity) < d * 2.0 && this.contains(entity.getX(), entity.getZ(), d);
	}

	public WorldBorderStage getStage() {
		return this.area.getStage();
	}

	public double getBoundWest() {
		return this.getBoundWest(0.0F);
	}

	public double getBoundWest(float tickProgress) {
		return this.area.getBoundWest(tickProgress);
	}

	public double getBoundNorth() {
		return this.getBoundNorth(0.0F);
	}

	public double getBoundNorth(float tickProgress) {
		return this.area.getBoundNorth(tickProgress);
	}

	public double getBoundEast() {
		return this.getBoundEast(0.0F);
	}

	public double getBoundEast(float tickProgress) {
		return this.area.getBoundEast(tickProgress);
	}

	public double getBoundSouth() {
		return this.getBoundSouth(0.0F);
	}

	public double getBoundSouth(float tickProgress) {
		return this.area.getBoundSouth(tickProgress);
	}

	public double getCenterX() {
		return this.centerX;
	}

	public double getCenterZ() {
		return this.centerZ;
	}

	public void setCenter(double x, double z) {
		this.centerX = x;
		this.centerZ = z;
		this.area.onCenterChanged();
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onCenterChanged(this, x, z);
		}
	}

	public double getSize() {
		return this.area.getSize();
	}

	public long getSizeLerpTime() {
		return this.area.getSizeLerpTime();
	}

	public double getSizeLerpTarget() {
		return this.area.getSizeLerpTarget();
	}

	public void setSize(double size) {
		this.area = new WorldBorder.StaticArea(size);
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onSizeChange(this, size);
		}
	}

	/**
	 * Interpolate size.
	 *
	 * @param fromSize from size
	 * @param toSize to size
	 * @param timeDuration time duration
	 * @param timeStart time start
	 */
	public void interpolateSize(double fromSize, double toSize, long timeDuration, long timeStart) {
		this.area = (WorldBorder.Area) (fromSize == toSize
		                                ? new WorldBorder.StaticArea(toSize)
		                                : new WorldBorder.MovingArea(fromSize, toSize, timeDuration, timeStart)
		);
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onInterpolateSize(this, fromSize, toSize, timeDuration, timeStart);
		}
	}

	protected List<WorldBorderListener> getListeners() {
		return Lists.newArrayList(this.listeners);
	}

	/**
	 * Добавляет listener.
	 *
	 * @param listener listener
	 */
	public void addListener(WorldBorderListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Удаляет listener.
	 *
	 * @param listener listener
	 */
	public void removeListener(WorldBorderListener listener) {
		this.listeners.remove(listener);
	}

	public void setMaxRadius(int maxRadius) {
		this.maxRadius = maxRadius;
		this.area.onMaxRadiusChanged();
	}

	public int getMaxRadius() {
		return this.maxRadius;
	}

	public double getSafeZone() {
		return this.safeZone;
	}

	public void setSafeZone(double safeZone) {
		this.safeZone = safeZone;
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onSafeZoneChanged(this, safeZone);
		}
	}

	public double getDamagePerBlock() {
		return this.damagePerBlock;
	}

	public void setDamagePerBlock(double damagePerBlock) {
		this.damagePerBlock = damagePerBlock;
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onDamagePerBlockChanged(this, damagePerBlock);
		}
	}

	public double getShrinkingSpeed() {
		return this.area.getShrinkingSpeed();
	}

	public int getWarningTime() {
		return this.warningTime;
	}

	public void setWarningTime(int warningTime) {
		this.warningTime = warningTime;
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onWarningTimeChanged(this, warningTime);
		}
	}

	public int getWarningBlocks() {
		return this.warningBlocks;
	}

	public void setWarningBlocks(int warningBlocks) {
		this.warningBlocks = warningBlocks;
		this.markDirty();

		for (WorldBorderListener worldBorderListener : this.getListeners()) {
			worldBorderListener.onWarningBlocksChanged(this, warningBlocks);
		}
	}

	/**
	 * Tick.
	 */
	public void tick() {
		this.area = this.area.getAreaInstance();
	}

	/**
	 * Ensure initialized.
	 *
	 * @param time time
	 */
	public void ensureInitialized(long time) {
		if (!this.initialized) {
			this.setCenter(this.properties.centerX(), this.properties.centerZ());
			this.setDamagePerBlock(this.properties.damagePerBlock());
			this.setSafeZone(this.properties.safeZone());
			this.setWarningBlocks(this.properties.warningBlocks());
			this.setWarningTime(this.properties.warningTime());
			if (this.properties.lerpTime() > 0L) {
				this.interpolateSize(
						this.properties.size(),
						this.properties.lerpTarget(),
						this.properties.lerpTime(),
						time
				);
			}
			else {
				this.setSize(this.properties.size());
			}

			this.initialized = true;
		}
	}

	/**
	 * {@code Area}.
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

		WorldBorder.Area getAreaInstance();

		VoxelShape asVoxelShape();
	}

	/**
	 * {@code MovingArea}.
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

		MovingArea(final double oldSize, final double newSize, final long timeDuration, final long timeStart) {
			this.oldSize = oldSize;
			this.newSize = newSize;
			this.timeDuration = timeDuration;
			this.remainingTimeDuration = timeDuration;
			this.timeStart = timeStart;
			this.timeEnd = this.timeStart + timeDuration;
			double d = this.currentSize();
			this.currentSize = d;
			this.lastSize = d;
		}

		@Override
		public double getBoundWest(float tickProgress) {
			return MathHelper.clamp(
					WorldBorder.this.getCenterX()
							- MathHelper.lerp((double) tickProgress, this.getLastSize(), this.getSize()) / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getBoundNorth(float tickProgress) {
			return MathHelper.clamp(
					WorldBorder.this.getCenterZ()
							- MathHelper.lerp((double) tickProgress, this.getLastSize(), this.getSize()) / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getBoundEast(float tickProgress) {
			return MathHelper.clamp(
					WorldBorder.this.getCenterX()
							+ MathHelper.lerp((double) tickProgress, this.getLastSize(), this.getSize()) / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getBoundSouth(float tickProgress) {
			return MathHelper.clamp(
					WorldBorder.this.getCenterZ()
							+ MathHelper.lerp((double) tickProgress, this.getLastSize(), this.getSize()) / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
		}

		@Override
		public double getSize() {
			return this.currentSize;
		}

		public double getLastSize() {
			return this.lastSize;
		}

		private double currentSize() {
			double d = (this.timeDuration - this.remainingTimeDuration) / this.timeDuration;
			return d < 1.0 ? MathHelper.lerp(d, this.oldSize, this.newSize) : this.newSize;
		}

		@Override
		public double getShrinkingSpeed() {
			return Math.abs(this.oldSize - this.newSize) / (this.timeEnd - this.timeStart);
		}

		@Override
		public long getSizeLerpTime() {
			return this.remainingTimeDuration;
		}

		@Override
		public double getSizeLerpTarget() {
			return this.newSize;
		}

		@Override
		public WorldBorderStage getStage() {
			return this.newSize < this.oldSize ? WorldBorderStage.SHRINKING : WorldBorderStage.GROWING;
		}

		@Override
		public void onCenterChanged() {
		}

		@Override
		public void onMaxRadiusChanged() {
		}

		@Override
		public WorldBorder.Area getAreaInstance() {
			this.remainingTimeDuration--;
			this.lastSize = this.currentSize;
			this.currentSize = this.currentSize();
			if (this.remainingTimeDuration <= 0L) {
				WorldBorder.this.markDirty();
				return WorldBorder.this.new StaticArea(this.newSize);
			}
			else {
				return this;
			}
		}

		@Override
		public VoxelShape asVoxelShape() {
			return VoxelShapes.combineAndSimplify(
					VoxelShapes.UNBOUNDED,
					VoxelShapes.cuboid(
							Math.floor(this.getBoundWest(0.0F)),
							Double.NEGATIVE_INFINITY,
							Math.floor(this.getBoundNorth(0.0F)),
							Math.ceil(this.getBoundEast(0.0F)),
							Double.POSITIVE_INFINITY,
							Math.ceil(this.getBoundSouth(0.0F))
					),
					BooleanBiFunction.ONLY_FIRST
			);
		}
	}

	/**
	 * {@code Properties}.
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

		public static final WorldBorder.Properties
				DEFAULT =
				new WorldBorder.Properties(0.0, 0.0, 0.2, 5.0, 5, 300, 5.999997E7F, 0L, 0.0);
		public static final Codec<WorldBorder.Properties> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec
								                    .doubleRange(-2.9999984E7, 2.9999984E7)
								                    .fieldOf("center_x")
								                    .forGetter(WorldBorder.Properties::centerX),
						                    Codec
								                    .doubleRange(-2.9999984E7, 2.9999984E7)
								                    .fieldOf("center_z")
								                    .forGetter(WorldBorder.Properties::centerZ),
						                    Codec.DOUBLE.fieldOf("damage_per_block").forGetter(WorldBorder.Properties::damagePerBlock),
						                    Codec.DOUBLE.fieldOf("safe_zone").forGetter(WorldBorder.Properties::safeZone),
						                    Codec.INT.fieldOf("warning_blocks").forGetter(WorldBorder.Properties::warningBlocks),
						                    Codec.INT.fieldOf("warning_time").forGetter(WorldBorder.Properties::warningTime),
						                    Codec.DOUBLE.fieldOf("size").forGetter(WorldBorder.Properties::size),
						                    Codec.LONG.fieldOf("lerp_time").forGetter(WorldBorder.Properties::lerpTime),
						                    Codec.DOUBLE.fieldOf("lerp_target").forGetter(WorldBorder.Properties::lerpTarget)
				                    )
				                    .apply(instance, WorldBorder.Properties::new)
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

	/**
	 * {@code StaticArea}.
	 */
	class StaticArea implements WorldBorder.Area {

		private final double size;
		private double boundWest;
		private double boundNorth;
		private double boundEast;
		private double boundSouth;
		private VoxelShape shape;

		public StaticArea(final double size) {
			this.size = size;
			this.recalculateBounds();
		}

		@Override
		public double getBoundWest(float tickProgress) {
			return this.boundWest;
		}

		@Override
		public double getBoundEast(float tickProgress) {
			return this.boundEast;
		}

		@Override
		public double getBoundNorth(float tickProgress) {
			return this.boundNorth;
		}

		@Override
		public double getBoundSouth(float tickProgress) {
			return this.boundSouth;
		}

		@Override
		public double getSize() {
			return this.size;
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
			return this.size;
		}

		private void recalculateBounds() {
			this.boundWest = MathHelper.clamp(
					WorldBorder.this.getCenterX() - this.size / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
			this.boundNorth = MathHelper.clamp(
					WorldBorder.this.getCenterZ() - this.size / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
			this.boundEast = MathHelper.clamp(
					WorldBorder.this.getCenterX() + this.size / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
			this.boundSouth = MathHelper.clamp(
					WorldBorder.this.getCenterZ() + this.size / 2.0,
					(double) (-WorldBorder.this.maxRadius),
					(double) WorldBorder.this.maxRadius
			);
			this.shape = VoxelShapes.combineAndSimplify(
					VoxelShapes.UNBOUNDED,
					VoxelShapes.cuboid(
							Math.floor(this.getBoundWest(0.0F)),
							Double.NEGATIVE_INFINITY,
							Math.floor(this.getBoundNorth(0.0F)),
							Math.ceil(this.getBoundEast(0.0F)),
							Double.POSITIVE_INFINITY,
							Math.ceil(this.getBoundSouth(0.0F))
					),
					BooleanBiFunction.ONLY_FIRST
			);
		}

		@Override
		public void onMaxRadiusChanged() {
			this.recalculateBounds();
		}

		@Override
		public void onCenterChanged() {
			this.recalculateBounds();
		}

		@Override
		public WorldBorder.Area getAreaInstance() {
			return this;
		}

		@Override
		public VoxelShape asVoxelShape() {
			return this.shape;
		}
	}
}
