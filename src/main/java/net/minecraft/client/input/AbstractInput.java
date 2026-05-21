package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Environment(EnvType.CLIENT)
/**
 * {@code AbstractInput}.
 */
public interface AbstractInput {

	int NOT_A_NUMBER = -1;

	@InputUtil.Keycode
	int getKeycode();

	@AbstractInput.Modifier
	int modifiers();

	default boolean isEnterOrSpace() {
		return this.getKeycode() == 257 || this.getKeycode() == 32 || this.getKeycode() == 335;
	}

	default boolean isEnter() {
		return this.getKeycode() == 257 || this.getKeycode() == 335;
	}

	default boolean isEscape() {
		return this.getKeycode() == 256;
	}

	default boolean isLeft() {
		return this.getKeycode() == 263;
	}

	default boolean isRight() {
		return this.getKeycode() == 262;
	}

	default boolean isUp() {
		return this.getKeycode() == 265;
	}

	default boolean isDown() {
		return this.getKeycode() == 264;
	}

	default boolean isTab() {
		return this.getKeycode() == 258;
	}

	default int asNumber() {
		int i = this.getKeycode() - 48;
		return i >= 0 && i <= 9 ? i : -1;
	}

	default boolean hasAlt() {
		return (this.modifiers() & 4) != 0;
	}

	default boolean hasShift() {
		return (this.modifiers() & 1) != 0;
	}

	default boolean hasCtrl() {
		return (this.modifiers() & 2) != 0;
	}

	default boolean hasCtrlOrCmd() {
		return (this.modifiers() & SystemKeycodes.CTRL_MOD) != 0;
	}

	default boolean isSelectAll() {
		return this.getKeycode() == 65 && this.hasCtrlOrCmd() && !this.hasShift() && !this.hasAlt();
	}

	default boolean isCopy() {
		return this.getKeycode() == 67 && this.hasCtrlOrCmd() && !this.hasShift() && !this.hasAlt();
	}

	default boolean isPaste() {
		return this.getKeycode() == 86 && this.hasCtrlOrCmd() && !this.hasShift() && !this.hasAlt();
	}

	default boolean isCut() {
		return this.getKeycode() == 88 && this.hasCtrlOrCmd() && !this.hasShift() && !this.hasAlt();
	}

	@Retention(RetentionPolicy.CLASS)
	@Target(
			{
					ElementType.FIELD,
					ElementType.PARAMETER,
					ElementType.LOCAL_VARIABLE,
					ElementType.METHOD,
					ElementType.TYPE_USE
			}
	)
	@Environment(EnvType.CLIENT)
	/**
	 * {@code Modifier}.
	 */
	public @interface Modifier {
	}
}
