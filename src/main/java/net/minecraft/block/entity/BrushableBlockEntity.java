package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BrushableBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Блок-сущность раскапываемого блока (подозрительный песок/гравий).
 * Управляет прогрессом раскопки кистью, генерацией лута из таблицы и спавном предмета.
 */
public class BrushableBlockEntity extends BlockEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String LOOT_TABLE_NBT_KEY = "LootTable";
	private static final String LOOT_TABLE_SEED_NBT_KEY = "LootTableSeed";
	private static final String HIT_DIRECTION_NBT_KEY = "hit_direction";
	private static final String ITEM_NBT_KEY = "item";
	private static final int BRUSH_COOLDOWN_TICKS = 10;
	private static final int DUST_COOLDOWN_TICKS = 40;
	private static final int BRUSHES_TO_COMPLETE = 10;
	private int brushesCount;
	private long nextDustTime;
	private long nextBrushTime;
	private ItemStack item = ItemStack.EMPTY;
	private @Nullable Direction hitDirection;
	private @Nullable RegistryKey<LootTable> lootTable;
	private long lootTableSeed;

	public BrushableBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BRUSHABLE_BLOCK, pos, state);
	}

	public boolean brush(
			long worldTime,
			ServerWorld world,
			LivingEntity brusher,
			Direction hitDirection,
			ItemStack brush
	) {
		if (this.hitDirection == null) {
			this.hitDirection = hitDirection;
		}

		nextDustTime = worldTime + DUST_COOLDOWN_TICKS;

		if (worldTime < nextBrushTime) {
			return false;
		}

		nextBrushTime = worldTime + BRUSH_COOLDOWN_TICKS;
		generateItem(world, brusher, brush);
		int prevDustLevel = getDustedLevel();

		if (++brushesCount >= BRUSHES_TO_COMPLETE) {
			finishBrushing(world, brusher, brush);
			return true;
		}

		world.scheduleBlockTick(getPos(), getCachedState().getBlock(), 2);
		int newDustLevel = getDustedLevel();

		if (prevDustLevel != newDustLevel) {
			world.setBlockState(getPos(), getCachedState().with(Properties.DUSTED, newDustLevel), 3);
		}

		return false;
	}

	private void generateItem(ServerWorld world, LivingEntity brusher, ItemStack brush) {
		if (lootTable == null) {
			return;
		}

		LootTable table = world.getServer().getReloadableRegistries().getLootTable(lootTable);

		if (brusher instanceof ServerPlayerEntity player) {
			Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger(player, lootTable);
		}

		LootWorldContext lootContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
				.luck(brusher.getLuck())
				.add(LootContextParameters.THIS_ENTITY, brusher)
				.add(LootContextParameters.TOOL, brush)
				.build(LootContextTypes.ARCHAEOLOGY);
		ObjectArrayList<ItemStack> loot = table.generateLoot(lootContext, lootTableSeed);

		item = switch (loot.size()) {
			case 0 -> ItemStack.EMPTY;
			case 1 -> loot.getFirst();
			default -> {
				LOGGER.warn("Expected max 1 loot from loot table {}, but got {}", lootTable.getValue(), loot.size());
				yield loot.getFirst();
			}
		};
		lootTable = null;
		markDirty();
	}

	private void finishBrushing(ServerWorld world, LivingEntity brusher, ItemStack brush) {
		spawnItem(world, brusher, brush);
		BlockState currentState = getCachedState();
		world.syncWorldEvent(3008, getPos(), Block.getRawIdFromState(currentState));
		Block baseBlock = currentState.getBlock() instanceof BrushableBlock brushableBlock
				? brushableBlock.getBaseBlock()
				: Blocks.AIR;
		world.setBlockState(pos, baseBlock.getDefaultState(), 3);
	}

	private void spawnItem(ServerWorld world, LivingEntity brusher, ItemStack brush) {
		generateItem(world, brusher, brush);

		if (item.isEmpty()) {
			return;
		}

		double itemWidth = EntityType.ITEM.getWidth();
		double freeSpace = 1.0 - itemWidth;
		double halfWidth = itemWidth / 2.0;
		Direction spawnDir = Objects.requireNonNullElse(hitDirection, Direction.UP);
		BlockPos spawnPos = pos.offset(spawnDir, 1);
		double spawnX = spawnPos.getX() + 0.5 * freeSpace + halfWidth;
		double spawnY = spawnPos.getY() + 0.5 + EntityType.ITEM.getHeight() / 2.0F;
		double spawnZ = spawnPos.getZ() + 0.5 * freeSpace + halfWidth;
		ItemEntity itemEntity = new ItemEntity(world, spawnX, spawnY, spawnZ, item.split(world.random.nextInt(21) + 10));
		itemEntity.setVelocity(Vec3d.ZERO);
		world.spawnEntity(itemEntity);
		item = ItemStack.EMPTY;
	}

	private static final int DUST_DECAY_RATE = 2;
	private static final int DUST_RESCHEDULE_TICKS = 4;

	public void scheduledTick(ServerWorld world) {
		if (brushesCount != 0 && world.getTime() >= nextDustTime) {
			int prevDustLevel = getDustedLevel();
			brushesCount = Math.max(0, brushesCount - DUST_DECAY_RATE);
			int newDustLevel = getDustedLevel();

			if (prevDustLevel != newDustLevel) {
				world.setBlockState(getPos(), getCachedState().with(Properties.DUSTED, newDustLevel), 3);
			}

			nextDustTime = world.getTime() + DUST_RESCHEDULE_TICKS;
		}

		if (brushesCount == 0) {
			hitDirection = null;
			nextDustTime = 0L;
			nextBrushTime = 0L;
		} else {
			world.scheduleBlockTick(getPos(), getCachedState().getBlock(), 2);
		}
	}

	private boolean readLootTableFromData(ReadView view) {
		lootTable = view.<RegistryKey<LootTable>>read(LOOT_TABLE_NBT_KEY, LootTable.TABLE_KEY).orElse(null);
		lootTableSeed = view.getLong(LOOT_TABLE_SEED_NBT_KEY, 0L);
		return lootTable != null;
	}

	private boolean writeLootTableToData(WriteView view) {
		if (lootTable == null) {
			return false;
		}

		view.put(LOOT_TABLE_NBT_KEY, LootTable.TABLE_KEY, lootTable);

		if (lootTableSeed != 0L) {
			view.putLong(LOOT_TABLE_SEED_NBT_KEY, lootTableSeed);
		}

		return true;
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound nbt = super.toInitialChunkDataNbt(registries);
		nbt.putNullable(HIT_DIRECTION_NBT_KEY, Direction.INDEX_CODEC, hitDirection);

		if (!item.isEmpty()) {
			RegistryOps<NbtElement> registryOps = registries.getOps(NbtOps.INSTANCE);
			nbt.put(ITEM_NBT_KEY, ItemStack.CODEC, registryOps, item);
		}

		return nbt;
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		item = readLootTableFromData(view)
				? ItemStack.EMPTY
				: view.<ItemStack>read(ITEM_NBT_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
		hitDirection = view.<Direction>read(HIT_DIRECTION_NBT_KEY, Direction.INDEX_CODEC).orElse(null);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		if (!writeLootTableToData(view) && !item.isEmpty()) {
			view.put(ITEM_NBT_KEY, ItemStack.CODEC, item);
		}
	}

	public void setLootTable(RegistryKey<LootTable> lootTable, long seed) {
		this.lootTable = lootTable;
		lootTableSeed = seed;
	}

	private int getDustedLevel() {
		if (brushesCount == 0) {
			return 0;
		}

		if (brushesCount < 3) {
			return 1;
		}

		return brushesCount < 6 ? 2 : 3;
	}

	public @Nullable Direction getHitDirection() {
		return hitDirection;
	}

	public ItemStack getItem() {
		return item;
	}
}
