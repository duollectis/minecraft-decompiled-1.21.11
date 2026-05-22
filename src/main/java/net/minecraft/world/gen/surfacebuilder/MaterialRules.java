package net.minecraft.world.gen.surfacebuilder;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Система правил материалов поверхности. Определяет, какой блок должен быть размещён
 * в данной позиции на основе условий (биом, высота, шум, глубина камня и т.д.).
 * Используется {@link SurfaceBuilder} для построения поверхности чанков.
 */
public class MaterialRules {

	public static final MaterialRules.MaterialCondition
			STONE_DEPTH_FLOOR =
			stoneDepth(0, false, VerticalSurfaceType.FLOOR);
	public static final MaterialRules.MaterialCondition
			STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH =
			stoneDepth(0, true, VerticalSurfaceType.FLOOR);
	public static final MaterialRules.MaterialCondition
			STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_6 =
			stoneDepth(0, true, 6, VerticalSurfaceType.FLOOR);
	public static final MaterialRules.MaterialCondition
			STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_30 =
			stoneDepth(0, true, 30, VerticalSurfaceType.FLOOR);
	public static final MaterialRules.MaterialCondition
			STONE_DEPTH_CEILING =
			stoneDepth(0, false, VerticalSurfaceType.CEILING);
	public static final MaterialRules.MaterialCondition
			STONE_DEPTH_CEILING_WITH_SURFACE_DEPTH =
			stoneDepth(0, true, VerticalSurfaceType.CEILING);

	public static MaterialRules.MaterialCondition stoneDepth(
			int offset,
			boolean addSurfaceDepth,
			VerticalSurfaceType verticalSurfaceType
	) {
		return new MaterialRules.StoneDepthMaterialCondition(offset, addSurfaceDepth, 0, verticalSurfaceType);
	}

	public static MaterialRules.MaterialCondition stoneDepth(
			int offset, boolean addSurfaceDepth, int secondaryDepthRange, VerticalSurfaceType verticalSurfaceType
	) {
		return new MaterialRules.StoneDepthMaterialCondition(
				offset,
				addSurfaceDepth,
				secondaryDepthRange,
				verticalSurfaceType
		);
	}

	public static MaterialRules.MaterialCondition not(MaterialRules.MaterialCondition target) {
		return new MaterialRules.NotMaterialCondition(target);
	}

	public static MaterialRules.MaterialCondition aboveY(YOffset anchor, int runDepthMultiplier) {
		return new MaterialRules.AboveYMaterialCondition(anchor, runDepthMultiplier, false);
	}

	public static MaterialRules.MaterialCondition aboveYWithStoneDepth(YOffset anchor, int runDepthMultiplier) {
		return new MaterialRules.AboveYMaterialCondition(anchor, runDepthMultiplier, true);
	}

	public static MaterialRules.MaterialCondition water(int offset, int runDepthMultiplier) {
		return new MaterialRules.WaterMaterialCondition(offset, runDepthMultiplier, false);
	}

	public static MaterialRules.MaterialCondition waterWithStoneDepth(int offset, int runDepthMultiplier) {
		return new MaterialRules.WaterMaterialCondition(offset, runDepthMultiplier, true);
	}

	@SafeVarargs
	public static MaterialRules.MaterialCondition biome(RegistryKey<Biome>... biomes) {
		return biome(List.of(biomes));
	}

	private static MaterialRules.BiomeMaterialCondition biome(List<RegistryKey<Biome>> biomes) {
		return new MaterialRules.BiomeMaterialCondition(biomes);
	}

	public static MaterialRules.MaterialCondition noiseThreshold(
			RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noise,
			double min
	) {
		return noiseThreshold(noise, min, Double.MAX_VALUE);
	}

	public static MaterialRules.MaterialCondition noiseThreshold(
			RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noise,
			double min,
			double max
	) {
		return new MaterialRules.NoiseThresholdMaterialCondition(noise, min, max);
	}

	public static MaterialRules.MaterialCondition verticalGradient(
			String id,
			YOffset trueAtAndBelow,
			YOffset falseAtAndAbove
	) {
		return new MaterialRules.VerticalGradientMaterialCondition(Identifier.of(id), trueAtAndBelow, falseAtAndAbove);
	}

	public static MaterialRules.MaterialCondition steepSlope() {
		return MaterialRules.SteepMaterialCondition.INSTANCE;
	}

	public static MaterialRules.MaterialCondition hole() {
		return MaterialRules.HoleMaterialCondition.INSTANCE;
	}

	public static MaterialRules.MaterialCondition surface() {
		return MaterialRules.SurfaceMaterialCondition.INSTANCE;
	}

	public static MaterialRules.MaterialCondition temperature() {
		return MaterialRules.TemperatureMaterialCondition.INSTANCE;
	}

	public static MaterialRules.MaterialRule condition(
			MaterialRules.MaterialCondition condition,
			MaterialRules.MaterialRule rule
	) {
		return new MaterialRules.ConditionMaterialRule(condition, rule);
	}

	public static MaterialRules.MaterialRule sequence(MaterialRules.MaterialRule... rules) {
		if (rules.length == 0) {
			throw new IllegalArgumentException("Need at least 1 rule for a sequence");
		}

		return new MaterialRules.SequenceMaterialRule(Arrays.asList(rules));
	}

	public static MaterialRules.MaterialRule block(BlockState state) {
		return new MaterialRules.BlockMaterialRule(state);
	}

	public static MaterialRules.MaterialRule terracottaBands() {
		return MaterialRules.TerracottaBandsMaterialRule.INSTANCE;
	}

	static <A> MapCodec<? extends A> register(
			Registry<MapCodec<? extends A>> registry,
			String id,
			CodecHolder<? extends A> codecHolder
	) {
		return Registry.register(registry, id, codecHolder.codec());
	}

	/**
	 * Условие: текущая позиция находится выше заданной высоты {@code anchor}.
	 * Опционально учитывает глубину камня и множитель глубины поверхности.
	 */
	record AboveYMaterialCondition(
			YOffset anchor,
			int surfaceDepthMultiplier,
			boolean addStoneDepth
	) implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.AboveYMaterialCondition> CODEC = CodecHolder.of(
				RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    YOffset.OFFSET_CODEC
										                    .fieldOf("anchor")
										                    .forGetter(MaterialRules.AboveYMaterialCondition::anchor),
								                    Codec
										                    .intRange(-20, 20)
										                    .fieldOf("surface_depth_multiplier")
										                    .forGetter(MaterialRules.AboveYMaterialCondition::surfaceDepthMultiplier),
								                    Codec.BOOL
										                    .fieldOf("add_stone_depth")
										                    .forGetter(MaterialRules.AboveYMaterialCondition::addStoneDepth)
						                    )
						                    .apply(instance, MaterialRules.AboveYMaterialCondition::new)
				)
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			/**
			 * {@code AboveYPredicate}.
			 */
			class AboveYPredicate extends MaterialRules.FullLazyAbstractPredicate {

				AboveYPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					return this.context.blockY + (AboveYMaterialCondition.this.addStoneDepth
					                              ? this.context.stoneDepthAbove : 0
					)
							>= AboveYMaterialCondition.this.anchor.getY(this.context.heightContext)
							+ this.context.runDepth * AboveYMaterialCondition.this.surfaceDepthMultiplier;
				}
			}

			return new AboveYPredicate();
		}
	}

	/**
	 * Условие: текущий биом входит в заданный список биомов.
	 */
	static final class BiomeMaterialCondition implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.BiomeMaterialCondition> CODEC = CodecHolder.of(
				RegistryKey.createCodec(RegistryKeys.BIOME)
				           .listOf()
				           .fieldOf("biome_is")
				           .xmap(MaterialRules::biome, biomeMaterialCondition -> biomeMaterialCondition.biomes)
		);
		private final List<RegistryKey<Biome>> biomes;
		final Predicate<RegistryKey<Biome>> predicate;

		BiomeMaterialCondition(List<RegistryKey<Biome>> biomes) {
			this.biomes = biomes;
			this.predicate = Set.copyOf(biomes)::contains;
		}

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			/**
			 * {@code BiomePredicate}.
			 */
			class BiomePredicate extends MaterialRules.FullLazyAbstractPredicate {

				BiomePredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					return this.context.biomeSupplier.get().matches(BiomeMaterialCondition.this.predicate);
				}
			}

			return new BiomePredicate();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			return o instanceof MaterialRules.BiomeMaterialCondition other
				? biomes.equals(other.biomes)
				: false;
		}

		@Override
		public int hashCode() {
			return this.biomes.hashCode();
		}

		@Override
		public String toString() {
			return "BiomeConditionSource[biomes=" + this.biomes + "]";
		}
	}

	/**
	 * Правило: всегда возвращает фиксированный {@link BlockState}.
	 */
	record BlockMaterialRule(
			BlockState resultState,
			MaterialRules.SimpleBlockStateRule rule
	) implements MaterialRules.MaterialRule {

		static final CodecHolder<MaterialRules.BlockMaterialRule> CODEC = CodecHolder.of(
				BlockState.CODEC
						.xmap(MaterialRules.BlockMaterialRule::new, MaterialRules.BlockMaterialRule::resultState)
						.fieldOf("result_state")
		);

		BlockMaterialRule(BlockState resultState) {
			this(resultState, new MaterialRules.SimpleBlockStateRule(resultState));
		}

		@Override
		public CodecHolder<? extends MaterialRules.MaterialRule> codec() {
			return CODEC;
		}

		public MaterialRules.BlockStateRule apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return this.rule;
		}
	}

	/**
	 * Функциональный интерфейс: возвращает {@link BlockState} для позиции или {@code null},
	 * если правило не применимо.
	 */
	protected interface BlockStateRule {

		@Nullable BlockState tryApply(int x, int y, int z);
	}

	/**
	 * Поставщик булевого значения, используемый предикатами условий материалов.
	 */
	interface BooleanSupplier {

		boolean get();
	}

	/**
	 * Правило: применяет {@code thenRun}, только если выполняется условие {@code ifTrue}.
	 */
	record ConditionMaterialRule(
			MaterialRules.MaterialCondition ifTrue,
			MaterialRules.MaterialRule thenRun
	) implements MaterialRules.MaterialRule {

		static final CodecHolder<MaterialRules.ConditionMaterialRule> CODEC = CodecHolder.of(
				RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    MaterialRules.MaterialCondition.CODEC
										                    .fieldOf("if_true")
										                    .forGetter(MaterialRules.ConditionMaterialRule::ifTrue),
								                    MaterialRules.MaterialRule.CODEC
										                    .fieldOf("then_run")
										                    .forGetter(MaterialRules.ConditionMaterialRule::thenRun)
						                    )
						                    .apply(instance, MaterialRules.ConditionMaterialRule::new)
				)
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialRule> codec() {
			return CODEC;
		}

		public MaterialRules.BlockStateRule apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return new MaterialRules.ConditionalBlockStateRule(
					this.ifTrue.apply(materialRuleContext),
					this.thenRun.apply(materialRuleContext)
			);
		}
	}

	/**
	 * Правило состояния блока с условием: делегирует в {@code followup} только при истинном условии.
	 */
	record ConditionalBlockStateRule(
			MaterialRules.BooleanSupplier condition,
			MaterialRules.BlockStateRule followup
	) implements MaterialRules.BlockStateRule {

		@Override
		public @Nullable BlockState tryApply(int x, int y, int z) {
			return condition.get() ? followup.tryApply(x, y, z) : null;
		}
	}

	/**
	 * Ленивый предикат, инвалидируемый при изменении полной 3D-позиции (X, Y, Z).
	 */
	abstract static class FullLazyAbstractPredicate extends MaterialRules.LazyAbstractPredicate {

		protected FullLazyAbstractPredicate(MaterialRules.MaterialRuleContext materialRuleContext) {
			super(materialRuleContext);
		}

		@Override
		protected long getCurrentUniqueValue() {
			return this.context.uniquePosValue;
		}
	}

	/**
	 * Условие: текущая позиция находится в «дыре» (runDepth &lt;= 0).
	 */
	enum HoleMaterialCondition implements MaterialRules.MaterialCondition {
		INSTANCE;

		static final CodecHolder<MaterialRules.HoleMaterialCondition> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return materialRuleContext.negativeRunDepthPredicate;
		}
	}

	/**
	 * Ленивый предикат, инвалидируемый только при изменении горизонтальной позиции (X, Z).
	 */
	abstract static class HorizontalLazyAbstractPredicate extends MaterialRules.LazyAbstractPredicate {

		protected HorizontalLazyAbstractPredicate(MaterialRules.MaterialRuleContext materialRuleContext) {
			super(materialRuleContext);
		}

		@Override
		protected long getCurrentUniqueValue() {
			return this.context.uniqueHorizontalPosValue;
		}
	}

	/**
	 * Инвертирующий поставщик булевого значения — обёртка над {@code target}.
	 */
	record InvertedBooleanSupplier(MaterialRules.BooleanSupplier target) implements MaterialRules.BooleanSupplier {

		@Override
		public boolean get() {
			return !this.target.get();
		}
	}

	/**
	 * Базовый ленивый предикат с кешированием результата. Пересчитывает значение только
	 * при изменении уникального ключа позиции, что позволяет избежать повторных вычислений
	 * для одной и той же позиции в рамках одного прохода построения поверхности.
	 */
	abstract static class LazyAbstractPredicate implements MaterialRules.BooleanSupplier {

		protected final MaterialRules.MaterialRuleContext context;
		private long uniqueValue;
		@Nullable Boolean result;

		protected LazyAbstractPredicate(MaterialRules.MaterialRuleContext context) {
			this.context = context;
			this.uniqueValue = this.getCurrentUniqueValue() - 1L;
		}

		@Override
		public boolean get() {
			long currentKey = getCurrentUniqueValue();

			if (currentKey == uniqueValue) {
				if (result == null) {
					throw new IllegalStateException("Update triggered but the result is null");
				}

				return result;
			}

			uniqueValue = currentKey;
			result = test();

			return result;
		}

		protected abstract long getCurrentUniqueValue();

		protected abstract boolean test();
	}

	/**
	 * Условие материала поверхности. Принимает контекст и возвращает {@link BooleanSupplier},
	 * который вычисляет истинность условия для каждой позиции.
	 */
	public interface MaterialCondition extends Function<MaterialRules.MaterialRuleContext, MaterialRules.BooleanSupplier> {

		Codec<MaterialRules.MaterialCondition> CODEC = Registries.MATERIAL_CONDITION
				.getCodec()
				.dispatch(materialCondition -> materialCondition.codec().codec(), Function.identity());

		static MapCodec<? extends MaterialRules.MaterialCondition> registerAndGetDefault(Registry<MapCodec<? extends MaterialRules.MaterialCondition>> registry) {
			MaterialRules.register(registry, "biome", MaterialRules.BiomeMaterialCondition.CODEC);
			MaterialRules.register(registry, "noise_threshold", MaterialRules.NoiseThresholdMaterialCondition.CODEC);
			MaterialRules.register(
					registry,
					"vertical_gradient",
					MaterialRules.VerticalGradientMaterialCondition.CODEC
			);
			MaterialRules.register(registry, "y_above", MaterialRules.AboveYMaterialCondition.CODEC);
			MaterialRules.register(registry, "water", MaterialRules.WaterMaterialCondition.CODEC);
			MaterialRules.register(registry, "temperature", MaterialRules.TemperatureMaterialCondition.CODEC);
			MaterialRules.register(registry, "steep", MaterialRules.SteepMaterialCondition.CODEC);
			MaterialRules.register(registry, "not", MaterialRules.NotMaterialCondition.CODEC);
			MaterialRules.register(registry, "hole", MaterialRules.HoleMaterialCondition.CODEC);
			MaterialRules.register(registry, "above_preliminary_surface", MaterialRules.SurfaceMaterialCondition.CODEC);
			return MaterialRules.register(registry, "stone_depth", MaterialRules.StoneDepthMaterialCondition.CODEC);
		}

		CodecHolder<? extends MaterialRules.MaterialCondition> codec();
	}

	/**
	 * Правило материала поверхности. Принимает контекст и возвращает {@link BlockStateRule},
	 * определяющее блок для каждой позиции.
	 */
	public interface MaterialRule extends Function<MaterialRules.MaterialRuleContext, MaterialRules.BlockStateRule> {

		Codec<MaterialRules.MaterialRule>
				CODEC =
				Registries.MATERIAL_RULE
						.getCodec()
						.dispatch(materialRule -> materialRule.codec().codec(), Function.identity());

		static MapCodec<? extends MaterialRules.MaterialRule> registerAndGetDefault(Registry<MapCodec<? extends MaterialRules.MaterialRule>> registry) {
			MaterialRules.register(registry, "bandlands", MaterialRules.TerracottaBandsMaterialRule.CODEC);
			MaterialRules.register(registry, "block", MaterialRules.BlockMaterialRule.CODEC);
			MaterialRules.register(registry, "sequence", MaterialRules.SequenceMaterialRule.CODEC);
			return MaterialRules.register(registry, "condition", MaterialRules.ConditionMaterialRule.CODEC);
		}

		CodecHolder<? extends MaterialRules.MaterialRule> codec();
	}

	/**
	 * Контекст выполнения правил материалов. Хранит текущую позицию, биом, глубину камня,
	 * уровень жидкости и другие параметры, необходимые для вычисления условий.
	 * Обновляется построчно в процессе генерации поверхности чанка.
	 */
	protected static final class MaterialRuleContext {

		private static final int CHUNK_COORD_BITS = 8;
		private static final int BIOME_CELL_SIZE = 4;
		private static final int CHUNK_SIZE_BLOCKS = 16;
		private static final int CHUNK_COORD_MASK = 15;
		final SurfaceBuilder surfaceBuilder;
		final MaterialRules.BooleanSupplier
				biomeTemperaturePredicate =
				new MaterialRules.MaterialRuleContext.BiomeTemperaturePredicate(this);
		final MaterialRules.BooleanSupplier
				steepSlopePredicate =
				new MaterialRules.MaterialRuleContext.SteepSlopePredicate(this);
		final MaterialRules.BooleanSupplier
				negativeRunDepthPredicate =
				new MaterialRules.MaterialRuleContext.NegativeRunDepthPredicate(this);
		final MaterialRules.BooleanSupplier surfacePredicate = new MaterialRules.MaterialRuleContext.SurfacePredicate();
		final NoiseConfig noiseConfig;
		final Chunk chunk;
		private final ChunkNoiseSampler chunkNoiseSampler;
		private final Function<BlockPos, RegistryEntry<Biome>> posToBiome;
		final HeightContext heightContext;
		private long packedChunkPos = Long.MAX_VALUE;
		private final int[] estimatedSurfaceHeights = new int[4];
		long uniqueHorizontalPosValue = -9223372036854775807L;
		int blockX;
		int blockZ;
		int runDepth;
		private long cachedSecondaryDepthKey = this.uniqueHorizontalPosValue - 1L;
		private double secondaryDepth;
		private long cachedSurfaceHeightKey = this.uniqueHorizontalPosValue - 1L;
		private int surfaceMinY;
		long uniquePosValue = -9223372036854775807L;
		final BlockPos.Mutable pos = new BlockPos.Mutable();
		Supplier<RegistryEntry<Biome>> biomeSupplier;
		int blockY;
		int fluidHeight;
		int stoneDepthBelow;
		int stoneDepthAbove;

		protected MaterialRuleContext(
				SurfaceBuilder surfaceBuilder,
				NoiseConfig noiseConfig,
				Chunk chunk,
				ChunkNoiseSampler chunkNoiseSampler,
				Function<BlockPos, RegistryEntry<Biome>> posToBiome,
				Registry<Biome> biomeRegistry,
				HeightContext heightContext
		) {
			this.surfaceBuilder = surfaceBuilder;
			this.noiseConfig = noiseConfig;
			this.chunk = chunk;
			this.chunkNoiseSampler = chunkNoiseSampler;
			this.posToBiome = posToBiome;
			this.heightContext = heightContext;
		}

		protected void initHorizontalContext(int blockX, int blockZ) {
			this.uniqueHorizontalPosValue++;
			this.uniquePosValue++;
			this.blockX = blockX;
			this.blockZ = blockZ;
			this.runDepth = this.surfaceBuilder.sampleRunDepth(blockX, blockZ);
		}

		protected void initVerticalContext(
				int stoneDepthAbove,
				int stoneDepthBelow,
				int fluidHeight,
				int blockX,
				int blockY,
				int blockZ
		) {
			this.uniquePosValue++;
			this.biomeSupplier = Suppliers.memoize(() -> this.posToBiome.apply(this.pos.set(blockX, blockY, blockZ)));
			this.blockY = blockY;
			this.fluidHeight = fluidHeight;
			this.stoneDepthBelow = stoneDepthBelow;
			this.stoneDepthAbove = stoneDepthAbove;
		}

		protected double getSecondaryDepth() {
			if (this.cachedSecondaryDepthKey != this.uniqueHorizontalPosValue) {
				this.cachedSecondaryDepthKey = this.uniqueHorizontalPosValue;
				this.secondaryDepth = this.surfaceBuilder.sampleSecondaryDepth(this.blockX, this.blockZ);
			}

			return this.secondaryDepth;
		}

		public int getSeaLevel() {
			return this.surfaceBuilder.getSeaLevel();
		}

		private static int blockToChunkCoord(int blockCoord) {
			return blockCoord >> 4;
		}

		private static int chunkToBlockCoord(int chunkCoord) {
			return chunkCoord << 4;
		}

		/**
		 * Оценивает минимальную высоту поверхности для текущей горизонтальной позиции.
		 * Использует билинейную интерполяцию по четырём углам чанка для плавного перехода.
		 * Результат кешируется до следующего изменения горизонтальной позиции.
		 */
		protected int estimateSurfaceHeight() {
			if (cachedSurfaceHeightKey == uniqueHorizontalPosValue) {
				return surfaceMinY;
			}

			cachedSurfaceHeightKey = uniqueHorizontalPosValue;
			int chunkX = blockToChunkCoord(blockX);
			int chunkZ = blockToChunkCoord(blockZ);
			long packedPos = ChunkPos.toLong(chunkX, chunkZ);

			if (packedChunkPos != packedPos) {
				packedChunkPos = packedPos;
				estimatedSurfaceHeights[0] =
						chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(chunkX), chunkToBlockCoord(chunkZ));
				estimatedSurfaceHeights[1] =
						chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(chunkX + 1), chunkToBlockCoord(chunkZ));
				estimatedSurfaceHeights[2] =
						chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(chunkX), chunkToBlockCoord(chunkZ + 1));
				estimatedSurfaceHeights[3] =
						chunkNoiseSampler.estimateSurfaceHeight(
								chunkToBlockCoord(chunkX + 1),
								chunkToBlockCoord(chunkZ + 1)
						);
			}

			int interpolatedHeight = MathHelper.floor(
					MathHelper.lerp2(
							(blockX & CHUNK_COORD_MASK) / 16.0F,
							(blockZ & CHUNK_COORD_MASK) / 16.0F,
							estimatedSurfaceHeights[0],
							estimatedSurfaceHeights[1],
							estimatedSurfaceHeights[2],
							estimatedSurfaceHeights[3]
					)
			);
			surfaceMinY = interpolatedHeight + runDepth - 8;

			return surfaceMinY;
		}

		/**
		 * Предикат: биом в текущей позиции является холодным (температура ниже порога замерзания).
		 */
		static class BiomeTemperaturePredicate extends MaterialRules.FullLazyAbstractPredicate {

			BiomeTemperaturePredicate(MaterialRules.MaterialRuleContext materialRuleContext) {
				super(materialRuleContext);
			}

			@Override
			protected boolean test() {
				return this.context
						.biomeSupplier
						.get()
						.value()
						.isCold(
								this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ),
								this.context.getSeaLevel()
						);
			}
		}

		/**
		 * Предикат: глубина поверхности (runDepth) отрицательна — позиция находится в «дыре».
		 */
		static final class NegativeRunDepthPredicate extends MaterialRules.HorizontalLazyAbstractPredicate {

			NegativeRunDepthPredicate(MaterialRules.MaterialRuleContext materialRuleContext) {
				super(materialRuleContext);
			}

			@Override
			protected boolean test() {
				return this.context.runDepth <= 0;
			}
		}

		/**
		 * Предикат: склон в текущей позиции является крутым (перепад высот &gt;= 4 блоков).
		 */
		static class SteepSlopePredicate extends MaterialRules.HorizontalLazyAbstractPredicate {

			SteepSlopePredicate(MaterialRules.MaterialRuleContext materialRuleContext) {
				super(materialRuleContext);
			}

			private static final int STEEP_SLOPE_THRESHOLD = 4;

			@Override
			protected boolean test() {
				int localX = context.blockX & CHUNK_COORD_MASK;
				int localZ = context.blockZ & CHUNK_COORD_MASK;
				int zMinus = Math.max(localZ - 1, 0);
				int zPlus = Math.min(localZ + 1, CHUNK_COORD_MASK);
				Chunk chunk = context.chunk;
				int heightZMinus = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, zMinus);
				int heightZPlus = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, zPlus);

				if (heightZPlus >= heightZMinus + STEEP_SLOPE_THRESHOLD) {
					return true;
				}

				int xMinus = Math.max(localX - 1, 0);
				int xPlus = Math.min(localX + 1, CHUNK_COORD_MASK);
				int heightXMinus = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, xMinus, localZ);
				int heightXPlus = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, xPlus, localZ);

				return heightXMinus >= heightXPlus + STEEP_SLOPE_THRESHOLD;
			}
		}

		/**
		 * Предикат: текущий блок находится выше или на уровне предварительной поверхности чанка.
		 */
		final class SurfacePredicate implements MaterialRules.BooleanSupplier {

			@Override
			public boolean get() {
				return MaterialRuleContext.this.blockY >= MaterialRuleContext.this.estimateSurfaceHeight();
			}
		}
	}

	/**
	 * Условие: значение шума Перлина в текущей горизонтальной позиции находится
	 * в диапазоне [{@code minThreshold}, {@code maxThreshold}].
	 */
	record NoiseThresholdMaterialCondition(
			RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noise,
			double minThreshold,
			double maxThreshold
	)
			implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.NoiseThresholdMaterialCondition> CODEC = CodecHolder.of(
				RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    RegistryKey
										                    .createCodec(RegistryKeys.NOISE_PARAMETERS)
										                    .fieldOf("noise")
										                    .forGetter(MaterialRules.NoiseThresholdMaterialCondition::noise),
								                    Codec.DOUBLE
										                    .fieldOf("min_threshold")
										                    .forGetter(MaterialRules.NoiseThresholdMaterialCondition::minThreshold),
								                    Codec.DOUBLE
										                    .fieldOf("max_threshold")
										                    .forGetter(MaterialRules.NoiseThresholdMaterialCondition::maxThreshold)
						                    )
						                    .apply(instance, MaterialRules.NoiseThresholdMaterialCondition::new)
				)
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			final DoublePerlinNoiseSampler
					doublePerlinNoiseSampler =
					materialRuleContext.noiseConfig.getOrCreateSampler(this.noise);

			/**
			 * {@code NoiseThresholdPredicate}.
			 */
			class NoiseThresholdPredicate extends MaterialRules.HorizontalLazyAbstractPredicate {

				NoiseThresholdPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					double d = doublePerlinNoiseSampler.sample(this.context.blockX, 0.0, this.context.blockZ);
					return d >= NoiseThresholdMaterialCondition.this.minThreshold
							&& d <= NoiseThresholdMaterialCondition.this.maxThreshold;
				}
			}

			return new NoiseThresholdPredicate();
		}
	}

	/**
	 * Условие-инвертор: истинно тогда и только тогда, когда {@code target} ложно.
	 */
	record NotMaterialCondition(MaterialRules.MaterialCondition target) implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.NotMaterialCondition> CODEC = CodecHolder.of(
				MaterialRules.MaterialCondition.CODEC
						.xmap(MaterialRules.NotMaterialCondition::new, MaterialRules.NotMaterialCondition::target)
						.fieldOf("invert")
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return new MaterialRules.InvertedBooleanSupplier(this.target.apply(materialRuleContext));
		}
	}

	/**
	 * Правило состояния блока: перебирает список правил и возвращает первый ненулевой результат.
	 */
	record SequenceBlockStateRule(List<MaterialRules.BlockStateRule> rules) implements MaterialRules.BlockStateRule {

		@Override
		public @Nullable BlockState tryApply(int x, int y, int z) {
			for (MaterialRules.BlockStateRule rule : rules) {
				BlockState state = rule.tryApply(x, y, z);

				if (state != null) {
					return state;
				}
			}

			return null;
		}
	}

	/**
	 * Правило-последовательность: применяет правила по порядку, возвращая первый ненулевой результат.
	 */
	record SequenceMaterialRule(List<MaterialRules.MaterialRule> sequence) implements MaterialRules.MaterialRule {

		static final CodecHolder<MaterialRules.SequenceMaterialRule> CODEC = CodecHolder.of(
				MaterialRules.MaterialRule.CODEC
						.listOf()
						.xmap(MaterialRules.SequenceMaterialRule::new, MaterialRules.SequenceMaterialRule::sequence)
						.fieldOf("sequence")
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialRule> codec() {
			return CODEC;
		}

		public MaterialRules.BlockStateRule apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			if (sequence.size() == 1) {
				return sequence.get(0).apply(materialRuleContext);
			}

			Builder<MaterialRules.BlockStateRule> builder = ImmutableList.builder();

			for (MaterialRules.MaterialRule rule : sequence) {
				builder.add(rule.apply(materialRuleContext));
			}

			return new MaterialRules.SequenceBlockStateRule(builder.build());
		}
	}

	/**
	 * Простейшее правило: всегда возвращает один и тот же {@link BlockState}.
	 */
	record SimpleBlockStateRule(BlockState state) implements MaterialRules.BlockStateRule {

		@Override
		public BlockState tryApply(int x, int y, int z) {
			return state;
		}
	}

	/**
	 * Условие: текущая позиция находится на крутом склоне.
	 */
	enum SteepMaterialCondition implements MaterialRules.MaterialCondition {
		INSTANCE;

		static final CodecHolder<MaterialRules.SteepMaterialCondition> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return materialRuleContext.steepSlopePredicate;
		}
	}

	/**
	 * Условие: глубина камня (расстояние до поверхности/потолка) не превышает заданного порога.
	 * Используется для размещения почвы, гравия и других поверхностных слоёв.
	 */
	record StoneDepthMaterialCondition(
			int offset,
			boolean addSurfaceDepth,
			int secondaryDepthRange,
			VerticalSurfaceType surfaceType
	)
			implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.StoneDepthMaterialCondition> CODEC = CodecHolder.of(
				RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    Codec.INT
										                    .fieldOf("offset")
										                    .forGetter(MaterialRules.StoneDepthMaterialCondition::offset),
								                    Codec.BOOL
										                    .fieldOf("add_surface_depth")
										                    .forGetter(MaterialRules.StoneDepthMaterialCondition::addSurfaceDepth),
								                    Codec.INT
										                    .fieldOf("secondary_depth_range")
										                    .forGetter(MaterialRules.StoneDepthMaterialCondition::secondaryDepthRange),
								                    VerticalSurfaceType.CODEC
										                    .fieldOf("surface_type")
										                    .forGetter(MaterialRules.StoneDepthMaterialCondition::surfaceType)
						                    )
						                    .apply(instance, MaterialRules.StoneDepthMaterialCondition::new)
				)
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			final boolean isCeiling = surfaceType == VerticalSurfaceType.CEILING;

			class StoneDepthPredicate extends MaterialRules.FullLazyAbstractPredicate {

				StoneDepthPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					int stoneDepth = isCeiling ? context.stoneDepthBelow : context.stoneDepthAbove;
					int surfaceDepthBonus = StoneDepthMaterialCondition.this.addSurfaceDepth ? context.runDepth : 0;
					int secondaryBonus = StoneDepthMaterialCondition.this.secondaryDepthRange == 0
							? 0
							: (int) MathHelper.map(
									context.getSecondaryDepth(),
									-1.0,
									1.0,
									0.0,
									StoneDepthMaterialCondition.this.secondaryDepthRange
							);

					return stoneDepth <= 1 + StoneDepthMaterialCondition.this.offset + surfaceDepthBonus + secondaryBonus;
				}
			}

			return new StoneDepthPredicate();
		}
	}

	/**
	 * Условие: текущий блок находится выше предварительной поверхности чанка.
	 */
	enum SurfaceMaterialCondition implements MaterialRules.MaterialCondition {
		INSTANCE;

		static final CodecHolder<MaterialRules.SurfaceMaterialCondition>
				CODEC =
				CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return materialRuleContext.surfacePredicate;
		}
	}

	/**
	 * Условие: биом в текущей позиции является холодным (температура ниже порога замерзания воды).
	 */
	enum TemperatureMaterialCondition implements MaterialRules.MaterialCondition {
		INSTANCE;

		static final CodecHolder<MaterialRules.TemperatureMaterialCondition>
				CODEC =
				CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return materialRuleContext.biomeTemperaturePredicate;
		}
	}

	/**
	 * Правило: возвращает блок из полос терракоты, определяемый координатой Y.
	 */
	enum TerracottaBandsMaterialRule implements MaterialRules.MaterialRule {
		INSTANCE;

		static final CodecHolder<MaterialRules.TerracottaBandsMaterialRule>
				CODEC =
				CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialRules.MaterialRule> codec() {
			return CODEC;
		}

		public MaterialRules.BlockStateRule apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			return materialRuleContext.surfaceBuilder::getTerracottaBlock;
		}
	}

	/**
	 * Условие: вертикальный градиент — вероятность истинности линейно убывает от {@code trueAtAndBelow}
	 * до {@code falseAtAndAbove}, используя детерминированный шум для плавного перехода.
	 */
	record VerticalGradientMaterialCondition(
			Identifier randomName,
			YOffset trueAtAndBelow,
			YOffset falseAtAndAbove
	) implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.VerticalGradientMaterialCondition> CODEC = CodecHolder.of(
				RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    Identifier.CODEC
										                    .fieldOf("random_name")
										                    .forGetter(MaterialRules.VerticalGradientMaterialCondition::randomName),
								                    YOffset.OFFSET_CODEC
										                    .fieldOf("true_at_and_below")
										                    .forGetter(MaterialRules.VerticalGradientMaterialCondition::trueAtAndBelow),
								                    YOffset.OFFSET_CODEC
										                    .fieldOf("false_at_and_above")
										                    .forGetter(MaterialRules.VerticalGradientMaterialCondition::falseAtAndAbove)
						                    )
						                    .apply(instance, MaterialRules.VerticalGradientMaterialCondition::new)
				)
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			final int trueAtY = trueAtAndBelow().getY(materialRuleContext.heightContext);
			final int falseAtY = falseAtAndAbove().getY(materialRuleContext.heightContext);
			final RandomSplitter randomSplitter = materialRuleContext.noiseConfig.getOrCreateRandomDeriver(randomName());

			class VerticalGradientPredicate extends MaterialRules.FullLazyAbstractPredicate {

				VerticalGradientPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					int blockY = context.blockY;

					if (blockY <= trueAtY) {
						return true;
					}

					if (blockY >= falseAtY) {
						return false;
					}

					double probability = MathHelper.map(blockY, trueAtY, falseAtY, 1.0, 0.0);
					Random random = randomSplitter.split(context.blockX, blockY, context.blockZ);

					return random.nextFloat() < probability;
				}
			}

			return new VerticalGradientPredicate();
		}
	}

	/**
	 * Условие: текущая позиция находится выше уровня жидкости с учётом смещения и глубины поверхности.
	 */
	record WaterMaterialCondition(
			int offset,
			int surfaceDepthMultiplier,
			boolean addStoneDepth
	) implements MaterialRules.MaterialCondition {

		static final CodecHolder<MaterialRules.WaterMaterialCondition> CODEC = CodecHolder.of(
				RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    Codec.INT.fieldOf("offset").forGetter(MaterialRules.WaterMaterialCondition::offset),
								                    Codec
										                    .intRange(-20, 20)
										                    .fieldOf("surface_depth_multiplier")
										                    .forGetter(MaterialRules.WaterMaterialCondition::surfaceDepthMultiplier),
								                    Codec.BOOL
										                    .fieldOf("add_stone_depth")
										                    .forGetter(MaterialRules.WaterMaterialCondition::addStoneDepth)
						                    )
						                    .apply(instance, MaterialRules.WaterMaterialCondition::new)
				)
		);

		@Override
		public CodecHolder<? extends MaterialRules.MaterialCondition> codec() {
			return CODEC;
		}

		public MaterialRules.BooleanSupplier apply(MaterialRules.MaterialRuleContext materialRuleContext) {
			/**
			 * {@code WaterPredicate}.
			 */
			class WaterPredicate extends MaterialRules.FullLazyAbstractPredicate {

				WaterPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					return this.context.fluidHeight == Integer.MIN_VALUE
							|| this.context.blockY + (WaterMaterialCondition.this.addStoneDepth
							                          ? this.context.stoneDepthAbove : 0
					)
							>= this.context.fluidHeight
							+ WaterMaterialCondition.this.offset
							+ this.context.runDepth * WaterMaterialCondition.this.surfaceDepthMultiplier;
				}
			}

			return new WaterPredicate();
		}
	}
}
