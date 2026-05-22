package net.minecraft.world.gen.densityfunction;

import com.mojang.serialization.Codec;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.chunk.Blender;
import org.jspecify.annotations.Nullable;

/**
 * Базовый интерфейс функции плотности — строительный блок системы генерации рельефа.
 * Функции плотности вычисляют скалярное значение в каждой точке пространства (blockX, blockY, blockZ),
 * которое затем используется для определения наличия твёрдого блока или воздуха.
 * Функции могут быть скомпонованы, кэшированы и интерполированы через {@link DensityFunctionTypes}.
 */
public interface DensityFunction {

	Codec<DensityFunction> CODEC = DensityFunctionTypes.CODEC;

	Codec<RegistryEntry<DensityFunction>> REGISTRY_ENTRY_CODEC =
			RegistryElementCodec.of(RegistryKeys.DENSITY_FUNCTION, CODEC);

	Codec<DensityFunction> FUNCTION_CODEC = REGISTRY_ENTRY_CODEC.xmap(
			DensityFunctionTypes.RegistryEntryHolder::new,
			function -> (RegistryEntry) (
					function instanceof DensityFunctionTypes.RegistryEntryHolder registryEntryHolder
					? registryEntryHolder.function()
					: new RegistryEntry.Direct<>(function)
			)
	);

	double sample(DensityFunction.NoisePos pos);

	void fill(double[] densities, DensityFunction.EachApplier applier);

	DensityFunction apply(DensityFunction.DensityFunctionVisitor visitor);

	double minValue();

	double maxValue();

	CodecHolder<? extends DensityFunction> getCodecHolder();

	default DensityFunction clamp(double min, double max) {
		return new DensityFunctionTypes.Clamp(this, min, max);
	}

	default DensityFunction abs() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.ABS);
	}

	default DensityFunction square() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.SQUARE);
	}

	default DensityFunction cube() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.CUBE);
	}

	default DensityFunction halfNegative() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.HALF_NEGATIVE);
	}

	default DensityFunction quarterNegative() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.QUARTER_NEGATIVE);
	}

	default DensityFunction invert() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.INVERT);
	}

	default DensityFunction squeeze() {
		return DensityFunctionTypes.unary(this, DensityFunctionTypes.UnaryOperation.Type.SQUEEZE);
	}

	/** Базовая реализация {@link DensityFunction} с дефолтными методами {@code fill} и {@code apply}. */
	public interface Base extends DensityFunction {

		@Override
		default void fill(double[] densities, DensityFunction.EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		default DensityFunction apply(DensityFunction.DensityFunctionVisitor visitor) {
			return visitor.apply(this);
		}
	}

	/** Посетитель (Visitor) для обхода и трансформации дерева функций плотности. */
	public interface DensityFunctionVisitor {

		DensityFunction apply(DensityFunction densityFunction);

		default DensityFunction.Noise apply(DensityFunction.Noise noiseDensityFunction) {
			return noiseDensityFunction;
		}
	}

	/** Применяет функцию плотности к каждой позиции в массиве, используется при заполнении чанка. */
	public interface EachApplier {

		DensityFunction.NoisePos at(int index);

		void fill(double[] densities, DensityFunction densityFunction);
	}

	/** Обёртка над {@link DoublePerlinNoiseSampler}, хранящая ссылку на параметры шума из реестра. */
	public record Noise(
			RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseData,
			@Nullable DoublePerlinNoiseSampler noise
	) {

		public static final Codec<DensityFunction.Noise>
				CODEC =
				DoublePerlinNoiseSampler.NoiseParameters.REGISTRY_ENTRY_CODEC
						.xmap(
								noiseData -> new DensityFunction.Noise(noiseData, null),
								DensityFunction.Noise::noiseData
						);

		public Noise(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseData) {
			this(noiseData, null);
		}

		public double sample(double x, double y, double z) {
			return noise == null ? 0.0 : noise.sample(x, y, z);
		}

		public double getMaxValue() {
			return noise == null ? 2.0 : noise.getMaxValue();
		}
	}

	/** Позиция в пространстве блоков, передаваемая в функцию плотности при сэмплировании. */
	public interface NoisePos {

		int blockX();

		int blockY();

		int blockZ();

		default Blender getBlender() {
			return Blender.getNoBlending();
		}
	}

	/** Простая реализация {@link NoisePos} без блендинга — используется вне контекста генерации чанка. */
	public record UnblendedNoisePos(int blockX, int blockY, int blockZ) implements DensityFunction.NoisePos {
	}
}
