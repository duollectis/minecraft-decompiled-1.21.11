package net.minecraft.client.option;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

@Environment(EnvType.CLIENT)
/**
 * {@code NarratorMode}.
 */
public enum NarratorMode {
	OFF(0, "options.narrator.off"),
	ALL(1, "options.narrator.all"),
	CHAT(2, "options.narrator.chat"),
	SYSTEM(3, "options.narrator.system");

	private static final IntFunction<NarratorMode> BY_ID = ValueLists.createIndexToValueFunction(
			(NarratorMode mode) -> mode.getId(), values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final Codec<NarratorMode> CODEC = Codec.INT.xmap(NarratorMode::byId, NarratorMode::getId);
	private final int id;
	private final Text name;

	private NarratorMode(final int id, final String name) {
		this.id = id;
		this.name = Text.translatable(name);
	}

	public int getId() {
		return this.id;
	}

	public Text getName() {
		return this.name;
	}

	/**
	 * By id.
	 *
	 * @param id id
	 *
	 * @return NarratorMode — результат операции
	 */
	public static NarratorMode byId(int id) {
		return BY_ID.apply(id);
	}

	/**
	 * Определяет, следует ли narrate chat.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldNarrateChat() {
		return this == ALL || this == CHAT;
	}

	/**
	 * Определяет, следует ли narrate system.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldNarrateSystem() {
		return this == ALL || this == SYSTEM;
	}

	/**
	 * Определяет, следует ли narrate.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldNarrate() {
		return this == ALL || this == SYSTEM || this == CHAT;
	}
}
