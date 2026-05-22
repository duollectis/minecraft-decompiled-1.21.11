package net.minecraft.world.chunk.light;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import org.jspecify.annotations.Nullable;

/**
 * Агрегатор провайдеров блочного и небесного освещения.
 * Делегирует все операции обновления и запроса света соответствующим провайдерам.
 * Если провайдер отсутствует (например, нет неба в Nether), операции игнорируются.
 */
public class LightingProvider implements LightingView {

	public static final int MAX_LIGHT_LEVEL = 1;
	public static final LightingProvider DEFAULT = new LightingProvider();
	protected final HeightLimitView world;
	private final @Nullable ChunkLightProvider<?, ?> blockLightProvider;
	private final @Nullable ChunkLightProvider<?, ?> skyLightProvider;

	public LightingProvider(ChunkProvider chunkProvider, boolean hasBlockLight, boolean hasSkyLight) {
		world = chunkProvider.getWorld();
		blockLightProvider = hasBlockLight ? new ChunkBlockLightProvider(chunkProvider) : null;
		skyLightProvider = hasSkyLight ? new ChunkSkyLightProvider(chunkProvider) : null;
	}

	private LightingProvider() {
		world = HeightLimitView.create(0, 0);
		blockLightProvider = null;
		skyLightProvider = null;
	}

	@Override
	public void checkBlock(BlockPos pos) {
		if (blockLightProvider != null) {
			blockLightProvider.checkBlock(pos);
		}

		if (skyLightProvider != null) {
			skyLightProvider.checkBlock(pos);
		}
	}

	@Override
	public boolean hasUpdates() {
		return (skyLightProvider != null && skyLightProvider.hasUpdates())
				|| (blockLightProvider != null && blockLightProvider.hasUpdates());
	}

	@Override
	public int doLightUpdates() {
		int updates = 0;

		if (blockLightProvider != null) {
			updates += blockLightProvider.doLightUpdates();
		}

		if (skyLightProvider != null) {
			updates += skyLightProvider.doLightUpdates();
		}

		return updates;
	}

	@Override
	public void setSectionStatus(ChunkSectionPos pos, boolean notReady) {
		if (blockLightProvider != null) {
			blockLightProvider.setSectionStatus(pos, notReady);
		}

		if (skyLightProvider != null) {
			skyLightProvider.setSectionStatus(pos, notReady);
		}
	}

	@Override
	public void setColumnEnabled(ChunkPos pos, boolean retainData) {
		if (blockLightProvider != null) {
			blockLightProvider.setColumnEnabled(pos, retainData);
		}

		if (skyLightProvider != null) {
			skyLightProvider.setColumnEnabled(pos, retainData);
		}
	}

	@Override
	public void propagateLight(ChunkPos chunkPos) {
		if (blockLightProvider != null) {
			blockLightProvider.propagateLight(chunkPos);
		}

		if (skyLightProvider != null) {
			skyLightProvider.propagateLight(chunkPos);
		}
	}

	public ChunkLightingView get(LightType lightType) {
		return lightType == LightType.BLOCK
				? (ChunkLightingView) (blockLightProvider == null ? ChunkLightingView.Empty.INSTANCE : blockLightProvider)
				: (ChunkLightingView) (skyLightProvider == null ? ChunkLightingView.Empty.INSTANCE : skyLightProvider);
	}

	public String displaySectionLevel(LightType lightType, ChunkSectionPos pos) {
		if (lightType == LightType.BLOCK) {
			if (blockLightProvider != null) {
				return blockLightProvider.displaySectionLevel(pos.asLong());
			}
		} else if (skyLightProvider != null) {
			return skyLightProvider.displaySectionLevel(pos.asLong());
		}

		return "n/a";
	}

	public LightStorage.Status getStatus(LightType lightType, ChunkSectionPos pos) {
		if (lightType == LightType.BLOCK) {
			if (blockLightProvider != null) {
				return blockLightProvider.getStatus(pos.asLong());
			}
		} else if (skyLightProvider != null) {
			return skyLightProvider.getStatus(pos.asLong());
		}

		return LightStorage.Status.EMPTY;
	}

	public void enqueueSectionData(LightType lightType, ChunkSectionPos pos, @Nullable ChunkNibbleArray nibbles) {
		if (lightType == LightType.BLOCK) {
			if (blockLightProvider != null) {
				blockLightProvider.enqueueSectionData(pos.asLong(), nibbles);
			}
		} else if (skyLightProvider != null) {
			skyLightProvider.enqueueSectionData(pos.asLong(), nibbles);
		}
	}

	public void setRetainData(ChunkPos pos, boolean retainData) {
		if (blockLightProvider != null) {
			blockLightProvider.setRetainColumn(pos, retainData);
		}

		if (skyLightProvider != null) {
			skyLightProvider.setRetainColumn(pos, retainData);
		}
	}

	public int getLight(BlockPos pos, int ambientDarkness) {
		int skyLight = skyLightProvider == null ? 0 : skyLightProvider.getLightLevel(pos) - ambientDarkness;
		int blockLight = blockLightProvider == null ? 0 : blockLightProvider.getLightLevel(pos);

		return Math.max(blockLight, skyLight);
	}

	public boolean isLightingEnabled(long sectionPos) {
		return blockLightProvider == null
				|| blockLightProvider.lightStorage.isColumnEnabled(sectionPos)
				&& (skyLightProvider == null || skyLightProvider.lightStorage.isColumnEnabled(sectionPos));
	}

	public int getHeight() {
		return world.countVerticalSections() + 2;
	}

	public int getBottomY() {
		return world.getBottomSectionCoord() - 1;
	}

	public int getTopY() {
		return getBottomY() + getHeight();
	}
}
