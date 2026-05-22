package net.minecraft.command;

import com.google.common.primitives.Doubles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.FabricEntitySelectorReader;
import net.minecraft.command.permission.PermissionSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.function.Object2FloatFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/**
 * {@code EntitySelectorReader}.
 */
public class EntitySelectorReader implements FabricEntitySelectorReader {

	public static final char SELECTOR_PREFIX = '@';
	private static final char ARGUMENTS_OPENING = '[';
	private static final char ARGUMENTS_CLOSING = ']';
	public static final char ARGUMENT_DEFINER = '=';
	private static final char ARGUMENT_SEPARATOR = ',';
	public static final char INVERT_MODIFIER = '!';
	public static final char TAG_MODIFIER = '#';
	private static final char NEAREST_PLAYER = 'p';
	private static final char ALL_PLAYERS = 'a';
	private static final char RANDOM_PLAYER = 'r';
	private static final char SELF = 's';
	private static final char ALL_ENTITIES = 'e';
	private static final char NEAREST_ENTITY = 'n';
	public static final SimpleCommandExceptionType
			INVALID_ENTITY_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.entity.invalid"));
	public static final DynamicCommandExceptionType UNKNOWN_SELECTOR_EXCEPTION = new DynamicCommandExceptionType(
			selectorType -> Text.stringifiedTranslatable("argument.entity.selector.unknown", selectorType)
	);
	public static final SimpleCommandExceptionType NOT_ALLOWED_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("argument.entity.selector.not_allowed")
	);
	public static final SimpleCommandExceptionType
			MISSING_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.entity.selector.missing"));
	public static final SimpleCommandExceptionType UNTERMINATED_EXCEPTION = new SimpleCommandExceptionType(
			Text.translatable("argument.entity.options.unterminated")
	);
	public static final DynamicCommandExceptionType VALUELESS_EXCEPTION = new DynamicCommandExceptionType(
			option -> Text.stringifiedTranslatable("argument.entity.options.valueless", option)
	);
	public static final BiConsumer<Vec3d, List<? extends Entity>> NEAREST = (pos, entities) -> entities.sort(
			(entity1, entity2) -> Doubles.compare(entity1.squaredDistanceTo(pos), entity2.squaredDistanceTo(pos))
	);
	public static final BiConsumer<Vec3d, List<? extends Entity>> FURTHEST = (pos, entities) -> entities.sort(
			(entity1, entity2) -> Doubles.compare(entity2.squaredDistanceTo(pos), entity1.squaredDistanceTo(pos))
	);
	public static final BiConsumer<Vec3d, List<? extends Entity>>
			RANDOM =
			(pos, entities) -> Collections.shuffle(entities);
	public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>>
			DEFAULT_SUGGESTION_PROVIDER =
			(builder, consumer) -> builder.buildFuture();
	private final StringReader reader;
	private final boolean atAllowed;
	private int limit;
	private boolean includesNonPlayers;
	private boolean localWorldOnly;
	private NumberRange.@Nullable DoubleRange distance;
	private NumberRange.@Nullable IntRange levelRange;
	private @Nullable Double x;
	private @Nullable Double y;
	private @Nullable Double z;
	private @Nullable Double dx;
	private @Nullable Double dy;
	private @Nullable Double dz;
	private NumberRange.@Nullable AngleRange pitchRange;
	private NumberRange.@Nullable AngleRange yawRange;
	private final List<Predicate<Entity>> predicates = new ArrayList<>();
	private BiConsumer<Vec3d, List<? extends Entity>> sorter = EntitySelector.ARBITRARY;
	private boolean senderOnly;
	private @Nullable String playerName;
	private int startCursor;
	private @Nullable UUID uuid;
	private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>>
			suggestionProvider =
			DEFAULT_SUGGESTION_PROVIDER;
	private boolean selectsName;
	private boolean excludesName;
	private boolean hasLimit;
	private boolean hasSorter;
	private boolean selectsGameMode;
	private boolean excludesGameMode;
	private boolean selectsTeam;
	private boolean excludesTeam;
	private @Nullable EntityType<?> entityType;
	private boolean excludesEntityType;
	private boolean selectsScores;
	private boolean selectsAdvancements;
	private boolean usesAt;

	public EntitySelectorReader(StringReader reader, boolean atAllowed) {
		this.reader = reader;
		this.atAllowed = atAllowed;
	}

	public static <S> boolean shouldAllowAtSelectors(S source) {
		return source instanceof PermissionSource permissionSource && permissionSource
				.getPermissions()
				.hasPermission(DefaultPermissions.ENTITY_SELECTORS);
	}

	@Deprecated
	public static boolean canUseSelectors(PermissionSource source) {
		return source.getPermissions().hasPermission(DefaultPermissions.ENTITY_SELECTORS);
	}

	public EntitySelector build() {
		Box box;
		if (dx == null && dy == null && dz == null) {
			if (distance != null && distance.getMax().isPresent()) {
				double maxDist = (Double) distance.getMax().get();
				box = new Box(-maxDist, -maxDist, -maxDist, maxDist + 1.0, maxDist + 1.0, maxDist + 1.0);
			}
			else {
				box = null;
			}
		}
		else {
			box = createBox(
					dx == null ? 0.0 : dx,
					dy == null ? 0.0 : dy,
					dz == null ? 0.0 : dz
			);
		}

		Function<Vec3d, Vec3d> positionFunction;
		if (x == null && y == null && z == null) {
			positionFunction = pos -> pos;
		}
		else {
			positionFunction = pos -> new Vec3d(
					x == null ? pos.x : x,
					y == null ? pos.y : y,
					z == null ? pos.z : z
			);
		}

		return new EntitySelector(
				limit,
				includesNonPlayers,
				localWorldOnly,
				List.copyOf(predicates),
				distance,
				positionFunction,
				box,
				sorter,
				senderOnly,
				playerName,
				uuid,
				entityType,
				usesAt
		);
	}

	private Box createBox(double x, double y, double z) {
		boolean negX = x < 0.0;
		boolean negY = y < 0.0;
		boolean negZ = z < 0.0;
		double minX = negX ? x : 0.0;
		double minY = negY ? y : 0.0;
		double minZ = negZ ? z : 0.0;
		double maxX = (negX ? 0.0 : x) + 1.0;
		double maxY = (negY ? 0.0 : y) + 1.0;
		double maxZ = (negZ ? 0.0 : z) + 1.0;
		return new Box(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private void buildPredicate() {
		if (pitchRange != null) {
			predicates.add(rotationPredicate(pitchRange, Entity::getPitch));
		}

		if (yawRange != null) {
			predicates.add(rotationPredicate(yawRange, Entity::getYaw));
		}

		if (levelRange != null) {
			predicates.add(entity -> entity instanceof ServerPlayerEntity player
					&& levelRange.test(player.experienceLevel));
		}
	}

	private Predicate<Entity> rotationPredicate(
			NumberRange.AngleRange range,
			Object2FloatFunction<Entity> rotationGetter
	) {
		float minAngle = MathHelper.wrapDegrees(range.getMin().orElse(0.0F));
		float maxAngle = MathHelper.wrapDegrees(range.getMax().orElse(359.0F));
		return entity -> {
			float angle = MathHelper.wrapDegrees(rotationGetter.applyAsFloat(entity));
			return minAngle > maxAngle ? angle >= minAngle || angle <= maxAngle : angle >= minAngle && angle <= maxAngle;
		};
	}

	protected void readAtVariable() throws CommandSyntaxException {
		usesAt = true;
		suggestionProvider = this::suggestSelectorRest;
		if (!reader.canRead()) {
			throw MISSING_EXCEPTION.createWithContext(reader);
		}

		int cursorBeforeSelector = reader.getCursor();
		char selectorChar = reader.read();

		boolean requiresAlive = switch (selectorChar) {
			case ALL_PLAYERS -> {
				limit = Integer.MAX_VALUE;
				includesNonPlayers = false;
				sorter = EntitySelector.ARBITRARY;
				setEntityType(EntityType.PLAYER);
				yield false;
			}
			case ALL_ENTITIES -> {
				limit = Integer.MAX_VALUE;
				includesNonPlayers = true;
				sorter = EntitySelector.ARBITRARY;
				yield true;
			}
			case NEAREST_ENTITY -> {
				limit = 1;
				includesNonPlayers = true;
				sorter = NEAREST;
				yield true;
			}
			case NEAREST_PLAYER -> {
				limit = 1;
				includesNonPlayers = false;
				sorter = NEAREST;
				setEntityType(EntityType.PLAYER);
				yield false;
			}
			case RANDOM_PLAYER -> {
				limit = 1;
				includesNonPlayers = false;
				sorter = RANDOM;
				setEntityType(EntityType.PLAYER);
				yield false;
			}
			case SELF -> {
				limit = 1;
				includesNonPlayers = true;
				senderOnly = true;
				yield false;
			}
			default -> {
				reader.setCursor(cursorBeforeSelector);
				throw UNKNOWN_SELECTOR_EXCEPTION.createWithContext(reader, "@" + selectorChar);
			}
		};

		if (requiresAlive) {
			predicates.add(Entity::isAlive);
		}

		suggestionProvider = this::suggestOpen;
		if (reader.canRead() && reader.peek() == ARGUMENTS_OPENING) {
			reader.skip();
			suggestionProvider = this::suggestOptionOrEnd;
			readArguments();
		}
	}

	protected void readRegular() throws CommandSyntaxException {
		if (reader.canRead()) {
			suggestionProvider = this::suggestNormal;
		}

		int startCursorPos = reader.getCursor();
		String token = reader.readString();

		try {
			uuid = UUID.fromString(token);
			includesNonPlayers = true;
		}
		catch (IllegalArgumentException ignored) {
			if (token.isEmpty() || token.length() > 16) {
				reader.setCursor(startCursorPos);
				throw INVALID_ENTITY_EXCEPTION.createWithContext(reader);
			}

			includesNonPlayers = false;
			playerName = token;
		}

		limit = 1;
	}

	protected void readArguments() throws CommandSyntaxException {
		suggestionProvider = this::suggestOption;
		reader.skipWhitespace();

		while (reader.canRead() && reader.peek() != ARGUMENTS_CLOSING) {
			reader.skipWhitespace();
			int optionCursor = reader.getCursor();
			String optionName = reader.readString();
			EntitySelectorOptions.SelectorHandler handler = EntitySelectorOptions.getHandler(this, optionName, optionCursor);
			reader.skipWhitespace();
			if (!reader.canRead() || reader.peek() != ARGUMENT_DEFINER) {
				reader.setCursor(optionCursor);
				throw VALUELESS_EXCEPTION.createWithContext(reader, optionName);
			}

			reader.skip();
			reader.skipWhitespace();
			suggestionProvider = DEFAULT_SUGGESTION_PROVIDER;
			handler.handle(this);
			reader.skipWhitespace();
			suggestionProvider = this::suggestEndNext;
			if (reader.canRead()) {
				if (reader.peek() != ARGUMENT_SEPARATOR) {
					if (reader.peek() != ARGUMENTS_CLOSING) {
						throw UNTERMINATED_EXCEPTION.createWithContext(reader);
					}
					break;
				}

				reader.skip();
				suggestionProvider = this::suggestOption;
			}
		}

		if (reader.canRead()) {
			reader.skip();
			suggestionProvider = DEFAULT_SUGGESTION_PROVIDER;
		}
		else {
			throw UNTERMINATED_EXCEPTION.createWithContext(reader);
		}
	}

	public boolean readNegationCharacter() {
		reader.skipWhitespace();
		if (reader.canRead() && reader.peek() == INVERT_MODIFIER) {
			reader.skip();
			reader.skipWhitespace();
			return true;
		}

		return false;
	}

	public boolean readTagCharacter() {
		reader.skipWhitespace();
		if (reader.canRead() && reader.peek() == TAG_MODIFIER) {
			reader.skip();
			reader.skipWhitespace();
			return true;
		}

		return false;
	}

	public StringReader getReader() {
		return reader;
	}

	public void addPredicate(Predicate<Entity> predicate) {
		predicates.add(predicate);
	}

	public void setLocalWorldOnly() {
		localWorldOnly = true;
	}

	public NumberRange.@Nullable DoubleRange getDistance() {
		return distance;
	}

	public void setDistance(NumberRange.DoubleRange distance) {
		this.distance = distance;
	}

	public NumberRange.@Nullable IntRange getLevelRange() {
		return levelRange;
	}

	public void setLevelRange(NumberRange.IntRange levelRange) {
		this.levelRange = levelRange;
	}

	public NumberRange.@Nullable AngleRange getPitchRange() {
		return pitchRange;
	}

	public void setPitchRange(NumberRange.AngleRange pitchRange) {
		this.pitchRange = pitchRange;
	}

	public NumberRange.@Nullable AngleRange getYawRange() {
		return yawRange;
	}

	public void setYawRange(NumberRange.AngleRange yawRange) {
		this.yawRange = yawRange;
	}

	public @Nullable Double getX() {
		return x;
	}

	public @Nullable Double getY() {
		return y;
	}

	public @Nullable Double getZ() {
		return z;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public void setDx(double dx) {
		this.dx = dx;
	}

	public void setDy(double dy) {
		this.dy = dy;
	}

	public void setDz(double dz) {
		this.dz = dz;
	}

	public @Nullable Double getDx() {
		return dx;
	}

	public @Nullable Double getDy() {
		return dy;
	}

	public @Nullable Double getDz() {
		return dz;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setIncludesNonPlayers(boolean includesNonPlayers) {
		this.includesNonPlayers = includesNonPlayers;
	}

	public BiConsumer<Vec3d, List<? extends Entity>> getSorter() {
		return sorter;
	}

	public void setSorter(BiConsumer<Vec3d, List<? extends Entity>> sorter) {
		this.sorter = sorter;
	}

	public EntitySelector read() throws CommandSyntaxException {
		startCursor = reader.getCursor();
		suggestionProvider = this::suggestSelector;
		if (reader.canRead() && reader.peek() == SELECTOR_PREFIX) {
			if (!atAllowed) {
				throw NOT_ALLOWED_EXCEPTION.createWithContext(reader);
			}

			reader.skip();
			readAtVariable();
		}
		else {
			readRegular();
		}

		buildPredicate();
		return build();
	}

	private static void suggestSelector(SuggestionsBuilder builder) {
		builder.suggest("@p", Text.translatable("argument.entity.selector.nearestPlayer"));
		builder.suggest("@a", Text.translatable("argument.entity.selector.allPlayers"));
		builder.suggest("@r", Text.translatable("argument.entity.selector.randomPlayer"));
		builder.suggest("@s", Text.translatable("argument.entity.selector.self"));
		builder.suggest("@e", Text.translatable("argument.entity.selector.allEntities"));
		builder.suggest("@n", Text.translatable("argument.entity.selector.nearestEntity"));
	}

	private CompletableFuture<Suggestions> suggestSelector(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		consumer.accept(builder);
		if (atAllowed) {
			suggestSelector(builder);
		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestNormal(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		SuggestionsBuilder offsetBuilder = builder.createOffset(startCursor);
		consumer.accept(offsetBuilder);
		return builder.add(offsetBuilder).buildFuture();
	}

	private CompletableFuture<Suggestions> suggestSelectorRest(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		SuggestionsBuilder offsetBuilder = builder.createOffset(builder.getStart() - 1);
		suggestSelector(offsetBuilder);
		builder.add(offsetBuilder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOpen(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		builder.suggest(String.valueOf(ARGUMENTS_OPENING));
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOptionOrEnd(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		builder.suggest(String.valueOf(ARGUMENTS_CLOSING));
		EntitySelectorOptions.suggestOptions(this, builder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOption(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		EntitySelectorOptions.suggestOptions(this, builder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestEndNext(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		builder.suggest(String.valueOf(ARGUMENT_SEPARATOR));
		builder.suggest(String.valueOf(ARGUMENTS_CLOSING));
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestDefinerNext(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		builder.suggest(String.valueOf(ARGUMENT_DEFINER));
		return builder.buildFuture();
	}

	public boolean isSenderOnly() {
		return senderOnly;
	}

	public void setSuggestionProvider(
			BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestionProvider
	) {
		this.suggestionProvider = suggestionProvider;
	}

	public CompletableFuture<Suggestions> listSuggestions(
			SuggestionsBuilder builder,
			Consumer<SuggestionsBuilder> consumer
	) {
		return suggestionProvider.apply(builder.createOffset(reader.getCursor()), consumer);
	}

	public boolean selectsName() {
		return selectsName;
	}

	public void setSelectsName(boolean selectsName) {
		this.selectsName = selectsName;
	}

	public boolean excludesName() {
		return excludesName;
	}

	public void setExcludesName(boolean excludesName) {
		this.excludesName = excludesName;
	}

	public boolean hasLimit() {
		return hasLimit;
	}

	public void setHasLimit(boolean hasLimit) {
		this.hasLimit = hasLimit;
	}

	public boolean hasSorter() {
		return hasSorter;
	}

	public void setHasSorter(boolean hasSorter) {
		this.hasSorter = hasSorter;
	}

	public boolean selectsGameMode() {
		return selectsGameMode;
	}

	public void setSelectsGameMode(boolean selectsGameMode) {
		this.selectsGameMode = selectsGameMode;
	}

	public boolean excludesGameMode() {
		return excludesGameMode;
	}

	public void setExcludesGameMode(boolean excludesGameMode) {
		this.excludesGameMode = excludesGameMode;
	}

	public boolean selectsTeam() {
		return selectsTeam;
	}

	public void setSelectsTeam(boolean selectsTeam) {
		this.selectsTeam = selectsTeam;
	}

	public boolean excludesTeam() {
		return excludesTeam;
	}

	public void setExcludesTeam(boolean excludesTeam) {
		this.excludesTeam = excludesTeam;
	}

	public void setEntityType(EntityType<?> entityType) {
		this.entityType = entityType;
	}

	public void setExcludesEntityType() {
		excludesEntityType = true;
	}

	public boolean selectsEntityType() {
		return entityType != null;
	}

	public boolean excludesEntityType() {
		return excludesEntityType;
	}

	public boolean selectsScores() {
		return selectsScores;
	}

	public void setSelectsScores(boolean selectsScores) {
		this.selectsScores = selectsScores;
	}

	public boolean selectsAdvancements() {
		return selectsAdvancements;
	}

	public void setSelectsAdvancements(boolean selectsAdvancements) {
		this.selectsAdvancements = selectsAdvancements;
	}
}
