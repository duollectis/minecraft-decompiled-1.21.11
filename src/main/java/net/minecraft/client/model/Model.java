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

/**
 * Базовый класс клиентской модели сущности или блока.
 * Хранит корневую часть {@link ModelPart} и фабрику слоёв рендера.
 * Подклассы переопределяют {@link #setAngles(Object)} для анимации.
 *
 * @param <S> тип состояния анимации (например, {@link Unit} для статичных моделей)
 */
@Environment(EnvType.CLIENT)
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
		return layerFactory.apply(texture);
	}

	public final void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		getRootPart().render(matrices, vertices, light, overlay, color);
	}

	public final void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		render(matrices, vertices, light, overlay, -1);
	}

	public final ModelPart getRootPart() {
		return root;
	}

	public final List<ModelPart> getParts() {
		return parts;
	}

	public void setAngles(S state) {
		resetTransforms();
	}

	/** Сбрасывает трансформации всех частей модели к значениям по умолчанию. */
	public final void resetTransforms() {
		for (ModelPart part : parts) {
			part.resetTransform();
		}
	}

	/** Статичная модель без анимации, принимающая {@link Unit} как состояние. */
	@Environment(EnvType.CLIENT)
	public static class SinglePartModel extends Model<Unit> {

		public SinglePartModel(ModelPart part, Function<Identifier, RenderLayer> layerFactory) {
			super(part, layerFactory);
		}

		@Override
		public void setAngles(Unit unit) {
		}
	}
}
