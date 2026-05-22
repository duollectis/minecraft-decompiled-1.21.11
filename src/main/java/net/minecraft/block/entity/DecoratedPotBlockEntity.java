package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Блок-сущность декорированного горшка. Хранит черепки на четырёх гранях и один предмет внутри.
 * Поддерживает лут-таблицы и анимацию покачивания при взаимодействии.
 */
public class DecoratedPotBlockEntity extends BlockEntity implements LootableInventory, SingleStackInventory.SingleStackBlockEntityInventory {

	public static final String SHERDS_NBT_KEY = "sherds";
	public static final String ITEM_NBT_KEY = "item";
	public static final int INVENTORY_SIZE = 1;
	public long lastWobbleTime;
	public DecoratedPotBlockEntity.@Nullable WobbleType lastWobbleType;
	private Sherds sherds;
	private ItemStack stack = ItemStack.EMPTY;
	protected @Nullable RegistryKey<LootTable> lootTableId;
	protected long lootTableSeed;

	public DecoratedPotBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.DECORATED_POT, pos, state);
		sherds = Sherds.DEFAULT;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!sherds.equals(Sherds.DEFAULT)) {
			view.put("sherds", Sherds.CODEC, sherds);
		}

		if (!writeLootTable(view) && !stack.isEmpty()) {
			view.put("item", ItemStack.CODEC, stack);
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		sherds = view.<Sherds>read("sherds", Sherds.CODEC).orElse(Sherds.DEFAULT);
		if (readLootTable(view)) {
			stack = ItemStack.EMPTY;
		} else {
			stack = view.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		}
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	public Direction getHorizontalFacing() {
		return getCachedState().get(Properties.HORIZONTAL_FACING);
	}

	public Sherds getSherds() {
		return sherds;
	}

	public static ItemStack getStackWith(Sherds sherds) {
		ItemStack itemStack = Items.DECORATED_POT.getDefaultStack();
		itemStack.set(DataComponentTypes.POT_DECORATIONS, sherds);
		return itemStack;
	}

	@Override
	public @Nullable RegistryKey<LootTable> getLootTable() {
		return lootTableId;
	}

	@Override
	public void setLootTable(@Nullable RegistryKey<LootTable> lootTable) {
		lootTableId = lootTable;
	}

	@Override
	public long getLootTableSeed() {
		return lootTableSeed;
	}

	@Override
	public void setLootTableSeed(long seed) {
		lootTableSeed = seed;
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.POT_DECORATIONS, sherds);
		builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(List.of(stack)));
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		sherds = components.getOrDefault(DataComponentTypes.POT_DECORATIONS, Sherds.DEFAULT);
		stack = components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyFirstStack();
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		super.removeFromCopiedStackData(view);
		view.remove("sherds");
		view.remove("item");
	}

	@Override
	public ItemStack getStack() {
		generateLoot(null);
		return stack;
	}

	@Override
	public ItemStack decreaseStack(int count) {
		generateLoot(null);
		ItemStack split = stack.split(count);
		if (stack.isEmpty()) {
			stack = ItemStack.EMPTY;
		}

		return split;
	}

	@Override
	public void setStack(ItemStack stack) {
		generateLoot(null);
		this.stack = stack;
	}

	@Override
	public BlockEntity asBlockEntity() {
		return this;
	}

	/** Запускает анимацию покачивания горшка на клиенте через синхронизированное событие блока. */
	public void wobble(DecoratedPotBlockEntity.WobbleType wobbleType) {
		if (world == null || world.isClient()) {
			return;
		}

		world.addSyncedBlockEvent(getPos(), getCachedState().getBlock(), 1, wobbleType.ordinal());
	}

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		if (world == null || type != 1 || data < 0 || data >= WobbleType.values().length) {
			return super.onSyncedBlockEvent(type, data);
		}

		lastWobbleTime = world.getTime();
		lastWobbleType = WobbleType.values()[data];
		return true;
	}

	/** Тип анимации покачивания горшка: позитивный (короткий) или негативный (длинный). */
	public enum WobbleType {
		POSITIVE(7),
		NEGATIVE(10);

		public final int lengthInTicks;

		private WobbleType(final int lengthInTicks) {
			this.lengthInTicks = lengthInTicks;
		}
	}
}
