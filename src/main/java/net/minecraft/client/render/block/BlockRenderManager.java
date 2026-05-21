package net.minecraft.client.render.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockRenderManager;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockRenderManager}.
 */
public class BlockRenderManager implements SynchronousResourceReloader, FabricBlockRenderManager {

	private final BlockModels models;
	private final SpriteHolder spriteHolder;
	private final BlockModelRenderer blockModelRenderer;
	private @Nullable FluidRenderer fluidRenderer;
	private final Random random = Random.create();
	private final List<BlockModelPart> parts = new ArrayList<>();
	private final BlockColors blockColors;

	public BlockRenderManager(BlockModels models, SpriteHolder spriteHolder, BlockColors blockColors) {
		this.models = models;
		this.spriteHolder = spriteHolder;
		this.blockColors = blockColors;
		this.blockModelRenderer = new BlockModelRenderer(this.blockColors);
	}

	public BlockModels getModels() {
		return this.models;
	}

	public void renderDamage(
			BlockState state,
			BlockPos pos,
			BlockRenderView world,
			MatrixStack matrices,
			VertexConsumer vertexConsumer
	) {
		if (state.getRenderType() == BlockRenderType.MODEL) {
			BlockStateModel blockStateModel = this.models.getModel(state);
			this.random.setSeed(state.getRenderingSeed(pos));
			this.parts.clear();
			blockStateModel.addParts(this.random, this.parts);
			this.blockModelRenderer.render(
					world,
					this.parts,
					state,
					pos,
					matrices,
					vertexConsumer,
					true,
					OverlayTexture.DEFAULT_UV
			);
		}
	}

	public void renderBlock(
			BlockState state,
			BlockPos pos,
			BlockRenderView world,
			MatrixStack matrices,
			VertexConsumer vertexConsumer,
			boolean cull,
			List<BlockModelPart> parts
	) {
		try {
			this.blockModelRenderer.render(
					world,
					parts,
					state,
					pos,
					matrices,
					vertexConsumer,
					cull,
					OverlayTexture.DEFAULT_UV
			);
		}
		catch (Throwable var11) {
			CrashReport crashReport = CrashReport.create(var11, "Tesselating block in world");
			CrashReportSection crashReportSection = crashReport.addElement("Block being tesselated");
			CrashReportSection.addBlockInfo(crashReportSection, world, pos, state);
			throw new CrashException(crashReport);
		}
	}

	public void renderFluid(
			BlockPos pos,
			BlockRenderView world,
			VertexConsumer vertexConsumer,
			BlockState blockState,
			FluidState fluidState
	) {
		try {
			Objects.requireNonNull(this.fluidRenderer).render(world, pos, vertexConsumer, blockState, fluidState);
		}
		catch (Throwable var9) {
			CrashReport crashReport = CrashReport.create(var9, "Tesselating liquid in world");
			CrashReportSection crashReportSection = crashReport.addElement("Block being tesselated");
			CrashReportSection.addBlockInfo(crashReportSection, world, pos, blockState);
			throw new CrashException(crashReport);
		}
	}

	public BlockModelRenderer getModelRenderer() {
		return this.blockModelRenderer;
	}

	public BlockStateModel getModel(BlockState state) {
		return this.models.getModel(state);
	}

	public void renderBlockAsEntity(
			BlockState state,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light,
			int overlay
	) {
		BlockRenderType blockRenderType = state.getRenderType();
		if (blockRenderType != BlockRenderType.INVISIBLE) {
			BlockStateModel blockStateModel = this.getModel(state);
			int i = this.blockColors.getColor(state, null, null, 0);
			float f = (i >> 16 & 0xFF) / 255.0F;
			float g = (i >> 8 & 0xFF) / 255.0F;
			float h = (i & 0xFF) / 255.0F;
			BlockModelRenderer.render(
					matrices.peek(),
					vertexConsumers.getBuffer(BlockRenderLayers.getEntityBlockLayer(state)),
					blockStateModel,
					f,
					g,
					h,
					light,
					overlay
			);
		}
	}

	@Override
	public void reload(ResourceManager manager) {
		this.fluidRenderer = new FluidRenderer(this.spriteHolder);
	}
}
