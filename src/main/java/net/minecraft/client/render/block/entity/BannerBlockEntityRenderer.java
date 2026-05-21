package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.model.BannerBlockModel;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.block.entity.state.BannerBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code BannerBlockEntityRenderer}.
 */
public class BannerBlockEntityRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerBlockEntityRenderState> {

	private static final int ROTATIONS = 16;
	private static final float BANNER_SCALE = 0.6666667F;
	private final SpriteHolder materials;
	private final BannerBlockModel standingModel;
	private final BannerBlockModel wallModel;
	private final BannerFlagBlockModel standingFlagModel;
	private final BannerFlagBlockModel wallFlagModel;

	public BannerBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		this(context.loadedEntityModels(), context.spriteHolder());
	}

	public BannerBlockEntityRenderer(SpecialModelRenderer.BakeContext context) {
		this(context.entityModelSet(), context.spriteHolder());
	}

	public BannerBlockEntityRenderer(LoadedEntityModels models, SpriteHolder materials) {
		this.materials = materials;
		this.standingModel = new BannerBlockModel(models.getModelPart(EntityModelLayers.STANDING_BANNER));
		this.wallModel = new BannerBlockModel(models.getModelPart(EntityModelLayers.WALL_BANNER));
		this.standingFlagModel = new BannerFlagBlockModel(models.getModelPart(EntityModelLayers.STANDING_BANNER_FLAG));
		this.wallFlagModel = new BannerFlagBlockModel(models.getModelPart(EntityModelLayers.WALL_BANNER_FLAG));
	}

	public BannerBlockEntityRenderState createRenderState() {
		return new BannerBlockEntityRenderState();
	}

	public void updateRenderState(
			BannerBlockEntity bannerBlockEntity,
			BannerBlockEntityRenderState bannerBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				bannerBlockEntity,
				bannerBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		bannerBlockEntityRenderState.dyeColor = bannerBlockEntity.getColorForState();
		bannerBlockEntityRenderState.bannerPatterns = bannerBlockEntity.getPatterns();
		BlockState blockState = bannerBlockEntity.getCachedState();
		if (blockState.getBlock() instanceof BannerBlock) {
			bannerBlockEntityRenderState.yaw = -RotationPropertyHelper.toDegrees(blockState.get(BannerBlock.ROTATION));
			bannerBlockEntityRenderState.standing = true;
		}
		else {
			bannerBlockEntityRenderState.yaw = -blockState.get(WallBannerBlock.FACING).getPositiveHorizontalDegrees();
			bannerBlockEntityRenderState.standing = false;
		}

		long l = bannerBlockEntity.getWorld() != null ? bannerBlockEntity.getWorld().getTime() : 0L;
		BlockPos blockPos = bannerBlockEntity.getPos();
		bannerBlockEntityRenderState.pitch =
				((float) Math.floorMod(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13 + l, 100L) + f)
						/ 100.0F;
	}

	public void render(
			BannerBlockEntityRenderState bannerBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		BannerBlockModel bannerBlockModel;
		BannerFlagBlockModel bannerFlagBlockModel;
		if (bannerBlockEntityRenderState.standing) {
			bannerBlockModel = this.standingModel;
			bannerFlagBlockModel = this.standingFlagModel;
		}
		else {
			bannerBlockModel = this.wallModel;
			bannerFlagBlockModel = this.wallFlagModel;
		}

		render(
				this.materials,
				matrixStack,
				orderedRenderCommandQueue,
				bannerBlockEntityRenderState.lightmapCoordinates,
				OverlayTexture.DEFAULT_UV,
				bannerBlockEntityRenderState.yaw,
				bannerBlockModel,
				bannerFlagBlockModel,
				bannerBlockEntityRenderState.pitch,
				bannerBlockEntityRenderState.dyeColor,
				bannerBlockEntityRenderState.bannerPatterns,
				bannerBlockEntityRenderState.crumblingOverlay,
				0
		);
	}

	public void renderAsItem(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			DyeColor baseColor,
			BannerPatternsComponent patterns,
			int i
	) {
		render(
				this.materials,
				matrices,
				queue,
				light,
				overlay,
				0.0F,
				this.standingModel,
				this.standingFlagModel,
				0.0F,
				baseColor,
				patterns,
				null,
				i
		);
	}

	private static void render(
			SpriteHolder materials,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			float yaw,
			BannerBlockModel model,
			BannerFlagBlockModel flagModel,
			float pitch,
			DyeColor dyeColor,
			BannerPatternsComponent bannerPatterns,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay,
			int i
	) {
		matrices.push();
		matrices.translate(0.5F, 0.0F, 0.5F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
		matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
		SpriteIdentifier spriteIdentifier = ModelBaker.BANNER_BASE;
		queue.submitModel(
				model,
				Unit.INSTANCE,
				matrices,
				spriteIdentifier.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				-1,
				materials.getSprite(spriteIdentifier),
				i,
				crumblingOverlay
		);
		renderCanvas(
				materials,
				matrices,
				queue,
				light,
				overlay,
				flagModel,
				pitch,
				spriteIdentifier,
				true,
				dyeColor,
				bannerPatterns,
				false,
				crumblingOverlay,
				i
		);
		matrices.pop();
	}

	public static <S> void renderCanvas(
			SpriteHolder materials,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			Model<S> model,
			S state,
			SpriteIdentifier spriteId,
			boolean useBannerLayer,
			DyeColor color,
			BannerPatternsComponent patterns,
			boolean bl,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand,
			int i
	) {
		queue.submitModel(
				model,
				state,
				matrices,
				spriteId.getRenderLayer(RenderLayers::entitySolid),
				light,
				overlay,
				-1,
				materials.getSprite(spriteId),
				i,
				crumblingOverlayCommand
		);
		if (bl) {
			queue.submitModel(
					model,
					state,
					matrices,
					RenderLayers.entityGlint(),
					light,
					overlay,
					-1,
					materials.getSprite(spriteId),
					0,
					crumblingOverlayCommand
			);
		}

		renderLayer(
				materials,
				matrices,
				queue,
				light,
				overlay,
				model,
				state,
				useBannerLayer ? TexturedRenderLayers.BANNER_BASE : TexturedRenderLayers.SHIELD_BASE,
				color,
				crumblingOverlayCommand
		);

		for (int j = 0; j < 16 && j < patterns.layers().size(); j++) {
			BannerPatternsComponent.Layer layer = patterns.layers().get(j);
			SpriteIdentifier spriteIdentifier = useBannerLayer
			                                    ? TexturedRenderLayers.getBannerPatternTextureId(layer.pattern())
			                                    : TexturedRenderLayers.getShieldPatternTextureId(layer.pattern());
			renderLayer(
					materials,
					matrices,
					queue,
					light,
					overlay,
					model,
					state,
					spriteIdentifier,
					layer.color(),
					null
			);
		}
	}

	private static <S> void renderLayer(
			SpriteHolder materials,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			int overlay,
			Model<S> model,
			S state,
			SpriteIdentifier spriteId,
			DyeColor color,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
	) {
		int i = color.getEntityColor();
		queue.submitModel(
				model,
				state,
				matrices,
				spriteId.getRenderLayer(RenderLayers::entityNoOutline),
				light,
				overlay,
				i,
				materials.getSprite(spriteId),
				0,
				crumblingOverlay
		);
	}

	public void collectVertices(Consumer<Vector3fc> consumer) {
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.translate(0.5F, 0.0F, 0.5F);
		matrixStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
		this.standingModel.getRootPart().collectVertices(matrixStack, consumer);
		this.standingFlagModel.setAngles(0.0F);
		this.standingFlagModel.getRootPart().collectVertices(matrixStack, consumer);
	}
}
