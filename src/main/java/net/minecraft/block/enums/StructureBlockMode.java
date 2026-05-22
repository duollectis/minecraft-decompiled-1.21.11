package net.minecraft.block.enums;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

/**
 * Режим работы блока структуры (Structure Block).
 * Определяет, в каком режиме находится блок: сохранение, загрузка, угловая метка или хранение данных.
 */
public enum StructureBlockMode implements StringIdentifiable {

	/** Режим сохранения структуры в файл. */
	SAVE("save"),
	/** Режим загрузки структуры из файла. */
	LOAD("load"),
	/** Угловая метка для определения границ структуры. */
	CORNER("corner"),
	/** Режим хранения произвольных данных (data tag). */
	DATA("data");

	@Deprecated
	public static final Codec<StructureBlockMode> CODEC = Codecs.enumByName(StructureBlockMode::valueOf);

	private final String name;
	private final Text text;

	StructureBlockMode(final String name) {
		this.name = name;
		this.text = Text.translatable("structure_block.mode_info." + name);
	}

	@Override
	public String asString() {
		return name;
	}

	/**
	 * Возвращает локализованный текст для отображения в интерфейсе блока структуры.
	 *
	 * @return переведённый текст текущего режима
	 */
	public Text asText() {
		return text;
	}
}
