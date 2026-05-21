package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.state.ConduitBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.SpriteMapper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ConduitBlockEntityRenderer}.
 */
public class ConduitBlockEntityRenderer implements BlockEntityRenderer<ConduitBlockEntity, ConduitBlockEntityRenderState> {

	public static final SpriteMapper
			SPRITE_MAPPER =
			new SpriteMapper(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, "entity/conduit");
	public static final SpriteIdentifier BASE_TEXTURE = SPRITE_MAPPER.mapVanilla("base");
	public static final SpriteIdentifier CAGE_TEXTURE = SPRITE_MAPPER.mapVanilla("cage");
	public static final SpriteIdentifier WIND_TEXTURE = SPRITE_MAPPER.mapVanilla("wind");
	public static final SpriteIdentifier WIND_VERTICAL_TEXTURE = SPRITE_MAPPER.mapVanilla("wind_vertical");
	public static final SpriteIdentifier OPEN_EYE_TEXTURE = SPRITE_MAPPER.mapVanilla("open_eye");
	public static final SpriteIdentifier CLOSED_EYE_TEXTURE = SPRITE_MAPPER.mapVanilla("closed_eye");
	private final SpriteHolder materials;
	private final ModelPart conduitEye;
	private final ModelPart conduitWind;
	private final ModelPart conduitShell;
	private final ModelPart conduit;

	public ConduitBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this.materials = ctx.spriteHolder();
		this.conduitEye = ctx.getLayerModelPart(EntityModelLayers.CONDUIT_EYE);
		this.conduitWind = ctx.getLayerModelPart(EntityModelLayers.CONDUIT_WIND);
		this.conduitShell = ctx.getLayerModelPart(EntityModelLayers.CONDUIT_SHELL);
		this.conduit = ctx.getLayerModelPart(EntityModelLayers.CONDUIT);
	}

	public static TexturedModelData getEyeTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"eye",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new Dilation(0.01F)),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 16, 16);
	}

	public static TexturedModelData getWindTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"wind",
				ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 32);
	}

	public static TexturedModelData getShellTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"shell",
				ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 32, 16);
	}

	public static TexturedModelData getPlainTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"shell",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 32, 16);
	}

	public ConduitBlockEntityRenderState createRenderState() {
		return new ConduitBlockEntityRenderState();
	}

	public void updateRenderState(
			ConduitBlockEntity conduitBlockEntity,
			ConduitBlockEntityRenderState conduitBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				conduitBlockEntity,
				conduitBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		conduitBlockEntityRenderState.active = conduitBlockEntity.isActive();
		conduitBlockEntityRenderState.rotation =
				conduitBlockEntity.getRotation(conduitBlockEntity.isActive() ? f : 0.0F);
		conduitBlockEntityRenderState.ticks = conduitBlockEntity.ticks + f;
		conduitBlockEntityRenderState.rotationPhase = conduitBlockEntity.ticks / 66 % 3;
		conduitBlockEntityRenderState.eyeOpen = conduitBlockEntity.isEyeOpen();
	}

	public void render(
			ConduitBlockEntityRenderState conduitBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (!conduitBlockEntityRenderState.active) {
			matrixStack.push();
			matrixStack.translate(0.5F, 0.5F, 0.5F);
			matrixStack.multiply(new Quaternionf().rotationY(
					conduitBlockEntityRenderState.rotation * (float) (Math.PI / 180.0)));
			orderedRenderCommandQueue.submitModelPart(
					this.conduitShell,
					matrixStack,
					BASE_TEXTURE.getRenderLayer(RenderLayers::entitySolid),
					conduitBlockEntityRenderState.lightmapCoordinates,
					OverlayTexture.DEFAULT_UV,
					this.materials.getSprite(BASE_TEXTURE),
					-1,
					conduitBlockEntityRenderState.crumblingOverlay
			);
			matrixStack.pop();
		}
		else {
			float f = conduitBlockEntityRenderState.rotation * (180.0F / (float) Math.PI);
			float g = MathHelper.sin(conduitBlockEntityRenderState.ticks * 0.1F) / 2.0F + 0.5F;
			g = g * g + g;
			matrixStack.push();
			matrixStack.translate(0.5F, 0.3F + g * 0.2F, 0.5F);
			Vector3f vector3f = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
			matrixStack.multiply(new Quaternionf().rotationAxis(f * (float) (Math.PI / 180.0), vector3f));
			orderedRenderCommandQueue.submitModelPart(
					this.conduit,
					matrixStack,
					CAGE_TEXTURE.getRenderLayer(RenderLayers::entityCutoutNoCull),
					conduitBlockEntityRenderState.lightmapCoordinates,
					OverlayTexture.DEFAULT_UV,
					this.materials.getSprite(CAGE_TEXTURE),
					-1,
					conduitBlockEntityRenderState.crumblingOverlay
			);
			matrixStack.pop();
			matrixStack.push();
			matrixStack.translate(0.5F, 0.5F, 0.5F);
			if (conduitBlockEntityRenderState.rotationPhase == 1) {
				matrixStack.multiply(new Quaternionf().rotationX((float) (Math.PI / 2)));
			}
			else if (conduitBlockEntityRenderState.rotationPhase == 2) {
				matrixStack.multiply(new Quaternionf().rotationZ((float) (Math.PI / 2)));
			}

			SpriteIdentifier
					spriteIdentifier =
					conduitBlockEntityRenderState.rotationPhase == 1 ? WIND_VERTICAL_TEXTURE : WIND_TEXTURE;
			RenderLayer renderLayer = spriteIdentifier.getRenderLayer(RenderLayers::entityCutoutNoCull);
			Sprite sprite = this.materials.getSprite(spriteIdentifier);
			orderedRenderCommandQueue.submitModelPart(
					this.conduitWind,
					matrixStack,
					renderLayer,
					conduitBlockEntityRenderState.lightmapCoordinates,
					OverlayTexture.DEFAULT_UV,
					sprite
			);
			matrixStack.pop();
			matrixStack.push();
			matrixStack.translate(0.5F, 0.5F, 0.5F);
			matrixStack.scale(0.875F, 0.875F, 0.875F);
			matrixStack.multiply(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
			orderedRenderCommandQueue.submitModelPart(
					this.conduitWind,
					matrixStack,
					renderLayer,
					conduitBlockEntityRenderState.lightmapCoordinates,
					OverlayTexture.DEFAULT_UV,
					sprite
			);
			matrixStack.pop();
			matrixStack.push();
			matrixStack.translate(0.5F, 0.3F + g * 0.2F, 0.5F);
			matrixStack.scale(0.5F, 0.5F, 0.5F);
			matrixStack.multiply(cameraRenderState.orientation);
			matrixStack.multiply(new Quaternionf().rotationZ((float) Math.PI).rotateY((float) Math.PI));
			float h = 1.3333334F;
			matrixStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
			SpriteIdentifier
					spriteIdentifier2 =
					conduitBlockEntityRenderState.eyeOpen ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE;
			orderedRenderCommandQueue.submitModelPart(
					this.conduitEye,
					matrixStack,
					spriteIdentifier2.getRenderLayer(RenderLayers::entityCutoutNoCull),
					conduitBlockEntityRenderState.lightmapCoordinates,
					OverlayTexture.DEFAULT_UV,
					this.materials.getSprite(spriteIdentifier2)
			);
			matrixStack.pop();
		}
	}
}
