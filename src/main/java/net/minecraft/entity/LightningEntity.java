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
 * {@code LightningEntity}.
 */
public class LightningEntity extends Entity {

	private static final int INITIAL_AMBIENT_TICK = 2;
	private static final double STRIKE_RADIUS = 3.0;
	private static final double EFFECT_RADIUS = 15.0;
	private int ambientTick;
	public long seed;
	private int remainingActions;
	private boolean cosmetic;
	private @Nullable ServerPlayerEntity channeler;
	private final Set<Entity> struckEntities = Sets.newHashSet();
	private int blocksSetOnFire;

	public LightningEntity(EntityType<? extends LightningEntity> entityType, World world) {
		super(entityType, world);
		this.ambientTick = 2;
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
		if (this.ambientTick == 2) {
			if (this.getEntityWorld().isClient()) {
				this.getEntityWorld()
				    .playSoundClient(
						    this.getX(),
						    this.getY(),
						    this.getZ(),
						    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
						    SoundCategory.WEATHER,
						    10000.0F,
						    0.8F + this.random.nextFloat() * 0.2F,
						    false
				    );
				this.getEntityWorld()
				    .playSoundClient(
						    this.getX(),
						    this.getY(),
						    this.getZ(),
						    SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
						    SoundCategory.WEATHER,
						    2.0F,
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
				if (this.getEntityWorld() instanceof ServerWorld) {
					List<Entity> list = this.getEntityWorld()
					                        .getOtherEntities(
							                        this,
							                        new Box(
									                        this.getX() - 15.0,
									                        this.getY() - 15.0,
									                        this.getZ() - 15.0,
									                        this.getX() + 15.0,
									                        this.getY() + 6.0 + 15.0,
									                        this.getZ() + 15.0
							                        ),
							                        entityx -> entityx.isAlive() && !this.struckEntities.contains(
									                        entityx)
					                        );

					for (ServerPlayerEntity serverPlayerEntity : ((ServerWorld) this.getEntityWorld())
							.getPlayers(serverPlayerEntityx -> serverPlayerEntityx.distanceTo(this) < 256.0F)) {
						Criteria.LIGHTNING_STRIKE.trigger(serverPlayerEntity, this, list);
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
			if (!(this.getEntityWorld() instanceof ServerWorld)) {
				this.getEntityWorld().setLightningTicksLeft(2);
			}
			else if (!this.cosmetic) {
				List<Entity> list = this.getEntityWorld()
				                        .getOtherEntities(
						                        this,
						                        new Box(
								                        this.getX() - 3.0,
								                        this.getY() - 3.0,
								                        this.getZ() - 3.0,
								                        this.getX() + 3.0,
								                        this.getY() + 6.0 + 3.0,
								                        this.getZ() + 3.0
						                        ),
						                        Entity::isAlive
				                        );

				for (Entity entity : list) {
					entity.onStruckByLightning((ServerWorld) this.getEntityWorld(), this);
				}

				this.struckEntities.addAll(list);
				if (this.channeler != null) {
					Criteria.CHANNELED_LIGHTNING.trigger(this.channeler, list);
				}
			}
		}
	}

	private BlockPos getAffectedBlockPos() {
		Vec3d vec3d = this.getEntityPos();
		return BlockPos.ofFloored(vec3d.x, vec3d.y - 1.0E-6, vec3d.z);
	}

	private void spawnFire(int spreadAttempts) {
		if (!this.cosmetic && this.getEntityWorld() instanceof ServerWorld serverWorld) {
			BlockPos var7 = this.getBlockPos();
			if (serverWorld.canFireSpread(var7)) {
				BlockState blockState = AbstractFireBlock.getState(serverWorld, var7);
				if (serverWorld.getBlockState(var7).isAir() && blockState.canPlaceAt(serverWorld, var7)) {
					serverWorld.setBlockState(var7, blockState);
					this.blocksSetOnFire++;
				}

				for (int i = 0; i < spreadAttempts; i++) {
					BlockPos
							blockPos2 =
							var7.add(
									this.random.nextInt(3) - 1,
									this.random.nextInt(3) - 1,
									this.random.nextInt(3) - 1
							);
					blockState = AbstractFireBlock.getState(serverWorld, blockPos2);
					if (serverWorld.getBlockState(blockPos2).isAir() && blockState.canPlaceAt(serverWorld, blockPos2)) {
						serverWorld.setBlockState(blockPos2, blockState);
						this.blocksSetOnFire++;
					}
				}
			}
		}
	}

	private static void cleanOxidation(World world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		boolean bl = HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(blockState.getBlock()) != null;
		boolean bl2 = blockState.getBlock() instanceof Oxidizable;
		if (bl2 || bl) {
			if (bl2) {
				world.setBlockState(pos, Oxidizable.getUnaffectedOxidationState(world.getBlockState(pos)));
			}

			BlockPos.Mutable mutable = pos.mutableCopy();
			int i = world.random.nextInt(3) + 3;

			for (int j = 0; j < i; j++) {
				int k = world.random.nextInt(8) + 1;
				cleanOxidationAround(world, pos, mutable, k);
			}
		}
	}

	private static void cleanOxidationAround(World world, BlockPos pos, BlockPos.Mutable mutablePos, int count) {
		mutablePos.set(pos);

		for (int i = 0; i < count; i++) {
			Optional<BlockPos> optional = cleanOxidationAround(world, mutablePos);
			if (optional.isEmpty()) {
				break;
			}

			mutablePos.set(optional.get());
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
		double d = 64.0 * getRenderDistanceMultiplier();
		return distance < d * d;
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
