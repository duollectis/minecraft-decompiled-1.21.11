package net.minecraft.block.dispenser;

import net.minecraft.util.math.BlockPointer;

/**
 * Расширение {@link ItemDispenserBehavior} для операций, которые могут завершиться неудачей.
 * Флаг {@code success} управляет звуком: успех → стандартный звук диспенсера,
 * неудача → звук «клика» (код 1001).
 */
public abstract class FallibleItemDispenserBehavior extends ItemDispenserBehavior {

	/** Код мирового события «диспенсер не сработал» (звук клика). */
	private static final int DISPENSE_FAIL_SOUND_EVENT = 1001;

	/** Код мирового события «диспенсер сработал» (звук). */
	private static final int DISPENSE_SUCCESS_SOUND_EVENT = 1000;

	private boolean success = true;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	protected void playSound(BlockPointer pointer) {
		pointer.world().syncWorldEvent(
				success ? DISPENSE_SUCCESS_SOUND_EVENT : DISPENSE_FAIL_SOUND_EVENT,
				pointer.pos(),
				0
		);
	}
}
