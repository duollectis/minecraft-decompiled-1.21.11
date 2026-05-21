package net.minecraft.block.dispenser;

import net.minecraft.util.math.BlockPointer;

/**
 * {@code FallibleItemDispenserBehavior}.
 */
public abstract class FallibleItemDispenserBehavior extends ItemDispenserBehavior {

	private boolean success = true;

	public boolean isSuccess() {
		return this.success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	protected void playSound(BlockPointer pointer) {
		pointer.world().syncWorldEvent(this.isSuccess() ? 1000 : 1001, pointer.pos(), 0);
	}
}
