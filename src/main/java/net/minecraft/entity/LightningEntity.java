package net.minecraft.entity;

import com.google.common.collect.Sets;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.HoneycombItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Сущность удара молнии. Существует несколько тиков, поджигает блоки вокруг,
 * наносит урон существам в радиусе {@code STRIKE_RADIUS}, очищает окисление меди
 * в радиусе {@code EFFECT_RADIUS}. Может быть «косметической» (без эффектов на сервере).
 */
public class LightningEntity extends Entity {

	private static final int INITIAL_AMBIENT_TICK = 2;
	private static final double STRIKE_RADIUS = 3.0;
	private static final double EFFECT_RADIUS = 15.0;
	private static final double EFFECT_BOX_HEIGHT_EXTRA = 6.0;
	private static final float THUNDER_VOLUME = 10000.0F;
	private static final float IMPACT_VOLUME = 2.0F;
	private static final float CRITERIA_TRIGGER_RADIUS = 256.0F;
	private int ambientTick;
	public long seed;
	private int remainingActions;
	private boolean cosmetic;
	private @Nullable ServerPlayerEntity channeler;
	private final Set<Entity> struckEntities = Sets.newHashSet();
	private int blocksSetOnFire;

	public LightningEntity(EntityType<? extends LightningEntity> entityType, World world) {
		super(entityType, world);
		this.ambientTick = INITIAL_AMBIENT_TICK;
		this.seed = this.random.nextLong();
		this.remainingActions = this.random.nextInt(3) + 1;
	}

	public void setCosmetic(boolean cosmetic) {
		this.cosmetic = cosmetic;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.WEATHER;
	}

	public @Nullable ServerPlayerEntity getChanneler() {
		return this.channeler;
	}

	public void setChanneler(@Nullable ServerPlayerEntity channeler) {
		this.channeler = channeler;
	}

	private void powerLightningRod() {
		BlockPos blockPos = this.getAffectedBlockPos();
		BlockState blockState = this.getEntityWorld().getBlockState(blockPos);
		if (blockState.getBlock() instanceof LightningRodBlock lightningRodBlock) {
			lightningRodBlock.setPowered(blockState, this.getEntityWorld(), blockPos);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.ambientTick == INITIAL_AMBIENT_TICK) {
			if (this.getEntityWorld().isClient()) {
				this.getEntityWorld().playSoundClient(
					this.getX(), this.getY(), this.getZ(),
					SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
					SoundCategory.WEATHER,
					THUNDER_VOLUME,
					0.8F + this.random.nextFloat() * 0.2F,
					false
				);
				this.getEntityWorld().playSoundClient(
					this.getX(), this.getY(), this.getZ(),
					SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
					SoundCategory.WEATHER,
					IMPACT_VOLUME,
					0.5F + this.random.nextFloat() * 0.2F,
					false
				);
			}
			else {
				Difficulty difficulty = this.getEntityWorld().getDifficulty();
				if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
					this.spawnFire(4);
				}

				this.powerLightningRod();
				cleanOxidation(this.getEntityWorld(), this.getAffectedBlockPos());
				this.emitGameEvent(GameEvent.LIGHTNING_STRIKE);
			}
		}

		this.ambientTick--;
		if (this.ambientTick < 0) {
			if (this.remainingActions == 0) {
				if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
					List<Entity> nearbyEntities = this.getEntityWorld().getOtherEntities(
						this,
						new Box(
							this.getX() - EFFECT_RADIUS,
							this.getY() - EFFECT_RADIUS,
							this.getZ() - EFFECT_RADIUS,
							this.getX() + EFFECT_RADIUS,
							this.getY() + EFFECT_BOX_HEIGHT_EXTRA + EFFECT_RADIUS,
							this.getZ() + EFFECT_RADIUS
						),
						entity -> entity.isAlive() && !this.struckEntities.contains(entity)
					);

					for (ServerPlayerEntity nearbyPlayer : serverWorld.getPlayers(
						player -> player.distanceTo(this) < CRITERIA_TRIGGER_RADIUS
					)) {
						Criteria.LIGHTNING_STRIKE.trigger(nearbyPlayer, this, nearbyEntities);
					}
				}

				this.discard();
			}
			else if (this.ambientTick < -this.random.nextInt(10)) {
				this.remainingActions--;
				this.ambientTick = 1;
				this.seed = this.random.nextLong();
				this.spawnFire(0);
			}
		}

		if (this.ambientTick >= 0) {
			if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
				if (!this.cosmetic) {
					List<Entity> struckNow = this.getEntityWorld().getOtherEntities(
						this,
						new Box(
							this.getX() - STRIKE_RADIUS,
							this.getY() - STRIKE_RADIUS,
							this.getZ() - STRIKE_RADIUS,
							this.getX() + STRIKE_RADIUS,
							this.getY() + EFFECT_BOX_HEIGHT_EXTRA + STRIKE_RADIUS,
							this.getZ() + STRIKE_RADIUS
						),
						Entity::isAlive
					);

					for (Entity entity : struckNow) {
						entity.onStruckByLightning(serverWorld, this);
					}

					this.struckEntities.addAll(struckNow);
					if (this.channeler != null) {
						Criteria.CHANNELED_LIGHTNING.trigger(this.channeler, struckNow);
					}
				}
			}
			else {
				this.getEntityWorld().setLightningTicksLeft(2);
			}
		}
	}

	private BlockPos getAffectedBlockPos() {
		Vec3d vec3d = this.getEntityPos();
		return BlockPos.ofFloored(vec3d.x, vec3d.y - 1.0E-6, vec3d.z);
	}

	private void spawnFire(int spreadAttempts) {
		if (this.cosmetic || !(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		BlockPos strikePos = this.getBlockPos();
		if (!serverWorld.canFireSpread(strikePos)) {
			return;
		}

		BlockState fireState = AbstractFireBlock.getState(serverWorld, strikePos);
		if (serverWorld.getBlockState(strikePos).isAir() && fireState.canPlaceAt(serverWorld, strikePos)) {
			serverWorld.setBlockState(strikePos, fireState);
			this.blocksSetOnFire++;
		}

		for (int attempt = 0; attempt < spreadAttempts; attempt++) {
			BlockPos spreadPos = strikePos.add(
					this.random.nextInt(3) - 1,
					this.random.nextInt(3) - 1,
					this.random.nextInt(3) - 1
			);
			BlockState spreadFireState = AbstractFireBlock.getState(serverWorld, spreadPos);
			if (serverWorld.getBlockState(spreadPos).isAir() && spreadFireState.canPlaceAt(serverWorld, spreadPos)) {
				serverWorld.setBlockState(spreadPos, spreadFireState);
				this.blocksSetOnFire++;
			}
		}
	}

	private static void cleanOxidation(World world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		boolean isWaxed = HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(blockState.getBlock()) != null;
		boolean isOxidizable = blockState.getBlock() instanceof Oxidizable;
		if (!isOxidizable && !isWaxed) {
			return;
		}

		if (isOxidizable) {
			world.setBlockState(pos, Oxidizable.getUnaffectedOxidationState(world.getBlockState(pos)));
		}

		BlockPos.Mutable mutable = pos.mutableCopy();
		int spreadCount = world.random.nextInt(3) + 3;

		for (int spread = 0; spread < spreadCount; spread++) {
			int chainLength = world.random.nextInt(8) + 1;
			cleanOxidationAround(world, pos, mutable, chainLength);
		}
	}

	private static void cleanOxidationAround(World world, BlockPos pos, BlockPos.Mutable mutablePos, int count) {
		mutablePos.set(pos);

		for (int step = 0; step < count; step++) {
			Optional<BlockPos> next = cleanOxidationAround(world, mutablePos);
			if (next.isEmpty()) {
				break;
			}

			mutablePos.set(next.get());
		}
	}

	private static Optional<BlockPos> cleanOxidationAround(World world, BlockPos pos) {
		for (BlockPos blockPos : BlockPos.iterateRandomly(world.random, 10, pos, 1)) {
			BlockState blockState = world.getBlockState(blockPos);
			if (blockState.getBlock() instanceof Oxidizable) {
				Oxidizable
						.getDecreasedOxidationState(blockState)
						.ifPresent(state -> world.setBlockState(blockPos, state));
				world.syncWorldEvent(3002, blockPos, -1);
				return Optional.of(blockPos);
			}
		}

		return Optional.empty();
	}

	@Override
	public boolean shouldRender(double distance) {
		double renderDistance = 64.0 * getRenderDistanceMultiplier();
		return distance < renderDistance * renderDistance;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void readCustomData(ReadView view) {
	}

	@Override
	protected void writeCustomData(WriteView view) {
	}

	public int getBlocksSetOnFire() {
		return this.blocksSetOnFire;
	}

	public Stream<Entity> getStruckEntities() {
		return this.struckEntities.stream().filter(Entity::isAlive);
	}

	@Override
	public final boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}
}
