package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
/**
 * {@code EntityRenderer}.
 */
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {

	private static final float SHADOW_RADIUS_MIN = 0.5F;
	private static final float SHADOW_DISTANCE_MAX = 32.0F;
	public static final float NAME_TAG_SCALE = 0.025F;
	protected final EntityRenderManager dispatcher;
	private final TextRenderer textRenderer;
	protected float shadowRadius;
	protected float shadowOpacity = 1.0F;

	protected EntityRenderer(EntityRendererFactory.Context context) {
		this.dispatcher = context.getRenderDispatcher();
		this.textRenderer = context.getTextRenderer();
	}

	public final int getLight(T entity, float tickProgress) {
		BlockPos blockPos = BlockPos.ofFloored(entity.getClientCameraPosVec(tickProgress));
		return LightmapTextureManager.pack(this.getBlockLight(entity, blockPos), this.getSkyLight(entity, blockPos));
	}

	protected int getSkyLight(T entity, BlockPos pos) {
		return entity.getEntityWorld().getLightLevel(LightType.SKY, pos);
	}

	protected int getBlockLight(T entity, BlockPos pos) {
		return entity.isOnFire() ? 15 : entity.getEntityWorld().getLightLevel(LightType.BLOCK, pos);
	}

	/**
	 * Определяет, следует ли render.
	 *
	 * @param entity entity
	 * @param frustum frustum
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
		if (!entity.shouldRender(x, y, z)) {
			return false;
		}
		else if (!this.canBeCulled(entity)) {
			return true;
		}
		else {
			Box box = this.getBoundingBox(entity).expand(0.5);
			if (box.isNaN() || box.getAverageSideLength() == 0.0) {
				box =
						new Box(
								entity.getX() - 2.0,
								entity.getY() - 2.0,
								entity.getZ() - 2.0,
								entity.getX() + 2.0,
								entity.getY() + 2.0,
								entity.getZ() + 2.0
						);
			}

			if (frustum.isVisible(box)) {
				return true;
			}
			else {
				if (entity instanceof Leashable leashable) {
					Entity entity2 = leashable.getLeashHolder();
					if (entity2 != null) {
						Box box2 = this.dispatcher.getRenderer(entity2).getBoundingBox(entity2);
						return frustum.isVisible(box2) || frustum.isVisible(box.union(box2));
					}
				}

				return false;
			}
		}
	}

	protected Box getBoundingBox(T entity) {
		return entity.getBoundingBox();
	}

	/**
	 * Проверяет возможность be culled.
	 *
	 * @param entity entity
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected boolean canBeCulled(T entity) {
		return true;
	}

	public Vec3d getPositionOffset(S state) {
		return state.positionOffset != null ? state.positionOffset : Vec3d.ZERO;
	}

	public void render(
			S renderState,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			CameraRenderState cameraState
	) {
		if (renderState.leashDatas != null) {
			for (EntityRenderState.LeashData leashData : renderState.leashDatas) {
				queue.submitLeash(matrices, leashData);
			}
		}

		this.renderLabelIfPresent(renderState, matrices, queue, cameraState);
	}

	protected boolean hasLabel(T entity, double squaredDistanceToCamera) {
		return entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity;
	}

	public TextRenderer getTextRenderer() {
		return this.textRenderer;
	}

	protected void renderLabelIfPresent(
			S state,
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			CameraRenderState cameraRenderState
	) {
		if (state.displayName != null) {
			queue.submitLabel(
					matrices,
					state.nameLabelPos,
					0,
					state.displayName,
					!state.sneaking,
					state.light,
					state.squaredDistanceToCamera,
					cameraRenderState
			);
		}
	}

	protected @Nullable Text getDisplayName(T entity) {
		return entity.getDisplayName();
	}

	protected float getShadowRadius(S state) {
		return this.shadowRadius;
	}

	protected float getShadowOpacity(S state) {
		return this.shadowOpacity;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return S — результат операции
	 */
	public abstract S createRenderState();

	public final S getAndUpdateRenderState(T entity, float tickProgress) {
		S entityRenderState = this.createRenderState();
		this.updateRenderState(entity, entityRenderState, tickProgress);
		this.updateShadow(entity, entityRenderState);
		return entityRenderState;
	}

	/**
	 * Обновляет render state.
	 *
	 * @param entity entity
	 * @param state state
	 * @param tickProgress tick progress
	 */
	public void updateRenderState(T entity, S state, float tickProgress) {
		state.entityType = entity.getType();
		state.x = MathHelper.lerp((double) tickProgress, entity.lastRenderX, entity.getX());
		state.y = MathHelper.lerp((double) tickProgress, entity.lastRenderY, entity.getY());
		state.z = MathHelper.lerp((double) tickProgress, entity.lastRenderZ, entity.getZ());
		state.invisible = entity.isInvisible();
		state.age = entity.age + tickProgress;
		state.width = entity.getWidth();
		state.height = entity.getHeight();
		state.standingEyeHeight = entity.getStandingEyeHeight();
		if (entity.hasVehicle()
				&& entity.getVehicle() instanceof AbstractMinecartEntity abstractMinecartEntity
				&& abstractMinecartEntity.getController() instanceof ExperimentalMinecartController experimentalMinecartController
				&& experimentalMinecartController.hasCurrentLerpSteps()) {
			double
					d =
					MathHelper.lerp(
							(double) tickProgress,
							abstractMinecartEntity.lastRenderX,
							abstractMinecartEntity.getX()
					);
			double
					e =
					MathHelper.lerp(
							(double) tickProgress,
							abstractMinecartEntity.lastRenderY,
							abstractMinecartEntity.getY()
					);
			double
					f =
					MathHelper.lerp(
							(double) tickProgress,
							abstractMinecartEntity.lastRenderZ,
							abstractMinecartEntity.getZ()
					);
			state.positionOffset =
					experimentalMinecartController.getLerpedPosition(tickProgress).subtract(new Vec3d(d, e, f));
		}
		else {
			state.positionOffset = null;
		}

		if (this.dispatcher.camera != null) {
			state.squaredDistanceToCamera = this.dispatcher.getSquaredDistanceToCamera(entity);
			boolean bl = state.squaredDistanceToCamera < 4096.0 && this.hasLabel(entity, state.squaredDistanceToCamera);
			if (bl) {
				state.displayName = this.getDisplayName(entity);
				state.nameLabelPos =
						entity
								.getAttachments()
								.getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getLerpedYaw(tickProgress));
			}
			else {
				state.displayName = null;
			}
		}

		label72:
		{
			state.sneaking = entity.isSneaky();
			World world = entity.getEntityWorld();
			if (entity instanceof Leashable leashable) {
				Entity g = leashable.getLeashHolder();
				if (g instanceof Entity) {
					float gx = entity.lerpYaw(tickProgress) * (float) (Math.PI / 180.0);
					Vec3d vec3d = leashable.getLeashOffset(tickProgress);
					BlockPos blockPos = BlockPos.ofFloored(entity.getCameraPosVec(tickProgress));
					BlockPos blockPos2 = BlockPos.ofFloored(g.getCameraPosVec(tickProgress));
					int i = this.getBlockLight(entity, blockPos);
					int j = this.dispatcher.getRenderer(g).getBlockLight(g, blockPos2);
					int k = world.getLightLevel(LightType.SKY, blockPos);
					int l = world.getLightLevel(LightType.SKY, blockPos2);
					boolean bl2 = g.hasQuadLeashAttachmentPoints() && leashable.canUseQuadLeashAttachmentPoint();
					int m = bl2 ? 4 : 1;
					if (state.leashDatas == null || state.leashDatas.size() != m) {
						state.leashDatas = new ArrayList<>(m);

						for (int n = 0; n < m; n++) {
							state.leashDatas.add(new EntityRenderState.LeashData());
						}
					}

					if (bl2) {
						float h = g.lerpYaw(tickProgress) * (float) (Math.PI / 180.0);
						Vec3d vec3d2 = g.getLerpedPos(tickProgress);
						Vec3d[] vec3ds = leashable.getQuadLeashOffsets();
						Vec3d[] vec3ds2 = g.getHeldQuadLeashOffsets();
						int o = 0;

						while (true) {
							if (o >= m) {
								break label72;
							}

							EntityRenderState.LeashData leashData = state.leashDatas.get(o);
							leashData.offset = vec3ds[o].rotateY(-gx);
							leashData.startPos = entity.getLerpedPos(tickProgress).add(leashData.offset);
							leashData.endPos = vec3d2.add(vec3ds2[o].rotateY(-h));
							leashData.leashedEntityBlockLight = i;
							leashData.leashHolderBlockLight = j;
							leashData.leashedEntitySkyLight = k;
							leashData.leashHolderSkyLight = l;
							leashData.slack = false;
							o++;
						}
					}
					else {
						Vec3d vec3d3 = vec3d.rotateY(-gx);
						EntityRenderState.LeashData leashData2 = state.leashDatas.getFirst();
						leashData2.offset = vec3d3;
						leashData2.startPos = entity.getLerpedPos(tickProgress).add(vec3d3);
						leashData2.endPos = g.getLeashPos(tickProgress);
						leashData2.leashedEntityBlockLight = i;
						leashData2.leashHolderBlockLight = j;
						leashData2.leashedEntitySkyLight = k;
						leashData2.leashHolderSkyLight = l;
						break label72;
					}
				}
			}

			state.leashDatas = null;
		}

		state.onFire = entity.doesRenderOnFire();
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		boolean bl3 = minecraftClient.hasOutline(entity);
		state.outlineColor = bl3 ? ColorHelper.fullAlpha(entity.getTeamColorValue()) : 0;
		state.light = this.getLight(entity, tickProgress);
	}

	/**
	 * Обновляет shadow.
	 *
	 * @param entity entity
	 * @param renderState render state
	 */
	protected void updateShadow(T entity, S renderState) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		World world = entity.getEntityWorld();
		this.updateShadow(renderState, minecraftClient, world);
	}

	private void updateShadow(S renderState, MinecraftClient client, World world) {
		renderState.shadowPieces.clear();
		if (client.options.getEntityShadows().getValue() && !renderState.invisible) {
			float f = Math.min(this.getShadowRadius(renderState), 32.0F);
			renderState.shadowRadius = f;
			if (f > 0.0F) {
				double d = renderState.squaredDistanceToCamera;
				float g = (float) ((1.0 - d / 256.0) * this.getShadowOpacity(renderState));
				if (g > 0.0F) {
					int i = MathHelper.floor(renderState.x - f);
					int j = MathHelper.floor(renderState.x + f);
					int k = MathHelper.floor(renderState.z - f);
					int l = MathHelper.floor(renderState.z + f);
					float h = Math.min(g / 0.5F - 1.0F, f);
					int m = MathHelper.floor(renderState.y - h);
					int n = MathHelper.floor(renderState.y);
					BlockPos.Mutable mutable = new BlockPos.Mutable();

					for (int o = k; o <= l; o++) {
						for (int p = i; p <= j; p++) {
							mutable.set(p, 0, o);
							Chunk chunk = world.getChunk(mutable);

							for (int q = m; q <= n; q++) {
								mutable.setY(q);
								this.addShadowPiece(renderState, world, g, mutable, chunk);
							}
						}
					}
				}
			}
		}
		else {
			renderState.shadowRadius = 0.0F;
		}
	}

	private void addShadowPiece(S renderState, World world, float shadowOpacity, BlockPos.Mutable pos, Chunk chunk) {
		float f = shadowOpacity - (float) (renderState.y - pos.getY()) * 0.5F;
		BlockPos blockPos = pos.down();
		BlockState blockState = chunk.getBlockState(blockPos);
		if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
			int i = world.getLightLevel(pos);
			if (i > 3) {
				if (blockState.isFullCube(chunk, blockPos)) {
					VoxelShape voxelShape = blockState.getOutlineShape(chunk, blockPos);
					if (!voxelShape.isEmpty()) {
						float
								g =
								MathHelper.clamp(
										f * 0.5F * LightmapTextureManager.getBrightness(world.getDimension(), i),
										0.0F,
										1.0F
								);
						float h = (float) (pos.getX() - renderState.x);
						float j = (float) (pos.getY() - renderState.y);
						float k = (float) (pos.getZ() - renderState.z);
						renderState.shadowPieces.add(new EntityRenderState.ShadowPiece(h, j, k, voxelShape, g));
					}
				}
			}
		}
	}

	private static @Nullable Entity getServerEntity(Entity clientEntity) {
		IntegratedServer integratedServer = MinecraftClient.getInstance().getServer();
		if (integratedServer != null) {
			ServerWorld serverWorld = integratedServer.getWorld(clientEntity.getEntityWorld().getRegistryKey());
			if (serverWorld != null) {
				return serverWorld.getEntityById(clientEntity.getId());
			}
		}

		return null;
	}
}
