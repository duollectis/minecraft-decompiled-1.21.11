package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelWithHead}.
 */
public interface ModelWithHead {

	ModelPart getHead();

	default void applyTransform(MatrixStack matrices) {
		this.getHead().applyTransform(matrices);
	}
}
