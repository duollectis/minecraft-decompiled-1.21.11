package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Optional;

/**
 * Базовый класс всех клиентских частиц. Реализует физику движения с учётом
 * гравитации, трения и коллизий с блоками мира. Поддерживает интерполяцию
 * позиции между тиками для плавного рендеринга.
 *
 * <p>Жизненный цикл: каждый тик вызывается {@link #tick()}, который обновляет
 * позицию и возраст. При достижении {@code maxAge} частица помечается мёртвой
 * через {@link #markDead()}.
 */
@Environment(EnvType.CLIENT)
public abstract class Particle {

	private static final Box EMPTY_BOUNDING_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
	private static final double MAX_SQUARED_COLLISION_CHECK_DISTANCE = MathHelper.square(100.0);

	protected final ClientWorld world;
	protected double lastX;
	protected double lastY;
	protected double lastZ;
	protected double x;
	protected double y;
	protected double z;
	protected double velocityX;
	protected double velocityY;
	protected double velocityZ;
	private Box boundingBox = EMPTY_BOUNDING_BOX;
	protected boolean onGround;
	protected boolean collidesWithWorld = true;
	private boolean stopped;
	protected boolean dead;
	protected float spacingXZ = 0.6F;
	protected float spacingY = 1.8F;
	protected final Random random = Random.create();
	protected int age;
	protected int maxAge;
	protected float gravityStrength;
	protected float velocityMultiplier = 0.98F;
	protected boolean ascending = false;

	protected Particle(ClientWorld world, double x, double y, double z) {
		this.world = world;
		this.setBoundingBoxSpacing(0.2F, 0.2F);
		this.setPos(x, y, z);
		this.lastX = x;
		this.lastY = y;
		this.lastZ = z;
		this.maxAge = (int) (4.0F / (this.random.nextFloat() * 0.9F + 0.1F));
	}

	public Particle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ
	) {
		this(world, x, y, z);
		this.velocityX = velocityX + (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F;
		this.velocityY = velocityY + (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F;
		this.velocityZ = velocityZ + (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F;

		double speed = (this.random.nextFloat() + this.random.nextFloat() + 1.0F) * 0.15F;
		double magnitude = Math.sqrt(
				this.velocityX * this.velocityX
				+ this.velocityY * this.velocityY
				+ this.velocityZ * this.velocityZ
		);
		this.velocityX = this.velocityX / magnitude * speed * 0.4F;
		this.velocityY = this.velocityY / magnitude * speed * 0.4F + 0.1F;
		this.velocityZ = this.velocityZ / magnitude * speed * 0.4F;
	}

	/**
	 * Масштабирует скорость частицы: горизонтальные компоненты умножаются на
	 * {@code speed}, вертикальная — интерполируется к 0.1 с тем же коэффициентом.
	 *
	 * @param speed коэффициент масштабирования скорости
	 * @return эта же частица (для цепочки вызовов)
	 */
	public Particle move(float speed) {
		this.velocityX *= speed;
		this.velocityY = (this.velocityY - 0.1F) * speed + 0.1F;
		this.velocityZ *= speed;
		return this;
	}

	public void setVelocity(double velocityX, double velocityY, double velocityZ) {
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
	}

	/**
	 * Масштабирует размер хитбокса частицы.
	 *
	 * @param scale коэффициент масштабирования
	 * @return эта же частица (для цепочки вызовов)
	 */
	public Particle scale(float scale) {
		this.setBoundingBoxSpacing(0.2F * scale, 0.2F * scale);
		return this;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public int getMaxAge() {
		return this.maxAge;
	}

	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		if (this.age++ >= this.maxAge) {
			this.markDead();
			return;
		}

		this.velocityY -= 0.04 * this.gravityStrength;
		this.move(this.velocityX, this.velocityY, this.velocityZ);

		if (this.ascending && this.y == this.lastY) {
			this.velocityX *= 1.1;
			this.velocityZ *= 1.1;
		}

		this.velocityX *= this.velocityMultiplier;
		this.velocityY *= this.velocityMultiplier;
		this.velocityZ *= this.velocityMultiplier;

		if (this.onGround) {
			this.velocityX *= 0.7F;
			this.velocityZ *= 0.7F;
		}
	}

	public abstract ParticleTextureSheet textureSheet();

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ", Pos (" + this.x + "," + this.y + "," + this.z + "), Age " + this.age;
	}

	public void markDead() {
		this.dead = true;
	}

	protected void setBoundingBoxSpacing(float spacingXZ, float spacingY) {
		if (spacingXZ == this.spacingXZ && spacingY == this.spacingY) {
			return;
		}

		this.spacingXZ = spacingXZ;
		this.spacingY = spacingY;
		Box box = this.getBoundingBox();
		double centerX = (box.minX + box.maxX - spacingXZ) / 2.0;
		double centerZ = (box.minZ + box.maxZ - spacingXZ) / 2.0;
		this.setBoundingBox(new Box(
				centerX,
				box.minY,
				centerZ,
				centerX + this.spacingXZ,
				box.minY + this.spacingY,
				centerZ + this.spacingXZ
		));
	}

	public void setPos(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		float halfXZ = this.spacingXZ / 2.0F;
		float height = this.spacingY;
		this.setBoundingBox(new Box(x - halfXZ, y, z - halfXZ, x + halfXZ, y + height, z + halfXZ));
	}

	/**
	 * Перемещает частицу на вектор (dx, dy, dz) с учётом коллизий с блоками.
	 * Если частица упирается в блок по какой-либо оси, скорость по этой оси
	 * обнуляется. При полной остановке по вертикали частица помечается как
	 * «застрявшая» и перестаёт двигаться.
	 *
	 * @param dx смещение по X
	 * @param dy смещение по Y
	 * @param dz смещение по Z
	 */
	public void move(double dx, double dy, double dz) {
		if (this.stopped) {
			return;
		}

		double originalDx = dx;
		double originalDy = dy;
		double originalDz = dz;

		if (this.collidesWithWorld
				&& (dx != 0.0 || dy != 0.0 || dz != 0.0)
				&& dx * dx + dy * dy + dz * dz < MAX_SQUARED_COLLISION_CHECK_DISTANCE
		) {
			Vec3d adjusted = Entity.adjustMovementForCollisions(
					null,
					new Vec3d(dx, dy, dz),
					this.getBoundingBox(),
					this.world,
					List.of()
			);
			dx = adjusted.x;
			dy = adjusted.y;
			dz = adjusted.z;
		}

		if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
			this.setBoundingBox(this.getBoundingBox().offset(dx, dy, dz));
			this.repositionFromBoundingBox();
		}

		if (Math.abs(originalDy) >= 1.0E-5F && Math.abs(dy) < 1.0E-5F) {
			this.stopped = true;
		}

		this.onGround = originalDy != dy && originalDy < 0.0;

		if (originalDx != dx) {
			this.velocityX = 0.0;
		}

		if (originalDz != dz) {
			this.velocityZ = 0.0;
		}
	}

	protected void repositionFromBoundingBox() {
		Box box = this.getBoundingBox();
		this.x = (box.minX + box.maxX) / 2.0;
		this.y = box.minY;
		this.z = (box.minZ + box.maxZ) / 2.0;
	}

	protected int getBrightness(float tint) {
		BlockPos blockPos = BlockPos.ofFloored(this.x, this.y, this.z);
		return this.world.isChunkLoaded(blockPos) ? WorldRenderer.getLightmapCoordinates(this.world, blockPos) : 0;
	}

	public boolean isAlive() {
		return !this.dead;
	}

	public Box getBoundingBox() {
		return this.boundingBox;
	}

	public void setBoundingBox(Box boundingBox) {
		this.boundingBox = boundingBox;
	}

	public Optional<ParticleGroup> getGroup() {
		return Optional.empty();
	}

	/**
	 * Описывает динамическую прозрачность частицы в зависимости от её возраста.
	 * Позволяет задать плавное появление и исчезновение через линейную
	 * интерполяцию альфа-канала в заданном диапазоне нормализованного возраста.
	 */
	@Environment(EnvType.CLIENT)
	public record DynamicAlpha(float startAlpha, float endAlpha, float startAtNormalizedAge, float endAtNormalizedAge) {

		public static final Particle.DynamicAlpha OPAQUE = new Particle.DynamicAlpha(1.0F, 1.0F, 0.0F, 1.0F);

		public boolean isOpaque() {
			return this.startAlpha >= 1.0F && this.endAlpha >= 1.0F;
		}

		/**
		 * Вычисляет текущее значение альфа-канала для заданного возраста частицы.
		 * Если начальная и конечная альфа совпадают — возвращает константу без
		 * вычислений. Иначе интерполирует в диапазоне нормализованного возраста.
		 *
		 * @param age         текущий возраст частицы в тиках
		 * @param maxAge      максимальный возраст частицы
		 * @param tickProgress прогресс текущего тика (0.0–1.0)
		 * @return значение альфа-канала в диапазоне [0.0, 1.0]
		 */
		public float getAlpha(int age, int maxAge, float tickProgress) {
			if (MathHelper.approximatelyEquals(this.startAlpha, this.endAlpha)) {
				return this.startAlpha;
			}

			float lerpProgress = MathHelper.getLerpProgress(
					(age + tickProgress) / maxAge,
					this.startAtNormalizedAge,
					this.endAtNormalizedAge
			);
			return MathHelper.clampedLerp(lerpProgress, this.startAlpha, this.endAlpha);
		}
	}
}
