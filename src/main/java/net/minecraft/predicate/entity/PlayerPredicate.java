package net.minecraft.predicate.entity;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.NumberRange;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerRecipeBook;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.StatType;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameModeList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Предикат для проверки состояния игрока: уровень опыта, режим игры, статистика,
 * рецепты, достижения, направление взгляда и состояние клавиш управления.
 */
public record PlayerPredicate(
		NumberRange.IntRange experienceLevel,
		GameModeList gameMode,
		List<PlayerPredicate.StatMatcher<?>> stats,
		Object2BooleanMap<RegistryKey<Recipe<?>>> recipes,
		Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements,
		Optional<EntityPredicate> lookingAt,
		Optional<InputPredicate> input
) implements EntitySubPredicate {

	public static final int LOOKING_AT_DISTANCE = 100;

	public static final MapCodec<PlayerPredicate> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					NumberRange.IntRange.CODEC
							.optionalFieldOf("level", NumberRange.IntRange.ANY)
							.forGetter(PlayerPredicate::experienceLevel),
					GameModeList.CODEC
							.optionalFieldOf("gamemode", GameModeList.ALL)
							.forGetter(PlayerPredicate::gameMode),
					PlayerPredicate.StatMatcher.CODEC
							.listOf()
							.optionalFieldOf("stats", List.of())
							.forGetter(PlayerPredicate::stats),
					Codecs.object2BooleanMap(Recipe.KEY_CODEC)
							.optionalFieldOf("recipes", Object2BooleanMaps.emptyMap())
							.forGetter(PlayerPredicate::recipes),
					Codec.unboundedMap(Identifier.CODEC, PlayerPredicate.AdvancementPredicate.CODEC)
							.optionalFieldOf("advancements", Map.of())
							.forGetter(PlayerPredicate::advancements),
					EntityPredicate.CODEC.optionalFieldOf("looking_at").forGetter(PlayerPredicate::lookingAt),
					InputPredicate.CODEC.optionalFieldOf("input").forGetter(PlayerPredicate::input)
			)
			.apply(instance, PlayerPredicate::new)
	);

	@Override
	public boolean test(Entity entity, ServerWorld world, @Nullable Vec3d pos) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return false;
		}

		if (!experienceLevel.test(player.experienceLevel)) {
			return false;
		}

		if (!gameMode.contains(player.getGameMode())) {
			return false;
		}

		StatHandler statHandler = player.getStatHandler();

		for (PlayerPredicate.StatMatcher<?> statMatcher : stats) {
			if (!statMatcher.test(statHandler)) {
				return false;
			}
		}

		ServerRecipeBook recipeBook = player.getRecipeBook();

		for (Object2BooleanMap.Entry<RegistryKey<Recipe<?>>> entry : recipes.object2BooleanEntrySet()) {
			if (recipeBook.isUnlocked(entry.getKey()) != entry.getBooleanValue()) {
				return false;
			}
		}

		if (!advancements.isEmpty()) {
			PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
			ServerAdvancementLoader advancementLoader = player.getEntityWorld().getServer().getAdvancementLoader();

			for (Map.Entry<Identifier, PlayerPredicate.AdvancementPredicate> entry : advancements.entrySet()) {
				AdvancementEntry advancementEntry = advancementLoader.get(entry.getKey());

				if (advancementEntry == null || !entry.getValue().test(advancementTracker.getProgress(advancementEntry))) {
					return false;
				}
			}
		}

		if (lookingAt.isPresent()) {
			Vec3d eyePos = player.getEyePos();
			Vec3d lookDir = player.getRotationVec(1.0F);
			Vec3d lookTarget = eyePos.add(lookDir.x * LOOKING_AT_DISTANCE, lookDir.y * LOOKING_AT_DISTANCE, lookDir.z * LOOKING_AT_DISTANCE);

			EntityHitResult hitResult = ProjectileUtil.getEntityCollision(
					player.getEntityWorld(),
					player,
					eyePos,
					lookTarget,
					new Box(eyePos, lookTarget).expand(1.0),
					hitEntity -> !hitEntity.isSpectator(),
					0.0F
			);

			if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
				return false;
			}

			Entity lookedAt = hitResult.getEntity();

			if (!lookingAt.get().test(player, lookedAt) || !player.canSee(lookedAt)) {
				return false;
			}
		}

		return input.isEmpty() || input.get().matches(player.getPlayerInput());
	}

	@Override
	public MapCodec<PlayerPredicate> getCodec() {
		return EntitySubPredicateTypes.PLAYER;
	}

	/**
	 * Предикат достижения, проверяющий выполнение отдельных критериев.
	 */
	record AdvancementCriteriaPredicate(Object2BooleanMap<String> criteria) implements PlayerPredicate.AdvancementPredicate {

		public static final Codec<PlayerPredicate.AdvancementCriteriaPredicate> CODEC = Codecs
				.object2BooleanMap(Codec.STRING)
				.xmap(
						PlayerPredicate.AdvancementCriteriaPredicate::new,
						PlayerPredicate.AdvancementCriteriaPredicate::criteria
				);

		public boolean test(AdvancementProgress advancementProgress) {
			for (Object2BooleanMap.Entry<String> entry : criteria.object2BooleanEntrySet()) {
				CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());

				if (criterionProgress == null || criterionProgress.isObtained() != entry.getBooleanValue()) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Общий интерфейс предикатов достижений: либо проверка полного выполнения,
	 * либо проверка отдельных критериев.
	 */
	interface AdvancementPredicate extends Predicate<AdvancementProgress> {

		Codec<PlayerPredicate.AdvancementPredicate> CODEC = Codec.either(
				PlayerPredicate.CompletedAdvancementPredicate.CODEC,
				PlayerPredicate.AdvancementCriteriaPredicate.CODEC
		)
		.xmap(
				Either::unwrap,
				predicate -> {
					if (predicate instanceof PlayerPredicate.CompletedAdvancementPredicate completed) {
						return Either.left(completed);
					}

					if (predicate instanceof PlayerPredicate.AdvancementCriteriaPredicate criteria) {
						return Either.right(criteria);
					}

					throw new UnsupportedOperationException();
				}
		);
	}

	/**
	 * Строитель {@link PlayerPredicate}.
	 */
	public static class Builder {

		private NumberRange.IntRange experienceLevel = NumberRange.IntRange.ANY;
		private GameModeList gameMode = GameModeList.ALL;
		private final ImmutableList.Builder<PlayerPredicate.StatMatcher<?>> stats = ImmutableList.builder();
		private final Object2BooleanMap<RegistryKey<Recipe<?>>> recipes = new Object2BooleanOpenHashMap<>();
		private final Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
		private Optional<EntityPredicate> lookingAt = Optional.empty();
		private Optional<InputPredicate> input = Optional.empty();

		public static PlayerPredicate.Builder create() {
			return new PlayerPredicate.Builder();
		}

		public PlayerPredicate.Builder experienceLevel(NumberRange.IntRange experienceLevel) {
			this.experienceLevel = experienceLevel;
			return this;
		}

		public <T> PlayerPredicate.Builder stat(
				StatType<T> statType,
				RegistryEntry.Reference<T> value,
				NumberRange.IntRange range
		) {
			stats.add(new PlayerPredicate.StatMatcher<>(statType, value, range));
			return this;
		}

		public PlayerPredicate.Builder recipe(RegistryKey<Recipe<?>> recipeKey, boolean unlocked) {
			recipes.put(recipeKey, unlocked);
			return this;
		}

		public PlayerPredicate.Builder gameMode(GameModeList gameMode) {
			this.gameMode = gameMode;
			return this;
		}

		public PlayerPredicate.Builder lookingAt(EntityPredicate.Builder lookingAt) {
			this.lookingAt = Optional.of(lookingAt.build());
			return this;
		}

		public PlayerPredicate.Builder advancement(Identifier id, boolean done) {
			advancements.put(id, new PlayerPredicate.CompletedAdvancementPredicate(done));
			return this;
		}

		public PlayerPredicate.Builder advancement(Identifier id, Map<String, Boolean> criteria) {
			advancements.put(id, new PlayerPredicate.AdvancementCriteriaPredicate(new Object2BooleanOpenHashMap<>(criteria)));
			return this;
		}

		public PlayerPredicate.Builder input(InputPredicate input) {
			this.input = Optional.of(input);
			return this;
		}

		public PlayerPredicate build() {
			return new PlayerPredicate(
					experienceLevel,
					gameMode,
					stats.build(),
					recipes,
					advancements,
					lookingAt,
					input
			);
		}
	}

	/**
	 * Предикат достижения, проверяющий только факт его полного выполнения.
	 */
	record CompletedAdvancementPredicate(boolean done) implements PlayerPredicate.AdvancementPredicate {

		public static final Codec<PlayerPredicate.CompletedAdvancementPredicate> CODEC = Codec.BOOL
				.xmap(
						PlayerPredicate.CompletedAdvancementPredicate::new,
						PlayerPredicate.CompletedAdvancementPredicate::done
				);

		public boolean test(AdvancementProgress advancementProgress) {
			return advancementProgress.isDone() == done;
		}
	}

	/**
	 * Сопоставитель статистики игрока с диапазоном допустимых значений.
	 * Использует мемоизированный {@link Supplier} для ленивого получения объекта {@link Stat}.
	 */
	record StatMatcher<T>(
			StatType<T> type,
			RegistryEntry<T> value,
			NumberRange.IntRange range,
			Supplier<Stat<T>> stat
	) {

		public static final Codec<PlayerPredicate.StatMatcher<?>> CODEC = Registries.STAT_TYPE
				.getCodec()
				.dispatch(PlayerPredicate.StatMatcher::type, PlayerPredicate.StatMatcher::createCodec);

		public StatMatcher(StatType<T> type, RegistryEntry<T> value, NumberRange.IntRange range) {
			this(type, value, range, Suppliers.memoize(() -> type.getOrCreateStat(value.value())));
		}

		private static <T> MapCodec<PlayerPredicate.StatMatcher<T>> createCodec(StatType<T> type) {
			return RecordCodecBuilder.mapCodec(
					instance -> instance.group(
							type.getRegistry()
									.getEntryCodec()
									.fieldOf("stat")
									.forGetter(PlayerPredicate.StatMatcher::value),
							NumberRange.IntRange.CODEC
									.optionalFieldOf("value", NumberRange.IntRange.ANY)
									.forGetter(PlayerPredicate.StatMatcher::range)
					)
					.apply(
							instance,
							(value, range) -> new PlayerPredicate.StatMatcher<>(type, value, range)
					)
			);
		}

		public boolean test(StatHandler statHandler) {
			return range.test(statHandler.getStat(stat.get()));
		}
	}
}
