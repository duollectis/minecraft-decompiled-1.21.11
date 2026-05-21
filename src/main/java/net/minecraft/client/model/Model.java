package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code Model}.
 */
public abstract class Model<S> implements FabricModel<S> {

	protected final ModelPart root;
	protected final Function<Identifier, RenderLayer> layerFactory;
	private final List<ModelPart> parts;

	public Model(ModelPart root, Function<Identifier, RenderLayer> layerFactory) {
		this.root = root;
		this.layerFactory = layerFactory;
		this.parts = root.traverse();
	}

	public final RenderLayer getLayer(Identifier texture) {
		return this.layerFactory.apply(texture);
	}

	/**
	 * Render.
	 *
	 * @param matrices matrices
	 * @param vertices vertices
	 * @param light light
	 * @param overlay overlay
	 * @param color color
	 */
	public final void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		this.getRootPart().render(matrices, vertices, light, overlay, color);
	}

	/**
	 * Render.
	 *
	 * @param matrices matrices
	 * @param vertices vertices
	 * @param light light
	 * @param overlay overlay
	 */
	public final void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.render(matrices, vertices, light, overlay, -1);
	}

	public final ModelPart getRootPart() {
		return this.root;
	}

	public final List<ModelPart> getParts() {
		return this.parts;
	}

	public void setAngles(S state) {
		this.resetTransforms();
	}

	/**
	 * Сбрасывает transforms.
	 */
	public final void resetTransforms() {
		for (ModelPart modelPart : this.parts) {
			modelPart.resetTransform();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SinglePartModel}.
	 */
	public static class SinglePartModel extends Model<Unit> {

		public SinglePartModel(ModelPart part, Function<Identifier, RenderLayer> layerFactory) {
			super(part, layerFactory);
		}

		public void setAngles(Unit unit) {
		}
	}
}
