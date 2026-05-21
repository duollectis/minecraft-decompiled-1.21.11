package net.minecraft.world.attribute;

import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * {@code EnvironmentAttributeFunction}.
 */
public sealed interface EnvironmentAttributeFunction<Value>
		permits EnvironmentAttributeFunction.Constant,
		EnvironmentAttributeFunction.TimeBased,
		EnvironmentAttributeFunction.Positional {

	@FunctionalInterface
	/**
	 * {@code Constant}.
	 */
	public non-sealed interface Constant<Value> extends EnvironmentAttributeFunction<Value> {

		Value applyConstant(Value value);
	}

	@FunctionalInterface
	/**
	 * {@code Positional}.
	 */
	public non-sealed interface Positional<Value> extends EnvironmentAttributeFunction<Value> {

		Value applyPositional(Value value, Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList);
	}

	@FunctionalInterface
	/**
	 * {@code TimeBased}.
	 */
	public non-sealed interface TimeBased<Value> extends EnvironmentAttributeFunction<Value> {

		Value applyTimeBased(Value value, int time);
	}
}
