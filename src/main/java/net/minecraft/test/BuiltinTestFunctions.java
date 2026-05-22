package net.minecraft.test;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Встроенные тестовые функции ванильного Minecraft.
 * Регистрирует предопределённые функции в реестре тестовых функций.
 */
public class BuiltinTestFunctions extends TestFunctionProvider {

	public static final RegistryKey<Consumer<TestContext>> ALWAYS_PASS = of("always_pass");
	public static final Consumer<TestContext> ALWAYS_PASS_FUNCTION = TestContext::complete;

	/**
	 * Регистрирует провайдер встроенных функций и возвращает функцию по умолчанию.
	 *
	 * @param registry реестр тестовых функций
	 * @return функция {@link #ALWAYS_PASS_FUNCTION}, используемая как дефолтная
	 */
	public static Consumer<TestContext> registerAndGetDefault(Registry<Consumer<TestContext>> registry) {
		addProvider(new BuiltinTestFunctions());
		registerAll(registry);
		return ALWAYS_PASS_FUNCTION;
	}

	@Override
	public void register(BiConsumer<RegistryKey<Consumer<TestContext>>, Consumer<TestContext>> registry) {
		registry.accept(ALWAYS_PASS, ALWAYS_PASS_FUNCTION);
	}

	private static RegistryKey<Consumer<TestContext>> of(String id) {
		return RegistryKey.of(RegistryKeys.TEST_FUNCTION, Identifier.ofVanilla(id));
	}
}
