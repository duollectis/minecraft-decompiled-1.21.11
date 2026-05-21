package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BeamEmitter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.state.BeaconBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BeaconBlockEntityRenderer}.
 */
public class BeaconBlockEntityRenderer<T extends BlockEntity & BeamEmitter> implements BlockEntityRenderer<T, BeaconBlockEntityRenderState> {

	public static final Identifier BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/beacon_beam.png");
	public static final int MAX_BEAM_HEIGHT = 2048;
	private static final float BEAM_TEXTURE_SIZE = 96.0F;
	public static final float BEAM_INNER_RADIUS = 0.2F;
	public static final float BEAM_OUTER_RADIUS = 0.25F;

	/**
	 * Создаёт render state.
	 *
	 * @return BeaconBlockEntityRenderState — результат операции
	 */
	public BeaconBlockEntityRenderState createRenderState() {
		return new BeaconBlockEntityRenderState();
	}

	public void updateRenderState(
			T blockEntity,
			BeaconBlockEntityRenderState beaconBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				blockEntity,
				beaconBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		updateBeaconRenderState(blockEntity, beaconBlockEntityRenderState, f, vec3d);
	}

	public static <T extends BlockEntity & BeamEmitter> void updateBeaconRenderState(
			T blockEntity, BeaconBlockEntityRenderState state, float tickProgress, Vec3d cameraPos
	) {
		state.beamRotationDegrees =
				blockEntity.getWorld() != null ? Math.floorMod(blockEntity.getWorld().getTime(), 40) + tickProgress
				                               : 0.0F;
		state.beamSegments = blockEntity.getBeamSegments()
		                                .stream()
		                                .map(beamSegment -> new BeaconBlockEntityRenderState.BeamSegment(
				                                beamSegment.getColor(),
				                                beamSegment.getHeight()
		                                ))
		                                .toList();
		float f = (float) cameraPos.subtract(state.pos.toCenterPos()).horizontalLength();
		ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
		state.beamScale =
				clientPlayerEntity != null && clientPlayerEntity.isUsingSpyglass() ? 1.0F : Math.max(1.0F, f / 96.0F);
	}

	public void render(
			BeaconBlockEntityRenderState beaconBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		int i = 0;

		for (int j = 0; j < beaconBlockEntityRenderState.beamSegments.size(); j++) {
			BeaconBlockEntityRenderState.BeamSegment beamSegment = beaconBlockEntityRenderState.beamSegments.get(j);
			renderBeam(
					matrixStack,
					orderedRenderCommandQueue,
					beaconBlockEntityRenderState.beamScale,
					beaconBlockEntityRenderState.beamRotationDegrees,
					i,
					j == beaconBlockEntityRenderState.beamSegments.size() - 1 ? 2048 : beamSegment.height(),
					beamSegment.color()
			);
			i += beamSegment.height();
		}
	}

	private static void renderBeam(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			float scale,
			float rotationDegrees,
			int minHeight,
			int maxHeight,
			int color
	) {
		renderBeam(
				matrices,
				queue,
				BEAM_TEXTURE,
				1.0F,
				rotationDegrees,
				minHeight,
				maxHeight,
				color,
				0.2F * scale,
				0.25F * scale
		);
	}

	public static void renderBeam(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			Identifier textureId,
			float beamHeight,
			float beamRotationDegrees,
			int minHeight,
			int maxHeight,
			int color,
			float innerScale,
			float outerScale
	) {
		int i = minHeight + maxHeight;
		matrices.push();
		matrices.translate(0.5, 0.0, 0.5);
		float f = maxHeight < 0 ? beamRotationDegrees : -beamRotationDegrees;
		float g = MathHelper.fractionalPart(f * 0.2F - MathHelper.floor(f * 0.1F));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(beamRotationDegrees * 2.25F - 45.0F));
		float innerM = -innerScale;
		float innerP = -innerScale;
		float innerS = -1.0F + g;
		float innerT = maxHeight * beamHeight * (0.5F / innerScale) + innerS;
		queue.submitCustom(
				matrices,
				RenderLayers.beaconBeam(textureId, false),
				(matricesEntry, vertexConsumer) -> renderBeamLayer(
						matricesEntry,
						vertexConsumer,
						color,
						minHeight,
						i,
						0.0F,
						innerScale,
						innerScale,
						0.0F,
						innerM,
						0.0F,
						0.0F,
						innerP,
						0.0F,
						1.0F,
						innerT,
						innerS
				)
		);
		matrices.pop();
		float outerH = -outerScale;
		float outerJ = -outerScale;
		float outerL = -outerScale;
		float outerM = -outerScale;
		float outerS = -1.0F + g;
		float outerT = maxHeight * beamHeight + outerS;
		queue.submitCustom(
				matrices,
				RenderLayers.beaconBeam(textureId, true),
				(matricesEntry, vertexConsumer) -> renderBeamLayer(
						matricesEntry,
						vertexConsumer,
						ColorHelper.withAlpha(32, color),
						minHeight,
						i,
						outerH,
						outerJ,
						outerScale,
						outerL,
						outerM,
						outerScale,
						outerScale,
						outerScale,
						0.0F,
						1.0F,
						outerT,
						outerS
				)
		);
		matrices.pop();
	}

	private static void renderBeamLayer(
			MatrixStack.Entry matricesEntry,
			VertexConsumer vertices,
			int color,
			int yOffset,
			int height,
			float x1,
			float z1,
			float x2,
			float z2,
			float x3,
			float z3,
			float x4,
			float z4,
			float u1,
			float u2,
			float v1,
			float v2
	) {
		renderBeamFace(matricesEntry, vertices, color, yOffset, height, x1, z1, x2, z2, u1, u2, v1, v2);
		renderBeamFace(matricesEntry, vertices, color, yOffset, height, x4, z4, x3, z3, u1, u2, v1, v2);
		renderBeamFace(matricesEntry, vertices, color, yOffset, height, x2, z2, x4, z4, u1, u2, v1, v2);
		renderBeamFace(matricesEntry, vertices, color, yOffset, height, x3, z3, x1, z1, u1, u2, v1, v2);
	}

	private static void renderBeamFace(
			MatrixStack.Entry matrix,
			VertexConsumer vertices,
			int color,
			int yOffset,
			int height,
			float x1,
			float z1,
			float x2,
			float z2,
			float u1,
			float u2,
			float v1,
			float v2
	) {
		renderBeamVertex(matrix, vertices, color, height, x1, z1, u2, v1);
		renderBeamVertex(matrix, vertices, color, yOffset, x1, z1, u2, v2);
		renderBeamVertex(matrix, vertices, color, yOffset, x2, z2, u1, v2);
		renderBeamVertex(matrix, vertices, color, height, x2, z2, u1, v1);
	}

	private static void renderBeamVertex(
			MatrixStack.Entry matrix,
			VertexConsumer vertices,
			int color,
			int y,
			float x,
			float z,
			float u,
			float v
	) {
		vertices
				.vertex(matrix, x, (float) y, z)
				.color(color)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(15728880)
				.normal(matrix, 0.0F, 1.0F, 0.0F);
	}

	@Override
	public boolean rendersOutsideBoundingBox() {
		return true;
	}

	@Override
	public int getRenderDistance() {
		return MinecraftClient.getInstance().options.getClampedViewDistance() * 16;
	}

	@Override
	public boolean isInRenderDistance(T blockEntity, Vec3d pos) {
		return Vec3d
				.ofCenter(blockEntity.getPos())
				.multiply(1.0, 0.0, 1.0)
				.isInRange(pos.multiply(1.0, 0.0, 1.0), this.getRenderDistance());
	}
}
