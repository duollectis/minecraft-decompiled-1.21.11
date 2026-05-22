package net.minecraft.test;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.ServerGameRules;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Определяет окружение, в котором выполняется тест: правила игры, время суток, погода и т.д.
 * Реализации применяются перед запуском батча тестов и откатываются после его завершения.
 */
public interface TestEnvironmentDefinition {

	Codec<TestEnvironmentDefinition> CODEC = Registries.TEST_ENVIRONMENT_DEFINITION_TYPE
		.getCodec()
		.dispatch(TestEnvironmentDefinition::getCodec, codec -> codec);

	Codec<RegistryEntry<TestEnvironmentDefinition>> ENTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.TEST_ENVIRONMENT, CODEC);

	static MapCodec<? extends TestEnvironmentDefinition> registerAndGetDefault(
		Registry<MapCodec<? extends TestEnvironmentDefinition>> registry
	) {
		Registry.register(registry, "all_of", AllOf.CODEC);
		Registry.register(registry, "game_rules", GameRules.CODEC);
		Registry.register(registry, "time_of_day", TimeOfDay.CODEC);
		Registry.register(registry, "weather", Weather.CODEC);
		return Registry.register(registry, "function", Function.CODEC);
	}

	void setup(ServerWorld world);

	default void teardown(ServerWorld world) {
	}

	MapCodec<? extends TestEnvironmentDefinition> getCodec();

	/**
	 * Составное окружение: применяет все вложенные определения последовательно.
	 */
	record AllOf(List<RegistryEntry<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {

		public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				TestEnvironmentDefinition.ENTRY_CODEC
					.listOf()
					.fieldOf("definitions")
					.forGetter(AllOf::definitions)
			).apply(instance, AllOf::new)
		);

		public AllOf(TestEnvironmentDefinition... definitionTypes) {
			this(Arrays.stream(definitionTypes).map(RegistryEntry::of).toList());
		}

		@Override
		public void setup(ServerWorld world) {
			definitions.forEach(definition -> definition.value().setup(world));
		}

		@Override
		public void teardown(ServerWorld world) {
			definitions.forEach(definition -> definition.value().teardown(world));
		}

		@Override
		public MapCodec<AllOf> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Окружение на основе функций датапака: вызывает mcfunction-скрипты при setup/teardown.
	 */
	record Function(
		Optional<Identifier> setupFunction,
		Optional<Identifier> teardownFunction
	) implements TestEnvironmentDefinition {

		private static final Logger LOGGER = LogUtils.getLogger();

		public static final MapCodec<Function> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Identifier.CODEC
					.optionalFieldOf("setup")
					.forGetter(Function::setupFunction),
				Identifier.CODEC
					.optionalFieldOf("teardown")
					.forGetter(Function::teardownFunction)
			).apply(instance, Function::new)
		);

		@Override
		public void setup(ServerWorld world) {
			setupFunction.ifPresent(functionId -> executeFunction(world, functionId));
		}

		@Override
		public void teardown(ServerWorld world) {
			teardownFunction.ifPresent(functionId -> executeFunction(world, functionId));
		}

		private static void executeFunction(ServerWorld world, Identifier functionId) {
			MinecraftServer server = world.getServer();
			CommandFunctionManager functionManager = server.getCommandFunctionManager();
			Optional<CommandFunction<ServerCommandSource>> function = functionManager.getFunction(functionId);

			if (function.isPresent()) {
				ServerCommandSource source = server.getCommandSource()
					.withPermissions(LeveledPermissionPredicate.GAMEMASTERS)
					.withSilent()
					.withWorld(world);
				functionManager.execute(function.get(), source);
			} else {
				LOGGER.error("Test Batch failed for non-existent function {}", functionId);
			}
		}

		@Override
		public MapCodec<Function> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Окружение, переопределяющее правила игры на время выполнения теста.
	 * После завершения батча все изменённые правила сбрасываются к значениям по умолчанию.
	 */
	record GameRules(ServerGameRules gameRulesMap) implements TestEnvironmentDefinition {

		public static final MapCodec<GameRules> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(ServerGameRules.CODEC
					.fieldOf("rules")
					.forGetter(GameRules::gameRulesMap))
				.apply(instance, GameRules::new)
		);

		@Override
		public void setup(ServerWorld world) {
			net.minecraft.world.rule.GameRules rules = world.getGameRules();
			rules.copyFrom(gameRulesMap, world.getServer());
		}

		@Override
		public void teardown(ServerWorld world) {
			gameRulesMap.keySet().forEach(rule -> resetValue(world, (GameRule<?>) rule));
		}

		private <T> void resetValue(ServerWorld world, GameRule<T> rule) {
			world.getGameRules().setValue(rule, rule.getDefaultValue(), world.getServer());
		}

		@Override
		public MapCodec<GameRules> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Окружение, устанавливающее конкретное время суток перед запуском теста.
	 */
	record TimeOfDay(int time) implements TestEnvironmentDefinition {

		public static final MapCodec<TimeOfDay> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Codecs.NON_NEGATIVE_INT
					.fieldOf("time")
					.forGetter(TimeOfDay::time))
				.apply(instance, TimeOfDay::new)
		);

		@Override
		public void setup(ServerWorld world) {
			world.setTimeOfDay(time);
		}

		@Override
		public MapCodec<TimeOfDay> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Окружение, устанавливающее конкретную погоду перед запуском теста.
	 * После завершения батча погода сбрасывается.
	 */
	record Weather(Weather.State weather) implements TestEnvironmentDefinition {

		public static final MapCodec<Weather> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(Weather.State.CODEC
					.fieldOf("weather")
					.forGetter(Weather::weather))
				.apply(instance, Weather::new)
		);

		@Override
		public void setup(ServerWorld world) {
			weather.apply(world);
		}

		@Override
		public void teardown(ServerWorld world) {
			world.resetWeather();
		}

		@Override
		public MapCodec<Weather> getCodec() {
			return CODEC;
		}

		/**
		 * Перечисление состояний погоды с параметрами длительности и флагами осадков.
		 */
		public enum State implements StringIdentifiable {
			CLEAR("clear", 100000, 0, false, false),
			RAIN("rain", 0, 100000, true, false),
			THUNDER("thunder", 0, 100000, true, true);

			public static final Codec<State> CODEC = StringIdentifiable.createCodec(State::values);

			private final String name;
			private final int clearDuration;
			private final int rainDuration;
			private final boolean raining;
			private final boolean thundering;

			State(
				String name,
				int clearDuration,
				int rainDuration,
				boolean raining,
				boolean thundering
			) {
				this.name = name;
				this.clearDuration = clearDuration;
				this.rainDuration = rainDuration;
				this.raining = raining;
				this.thundering = thundering;
			}

			void apply(ServerWorld world) {
				world.setWeather(clearDuration, rainDuration, raining, thundering);
			}

			@Override
			public String asString() {
				return name;
			}
		}
	}
}
