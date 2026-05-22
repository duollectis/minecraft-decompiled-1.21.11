package net.minecraft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Параметры генератора мира: сид, флаги структур и бонусного сундука.
 * Иммутабельный объект — все «мутирующие» методы возвращают новый экземпляр.
 */
public class GeneratorOptions {

	public static final MapCodec<GeneratorOptions> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.LONG.fieldOf("seed").stable().forGetter(GeneratorOptions::getSeed),
			Codec.BOOL
				.fieldOf("generate_features")
				.orElse(true)
				.stable()
				.forGetter(GeneratorOptions::shouldGenerateStructures),
			Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(GeneratorOptions::hasBonusChest),
			Codec.STRING
				.lenientOptionalFieldOf("legacy_custom_options")
				.stable()
				.forGetter(options -> options.legacyCustomOptions)
		).apply(instance, instance.stable(GeneratorOptions::new))
	);

	public static final GeneratorOptions DEMO_OPTIONS = new GeneratorOptions("North Carolina".hashCode(), true, true);

	private final long seed;
	private final boolean generateStructures;
	private final boolean bonusChest;
	private final Optional<String> legacyCustomOptions;

	public GeneratorOptions(long seed, boolean generateStructures, boolean bonusChest) {
		this(seed, generateStructures, bonusChest, Optional.empty());
	}

	private GeneratorOptions(
		long seed,
		boolean generateStructures,
		boolean bonusChest,
		Optional<String> legacyCustomOptions
	) {
		this.seed = seed;
		this.generateStructures = generateStructures;
		this.bonusChest = bonusChest;
		this.legacyCustomOptions = legacyCustomOptions;
	}

	/** Создаёт опции с случайным сидом, структурами включены, бонусный сундук выключен. */
	public static GeneratorOptions createRandom() {
		return new GeneratorOptions(getRandomSeed(), true, false);
	}

	/** Создаёт опции для тестового мира: случайный сид, структуры отключены. */
	public static GeneratorOptions createTestWorld() {
		return new GeneratorOptions(getRandomSeed(), false, false);
	}

	public long getSeed() {
		return seed;
	}

	public boolean shouldGenerateStructures() {
		return generateStructures;
	}

	public boolean hasBonusChest() {
		return bonusChest;
	}

	public boolean isLegacyCustomizedType() {
		return legacyCustomOptions.isPresent();
	}

	public GeneratorOptions withBonusChest(boolean bonusChest) {
		return new GeneratorOptions(seed, generateStructures, bonusChest, legacyCustomOptions);
	}

	public GeneratorOptions withStructures(boolean structures) {
		return new GeneratorOptions(seed, structures, bonusChest, legacyCustomOptions);
	}

	/**
	 * Возвращает новые опции с заданным сидом.
	 * Если {@code seed} пуст — генерируется случайный сид.
	 */
	public GeneratorOptions withSeed(OptionalLong seed) {
		return new GeneratorOptions(
			seed.orElse(getRandomSeed()),
			generateStructures,
			bonusChest,
			legacyCustomOptions
		);
	}

	/**
	 * Разбирает строку сида: числовая строка → long, иначе → hashCode строки.
	 * Пустая строка возвращает {@link OptionalLong#empty()}.
	 */
	public static OptionalLong parseSeed(String seed) {
		seed = seed.trim();

		if (StringUtils.isEmpty(seed)) {
			return OptionalLong.empty();
		}

		try {
			return OptionalLong.of(Long.parseLong(seed));
		} catch (NumberFormatException ignored) {
			return OptionalLong.of(seed.hashCode());
		}
	}

	public static long getRandomSeed() {
		return Random.create().nextLong();
	}
}
