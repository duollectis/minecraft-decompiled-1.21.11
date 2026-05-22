package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.scoreboard.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.management.listener.ManagementListener;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.NameToIdCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Управляет подключёнными игроками на сервере: обрабатывает вход/выход,
 * рассылку пакетов, бан-листы, белый список, операторов, статистику и прогресс достижений.
 * Является центральным узлом для всех операций, связанных с жизненным циклом игрока.
 */
public abstract class PlayerManager {

	public static final File BANNED_PLAYERS_FILE = new File("banned-players.json");
	public static final File BANNED_IPS_FILE = new File("banned-ips.json");
	public static final File OPERATORS_FILE = new File("ops.json");
	public static final File WHITELIST_FILE = new File("whitelist.json");
	public static final Text FILTERED_FULL_TEXT = Text.translatable("chat.filtered_full");
	public static final Text DUPLICATE_LOGIN_TEXT = Text.translatable("multiplayer.disconnect.duplicate_login");
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int LATENCY_UPDATE_INTERVAL = 600;
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z", Locale.ROOT);
	private final MinecraftServer server;
	private final List<ServerPlayerEntity> players = Lists.newArrayList();
	private final Map<UUID, ServerPlayerEntity> playerMap = Maps.newHashMap();
	private final BannedPlayerList bannedProfiles;
	private final BannedIpList bannedIps;
	private final OperatorList ops;
	private final Whitelist whitelist;
	private final Map<UUID, ServerStatHandler> statisticsMap = Maps.newHashMap();
	private final Map<UUID, PlayerAdvancementTracker> advancementTrackers = Maps.newHashMap();
	private final PlayerSaveHandler saveHandler;
	private final CombinedDynamicRegistries<ServerDynamicRegistryType> registryManager;
	private int viewDistance;
	private int simulationDistance;
	private boolean cheatsAllowed;
	private int latencyUpdateTimer;

	public PlayerManager(
			MinecraftServer server,
			CombinedDynamicRegistries<ServerDynamicRegistryType> registryManager,
			PlayerSaveHandler saveHandler,
			ManagementListener managementListener
	) {
		this.server = server;
		this.registryManager = registryManager;
		this.saveHandler = saveHandler;
		this.whitelist = new Whitelist(WHITELIST_FILE, managementListener);
		this.ops = new OperatorList(OPERATORS_FILE, managementListener);
		this.bannedProfiles = new BannedPlayerList(BANNED_PLAYERS_FILE, managementListener);
		this.bannedIps = new BannedIpList(BANNED_IPS_FILE, managementListener);
	}

	/**
	 * Обрабатывает полное подключение игрока к серверу: инициализирует сетевой обработчик,
	 * отправляет все необходимые пакеты синхронизации состояния мира, добавляет игрока
	 * в список активных игроков и уведомляет остальных о входе.
	 *
	 * @param connection активное сетевое соединение клиента
	 * @param player     сущность подключающегося игрока
	 * @param clientData данные клиента (версия, трансфер и т.д.)
	 */
	public void onPlayerConnect(
			ClientConnection connection,
			ServerPlayerEntity player,
			ConnectedClientData clientData
	) {
		PlayerConfigEntry playerConfigEntry = player.getPlayerConfigEntry();
		NameToIdCache nameToIdCache = server.getApiServices().nameToIdCache();
		Optional<PlayerConfigEntry> cached = nameToIdCache.getByUuid(playerConfigEntry.id());
		String previousName = cached.map(PlayerConfigEntry::name).orElse(playerConfigEntry.name());
		nameToIdCache.add(playerConfigEntry);

		ServerWorld world = player.getEntityWorld();
		String addressString = connection.getAddressAsString(server.shouldLogIps());
		LOGGER.info(
				"{}[{}] logged in with entity id {} at ({}, {}, {})",
				new Object[]{
						player.getStringifiedName(),
						addressString,
						player.getId(),
						player.getX(),
						player.getY(),
						player.getZ()
				}
		);

		WorldProperties worldProperties = world.getLevelProperties();
		ServerPlayNetworkHandler networkHandler = new ServerPlayNetworkHandler(server, connection, player, clientData);
		connection.transitionInbound(
				PlayStateFactories.C2S.bind(
						RegistryByteBuf.makeFactory(server.getRegistryManager()),
						networkHandler
				), networkHandler
		);
		networkHandler.disableFlush();

		GameRules gameRules = world.getGameRules();
		boolean immediateRespawn = gameRules.getValue(GameRules.DO_IMMEDIATE_RESPAWN);
		boolean reducedDebugInfo = gameRules.getValue(GameRules.REDUCED_DEBUG_INFO);
		boolean limitedCrafting = gameRules.getValue(GameRules.LIMITED_CRAFTING);
		networkHandler.sendPacket(
				new GameJoinS2CPacket(
						player.getId(),
						worldProperties.isHardcore(),
						server.getWorldRegistryKeys(),
						getMaxPlayerCount(),
						getViewDistance(),
						getSimulationDistance(),
						reducedDebugInfo,
						!immediateRespawn,
						limitedCrafting,
						player.createCommonPlayerSpawnInfo(world),
						server.shouldEnforceSecureProfile()
				)
		);
		networkHandler.sendPacket(new DifficultyS2CPacket(
				worldProperties.getDifficulty(),
				worldProperties.isDifficultyLocked()
		));
		networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
		networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().getSelectedSlot()));

		ServerRecipeManager recipeManager = server.getRecipeManager();
		networkHandler.sendPacket(
				new SynchronizeRecipesS2CPacket(
						recipeManager.getPropertySets(),
						recipeManager.getStonecutterRecipeForSync()
				)
		);
		sendCommandTree(player);
		player.getStatHandler().updateStatSet();
		player.getRecipeBook().sendInitRecipesPacket(player);
		sendScoreboard(world.getScoreboard(), player);
		server.forcePlayerSampleUpdate();

		boolean nameUnchanged = player.getGameProfile().name().equalsIgnoreCase(previousName);
		MutableText joinMessage = nameUnchanged
				? Text.translatable("multiplayer.player.joined", player.getDisplayName())
				: Text.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), previousName);
		broadcast(joinMessage.formatted(Formatting.YELLOW), false);

		networkHandler.requestTeleport(
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYaw(),
				player.getPitch()
		);

		ServerMetadata serverMetadata = server.getServerMetadata();
		if (serverMetadata != null && !clientData.transferred()) {
			player.sendServerMetadata(serverMetadata);
		}

		player.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(players));
		players.add(player);
		playerMap.put(player.getUuid(), player);
		sendToAll(PlayerListS2CPacket.entryFromPlayer(List.of(player)));
		sendWorldInfo(player, world);
		world.onPlayerConnected(player);
		server.getBossBarManager().onPlayerConnect(player);
		sendStatusEffects(player);
		player.onSpawn();
		server.getManagementListener().onPlayerJoined(player);
		networkHandler.enableFlush();
	}

	protected void sendScoreboard(ServerScoreboard scoreboard, ServerPlayerEntity player) {
		Set<ScoreboardObjective> sentObjectives = Sets.newHashSet();

		for (Team team : scoreboard.getTeams()) {
			player.networkHandler.sendPacket(TeamS2CPacket.updateTeam(team, true));
		}

		for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
			ScoreboardObjective objective = scoreboard.getObjectiveForSlot(slot);
			if (objective == null || sentObjectives.contains(objective)) {
				continue;
			}

			for (Packet<?> packet : scoreboard.createChangePackets(objective)) {
				player.networkHandler.sendPacket(packet);
			}

			sentObjectives.add(objective);
		}
	}

	public void setMainWorld(ServerWorld world) {
		world.getWorldBorder().addListener(new WorldBorderListener() {
			@Override
			public void onSizeChange(WorldBorder border, double size) {
				sendToDimension(new WorldBorderSizeChangedS2CPacket(border), world.getRegistryKey());
			}

			@Override
			public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time, long duration) {
				sendToDimension(new WorldBorderInterpolateSizeS2CPacket(border), world.getRegistryKey());
			}

			@Override
			public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
				sendToDimension(new WorldBorderCenterChangedS2CPacket(border), world.getRegistryKey());
			}

			@Override
			public void onWarningTimeChanged(WorldBorder border, int warningTime) {
				sendToDimension(new WorldBorderWarningTimeChangedS2CPacket(border), world.getRegistryKey());
			}

			@Override
			public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
				sendToDimension(new WorldBorderWarningBlocksChangedS2CPacket(border), world.getRegistryKey());
			}

			@Override
			public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {
			}

			@Override
			public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {
			}
		});
	}

	/**
	 * Загружает NBT-данные игрока. Для хоста одиночной игры возвращает данные из свойств сохранения,
	 * для остальных — делегирует чтение в {@link PlayerSaveHandler}.
	 *
	 * @param player профиль игрока, чьи данные нужно загрузить
	 * @return {@link Optional} с NBT-данными, либо пустой, если данных нет
	 */
	public Optional<NbtCompound> loadPlayerData(PlayerConfigEntry player) {
		NbtCompound hostData = server.getSaveProperties().getPlayerData();
		if (server.isHost(player) && hostData != null) {
			LOGGER.debug("loading single player");
			return Optional.of(hostData);
		}

		return saveHandler.loadPlayerData(player);
	}

	protected void savePlayerData(ServerPlayerEntity player) {
		saveHandler.savePlayerData(player);

		ServerStatHandler statHandler = statisticsMap.get(player.getUuid());
		if (statHandler != null) {
			statHandler.save();
		}

		PlayerAdvancementTracker advancementTracker = advancementTrackers.get(player.getUuid());
		if (advancementTracker != null) {
			advancementTracker.save();
		}
	}

	public void remove(ServerPlayerEntity player) {
		ServerWorld world = player.getEntityWorld();
		player.incrementStat(Stats.LEAVE_GAME);
		savePlayerData(player);

		if (player.hasVehicle()) {
			Entity vehicle = player.getRootVehicle();
			if (vehicle.hasPlayerRider()) {
				LOGGER.debug("Removing player mount");
				player.stopRiding();
				vehicle
						.streamPassengersAndSelf()
						.forEach(entity -> entity.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
			}
		}

		player.detach();

		for (EnderPearlEntity pearl : player.getEnderPearls()) {
			pearl.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
		}

		world.removePlayer(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
		player.getAdvancementTracker().clearCriteria();
		players.remove(player);
		server.getBossBarManager().onPlayerDisconnect(player);

		UUID uuid = player.getUuid();
		ServerPlayerEntity mapped = playerMap.get(uuid);
		if (mapped == player) {
			playerMap.remove(uuid);
			statisticsMap.remove(uuid);
			advancementTrackers.remove(uuid);
			server.getManagementListener().onPlayerLeft(player);
		}

		sendToAll(new PlayerRemoveS2CPacket(List.of(player.getUuid())));
	}

	/**
	 * Проверяет, может ли игрок подключиться к серверу, последовательно проверяя
	 * бан-лист профилей, белый список, бан-лист IP и лимит игроков.
	 *
	 * @param address     сетевой адрес подключающегося клиента
	 * @param configEntry профиль игрока
	 * @return текст причины отказа, либо {@code null} если подключение разрешено
	 */
	public @Nullable Text checkCanJoin(SocketAddress address, PlayerConfigEntry configEntry) {
		if (bannedProfiles.contains(configEntry)) {
			BannedPlayerEntry ban = bannedProfiles.get(configEntry);
			MutableText reason = Text.translatable("multiplayer.disconnect.banned.reason", ban.getReasonText());
			if (ban.getExpiryDate() != null) {
				reason.append(Text.translatable(
						"multiplayer.disconnect.banned.expiration",
						DATE_FORMATTER.format(ban.getExpiryDate())
				));
			}

			return reason;
		}

		if (!isWhitelisted(configEntry)) {
			return Text.translatable("multiplayer.disconnect.not_whitelisted");
		}

		if (bannedIps.isBanned(address)) {
			BannedIpEntry ipBan = bannedIps.get(address);
			MutableText reason = Text.translatable("multiplayer.disconnect.banned_ip.reason", ipBan.getReasonText());
			if (ipBan.getExpiryDate() != null) {
				reason.append(Text.translatable(
						"multiplayer.disconnect.banned_ip.expiration",
						DATE_FORMATTER.format(ipBan.getExpiryDate())
				));
			}

			return reason;
		}

		return players.size() >= getMaxPlayerCount() && !canBypassPlayerLimit(configEntry)
				? Text.translatable("multiplayer.disconnect.server_full")
				: null;
	}

	/**
	 * Отключает все активные сессии с указанным UUID, предотвращая дублирующиеся входы.
	 *
	 * @param uuid UUID игрока, чьи дублирующиеся сессии нужно завершить
	 * @return {@code true} если хотя бы одна сессия была отключена
	 */
	public boolean disconnectDuplicateLogins(UUID uuid) {
		Set<ServerPlayerEntity> duplicates = Sets.newIdentityHashSet();

		for (ServerPlayerEntity player : players) {
			if (player.getUuid().equals(uuid)) {
				duplicates.add(player);
			}
		}

		ServerPlayerEntity mappedPlayer = playerMap.get(uuid);
		if (mappedPlayer != null) {
			duplicates.add(mappedPlayer);
		}

		for (ServerPlayerEntity duplicate : duplicates) {
			duplicate.networkHandler.disconnect(DUPLICATE_LOGIN_TEXT);
		}

		return !duplicates.isEmpty();
	}

	/**
	 * Возрождает игрока: создаёт новую сущность, копирует состояние, телепортирует
	 * в точку возрождения и отправляет все необходимые пакеты синхронизации.
	 *
	 * @param player        игрок, которого нужно возродить
	 * @param alive         {@code true} если игрок жив (смена измерения), {@code false} при смерти
	 * @param removalReason причина удаления старой сущности из мира
	 * @return новая сущность игрока после возрождения
	 */
	public ServerPlayerEntity respawnPlayer(
			ServerPlayerEntity player,
			boolean alive,
			Entity.RemovalReason removalReason
	) {
		TeleportTarget teleportTarget = player.getRespawnTarget(!alive, TeleportTarget.NO_OP);
		players.remove(player);
		player.getEntityWorld().removePlayer(player, removalReason);

		ServerWorld spawnWorld = teleportTarget.world();
		ServerPlayerEntity respawned = new ServerPlayerEntity(server, spawnWorld, player.getGameProfile(), player.getClientOptions());
		respawned.networkHandler = player.networkHandler;
		respawned.copyFrom(player, alive);
		respawned.setId(player.getId());
		respawned.setMainArm(player.getMainArm());
		if (!teleportTarget.missingRespawnBlock()) {
			respawned.setSpawnPointFrom(player);
		}

		for (String tag : player.getCommandTags()) {
			respawned.addCommandTag(tag);
		}

		Vec3d spawnPos = teleportTarget.position();
		respawned.refreshPositionAndAngles(
				spawnPos.x,
				spawnPos.y,
				spawnPos.z,
				teleportTarget.yaw(),
				teleportTarget.pitch()
		);
		if (teleportTarget.missingRespawnBlock()) {
			respawned.networkHandler.sendPacket(new GameStateChangeS2CPacket(
					GameStateChangeS2CPacket.NO_RESPAWN_BLOCK,
					0.0F
			));
		}

		byte respawnFlag = (byte) (alive ? 1 : 0);
		ServerWorld respawnedWorld = respawned.getEntityWorld();
		WorldProperties worldProperties = respawnedWorld.getLevelProperties();
		respawned.networkHandler.sendPacket(new PlayerRespawnS2CPacket(
				respawned.createCommonPlayerSpawnInfo(respawnedWorld), respawnFlag
		));
		respawned.networkHandler.requestTeleport(
				respawned.getX(),
				respawned.getY(),
				respawned.getZ(),
				respawned.getYaw(),
				respawned.getPitch()
		);
		respawned.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket(spawnWorld.getSpawnPoint()));
		respawned.networkHandler.sendPacket(new DifficultyS2CPacket(
				worldProperties.getDifficulty(),
				worldProperties.isDifficultyLocked()
		));
		respawned.networkHandler.sendPacket(
				new ExperienceBarUpdateS2CPacket(
						respawned.experienceProgress,
						respawned.totalExperience,
						respawned.experienceLevel
				)
		);
		sendStatusEffects(respawned);
		sendWorldInfo(respawned, spawnWorld);
		sendCommandTree(respawned);
		spawnWorld.onPlayerRespawned(respawned);
		players.add(respawned);
		playerMap.put(respawned.getUuid(), respawned);
		respawned.onSpawn();
		respawned.setHealth(respawned.getHealth());

		ServerPlayerEntity.Respawn respawnData = respawned.getRespawn();
		if (!alive && respawnData != null) {
			WorldProperties.SpawnPoint spawnPoint = respawnData.respawnData();
			ServerWorld respawnAnchorWorld = server.getWorld(spawnPoint.getDimension());
			if (respawnAnchorWorld != null) {
				BlockPos anchorPos = spawnPoint.getPos();
				BlockState anchorState = respawnAnchorWorld.getBlockState(anchorPos);
				if (anchorState.isOf(Blocks.RESPAWN_ANCHOR)) {
					respawned.networkHandler.sendPacket(
							new PlaySoundS2CPacket(
									SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
									SoundCategory.BLOCKS,
									anchorPos.getX(),
									anchorPos.getY(),
									anchorPos.getZ(),
									1.0F,
									1.0F,
									spawnWorld.getRandom().nextLong()
							)
					);
				}
			}
		}

		return respawned;
	}

	public void sendStatusEffects(ServerPlayerEntity player) {
		sendStatusEffects(player, player.networkHandler);
	}

	public void sendStatusEffects(LivingEntity entity, ServerPlayNetworkHandler networkHandler) {
		for (StatusEffectInstance effect : entity.getStatusEffects()) {
			networkHandler.sendPacket(new EntityStatusEffectS2CPacket(entity.getId(), effect, false));
		}
	}

	public void sendCommandTree(ServerPlayerEntity player) {
		LeveledPermissionPredicate permissions = server.getPermissionLevel(player.getPlayerConfigEntry());
		sendCommandTree(player, permissions);
	}

	public void updatePlayerLatency() {
		if (++latencyUpdateTimer > LATENCY_UPDATE_INTERVAL) {
			sendToAll(new PlayerListS2CPacket(
					EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LATENCY),
					players
			));
			latencyUpdateTimer = 0;
		}
	}

	public void sendToAll(Packet<?> packet) {
		for (ServerPlayerEntity player : players) {
			player.networkHandler.sendPacket(packet);
		}
	}

	public void sendToDimension(Packet<?> packet, RegistryKey<World> dimension) {
		for (ServerPlayerEntity player : players) {
			if (player.getEntityWorld().getRegistryKey() == dimension) {
				player.networkHandler.sendPacket(packet);
			}
		}
	}

	public void sendToTeam(PlayerEntity source, Text message) {
		AbstractTeam team = source.getScoreboardTeam();
		if (team == null) {
			return;
		}

		for (String memberName : team.getPlayerList()) {
			ServerPlayerEntity member = getPlayer(memberName);
			if (member != null && member != source) {
				member.sendMessage(message);
			}
		}
	}

	public void sendToOtherTeams(PlayerEntity source, Text message) {
		AbstractTeam team = source.getScoreboardTeam();
		if (team == null) {
			broadcast(message, false);
			return;
		}

		for (ServerPlayerEntity player : players) {
			if (player.getScoreboardTeam() != team) {
				player.sendMessage(message);
			}
		}
	}

	public String[] getPlayerNames() {
		String[] names = new String[players.size()];

		for (int i = 0; i < players.size(); i++) {
			names[i] = players.get(i).getGameProfile().name();
		}

		return names;
	}

	public BannedPlayerList getUserBanList() {
		return bannedProfiles;
	}

	public BannedIpList getIpBanList() {
		return bannedIps;
	}

	public void addToOperators(PlayerConfigEntry player) {
		addToOperators(player, Optional.empty(), Optional.empty());
	}

	public void addToOperators(
			PlayerConfigEntry player,
			Optional<LeveledPermissionPredicate> permissionLevel,
			Optional<Boolean> canBypassPlayerLimit
	) {
		ops.add(
				new OperatorEntry(
						player,
						permissionLevel.orElse(server.getOpPermissionLevel()),
						canBypassPlayerLimit.orElse(ops.canBypassPlayerLimit(player))
				)
		);

		ServerPlayerEntity online = getPlayer(player.id());
		if (online != null) {
			sendCommandTree(online);
		}
	}

	public void removeFromOperators(PlayerConfigEntry player) {
		if (!ops.remove(player)) {
			return;
		}

		ServerPlayerEntity online = getPlayer(player.id());
		if (online != null) {
			sendCommandTree(online);
		}
	}

	private void sendCommandTree(ServerPlayerEntity player, LeveledPermissionPredicate permissions) {
		if (player.networkHandler != null) {
			byte statusId = switch (permissions.getLevel()) {
				case ALL -> 24;
				case MODERATORS -> 25;
				case GAMEMASTERS -> 26;
				case ADMINS -> 27;
				case OWNERS -> 28;
			};
			player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, statusId));
		}

		server.getCommandManager().sendCommandTree(player);
	}

	public boolean isWhitelisted(PlayerConfigEntry player) {
		return !isWhitelistEnabled() || ops.contains(player) || whitelist.contains(player);
	}

	public boolean isOperator(PlayerConfigEntry player) {
		return ops.contains(player)
				|| server.isHost(player) && server.getSaveProperties().areCommandsAllowed()
				|| cheatsAllowed;
	}

	public @Nullable ServerPlayerEntity getPlayer(String name) {
		for (ServerPlayerEntity player : players) {
			if (player.getGameProfile().name().equalsIgnoreCase(name)) {
				return player;
			}
		}

		return null;
	}

	public void sendToAround(
			@Nullable PlayerEntity player,
			double x,
			double y,
			double z,
			double distance,
			RegistryKey<World> worldKey,
			Packet<?> packet
	) {
		for (ServerPlayerEntity candidate : players) {
			if (candidate == player || candidate.getEntityWorld().getRegistryKey() != worldKey) {
				continue;
			}

			double dx = x - candidate.getX();
			double dy = y - candidate.getY();
			double dz = z - candidate.getZ();
			if (dx * dx + dy * dy + dz * dz < distance * distance) {
				candidate.networkHandler.sendPacket(packet);
			}
		}
	}

	public void saveAllPlayerData() {
		for (ServerPlayerEntity player : players) {
			savePlayerData(player);
		}
	}

	public Whitelist getWhitelist() {
		return whitelist;
	}

	public String[] getWhitelistedNames() {
		return whitelist.getNames();
	}

	public OperatorList getOpList() {
		return ops;
	}

	public String[] getOpNames() {
		return ops.getNames();
	}

	public void reloadWhitelist() {
	}

	public void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
		WorldBorder worldBorder = world.getWorldBorder();
		player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(worldBorder));
		player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(
				world.getTime(),
				world.getTimeOfDay(),
				world.getGameRules().getValue(GameRules.ADVANCE_TIME)
		));
		player.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket(world.getSpawnPoint()));

		if (world.isRaining()) {
			player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0.0F));
			player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
					GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED,
					world.getRainGradient(1.0F)
			));
			player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
					GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED,
					world.getThunderGradient(1.0F)
			));
		}

		player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
				GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING,
				0.0F
		));
		server.getTickManager().sendPackets(player);
	}

	public void sendPlayerStatus(ServerPlayerEntity player) {
		player.playerScreenHandler.syncState();
		player.markHealthDirty();
		player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().getSelectedSlot()));
	}

	public int getCurrentPlayerCount() {
		return players.size();
	}

	public int getMaxPlayerCount() {
		return server.getMaxPlayerCount();
	}

	public boolean isWhitelistEnabled() {
		return server.getUseAllowlist();
	}

	public List<ServerPlayerEntity> getPlayersByIp(String ip) {
		List<ServerPlayerEntity> result = Lists.newArrayList();

		for (ServerPlayerEntity player : players) {
			if (player.getIp().equals(ip)) {
				result.add(player);
			}
		}

		return result;
	}

	public int getViewDistance() {
		return viewDistance;
	}

	public int getSimulationDistance() {
		return simulationDistance;
	}

	public MinecraftServer getServer() {
		return server;
	}

	public @Nullable NbtCompound getUserData() {
		return null;
	}

	public void setCheatsAllowed(boolean cheatsAllowed) {
		this.cheatsAllowed = cheatsAllowed;
	}

	public void disconnectAllPlayers() {
		for (ServerPlayerEntity player : players) {
			player.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.server_shutdown"));
		}
	}

	public void broadcast(Text message, boolean overlay) {
		broadcast(message, player -> message, overlay);
	}

	public void broadcast(Text message, Function<ServerPlayerEntity, Text> playerMessageFactory, boolean overlay) {
		server.sendMessage(message);

		for (ServerPlayerEntity player : players) {
			Text playerMessage = playerMessageFactory.apply(player);
			if (playerMessage != null) {
				player.sendMessageToClient(playerMessage, overlay);
			}
		}
	}

	public void broadcast(SignedMessage message, ServerCommandSource source, MessageType.Parameters params) {
		broadcast(message, source::shouldFilterText, source.getPlayer(), params);
	}

	public void broadcast(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
		broadcast(message, sender::shouldFilterMessagesSentTo, sender, params);
	}

	private void broadcast(
			SignedMessage message,
			Predicate<ServerPlayerEntity> shouldSendFiltered,
			@Nullable ServerPlayerEntity sender,
			MessageType.Parameters params
	) {
		boolean verified = verify(message);
		server.logChatMessage(message.getContent(), params, verified ? null : "Not Secure");
		SentMessage sentMessage = SentMessage.of(message);
		boolean anyFiltered = false;

		for (ServerPlayerEntity player : players) {
			boolean sendFiltered = shouldSendFiltered.test(player);
			player.sendChatMessage(sentMessage, sendFiltered, params);
			anyFiltered |= sendFiltered && message.isFullyFiltered();
		}

		if (anyFiltered && sender != null) {
			sender.sendMessage(FILTERED_FULL_TEXT);
		}
	}

	private boolean verify(SignedMessage message) {
		return message.hasSignature() && !message.isExpiredOnServer(Instant.now());
	}

	public ServerStatHandler createStatHandler(PlayerEntity player) {
		GameProfile gameProfile = player.getGameProfile();
		return statisticsMap.computeIfAbsent(
				gameProfile.id(), uuid -> {
					Path statPath = locateStatFilePath(gameProfile);
					return new ServerStatHandler(server, statPath);
				}
		);
	}

	private Path locateStatFilePath(GameProfile profile) {
		Path statsDir = server.getSavePath(WorldSavePath.STATS);
		Path uuidPath = statsDir.resolve(profile.id() + ".json");
		if (Files.exists(uuidPath)) {
			return uuidPath;
		}

		String legacyName = profile.name() + ".json";
		if (PathUtil.isPathSegmentValid(legacyName)) {
			Path legacyPath = statsDir.resolve(legacyName);
			if (Files.isRegularFile(legacyPath)) {
				try {
					return Files.move(legacyPath, uuidPath);
				}
				catch (IOException exception) {
					LOGGER.warn("Failed to copy file {} to {}", legacyName, uuidPath);
					return legacyPath;
				}
			}
		}

		return uuidPath;
	}

	public PlayerAdvancementTracker getAdvancementTracker(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		PlayerAdvancementTracker tracker = advancementTrackers.get(uuid);
		if (tracker == null) {
			Path path = server.getSavePath(WorldSavePath.ADVANCEMENTS).resolve(uuid + ".json");
			tracker = new PlayerAdvancementTracker(
					server.getDataFixer(),
					this,
					server.getAdvancementLoader(),
					path,
					player
			);
			advancementTrackers.put(uuid, tracker);
		}

		tracker.setOwner(player);
		return tracker;
	}

	public void setViewDistance(int viewDistance) {
		this.viewDistance = viewDistance;
		sendToAll(new ChunkLoadDistanceS2CPacket(viewDistance));

		for (ServerWorld world : server.getWorlds()) {
			world.getChunkManager().applyViewDistance(viewDistance);
		}
	}

	public void setSimulationDistance(int simulationDistance) {
		this.simulationDistance = simulationDistance;
		sendToAll(new SimulationDistanceS2CPacket(simulationDistance));

		for (ServerWorld world : server.getWorlds()) {
			world.getChunkManager().applySimulationDistance(simulationDistance);
		}
	}

	public List<ServerPlayerEntity> getPlayerList() {
		return List.copyOf(players);
	}

	public @Nullable ServerPlayerEntity getPlayer(UUID uuid) {
		return playerMap.get(uuid);
	}

	public @Nullable ServerPlayerEntity isAlreadyConnected(String playerName) {
		for (ServerPlayerEntity player : players) {
			if (player.getGameProfile().name().equalsIgnoreCase(playerName)) {
				return player;
			}
		}

		return null;
	}

	public boolean canBypassPlayerLimit(PlayerConfigEntry configEntry) {
		return false;
	}

	public void onDataPacksReloaded() {
		for (PlayerAdvancementTracker tracker : advancementTrackers.values()) {
			tracker.reload(server.getAdvancementLoader());
		}

		sendToAll(new SynchronizeTagsS2CPacket(TagPacketSerializer.serializeTags(registryManager)));
		ServerRecipeManager recipeManager = server.getRecipeManager();
		SynchronizeRecipesS2CPacket recipesPacket = new SynchronizeRecipesS2CPacket(
				recipeManager.getPropertySets(), recipeManager.getStonecutterRecipeForSync()
		);

		for (ServerPlayerEntity player : players) {
			player.networkHandler.sendPacket(recipesPacket);
			player.getRecipeBook().sendInitRecipesPacket(player);
		}
	}

	public boolean areCheatsAllowed() {
		return cheatsAllowed;
	}
}
