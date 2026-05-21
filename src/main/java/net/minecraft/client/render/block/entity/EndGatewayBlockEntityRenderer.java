package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.state.EndGatewayBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code EndGatewayBlockEntityRenderer}.
 */
public class EndGatewayBlockEntityRenderer extends AbstractEndPortalBlockEntityRenderer<EndGatewayBlockEntity, EndGatewayBlockEntityRenderState> {

	private static final Identifier BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/end_gateway_beam.png");

	/**
	 * Создаёт render state.
	 *
	 * @return EndGatewayBlockEntityRenderState — результат операции
	 */
	public EndGatewayBlockEntityRenderState createRenderState() {
		return new EndGatewayBlockEntityRenderState();
	}

	public void updateRenderState(
			EndGatewayBlockEntity endGatewayBlockEntity,
			EndGatewayBlockEntityRenderState endGatewayBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		super.updateRenderState(
				endGatewayBlockEntity,
				endGatewayBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		World world = endGatewayBlockEntity.getWorld();
		if (endGatewayBlockEntity.isRecentlyGenerated()
				|| endGatewayBlockEntity.needsCooldownBeforeTeleporting() && world != null) {
			endGatewayBlockEntityRenderState.beamHeight = endGatewayBlockEntity.isRecentlyGenerated()
			                                              ? endGatewayBlockEntity.getRecentlyGeneratedBeamHeight(f)
			                                              : endGatewayBlockEntity.getCooldownBeamHeight(f);
			double
					d =
					endGatewayBlockEntity.isRecentlyGenerated() ? endGatewayBlockEntity.getWorld().getTopYInclusive()
					                                            : 50.0;
			endGatewayBlockEntityRenderState.beamHeight =
					MathHelper.sin(endGatewayBlockEntityRenderState.beamHeight * (float) Math.PI);
			endGatewayBlockEntityRenderState.beamSpan =
					MathHelper.floor(endGatewayBlockEntityRenderState.beamHeight * d);
			endGatewayBlockEntityRenderState.beamColor = endGatewayBlockEntity.isRecentlyGenerated()
			                                             ? DyeColor.MAGENTA.getEntityColor()
			                                             : DyeColor.PURPLE.getEntityColor();
			endGatewayBlockEntityRenderState.beamRotationDegrees = endGatewayBlockEntity.getWorld() != null
			                                                       ? Math.floorMod(
					endGatewayBlockEntity
					.getWorld()
					.getTime(), 40
			) + f
			                                                       : 0.0F;
		}
		else {
			endGatewayBlockEntityRenderState.beamSpan = 0;
		}
	}

	public void render(
			EndGatewayBlockEntityRenderState endGatewayBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (endGatewayBlockEntityRenderState.beamSpan > 0) {
			BeaconBlockEntityRenderer.renderBeam(
					matrixStack,
					orderedRenderCommandQueue,
					BEAM_TEXTURE,
					endGatewayBlockEntityRenderState.beamHeight,
					endGatewayBlockEntityRenderState.beamRotationDegrees,
					-endGatewayBlockEntityRenderState.beamSpan,
					endGatewayBlockEntityRenderState.beamSpan * 2,
					endGatewayBlockEntityRenderState.beamColor,
					0.15F,
					0.175F
			);
		}

		super.render(endGatewayBlockEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	@Override
	protected float getTopYOffset() {
		return 1.0F;
	}

	@Override
	protected float getBottomYOffset() {
		return 0.0F;
	}

	@Override
	protected RenderLayer getLayer() {
		return RenderLayers.endGateway();
	}

	@Override
	public int getRenderDistance() {
		return 256;
	}
}
