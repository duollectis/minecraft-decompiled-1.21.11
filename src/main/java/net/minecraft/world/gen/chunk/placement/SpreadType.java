package net.minecraft.world.gen.chunk.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;

/**
 * Стратегия распределения смещения структуры внутри региона размещения.
 * {@link #LINEAR} даёт равномерное распределение, {@link #TRIANGULAR} — нормальное
 * (среднее значение более вероятно), что создаёт более органичное расположение структур.
 */
public enum SpreadType implements StringIdentifiable {
	LINEAR("linear"),
	TRIANGULAR("triangular");

	public static final Codec<SpreadType> CODEC = StringIdentifiable.createCodec(SpreadType::values);

	private final String name;

	SpreadType(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}

	/**
	 * Возвращает случайное смещение в диапазоне {@code [0, bound)}.
	 * Для {@link #TRIANGULAR} усредняет два броска, смещая результат к центру диапазона.
	 */
	public int get(Random random, int bound) {
		return switch (this) {
			case LINEAR -> random.nextInt(bound);
			case TRIANGULAR -> (random.nextInt(bound) + random.nextInt(bound)) / 2;
		};
	}
}
