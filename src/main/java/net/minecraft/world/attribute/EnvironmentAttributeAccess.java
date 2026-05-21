package net.minecraft.world.attribute;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * {@code EnvironmentAttributeAccess}.
 */
public interface EnvironmentAttributeAccess {

	EnvironmentAttributeAccess DEFAULT = new EnvironmentAttributeAccess() {
		@Override
		public <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute) {
			return attribute.getDefaultValue();
		}

		@Override
		public <Value> Value getAttributeValue(
				EnvironmentAttribute<Value> attribute,
				Vec3d pos,
				@Nullable WeightedAttributeList pool
		) {
			return attribute.getDefaultValue();
		}
	};

	<Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute);

	default <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute, BlockPos pos) {
		return this.getAttributeValue(attribute, Vec3d.ofCenter(pos));
	}

	default <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute, Vec3d pos) {
		return this.getAttributeValue(attribute, pos, null);
	}

	<Value> Value getAttributeValue(
			EnvironmentAttribute<Value> attribute,
			Vec3d pos,
			@Nullable WeightedAttributeList pool
	);
}
