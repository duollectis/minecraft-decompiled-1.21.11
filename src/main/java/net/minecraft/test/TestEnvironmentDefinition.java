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
 * {@code TestEnvironmentDefinition}.
 */
public interface TestEnvironmentDefinition {

	Codec<TestEnvironmentDefinition> CODEC = Registries.TEST_ENVIRONMENT_DEFINITION_TYPE
			.getCodec()
			.dispatch(TestEnvironmentDefinition::getCodec, codec -> codec);

	Codec<RegistryEntry<TestEnvironmentDefinition>>
			ENTRY_CODEC =
			RegistryElementCodec.of(RegistryKeys.TEST_ENVIRONMENT, CODEC);

	static MapCodec<? extends TestEnvironmentDefinition> registerAndGetDefault(Registry<MapCodec<? extends TestEnvironmentDefinition>> registry) {
		Registry.register(registry, "all_of", TestEnvironmentDefinition.AllOf.CODEC);
		Registry.register(registry, "game_rules", TestEnvironmentDefinition.GameRules.CODEC);
		Registry.register(registry, "time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
		Registry.register(registry, "weather", TestEnvironmentDefinition.Weather.CODEC);
		return Registry.register(registry, "function", TestEnvironmentDefinition.Function.CODEC);
	}

	void setup(ServerWorld world);

	default void teardown(ServerWorld world) {
	}

	MapCodec<? extends TestEnvironmentDefinition> getCodec();

	/**
	 * {@code AllOf}.
	 */
	public record AllOf(List<RegistryEntry<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {

		public static final MapCodec<TestEnvironmentDefinition.AllOf> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    TestEnvironmentDefinition.ENTRY_CODEC
								                    .listOf()
								                    .fieldOf("definitions")
								                    .forGetter(TestEnvironmentDefinition.AllOf::definitions)
				                    )
				                    .apply(instance, TestEnvironmentDefinition.AllOf::new)
		);

		public AllOf(TestEnvironmentDefinition... definitionTypes) {
			this(Arrays.stream(definitionTypes).map(RegistryEntry::of).toList());
		}

		@Override
		public void setup(ServerWorld world) {
			this.definitions.forEach(definition -> definition.value().setup(world));
		}

		@Override
		public void teardown(ServerWorld world) {
			this.definitions.forEach(definition -> definition.value().teardown(world));
		}

		@Override
		public MapCodec<TestEnvironmentDefinition.AllOf> getCodec() {
			return CODEC;
		}
	}

	/**
	 * {@code Function}.
	 */
	public record Function(
			Optional<Identifier> setupFunction,
			Optional<Identifier> teardownFunction
	) implements TestEnvironmentDefinition {

		private static final Logger LOGGER = LogUtils.getLogger();
		public static final MapCodec<TestEnvironmentDefinition.Function> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Identifier.CODEC
								                    .optionalFieldOf("setup")
								                    .forGetter(TestEnvironmentDefinition.Function::setupFunction),
						                    Identifier.CODEC
								                    .optionalFieldOf("teardown")
								                    .forGetter(TestEnvironmentDefinition.Function::teardownFunction)
				                    )
				                    .apply(instance, TestEnvironmentDefinition.Function::new)
		);

		@Override
		public void setup(ServerWorld world) {
			this.setupFunction.ifPresent(functionId -> executeFunction(world, functionId));
		}

		@Override
		public void teardown(ServerWorld world) {
			this.teardownFunction.ifPresent(functionId -> executeFunction(world, functionId));
		}

		private static void executeFunction(ServerWorld world, Identifier functionId) {
			MinecraftServer minecraftServer = world.getServer();
			CommandFunctionManager commandFunctionManager = minecraftServer.getCommandFunctionManager();
			Optional<CommandFunction<ServerCommandSource>> optional = commandFunctionManager.getFunction(functionId);
			if (optional.isPresent()) {
				ServerCommandSource serverCommandSource = minecraftServer.getCommandSource()
				                                                         .withPermissions(LeveledPermissionPredicate.GAMEMASTERS)
				                                                         .withSilent()
				                                                         .withWorld(world);
				commandFunctionManager.execute(optional.get(), serverCommandSource);
			}
			else {
				LOGGER.error("Test Batch failed for non-existent function {}", functionId);
			}
		}

		@Override
		public MapCodec<TestEnvironmentDefinition.Function> getCodec() {
			return CODEC;
		}
	}

	/**
	 * {@code GameRules}.
	 */
	public record GameRules(ServerGameRules gameRulesMap) implements TestEnvironmentDefinition {

		public static final MapCodec<TestEnvironmentDefinition.GameRules> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(ServerGameRules.CODEC
								.fieldOf("rules")
								.forGetter(TestEnvironmentDefinition.GameRules::gameRulesMap))
						.apply(instance, TestEnvironmentDefinition.GameRules::new)
		);

		@Override
		public void setup(ServerWorld world) {
			net.minecraft.world.rule.GameRules gameRules = world.getGameRules();
			MinecraftServer minecraftServer = world.getServer();
			gameRules.copyFrom(this.gameRulesMap, minecraftServer);
		}

		@Override
		public void teardown(ServerWorld world) {
			this.gameRulesMap.keySet().forEach(rule -> this.resetValue(world, (GameRule<?>) rule));
		}

		private <T> void resetValue(ServerWorld serverWorld, GameRule<T> rule) {
			serverWorld.getGameRules().setValue(rule, rule.getDefaultValue(), serverWorld.getServer());
		}

		@Override
		public MapCodec<TestEnvironmentDefinition.GameRules> getCodec() {
			return CODEC;
		}
	}

	/**
	 * {@code TimeOfDay}.
	 */
	public record TimeOfDay(int time) implements TestEnvironmentDefinition {

		public static final MapCodec<TestEnvironmentDefinition.TimeOfDay> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Codecs.NON_NEGATIVE_INT
								.fieldOf("time")
								.forGetter(TestEnvironmentDefinition.TimeOfDay::time))
						.apply(instance, TestEnvironmentDefinition.TimeOfDay::new)
		);

		@Override
		public void setup(ServerWorld world) {
			world.setTimeOfDay(this.time);
		}

		@Override
		public MapCodec<TestEnvironmentDefinition.TimeOfDay> getCodec() {
			return CODEC;
		}
	}

	/**
	 * {@code Weather}.
	 */
	public record Weather(TestEnvironmentDefinition.Weather.State weather) implements TestEnvironmentDefinition {

		public static final MapCodec<TestEnvironmentDefinition.Weather> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(TestEnvironmentDefinition.Weather.State.CODEC
								.fieldOf("weather")
								.forGetter(TestEnvironmentDefinition.Weather::weather))
						.apply(instance, TestEnvironmentDefinition.Weather::new)
		);

		@Override
		public void setup(ServerWorld world) {
			this.weather.apply(world);
		}

		@Override
		public void teardown(ServerWorld world) {
			world.resetWeather();
		}

		@Override
		public MapCodec<TestEnvironmentDefinition.Weather> getCodec() {
			return CODEC;
		}

		/**
		 * {@code State}.
		 */
		public static enum State implements StringIdentifiable {
			CLEAR("clear", 100000, 0, false, false),
			RAIN("rain", 0, 100000, true, false),
			THUNDER("thunder", 0, 100000, true, true);

			public static final Codec<TestEnvironmentDefinition.Weather.State> CODEC = StringIdentifiable.createCodec(
					TestEnvironmentDefinition.Weather.State::values
			);
			private final String name;
			private final int clearDuration;
			private final int rainDuration;
			private final boolean raining;
			private final boolean thundering;

			private State(
					final String name,
					final int clearDuration,
					final int rainDuration,
					final boolean raining,
					final boolean thundering
			) {
				this.name = name;
				this.clearDuration = clearDuration;
				this.rainDuration = rainDuration;
				this.raining = raining;
				this.thundering = thundering;
			}

			void apply(ServerWorld world) {
				world.setWeather(this.clearDuration, this.rainDuration, this.raining, this.thundering);
			}

			@Override
			public String asString() {
				return this.name;
			}
		}
	}
}
