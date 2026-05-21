package net.minecraft.client.option;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

@Environment(EnvType.CLIENT)
/**
 * {@code AttackIndicator}.
 */
public enum AttackIndicator {
	OFF(0, "options.off"),
	CROSSHAIR(1, "options.attack.crosshair"),
	HOTBAR(2, "options.attack.hotbar");

	private static final IntFunction<AttackIndicator> BY_ID = ValueLists.createIndexToValueFunction(
			(AttackIndicator attackIndicator) -> attackIndicator.id, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final Codec<AttackIndicator>
			CODEC =
			Codec.INT.xmap(BY_ID::apply, attackIndicator -> attackIndicator.id);
	private final int id;
	private final Text text;

	private AttackIndicator(final int id, final String translationKey) {
		this.id = id;
		this.text = Text.translatable(translationKey);
	}

	public Text getText() {
		return this.text;
	}
}
