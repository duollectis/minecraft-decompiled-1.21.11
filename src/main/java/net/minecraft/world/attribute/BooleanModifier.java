package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.Interpolator;

/**
 * Модификатор булевых атрибутов окружения. Реализует стандартные логические
 * операции над текущим значением атрибута и аргументом модификатора.
 */
public enum BooleanModifier implements EnvironmentAttributeModifier<Boolean, Boolean> {
	AND,
	NAND,
	OR,
	NOR,
	XOR,
	XNOR;

	/**
	 * Применяет логическую операцию к текущему значению и аргументу.
	 *
	 * @param current текущее значение атрибута
	 * @param argument аргумент модификатора
	 * @return результат логической операции
	 */
	@Override
	public Boolean apply(Boolean current, Boolean argument) {
		return switch (this) {
			case AND -> argument && current;
			case NAND -> !argument || !current;
			case OR -> argument || current;
			case NOR -> !argument && !current;
			case XOR -> argument ^ current;
			case XNOR -> argument == current;
		};
	}

	@Override
	public Codec<Boolean> argumentCodec(EnvironmentAttribute<Boolean> attribute) {
		return Codec.BOOL;
	}

	@Override
	public Interpolator<Boolean> argumentKeyframeLerp(EnvironmentAttribute<Boolean> attribute) {
		return Interpolator.first();
	}
}
