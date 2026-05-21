package net.minecraft.client.render.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;

@Environment(EnvType.CLIENT)
/**
 * {@code FluidRenderer}.
 */
public class FluidRenderer {

	private static final float FLUID_HEIGHT = 0.8888889F;
	private final Sprite lavaStillSprite;
	private final Sprite lavaFlowSprite;
	private final Sprite waterStillSprite;
	private final Sprite waterFlowSprite;
	private final Sprite waterOverlaySprite;

	public FluidRenderer(SpriteHolder spriteHolder) {
		this.lavaStillSprite = spriteHolder.getSprite(ModelBaker.LAVA_STILL);
		this.lavaFlowSprite = spriteHolder.getSprite(ModelBaker.LAVA_FLOW);
		this.waterStillSprite = spriteHolder.getSprite(ModelBaker.WATER_STILL);
		this.waterFlowSprite = spriteHolder.getSprite(ModelBaker.WATER_FLOW);
		this.waterOverlaySprite = spriteHolder.getSprite(ModelBaker.WATER_OVERLAY);
	}

	private static boolean isSameFluid(FluidState a, FluidState b) {
		return b.getFluid().matchesType(a.getFluid());
	}

	private static boolean isSideCovered(Direction side, float height, BlockState state) {
		VoxelShape voxelShape = state.getCullingFace(side.getOpposite());
		if (voxelShape == VoxelShapes.empty()) {
			return false;
		}
		else if (voxelShape == VoxelShapes.fullCube()) {
			boolean bl = height == 1.0F;
			return side != Direction.UP || bl;
		}
		else {
			VoxelShape voxelShape2 = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, height, 1.0);
			return VoxelShapes.isSideCovered(voxelShape2, voxelShape, side);
		}
	}

	private static boolean shouldSkipRendering(Direction side, float height, BlockState state) {
		return isSideCovered(side, height, state);
	}

	private static boolean isOppositeSideCovered(BlockState state, Direction side) {
		return isSideCovered(side.getOpposite(), 1.0F, state);
	}

	public static boolean shouldRenderSide(
			FluidState fluid,
			BlockState state,
			Direction side,
			FluidState fluidFromSide
	) {
		return !isOppositeSideCovered(state, side) && !isSameFluid(fluid, fluidFromSide);
	}

	public void render(
			BlockRenderView world,
			BlockPos pos,
			VertexConsumer vertexConsumer,
			BlockState blockState,
			FluidState fluidState
	) {
		boolean bl = fluidState.isIn(FluidTags.LAVA);
		Sprite sprite = bl ? this.lavaStillSprite : this.waterStillSprite;
		Sprite sprite2 = bl ? this.lavaFlowSprite : this.waterFlowSprite;
		int i = bl ? 16777215 : BiomeColors.getWaterColor(world, pos);
		float f = (i >> 16 & 0xFF) / 255.0F;
		float g = (i >> 8 & 0xFF) / 255.0F;
		float h = (i & 0xFF) / 255.0F;
		BlockState blockState2 = world.getBlockState(pos.offset(Direction.DOWN));
		FluidState fluidState2 = blockState2.getFluidState();
		BlockState blockState3 = world.getBlockState(pos.offset(Direction.UP));
		FluidState fluidState3 = blockState3.getFluidState();
		BlockState blockState4 = world.getBlockState(pos.offset(Direction.NORTH));
		FluidState fluidState4 = blockState4.getFluidState();
		BlockState blockState5 = world.getBlockState(pos.offset(Direction.SOUTH));
		FluidState fluidState5 = blockState5.getFluidState();
		BlockState blockState6 = world.getBlockState(pos.offset(Direction.WEST));
		FluidState fluidState6 = blockState6.getFluidState();
		BlockState blockState7 = world.getBlockState(pos.offset(Direction.EAST));
		FluidState fluidState7 = blockState7.getFluidState();
		boolean bl2 = !isSameFluid(fluidState, fluidState3);
		boolean
				bl3 =
				shouldRenderSide(fluidState, blockState, Direction.DOWN, fluidState2)
						&& !shouldSkipRendering(Direction.DOWN, 0.8888889F, blockState2);
		boolean bl4 = shouldRenderSide(fluidState, blockState, Direction.NORTH, fluidState4);
		boolean bl5 = shouldRenderSide(fluidState, blockState, Direction.SOUTH, fluidState5);
		boolean bl6 = shouldRenderSide(fluidState, blockState, Direction.WEST, fluidState6);
		boolean bl7 = shouldRenderSide(fluidState, blockState, Direction.EAST, fluidState7);
		if (bl2 || bl3 || bl7 || bl6 || bl4 || bl5) {
			float j = world.getBrightness(Direction.DOWN, true);
			float k = world.getBrightness(Direction.UP, true);
			float l = world.getBrightness(Direction.NORTH, true);
			float m = world.getBrightness(Direction.WEST, true);
			Fluid fluid = fluidState.getFluid();
			float n = this.getFluidHeight(world, fluid, pos, blockState, fluidState);
			float o;
			float p;
			float q;
			float r;
			if (n >= 1.0F) {
				o = 1.0F;
				p = 1.0F;
				q = 1.0F;
				r = 1.0F;
			}
			else {
				float s = this.getFluidHeight(world, fluid, pos.north(), blockState4, fluidState4);
				float t = this.getFluidHeight(world, fluid, pos.south(), blockState5, fluidState5);
				float u = this.getFluidHeight(world, fluid, pos.east(), blockState7, fluidState7);
				float v = this.getFluidHeight(world, fluid, pos.west(), blockState6, fluidState6);
				o =
						this.calculateFluidHeight(
								world,
								fluid,
								n,
								s,
								u,
								pos.offset(Direction.NORTH).offset(Direction.EAST)
						);
				p =
						this.calculateFluidHeight(
								world,
								fluid,
								n,
								s,
								v,
								pos.offset(Direction.NORTH).offset(Direction.WEST)
						);
				q =
						this.calculateFluidHeight(
								world,
								fluid,
								n,
								t,
								u,
								pos.offset(Direction.SOUTH).offset(Direction.EAST)
						);
				r =
						this.calculateFluidHeight(
								world,
								fluid,
								n,
								t,
								v,
								pos.offset(Direction.SOUTH).offset(Direction.WEST)
						);
			}

			float s = pos.getX() & 15;
			float t = pos.getY() & 15;
			float u = pos.getZ() & 15;
			float v = 0.001F;
			float w = bl3 ? 0.001F : 0.0F;
			if (bl2 && !shouldSkipRendering(Direction.UP, Math.min(Math.min(p, r), Math.min(q, o)), blockState3)) {
				p -= 0.001F;
				r -= 0.001F;
				q -= 0.001F;
				o -= 0.001F;
				Vec3d vec3d = fluidState.getVelocity(world, pos);
				float x;
				float z;
				float ab;
				float ad;
				float y;
				float aa;
				float ac;
				float ae;
				if (vec3d.x == 0.0 && vec3d.z == 0.0) {
					x = sprite.getFrameU(0.0F);
					y = sprite.getFrameV(0.0F);
					z = x;
					aa = sprite.getFrameV(1.0F);
					ab = sprite.getFrameU(1.0F);
					ac = aa;
					ad = ab;
					ae = y;
				}
				else {
					float af = (float) MathHelper.atan2(vec3d.z, vec3d.x) - (float) (Math.PI / 2);
					float ag = MathHelper.sin(af) * 0.25F;
					float ah = MathHelper.cos(af) * 0.25F;
					float ai = 0.5F;
					x = sprite2.getFrameU(0.5F + (-ah - ag));
					y = sprite2.getFrameV(0.5F + (-ah + ag));
					z = sprite2.getFrameU(0.5F + (-ah + ag));
					aa = sprite2.getFrameV(0.5F + (ah + ag));
					ab = sprite2.getFrameU(0.5F + (ah + ag));
					ac = sprite2.getFrameV(0.5F + (ah - ag));
					ad = sprite2.getFrameU(0.5F + (ah - ag));
					ae = sprite2.getFrameV(0.5F + (-ah - ag));
				}

				int aj = this.getLight(world, pos);
				float ag = k * f;
				float ah = k * g;
				float ai = k * h;
				this.vertex(vertexConsumer, s + 0.0F, t + p, u + 0.0F, ag, ah, ai, x, y, aj);
				this.vertex(vertexConsumer, s + 0.0F, t + r, u + 1.0F, ag, ah, ai, z, aa, aj);
				this.vertex(vertexConsumer, s + 1.0F, t + q, u + 1.0F, ag, ah, ai, ab, ac, aj);
				this.vertex(vertexConsumer, s + 1.0F, t + o, u + 0.0F, ag, ah, ai, ad, ae, aj);
				if (fluidState.canFlowTo(world, pos.up())) {
					this.vertex(vertexConsumer, s + 0.0F, t + p, u + 0.0F, ag, ah, ai, x, y, aj);
					this.vertex(vertexConsumer, s + 1.0F, t + o, u + 0.0F, ag, ah, ai, ad, ae, aj);
					this.vertex(vertexConsumer, s + 1.0F, t + q, u + 1.0F, ag, ah, ai, ab, ac, aj);
					this.vertex(vertexConsumer, s + 0.0F, t + r, u + 1.0F, ag, ah, ai, z, aa, aj);
				}
			}

			if (bl3) {
				float xx = sprite.getMinU();
				float zx = sprite.getMaxU();
				float abx = sprite.getMinV();
				float adx = sprite.getMaxV();
				int ak = this.getLight(world, pos.down());
				float aax = j * f;
				float acx = j * g;
				float aex = j * h;
				this.vertex(vertexConsumer, s, t + w, u + 1.0F, aax, acx, aex, xx, adx, ak);
				this.vertex(vertexConsumer, s, t + w, u, aax, acx, aex, xx, abx, ak);
				this.vertex(vertexConsumer, s + 1.0F, t + w, u, aax, acx, aex, zx, abx, ak);
				this.vertex(vertexConsumer, s + 1.0F, t + w, u + 1.0F, aax, acx, aex, zx, adx, ak);
			}

			int al = this.getLight(world, pos);

			for (Direction direction : Direction.Type.HORIZONTAL) {
				float adx;
				float yx;
				float aax;
				float acx;
				float aex;
				float am;
				boolean bl8;
				switch (direction) {
					case NORTH:
						adx = p;
						yx = o;
						aax = s;
						aex = s + 1.0F;
						acx = u + 0.001F;
						am = u + 0.001F;
						bl8 = bl4;
						break;
					case SOUTH:
						adx = q;
						yx = r;
						aax = s + 1.0F;
						aex = s;
						acx = u + 1.0F - 0.001F;
						am = u + 1.0F - 0.001F;
						bl8 = bl5;
						break;
					case WEST:
						adx = r;
						yx = p;
						aax = s + 0.001F;
						aex = s + 0.001F;
						acx = u + 1.0F;
						am = u;
						bl8 = bl6;
						break;
					default:
						adx = o;
						yx = q;
						aax = s + 1.0F - 0.001F;
						aex = s + 1.0F - 0.001F;
						acx = u;
						am = u + 1.0F;
						bl8 = bl7;
				}

				if (bl8 && !shouldSkipRendering(
						direction,
						Math.max(adx, yx),
						world.getBlockState(pos.offset(direction))
				)) {
					BlockPos blockPos = pos.offset(direction);
					Sprite sprite3 = sprite2;
					if (!bl) {
						Block block = world.getBlockState(blockPos).getBlock();
						if (block instanceof TranslucentBlock || block instanceof LeavesBlock) {
							sprite3 = this.waterOverlaySprite;
						}
					}

					float ai = sprite3.getFrameU(0.0F);
					float an = sprite3.getFrameU(0.5F);
					float ao = sprite3.getFrameV((1.0F - adx) * 0.5F);
					float ap = sprite3.getFrameV((1.0F - yx) * 0.5F);
					float aq = sprite3.getFrameV(0.5F);
					float ar = direction.getAxis() == Direction.Axis.Z ? l : m;
					float as = k * ar * f;
					float at = k * ar * g;
					float au = k * ar * h;
					this.vertex(vertexConsumer, aax, t + adx, acx, as, at, au, ai, ao, al);
					this.vertex(vertexConsumer, aex, t + yx, am, as, at, au, an, ap, al);
					this.vertex(vertexConsumer, aex, t + w, am, as, at, au, an, aq, al);
					this.vertex(vertexConsumer, aax, t + w, acx, as, at, au, ai, aq, al);
					if (sprite3 != this.waterOverlaySprite) {
						this.vertex(vertexConsumer, aax, t + w, acx, as, at, au, ai, aq, al);
						this.vertex(vertexConsumer, aex, t + w, am, as, at, au, an, aq, al);
						this.vertex(vertexConsumer, aex, t + yx, am, as, at, au, an, ap, al);
						this.vertex(vertexConsumer, aax, t + adx, acx, as, at, au, ai, ao, al);
					}
				}
			}
		}
	}

	private float calculateFluidHeight(
			BlockRenderView world,
			Fluid fluid,
			float originHeight,
			float northSouthHeight,
			float eastWestHeight,
			BlockPos pos
	) {
		if (!(eastWestHeight >= 1.0F) && !(northSouthHeight >= 1.0F)) {
			float[] fs = new float[2];
			if (eastWestHeight > 0.0F || northSouthHeight > 0.0F) {
				float f = this.getFluidHeight(world, fluid, pos);
				if (f >= 1.0F) {
					return 1.0F;
				}

				this.addHeight(fs, f);
			}

			this.addHeight(fs, originHeight);
			this.addHeight(fs, eastWestHeight);
			this.addHeight(fs, northSouthHeight);
			return fs[0] / fs[1];
		}
		else {
			return 1.0F;
		}
	}

	private void addHeight(float[] weightedAverageHeight, float height) {
		if (height >= 0.8F) {
			weightedAverageHeight[0] += height * 10.0F;
			weightedAverageHeight[1] += 10.0F;
		}
		else if (height >= 0.0F) {
			weightedAverageHeight[0] += height;
			weightedAverageHeight[1]++;
		}
	}

	private float getFluidHeight(BlockRenderView world, Fluid fluid, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return this.getFluidHeight(world, fluid, pos, blockState, blockState.getFluidState());
	}

	private float getFluidHeight(
			BlockRenderView world,
			Fluid fluid,
			BlockPos pos,
			BlockState blockState,
			FluidState fluidState
	) {
		if (fluid.matchesType(fluidState.getFluid())) {
			BlockState blockState2 = world.getBlockState(pos.up());
			return fluid.matchesType(blockState2.getFluidState().getFluid()) ? 1.0F : fluidState.getHeight();
		}
		else {
			return !blockState.isSolid() ? 0.0F : -1.0F;
		}
	}

	private void vertex(
			VertexConsumer vertexConsumer,
			float x,
			float y,
			float z,
			float red,
			float green,
			float blue,
			float u,
			float v,
			int light
	) {
		vertexConsumer
				.vertex(x, y, z)
				.color(red, green, blue, 1.0F)
				.texture(u, v)
				.light(light)
				.normal(0.0F, 1.0F, 0.0F);
	}

	private int getLight(BlockRenderView world, BlockPos pos) {
		int i = WorldRenderer.getLightmapCoordinates(world, pos);
		int j = WorldRenderer.getLightmapCoordinates(world, pos.up());
		int k = i & 0xFF;
		int l = j & 0xFF;
		int m = i >> 16 & 0xFF;
		int n = j >> 16 & 0xFF;
		return (k > l ? k : l) | (m > n ? m : n) << 16;
	}
}
