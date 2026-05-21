package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.Identifier;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code EntityModel}.
 */
public abstract class EntityModel<T extends EntityRenderState> extends Model<T> {

	public static final float RIDING_Y_OFFSET = -1.501F;

	protected EntityModel(ModelPart root) {
		this(root, RenderLayers::entityCutoutNoCull);
	}

	protected EntityModel(ModelPart modelPart, Function<Identifier, RenderLayer> function) {
		super(modelPart, function);
	}
}
