package net.minecraft.client.render.block.entity;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.state.SkullBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code SkullBlockEntityRenderer}.
 */
public class SkullBlockEntityRenderer implements BlockEntityRenderer<SkullBlockEntity, SkullBlockEntityRenderState> {

	private final Function<SkullBlock.SkullType, SkullBlockEntityModel> models;
	private static final Map<SkullBlock.SkullType, Identifier> TEXTURES = Util.make(
			Maps.newHashMap(), map -> {
				map.put(SkullBlock.Type.SKELETON, Identifier.ofVanilla("textures/entity/skeleton/skeleton.png"));
				map.put(
						SkullBlock.Type.WITHER_SKELETON,
						Identifier.ofVanilla("textures/entity/skeleton/wither_skeleton.png")
				);
				map.put(SkullBlock.Type.ZOMBIE, Identifier.ofVanilla("textures/entity/zombie/zombie.png"));
				map.put(SkullBlock.Type.CREEPER, Identifier.ofVanilla("textures/entity/creeper/creeper.png"));
				map.put(SkullBlock.Type.DRAGON, Identifier.ofVanilla("textures/entity/enderdragon/dragon.png"));
				map.put(SkullBlock.Type.PIGLIN, Identifier.ofVanilla("textures/entity/piglin/piglin.png"));
				map.put(SkullBlock.Type.PLAYER, DefaultSkinHelper.getTexture());
			}
	);
	private final PlayerSkinCache skinCache;

	public static @Nullable SkullBlockEntityModel getModels(LoadedEntityModels models, SkullBlock.SkullType type) {
		if (type instanceof SkullBlock.Type type2) {
			return (SkullBlockEntityModel) (switch (type2) {
				case SKELETON -> new SkullEntityModel(models.getModelPart(EntityModelLayers.SKELETON_SKULL));
				case WITHER_SKELETON ->
						new SkullEntityModel(models.getModelPart(EntityModelLayers.WITHER_SKELETON_SKULL));
				case PLAYER -> new SkullEntityModel(models.getModelPart(EntityModelLayers.PLAYER_HEAD));
				case ZOMBIE -> new SkullEntityModel(models.getModelPart(EntityModelLayers.ZOMBIE_HEAD));
				case CREEPER -> new SkullEntityModel(models.getModelPart(EntityModelLayers.CREEPER_HEAD));
				case DRAGON -> new DragonHeadEntityModel(models.getModelPart(EntityModelLayers.DRAGON_SKULL));
				case PIGLIN -> new PiglinHeadEntityModel(models.getModelPart(EntityModelLayers.PIGLIN_HEAD));
			}
			);
		}
		else {
			return null;
		}
	}

	public SkullBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
		LoadedEntityModels loadedEntityModels = context.loadedEntityModels();
		this.skinCache = context.playerSkinRenderCache();
		this.models = Util.memoize(type -> getModels(loadedEntityModels, type));
	}

	/**
	 * Создаёт render state.
	 *
	 * @return SkullBlockEntityRenderState — результат операции
	 */
	public SkullBlockEntityRenderState createRenderState() {
		return new SkullBlockEntityRenderState();
	}

	public void updateRenderState(
			SkullBlockEntity skullBlockEntity,
			SkullBlockEntityRenderState skullBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				skullBlockEntity,
				skullBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		skullBlockEntityRenderState.poweredTicks = skullBlockEntity.getPoweredTicks(f);
		BlockState blockState = skullBlockEntity.getCachedState();
		boolean bl = blockState.getBlock() instanceof WallSkullBlock;
		skullBlockEntityRenderState.facing = bl ? blockState.get(WallSkullBlock.FACING) : null;
		int
				i =
				bl ? RotationPropertyHelper.fromDirection(skullBlockEntityRenderState.facing.getOpposite())
				   : blockState.get(SkullBlock.ROTATION);
		skullBlockEntityRenderState.yaw = RotationPropertyHelper.toDegrees(i);
		skullBlockEntityRenderState.skullType = ((AbstractSkullBlock) blockState.getBlock()).getSkullType();
		skullBlockEntityRenderState.renderLayer =
				this.renderSkull(skullBlockEntityRenderState.skullType, skullBlockEntity);
	}

	public void render(
			SkullBlockEntityRenderState skullBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		SkullBlockEntityModel skullBlockEntityModel = this.models.apply(skullBlockEntityRenderState.skullType);
		render(
				skullBlockEntityRenderState.facing,
				skullBlockEntityRenderState.yaw,
				skullBlockEntityRenderState.poweredTicks,
				matrixStack,
				orderedRenderCommandQueue,
				skullBlockEntityRenderState.lightmapCoordinates,
				skullBlockEntityModel,
				skullBlockEntityRenderState.renderLayer,
				0,
				skullBlockEntityRenderState.crumblingOverlay
		);
	}

	public static void render(
			@Nullable Direction facing,
			float yaw,
			float poweredTicks,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			int light,
			SkullBlockEntityModel model,
			RenderLayer renderLayer,
			int outlineColor,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlay
	) {
		matrices.push();
		if (facing == null) {
			matrices.translate(0.5F, 0.0F, 0.5F);
		}
		else {
			float f = 0.25F;
			matrices.translate(0.5F - facing.getOffsetX() * 0.25F, 0.25F, 0.5F - facing.getOffsetZ() * 0.25F);
		}

		matrices.scale(-1.0F, -1.0F, 1.0F);
		SkullBlockEntityModel.SkullModelState skullModelState = new SkullBlockEntityModel.SkullModelState();
		skullModelState.poweredTicks = poweredTicks;
		skullModelState.yaw = yaw;
		queue.submitModel(
				model,
				skullModelState,
				matrices,
				renderLayer,
				light,
				OverlayTexture.DEFAULT_UV,
				outlineColor,
				crumblingOverlay
		);
		matrices.pop();
	}

	private RenderLayer renderSkull(SkullBlock.SkullType skullType, SkullBlockEntity blockEntity) {
		if (skullType == SkullBlock.Type.PLAYER) {
			ProfileComponent profileComponent = blockEntity.getOwner();
			if (profileComponent != null) {
				return this.skinCache.get(profileComponent).getRenderLayer();
			}
		}

		return getCutoutRenderLayer(skullType, null);
	}

	public static RenderLayer getCutoutRenderLayer(SkullBlock.SkullType type, @Nullable Identifier texture) {
		return RenderLayers.entityCutoutNoCullZOffset(texture != null ? texture : TEXTURES.get(type));
	}

	public static RenderLayer getTranslucentRenderLayer(Identifier texture) {
		return RenderLayers.entityTranslucent(texture);
	}
}
