package net.minecraft.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.function.Function;

/**
 * Иммутабельные данные тестового инстанса, описывающие его параметры.
 *
 * @param environment       окружение, в котором выполняется тест
 * @param structure         идентификатор структуры теста
 * @param maxTicks          максимальное число тиков до таймаута
 * @param setupTicks        число тиков задержки перед стартом
 * @param required          является ли тест обязательным
 * @param rotation          базовый поворот структуры
 * @param manualOnly        запускается ли тест только вручную
 * @param maxAttempts       максимальное число попыток (для флакующих тестов)
 * @param requiredSuccesses минимальное число успехов для флакующего теста
 * @param skyAccess         требует ли тест доступа к небу
 */
public record TestData<EnvironmentType>(
		EnvironmentType environment,
		Identifier structure,
		int maxTicks,
		int setupTicks,
		boolean required,
		BlockRotation rotation,
		boolean manualOnly,
		int maxAttempts,
		int requiredSuccesses,
		boolean skyAccess
) {

	public static final MapCodec<TestData<RegistryEntry<TestEnvironmentDefinition>>> CODEC =
			RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							TestEnvironmentDefinition.ENTRY_CODEC
									.fieldOf("environment")
									.forGetter(TestData::environment),
							Identifier.CODEC.fieldOf("structure").forGetter(TestData::structure),
							Codecs.POSITIVE_INT.fieldOf("max_ticks").forGetter(TestData::maxTicks),
							Codecs.NON_NEGATIVE_INT.optionalFieldOf("setup_ticks", 0).forGetter(TestData::setupTicks),
							Codec.BOOL.optionalFieldOf("required", true).forGetter(TestData::required),
							BlockRotation.CODEC
									.optionalFieldOf("rotation", BlockRotation.NONE)
									.forGetter(TestData::rotation),
							Codec.BOOL.optionalFieldOf("manual_only", false).forGetter(TestData::manualOnly),
							Codecs.POSITIVE_INT.optionalFieldOf("max_attempts", 1).forGetter(TestData::maxAttempts),
							Codecs.POSITIVE_INT
									.optionalFieldOf("required_successes", 1)
									.forGetter(TestData::requiredSuccesses),
							Codec.BOOL.optionalFieldOf("sky_access", false).forGetter(TestData::skyAccess)
					).apply(instance, TestData::new)
			);

	public TestData(
			EnvironmentType environment,
			Identifier structure,
			int maxTicks,
			int setupTicks,
			boolean required,
			BlockRotation rotation
	) {
		this(environment, structure, maxTicks, setupTicks, required, rotation, false, 1, 1, false);
	}

	public TestData(EnvironmentType environment, Identifier structure, int maxTicks, int setupTicks, boolean required) {
		this(environment, structure, maxTicks, setupTicks, required, BlockRotation.NONE);
	}

	/**
	 * Создаёт копию с преобразованным типом окружения.
	 *
	 * @param environmentFunction функция преобразования окружения
	 */
	public <T> TestData<T> applyToEnvironment(Function<EnvironmentType, T> environmentFunction) {
		return new TestData<>(
				environmentFunction.apply(environment),
				structure,
				maxTicks,
				setupTicks,
				required,
				rotation,
				manualOnly,
				maxAttempts,
				requiredSuccesses,
				skyAccess
		);
	}
}
