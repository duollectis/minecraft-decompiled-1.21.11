package net.minecraft.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.dialog.input.*;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * {@code InputControlTypes}.
 */
public class InputControlTypes {

	public static MapCodec<? extends InputControl> registerAndGetDefault(Registry<MapCodec<? extends InputControl>> registry) {
		Registry.register(registry, Identifier.ofVanilla("boolean"), BooleanInputControl.CODEC);
		Registry.register(registry, Identifier.ofVanilla("number_range"), NumberRangeInputControl.CODEC);
		Registry.register(registry, Identifier.ofVanilla("single_option"), SingleOptionInputControl.CODEC);
		return Registry.register(registry, Identifier.ofVanilla("text"), TextInputControl.CODEC);
	}
}
