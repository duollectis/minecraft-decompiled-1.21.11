package net.minecraft.test;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Реализация тестового инстанса, делегирующая логику зарегистрированной функции
 * из реестра {@link RegistryKeys#TEST_FUNCTION}.
 */
public class FunctionTestInstance extends TestInstance {

	public static final MapCodec<FunctionTestInstance> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					RegistryKey.createCodec(RegistryKeys.TEST_FUNCTION)
							.fieldOf("function")
							.forGetter(FunctionTestInstance::getFunction),
					TestData.CODEC.forGetter(TestInstance::getData)
			).apply(instance, FunctionTestInstance::new)
	);

	private final RegistryKey<Consumer<TestContext>> function;

	public FunctionTestInstance(
			RegistryKey<Consumer<TestContext>> function,
			TestData<RegistryEntry<TestEnvironmentDefinition>> data
	) {
		super(data);
		this.function = function;
	}

	/**
	 * Запускает тест, вызывая функцию из реестра по ключу {@link #function}.
	 * Бросает {@link IllegalStateException}, если функция не найдена в реестре.
	 */
	@Override
	public void start(TestContext context) {
		context.getWorld()
				.getRegistryManager()
				.getOptionalEntry(function)
				.map(RegistryEntry.Reference::value)
				.orElseThrow(() -> new IllegalStateException(
						"Trying to access missing test function: " + function.getValue()))
				.accept(context);
	}

	@Override
	public MapCodec<FunctionTestInstance> getCodec() {
		return CODEC;
	}

	@Override
	protected MutableText getTypeDescription() {
		return Text.translatable("test_instance.type.function");
	}

	@Override
	public Text getDescription() {
		return getFormattedTypeDescription()
				.append(getFormattedDescription(
						"test_instance.description.function",
						function.getValue().toString()
				))
				.append(getStructureAndBatchDescription());
	}

	private RegistryKey<Consumer<TestContext>> getFunction() {
		return function;
	}
}
