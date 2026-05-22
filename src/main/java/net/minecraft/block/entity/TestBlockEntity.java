package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TestBlock;
import net.minecraft.block.enums.TestBlockMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Блок-сущность тестового блока ({@code test_block}), используемого в игровых тестах.
 * Хранит режим работы, сообщение и состояние активации.
 */
public class TestBlockEntity extends BlockEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private TestBlockMode mode;
	private String message = "";
	private boolean powered = false;
	private boolean triggered;

	public TestBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.TEST_BLOCK, pos, state);
		mode = state.get(TestBlock.MODE);
	}

	@Override
	protected void writeData(WriteView view) {
		view.put("mode", TestBlockMode.CODEC, mode);
		view.putString("message", message);
		view.putBoolean("powered", powered);
	}

	@Override
	protected void readData(ReadView view) {
		mode = view.<TestBlockMode>read("mode", TestBlockMode.CODEC).orElse(TestBlockMode.FAIL);
		message = view.getString("message", "");
		powered = view.getBoolean("powered", false);
	}

	private void update() {
		if (world == null) {
			return;
		}

		BlockPos blockPos = getPos();
		BlockState blockState = world.getBlockState(blockPos);

		if (blockState.isOf(Blocks.TEST_BLOCK)) {
			world.setBlockState(blockPos, blockState.with(TestBlock.MODE, mode), 2);
		}
	}

	@Override
	public @Nullable BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	public boolean isPowered() {
		return powered;
	}

	public void setPowered(boolean powered) {
		this.powered = powered;
	}

	public TestBlockMode getMode() {
		return mode;
	}

	public void setMode(TestBlockMode mode) {
		this.mode = mode;
		update();
	}

	private Block getBlock() {
		return getCachedState().getBlock();
	}

	public void reset() {
		triggered = false;

		if (mode == TestBlockMode.START && world != null) {
			setPowered(false);
			world.updateNeighbors(getPos(), getBlock());
		}
	}

	public void trigger() {
		if (mode == TestBlockMode.START && world != null) {
			setPowered(true);
			BlockPos blockPos = getPos();
			world.updateNeighbors(blockPos, getBlock());
			world.getBlockTickScheduler().isTicking(blockPos, getBlock());
			logMessage();
			return;
		}

		if (mode == TestBlockMode.LOG) {
			logMessage();
		}

		triggered = true;
	}

	public void logMessage() {
		if (!message.isBlank()) {
			LOGGER.info("Test {} (at {}): {}", mode.asString(), getPos(), message);
		}
	}

	public boolean hasTriggered() {
		return triggered;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
