package net.minecraft.text.object;

import com.mojang.serialization.MapCodec;
import net.minecraft.text.StyleSpriteSource;

/**
 * Содержимое встроенного объекта в текстовом компоненте.
 *
 * <p>Определяет два контракта:
 * <ul>
 *   <li>{@link #spriteSource()} — источник спрайта для рендерера (шрифт, скин игрока, атлас);</li>
 *   <li>{@link #asText()} — человекочитаемое строковое представление для текстового обхода.</li>
 * </ul>
 * Реализации: {@link AtlasTextObjectContents} (спрайт из атласа) и
 * {@link PlayerTextObjectContents} (голова игрока).</p>
 */
public interface TextObjectContents {

	/**
	 * Возвращает источник спрайта, используемый рендерером для отображения объекта.
	 *
	 * @return источник спрайта ({@link StyleSpriteSource.Sprite}, {@link StyleSpriteSource.Player} и т.д.)
	 */
	StyleSpriteSource spriteSource();

	/**
	 * Возвращает человекочитаемое строковое представление объекта.
	 * Используется при текстовом обходе (например, для копирования в буфер обмена).
	 *
	 * @return строковое представление объекта
	 */
	String asText();

	/** @return codec для сериализации конкретной реализации */
	MapCodec<? extends TextObjectContents> getCodec();
}
