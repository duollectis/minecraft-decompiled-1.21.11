package net.minecraft.client.render.block;

import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3fc;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockModelRenderer}.
 */
public class BlockModelRenderer implements FabricBlockModelRenderer {

	private static final Direction[] DIRECTIONS = Direction.values();
	private final BlockColors colors;
	private static final int BRIGHTNESS_CACHE_MAX_SIZE = 100;
	static final ThreadLocal<BlockModelRenderer.BrightnessCache>
			BRIGHTNESS_CACHE =
			ThreadLocal.withInitial(BlockModelRenderer.BrightnessCache::new);

	public BlockModelRenderer(BlockColors colors) {
		this.colors = colors;
	}

	public void render(
			BlockRenderView world,
			List<BlockModelPart> parts,
			BlockState state,
			BlockPos pos,
			MatrixStack matrices,
			VertexConsumer vertexConsumer,
			boolean cull,
			int overlay
	) {
		if (!parts.isEmpty()) {
			boolean
					bl =
					MinecraftClient.isAmbientOcclusionEnabled() && state.getLuminance() == 0 && parts
							.getFirst()
							.useAmbientOcclusion();
			matrices.translate(state.getModelOffset(pos));

			try {
				if (bl) {
					this.renderSmooth(world, parts, state, pos, matrices, vertexConsumer, cull, overlay);
				}
				else {
					this.renderFlat(world, parts, state, pos, matrices, vertexConsumer, cull, overlay);
				}
			}
			catch (Throwable var13) {
				CrashReport crashReport = CrashReport.create(var13, "Tesselating block model");
				CrashReportSection crashReportSection = crashReport.addElement("Block model being tesselated");
				CrashReportSection.addBlockInfo(crashReportSection, world, pos, state);
				crashReportSection.add("Using AO", bl);
				throw new CrashException(crashReport);
			}
		}
	}

	private static boolean shouldDrawFace(
			BlockRenderView world,
			BlockState state,
			boolean cull,
			Direction side,
			BlockPos pos
	) {
		if (!cull) {
			return true;
		}
		else {
			BlockState blockState = world.getBlockState(pos);
			return Block.shouldDrawSide(state, blockState, side);
		}
	}

	public void renderSmooth(
			BlockRenderView world,
			List<BlockModelPart> parts,
			BlockState state,
			BlockPos pos,
			MatrixStack matrices,
			VertexConsumer vertexConsumer,
			boolean cull,
			int overlay
	) {
		BlockModelRenderer.AmbientOcclusionCalculator
				ambientOcclusionCalculator =
				new BlockModelRenderer.AmbientOcclusionCalculator();
		int i = 0;
		int j = 0;

		for (BlockModelPart blockModelPart : parts) {
			for (Direction direction : DIRECTIONS) {
				int k = 1 << direction.ordinal();
				boolean bl = (i & k) == 1;
				boolean bl2 = (j & k) == 1;
				if (!bl || bl2) {
					List<BakedQuad> list = blockModelPart.getQuads(direction);
					if (!list.isEmpty()) {
						if (!bl) {
							bl2 =
									shouldDrawFace(
											world,
											state,
											cull,
											direction,
											ambientOcclusionCalculator.pos.set(pos, direction)
									);
							i |= k;
							if (bl2) {
								j |= k;
							}
						}

						if (bl2) {
							this.renderQuadsSmooth(
									world,
									state,
									pos,
									matrices,
									vertexConsumer,
									list,
									ambientOcclusionCalculator,
									overlay
							);
						}
					}
				}
			}

			List<BakedQuad> list2 = blockModelPart.getQuads(null);
			if (!list2.isEmpty()) {
				this.renderQuadsSmooth(
						world,
						state,
						pos,
						matrices,
						vertexConsumer,
						list2,
						ambientOcclusionCalculator,
						overlay
				);
			}
		}
	}

	public void renderFlat(
			BlockRenderView world,
			List<BlockModelPart> parts,
			BlockState state,
			BlockPos pos,
			MatrixStack matrices,
			VertexConsumer vertexConsumer,
			boolean cull,
			int overlay
	) {
		BlockModelRenderer.LightmapCache lightmapCache = new BlockModelRenderer.LightmapCache();
		int i = 0;
		int j = 0;

		for (BlockModelPart blockModelPart : parts) {
			for (Direction direction : DIRECTIONS) {
				int k = 1 << direction.ordinal();
				boolean bl = (i & k) == 1;
				boolean bl2 = (j & k) == 1;
				if (!bl || bl2) {
					List<BakedQuad> list = blockModelPart.getQuads(direction);
					if (!list.isEmpty()) {
						BlockPos blockPos = lightmapCache.pos.set(pos, direction);
						if (!bl) {
							bl2 = shouldDrawFace(world, state, cull, direction, blockPos);
							i |= k;
							if (bl2) {
								j |= k;
							}
						}

						if (bl2) {
							int l = lightmapCache.brightnessCache.getInt(state, world, blockPos);
							this.renderQuadsFlat(
									world,
									state,
									pos,
									l,
									overlay,
									false,
									matrices,
									vertexConsumer,
									list,
									lightmapCache
							);
						}
					}
				}
			}

			List<BakedQuad> list2 = blockModelPart.getQuads(null);
			if (!list2.isEmpty()) {
				this.renderQuadsFlat(
						world,
						state,
						pos,
						-1,
						overlay,
						true,
						matrices,
						vertexConsumer,
						list2,
						lightmapCache
				);
			}
		}
	}

	private void renderQuadsSmooth(
			BlockRenderView world,
			BlockState state,
			BlockPos pos,
			MatrixStack matrices,
			VertexConsumer vertexConsumer,
			List<BakedQuad> quads,
			BlockModelRenderer.AmbientOcclusionCalculator ambientOcclusionCalculator,
			int overlay
	) {
		for (BakedQuad bakedQuad : quads) {
			getQuadDimensions(world, state, pos, bakedQuad, ambientOcclusionCalculator);
			ambientOcclusionCalculator.apply(world, state, pos, bakedQuad.face(), bakedQuad.shade());
			this.renderQuad(
					world,
					state,
					pos,
					vertexConsumer,
					matrices.peek(),
					bakedQuad,
					ambientOcclusionCalculator,
					overlay
			);
		}
	}

	private void renderQuad(
			BlockRenderView world,
			BlockState state,
			BlockPos pos,
			VertexConsumer vertexConsumer,
			MatrixStack.Entry matrixEntry,
			BakedQuad quad,
			BlockModelRenderer.LightmapCache lightmap,
			int light
	) {
		int i = quad.tintIndex();
		float f;
		float g;
		float h;
		if (i != -1) {
			int j;
			if (lightmap.lastTintIndex == i) {
				j = lightmap.colorOfLastTintIndex;
			}
			else {
				j = this.colors.getColor(state, world, pos, i);
				lightmap.lastTintIndex = i;
				lightmap.colorOfLastTintIndex = j;
			}

			f = ColorHelper.getRedFloat(j);
			g = ColorHelper.getGreenFloat(j);
			h = ColorHelper.getBlueFloat(j);
		}
		else {
			f = 1.0F;
			g = 1.0F;
			h = 1.0F;
		}

		vertexConsumer.quad(matrixEntry, quad, lightmap.fs, f, g, h, 1.0F, lightmap.is, light);
	}

	private static void getQuadDimensions(
			BlockRenderView world,
			BlockState state,
			BlockPos pos,
			BakedQuad bakedQuad,
			BlockModelRenderer.LightmapCache lightmapCache
	) {
		float f = 32.0F;
		float g = 32.0F;
		float h = 32.0F;
		float i = -32.0F;
		float j = -32.0F;
		float k = -32.0F;

		for (int l = 0; l < 4; l++) {
			Vector3fc vector3fc = bakedQuad.getPosition(l);
			float m = vector3fc.x();
			float n = vector3fc.y();
			float o = vector3fc.z();
			f = Math.min(f, m);
			g = Math.min(g, n);
			h = Math.min(h, o);
			i = Math.max(i, m);
			j = Math.max(j, n);
			k = Math.max(k, o);
		}

		if (lightmapCache instanceof BlockModelRenderer.AmbientOcclusionCalculator ambientOcclusionCalculator) {
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.WEST.index] = f;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.EAST.index] = i;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.DOWN.index] = g;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.UP.index] = j;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.NORTH.index] = h;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.SOUTH.index] = k;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.FLIP_WEST.index] = 1.0F - f;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.FLIP_EAST.index] = 1.0F - i;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.FLIP_DOWN.index] = 1.0F - g;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.FLIP_UP.index] = 1.0F - j;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.FLIP_NORTH.index] = 1.0F - h;
			ambientOcclusionCalculator.aoWeights[BlockModelRenderer.NeighborOrientation.FLIP_SOUTH.index] = 1.0F - k;
		}

		float p = 1.0E-4F;
		float q = 0.9999F;

		lightmapCache.useNonCubicWeights = switch (bakedQuad.face()) {
			case DOWN, UP -> f >= 1.0E-4F || h >= 1.0E-4F || i <= 0.9999F || k <= 0.9999F;
			case NORTH, SOUTH -> f >= 1.0E-4F || g >= 1.0E-4F || i <= 0.9999F || j <= 0.9999F;
			case WEST, EAST -> g >= 1.0E-4F || h >= 1.0E-4F || j <= 0.9999F || k <= 0.9999F;
		};

		lightmapCache.isFaceOffset = switch (bakedQuad.face()) {
			case DOWN -> g == j && (g < 1.0E-4F || state.isFullCube(world, pos));
			case UP -> g == j && (j > 0.9999F || state.isFullCube(world, pos));
			case NORTH -> h == k && (h < 1.0E-4F || state.isFullCube(world, pos));
			case SOUTH -> h == k && (k > 0.9999F || state.isFullCube(world, pos));
			case WEST -> f == i && (f < 1.0E-4F || state.isFullCube(world, pos));
			case EAST -> f == i && (i > 0.9999F || state.isFullCube(world, pos));
		};
	}

	private void renderQuadsFlat(
			BlockRenderView world,
			BlockState state,
			BlockPos pos,
			int light,
			int overlay,
			boolean useWorldLight,
			MatrixStack matrices,
			VertexConsumer vertexConsumer,
			List<BakedQuad> quads,
			BlockModelRenderer.LightmapCache lightmap
	) {
		for (BakedQuad bakedQuad : quads) {
			if (useWorldLight) {
				getQuadDimensions(world, state, pos, bakedQuad, lightmap);
				BlockPos blockPos = (BlockPos) (lightmap.isFaceOffset ? lightmap.pos.set(pos, bakedQuad.face()) : pos);
				light = lightmap.brightnessCache.getInt(state, world, blockPos);
			}

			float f = world.getBrightness(bakedQuad.face(), bakedQuad.shade());
			lightmap.fs[0] = f;
			lightmap.fs[1] = f;
			lightmap.fs[2] = f;
			lightmap.fs[3] = f;
			lightmap.is[0] = light;
			lightmap.is[1] = light;
			lightmap.is[2] = light;
			lightmap.is[3] = light;
			this.renderQuad(world, state, pos, vertexConsumer, matrices.peek(), bakedQuad, lightmap, overlay);
		}
	}

	public static void render(
			MatrixStack.Entry entry,
			VertexConsumer vertexConsumer,
			BlockStateModel model,
			float red,
			float green,
			float blue,
			int light,
			int overlay
	) {
		for (BlockModelPart blockModelPart : model.getParts(Random.create(42L))) {
			for (Direction direction : DIRECTIONS) {
				renderQuads(
						entry,
						vertexConsumer,
						red,
						green,
						blue,
						blockModelPart.getQuads(direction),
						light,
						overlay
				);
			}

			renderQuads(entry, vertexConsumer, red, green, blue, blockModelPart.getQuads(null), light, overlay);
		}
	}

	private static void renderQuads(
			MatrixStack.Entry entry,
			VertexConsumer vertexConsumer,
			float red,
			float green,
			float blue,
			List<BakedQuad> quads,
			int light,
			int overlay
	) {
		for (BakedQuad bakedQuad : quads) {
			float f;
			float g;
			float h;
			if (bakedQuad.hasTint()) {
				f = MathHelper.clamp(red, 0.0F, 1.0F);
				g = MathHelper.clamp(green, 0.0F, 1.0F);
				h = MathHelper.clamp(blue, 0.0F, 1.0F);
			}
			else {
				f = 1.0F;
				g = 1.0F;
				h = 1.0F;
			}

			vertexConsumer.quad(entry, bakedQuad, f, g, h, 1.0F, light, overlay);
		}
	}

	/**
	 * Включает brightness cache.
	 */
	public static void enableBrightnessCache() {
		BRIGHTNESS_CACHE.get().enable();
	}

	/**
	 * Отключает brightness cache.
	 */
	public static void disableBrightnessCache() {
		BRIGHTNESS_CACHE.get().disable();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AmbientOcclusionCalculator}.
	 */
	static class AmbientOcclusionCalculator extends BlockModelRenderer.LightmapCache {

		final float[] aoWeights = new float[BlockModelRenderer.NeighborOrientation.SIZE];

		public AmbientOcclusionCalculator() {
		}

		/**
		 * Apply.
		 *
		 * @param world world
		 * @param state state
		 * @param pos pos
		 * @param direction direction
		 * @param bl bl
		 */
		public void apply(BlockRenderView world, BlockState state, BlockPos pos, Direction direction, boolean bl) {
			BlockPos blockPos = this.isFaceOffset ? pos.offset(direction) : pos;
			BlockModelRenderer.NeighborData neighborData = BlockModelRenderer.NeighborData.getData(direction);
			BlockPos.Mutable mutable = this.pos;
			mutable.set(blockPos, neighborData.faces[0]);
			BlockState blockState = world.getBlockState(mutable);
			int i = this.brightnessCache.getInt(blockState, world, mutable);
			float f = this.brightnessCache.getFloat(blockState, world, mutable);
			mutable.set(blockPos, neighborData.faces[1]);
			BlockState blockState2 = world.getBlockState(mutable);
			int j = this.brightnessCache.getInt(blockState2, world, mutable);
			float g = this.brightnessCache.getFloat(blockState2, world, mutable);
			mutable.set(blockPos, neighborData.faces[2]);
			BlockState blockState3 = world.getBlockState(mutable);
			int k = this.brightnessCache.getInt(blockState3, world, mutable);
			float h = this.brightnessCache.getFloat(blockState3, world, mutable);
			mutable.set(blockPos, neighborData.faces[3]);
			BlockState blockState4 = world.getBlockState(mutable);
			int l = this.brightnessCache.getInt(blockState4, world, mutable);
			float m = this.brightnessCache.getFloat(blockState4, world, mutable);
			BlockState blockState5 = world.getBlockState(mutable.set(blockPos, neighborData.faces[0]).move(direction));
			boolean bl2 = !blockState5.shouldBlockVision(world, mutable) || blockState5.getOpacity() == 0;
			BlockState blockState6 = world.getBlockState(mutable.set(blockPos, neighborData.faces[1]).move(direction));
			boolean bl3 = !blockState6.shouldBlockVision(world, mutable) || blockState6.getOpacity() == 0;
			BlockState blockState7 = world.getBlockState(mutable.set(blockPos, neighborData.faces[2]).move(direction));
			boolean bl4 = !blockState7.shouldBlockVision(world, mutable) || blockState7.getOpacity() == 0;
			BlockState blockState8 = world.getBlockState(mutable.set(blockPos, neighborData.faces[3]).move(direction));
			boolean bl5 = !blockState8.shouldBlockVision(world, mutable) || blockState8.getOpacity() == 0;
			float n;
			int o;
			if (!bl4 && !bl2) {
				n = f;
				o = i;
			}
			else {
				mutable.set(blockPos, neighborData.faces[0]).move(neighborData.faces[2]);
				BlockState blockState9 = world.getBlockState(mutable);
				n = this.brightnessCache.getFloat(blockState9, world, mutable);
				o = this.brightnessCache.getInt(blockState9, world, mutable);
			}

			float p;
			int q;
			if (!bl5 && !bl2) {
				p = f;
				q = i;
			}
			else {
				mutable.set(blockPos, neighborData.faces[0]).move(neighborData.faces[3]);
				BlockState blockState9 = world.getBlockState(mutable);
				p = this.brightnessCache.getFloat(blockState9, world, mutable);
				q = this.brightnessCache.getInt(blockState9, world, mutable);
			}

			float r;
			int s;
			if (!bl4 && !bl3) {
				r = f;
				s = i;
			}
			else {
				mutable.set(blockPos, neighborData.faces[1]).move(neighborData.faces[2]);
				BlockState blockState9 = world.getBlockState(mutable);
				r = this.brightnessCache.getFloat(blockState9, world, mutable);
				s = this.brightnessCache.getInt(blockState9, world, mutable);
			}

			float t;
			int u;
			if (!bl5 && !bl3) {
				t = f;
				u = i;
			}
			else {
				mutable.set(blockPos, neighborData.faces[1]).move(neighborData.faces[3]);
				BlockState blockState9 = world.getBlockState(mutable);
				t = this.brightnessCache.getFloat(blockState9, world, mutable);
				u = this.brightnessCache.getInt(blockState9, world, mutable);
			}

			int v = this.brightnessCache.getInt(state, world, pos);
			mutable.set(pos, direction);
			BlockState blockState10 = world.getBlockState(mutable);
			if (this.isFaceOffset || !blockState10.isOpaqueFullCube()) {
				v = this.brightnessCache.getInt(blockState10, world, mutable);
			}

			float w = this.isFaceOffset
			          ? this.brightnessCache.getFloat(world.getBlockState(blockPos), world, blockPos)
			          : this.brightnessCache.getFloat(world.getBlockState(pos), world, pos);
			BlockModelRenderer.Translation translation = BlockModelRenderer.Translation.getTranslations(direction);
			if (this.useNonCubicWeights && neighborData.nonCubicWeight) {
				float x = (m + f + p + w) * 0.25F;
				float y = (h + f + n + w) * 0.25F;
				float z = (h + g + r + w) * 0.25F;
				float aa = (m + g + t + w) * 0.25F;
				float
						ab =
						this.aoWeights[neighborData.firstCornerOrientations[0].index]
								* this.aoWeights[neighborData.firstCornerOrientations[1].index];
				float
						ac =
						this.aoWeights[neighborData.firstCornerOrientations[2].index]
								* this.aoWeights[neighborData.firstCornerOrientations[3].index];
				float
						ad =
						this.aoWeights[neighborData.firstCornerOrientations[4].index]
								* this.aoWeights[neighborData.firstCornerOrientations[5].index];
				float
						ae =
						this.aoWeights[neighborData.firstCornerOrientations[6].index]
								* this.aoWeights[neighborData.firstCornerOrientations[7].index];
				float
						af =
						this.aoWeights[neighborData.secondCornerOrientations[0].index]
								* this.aoWeights[neighborData.secondCornerOrientations[1].index];
				float
						ag =
						this.aoWeights[neighborData.secondCornerOrientations[2].index]
								* this.aoWeights[neighborData.secondCornerOrientations[3].index];
				float
						ah =
						this.aoWeights[neighborData.secondCornerOrientations[4].index]
								* this.aoWeights[neighborData.secondCornerOrientations[5].index];
				float
						ai =
						this.aoWeights[neighborData.secondCornerOrientations[6].index]
								* this.aoWeights[neighborData.secondCornerOrientations[7].index];
				float
						aj =
						this.aoWeights[neighborData.thirdCornerOrientations[0].index]
								* this.aoWeights[neighborData.thirdCornerOrientations[1].index];
				float
						ak =
						this.aoWeights[neighborData.thirdCornerOrientations[2].index]
								* this.aoWeights[neighborData.thirdCornerOrientations[3].index];
				float
						al =
						this.aoWeights[neighborData.thirdCornerOrientations[4].index]
								* this.aoWeights[neighborData.thirdCornerOrientations[5].index];
				float
						am =
						this.aoWeights[neighborData.thirdCornerOrientations[6].index]
								* this.aoWeights[neighborData.thirdCornerOrientations[7].index];
				float
						an =
						this.aoWeights[neighborData.fourthCornerOrientations[0].index]
								* this.aoWeights[neighborData.fourthCornerOrientations[1].index];
				float
						ao =
						this.aoWeights[neighborData.fourthCornerOrientations[2].index]
								* this.aoWeights[neighborData.fourthCornerOrientations[3].index];
				float
						ap =
						this.aoWeights[neighborData.fourthCornerOrientations[4].index]
								* this.aoWeights[neighborData.fourthCornerOrientations[5].index];
				float
						aq =
						this.aoWeights[neighborData.fourthCornerOrientations[6].index]
								* this.aoWeights[neighborData.fourthCornerOrientations[7].index];
				this.fs[translation.firstCorner] = Math.clamp(x * ab + y * ac + z * ad + aa * ae, 0.0F, 1.0F);
				this.fs[translation.secondCorner] = Math.clamp(x * af + y * ag + z * ah + aa * ai, 0.0F, 1.0F);
				this.fs[translation.thirdCorner] = Math.clamp(x * aj + y * ak + z * al + aa * am, 0.0F, 1.0F);
				this.fs[translation.fourthCorner] = Math.clamp(x * an + y * ao + z * ap + aa * aq, 0.0F, 1.0F);
				int ar = getAmbientOcclusionBrightness(l, i, q, v);
				int as = getAmbientOcclusionBrightness(k, i, o, v);
				int at = getAmbientOcclusionBrightness(k, j, s, v);
				int au = getAmbientOcclusionBrightness(l, j, u, v);
				this.is[translation.firstCorner] = getBrightness(ar, as, at, au, ab, ac, ad, ae);
				this.is[translation.secondCorner] = getBrightness(ar, as, at, au, af, ag, ah, ai);
				this.is[translation.thirdCorner] = getBrightness(ar, as, at, au, aj, ak, al, am);
				this.is[translation.fourthCorner] = getBrightness(ar, as, at, au, an, ao, ap, aq);
			}
			else {
				float x = (m + f + p + w) * 0.25F;
				float y = (h + f + n + w) * 0.25F;
				float z = (h + g + r + w) * 0.25F;
				float aa = (m + g + t + w) * 0.25F;
				this.is[translation.firstCorner] = getAmbientOcclusionBrightness(l, i, q, v);
				this.is[translation.secondCorner] = getAmbientOcclusionBrightness(k, i, o, v);
				this.is[translation.thirdCorner] = getAmbientOcclusionBrightness(k, j, s, v);
				this.is[translation.fourthCorner] = getAmbientOcclusionBrightness(l, j, u, v);
				this.fs[translation.firstCorner] = x;
				this.fs[translation.secondCorner] = y;
				this.fs[translation.thirdCorner] = z;
				this.fs[translation.fourthCorner] = aa;
			}

			float x = world.getBrightness(direction, bl);

			for (int av = 0; av < this.fs.length; av++) {
				this.fs[av] = this.fs[av] * x;
			}
		}

		private static int getAmbientOcclusionBrightness(int i, int j, int k, int l) {
			if (i == 0) {
				i = l;
			}

			if (j == 0) {
				j = l;
			}

			if (k == 0) {
				k = l;
			}

			return i + j + k + l >> 2 & 16711935;
		}

		private static int getBrightness(int i, int j, int k, int l, float f, float g, float h, float m) {
			int
					n =
					(int) ((i >> 16 & 0xFF) * f + (j >> 16 & 0xFF) * g + (k >> 16 & 0xFF) * h + (l >> 16 & 0xFF) * m)
							& 0xFF;
			int o = (int) ((i & 0xFF) * f + (j & 0xFF) * g + (k & 0xFF) * h + (l & 0xFF) * m) & 0xFF;
			return n << 16 | o;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BrightnessCache}.
	 */
	static class BrightnessCache {

		private boolean enabled;
		private final Long2IntLinkedOpenHashMap intCache = Util.make(() -> {
			Long2IntLinkedOpenHashMap long2IntLinkedOpenHashMap = new Long2IntLinkedOpenHashMap(BRIGHTNESS_CACHE_MAX_SIZE, 0.25F) {
				/**
				 * Rehash.
				 *
				 * @param newN new n
				 */
				protected void rehash(int newN) {
				}
			};
			long2IntLinkedOpenHashMap.defaultReturnValue(Integer.MAX_VALUE);
			return long2IntLinkedOpenHashMap;
		});
		private final Long2FloatLinkedOpenHashMap floatCache = Util.make(() -> {
			Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(BRIGHTNESS_CACHE_MAX_SIZE, 0.25F) {
				/**
				 * Rehash.
				 *
				 * @param newN new n
				 */
				protected void rehash(int newN) {
				}
			};
			long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
			return long2FloatLinkedOpenHashMap;
		});
		private final WorldRenderer.BrightnessGetter brightnessCache = (world, pos) -> {
			long l = pos.asLong();
			int i = this.intCache.get(l);
			if (i != Integer.MAX_VALUE) {
				return i;
			}
			else {
				int j = WorldRenderer.BrightnessGetter.DEFAULT.packedBrightness(world, pos);
				if (this.intCache.size() == BRIGHTNESS_CACHE_MAX_SIZE) {
					this.intCache.removeFirstInt();
				}

				this.intCache.put(l, j);
				return j;
			}
		};

		private BrightnessCache() {
		}

		/**
		 * Enable.
		 */
		public void enable() {
			this.enabled = true;
		}

		/**
		 * Disable.
		 */
		public void disable() {
			this.enabled = false;
			this.intCache.clear();
			this.floatCache.clear();
		}

		public int getInt(BlockState state, BlockRenderView world, BlockPos pos) {
			return WorldRenderer.getLightmapCoordinates(
					this.enabled ? this.brightnessCache : WorldRenderer.BrightnessGetter.DEFAULT,
					world,
					state,
					pos
			);
		}

		public float getFloat(BlockState state, BlockRenderView blockView, BlockPos pos) {
			long l = pos.asLong();
			if (this.enabled) {
				float f = this.floatCache.get(l);
				if (!Float.isNaN(f)) {
					return f;
				}
			}

			float f = state.getAmbientOcclusionLightLevel(blockView, pos);
			if (this.enabled) {
				if (this.floatCache.size() == BRIGHTNESS_CACHE_MAX_SIZE) {
					this.floatCache.removeFirstFloat();
				}

				this.floatCache.put(l, f);
			}

			return f;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LightmapCache}.
	 */
	static class LightmapCache {

		public final BlockPos.Mutable pos = new BlockPos.Mutable();
		public boolean isFaceOffset;
		public boolean useNonCubicWeights;
		public final float[] fs = new float[4];
		public final int[] is = new int[4];
		public int lastTintIndex = -1;
		public int colorOfLastTintIndex;
		public final BlockModelRenderer.BrightnessCache brightnessCache = BlockModelRenderer.BRIGHTNESS_CACHE.get();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code NeighborData}.
	 */
	protected static enum NeighborData {
		DOWN(
				new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
				0.5F,
				true,
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.SOUTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.SOUTH
				}
		),
		UP(
				new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
				1.0F,
				true,
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.SOUTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.SOUTH
				}
		),
		NORTH(
				new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
				0.8F,
				true,
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST
				}
		),
		SOUTH(
				new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
				0.8F,
				true,
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.WEST
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_WEST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.WEST,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.WEST
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.EAST
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_EAST,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.EAST,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.EAST
				}
		),
		WEST(
				new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
				0.6F,
				true,
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.SOUTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.SOUTH
				}
		),
		EAST(
				new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
				0.6F,
				true,
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.SOUTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.DOWN,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.NORTH,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_NORTH,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.NORTH
				},
				new BlockModelRenderer.NeighborOrientation[]{
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.SOUTH,
						BlockModelRenderer.NeighborOrientation.FLIP_UP,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.FLIP_SOUTH,
						BlockModelRenderer.NeighborOrientation.UP,
						BlockModelRenderer.NeighborOrientation.SOUTH
				}
		);

		final Direction[] faces;
		final boolean nonCubicWeight;
		final BlockModelRenderer.NeighborOrientation[] firstCornerOrientations;
		final BlockModelRenderer.NeighborOrientation[] secondCornerOrientations;
		final BlockModelRenderer.NeighborOrientation[] thirdCornerOrientations;
		final BlockModelRenderer.NeighborOrientation[] fourthCornerOrientations;
		private static final BlockModelRenderer.NeighborData[] VALUES = Util.make(
				new BlockModelRenderer.NeighborData[6], values -> {
					values[Direction.DOWN.getIndex()] = DOWN;
					values[Direction.UP.getIndex()] = UP;
					values[Direction.NORTH.getIndex()] = NORTH;
					values[Direction.SOUTH.getIndex()] = SOUTH;
					values[Direction.WEST.getIndex()] = WEST;
					values[Direction.EAST.getIndex()] = EAST;
				}
		);

		private NeighborData(
				final Direction[] faces,
				final float f,
				final boolean nonCubicWeight,
				final BlockModelRenderer.NeighborOrientation[] neighborOrientations,
				final BlockModelRenderer.NeighborOrientation[] neighborOrientations2,
				final BlockModelRenderer.NeighborOrientation[] neighborOrientations3,
				final BlockModelRenderer.NeighborOrientation[] neighborOrientations4
		) {
			this.faces = faces;
			this.nonCubicWeight = nonCubicWeight;
			this.firstCornerOrientations = neighborOrientations;
			this.secondCornerOrientations = neighborOrientations2;
			this.thirdCornerOrientations = neighborOrientations3;
			this.fourthCornerOrientations = neighborOrientations4;
		}

		public static BlockModelRenderer.NeighborData getData(Direction direction) {
			return VALUES[direction.getIndex()];
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code NeighborOrientation}.
	 */
	protected static enum NeighborOrientation {
		DOWN(0),
		UP(1),
		NORTH(2),
		SOUTH(3),
		WEST(4),
		EAST(5),
		FLIP_DOWN(6),
		FLIP_UP(7),
		FLIP_NORTH(8),
		FLIP_SOUTH(9),
		FLIP_WEST(10),
		FLIP_EAST(11);

		public static final int SIZE = values().length;
		final int index;

		private NeighborOrientation(final int index) {
			this.index = index;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Translation}.
	 */
	static enum Translation {
		DOWN(0, 1, 2, 3),
		UP(2, 3, 0, 1),
		NORTH(3, 0, 1, 2),
		SOUTH(0, 1, 2, 3),
		WEST(3, 0, 1, 2),
		EAST(1, 2, 3, 0);

		final int firstCorner;
		final int secondCorner;
		final int thirdCorner;
		final int fourthCorner;
		private static final BlockModelRenderer.Translation[] VALUES = Util.make(
				new BlockModelRenderer.Translation[6], values -> {
					values[Direction.DOWN.getIndex()] = DOWN;
					values[Direction.UP.getIndex()] = UP;
					values[Direction.NORTH.getIndex()] = NORTH;
					values[Direction.SOUTH.getIndex()] = SOUTH;
					values[Direction.WEST.getIndex()] = WEST;
					values[Direction.EAST.getIndex()] = EAST;
				}
		);

		private Translation(
				final int firstCorner,
				final int secondCorner,
				final int thirdCorner,
				final int fourthCorner
		) {
			this.firstCorner = firstCorner;
			this.secondCorner = secondCorner;
			this.thirdCorner = thirdCorner;
			this.fourthCorner = fourthCorner;
		}

		public static BlockModelRenderer.Translation getTranslations(Direction direction) {
			return VALUES[direction.getIndex()];
		}
	}
}
