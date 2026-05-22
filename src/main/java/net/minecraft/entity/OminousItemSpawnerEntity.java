package net.minecraft.entity;

import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Сущность-спаунер предметов, создаваемая при активации зловещего испытания.
 * Парит в воздухе, через случайное время выбрасывает предмет (или снаряд) вниз
 * и уничтожается. На клиенте каждые 5 тиков генерирует частицы {@code OMINOUS_SPAWNING}.
 */
public class OminousItemSpawnerEntity extends Entity {

	private static final int MIN_SPAWN_ITEM_AFTER_TICKS = 60;
	private static final int MAX_SPAWN_ITEM_AFTER_TICKS = 120;
	private static final double PARTICLE_SPREAD = 0.4;
	private static final String SPAWN_ITEM_AFTER_TICKS_NBT_KEY = "spawn_item_after_ticks";
	private static final String ITEM_NBT_KEY = "item";
	private static final TrackedData<ItemStack>
			ITEM =
			DataTracker.registerData(OminousItemSpawnerEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	public static final int PRE_SPAWN_SOUND_OFFSET = 36;
	private long spawnItemAfterTicks;

	public OminousItemSpawnerEntity(EntityType<? extends OminousItemSpawnerEntity> entityType, World world) {
		super(entityType, world);
		this.noClip = true;
	}

	/**
	 * Создаёт новый спаунер с заданным предметом и случайной задержкой спауна.
	 * Задержка выбирается равномерно в диапазоне [{@code MIN_SPAWN_ITEM_AFTER_TICKS}, {@code MAX_SPAWN_ITEM_AFTER_TICKS}].
	 */
	public static OminousItemSpawnerEntity create(World world, ItemStack stack) {
		OminousItemSpawnerEntity spawner = new OminousItemSpawnerEntity(EntityType.OMINOUS_ITEM_SPAWNER, world);
		spawner.spawnItemAfterTicks = world.random.nextBetween(MIN_SPAWN_ITEM_AFTER_TICKS, MAX_SPAWN_ITEM_AFTER_TICKS);
		spawner.setItem(stack);
		return spawner;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			this.tickServer(serverWorld);
		}
		else {
			this.tickClient();
		}
	}

	private void tickServer(ServerWorld world) {
		if (this.age == this.spawnItemAfterTicks - PRE_SPAWN_SOUND_OFFSET) {
			world.playSound(
					null,
					this.getBlockPos(),
					SoundEvents.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM,
					SoundCategory.NEUTRAL
			);
		}

		if (this.age >= this.spawnItemAfterTicks) {
			this.spawnItem();
			this.kill(world);
		}
	}

	private void tickClient() {
		if (this.getEntityWorld().getTime() % 5L == 0L) {
			this.addParticles();
		}
	}

	private void spawnItem() {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			ItemStack itemStack = this.getItem();
			if (!itemStack.isEmpty()) {
				Entity entity;
				if (itemStack.getItem() instanceof ProjectileItem projectileItem) {
					entity = this.spawnProjectile(serverWorld, projectileItem, itemStack);
				}
				else {
					entity = new ItemEntity(serverWorld, this.getX(), this.getY(), this.getZ(), itemStack);
					serverWorld.spawnEntity(entity);
				}

				serverWorld.syncWorldEvent(3021, this.getBlockPos(), 1);
				serverWorld.emitGameEvent(entity, GameEvent.ENTITY_PLACE, this.getEntityPos());
				this.setItem(ItemStack.EMPTY);
			}
		}
	}

	private Entity spawnProjectile(ServerWorld world, ProjectileItem item, ItemStack stack) {
		ProjectileItem.Settings settings = item.getProjectileSettings();
		settings
				.overrideDispenseEvent()
				.ifPresent(dispenseEvent -> world.syncWorldEvent(dispenseEvent, this.getBlockPos(), 0));
		Direction direction = Direction.DOWN;
		ProjectileEntity projectileEntity = ProjectileEntity.spawnWithVelocity(
				item.createEntity(world, this.getEntityPos(), stack, direction),
				world,
				stack,
				direction.getOffsetX(),
				direction.getOffsetY(),
				direction.getOffsetZ(),
				settings.power(),
				settings.uncertainty()
		);
		projectileEntity.setOwner(this);
		return projectileEntity;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ITEM, ItemStack.EMPTY);
	}

	@Override
	protected void readCustomData(ReadView view) {
		this.setItem(view.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		this.spawnItemAfterTicks = view.getLong("spawn_item_after_ticks", 0L);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		if (!this.getItem().isEmpty()) {
			view.put("item", ItemStack.CODEC, this.getItem());
		}

		view.putLong("spawn_item_after_ticks", this.spawnItemAfterTicks);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return false;
	}

	@Override
	protected boolean couldAcceptPassenger() {
		return false;
	}

	@Override
	protected void addPassenger(Entity passenger) {
		throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	@Override
	public boolean canAvoidTraps() {
		return true;
	}

	/**
	 * Генерирует клиентские частицы {@code OMINOUS_SPAWNING} вокруг спаунера.
	 * Каждый вызов создаёт от 1 до 3 частиц со случайным смещением по Гауссу.
	 */
	public void addParticles() {
		Vec3d center = getEntityPos();
		int particleCount = random.nextBetween(1, 3);

		for (int index = 0; index < particleCount; index++) {
			Vec3d particlePos = new Vec3d(
					getX() + PARTICLE_SPREAD * (random.nextGaussian() - random.nextGaussian()),
					getY() + PARTICLE_SPREAD * (random.nextGaussian() - random.nextGaussian()),
					getZ() + PARTICLE_SPREAD * (random.nextGaussian() - random.nextGaussian())
			);
			Vec3d velocity = center.relativize(particlePos);

			getEntityWorld().addParticleClient(
					ParticleTypes.OMINOUS_SPAWNING,
					center.getX(),
					center.getY(),
					center.getZ(),
					velocity.getX(),
					velocity.getY(),
					velocity.getZ()
			);
		}
	}

	public ItemStack getItem() {
		return this.getDataTracker().get(ITEM);
	}

	private void setItem(ItemStack stack) {
		this.getDataTracker().set(ITEM, stack);
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}
}
