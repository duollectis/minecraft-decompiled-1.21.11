package net.minecraft.block.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.jukebox.JukeboxManager;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

/**
 * Блок-сущность музыкального ящика. Управляет воспроизведением пластинок через {@link JukeboxManager},
 * синхронизирует состояние блока и выбрасывает пластинку при разрушении.
 */
public class JukeboxBlockEntity extends BlockEntity implements SingleStackInventory.SingleStackBlockEntityInventory {

	public static final String RECORD_ITEM_NBT_KEY = "RecordItem";
	public static final String TICKS_SINCE_SONG_STARTED_NBT_KEY = "ticks_since_song_started";
	private ItemStack recordStack = ItemStack.EMPTY;
	private final JukeboxManager manager = new JukeboxManager(this::onManagerChange, this.getPos());

	public JukeboxBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.JUKEBOX, pos, state);
	}

	public JukeboxManager getManager() {
		return manager;
	}

	public void onManagerChange() {
		world.updateNeighbors(getPos(), getCachedState().getBlock());
		markDirty();
	}

	private void onRecordStackChanged(boolean hasRecord) {
		if (world == null || world.getBlockState(getPos()) != getCachedState()) {
			return;
		}

		world.setBlockState(getPos(), getCachedState().with(JukeboxBlock.HAS_RECORD, hasRecord), 2);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, getPos(), GameEvent.Emitter.of(getCachedState()));
	}

	public void dropRecord() {
		if (world == null || world.isClient()) {
			return;
		}

		ItemStack record = getStack();
		if (record.isEmpty()) {
			return;
		}

		emptyStack();
		Vec3d spawnPos = Vec3d.add(getPos(), 0.5, 1.01, 0.5).addHorizontalRandom(world.random, 0.7F);
		ItemEntity itemEntity = new ItemEntity(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), record.copy());
		itemEntity.setToDefaultPickupDelay();
		world.spawnEntity(itemEntity);
		onManagerChange();
	}

	public static void tick(World world, BlockPos pos, BlockState state, JukeboxBlockEntity blockEntity) {
		blockEntity.manager.tick(world, state);
	}

	public int getComparatorOutput() {
		return JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), recordStack)
				.map(RegistryEntry::value)
				.map(JukeboxSong::comparatorOutput)
				.orElse(0);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		ItemStack newRecord = view.<ItemStack>read("RecordItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		if (!recordStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(newRecord, recordStack)) {
			manager.stopPlaying(world, getCachedState());
		}

		recordStack = newRecord;
		view.getOptionalLong("ticks_since_song_started").ifPresent(
				ticksSinceSongStarted -> JukeboxSong.getSongEntryFromStack(view.getRegistries(), recordStack)
						.ifPresent(song -> manager.setValues((RegistryEntry<JukeboxSong>) song, ticksSinceSongStarted))
		);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!getStack().isEmpty()) {
			view.put("RecordItem", ItemStack.CODEC, getStack());
		}

		if (manager.getSong() != null) {
			view.putLong("ticks_since_song_started", manager.getTicksSinceSongStarted());
		}
	}

	@Override
	public ItemStack getStack() {
		return recordStack;
	}

	@Override
	public ItemStack decreaseStack(int count) {
		ItemStack record = recordStack;
		setStack(ItemStack.EMPTY);
		return record;
	}

	@Override
	public void setStack(ItemStack stack) {
		recordStack = stack;
		boolean hasRecord = !recordStack.isEmpty();
		Optional<RegistryEntry<JukeboxSong>> songEntry =
				JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), recordStack);

		onRecordStackChanged(hasRecord);

		if (hasRecord && songEntry.isPresent()) {
			manager.startPlaying(world, songEntry.get());
		} else {
			manager.stopPlaying(world, getCachedState());
		}
	}

	@Override
	public void markRemoved() {
		super.markRemoved();
		world.emitGameEvent(GameEvent.JUKEBOX_STOP_PLAY, getPos(), GameEvent.Emitter.of(getCachedState()));
		world.syncWorldEvent(1011, getPos(), 0);
	}

	@Override
	public int getMaxCountPerStack() {
		return 1;
	}

	@Override
	public BlockEntity asBlockEntity() {
		return this;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE) && getStack(slot).isEmpty();
	}

	@Override
	public boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
		return hopperInventory.containsAny(ItemStack::isEmpty);
	}

	@Override
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		dropRecord();
	}

	@VisibleForTesting
	public void setDisc(ItemStack stack) {
		recordStack = stack;
		JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), stack)
				.ifPresent(song -> manager.setValues((RegistryEntry<JukeboxSong>) song, 0L));
		world.updateNeighbors(getPos(), getCachedState().getBlock());
		markDirty();
	}

	@VisibleForTesting
	public void reloadDisc() {
		JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), getStack())
				.ifPresent(song -> manager.startPlaying(world, (RegistryEntry<JukeboxSong>) song));
	}
}
