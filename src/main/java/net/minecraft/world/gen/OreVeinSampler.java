package net.minecraft.world.gen;

import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Сэмплер рудных жил для процедурной генерации крупных рудных образований.
 * Реализует алгоритм на основе плотностных функций: медная жила генерируется выше,
 * железная — глубже. Активируется только при включённом флаге {@link SharedConstants#ORE_VEINS}.
 */
public final class OreVeinSampler {

	private static final float DENSITY_THRESHOLD = 0.4F;
	private static final int MAX_DENSITY_INTRUSION = 20;
	private static final double LIMINAL_DENSITY_REDUCTION = 0.2;
	private static final float BLOCK_GENERATION_CHANCE = 0.7F;
	private static final float MIN_ORE_CHANCE = 0.1F;
	private static final float MAX_ORE_CHANCE = 0.3F;
	private static final float DENSITY_FOR_MAX_ORE_CHANCE = 0.6F;
	private static final float RAW_ORE_BLOCK_CHANCE = 0.02F;
	private static final float VEIN_GAP_THRESHOLD = -0.3F;

	private OreVeinSampler() {
	}

	/**
	 * Создаёт сэмплер состояний блоков для рудных жил.
	 * Использует три плотностные функции: переключатель типа жилы, гребень жилы и зазор.
	 *
	 * @param veinToggle    функция, знак которой определяет тип жилы (медь/железо)
	 * @param veinRidged    функция гребня — блокирует генерацию при значении ≥ 0
	 * @param veinGap       функция зазора — блокирует руду при значении ≤ {@link #VEIN_GAP_THRESHOLD}
	 * @param randomDeriver источник случайности, разделённый по позиции блока
	 */
	public static ChunkNoiseSampler.BlockStateSampler create(
		DensityFunction veinToggle,
		DensityFunction veinRidged,
		DensityFunction veinGap,
		RandomSplitter randomDeriver
	) {
		BlockState debugState = SharedConstants.ORE_VEINS ? Blocks.AIR.getDefaultState() : null;

		return pos -> {
			double toggleValue = veinToggle.sample(pos);
			int blockY = pos.blockY();
			VeinType veinType = toggleValue > 0.0 ? VeinType.COPPER : VeinType.IRON;
			double absDensity = Math.abs(toggleValue);
			int distToTop = veinType.maxY - blockY;
			int distToBottom = blockY - veinType.minY;

			if (distToBottom < 0 || distToTop < 0) {
				return debugState;
			}

			int edgeDist = Math.min(distToTop, distToBottom);
			// Плавное затухание плотности у границ диапазона высот жилы
			double edgeFalloff = MathHelper.clampedMap(edgeDist, 0.0, MAX_DENSITY_INTRUSION, -LIMINAL_DENSITY_REDUCTION, 0.0);

			if (absDensity + edgeFalloff < DENSITY_THRESHOLD) {
				return debugState;
			}

			Random random = randomDeriver.split(pos.blockX(), blockY, pos.blockZ());

			if (random.nextFloat() > BLOCK_GENERATION_CHANCE) {
				return debugState;
			}

			if (veinRidged.sample(pos) >= 0.0) {
				return debugState;
			}

			double oreChance = MathHelper.clampedMap(absDensity, DENSITY_THRESHOLD, DENSITY_FOR_MAX_ORE_CHANCE, MIN_ORE_CHANCE, MAX_ORE_CHANCE);

			if (random.nextFloat() < oreChance && veinGap.sample(pos) > VEIN_GAP_THRESHOLD) {
				return random.nextFloat() < RAW_ORE_BLOCK_CHANCE ? veinType.rawOreBlock : veinType.ore;
			}

			return SharedConstants.ORE_VEINS ? Blocks.OAK_BUTTON.getDefaultState() : veinType.stone;
		};
	}

	/**
	 * Типы рудных жил с диапазонами высот и соответствующими блоками.
	 */
	public enum VeinType {
		COPPER(
			Blocks.COPPER_ORE.getDefaultState(),
			Blocks.RAW_COPPER_BLOCK.getDefaultState(),
			Blocks.GRANITE.getDefaultState(),
			0,
			50
		),
		IRON(
			Blocks.DEEPSLATE_IRON_ORE.getDefaultState(),
			Blocks.RAW_IRON_BLOCK.getDefaultState(),
			Blocks.TUFF.getDefaultState(),
			-60,
			-8
		);

		final BlockState ore;
		final BlockState rawOreBlock;
		final BlockState stone;
		public final int minY;
		public final int maxY;

		VeinType(BlockState ore, BlockState rawOreBlock, BlockState stone, int minY, int maxY) {
			this.ore = ore;
			this.rawOreBlock = rawOreBlock;
			this.stone = stone;
			this.minY = minY;
			this.maxY = maxY;
		}
	}
}
