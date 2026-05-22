package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

/**
 * Синглтон-генератор псевдослучайных фраз на шрифте «alt» для стола зачарований.
 * Фразы составляются из случайного набора слов и обрезаются по заданной ширине.
 */
@Environment(EnvType.CLIENT)
public class EnchantingPhrases {

	private static final StyleSpriteSource FONT_ID = new StyleSpriteSource.Font(Identifier.ofVanilla("alt"));
	private static final Style STYLE = Style.EMPTY.withFont(FONT_ID);
	private static final EnchantingPhrases INSTANCE = new EnchantingPhrases();
	private static final int MIN_WORDS = 3;
	private static final int MAX_EXTRA_WORDS = 2;

	private final Random random = Random.create();
	private final String[] phrases = new String[]{
		"the", "elder", "scrolls", "klaatu", "berata", "niktu", "xyzzy",
		"bless", "curse", "light", "darkness", "fire", "air", "earth", "water",
		"hot", "dry", "cold", "wet", "ignite", "snuff", "embiggen", "twist",
		"shorten", "stretch", "fiddle", "destroy", "imbue", "galvanize", "enchant",
		"free", "limited", "range", "of", "towards", "inside", "sphere", "cube",
		"self", "other", "ball", "mental", "physical", "grow", "shrink", "demon",
		"elemental", "spirit", "animal", "creature", "beast", "humanoid", "undead",
		"fresh", "stale", "phnglui", "mglwnafh", "cthulhu", "rlyeh", "wgahnagl",
		"fhtagn", "baguette"
	};

	private EnchantingPhrases() {
	}

	public static EnchantingPhrases getInstance() {
		return INSTANCE;
	}

	/**
	 * Генерирует случайную фразу из 3–4 слов на шрифте «alt», обрезанную по заданной ширине.
	 *
	 * @param textRenderer рендерер для измерения ширины текста
	 * @param width максимальная ширина в пикселях
	 * @return обрезанная фраза в стиле шрифта зачарований
	 */
	public StringVisitable generatePhrase(TextRenderer textRenderer, int width) {
		StringBuilder builder = new StringBuilder();
		int wordCount = random.nextInt(MAX_EXTRA_WORDS) + MIN_WORDS;

		for (int wordIndex = 0; wordIndex < wordCount; wordIndex++) {
			if (wordIndex != 0) {
				builder.append(" ");
			}

			builder.append(Util.getRandom(phrases, random));
		}

		return textRenderer
			.getTextHandler()
			.trimToWidth(Text.literal(builder.toString()).fillStyle(STYLE), width, Style.EMPTY);
	}

	public void setSeed(long seed) {
		random.setSeed(seed);
	}
}
