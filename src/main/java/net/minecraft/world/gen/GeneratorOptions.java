package net.minecraft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * {@code GeneratorOptions}.
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
							                    .forGetter(generatorOptions -> generatorOptions.legacyCustomOptions)
			                    )
			                    .apply(instance, instance.stable(GeneratorOptions::new))
	);
	public static final GeneratorOptions DEMO_OPTIONS = new GeneratorOptions("North Carolina".hashCode(), true, true);
	private final long seed;
	private final boolean generateStructures;
	private final boolean bonusChest;
	private final Optional<String> legacyCustomOptions;

	public GeneratorOptions(long seed, boolean generateStructures, boolean bonusChest) {
		this(seed, generateStructures, bonusChest, Optional.empty());
	}

	/**
	 * Создаёт random.
	 *
	 * @return GeneratorOptions — результат операции
	 */
	public static GeneratorOptions createRandom() {
		return new GeneratorOptions(getRandomSeed(), true, false);
	}

	/**
	 * Создаёт test world.
	 *
	 * @return GeneratorOptions — результат операции
	 */
	public static GeneratorOptions createTestWorld() {
		return new GeneratorOptions(getRandomSeed(), false, false);
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

	public long getSeed() {
		return this.seed;
	}

	/**
	 * Определяет, следует ли generate structures.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldGenerateStructures() {
		return this.generateStructures;
	}

	public boolean hasBonusChest() {
		return this.bonusChest;
	}

	public boolean isLegacyCustomizedType() {
		return this.legacyCustomOptions.isPresent();
	}

	/**
	 * With bonus chest.
	 *
	 * @param bonusChest bonus chest
	 *
	 * @return GeneratorOptions — результат операции
	 */
	public GeneratorOptions withBonusChest(boolean bonusChest) {
		return new GeneratorOptions(this.seed, this.generateStructures, bonusChest, this.legacyCustomOptions);
	}

	/**
	 * With structures.
	 *
	 * @param structures structures
	 *
	 * @return GeneratorOptions — результат операции
	 */
	public GeneratorOptions withStructures(boolean structures) {
		return new GeneratorOptions(this.seed, structures, this.bonusChest, this.legacyCustomOptions);
	}

	/**
	 * With seed.
	 *
	 * @param seed seed
	 *
	 * @return GeneratorOptions — результат операции
	 */
	public GeneratorOptions withSeed(OptionalLong seed) {
		return new GeneratorOptions(
				seed.orElse(getRandomSeed()),
				this.generateStructures,
				this.bonusChest,
				this.legacyCustomOptions
		);
	}

	/**
	 * Разбирает seed.
	 *
	 * @param seed seed
	 *
	 * @return OptionalLong — результат операции
	 */
	public static OptionalLong parseSeed(String seed) {
		seed = seed.trim();
		if (StringUtils.isEmpty(seed)) {
			return OptionalLong.empty();
		}
		else {
			try {
				return OptionalLong.of(Long.parseLong(seed));
			}
			catch (NumberFormatException var2) {
				return OptionalLong.of(seed.hashCode());
			}
		}
	}

	public static long getRandomSeed() {
		return Random.create().nextLong();
	}
}
