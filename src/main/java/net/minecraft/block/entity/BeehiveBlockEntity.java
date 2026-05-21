package net.minecraft.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BeesComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.BeeHiveDebugData;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * {@code BeehiveBlockEntity}.
 */
public class BeehiveBlockEntity extends BlockEntity {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final String FLOWER_POS_KEY = "flower_pos";
	private static final String BEES_KEY = "bees";
	static final List<String> IRRELEVANT_BEE_NBT_KEYS = Arrays.asList(
			"Air",
			"drop_chances",
			"equipment",
			"Brain",
			"CanPickUpLoot",
			"DeathTime",
			"fall_distance",
			"FallFlying",
			"Fire",
			"HurtByTimestamp",
			"HurtTime",
			"LeftHanded",
			"Motion",
			"NoGravity",
			"OnGround",
			"PortalCooldown",
			"Pos",
			"Rotation",
			"sleeping_pos",
			"CannotEnterHiveTicks",
			"TicksSincePollination",
			"CropsGrownSincePollination",
			"hive_pos",
			"Passengers",
			"leash",
			"UUID"
	);
	public static final int MAX_BEE_COUNT = 3;
	private static final int ANGERED_CANNOT_ENTER_HIVE_TICKS = 400;
	private static final int MIN_OCCUPATION_TICKS_WITH_NECTAR = 2400;
	public static final int MIN_OCCUPATION_TICKS_WITHOUT_NECTAR = 600;
	private final List<BeehiveBlockEntity.Bee> bees = Lists.newArrayList();
	private @Nullable BlockPos flowerPos;

	public BeehiveBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BEEHIVE, pos, state);
	}

	@Override
	public void markDirty() {
		if (this.isNearFire()) {
			this.angerBees(null, this.world.getBlockState(this.getPos()), BeehiveBlockEntity.BeeState.EMERGENCY);
		}

		super.markDirty();
	}

	public boolean isNearFire() {
		if (this.world == null) {
			return false;
		}
		else {
			for (BlockPos blockPos : BlockPos.iterate(this.pos.add(-1, -1, -1), this.pos.add(1, 1, 1))) {
				if (this.world.getBlockState(blockPos).getBlock() instanceof FireBlock) {
					return true;
				}
			}

			return false;
		}
	}

	public boolean hasNoBees() {
		return this.bees.isEmpty();
	}

	public boolean isFullOfBees() {
		return this.bees.size() == 3;
	}

	/**
	 * Anger bees.
	 *
	 * @param player player
	 * @param state state
	 * @param beeState bee state
	 */
	public void angerBees(@Nullable PlayerEntity player, BlockState state, BeehiveBlockEntity.BeeState beeState) {
		List<Entity> list = this.tryReleaseBee(state, beeState);
		if (player != null) {
			for (Entity entity : list) {
				if (entity instanceof BeeEntity beeEntity
						&& player.getEntityPos().squaredDistanceTo(entity.getEntityPos()) <= 16.0) {
					if (!this.isSmoked()) {
						beeEntity.setTarget(player);
					}
					else {
						beeEntity.setCannotEnterHiveTicks(400);
					}
				}
			}
		}
	}

	private List<Entity> tryReleaseBee(BlockState state, BeehiveBlockEntity.BeeState beeState) {
		List<Entity> list = Lists.newArrayList();
		this.bees.removeIf(bee -> releaseBee(
				this.world,
				this.pos,
				state,
				bee.createData(),
				list,
				beeState,
				this.flowerPos
		));
		if (!list.isEmpty()) {
			super.markDirty();
		}

		return list;
	}

	@Debug
	public int getBeeCount() {
		return this.bees.size();
	}

	public static int getHoneyLevel(BlockState state) {
		return state.get(BeehiveBlock.HONEY_LEVEL);
	}

	@Debug
	public boolean isSmoked() {
		return CampfireBlock.isLitCampfireInRange(this.world, this.getPos());
	}

	/**
	 * Try enter hive.
	 *
	 * @param entity entity
	 */
	public void tryEnterHive(BeeEntity entity) {
		if (this.bees.size() < 3) {
			entity.stopRiding();
			entity.removeAllPassengers();
			entity.detachLeash();
			this.addBee(BeehiveBlockEntity.BeeData.of(entity));
			if (this.world != null) {
				if (entity.hasFlower() && (!this.hasFlowerPos() || this.world.random.nextBoolean())) {
					this.flowerPos = entity.getFlowerPos();
				}

				BlockPos blockPos = this.getPos();
				this.world
						.playSound(
								null,
								(double) blockPos.getX(),
								(double) blockPos.getY(),
								(double) blockPos.getZ(),
								SoundEvents.BLOCK_BEEHIVE_ENTER,
								SoundCategory.BLOCKS,
								1.0F,
								1.0F
						);
				this.world.emitGameEvent(
						GameEvent.BLOCK_CHANGE,
						blockPos,
						GameEvent.Emitter.of(entity, this.getCachedState())
				);
			}

			entity.discard();
			super.markDirty();
		}
	}

	/**
	 * Добавляет bee.
	 *
	 * @param bee bee
	 */
	public void addBee(BeehiveBlockEntity.BeeData bee) {
		this.bees.add(new BeehiveBlockEntity.Bee(bee));
	}

	private static boolean releaseBee(
			World world,
			BlockPos pos,
			BlockState state,
			BeehiveBlockEntity.BeeData bee,
			@Nullable List<Entity> entities,
			BeehiveBlockEntity.BeeState beeState,
			@Nullable BlockPos flowerPos
	) {
		if (world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, pos)
				&& beeState != BeehiveBlockEntity.BeeState.EMERGENCY) {
			return false;
		}
		else {
			Direction direction = state.get(BeehiveBlock.FACING);
			BlockPos blockPos = pos.offset(direction);
			boolean bl = !world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty();
			if (bl && beeState != BeehiveBlockEntity.BeeState.EMERGENCY) {
				return false;
			}
			else {
				Entity entity = bee.loadEntity(world, pos);
				if (entity != null) {
					if (entity instanceof BeeEntity beeEntity) {
						if (flowerPos != null && !beeEntity.hasFlower() && world.random.nextFloat() < 0.9F) {
							beeEntity.setFlowerPos(flowerPos);
						}

						if (beeState == BeehiveBlockEntity.BeeState.HONEY_DELIVERED) {
							beeEntity.onHoneyDelivered();
							if (state.isIn(BlockTags.BEEHIVES, statex -> statex.contains(BeehiveBlock.HONEY_LEVEL))) {
								int i = getHoneyLevel(state);
								if (i < 5) {
									int j = world.random.nextInt(100) == 0 ? 2 : 1;
									if (i + j > 5) {
										j--;
									}

									world.setBlockState(pos, state.with(BeehiveBlock.HONEY_LEVEL, i + j));
								}
							}
						}

						if (entities != null) {
							entities.add(beeEntity);
						}

						float f = entity.getWidth();
						double d = bl ? 0.0 : 0.55 + f / 2.0F;
						double e = pos.getX() + 0.5 + d * direction.getOffsetX();
						double g = pos.getY() + 0.5 - entity.getHeight() / 2.0F;
						double h = pos.getZ() + 0.5 + d * direction.getOffsetZ();
						entity.refreshPositionAndAngles(e, g, h, entity.getYaw(), entity.getPitch());
					}

					world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
					world.emitGameEvent(
							GameEvent.BLOCK_CHANGE,
							pos,
							GameEvent.Emitter.of(entity, world.getBlockState(pos))
					);
					return world.spawnEntity(entity);
				}
				else {
					return false;
				}
			}
		}
	}

	private boolean hasFlowerPos() {
		return this.flowerPos != null;
	}

	private static void tickBees(
			World world,
			BlockPos pos,
			BlockState state,
			List<BeehiveBlockEntity.Bee> bees,
			@Nullable BlockPos flowerPos
	) {
		boolean bl = false;
		Iterator<BeehiveBlockEntity.Bee> iterator = bees.iterator();

		while (iterator.hasNext()) {
			BeehiveBlockEntity.Bee bee = iterator.next();
			if (bee.canExitHive()) {
				BeehiveBlockEntity.BeeState
						beeState =
						bee.hasNectar() ? BeehiveBlockEntity.BeeState.HONEY_DELIVERED
						                : BeehiveBlockEntity.BeeState.BEE_RELEASED;
				if (releaseBee(world, pos, state, bee.createData(), null, beeState, flowerPos)) {
					bl = true;
					iterator.remove();
				}
			}
		}

		if (bl) {
			markDirty(world, pos, state);
		}
	}

	/**
	 * Server tick.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 * @param blockEntity block entity
	 */
	public static void serverTick(World world, BlockPos pos, BlockState state, BeehiveBlockEntity blockEntity) {
		tickBees(world, pos, state, blockEntity.bees, blockEntity.flowerPos);
		if (!blockEntity.bees.isEmpty() && world.getRandom().nextDouble() < 0.005) {
			double d = pos.getX() + 0.5;
			double e = pos.getY();
			double f = pos.getZ() + 0.5;
			world.playSound(null, d, e, f, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.bees.clear();
		view
				.<List<BeehiveBlockEntity.BeeData>>read("bees", BeehiveBlockEntity.BeeData.LIST_CODEC)
				.orElse(List.of())
				.forEach(this::addBee);
		this.flowerPos = view.<BlockPos>read("flower_pos", BlockPos.CODEC).orElse(null);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.put("bees", BeehiveBlockEntity.BeeData.LIST_CODEC, this.createBeesData());
		view.putNullable("flower_pos", BlockPos.CODEC, this.flowerPos);
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		this.bees.clear();
		List<BeehiveBlockEntity.BeeData>
				list =
				components.getOrDefault(DataComponentTypes.BEES, BeesComponent.DEFAULT).bees();
		list.forEach(this::addBee);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.BEES, new BeesComponent(this.createBeesData()));
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		super.removeFromCopiedStackData(view);
		view.remove("bees");
	}

	private List<BeehiveBlockEntity.BeeData> createBeesData() {
		return this.bees.stream().map(BeehiveBlockEntity.Bee::createData).toList();
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		tracker.track(DebugSubscriptionTypes.BEE_HIVES, () -> BeeHiveDebugData.fromBeehive(this));
	}

	/**
	 * {@code Bee}.
	 */
	static class Bee {

		private final BeehiveBlockEntity.BeeData data;
		private int ticksInHive;

		Bee(BeehiveBlockEntity.BeeData data) {
			this.data = data;
			this.ticksInHive = data.ticksInHive();
		}

		/**
		 * Проверяет возможность exit hive.
		 *
		 * @return boolean — {@code true} если условие выполнено
		 */
		public boolean canExitHive() {
			return this.ticksInHive++ > this.data.minTicksInHive;
		}

		public BeehiveBlockEntity.BeeData createData() {
			return new BeehiveBlockEntity.BeeData(this.data.entityData, this.ticksInHive, this.data.minTicksInHive);
		}

		public boolean hasNectar() {
			return this.data.entityData.getNbtWithoutId().getBoolean("HasNectar", false);
		}
	}

	/**
	 * {@code BeeData}.
	 */
	public record BeeData(TypedEntityData<EntityType<?>> entityData, int ticksInHive, int minTicksInHive) {

		public static final Codec<BeehiveBlockEntity.BeeData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    TypedEntityData
								                    .createCodec(EntityType.CODEC)
								                    .fieldOf("entity_data")
								                    .forGetter(BeehiveBlockEntity.BeeData::entityData),
						                    Codec.INT.fieldOf("ticks_in_hive").forGetter(BeehiveBlockEntity.BeeData::ticksInHive),
						                    Codec.INT.fieldOf("min_ticks_in_hive").forGetter(BeehiveBlockEntity.BeeData::minTicksInHive)
				                    )
				                    .apply(instance, BeehiveBlockEntity.BeeData::new)
		);
		public static final Codec<List<BeehiveBlockEntity.BeeData>> LIST_CODEC = CODEC.listOf();
		public static final PacketCodec<RegistryByteBuf, BeehiveBlockEntity.BeeData> PACKET_CODEC = PacketCodec.tuple(
				TypedEntityData.createPacketCodec(EntityType.PACKET_CODEC),
				BeehiveBlockEntity.BeeData::entityData,
				PacketCodecs.VAR_INT,
				BeehiveBlockEntity.BeeData::ticksInHive,
				PacketCodecs.VAR_INT,
				BeehiveBlockEntity.BeeData::minTicksInHive,
				BeehiveBlockEntity.BeeData::new
		);

		public static BeehiveBlockEntity.BeeData of(Entity entity) {
			BeehiveBlockEntity.BeeData var5;
			try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
					entity.getErrorReporterContext(),
					BeehiveBlockEntity.LOGGER
			)
			) {
				NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
				entity.saveData(nbtWriteView);
				BeehiveBlockEntity.IRRELEVANT_BEE_NBT_KEYS.forEach(nbtWriteView::remove);
				NbtCompound nbtCompound = nbtWriteView.getNbt();
				boolean bl = nbtCompound.getBoolean("HasNectar", false);
				var5 =
						new BeehiveBlockEntity.BeeData(
								TypedEntityData.create(entity.getType(), nbtCompound),
								0,
								bl ? 2400 : 600
						);
			}

			return var5;
		}

		public static BeehiveBlockEntity.BeeData create(int ticksInHive) {
			return new BeehiveBlockEntity.BeeData(
					TypedEntityData.create(EntityType.BEE, new NbtCompound()),
					ticksInHive,
					600
			);
		}

		/**
		 * Загружает entity.
		 *
		 * @param world world
		 * @param pos pos
		 *
		 * @return @Nullable Entity — результат операции
		 */
		public @Nullable Entity loadEntity(World world, BlockPos pos) {
			NbtCompound nbtCompound = this.entityData.copyNbtWithoutId();
			BeehiveBlockEntity.IRRELEVANT_BEE_NBT_KEYS.forEach(nbtCompound::remove);
			Entity
					entity =
					EntityType.loadEntityWithPassengers(
							this.entityData.getType(),
							nbtCompound,
							world,
							SpawnReason.LOAD,
							LoadedEntityProcessor.NOOP
					);
			if (entity != null && entity.getType().isIn(EntityTypeTags.BEEHIVE_INHABITORS)) {
				entity.setNoGravity(true);
				if (entity instanceof BeeEntity beeEntity) {
					beeEntity.setHivePos(pos);
					tickEntity(this.ticksInHive, beeEntity);
				}

				return entity;
			}
			else {
				return null;
			}
		}

		private static void tickEntity(int ticksInHive, BeeEntity beeEntity) {
			int i = beeEntity.getBreedingAge();
			if (i < 0) {
				beeEntity.setBreedingAge(Math.min(0, i + ticksInHive));
			}
			else if (i > 0) {
				beeEntity.setBreedingAge(Math.max(0, i - ticksInHive));
			}

			beeEntity.setLoveTicks(Math.max(0, beeEntity.getLoveTicks() - ticksInHive));
		}
	}

	/**
	 * {@code BeeState}.
	 */
	public static enum BeeState {
		HONEY_DELIVERED,
		BEE_RELEASED,
		EMERGENCY;
	}
}
