package net.minecraft.client.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;

import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
/**
 * {@code StickyKeyBinding}.
 */
public class StickyKeyBinding extends KeyBinding {

	private final BooleanSupplier toggleGetter;
	private boolean resetOnScreenClose;
	private final boolean restore;

	public StickyKeyBinding(
			String id,
			int code,
			KeyBinding.Category category,
			BooleanSupplier toggleGetter,
			boolean restore
	) {
		this(id, InputUtil.Type.KEYSYM, code, category, toggleGetter, restore);
	}

	public StickyKeyBinding(
			String id,
			InputUtil.Type type,
			int code,
			KeyBinding.Category category,
			BooleanSupplier toggleGetter,
			boolean restore
	) {
		super(id, type, code, category);
		this.toggleGetter = toggleGetter;
		this.restore = restore;
	}

	@Override
	protected boolean shouldSetOnGameFocus() {
		return super.shouldSetOnGameFocus() && !this.toggleGetter.getAsBoolean();
	}

	@Override
	public void setPressed(boolean pressed) {
		if (this.toggleGetter.getAsBoolean()) {
			if (pressed) {
				super.setPressed(!this.isPressed());
			}
		}
		else {
			super.setPressed(pressed);
		}
	}

	@Override
	protected void reset() {
		if (this.toggleGetter.getAsBoolean() && this.isPressed() || this.resetOnScreenClose) {
			this.resetOnScreenClose = true;
		}

		this.untoggle();
	}

	/**
	 * Определяет, следует ли restore on screen close.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldRestoreOnScreenClose() {
		boolean
				bl =
				this.restore && this.toggleGetter.getAsBoolean() && this.boundKey.getCategory() == InputUtil.Type.KEYSYM
						&& this.resetOnScreenClose;
		this.resetOnScreenClose = false;
		return bl;
	}

	/**
	 * Untoggle.
	 */
	protected void untoggle() {
		super.setPressed(false);
	}
}
