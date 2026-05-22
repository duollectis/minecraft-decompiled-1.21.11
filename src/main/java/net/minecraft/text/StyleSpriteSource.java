package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;

/**
 * Источник спрайта для стиля текстового компонента.
 *
 * <p>Определяет, откуда рендерер берёт глиф для отображения символа.
 * Три реализации:
 * <ul>
 *   <li>{@link Font} — стандартный шрифт по идентификатору атласа;</li>
 *   <li>{@link Player} — скин игрока (голова);</li>
 *   <li>{@link Sprite} — конкретный спрайт из атласа текстур.</li>
 * </ul>
 * Codec {@link #FONT_CODEC} сериализует только {@link Font} через {@link Identifier},
 * остальные типы передаются только по сети через отдельные механизмы.</p>
 */
public interface StyleSpriteSource {

	/**
	 * Codec для сериализации источника шрифта как {@link Identifier}.
	 * Поддерживает только {@link Font}; другие реализации вернут ошибку при кодировании.
	 */
	Codec<StyleSpriteSource> FONT_CODEC = Identifier.CODEC
			.flatComapMap(
					StyleSpriteSource.Font::new,
					instance -> instance instanceof StyleSpriteSource.Font font
					            ? DataResult.success(font.id())
					            : DataResult.error(() -> "Unsupported font description type: " + instance)
			);

	/** Шрифт по умолчанию — стандартный шрифт Minecraft. */
	StyleSpriteSource.Font DEFAULT = new StyleSpriteSource.Font(Identifier.ofVanilla("default"));

	/**
	 * Стандартный шрифт, идентифицируемый по {@link Identifier} атласа.
	 *
	 * @param id идентификатор атласа шрифта (например, {@code minecraft:default})
	 */
	record Font(Identifier id) implements StyleSpriteSource {
	}

	/**
	 * Источник спрайта на основе скина игрока.
	 *
	 * @param profile профиль игрока, содержащий UUID и текстуры
	 * @param hat {@code true} — использовать слой шляпы (второй слой скина)
	 */
	record Player(ProfileComponent profile, boolean hat) implements StyleSpriteSource {
	}

	/**
	 * Конкретный спрайт из атласа текстур.
	 *
	 * @param atlasId идентификатор атласа текстур
	 * @param spriteId идентификатор спрайта внутри атласа
	 */
	record Sprite(Identifier atlasId, Identifier spriteId) implements StyleSpriteSource {
	}
}
