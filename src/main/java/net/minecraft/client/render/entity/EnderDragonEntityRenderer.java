package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.EnderDragonEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
/**
 * {@code EnderDragonEntityRenderer}.
 */
public class EnderDragonEntityRenderer extends EntityRenderer<EnderDragonEntity, EnderDragonEntityRenderState> {

	public static final Identifier
			CRYSTAL_BEAM_TEXTURE =
			Identifier.ofVanilla("textures/entity/end_crystal/end_crystal_beam.png");
	private static final Identifier
			EXPLOSION_TEXTURE =
			Identifier.ofVanilla("textures/entity/enderdragon/dragon_exploding.png");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/enderdragon/dragon.png");
	private static final Identifier EYE_TEXTURE = Identifier.ofVanilla("textures/entity/enderdragon/dragon_eyes.png");
	private static final RenderLayer DRAGON_CUTOUT = RenderLayers.entityCutoutNoCull(TEXTURE);
	private static final RenderLayer DRAGON_DECAL = RenderLayers.entityDecal(TEXTURE);
	private static final RenderLayer DRAGON_EYES = RenderLayers.eyes(EYE_TEXTURE);
	private static final RenderLayer CRYSTAL_BEAM_LAYER = RenderLayers.entitySmoothCutout(CRYSTAL_BEAM_TEXTURE);
	private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0) / 2.0);
	private final DragonEntityModel model;

	public EnderDragonEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.shadowRadius = 0.5F;
		this.model = new DragonEntityModel(context.getPart(EntityModelLayers.ENDER_DRAGON));
	}

	public void render(
			EnderDragonEntityRenderState enderDragonEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		float f = enderDragonEntityRenderState.getLerpedFrame(7).yRot();
		float
				g =
				(float) (enderDragonEntityRenderState.getLerpedFrame(5).y() - enderDragonEntityRenderState
						.getLerpedFrame(10)
						.y()
				);
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-f));
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * 10.0F));
		matrixStack.translate(0.0F, 0.0F, 1.0F);
		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		matrixStack.translate(0.0F, -1.501F, 0.0F);
		int i = OverlayTexture.getUv(0.0F, enderDragonEntityRenderState.hurt);
		if (enderDragonEntityRenderState.ticksSinceDeath > 0.0F) {
			int j = ColorHelper.getWhite(enderDragonEntityRenderState.ticksSinceDeath / 200.0F);
			orderedRenderCommandQueue.getBatchingQueue(0)
			                         .submitModel(
					                         this.model,
					                         enderDragonEntityRenderState,
					                         matrixStack,
					                         RenderLayers.entityAlpha(EXPLOSION_TEXTURE),
					                         enderDragonEntityRenderState.light,
					                         OverlayTexture.DEFAULT_UV,
					                         j,
					                         null,
					                         enderDragonEntityRenderState.outlineColor,
					                         null
			                         );
			orderedRenderCommandQueue.getBatchingQueue(1)
			                         .submitModel(
					                         this.model,
					                         enderDragonEntityRenderState,
					                         matrixStack,
					                         DRAGON_DECAL,
					                         enderDragonEntityRenderState.light,
					                         i,
					                         -1,
					                         null,
					                         enderDragonEntityRenderState.outlineColor,
					                         null
			                         );
		}
		else {
			orderedRenderCommandQueue.getBatchingQueue(0)
			                         .submitModel(
					                         this.model,
					                         enderDragonEntityRenderState,
					                         matrixStack,
					                         DRAGON_CUTOUT,
					                         enderDragonEntityRenderState.light,
					                         i,
					                         -1,
					                         null,
					                         enderDragonEntityRenderState.outlineColor,
					                         null
			                         );
		}

		orderedRenderCommandQueue.submitModel(
				this.model,
				enderDragonEntityRenderState,
				matrixStack,
				DRAGON_EYES,
				enderDragonEntityRenderState.light,
				OverlayTexture.DEFAULT_UV,
				enderDragonEntityRenderState.outlineColor,
				null
		);
		if (enderDragonEntityRenderState.ticksSinceDeath > 0.0F) {
			float h = enderDragonEntityRenderState.ticksSinceDeath / 200.0F;
			matrixStack.push();
			matrixStack.translate(0.0F, -1.0F, -2.0F);
			renderDeathAnimation(matrixStack, h, orderedRenderCommandQueue, RenderLayers.dragonRays());
			renderDeathAnimation(matrixStack, h, orderedRenderCommandQueue, RenderLayers.dragonRaysDepth());
			matrixStack.pop();
		}

		matrixStack.pop();
		if (enderDragonEntityRenderState.crystalBeamPos != null) {
			renderCrystalBeam(
					(float) enderDragonEntityRenderState.crystalBeamPos.x,
					(float) enderDragonEntityRenderState.crystalBeamPos.y,
					(float) enderDragonEntityRenderState.crystalBeamPos.z,
					enderDragonEntityRenderState.age,
					matrixStack,
					orderedRenderCommandQueue,
					enderDragonEntityRenderState.light
			);
		}

		super.render(enderDragonEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	private static void renderDeathAnimation(
			MatrixStack matrices,
			float animationProgress,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			RenderLayer renderLayer
	) {
		orderedRenderCommandQueue.submitCustom(
				matrices,
				renderLayer,
				(entry, vertexConsumer) -> {
					float g = Math.min(animationProgress > 0.8F ? (animationProgress - 0.8F) / 0.2F : 0.0F, 1.0F);
					int i = ColorHelper.fromFloats(1.0F - g, 1.0F, 1.0F, 1.0F);
					int j = 16711935;
					Random random = Random.create(432L);
					Vector3f vector3f = new Vector3f();
					Vector3f vector3f2 = new Vector3f();
					Vector3f vector3f3 = new Vector3f();
					Vector3f vector3f4 = new Vector3f();
					Quaternionf quaternionf = new Quaternionf();
					int
							k =
							MathHelper.floor(
									(animationProgress + animationProgress * animationProgress) / 2.0F * 60.0F);

					for (int l = 0; l < k; l++) {
						quaternionf.rotationXYZ(
								           random.nextFloat() * (float) (Math.PI * 2),
								           random.nextFloat() * (float) (Math.PI * 2),
								           random.nextFloat() * (float) (Math.PI * 2)
						           )
						           .rotateXYZ(
								           random.nextFloat() * (float) (Math.PI * 2),
								           random.nextFloat() * (float) (Math.PI * 2),
								           random.nextFloat() * (float) (Math.PI * 2) + animationProgress * (float) (
										           Math.PI / 2
								           )
						           );
						entry.rotate(quaternionf);
						float h = random.nextFloat() * 20.0F + 5.0F + g * 10.0F;
						float m = random.nextFloat() * 2.0F + 1.0F + g * 2.0F;
						vector3f2.set(-HALF_SQRT_3 * m, h, -0.5F * m);
						vector3f3.set(HALF_SQRT_3 * m, h, -0.5F * m);
						vector3f4.set(0.0F, h, m);
						vertexConsumer.vertex(entry, vector3f).color(i);
						vertexConsumer.vertex(entry, vector3f2).color(16711935);
						vertexConsumer.vertex(entry, vector3f3).color(16711935);
						vertexConsumer.vertex(entry, vector3f).color(i);
						vertexConsumer.vertex(entry, vector3f3).color(16711935);
						vertexConsumer.vertex(entry, vector3f4).color(16711935);
						vertexConsumer.vertex(entry, vector3f).color(i);
						vertexConsumer.vertex(entry, vector3f4).color(16711935);
						vertexConsumer.vertex(entry, vector3f2).color(16711935);
					}
				}
		);
	}

	public static void renderCrystalBeam(
			float dx,
			float dy,
			float dz,
			float tickProgress,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light
	) {
		float f = MathHelper.sqrt(dx * dx + dz * dz);
		float g = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
		matrices.push();
		matrices.translate(0.0F, 2.0F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) (-Math.atan2(dz, dx)) - (float) (Math.PI / 2)));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) (-Math.atan2(f, dy)) - (float) (Math.PI / 2)));
		float h = 0.0F - tickProgress * 0.01F;
		float i = g / 32.0F - tickProgress * 0.01F;
		orderedRenderCommandQueue.submitCustom(
				matrices,
				CRYSTAL_BEAM_LAYER,
				(entry, vertexConsumer) -> {
					int j = 8;
					float k = 0.0F;
					float l = 0.75F;
					float m = 0.0F;

					for (int n = 1; n <= 8; n++) {
						float o = MathHelper.sin(n * (float) (Math.PI * 2) / 8.0F) * 0.75F;
						float p = MathHelper.cos(n * (float) (Math.PI * 2) / 8.0F) * 0.75F;
						float q = n / 8.0F;
						vertexConsumer.vertex(entry, k * 0.2F, l * 0.2F, 0.0F)
						              .color(-16777216)
						              .texture(m, h)
						              .overlay(OverlayTexture.DEFAULT_UV)
						              .light(light)
						              .normal(entry, 0.0F, -1.0F, 0.0F);
						vertexConsumer
								.vertex(entry, k, l, g)
								.color(-1)
								.texture(m, i)
								.overlay(OverlayTexture.DEFAULT_UV)
								.light(light)
								.normal(entry, 0.0F, -1.0F, 0.0F);
						vertexConsumer
								.vertex(entry, o, p, g)
								.color(-1)
								.texture(q, i)
								.overlay(OverlayTexture.DEFAULT_UV)
								.light(light)
								.normal(entry, 0.0F, -1.0F, 0.0F);
						vertexConsumer.vertex(entry, o * 0.2F, p * 0.2F, 0.0F)
						              .color(-16777216)
						              .texture(q, h)
						              .overlay(OverlayTexture.DEFAULT_UV)
						              .light(light)
						              .normal(entry, 0.0F, -1.0F, 0.0F);
						k = o;
						l = p;
						m = q;
					}
				}
		);
		matrices.pop();
	}

	public EnderDragonEntityRenderState createRenderState() {
		return new EnderDragonEntityRenderState();
	}

	public void updateRenderState(
			EnderDragonEntity enderDragonEntity,
			EnderDragonEntityRenderState enderDragonEntityRenderState,
			float f
	) {
		super.updateRenderState(enderDragonEntity, enderDragonEntityRenderState, f);
		enderDragonEntityRenderState.wingPosition =
				MathHelper.lerp(f, enderDragonEntity.lastWingPosition, enderDragonEntity.wingPosition);
		enderDragonEntityRenderState.ticksSinceDeath =
				enderDragonEntity.ticksSinceDeath > 0 ? enderDragonEntity.ticksSinceDeath + f : 0.0F;
		enderDragonEntityRenderState.hurt = enderDragonEntity.hurtTime > 0;
		EndCrystalEntity endCrystalEntity = enderDragonEntity.connectedCrystal;
		if (endCrystalEntity != null) {
			Vec3d
					vec3d =
					endCrystalEntity
							.getLerpedPos(f)
							.add(0.0, EndCrystalEntityRenderer.getYOffset(endCrystalEntity.endCrystalAge + f), 0.0);
			enderDragonEntityRenderState.crystalBeamPos = vec3d.subtract(enderDragonEntity.getLerpedPos(f));
		}
		else {
			enderDragonEntityRenderState.crystalBeamPos = null;
		}

		Phase phase = enderDragonEntity.getPhaseManager().getCurrent();
		enderDragonEntityRenderState.inLandingOrTakeoffPhase = phase == PhaseType.LANDING || phase == PhaseType.TAKEOFF;
		enderDragonEntityRenderState.sittingOrHovering = phase.isSittingOrHovering();
		BlockPos blockPos = enderDragonEntity.getEntityWorld()
		                                     .getTopPosition(
				                                     Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				                                     EndPortalFeature.offsetOrigin(enderDragonEntity.getFightOrigin())
		                                     );
		enderDragonEntityRenderState.squaredDistanceFromOrigin =
				blockPos.getSquaredDistance(enderDragonEntity.getEntityPos());
		enderDragonEntityRenderState.tickProgress = enderDragonEntity.isDead() ? 0.0F : f;
		enderDragonEntityRenderState.frameTracker.copyFrom(enderDragonEntity.frameTracker);
	}

	protected boolean canBeCulled(EnderDragonEntity enderDragonEntity) {
		return false;
	}
}
