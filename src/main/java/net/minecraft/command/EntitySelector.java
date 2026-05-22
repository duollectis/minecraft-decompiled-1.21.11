package net.minecraft.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@code EntitySelector}.
 */
public class EntitySelector {

	public static final int MAX_VALUE = Integer.MAX_VALUE;
	public static final BiConsumer<Vec3d, List<? extends Entity>> ARBITRARY = (pos, entities) -> {};
	private static final TypeFilter<Entity, ?> PASSTHROUGH_FILTER = new TypeFilter<Entity, Entity>() {
		public Entity downcast(Entity entity) {
			return entity;
		}

		@Override
		public Class<? extends Entity> getBaseClass() {
			return Entity.class;
		}
	};
	private final int limit;
	private final boolean includesNonPlayers;
	private final boolean localWorldOnly;
	private final List<Predicate<Entity>> predicates;
	private final NumberRange.@Nullable DoubleRange distance;
	private final Function<Vec3d, Vec3d> positionOffset;
	private final @Nullable Box box;
	private final BiConsumer<Vec3d, List<? extends Entity>> sorter;
	private final boolean senderOnly;
	private final @Nullable String playerName;
	private final @Nullable UUID uuid;
	private final TypeFilter<Entity, ?> entityFilter;
	private final boolean usesAt;

	public EntitySelector(
			int count,
			boolean includesNonPlayers,
			boolean localWorldOnly,
			List<Predicate<Entity>> predicates,
			NumberRange.@Nullable DoubleRange distance,
			Function<Vec3d, Vec3d> positionOffset,
			@Nullable Box box,
			BiConsumer<Vec3d, List<? extends Entity>> sorter,
			boolean senderOnly,
			@Nullable String playerName,
			@Nullable UUID uuid,
			@Nullable EntityType<?> type,
			boolean usesAt
	) {
		this.limit = count;
		this.includesNonPlayers = includesNonPlayers;
		this.localWorldOnly = localWorldOnly;
		this.predicates = predicates;
		this.distance = distance;
		this.positionOffset = positionOffset;
		this.box = box;
		this.sorter = sorter;
		this.senderOnly = senderOnly;
		this.playerName = playerName;
		this.uuid = uuid;
		this.entityFilter = (TypeFilter<Entity, ?>) (type == null ? PASSTHROUGH_FILTER : type);
		this.usesAt = usesAt;
	}

	public int getLimit() {
		return this.limit;
	}

	public boolean includesNonPlayers() {
		return this.includesNonPlayers;
	}

	public boolean isSenderOnly() {
		return this.senderOnly;
	}

	public boolean isLocalWorldOnly() {
		return this.localWorldOnly;
	}

	public boolean usesAt() {
		return this.usesAt;
	}

	private void checkSourcePermission(ServerCommandSource source) throws CommandSyntaxException {
		if (usesAt && !source.getPermissions().hasPermission(DefaultPermissions.ENTITY_SELECTORS)) {
			throw EntityArgumentType.NOT_ALLOWED_EXCEPTION.create();
		}
	}

	public Entity getEntity(ServerCommandSource source) throws CommandSyntaxException {
		checkSourcePermission(source);
		List<? extends Entity> entities = getEntities(source);
		if (entities.isEmpty()) {
			throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		else if (entities.size() > 1) {
			throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
		}
		else {
			return entities.get(0);
		}
	}

	public List<? extends Entity> getEntities(ServerCommandSource source) throws CommandSyntaxException {
		checkSourcePermission(source);
		if (!includesNonPlayers) {
			return getPlayers(source);
		}
		else if (playerName != null) {
			ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(playerName);
			return player == null ? List.of() : List.of(player);
		}
		else if (uuid != null) {
			for (ServerWorld world : source.getServer().getWorlds()) {
				Entity entity = world.getEntity(uuid);
				if (entity != null) {
					if (entity.getType().isEnabled(source.getEnabledFeatures())) {
						return List.of(entity);
					}
					break;
				}
			}

			return List.of();
		}
		else {
			Vec3d pos = positionOffset.apply(source.getPosition());
			Box offsetBox = getOffsetBox(pos);
			if (senderOnly) {
				Predicate<Entity> predicate = getPositionPredicate(pos, offsetBox, null);
				return source.getEntity() != null && predicate.test(source.getEntity())
						? List.of(source.getEntity())
						: List.of();
			}
			else {
				Predicate<Entity> predicate = getPositionPredicate(pos, offsetBox, source.getEnabledFeatures());
				List<Entity> result = new ObjectArrayList();
				if (isLocalWorldOnly()) {
					appendEntitiesFromWorld(result, source.getWorld(), offsetBox, predicate);
				}
				else {
					for (ServerWorld world : source.getServer().getWorlds()) {
						appendEntitiesFromWorld(result, world, offsetBox, predicate);
					}
				}

				return getEntities(pos, result);
			}
		}
	}

	private void appendEntitiesFromWorld(
			List<Entity> entities,
			ServerWorld world,
			@Nullable Box box,
			Predicate<Entity> predicate
	) {
		int appendLimit = getAppendLimit();
		if (entities.size() < appendLimit) {
			if (box != null) {
				world.collectEntitiesByType(entityFilter, box, predicate, entities, appendLimit);
			}
			else {
				world.collectEntitiesByType(entityFilter, predicate, entities, appendLimit);
			}
		}
	}

	private int getAppendLimit() {
		return sorter == ARBITRARY ? limit : Integer.MAX_VALUE;
	}

	public ServerPlayerEntity getPlayer(ServerCommandSource source) throws CommandSyntaxException {
		checkSourcePermission(source);
		List<ServerPlayerEntity> players = getPlayers(source);
		if (players.size() != 1) {
			throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
		}
		else {
			return players.get(0);
		}
	}

	public List<ServerPlayerEntity> getPlayers(ServerCommandSource source) throws CommandSyntaxException {
		checkSourcePermission(source);
		if (playerName != null) {
			ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(playerName);
			return player == null ? List.of() : List.of(player);
		}
		else if (uuid != null) {
			ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(uuid);
			return player == null ? List.of() : List.of(player);
		}
		else {
			Vec3d pos = positionOffset.apply(source.getPosition());
			Box offsetBox = getOffsetBox(pos);
			Predicate<Entity> predicate = getPositionPredicate(pos, offsetBox, null);
			if (senderOnly) {
				return source.getEntity() instanceof ServerPlayerEntity senderPlayer && predicate.test(senderPlayer)
						? List.of(senderPlayer)
						: List.of();
			}
			else {
				int appendLimit = getAppendLimit();
				List<ServerPlayerEntity> players;
				if (isLocalWorldOnly()) {
					players = source.getWorld().getPlayers(predicate, appendLimit);
				}
				else {
					players = new ObjectArrayList();

					for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
						if (predicate.test(player)) {
							players.add(player);
							if (players.size() >= appendLimit) {
								return players;
							}
						}
					}
				}

				return getEntities(pos, players);
			}
		}
	}

	private @Nullable Box getOffsetBox(Vec3d offset) {
		return box != null ? box.offset(offset) : null;
	}

	private Predicate<Entity> getPositionPredicate(Vec3d pos, @Nullable Box box, @Nullable FeatureSet enabledFeatures) {
		boolean checkFeatures = enabledFeatures != null;
		boolean checkBox = box != null;
		boolean checkDistance = distance != null;
		int extraCount = (checkFeatures ? 1 : 0) + (checkBox ? 1 : 0) + (checkDistance ? 1 : 0);
		if (extraCount == 0) {
			return Util.allOf(predicates);
		}

		List<Predicate<Entity>> combined = new ObjectArrayList(predicates.size() + extraCount);
		combined.addAll(predicates);
		if (checkFeatures) {
			combined.add(entity -> entity.getType().isEnabled(enabledFeatures));
		}

		if (checkBox) {
			combined.add(entity -> box.intersects(entity.getBoundingBox()));
		}

		if (checkDistance) {
			combined.add(entity -> distance.testSqrt(entity.squaredDistanceTo(pos)));
		}

		return Util.allOf(combined);
	}

	private <T extends Entity> List<T> getEntities(Vec3d pos, List<T> entities) {
		if (entities.size() > 1) {
			sorter.accept(pos, entities);
		}

		return entities.subList(0, Math.min(limit, entities.size()));
	}

	public static Text getNames(List<? extends Entity> entities) {
		return Texts.join(entities, Entity::getDisplayName);
	}
}
