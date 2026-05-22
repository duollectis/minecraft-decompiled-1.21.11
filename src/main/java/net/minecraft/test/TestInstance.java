package net.minecraft.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Абстрактный экземпляр теста, хранящий метаданные и логику запуска.
 * Конкретные реализации: {@link BlockBasedTestInstance} и {@link FunctionTestInstance}.
 */
public abstract class TestInstance {

	public static final Codec<TestInstance> CODEC =
		Registries.TEST_INSTANCE_TYPE.getCodec().dispatch(TestInstance::getCodec, codec -> codec);

	private final TestData<RegistryEntry<TestEnvironmentDefinition>> data;

	public static MapCodec<? extends TestInstance> registerAndGetDefault(
		Registry<MapCodec<? extends TestInstance>> registry
	) {
		register(registry, "block_based", BlockBasedTestInstance.CODEC);
		return register(registry, "function", FunctionTestInstance.CODEC);
	}

	private static MapCodec<? extends TestInstance> register(
		Registry<MapCodec<? extends TestInstance>> registry,
		String id,
		MapCodec<? extends TestInstance> codec
	) {
		return Registry.register(
			registry,
			RegistryKey.of(RegistryKeys.TEST_INSTANCE_TYPE, Identifier.ofVanilla(id)),
			codec
		);
	}

	protected TestInstance(TestData<RegistryEntry<TestEnvironmentDefinition>> data) {
		this.data = data;
	}

	public abstract void start(TestContext context);

	public abstract MapCodec<? extends TestInstance> getCodec();

	public RegistryEntry<TestEnvironmentDefinition> getEnvironment() {
		return data.environment();
	}

	public Identifier getStructure() {
		return data.structure();
	}

	public int getMaxTicks() {
		return data.maxTicks();
	}

	public int getSetupTicks() {
		return data.setupTicks();
	}

	public boolean isRequired() {
		return data.required();
	}

	public boolean isManualOnly() {
		return data.manualOnly();
	}

	public int getMaxAttempts() {
		return data.maxAttempts();
	}

	public int getRequiredSuccesses() {
		return data.requiredSuccesses();
	}

	public boolean requiresSkyAccess() {
		return data.skyAccess();
	}

	public BlockRotation getRotation() {
		return data.rotation();
	}

	protected TestData<RegistryEntry<TestEnvironmentDefinition>> getData() {
		return data;
	}

	protected abstract MutableText getTypeDescription();

	public Text getDescription() {
		return getFormattedTypeDescription().append(getStructureAndBatchDescription());
	}

	protected MutableText getFormattedTypeDescription() {
		return getFormattedDescription("test_instance.description.type", getTypeDescription());
	}

	protected Text getStructureAndBatchDescription() {
		return getFormattedDescription("test_instance.description.structure", data.structure().toString())
			.append(getFormattedDescription(
				"test_instance.description.batch",
				data.environment().getIdAsString()
			));
	}

	protected MutableText getFormattedDescription(String key, String description) {
		return getFormattedDescription(key, Text.literal(description));
	}

	protected MutableText getFormattedDescription(String key, MutableText description) {
		return Text.translatable(key, description.formatted(Formatting.BLUE)).append(Text.literal("\n"));
	}
}
