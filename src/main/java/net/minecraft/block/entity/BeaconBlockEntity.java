package net.minecraft.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Stainable;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Блок-сущность маяка. Управляет логикой луча, уровнями пирамиды и применением
 * эффектов статуса к игрокам в радиусе действия.
 */
public class BeaconBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Nameable, BeamEmitter {

	private static final int MAX_LEVEL = 4;
	public static final List<List<RegistryEntry<StatusEffect>>> EFFECTS_BY_LEVEL = List.of(
			List.of(StatusEffects.SPEED, StatusEffects.HASTE),
			List.of(StatusEffects.RESISTANCE, StatusEffects.JUMP_BOOST),
			List.of(StatusEffects.STRENGTH),
			List.of(StatusEffects.REGENERATION)
	);
	private static final Set<RegistryEntry<StatusEffect>>
			EFFECTS =
			EFFECTS_BY_LEVEL.stream().flatMap(Collection::stream).collect(Collectors.toSet());
	public static final int LEVEL_PROPERTY_INDEX = 0;
	public static final int PRIMARY_PROPERTY_INDEX = 1;
	public static final int SECONDARY_PROPERTY_INDEX = 2;
	public static final int PROPERTY_COUNT = 3;
	private static final int MAX_BEAM_BLOCKS_PER_TICK = 10;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.beacon");
	private static final String PRIMARY_EFFECT_NBT_KEY = "primary_effect";
	private static final String SECONDARY_EFFECT_NBT_KEY = "secondary_effect";
	List<BeamEmitter.BeamSegment> beamSegments = new ArrayList<>();
	private List<BeamEmitter.BeamSegment> pendingBeamSegments = new ArrayList<>();
	int level;
	private int minY;
	@Nullable RegistryEntry<StatusEffect> primary;
	@Nullable RegistryEntry<StatusEffect> secondary;
	private @Nullable Text customName;
	private ContainerLock lock = ContainerLock.EMPTY;
	private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		@Override
		public int get(int index) {
			return switch (index) {
				case LEVEL_PROPERTY_INDEX -> level;
				case PRIMARY_PROPERTY_INDEX -> BeaconScreenHandler.getRawIdForStatusEffect(primary);
				case SECONDARY_PROPERTY_INDEX -> BeaconScreenHandler.getRawIdForStatusEffect(secondary);
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case LEVEL_PROPERTY_INDEX:
					level = value;
					break;
				case PRIMARY_PROPERTY_INDEX:
					if (!world.isClient() && !beamSegments.isEmpty()) {
						BeaconBlockEntity.playSound(world, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT);
					}

					primary = BeaconBlockEntity.getEffectOrNull(BeaconScreenHandler.getStatusEffectForRawId(value));
					break;
				case SECONDARY_PROPERTY_INDEX:
					secondary = BeaconBlockEntity.getEffectOrNull(BeaconScreenHandler.getStatusEffectForRawId(value));
			}
		}

		@Override
		public int size() {
			return PROPERTY_COUNT;
		}
	};

	static @Nullable RegistryEntry<StatusEffect> getEffectOrNull(@Nullable RegistryEntry<StatusEffect> effect) {
		return EFFECTS.contains(effect) ? effect : null;
	}

	public BeaconBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BEACON, pos, state);
	}

	/**
	 * Тикает маяк: обновляет луч блок за блоком (до {@code MAX_BEAM_BLOCKS_PER_TICK} за тик),
	 * каждые 80 тиков пересчитывает уровень пирамиды и применяет эффекты к игрокам.
	 */
	public static void tick(World world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity) {
		int beaconX = pos.getX();
		int beaconY = pos.getY();
		int beaconZ = pos.getZ();

		BlockPos scanPos;
		if (blockEntity.minY < beaconY) {
			scanPos = pos;
			blockEntity.pendingBeamSegments = Lists.newArrayList();
			blockEntity.minY = pos.getY() - 1;
		} else {
			scanPos = new BlockPos(beaconX, blockEntity.minY + 1, beaconZ);
		}

		BeamEmitter.BeamSegment beamSegment = blockEntity.pendingBeamSegments.isEmpty()
				? null
				: blockEntity.pendingBeamSegments.get(blockEntity.pendingBeamSegments.size() - 1);
		int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, beaconX, beaconZ);

		for (int step = 0; step < MAX_BEAM_BLOCKS_PER_TICK && scanPos.getY() <= surfaceY; step++) {
			BlockState blockState = world.getBlockState(scanPos);

			if (blockState.getBlock() instanceof Stainable stainable) {
				int blockColor = stainable.getColor().getEntityColor();

				if (blockEntity.pendingBeamSegments.size() <= 1) {
					beamSegment = new BeamEmitter.BeamSegment(blockColor);
					blockEntity.pendingBeamSegments.add(beamSegment);
				} else if (beamSegment != null) {
					if (blockColor == beamSegment.getColor()) {
						beamSegment.increaseHeight();
					} else {
						beamSegment = new BeamEmitter.BeamSegment(ColorHelper.average(beamSegment.getColor(), blockColor));
						blockEntity.pendingBeamSegments.add(beamSegment);
					}
				}
			} else {
				if (beamSegment == null || blockState.getOpacity() >= 15 && !blockState.isOf(Blocks.BEDROCK)) {
					blockEntity.pendingBeamSegments.clear();
					blockEntity.minY = surfaceY;
					break;
				}

				beamSegment.increaseHeight();
			}

			scanPos = scanPos.up();
			blockEntity.minY++;
		}

		int prevLevel = blockEntity.level;

		if (world.getTime() % 80L == 0L) {
			if (!blockEntity.beamSegments.isEmpty()) {
				blockEntity.level = updateLevel(world, beaconX, beaconY, beaconZ);
			}

			if (blockEntity.level > 0 && !blockEntity.beamSegments.isEmpty()) {
				applyPlayerEffects(world, pos, blockEntity.level, blockEntity.primary, blockEntity.secondary);
				playSound(world, pos, SoundEvents.BLOCK_BEACON_AMBIENT);
			}
		}

		if (blockEntity.minY >= surfaceY) {
			blockEntity.minY = world.getBottomY() - 1;
			boolean wasActive = prevLevel > 0;
			blockEntity.beamSegments = blockEntity.pendingBeamSegments;

			if (!world.isClient()) {
				boolean isActive = blockEntity.level > 0;

				if (isActive && !wasActive) {
					playSound(world, pos, SoundEvents.BLOCK_BEACON_ACTIVATE);

					for (ServerPlayerEntity player : world.getNonSpectatingEntities(
							ServerPlayerEntity.class,
							new Box(beaconX, beaconY, beaconZ, beaconX, beaconY - 4, beaconZ).expand(10.0, 5.0, 10.0)
					)) {
						Criteria.CONSTRUCT_BEACON.trigger(player, blockEntity.level);
					}
				} else if (wasActive && !isActive) {
					playSound(world, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE);
				}
			}
		}
	}

	private static int updateLevel(World world, int x, int y, int z) {
		int level = 0;

		for (int tier = 1; tier <= MAX_LEVEL; level = tier++) {
			int layerY = y - tier;
			if (layerY < world.getBottomY()) {
				break;
			}

			boolean layerComplete = true;

			for (int lx = x - tier; lx <= x + tier && layerComplete; lx++) {
				for (int lz = z - tier; lz <= z + tier; lz++) {
					if (!world.getBlockState(new BlockPos(lx, layerY, lz)).isIn(BlockTags.BEACON_BASE_BLOCKS)) {
						layerComplete = false;
						break;
					}
				}
			}

			if (!layerComplete) {
				break;
			}
		}

		return level;
	}

	@Override
	public void markRemoved() {
		playSound(this.world, this.pos, SoundEvents.BLOCK_BEACON_DEACTIVATE);
		super.markRemoved();
	}

	private static void applyPlayerEffects(
			World world,
			BlockPos pos,
			int beaconLevel,
			@Nullable RegistryEntry<StatusEffect> primaryEffect,
			@Nullable RegistryEntry<StatusEffect> secondaryEffect
	) {
		if (world.isClient() || primaryEffect == null) {
			return;
		}

		double effectRange = beaconLevel * MAX_BEAM_BLOCKS_PER_TICK + MAX_BEAM_BLOCKS_PER_TICK;
		int amplifier = beaconLevel >= 4 && Objects.equals(primaryEffect, secondaryEffect) ? 1 : 0;
		int duration = (9 + beaconLevel * 2) * 20;
		Box box = new Box(pos).expand(effectRange).stretch(0.0, world.getHeight(), 0.0);
		List<PlayerEntity> players = world.getNonSpectatingEntities(PlayerEntity.class, box);

		for (PlayerEntity player : players) {
			player.addStatusEffect(new StatusEffectInstance(primaryEffect, duration, amplifier, true, true));
		}

		if (beaconLevel >= 4 && !Objects.equals(primaryEffect, secondaryEffect) && secondaryEffect != null) {
			for (PlayerEntity player : players) {
				player.addStatusEffect(new StatusEffectInstance(secondaryEffect, duration, 0, true, true));
			}
		}
	}

	public static void playSound(World world, BlockPos pos, SoundEvent sound) {
		world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
	}

	@Override
	public List<BeamEmitter.BeamSegment> getBeamSegments() {
		return level == 0 ? ImmutableList.of() : beamSegments;
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	private static void writeStatusEffect(WriteView view, String key, @Nullable RegistryEntry<StatusEffect> effect) {
		if (effect != null) {
			effect.getKey().ifPresent(entryKey -> view.putString(key, entryKey.getValue().toString()));
		}
	}

	private static @Nullable RegistryEntry<StatusEffect> readStatusEffect(ReadView view, String key) {
		return view
				.<RegistryEntry<StatusEffect>>read(key, Registries.STATUS_EFFECT.getEntryCodec())
				.filter(EFFECTS::contains)
				.orElse(null);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		primary = readStatusEffect(view, PRIMARY_EFFECT_NBT_KEY);
		secondary = readStatusEffect(view, SECONDARY_EFFECT_NBT_KEY);
		customName = tryParseCustomName(view, "CustomName");
		lock = ContainerLock.read(view);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		writeStatusEffect(view, PRIMARY_EFFECT_NBT_KEY, primary);
		writeStatusEffect(view, SECONDARY_EFFECT_NBT_KEY, secondary);
		view.putInt("Levels", level);
		view.putNullable("CustomName", TextCodecs.CODEC, customName);
		lock.write(view);
	}

	public void setCustomName(@Nullable Text customName) {
		this.customName = customName;
	}

	@Override
	public @Nullable Text getCustomName() {
		return customName;
	}

	@Override
	public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		if (lock.checkUnlocked(player)) {
			return new BeaconScreenHandler(
					syncId,
					playerInventory,
					propertyDelegate,
					ScreenHandlerContext.create(world, getPos())
			);
		}

		LockableContainerBlockEntity.handleLocked(getPos().toCenterPos(), player, getDisplayName());
		return null;
	}

	@Override
	public Text getDisplayName() {
		return getName();
	}

	@Override
	public Text getName() {
		return customName != null ? customName : CONTAINER_NAME_TEXT;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		customName = components.get(DataComponentTypes.CUSTOM_NAME);
		lock = components.getOrDefault(DataComponentTypes.LOCK, ContainerLock.EMPTY);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.CUSTOM_NAME, customName);
		if (!lock.equals(ContainerLock.EMPTY)) {
			builder.add(DataComponentTypes.LOCK, lock);
		}
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("CustomName");
		view.remove("lock");
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		minY = world.getBottomY() - 1;
	}
}
