package net.minecraft.client.render.chunk;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.DebugChunkGenerator;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code RenderedChunk}.
 */
class RenderedChunk {

	private final Map<BlockPos, BlockEntity> blockEntities;
	private final @Nullable PalettedContainer<BlockState> blockPalette;
	private final boolean debugWorld;
	private final HeightLimitView heightLimitView;

	RenderedChunk(WorldChunk chunk, int sectionIndex) {
		this.heightLimitView = chunk;
		this.debugWorld = chunk.getWorld().isDebugWorld();
		this.blockEntities = ImmutableMap.copyOf(chunk.getBlockEntities());
		if (chunk instanceof EmptyChunk) {
			this.blockPalette = null;
		}
		else {
			ChunkSection[] chunkSections = chunk.getSectionArray();
			if (sectionIndex >= 0 && sectionIndex < chunkSections.length) {
				ChunkSection chunkSection = chunkSections[sectionIndex];
				this.blockPalette = chunkSection.isEmpty() ? null : chunkSection.getBlockStateContainer().copy();
			}
			else {
				this.blockPalette = null;
			}
		}
	}

	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return this.blockEntities.get(pos);
	}

	public BlockState getBlockState(BlockPos pos) {
		int i = pos.getX();
		int j = pos.getY();
		int k = pos.getZ();
		if (this.debugWorld) {
			BlockState blockState = null;
			if (j == 60) {
				blockState = Blocks.BARRIER.getDefaultState();
			}

			if (j == 70) {
				blockState = DebugChunkGenerator.getBlockState(i, k);
			}

			return blockState == null ? Blocks.AIR.getDefaultState() : blockState;
		}
		else if (this.blockPalette == null) {
			return Blocks.AIR.getDefaultState();
		}
		else {
			try {
				return this.blockPalette.get(i & 15, j & 15, k & 15);
			}
			catch (Throwable var8) {
				CrashReport crashReport = CrashReport.create(var8, "Getting block state");
				CrashReportSection crashReportSection = crashReport.addElement("Block being got");
				crashReportSection.add(
						"Location",
						() -> CrashReportSection.createPositionString(this.heightLimitView, i, j, k)
				);
				throw new CrashException(crashReport);
			}
		}
	}
}
