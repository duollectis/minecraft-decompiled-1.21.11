package net.minecraft.util.math.intprovider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.random.Random;

/**
 * Поставщик целых чисел, всегда возвращающий одно и то же константное значение.
 * Оптимизирован: для нуля возвращает кешированный экземпляр {@link #ZERO}.
 */
public class ConstantIntProvider extends IntProvider {

	public static final ConstantIntProvider ZERO = new ConstantIntProvider(0);
	public static final MapCodec<ConstantIntProvider> CODEC = Codec.INT
		.fieldOf("value")
		.xmap(ConstantIntProvider::create, ConstantIntProvider::getValue);

	private final int value;

	public static ConstantIntProvider create(int value) {
		return value == 0 ? ZERO : new ConstantIntProvider(value);
	}

	private ConstantIntProvider(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	@Override
	public int get(Random random) {
		return value;
	}

	@Override
	public int getMin() {
		return value;
	}

	@Override
	public int getMax() {
		return value;
	}

	@Override
	public IntProviderType<?> getType() {
		return IntProviderType.CONSTANT;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
