package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.dialog.DialogNetworkAccess;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.ClientRecipeManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.sound.*;
import net.minecraft.client.toast.RecipeToast;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.client.world.ClientWaypointHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.DataCache;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.SignedArgumentList;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.encryption.*;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.message.*;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.state.ConfigurationStates;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.*;
import net.minecraft.screen.*;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.server.command.CommandManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.storage.NbtReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.HashCodeOps;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.tick.TickManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Клиентский обработчик сетевых пакетов фазы PLAY.
 * <p>
 * Принимает и обрабатывает все серверные пакеты (S2C) во время активной игровой сессии:
 * спавн сущностей, обновление мира, чат, инвентарь, эффекты, телепортацию и т.д.
 * Также отвечает за отправку клиентских пакетов (C2S): движение, чат, команды.
 */
@Environment(EnvType.CLIENT)
public class ClientPlayNetworkHandler extends ClientCommonNetworkHandler implements ClientPlayPacketListener, TickablePacketListener {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text UNSECURE_SERVER_TOAST_TITLE = Text.translatable("multiplayer.unsecureserver.toast.title");
	private static final Text UNSECURE_SERVER_TOAST_TEXT = Text.translatable("multiplayer.unsecureserver.toast");
	private static final Text INVALID_PACKET_TEXT = Text.translatable("multiplayer.disconnect.invalid_packet");
	private static final Text RECONFIGURING_TEXT = Text.translatable("connect.reconfiguring");
	private static final Text BAD_CHAT_INDEX_TEXT = Text.translatable("multiplayer.disconnect.bad_chat_index");
	private static final Text CONFIRM_COMMAND_TITLE_TEXT = Text.translatable("multiplayer.confirm_command.title");
	private static final Text CONFIRM_RUN_COMMAND_TEXT = Text.translatable("multiplayer.confirm_command.run_command");
	private static final Text
			CONFIRM_SUGGEST_COMMAND_TEXT =
			Text.translatable("multiplayer.confirm_command.suggest_command");
	private static final int ACKNOWLEDGMENT_BATCH_SIZE = 64;
	public static final int CHUNK_BATCH_SIZE = 64;
	private static final Permission RESTRICTED_PERMISSION = Permission.Atom.ofVanilla("client/commands/restricted");
	static final PermissionCheck RESTRICTED_PERMISSION_CHECK = new PermissionCheck.Require(RESTRICTED_PERMISSION);
	private static final PermissionPredicate
			RESTRICTED_PERMISSION_PREDICATE =
			permission -> permission.equals(RESTRICTED_PERMISSION);
	private static final CommandTreeS2CPacket.NodeFactory<ClientCommandSource>
			COMMAND_NODE_FACTORY =
			new CommandTreeS2CPacket.NodeFactory<ClientCommandSource>() {
				@Override
				public ArgumentBuilder<ClientCommandSource, ?> literal(String name) {
					return LiteralArgumentBuilder.literal(name);
				}

				@Override
				public ArgumentBuilder<ClientCommandSource, ?> argument(
						String name,
						ArgumentType<?> type,
						@Nullable Identifier suggestionProviderId
				) {
					RequiredArgumentBuilder<ClientCommandSource, ?>
							builder =
							RequiredArgumentBuilder.argument(name, type);

					if (suggestionProviderId != null) {
						builder.suggests(SuggestionProviders.byId(suggestionProviderId));
					}

					return builder;
				}

				@Override
				public ArgumentBuilder<ClientCommandSource, ?> modifyNode(
						ArgumentBuilder<ClientCommandSource, ?> arg,
						boolean disableExecution,
						boolean requireTrusted
				) {
					if (disableExecution) {
						arg.executes(context -> 0);
					}

					if (requireTrusted) {
						arg.requires(CommandManager.requirePermissionLevel(RESTRICTED_PERMISSION_CHECK));
					}

					return arg;
				}
			};

	private final GameProfile profile;
	private ClientWorld world;
	private ClientWorld.Properties worldProperties;
	private final Map<UUID, PlayerListEntry> playerListEntries = Maps.newHashMap();
	private final Set<PlayerListEntry> listedPlayerListEntries = new ReferenceOpenHashSet();
	private final ClientAdvancementManager advancementHandler;
	private final ClientCommandSource commandSource;
	private final ClientCommandSource restrictedCommandSource;
	private final DataQueryHandler dataQueryHandler = new DataQueryHandler(this);
	private int chunkLoadDistance = 3;
	private int simulationDistance = 3;
	private final Random random = Random.createThreadSafe();
	private CommandDispatcher<ClientCommandSource> commandDispatcher = new CommandDispatcher();
	private ClientRecipeManager
			recipeManager =
			new ClientRecipeManager(Map.of(), CuttingRecipeDisplay.Grouping.empty());
	private final UUID sessionId = UUID.randomUUID();
	private Set<RegistryKey<World>> worldKeys;
	private final DynamicRegistryManager.Immutable combinedDynamicRegistries;
	private final FeatureSet enabledFeatures;
	private final BrewingRecipeRegistry brewingRecipeRegistry;
	private FuelRegistry fuelRegistry;
	private final ComponentChangesHash.ComponentHasher componentHasher;
	private OptionalInt removedPlayerVehicleId = OptionalInt.empty();
	private @Nullable ClientPlayerSession session;
	private MessageChain.Packer messagePacker = MessageChain.Packer.NONE;
	private int globalChatMessageIndex;
	private LastSeenMessagesCollector lastSeenMessagesCollector = new LastSeenMessagesCollector(20);
	private MessageSignatureStorage signatureStorage = MessageSignatureStorage.create();
	private @Nullable CompletableFuture<Optional<PlayerKeyPair>> profileKeyPairFuture;
	private @Nullable SyncedClientOptions syncedOptions;
	private final ChunkBatchSizeCalculator chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
	private final PingMeasurer pingMeasurer;
	private final ClientDebugSubscriptionManager debugSubscriptionManager;
	private @Nullable ClientChunkLoadProgress chunkLoadProgress;
	private boolean secureChatEnforced;
	private volatile boolean worldCleared;
	private final Scoreboard scoreboard = new Scoreboard();
	private final ClientWaypointHandler waypointHandler = new ClientWaypointHandler();
	private final SearchManager searchManager = new SearchManager();
	private final List<WeakReference<DataCache<?, ?>>> cachedData = new ArrayList<>();
	private boolean playerLoaded;

	/**
	 * Создаёт обработчик пакетов фазы PLAY.
	 * Инициализирует источники команд, менеджер достижений, измеритель пинга и прочие подсистемы.
	 */
	public ClientPlayNetworkHandler(
			MinecraftClient client,
			ClientConnection clientConnection,
			ClientConnectionState clientConnectionState
	) {
		super(client, clientConnection, clientConnectionState);
		profile = clientConnectionState.localGameProfile();
		combinedDynamicRegistries = clientConnectionState.receivedRegistries();
		RegistryOps<HashCode> registryOps = combinedDynamicRegistries.getOps(HashCodeOps.INSTANCE);
		componentHasher = component -> ((HashCode) component.encode(registryOps)
		                                                    .getOrThrow(error -> new IllegalArgumentException(
				                                                    "Failed to hash " + component + ": " + error))
		)
				.asInt();
		enabledFeatures = clientConnectionState.enabledFeatures();
		advancementHandler = new ClientAdvancementManager(client, worldSession);
		PermissionPredicate permissionPredicate = permission -> {
			ClientPlayerEntity player = client.player;
			return player != null && player.getPermissions().hasPermission(permission);
		};
		commandSource = new ClientCommandSource(this, client, permissionPredicate.or(RESTRICTED_PERMISSION_PREDICATE));
		restrictedCommandSource = new ClientCommandSource(this, client, PermissionPredicate.NONE);
		pingMeasurer = new PingMeasurer(this, client.getDebugHud().getPingLog());
		debugSubscriptionManager = new ClientDebugSubscriptionManager(this, client.getDebugHud());

		if (clientConnectionState.chatState() != null) {
			client.inGameHud.getChatHud().restoreChatState(clientConnectionState.chatState());
		}

		brewingRecipeRegistry = BrewingRecipeRegistry.create(enabledFeatures);
		fuelRegistry = FuelRegistry.createDefault(clientConnectionState.receivedRegistries(), enabledFeatures);
		chunkLoadProgress = clientConnectionState.chunkLoadProgress();
	}

	/**
	 * Возвращает источник команд текущего игрока (с расширенными правами).
	 */
	public ClientCommandSource getCommandSource() {
		return commandSource;
	}

	/**
	 * Выгружает текущий мир и уведомляет игровую сессию.
	 */
	public void unloadWorld() {
		worldCleared = true;
		clearWorld();
		worldSession.onUnload();
	}

	/**
	 * Очищает ссылки на мир и прогресс загрузки чанков.
	 */
	public void clearWorld() {
		clearCachedData();
		world = null;
		chunkLoadProgress = null;
	}

	/** Очищает кэшированные данные всех зарегистрированных DataCache. */
	private void clearCachedData() {
		for (WeakReference<DataCache<?, ?>> weakReference : cachedData) {
			DataCache<?, ?> dataCache = weakReference.get();

			if (dataCache != null) {
				dataCache.clean();
			}
		}

		cachedData.clear();
	}

	/**
	 * Возвращает менеджер рецептов текущей сессии.
	 */
	public RecipeManager getRecipeManager() {
		return recipeManager;
	}

	/** Обрабатывает пакет входа в игру: создаёт мир, игрока и инициализирует игровую сессию. */
	@Override
	public void onGameJoin(GameJoinS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.interactionManager = new ClientPlayerInteractionManager(client, this);
		CommonPlayerSpawnInfo commonPlayerSpawnInfo = packet.commonPlayerSpawnInfo();
		List<RegistryKey<World>> list = Lists.newArrayList(packet.dimensionIds());
		Collections.shuffle(list);
		worldKeys = Sets.newLinkedHashSet(list);
		RegistryKey<World> registryKey = commonPlayerSpawnInfo.dimension();
		RegistryEntry<DimensionType> registryEntry = commonPlayerSpawnInfo.dimensionType();
		chunkLoadDistance = packet.viewDistance();
		simulationDistance = packet.simulationDistance();
		boolean bl = commonPlayerSpawnInfo.isDebug();
		boolean bl2 = commonPlayerSpawnInfo.isFlat();
		int i = commonPlayerSpawnInfo.seaLevel();
		ClientWorld.Properties properties = new ClientWorld.Properties(Difficulty.NORMAL, packet.hardcore(), bl2);
		worldProperties = properties;
		world = new ClientWorld(
				this,
				properties,
				registryKey,
				registryEntry,
				chunkLoadDistance,
				simulationDistance,
				client.worldRenderer,
				bl,
				commonPlayerSpawnInfo.seed(),
				i
		);
		client.joinWorld(world);
		if (client.player == null) {
			client.player = client.interactionManager.createPlayer(world, new StatHandler(), new ClientRecipeBook());
			client.player.setYaw(-180.0F);
			if (client.getServer() != null) {
				client.getServer().setLocalPlayerUuid(client.player.getUuid());
			}
		}

		setPlayerLoadedState(false);
		debugSubscriptionManager.clearAllSubscriptions();
		client.worldRenderer.debugRenderer.initRenderers();
		client.player.init();
		client.player.setId(packet.playerEntityId());
		world.addEntity(client.player);
		client.player.input = new KeyboardInput(client.options);
		client.interactionManager.copyAbilities(client.player);
		client.setCameraEntity(client.player);
		startWorldLoading(client.player, world, LevelLoadingScreen.WorldEntryReason.OTHER);
		client.player.setReducedDebugInfo(packet.reducedDebugInfo());
		client.player.setShowsDeathScreen(packet.showDeathScreen());
		client.player.setLimitedCraftingEnabled(packet.doLimitedCrafting());
		client.player.setLastDeathPos(commonPlayerSpawnInfo.lastDeathLocation());
		client.player.setPortalCooldown(commonPlayerSpawnInfo.portalCooldown());
		client.interactionManager.setGameModes(commonPlayerSpawnInfo.gameMode(), commonPlayerSpawnInfo.lastGameMode());
		client.options.setServerViewDistance(packet.viewDistance());
		session = null;
		messagePacker = MessageChain.Packer.NONE;
		globalChatMessageIndex = 0;
		lastSeenMessagesCollector = new LastSeenMessagesCollector(20);
		signatureStorage = MessageSignatureStorage.create();
		if (connection.isEncrypted()) {
			fetchProfileKey();
		}

		worldSession.setGameMode(commonPlayerSpawnInfo.gameMode(), packet.hardcore());
		client.getQuickPlayLogger().save(client);
		secureChatEnforced = packet.enforcesSecureChat();
		if (serverInfo != null && !seenInsecureChatWarning && !isSecureChatEnforced()) {
			SystemToast systemToast = SystemToast.create(
					client,
					SystemToast.Type.UNSECURE_SERVER_WARNING,
					UNSECURE_SERVER_TOAST_TITLE,
					UNSECURE_SERVER_TOAST_TEXT
			);
			client.getToastManager().add(systemToast);
			seenInsecureChatWarning = true;
		}
	}

	/** Обрабатывает спавн новой сущности в мире. */
	@Override
	public void onEntitySpawn(EntitySpawnS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (removedPlayerVehicleId.isPresent() && removedPlayerVehicleId.getAsInt() == packet.getEntityId()) {
			removedPlayerVehicleId = OptionalInt.empty();
		}

		Entity entity = createEntity(packet);
		if (entity != null) {
			entity.onSpawnPacket(packet);
			world.addEntity(entity);
			playSpawnSound(entity);
		}
		else {
			LOGGER.warn("Skipping Entity with id {}", packet.getEntityType());
		}

		if (entity instanceof PlayerEntity playerEntity) {
			UUID uUID = playerEntity.getUuid();
			PlayerListEntry playerListEntry = playerListEntries.get(uUID);
			if (playerListEntry != null) {
				seenPlayers.put(uUID, playerListEntry);
			}
		}
	}

	/** Создаёт клиентскую сущность по данным пакета спавна. */
	private @Nullable Entity createEntity(EntitySpawnS2CPacket packet) {
		EntityType<?> entityType = packet.getEntityType();
		if (entityType == EntityType.PLAYER) {
			PlayerListEntry playerListEntry = getPlayerListEntry(packet.getUuid());
			if (playerListEntry == null) {
				LOGGER.warn(
						"Server attempted to add player prior to sending player info (Player id {})",
						packet.getUuid()
				);
				return null;
			}
			else {
				return new OtherClientPlayerEntity(world, playerListEntry.getProfile());
			}
		}
		else {
			return entityType.create(world, SpawnReason.LOAD);
		}
	}

	/** Запускает звук для только что заспавненной сущности (тележка, пчела). */
	private void playSpawnSound(Entity entity) {
		if (entity instanceof AbstractMinecartEntity abstractMinecartEntity) {
			client.getSoundManager().play(new MovingMinecartSoundInstance(abstractMinecartEntity));
		}
		else if (entity instanceof BeeEntity beeEntity) {
			boolean bl = beeEntity.hasAngerTime();
			AbstractBeeSoundInstance abstractBeeSoundInstance;
			if (bl) {
				abstractBeeSoundInstance = new AggressiveBeeSoundInstance(beeEntity);
			}
			else {
				abstractBeeSoundInstance = new PassiveBeeSoundInstance(beeEntity);
			}

			client.getSoundManager().playNextTick(abstractBeeSoundInstance);
		}
	}

	/** Обновляет скорость сущности по данным сервера. */
	@Override
	public void onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		if (entity != null) {
			entity.setVelocityClient(packet.getVelocity());
		}
	}

	/** Обновляет отслеживаемые данные сущности (DataTracker). */
	@Override
	public void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.id());
		if (entity != null) {
			entity.getDataTracker().writeUpdatedEntries(packet.trackedValues());
		}
	}

	/** Синхронизирует позицию и углы сущности с сервером. */
	@Override
	public void onEntityPositionSync(EntityPositionSyncS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.id());
		if (entity != null) {
			Vec3d vec3d = packet.values().position();
			entity.getTrackedPosition().setPos(vec3d);
			if (!entity.isLogicalSideForUpdatingMovement()) {
				float f = packet.values().yaw();
				float g = packet.values().pitch();
				boolean bl = entity.getEntityPos().squaredDistanceTo(vec3d) > 4096.0;
				if (world.hasEntity(entity) && !bl) {
					entity.updateTrackedPositionAndAngles(vec3d, f, g);
				}
				else {
					entity.refreshPositionAndAngles(vec3d, f, g);
				}

				if (!entity.isInterpolating() && entity.hasPassengerDeep(client.player)) {
					entity.updatePassengerPosition(client.player);
					client.player.resetPosition();
				}

				entity.setOnGround(packet.onGround());
			}
		}
	}

	/** Обновляет позицию сущности (абсолютная телепортация). */
	@Override
	public void onEntityPosition(EntityPositionS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.entityId());
		if (entity == null) {
			if (removedPlayerVehicleId.isPresent() && removedPlayerVehicleId.getAsInt() == packet.entityId()) {
				LOGGER.debug(
						"Trying to teleport entity with id {}, that was formerly player vehicle, applying teleport to player instead",
						packet.entityId()
				);
				setPosition(packet.change(), packet.relatives(), client.player, false);
				connection
						.send(
								new PlayerMoveC2SPacket.Full(
										client.player.getX(),
										client.player.getY(),
										client.player.getZ(),
										client.player.getYaw(),
										client.player.getPitch(),
										false,
										false
								)
						);
			}
		}
		else {
			boolean
					bl =
					packet.relatives().contains(PositionFlag.X) || packet.relatives().contains(PositionFlag.Y) || packet
							.relatives()
							.contains(PositionFlag.Z);
			boolean bl2 = world.hasEntity(entity) || !entity.isLogicalSideForUpdatingMovement() || bl;
			boolean bl3 = setPosition(packet.change(), packet.relatives(), entity, bl2);
			entity.setOnGround(packet.onGround());
			if (!bl3 && entity.hasPassengerDeep(client.player)) {
				entity.updatePassengerPosition(client.player);
				client.player.resetPosition();
				if (entity.isLogicalSideForUpdatingMovement()) {
					connection.send(VehicleMoveC2SPacket.fromVehicle(entity));
				}
			}
		}
	}

	/** Обновляет скорость тиков. */
	@Override
	public void onUpdateTickRate(UpdateTickRateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (client.world != null) {
			TickManager tickManager = client.world.getTickManager();
			tickManager.setTickRate(packet.tickRate());
			tickManager.setFrozen(packet.isFrozen());
		}
	}

	/** Обрабатывает шаг тика (для отладки). */
	@Override
	public void onTickStep(TickStepS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (client.world != null) {
			TickManager tickManager = client.world.getTickManager();
			tickManager.setStepTicks(packet.tickSteps());
		}
	}

	/** Обновляет выбранный слот хотбара. */
	@Override
	public void onUpdateSelectedSlot(UpdateSelectedSlotS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (PlayerInventory.isValidHotbarIndex(packet.slot())) {
			client.player.getInventory().setSelectedSlot(packet.slot());
		}
	}

	@Override
	public void onEntity(EntityS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = packet.getEntity(world);
		if (entity != null) {
			if (entity.isLogicalSideForUpdatingMovement()) {
				TrackedPosition trackedPosition = entity.getTrackedPosition();
				Vec3d vec3d = trackedPosition.withDelta(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
				trackedPosition.setPos(vec3d);
			}
			else {
				if (packet.isPositionChanged()) {
					TrackedPosition trackedPosition = entity.getTrackedPosition();
					Vec3d vec3d = trackedPosition.withDelta(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
					trackedPosition.setPos(vec3d);
					if (packet.hasRotation()) {
						entity.updateTrackedPositionAndAngles(vec3d, packet.getYaw(), packet.getPitch());
					}
					else {
						entity.updateTrackedPosition(vec3d);
					}
				}
				else if (packet.hasRotation()) {
					entity.updateTrackedAngles(packet.getYaw(), packet.getPitch());
				}

				entity.setOnGround(packet.isOnGround());
			}
		}
	}

	/** Обновляет движение тележки по рельсам. */
	@Override
	public void onMoveMinecartAlongTrack(MoveMinecartAlongTrackS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (packet.getEntity(world) instanceof AbstractMinecartEntity abstractMinecartEntity) {
			if (abstractMinecartEntity.getController() instanceof ExperimentalMinecartController experimentalMinecartController) {
				experimentalMinecartController.stagingLerpSteps.addAll(packet.lerpSteps());
			}
		}
	}

	/** Устанавливает угол поворота головы сущности. */
	@Override
	public void onEntitySetHeadYaw(EntitySetHeadYawS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = packet.getEntity(world);
		if (entity != null) {
			entity.updateTrackedHeadRotation(packet.getHeadYaw(), 3);
		}
	}

	/** Удаляет сущности из мира по их идентификаторам. */
	@Override
	public void onEntitiesDestroy(EntitiesDestroyS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		packet.getEntityIds().forEach(id -> {
			Entity entity = world.getEntityById(id);
			if (entity != null) {
				if (entity.hasPassengerDeep(client.player)) {
					LOGGER.debug("Remove entity {}:{} that has player as passenger", entity.getType(), id);
					removedPlayerVehicleId = OptionalInt.of(id);
				}

				world.removeEntity(id, Entity.RemovalReason.DISCARDED);
				debugSubscriptionManager.removeEntity(entity);
			}
		});
	}

	/** Телепортирует игрока на позицию, указанную сервером. */
	@Override
	public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		if (!playerEntity.hasVehicle()) {
			setPosition(packet.change(), packet.relatives(), playerEntity, false);
		}

		connection.send(new TeleportConfirmC2SPacket(packet.teleportId()));
		connection
				.send(
						new PlayerMoveC2SPacket.Full(
								playerEntity.getX(),
								playerEntity.getY(),
								playerEntity.getZ(),
								playerEntity.getYaw(),
								playerEntity.getPitch(),
								false,
								false
						)
				);
	}

	/** Применяет позицию к сущности с учётом флагов относительности. */
	private static boolean setPosition(EntityPosition pos, Set<PositionFlag> flags, Entity entity, boolean bl) {
		EntityPosition entityPosition = EntityPosition.fromEntity(entity);
		EntityPosition entityPosition2 = EntityPosition.apply(entityPosition, pos, flags);
		boolean bl2 = entityPosition.position().squaredDistanceTo(entityPosition2.position()) > 4096.0;
		if (bl && !bl2) {
			entity.updateTrackedPositionAndAngles(
					entityPosition2.position(),
					entityPosition2.yaw(),
					entityPosition2.pitch()
			);
			entity.setVelocity(entityPosition2.deltaMovement());
			return true;
		}
		else {
			entity.setPosition(entityPosition2.position());
			entity.setVelocity(entityPosition2.deltaMovement());
			entity.setYaw(entityPosition2.yaw());
			entity.setPitch(entityPosition2.pitch());
			EntityPosition
					entityPosition3 =
					new EntityPosition(entity.getLastRenderPos(), Vec3d.ZERO, entity.lastYaw, entity.lastPitch);
			EntityPosition entityPosition4 = EntityPosition.apply(entityPosition3, pos, flags);
			entity.setLastPositionAndAngles(entityPosition4.position(), entityPosition4.yaw(), entityPosition4.pitch());
			return false;
		}
	}

	/** Принудительно поворачивает игрока на угол, заданный сервером. */
	@Override
	public void onPlayerRotation(PlayerRotationS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		Set<PositionFlag> set = PositionFlag.ofRot(packet.relativeYaw(), packet.relativePitch());
		EntityPosition entityPosition = EntityPosition.fromEntity(playerEntity);
		EntityPosition
				entityPosition2 =
				EntityPosition.apply(entityPosition, entityPosition.withRotation(packet.yaw(), packet.pitch()), set);
		playerEntity.setYaw(entityPosition2.yaw());
		playerEntity.setPitch(entityPosition2.pitch());
		playerEntity.updateLastAngles();
		connection.send(new PlayerMoveC2SPacket.LookAndOnGround(
				playerEntity.getYaw(),
				playerEntity.getPitch(),
				false,
				false
		));
	}

	/** Обновляет несколько блоков в одном чанке. */
	@Override
	public void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		packet.visitUpdates((pos, state) -> world.handleBlockUpdate(pos, state, 19));
	}

	/** Загружает данные чанка, полученные от сервера. */
	@Override
	public void onChunkData(ChunkDataS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		int i = packet.getChunkX();
		int j = packet.getChunkZ();
		loadChunk(i, j, packet.getChunkData());
		LightData lightData = packet.getLightData();
		world.enqueueChunkUpdate(() -> {
			readLightData(i, j, lightData, false);
			WorldChunk worldChunk = world.getChunkManager().getWorldChunk(i, j, false);
			if (worldChunk != null) {
				scheduleRenderChunk(worldChunk, i, j);
				client.worldRenderer.scheduleNeighborUpdates(worldChunk.getPos());
			}
		});
	}

	/** Обновляет данные биомов в чанке. */
	@Override
	public void onChunkBiomeData(ChunkBiomeDataS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		for (ChunkBiomeDataS2CPacket.Serialized serialized : packet.chunkBiomeData()) {
			world.getChunkManager().onChunkBiomeData(serialized.pos().x, serialized.pos().z, serialized.toReadingBuf());
		}

		for (ChunkBiomeDataS2CPacket.Serialized serialized : packet.chunkBiomeData()) {
			world.resetChunkColor(new ChunkPos(serialized.pos().x, serialized.pos().z));
		}

		for (ChunkBiomeDataS2CPacket.Serialized serialized : packet.chunkBiomeData()) {
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					for (int k = world.getBottomSectionCoord(); k <= world.getTopSectionCoord(); k++) {
						client.worldRenderer.scheduleChunkRender(serialized.pos().x + i, k, serialized.pos().z + j);
					}
				}
			}
		}
	}

	private void loadChunk(int x, int z, ChunkData chunkData) {
		world
				.getChunkManager()
				.loadChunkFromPacket(
						x,
						z,
						chunkData.getSectionsDataBuf(),
						chunkData.getHeightmap(),
						chunkData.getBlockEntities(x, z)
				);
	}

	/** Планирует перерисовку чанка после его загрузки. */
	private void scheduleRenderChunk(WorldChunk chunk, int x, int z) {
		LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
		ChunkSection[] chunkSections = chunk.getSectionArray();
		ChunkPos chunkPos = chunk.getPos();

		for (int i = 0; i < chunkSections.length; i++) {
			ChunkSection chunkSection = chunkSections[i];
			int j = world.sectionIndexToCoord(i);
			lightingProvider.setSectionStatus(ChunkSectionPos.from(chunkPos, j), chunkSection.isEmpty());
		}

		world.scheduleChunkRenders(
				x - 1,
				world.getBottomSectionCoord(),
				z - 1,
				x + 1,
				world.getTopSectionCoord(),
				z + 1
		);
	}

	/** Выгружает чанк из клиентского мира. */
	@Override
	public void onUnloadChunk(UnloadChunkS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.getChunkManager().unload(packet.pos());
		debugSubscriptionManager.removeChunk(packet.pos());
		unloadChunk(packet);
	}

	/** Выполняет выгрузку чанка в очереди обновлений мира. */
	private void unloadChunk(UnloadChunkS2CPacket packet) {
		ChunkPos chunkPos = packet.pos();
		world.enqueueChunkUpdate(() -> {
			LightingProvider lightingProvider = world.getLightingProvider();
			lightingProvider.setColumnEnabled(chunkPos, false);

			for (int i = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); i++) {
				ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos, i);
				lightingProvider.enqueueSectionData(LightType.BLOCK, chunkSectionPos, null);
				lightingProvider.enqueueSectionData(LightType.SKY, chunkSectionPos, null);
			}

			for (int i = world.getBottomSectionCoord(); i <= world.getTopSectionCoord(); i++) {
				lightingProvider.setSectionStatus(ChunkSectionPos.from(chunkPos, i), true);
			}
		});
	}

	/** Обновляет состояние одного блока. */
	@Override
	public void onBlockUpdate(BlockUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.handleBlockUpdate(packet.getPos(), packet.getState(), 19);
	}

	/** Переводит клиент в фазу CONFIGURATION. */
	@Override
	public void onEnterReconfiguration(EnterReconfigurationS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.getMessageHandler().processAll();
		sendAcknowledgment();
		ChatHud.ChatState chatState = client.inGameHud.getChatHud().toChatState();
		client.enterReconfiguration(new ReconfiguringScreen(RECONFIGURING_TEXT, connection));
		connection
				.transitionInbound(
						ConfigurationStates.S2C,
						new ClientConfigurationNetworkHandler(
								client,
								connection,
								new ClientConnectionState(
										new ClientChunkLoadProgress(),
										profile,
										worldSession,
										combinedDynamicRegistries,
										enabledFeatures,
										brand,
										serverInfo,
										postDisconnectScreen,
										serverCookies,
										chatState,
										customReportDetails,
										getServerLinks(),
										seenPlayers,
										seenInsecureChatWarning
								)
						)
				);
		sendPacket(AcknowledgeReconfigurationC2SPacket.INSTANCE);
		connection.transitionOutbound(ConfigurationStates.C2S);
	}

	@Override
	public void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		LivingEntity livingEntity = (LivingEntity) world.getEntityById(packet.getCollectorEntityId());
		if (livingEntity == null) {
			livingEntity = client.player;
		}

		if (entity != null) {
			if (entity instanceof ExperienceOrbEntity) {
				world
						.playSoundClient(
								entity.getX(),
								entity.getY(),
								entity.getZ(),
								SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
								SoundCategory.PLAYERS,
								0.1F,
								(random.nextFloat() - random.nextFloat()) * 0.35F + 0.9F,
								false
						);
			}
			else {
				world
						.playSoundClient(
								entity.getX(),
								entity.getY(),
								entity.getZ(),
								SoundEvents.ENTITY_ITEM_PICKUP,
								SoundCategory.PLAYERS,
								0.2F,
								(random.nextFloat() - random.nextFloat()) * 1.4F + 2.0F,
								false
						);
			}

			EntityRenderState
					entityRenderState =
					client.getEntityRenderDispatcher().getAndUpdateRenderState(entity, 1.0F);
			client.particleManager.addParticle(new ItemPickupParticle(
					world,
					entityRenderState,
					livingEntity,
					entity.getVelocity()
			));
			if (entity instanceof ItemEntity itemEntity) {
				ItemStack itemStack = itemEntity.getStack();
				if (!itemStack.isEmpty()) {
					itemStack.decrement(packet.getStackAmount());
				}

				if (itemStack.isEmpty()) {
					world.removeEntity(packet.getEntityId(), Entity.RemovalReason.DISCARDED);
				}
			}
			else if (!(entity instanceof ExperienceOrbEntity)) {
				world.removeEntity(packet.getEntityId(), Entity.RemovalReason.DISCARDED);
			}
		}
	}

	/** Обрабатывает системное игровое сообщение. */
	@Override
	public void onGameMessage(GameMessageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.getMessageHandler().onGameMessage(packet.content(), packet.overlay());
	}

	/** Обрабатывает подписанное сообщение чата от игрока. */
	@Override
	public void onChatMessage(ChatMessageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		int i = globalChatMessageIndex++;
		if (packet.globalIndex() != i) {
			LOGGER.error(
					"Missing or out-of-order chat message from server, expected index {} but got {}",
					i,
					packet.globalIndex()
			);
			connection.disconnect(BAD_CHAT_INDEX_TEXT);
		}
		else {
			Optional<MessageBody> optional = packet.body().toBody(signatureStorage);
			if (optional.isEmpty()) {
				LOGGER.error("Message from player with ID {} referenced unrecognized signature id", packet.sender());
				connection.disconnect(INVALID_PACKET_TEXT);
			}
			else {
				signatureStorage.add(optional.get(), packet.signature());
				UUID uUID = packet.sender();
				PlayerListEntry playerListEntry = getPlayerListEntry(uUID);
				if (playerListEntry == null) {
					LOGGER.error("Received player chat packet for unknown player with ID: {}", uUID);
					client
							.getMessageHandler()
							.onUnverifiedMessage(uUID, packet.signature(), packet.serializedParameters());
				}
				else {
					PublicPlayerSession publicPlayerSession = playerListEntry.getSession();
					MessageLink messageLink;
					if (publicPlayerSession != null) {
						messageLink = new MessageLink(packet.index(), uUID, publicPlayerSession.sessionId());
					}
					else {
						messageLink = MessageLink.of(uUID);
					}

					SignedMessage
							signedMessage =
							new SignedMessage(
									messageLink,
									packet.signature(),
									optional.get(),
									packet.unsignedContent(),
									packet.filterMask()
							);
					signedMessage = playerListEntry.getMessageVerifier().ensureVerified(signedMessage);
					if (signedMessage != null) {
						client
								.getMessageHandler()
								.onChatMessage(
										signedMessage,
										playerListEntry.getProfile(),
										packet.serializedParameters()
								);
					}
					else {
						client
								.getMessageHandler()
								.onUnverifiedMessage(uUID, packet.signature(), packet.serializedParameters());
					}
				}
			}
		}
	}

	/** Обрабатывает системное сообщение чата без профиля. */
	@Override
	public void onProfilelessChatMessage(ProfilelessChatMessageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.getMessageHandler().onProfilelessMessage(packet.message(), packet.chatType());
	}

	/** Удаляет сообщение из чата по подписи. */
	@Override
	public void onRemoveMessage(RemoveMessageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Optional<MessageSignatureData> optional = packet.messageSignature().getSignature(signatureStorage);
		if (optional.isEmpty()) {
			connection.disconnect(INVALID_PACKET_TEXT);
		}
		else {
			lastSeenMessagesCollector.remove(optional.get());
			if (!client.getMessageHandler().removeDelayedMessage(optional.get())) {
				client.inGameHud.getChatHud().removeMessage(optional.get());
			}
		}
	}

	/** Воспроизводит анимацию сущности (удар, получение урона). */
	@Override
	public void onEntityAnimation(EntityAnimationS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		if (entity != null) {
			if (packet.getAnimationId() == 0) {
				LivingEntity livingEntity = (LivingEntity) entity;
				livingEntity.swingHand(Hand.MAIN_HAND);
			}
			else if (packet.getAnimationId() == 3) {
				LivingEntity livingEntity = (LivingEntity) entity;
				livingEntity.swingHand(Hand.OFF_HAND);
			}
			else if (packet.getAnimationId() == 2) {
				PlayerEntity playerEntity = (PlayerEntity) entity;
				playerEntity.wakeUp(false, false);
			}
			else if (packet.getAnimationId() == 4) {
				client.particleManager.addEmitter(entity, ParticleTypes.CRIT);
			}
			else if (packet.getAnimationId() == 5) {
				client.particleManager.addEmitter(entity, ParticleTypes.ENCHANTED_HIT);
			}
		}
	}

	/** Применяет наклон камеры при получении урона. */
	@Override
	public void onDamageTilt(DamageTiltS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.id());
		if (entity != null) {
			entity.animateDamage(packet.yaw());
		}
	}

	/** Обновляет время суток в мире. */
	@Override
	public void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.setTime(packet.time(), packet.timeOfDay(), packet.tickDayTime());
		worldSession.setTick(packet.time());
	}

	/** Устанавливает точку спавна игрока в мире. */
	@Override
	public void onPlayerSpawnPosition(PlayerSpawnPositionS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.world.setSpawnPoint(packet.respawnData());
	}

	/** Устанавливает список пассажиров сущности. */
	@Override
	public void onEntityPassengersSet(EntityPassengersSetS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		if (entity == null) {
			LOGGER.warn("Received passengers for unknown entity");
		}
		else {
			boolean bl = entity.hasPassengerDeep(client.player);
			entity.removeAllPassengers();

			for (int i : packet.getPassengerIds()) {
				Entity entity2 = world.getEntityById(i);
				if (entity2 != null) {
					entity2.startRiding(entity, true, false);
					if (entity2 == client.player) {
						removedPlayerVehicleId = OptionalInt.empty();
						if (!bl) {
							if (entity instanceof AbstractBoatEntity) {
								client.player.lastYaw = entity.getYaw();
								client.player.setYaw(entity.getYaw());
								client.player.setHeadYaw(entity.getYaw());
							}

							Text
									text =
									Text.translatable(
											"mount.onboard",
											client.options.sneakKey.getBoundKeyLocalizedText()
									);
							client.inGameHud.setOverlayMessage(text, false);
							client.getNarratorManager().narrateSystemImmediately(text);
						}
					}
				}
			}
		}
	}

	/** Привязывает или отвязывает сущность (поводок, транспорт). */
	@Override
	public void onEntityAttach(EntityAttachS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (world.getEntityById(packet.getAttachedEntityId()) instanceof Leashable leashable) {
			leashable.setUnresolvedLeashHolderId(packet.getHoldingEntityId());
		}
	}

	/** Возвращает предмет защиты от смерти, если он активен. */
	private static ItemStack getActiveDeathProtector(PlayerEntity player) {
		for (Hand hand : Hand.values()) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (itemStack.contains(DataComponentTypes.DEATH_PROTECTION)) {
				return itemStack;
			}
		}

		return new ItemStack(Items.TOTEM_OF_UNDYING);
	}

	/** Обрабатывает статусное событие сущности (анимация, эффект). */
	@Override
	public void onEntityStatus(EntityStatusS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = packet.getEntity(world);
		if (entity != null) {
			switch (packet.getStatus()) {
				case 21:
					client.getSoundManager().play(new GuardianAttackSoundInstance((GuardianEntity) entity));
					break;
				case 35:
					int i = 40;
					client.particleManager.addEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
					world
							.playSoundClient(
									entity.getX(),
									entity.getY(),
									entity.getZ(),
									SoundEvents.ITEM_TOTEM_USE,
									entity.getSoundCategory(),
									1.0F,
									1.0F,
									false
							);
					if (entity == client.player) {
						client.gameRenderer.showFloatingItem(getActiveDeathProtector(client.player));
					}
					break;
				case 63:
					client.getSoundManager().play(new SnifferDigSoundInstance((SnifferEntity) entity));
					break;
				default:
					entity.handleStatus(packet.getStatus());
			}
		}
	}

	/** Обрабатывает событие получения урона сущностью. */
	@Override
	public void onEntityDamage(EntityDamageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.entityId());
		if (entity != null) {
			entity.onDamaged(packet.createDamageSource(world));
		}
	}

	/** Обновляет здоровье, голод и насыщение игрока. */
	@Override
	public void onHealthUpdate(HealthUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.player.updateHealth(packet.getHealth());
		client.player.getHungerManager().setFoodLevel(packet.getFood());
		client.player.getHungerManager().setSaturationLevel(packet.getSaturation());
	}

	/** Обновляет шкалу опыта игрока. */
	@Override
	public void onExperienceBarUpdate(ExperienceBarUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.player.setExperience(packet.getBarProgress(), packet.getExperienceLevel(), packet.getExperience());
	}

	/** Обрабатывает возрождение игрока (смерть или смена измерения). */
	@Override
	public void onPlayerRespawn(PlayerRespawnS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		CommonPlayerSpawnInfo commonPlayerSpawnInfo = packet.commonPlayerSpawnInfo();
		RegistryKey<World> registryKey = commonPlayerSpawnInfo.dimension();
		RegistryEntry<DimensionType> registryEntry = commonPlayerSpawnInfo.dimensionType();
		ClientPlayerEntity clientPlayerEntity = client.player;
		RegistryKey<World> registryKey2 = clientPlayerEntity.getEntityWorld().getRegistryKey();
		boolean bl = registryKey != registryKey2;
		LevelLoadingScreen.WorldEntryReason
				worldEntryReason =
				getWorldEntryReason(clientPlayerEntity.isDead(), registryKey, registryKey2);
		if (bl) {
			Map<MapIdComponent, MapState> map = world.getMapStates();
			boolean bl2 = commonPlayerSpawnInfo.isDebug();
			boolean bl3 = commonPlayerSpawnInfo.isFlat();
			int i = commonPlayerSpawnInfo.seaLevel();
			ClientWorld.Properties
					properties =
					new ClientWorld.Properties(worldProperties.getDifficulty(), worldProperties.isHardcore(), bl3);
			worldProperties = properties;
			world = new ClientWorld(
					this,
					properties,
					registryKey,
					registryEntry,
					chunkLoadDistance,
					simulationDistance,
					client.worldRenderer,
					bl2,
					commonPlayerSpawnInfo.seed(),
					i
			);
			world.putMapStates(map);
			client.joinWorld(world);
			debugSubscriptionManager.clearValues();
		}

		client.setCameraEntity(null);
		if (clientPlayerEntity.shouldCloseHandledScreenOnRespawn()) {
			clientPlayerEntity.closeHandledScreen();
		}

		ClientPlayerEntity clientPlayerEntity2;
		if (packet.hasFlag((byte) 2)) {
			clientPlayerEntity2 = client
					.interactionManager
					.createPlayer(
							world,
							clientPlayerEntity.getStatHandler(),
							clientPlayerEntity.getRecipeBook(),
							clientPlayerEntity.getLastPlayerInput(),
							clientPlayerEntity.isSprinting()
					);
		}
		else {
			clientPlayerEntity2 =
					client.interactionManager.createPlayer(
							world,
							clientPlayerEntity.getStatHandler(),
							clientPlayerEntity.getRecipeBook()
					);
		}

		setPlayerLoadedState(false);
		startWorldLoading(clientPlayerEntity2, world, worldEntryReason);
		clientPlayerEntity2.setId(clientPlayerEntity.getId());
		client.player = clientPlayerEntity2;
		if (bl) {
			client.getMusicTracker().stop();
		}

		client.setCameraEntity(clientPlayerEntity2);
		if (packet.hasFlag((byte) 2)) {
			List<DataTracker.SerializedEntry<?>> list = clientPlayerEntity.getDataTracker().getChangedEntries();
			if (list != null) {
				clientPlayerEntity2.getDataTracker().writeUpdatedEntries(list);
			}

			clientPlayerEntity2.setVelocity(clientPlayerEntity.getVelocity());
			clientPlayerEntity2.setYaw(clientPlayerEntity.getYaw());
			clientPlayerEntity2.setPitch(clientPlayerEntity.getPitch());
		}
		else {
			clientPlayerEntity2.init();
			clientPlayerEntity2.setYaw(-180.0F);
		}

		if (packet.hasFlag((byte) 1)) {
			clientPlayerEntity2.getAttributes().setFrom(clientPlayerEntity.getAttributes());
		}
		else {
			clientPlayerEntity2.getAttributes().setBaseFrom(clientPlayerEntity.getAttributes());
		}

		world.addEntity(clientPlayerEntity2);
		clientPlayerEntity2.input = new KeyboardInput(client.options);
		client.interactionManager.copyAbilities(clientPlayerEntity2);
		clientPlayerEntity2.setReducedDebugInfo(clientPlayerEntity.hasReducedDebugInfo());
		clientPlayerEntity2.setShowsDeathScreen(clientPlayerEntity.showsDeathScreen());
		clientPlayerEntity2.setLastDeathPos(commonPlayerSpawnInfo.lastDeathLocation());
		clientPlayerEntity2.setPortalCooldown(commonPlayerSpawnInfo.portalCooldown());
		clientPlayerEntity2.nauseaIntensity = clientPlayerEntity.nauseaIntensity;
		clientPlayerEntity2.lastNauseaIntensity = clientPlayerEntity.lastNauseaIntensity;
		if (client.currentScreen instanceof DeathScreen
				|| client.currentScreen instanceof DeathScreen.TitleScreenConfirmScreen) {
			client.setScreen(null);
		}

		client.interactionManager.setGameModes(commonPlayerSpawnInfo.gameMode(), commonPlayerSpawnInfo.lastGameMode());
	}

	private LevelLoadingScreen.WorldEntryReason getWorldEntryReason(
			boolean dead,
			RegistryKey<World> from,
			RegistryKey<World> to
	) {
		LevelLoadingScreen.WorldEntryReason worldEntryReason = LevelLoadingScreen.WorldEntryReason.OTHER;
		if (!dead) {
			if (from == World.NETHER || to == World.NETHER) {
				worldEntryReason = LevelLoadingScreen.WorldEntryReason.NETHER_PORTAL;
			}
			else if (from == World.END || to == World.END) {
				worldEntryReason = LevelLoadingScreen.WorldEntryReason.END_PORTAL;
			}
		}

		return worldEntryReason;
	}

	/** Обрабатывает взрыв: применяет урон блокам и отталкивает игрока. */
	@Override
	public void onExplosion(ExplosionS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Vec3d vec3d = packet.center();
		client
				.world
				.playSoundClient(
						vec3d.getX(),
						vec3d.getY(),
						vec3d.getZ(),
						packet.explosionSound().value(),
						SoundCategory.BLOCKS,
						4.0F,
						(1.0F + (client.world.random.nextFloat() - client.world.random.nextFloat()) * 0.2F) * 0.7F,
						false
				);
		client.world.addParticleClient(
				packet.explosionParticle(),
				vec3d.getX(),
				vec3d.getY(),
				vec3d.getZ(),
				1.0,
				0.0,
				0.0
		);
		client.world.addBlockParticleEffects(vec3d, packet.radius(), packet.blockCount(), packet.blockParticles());
		packet.playerKnockback().ifPresent(client.player::addVelocityInternal);
	}

	/** Открывает экран управления верховым животным. */
	@Override
	public void onOpenMountScreen(OpenMountScreenS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getMountId());
		ClientPlayerEntity clientPlayerEntity = client.player;
		int i = packet.getSlotColumnCount();
		SimpleInventory simpleInventory = new SimpleInventory(MountScreenHandler.getSlotCount(i));
		if (entity instanceof AbstractHorseEntity abstractHorseEntity) {
			HorseScreenHandler horseScreenHandler = new HorseScreenHandler(
					packet.getSyncId(), clientPlayerEntity.getInventory(), simpleInventory, abstractHorseEntity, i
			);
			clientPlayerEntity.currentScreenHandler = horseScreenHandler;
			client.setScreen(new HorseScreen(
					horseScreenHandler,
					clientPlayerEntity.getInventory(),
					abstractHorseEntity,
					i
			));
		}
		else if (entity instanceof AbstractNautilusEntity abstractNautilusEntity) {
			NautilusScreenHandler nautilusScreenHandler = new NautilusScreenHandler(
					packet.getSyncId(), clientPlayerEntity.getInventory(), simpleInventory, abstractNautilusEntity, i
			);
			clientPlayerEntity.currentScreenHandler = nautilusScreenHandler;
			client.setScreen(new NautilusScreen(
					nautilusScreenHandler,
					clientPlayerEntity.getInventory(),
					abstractNautilusEntity,
					i
			));
		}
	}

	/** Открывает экран инвентаря или другого интерфейса. */
	@Override
	public void onOpenScreen(OpenScreenS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		HandledScreens.open(packet.getScreenHandlerType(), client, packet.getSyncId(), packet.getName());
	}

	/** Обновляет один слот в открытом экране. */
	@Override
	public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		ItemStack itemStack = packet.getStack();
		int i = packet.getSlot();
		client.getTutorialManager().onSlotUpdate(itemStack);
		boolean bl;
		if (client.currentScreen instanceof CreativeInventoryScreen creativeInventoryScreen) {
			bl = !creativeInventoryScreen.isInventoryTabSelected();
		}
		else {
			bl = false;
		}

		if (packet.getSyncId() == 0) {
			if (PlayerScreenHandler.isInHotbar(i) && !itemStack.isEmpty()) {
				ItemStack itemStack2 = playerEntity.playerScreenHandler.getSlot(i).getStack();
				if (itemStack2.isEmpty() || itemStack2.getCount() < itemStack.getCount()) {
					itemStack.setBobbingAnimationTime(5);
				}
			}

			playerEntity.playerScreenHandler.setStackInSlot(i, packet.getRevision(), itemStack);
		}
		else if (packet.getSyncId() == playerEntity.currentScreenHandler.syncId && (packet.getSyncId() != 0 || !bl)) {
			playerEntity.currentScreenHandler.setStackInSlot(i, packet.getRevision(), itemStack);
		}

		if (client.currentScreen instanceof CreativeInventoryScreen) {
			playerEntity.playerScreenHandler.setReceivedStack(i, itemStack);
			playerEntity.playerScreenHandler.sendContentUpdates();
		}
	}

	/** Устанавливает предмет на курсоре мыши в инвентаре. */
	@Override
	public void onSetCursorItem(SetCursorItemS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.getTutorialManager().onSlotUpdate(packet.contents());
		if (!(client.currentScreen instanceof CreativeInventoryScreen)) {
			client.player.currentScreenHandler.setCursorStack(packet.contents());
		}
	}

	/** Устанавливает предмет в конкретный слот инвентаря игрока. */
	@Override
	public void onSetPlayerInventory(SetPlayerInventoryS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.getTutorialManager().onSlotUpdate(packet.contents());
		client.player.getInventory().setStack(packet.slot(), packet.contents());
	}

	/** Синхронизирует содержимое инвентаря с сервером. */
	@Override
	public void onInventory(InventoryS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		if (packet.syncId() == 0) {
			playerEntity.playerScreenHandler.updateSlotStacks(
					packet.revision(),
					packet.contents(),
					packet.cursorStack()
			);
		}
		else if (packet.syncId() == playerEntity.currentScreenHandler.syncId) {
			playerEntity.currentScreenHandler.updateSlotStacks(
					packet.revision(),
					packet.contents(),
					packet.cursorStack()
			);
		}
	}

	/** Открывает экран редактирования таблички. */
	@Override
	public void onSignEditorOpen(SignEditorOpenS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		BlockPos blockPos = packet.getPos();
		if (world.getBlockEntity(blockPos) instanceof SignBlockEntity signBlockEntity) {
			client.player.openEditSignScreen(signBlockEntity, packet.isFront());
		}
		else {
			LOGGER.warn(
					"Ignoring openTextEdit on an invalid entity: {} at pos {}",
					world.getBlockEntity(blockPos),
					blockPos
			);
		}
	}

	/** Обновляет данные блок-сущности (сундук, печь и т.д.). */
	@Override
	public void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		BlockPos blockPos = packet.getPos();
		client.world.getBlockEntity(blockPos, packet.getBlockEntityType()).ifPresent(blockEntity -> {
			ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER);

			try {
				blockEntity.read(NbtReadView.create(logging, combinedDynamicRegistries, packet.getNbt()));
			}
			catch (Throwable var7) {
				try {
					logging.close();
				}
				catch (Throwable var6) {
					var7.addSuppressed(var6);
				}

				throw var7;
			}

			logging.close();
			if (blockEntity instanceof CommandBlockBlockEntity && client.currentScreen instanceof CommandBlockScreen) {
				((CommandBlockScreen) client.currentScreen).updateCommandBlock();
			}
		});
	}

	/** Обновляет числовое свойство открытого экрана (прогресс плавки и т.д.). */
	@Override
	public void onScreenHandlerPropertyUpdate(ScreenHandlerPropertyUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		if (playerEntity.currentScreenHandler.syncId == packet.getSyncId()) {
			playerEntity.currentScreenHandler.setProperty(packet.getPropertyId(), packet.getValue());
		}
	}

	/** Обновляет экипировку сущности. */
	@Override
	public void onEntityEquipmentUpdate(EntityEquipmentUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (world.getEntityById(packet.getEntityId()) instanceof LivingEntity livingEntity) {
			packet
					.getEquipmentList()
					.forEach(pair -> livingEntity.equipStack(
							(EquipmentSlot) pair.getFirst(),
							(ItemStack) pair.getSecond()
					));
		}
	}

	/** Закрывает текущий открытый экран. */
	@Override
	public void onCloseScreen(CloseScreenS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.player.closeScreen();
	}

	/** Обрабатывает событие блока (анимация сундука, поршня и т.д.). */
	@Override
	public void onBlockEvent(BlockEventS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.world.addSyncedBlockEvent(packet.getPos(), packet.getBlock(), packet.getType(), packet.getData());
	}

	/** Обновляет прогресс разрушения блока. */
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.world.setBlockBreakingInfo(packet.getEntityId(), packet.getPos(), packet.getProgress());
	}

	/** Обрабатывает изменение состояния игры (погода, режим игры, конец игры). */
	@Override
	public void onGameStateChange(GameStateChangeS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		GameStateChangeS2CPacket.Reason reason = packet.getReason();
		float f = packet.getValue();
		int i = MathHelper.floor(f + 0.5F);
		if (reason == GameStateChangeS2CPacket.NO_RESPAWN_BLOCK) {
			playerEntity.sendMessage(Text.translatable("block.minecraft.spawn.not_valid"), false);
		}
		else if (reason == GameStateChangeS2CPacket.RAIN_STARTED) {
			world.getLevelProperties().setRaining(true);
			world.setRainGradient(0.0F);
		}
		else if (reason == GameStateChangeS2CPacket.RAIN_STOPPED) {
			world.getLevelProperties().setRaining(false);
			world.setRainGradient(1.0F);
		}
		else if (reason == GameStateChangeS2CPacket.GAME_MODE_CHANGED) {
			client.interactionManager.setGameMode(GameMode.byIndex(i));
		}
		else if (reason == GameStateChangeS2CPacket.GAME_WON) {
			client.setScreen(new CreditsScreen(
					true, () -> {
				client.player.networkHandler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
				client.setScreen(null);
			}
			));
		}
		else if (reason == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN) {
			GameOptions gameOptions = client.options;
			Text text = null;
			if (f == 0.0F) {
				client.setScreen(new DemoScreen());
			}
			else if (f == 101.0F) {
				text = Text.translatable(
						"demo.help.movement",
						gameOptions.forwardKey.getBoundKeyLocalizedText(),
						gameOptions.leftKey.getBoundKeyLocalizedText(),
						gameOptions.backKey.getBoundKeyLocalizedText(),
						gameOptions.rightKey.getBoundKeyLocalizedText()
				);
			}
			else if (f == 102.0F) {
				text = Text.translatable("demo.help.jump", gameOptions.jumpKey.getBoundKeyLocalizedText());
			}
			else if (f == 103.0F) {
				text = Text.translatable("demo.help.inventory", gameOptions.inventoryKey.getBoundKeyLocalizedText());
			}
			else if (f == 104.0F) {
				text = Text.translatable("demo.day.6", gameOptions.screenshotKey.getBoundKeyLocalizedText());
			}

			if (text != null) {
				client.inGameHud.getChatHud().addMessage(text);
				client.getNarratorManager().narrateSystemMessage(text);
			}
		}
		else if (reason == GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER) {
			world
					.playSound(
							playerEntity,
							playerEntity.getX(),
							playerEntity.getEyeY(),
							playerEntity.getZ(),
							SoundEvents.ENTITY_ARROW_HIT_PLAYER,
							SoundCategory.PLAYERS,
							0.18F,
							0.45F
					);
		}
		else if (reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED) {
			world.setRainGradient(f);
		}
		else if (reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) {
			world.setThunderGradient(f);
		}
		else if (reason == GameStateChangeS2CPacket.PUFFERFISH_STING) {
			world
					.playSound(
							playerEntity,
							playerEntity.getX(),
							playerEntity.getY(),
							playerEntity.getZ(),
							SoundEvents.ENTITY_PUFFER_FISH_STING,
							SoundCategory.NEUTRAL,
							1.0F,
							1.0F
					);
		}
		else if (reason == GameStateChangeS2CPacket.ELDER_GUARDIAN_EFFECT) {
			world.addParticleClient(
					ParticleTypes.ELDER_GUARDIAN,
					playerEntity.getX(),
					playerEntity.getY(),
					playerEntity.getZ(),
					0.0,
					0.0,
					0.0
			);
			if (i == 1) {
				world
						.playSound(
								playerEntity,
								playerEntity.getX(),
								playerEntity.getY(),
								playerEntity.getZ(),
								SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
								SoundCategory.HOSTILE,
								1.0F,
								1.0F
						);
			}
		}
		else if (reason == GameStateChangeS2CPacket.IMMEDIATE_RESPAWN) {
			client.player.setShowsDeathScreen(f == 0.0F);
		}
		else if (reason == GameStateChangeS2CPacket.LIMITED_CRAFTING_TOGGLED) {
			client.player.setLimitedCraftingEnabled(f == 1.0F);
		}
		else if (reason == GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING && chunkLoadProgress != null) {
			chunkLoadProgress.initialChunksComing();
		}
	}

	/** Запускает загрузку мира и показывает экран загрузки. */
	private void startWorldLoading(
			ClientPlayerEntity player,
			ClientWorld world,
			LevelLoadingScreen.WorldEntryReason reason
	) {
		LOGGER.debug(
				"[WorldLoading] startWorldLoading() — причина: {}, мир: {}",
				reason,
				world.getRegistryKey().getValue()
		);

		if (chunkLoadProgress == null) {
			chunkLoadProgress = new ClientChunkLoadProgress();
		}

		chunkLoadProgress.startWorldLoading(player, world, client.worldRenderer);

		if (client.currentScreen instanceof LevelLoadingScreen levelLoadingScreen) {
			LOGGER.debug("[WorldLoading] LevelLoadingScreen уже активен — переинициализируем");
			levelLoadingScreen.init(chunkLoadProgress, reason);
		}
		else {
			LOGGER.debug("[WorldLoading] Открываем новый LevelLoadingScreen");
			client.inGameHud.getChatHud().setScreen();
			client.setScreenAndRender(new LevelLoadingScreen(chunkLoadProgress, reason));
		}
	}

	/** Обновляет данные карты. */
	@Override
	public void onMapUpdate(MapUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		MapIdComponent mapIdComponent = packet.mapId();
		MapState mapState = client.world.getMapState(mapIdComponent);
		if (mapState == null) {
			mapState = MapState.of(packet.scale(), packet.locked(), client.world.getRegistryKey());
			client.world.putClientsideMapState(mapIdComponent, mapState);
		}

		packet.apply(mapState);
		client.getMapTextureManager().setNeedsUpdate(mapIdComponent, mapState);
	}

	/** Воспроизводит звук или эффект мирового события (взрыв двери и т.д.). */
	@Override
	public void onWorldEvent(WorldEventS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (packet.isGlobal()) {
			client.world.syncGlobalEvent(packet.getEventId(), packet.getPos(), packet.getData());
		}
		else {
			client.world.syncWorldEvent(packet.getEventId(), packet.getPos(), packet.getData());
		}
	}

	@Override
	public void onAdvancements(AdvancementUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		advancementHandler.onAdvancements(packet);
	}

	/** Выбирает вкладку достижений. */
	@Override
	public void onSelectAdvancementTab(SelectAdvancementTabS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Identifier identifier = packet.getTabId();
		if (identifier == null) {
			advancementHandler.selectTab(null, false);
		}
		else {
			AdvancementEntry advancementEntry = advancementHandler.get(identifier);
			advancementHandler.selectTab(advancementEntry, false);
		}
	}

	/** Обновляет дерево команд от сервера. */
	@Override
	public void onCommandTree(CommandTreeS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		commandDispatcher = new CommandDispatcher(
				packet.getCommandTree(
						CommandRegistryAccess.of(combinedDynamicRegistries, enabledFeatures),
						COMMAND_NODE_FACTORY
				)
		);
	}

	/** Останавливает воспроизведение звука. */
	@Override
	public void onStopSound(StopSoundS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.getSoundManager().stopSounds(packet.getSoundId(), packet.getCategory());
	}

	/** Обрабатывает подсказки автодополнения команд. */
	@Override
	public void onCommandSuggestions(CommandSuggestionsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		commandSource.onCommandSuggestions(packet.id(), packet.getSuggestions());
	}

	/** Синхронизирует рецепты крафта с сервером. */
	@Override
	public void onSynchronizeRecipes(SynchronizeRecipesS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		recipeManager = new ClientRecipeManager(packet.itemSets(), packet.stonecutterRecipes());
	}

	/** Заставляет игрока смотреть на указанную точку или сущность. */
	@Override
	public void onLookAt(LookAtS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Vec3d vec3d = packet.getTargetPosition(world);
		if (vec3d != null) {
			client.player.lookAt(packet.getSelfAnchor(), vec3d);
		}
	}

	/** Обрабатывает ответ на NBT-запрос к блоку или сущности. */
	@Override
	public void onNbtQueryResponse(NbtQueryResponseS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (!dataQueryHandler.handleQueryResponse(packet.getTransactionId(), packet.getNbt())) {
			LOGGER.debug("Got unhandled response to tag query {}", packet.getTransactionId());
		}
	}

	/** Обновляет статистику игрока. */
	@Override
	public void onStatistics(StatisticsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ObjectIterator statsScreen = packet.stats().object2IntEntrySet().iterator();

		while (statsScreen.hasNext()) {
			Entry<Stat<?>> entry = (Entry<Stat<?>>) statsScreen.next();
			Stat<?> stat = (Stat<?>) entry.getKey();
			int i = entry.getIntValue();
			client.player.getStatHandler().setStat(client.player, stat, i);
		}

		if (client.currentScreen instanceof StatsScreen statsScreenx) {
			statsScreenx.onStatsReady();
		}
	}

	/** Добавляет рецепты в книгу рецептов. */
	@Override
	public void onRecipeBookAdd(RecipeBookAddS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ClientRecipeBook clientRecipeBook = client.player.getRecipeBook();
		if (packet.replace()) {
			clientRecipeBook.clear();
		}

		for (RecipeBookAddS2CPacket.Entry entry : packet.entries()) {
			clientRecipeBook.add(entry.contents());
			if (entry.isHighlighted()) {
				clientRecipeBook.markHighlighted(entry.contents().id());
			}

			if (entry.shouldShowNotification()) {
				RecipeToast.show(client.getToastManager(), entry.contents().display());
			}
		}

		refreshRecipeBook(clientRecipeBook);
	}

	/** Удаляет рецепты из книги рецептов. */
	@Override
	public void onRecipeBookRemove(RecipeBookRemoveS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ClientRecipeBook clientRecipeBook = client.player.getRecipeBook();

		for (NetworkRecipeId networkRecipeId : packet.recipes()) {
			clientRecipeBook.remove(networkRecipeId);
		}

		refreshRecipeBook(clientRecipeBook);
	}

	/** Обновляет настройки книги рецептов. */
	@Override
	public void onRecipeBookSettings(RecipeBookSettingsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ClientRecipeBook clientRecipeBook = client.player.getRecipeBook();
		clientRecipeBook.setOptions(packet.bookSettings());
		refreshRecipeBook(clientRecipeBook);
	}

	/** Обновляет поисковый индекс книги рецептов. */
	private void refreshRecipeBook(ClientRecipeBook recipeBook) {
		recipeBook.refresh();
		searchManager.addRecipeOutputReloader(recipeBook, world);
		if (client.currentScreen instanceof RecipeBookProvider recipeBookProvider) {
			recipeBookProvider.refreshRecipeBook();
		}
	}

	/** Добавляет эффект зелья к сущности. */
	@Override
	public void onEntityStatusEffect(EntityStatusEffectS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		if (entity instanceof LivingEntity) {
			RegistryEntry<StatusEffect> registryEntry = packet.getEffectId();
			StatusEffectInstance statusEffectInstance = new StatusEffectInstance(
					registryEntry,
					packet.getDuration(),
					packet.getAmplifier(),
					packet.isAmbient(),
					packet.shouldShowParticles(),
					packet.shouldShowIcon(),
					null
			);
			if (!packet.keepFading()) {
				statusEffectInstance.skipFading();
			}

			((LivingEntity) entity).setStatusEffect(statusEffectInstance, null);
		}
	}

	private <T> Registry.PendingTagLoad<T> startTagReload(
			RegistryKey<? extends Registry<? extends T>> registryRef,
			TagPacketSerializer.Serialized serialized
	) {
		Registry<T> registry = combinedDynamicRegistries.getOrThrow(registryRef);
		return registry.startTagReload(serialized.toRegistryTags(registry));
	}

	/** Синхронизирует теги реестров с сервером. */
	@Override
	public void onSynchronizeTags(SynchronizeTagsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		List<Registry.PendingTagLoad<?>> list = new ArrayList<>(packet.getGroups().size());
		boolean bl = connection.isLocal();
		packet.getGroups().forEach((registryRef, serialized) -> {
			if (!bl || SerializableRegistries.isSynced((RegistryKey<? extends Registry<?>>) registryRef)) {
				list.add(startTagReload((RegistryKey<? extends Registry<?>>) registryRef, serialized));
			}
		});
		list.forEach(Registry.PendingTagLoad::apply);
		fuelRegistry = FuelRegistry.createDefault(combinedDynamicRegistries, enabledFeatures);
		List<ItemStack> list2 = List.copyOf(ItemGroups.getSearchGroup().getDisplayStacks());
		searchManager.addItemTagReloader(list2);
	}

	/** Уведомляет о завершении боя. */
	@Override
	public void onEndCombat(EndCombatS2CPacket packet) {
	}

	/** Уведомляет о начале боя. */
	@Override
	public void onEnterCombat(EnterCombatS2CPacket packet) {
	}

	/** Обрабатывает смерть игрока: показывает экран смерти или возрождает. */
	@Override
	public void onDeathMessage(DeathMessageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.playerId());
		if (entity == client.player) {
			if (client.player.showsDeathScreen()) {
				client.setScreen(new DeathScreen(
						packet.message(),
						world.getLevelProperties().isHardcore(),
						client.player
				));
			}
			else {
				client.player.requestRespawn();
			}
		}
	}

	/** Обновляет сложность игры. */
	@Override
	public void onDifficulty(DifficultyS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		worldProperties.setDifficulty(packet.difficulty());
		worldProperties.setDifficultyLocked(packet.difficultyLocked());
	}

	/** Устанавливает сущность, от лица которой ведётся наблюдение. */
	@Override
	public void onSetCameraEntity(SetCameraEntityS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = packet.getEntity(world);
		if (entity != null) {
			client.setCameraEntity(entity);
		}
	}

	/** Инициализирует границу мира. */
	@Override
	public void onWorldBorderInitialize(WorldBorderInitializeS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		WorldBorder worldBorder = world.getWorldBorder();
		worldBorder.setCenter(packet.getCenterX(), packet.getCenterZ());
		long l = packet.getSizeLerpTime();
		if (l > 0L) {
			worldBorder.interpolateSize(packet.getSize(), packet.getSizeLerpTarget(), l, world.getTime());
		}
		else {
			worldBorder.setSize(packet.getSizeLerpTarget());
		}

		worldBorder.setMaxRadius(packet.getMaxRadius());
		worldBorder.setWarningBlocks(packet.getWarningBlocks());
		worldBorder.setWarningTime(packet.getWarningTime());
	}

	/** Перемещает центр границы мира. */
	@Override
	public void onWorldBorderCenterChanged(WorldBorderCenterChangedS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.getWorldBorder().setCenter(packet.getCenterX(), packet.getCenterZ());
	}

	/** Интерполирует изменение размера границы мира. */
	@Override
	public void onWorldBorderInterpolateSize(WorldBorderInterpolateSizeS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world
				.getWorldBorder()
				.interpolateSize(
						packet.getSize(),
						packet.getSizeLerpTarget(),
						packet.getSizeLerpTime(),
						world.getTime()
				);
	}

	/** Устанавливает новый размер границы мира. */
	@Override
	public void onWorldBorderSizeChanged(WorldBorderSizeChangedS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.getWorldBorder().setSize(packet.getSizeLerpTarget());
	}

	/** Обновляет предупреждение о расстоянии до границы (в блоках). */
	@Override
	public void onWorldBorderWarningBlocksChanged(WorldBorderWarningBlocksChangedS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.getWorldBorder().setWarningBlocks(packet.getWarningBlocks());
	}

	/** Обновляет предупреждение о времени до границы. */
	@Override
	public void onWorldBorderWarningTimeChanged(WorldBorderWarningTimeChangedS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.getWorldBorder().setWarningTime(packet.getWarningTime());
	}

	@Override
	public void onTitleClear(ClearTitleS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.clearTitle();
		if (packet.shouldReset()) {
			client.inGameHud.setDefaultTitleFade();
		}
	}

	/** Обрабатывает метаданные сервера (MOTD, иконка). */
	@Override
	public void onServerMetadata(ServerMetadataS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (serverInfo != null) {
			serverInfo.label = packet.description();
			packet.favicon().map(ServerInfo::validateFavicon).ifPresent(serverInfo::setFavicon);
			ServerList.updateServerListEntry(serverInfo);
		}
	}

	/** Обновляет подсказки чата. */
	@Override
	public void onChatSuggestions(ChatSuggestionsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		commandSource.onChatSuggestions(packet.action(), packet.entries());
	}

	/** Отображает сообщение в области действия (над хотбаром). */
	@Override
	public void onOverlayMessage(OverlayMessageS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.setOverlayMessage(packet.text(), false);
	}

	/** Отображает заголовок на экране. */
	@Override
	public void onTitle(TitleS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.setTitle(packet.text());
	}

	/** Отображает подзаголовок на экране. */
	@Override
	public void onSubtitle(SubtitleS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.setSubtitle(packet.text());
	}

	/** Устанавливает параметры появления/исчезновения заголовка. */
	@Override
	public void onTitleFade(TitleFadeS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.setTitleTicks(packet.getFadeInTicks(), packet.getStayTicks(), packet.getFadeOutTicks());
	}

	/** Обновляет заголовок и подвал списка игроков. */
	@Override
	public void onPlayerListHeader(PlayerListHeaderS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.getPlayerListHud().setHeader(packet.header().getString().isEmpty() ? null : packet.header());
		client.inGameHud.getPlayerListHud().setFooter(packet.footer().getString().isEmpty() ? null : packet.footer());
	}

	/** Удаляет эффект зелья с сущности. */
	@Override
	public void onRemoveEntityStatusEffect(RemoveEntityStatusEffectS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (packet.getEntity(world) instanceof LivingEntity livingEntity) {
			livingEntity.removeStatusEffectInternal(packet.effect());
		}
	}

	/** Удаляет игроков из списка по UUID. */
	@Override
	public void onPlayerRemove(PlayerRemoveS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		for (UUID uUID : packet.profileIds()) {
			client.getSocialInteractionsManager().setPlayerOffline(uUID);
			PlayerListEntry playerListEntry = playerListEntries.remove(uUID);
			if (playerListEntry != null) {
				listedPlayerListEntries.remove(playerListEntry);
			}
		}
	}

	/** Обновляет список игроков (добавление, удаление, обновление). */
	@Override
	public void onPlayerList(PlayerListS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
			PlayerListEntry
					playerListEntry =
					new PlayerListEntry(Objects.requireNonNull(entry.profile()), isSecureChatEnforced());
			if (playerListEntries.putIfAbsent(entry.profileId(), playerListEntry) == null) {
				client.getSocialInteractionsManager().setPlayerOnline(playerListEntry);
			}
		}

		for (PlayerListS2CPacket.Entry entryx : packet.getEntries()) {
			PlayerListEntry playerListEntry = playerListEntries.get(entryx.profileId());
			if (playerListEntry == null) {
				LOGGER.warn(
						"Ignoring player info update for unknown player {} ({})",
						entryx.profileId(),
						packet.getActions()
				);
			}
			else {
				for (PlayerListS2CPacket.Action action : packet.getActions()) {
					handlePlayerListAction(action, entryx, playerListEntry);
				}
			}
		}
	}

	/** Применяет одно действие к записи в списке игроков. */
	private void handlePlayerListAction(
			PlayerListS2CPacket.Action action,
			PlayerListS2CPacket.Entry receivedEntry,
			PlayerListEntry currentEntry
	) {
		switch (action) {
			case INITIALIZE_CHAT:
				setPublicSession(receivedEntry, currentEntry);
				break;
			case UPDATE_GAME_MODE:
				if (currentEntry.getGameMode() != receivedEntry.gameMode()
						&& client.player != null
						&& client.player.getUuid().equals(receivedEntry.profileId())) {
					client.player.onGameModeChanged(receivedEntry.gameMode());
				}

				currentEntry.setGameMode(receivedEntry.gameMode());
				break;
			case UPDATE_LISTED:
				if (receivedEntry.listed()) {
					listedPlayerListEntries.add(currentEntry);
				}
				else {
					listedPlayerListEntries.remove(currentEntry);
				}
				break;
			case UPDATE_LATENCY:
				currentEntry.setLatency(receivedEntry.latency());
				break;
			case UPDATE_DISPLAY_NAME:
				currentEntry.setDisplayName(receivedEntry.displayName());
				break;
			case UPDATE_HAT:
				currentEntry.setShowHat(receivedEntry.showHat());
				break;
			case UPDATE_LIST_ORDER:
				currentEntry.setListOrder(receivedEntry.listOrder());
		}
	}

	/** Устанавливает публичную сессию игрока для верификации подписей. */
	private void setPublicSession(PlayerListS2CPacket.Entry receivedEntry, PlayerListEntry currentEntry) {
		GameProfile gameProfile = currentEntry.getProfile();
		SignatureVerifier signatureVerifier = client.getApiServices().serviceSignatureVerifier();
		if (signatureVerifier == null) {
			LOGGER.warn("Ignoring chat session from {} due to missing Services public key", gameProfile.name());
			currentEntry.resetSession(isSecureChatEnforced());
		}
		else {
			PublicPlayerSession.Serialized serialized = receivedEntry.chatSession();
			if (serialized != null) {
				try {
					PublicPlayerSession publicPlayerSession = serialized.toSession(gameProfile, signatureVerifier);
					currentEntry.setSession(publicPlayerSession);
				}
				catch (PlayerPublicKey.PublicKeyException var7) {
					LOGGER.error("Failed to validate profile key for player: '{}'", gameProfile.name(), var7);
					currentEntry.resetSession(isSecureChatEnforced());
				}
			}
			else {
				currentEntry.resetSession(isSecureChatEnforced());
			}
		}
	}

	/** Возвращает true, если сервер требует безопасный чат. */
	private boolean isSecureChatEnforced() {
		return client.getApiServices().providesProfileKeys() && secureChatEnforced;
	}

	/** Обновляет способности игрока (полёт, скорость и т.д.). */
	@Override
	public void onPlayerAbilities(PlayerAbilitiesS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		PlayerEntity playerEntity = client.player;
		playerEntity.getAbilities().flying = packet.isFlying();
		playerEntity.getAbilities().creativeMode = packet.isCreativeMode();
		playerEntity.getAbilities().invulnerable = packet.isInvulnerable();
		playerEntity.getAbilities().allowFlying = packet.allowFlying();
		playerEntity.getAbilities().setFlySpeed(packet.getFlySpeed());
		playerEntity.getAbilities().setWalkSpeed(packet.getWalkSpeed());
	}

	/** Воспроизводит звук в указанной позиции. */
	@Override
	public void onPlaySound(PlaySoundS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client
				.world
				.playSound(
						client.player,
						packet.getX(),
						packet.getY(),
						packet.getZ(),
						packet.getSound(),
						packet.getCategory(),
						packet.getVolume(),
						packet.getPitch(),
						packet.getSeed()
				);
	}

	/** Воспроизводит звук, привязанный к сущности. */
	@Override
	public void onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		if (entity != null) {
			client
					.world
					.playSoundFromEntity(
							client.player,
							entity,
							packet.getSound(),
							packet.getCategory(),
							packet.getVolume(),
							packet.getPitch(),
							packet.getSeed()
					);
		}
	}

	/** Обновляет полосу здоровья босса. */
	@Override
	public void onBossBar(BossBarS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.inGameHud.getBossBarHud().handlePacket(packet);
	}

	/** Обновляет кулдаун предмета. */
	@Override
	public void onCooldownUpdate(CooldownUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (packet.cooldown() == 0) {
			client.player.getItemCooldownManager().remove(packet.cooldownGroup());
		}
		else {
			client.player.getItemCooldownManager().set(packet.cooldownGroup(), packet.cooldown());
		}
	}

	/** Синхронизирует движение транспортного средства. */
	@Override
	public void onVehicleMove(VehicleMoveS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = client.player.getRootVehicle();
		if (entity != client.player && entity.isLogicalSideForUpdatingMovement()) {
			Vec3d vec3d = packet.position();
			Vec3d vec3d2;
			if (entity.isInterpolating()) {
				vec3d2 = entity.getInterpolator().getLerpedPos();
			}
			else {
				vec3d2 = entity.getEntityPos();
			}

			if (vec3d.distanceTo(vec3d2) > 1.0E-5F) {
				if (entity.isInterpolating()) {
					entity.getInterpolator().clear();
				}

				entity.updatePositionAndAngles(vec3d.getX(), vec3d.getY(), vec3d.getZ(), packet.yaw(), packet.pitch());
			}

			connection.send(VehicleMoveC2SPacket.fromVehicle(entity));
		}
	}

	/** Открывает экран чтения написанной книги. */
	@Override
	public void onOpenWrittenBook(OpenWrittenBookS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ItemStack itemStack = client.player.getStackInHand(packet.getHand());
		BookScreen.Contents contents = BookScreen.Contents.create(itemStack);
		if (contents != null) {
			client.setScreen(new BookScreen(contents));
		}
	}

	@Override
	public void onCustomPayload(CustomPayload payload) {
		warnOnUnknownPayload(payload);
	}

	private void warnOnUnknownPayload(CustomPayload payload) {
		LOGGER.warn("Unknown custom packet payload: {}", payload.getId().id());
	}

	/** Обновляет цель таблицы результатов. */
	@Override
	public void onScoreboardObjectiveUpdate(ScoreboardObjectiveUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		String string = packet.getName();
		if (packet.getMode() == 0) {
			scoreboard
					.addObjective(
							string,
							ScoreboardCriterion.DUMMY,
							packet.getDisplayName(),
							packet.getType(),
							false,
							packet.getNumberFormat().orElse(null)
					);
		}
		else {
			ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(string);
			if (scoreboardObjective != null) {
				if (packet.getMode() == 1) {
					scoreboard.removeObjective(scoreboardObjective);
				}
				else if (packet.getMode() == 2) {
					scoreboardObjective.setRenderType(packet.getType());
					scoreboardObjective.setDisplayName(packet.getDisplayName());
					scoreboardObjective.setNumberFormat(packet.getNumberFormat().orElse(null));
				}
			}
		}
	}

	/** Обновляет счёт в таблице результатов. */
	@Override
	public void onScoreboardScoreUpdate(ScoreboardScoreUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		String string = packet.objectiveName();
		ScoreHolder scoreHolder = ScoreHolder.fromName(packet.scoreHolderName());
		ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(string);
		if (scoreboardObjective != null) {
			ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, scoreboardObjective, true);
			scoreAccess.setScore(packet.score());
			scoreAccess.setDisplayText(packet.display().orElse(null));
			scoreAccess.setNumberFormat(packet.numberFormat().orElse(null));
		}
		else {
			LOGGER.warn("Received packet for unknown scoreboard objective: {}", string);
		}
	}

	/** Сбрасывает счёт в таблице результатов. */
	@Override
	public void onScoreboardScoreReset(ScoreboardScoreResetS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		String string = packet.objectiveName();
		ScoreHolder scoreHolder = ScoreHolder.fromName(packet.scoreHolderName());
		if (string == null) {
			scoreboard.removeScores(scoreHolder);
		}
		else {
			ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(string);
			if (scoreboardObjective != null) {
				scoreboard.removeScore(scoreHolder, scoreboardObjective);
			}
			else {
				LOGGER.warn("Received packet for unknown scoreboard objective: {}", string);
			}
		}
	}

	/** Устанавливает отображение таблицы результатов. */
	@Override
	public void onScoreboardDisplay(ScoreboardDisplayS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		String string = packet.getName();
		ScoreboardObjective scoreboardObjective = string == null ? null : scoreboard.getNullableObjective(string);
		scoreboard.setObjectiveSlot(packet.getSlot(), scoreboardObjective);
	}

	/** Обрабатывает обновление команды (создание, удаление, изменение). */
	@Override
	public void onTeam(TeamS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		TeamS2CPacket.Operation operation = packet.getTeamOperation();
		Team team;
		if (operation == TeamS2CPacket.Operation.ADD) {
			team = scoreboard.addTeam(packet.getTeamName());
		}
		else {
			team = scoreboard.getTeam(packet.getTeamName());
			if (team == null) {
				LOGGER.warn(
						"Received packet for unknown team {}: team action: {}, player action: {}",
						new Object[]{packet.getTeamName(), packet.getTeamOperation(), packet.getPlayerListOperation()}
				);
				return;
			}
		}

		Optional<TeamS2CPacket.SerializableTeam> optional = packet.getTeam();
		optional.ifPresent(teamx -> {
			team.setDisplayName(teamx.getDisplayName());
			team.setColor(teamx.getColor());
			team.setFriendlyFlagsBitwise(teamx.getFriendlyFlagsBitwise());
			team.setNameTagVisibilityRule(teamx.getNameTagVisibilityRule());
			team.setCollisionRule(teamx.getCollisionRule());
			team.setPrefix(teamx.getPrefix());
			team.setSuffix(teamx.getSuffix());
		});
		TeamS2CPacket.Operation operation2 = packet.getPlayerListOperation();
		if (operation2 == TeamS2CPacket.Operation.ADD) {
			for (String string : packet.getPlayerNames()) {
				scoreboard.addScoreHolderToTeam(string, team);
			}
		}
		else if (operation2 == TeamS2CPacket.Operation.REMOVE) {
			for (String string : packet.getPlayerNames()) {
				scoreboard.removeScoreHolderFromTeam(string, team);
			}
		}

		if (operation == TeamS2CPacket.Operation.REMOVE) {
			scoreboard.removeTeam(team);
		}
	}

	/** Создаёт частицы в указанной позиции. */
	@Override
	public void onParticle(ParticleS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (packet.getCount() == 0) {
			double d = packet.getSpeed() * packet.getOffsetX();
			double e = packet.getSpeed() * packet.getOffsetY();
			double f = packet.getSpeed() * packet.getOffsetZ();

			try {
				world
						.addParticleClient(
								packet.getParameters(),
								packet.shouldForceSpawn(),
								packet.isImportant(),
								packet.getX(),
								packet.getY(),
								packet.getZ(),
								d,
								e,
								f
						);
			}
			catch (Throwable var17) {
				LOGGER.warn("Could not spawn particle effect {}", packet.getParameters());
			}
		}
		else {
			for (int i = 0; i < packet.getCount(); i++) {
				double g = random.nextGaussian() * packet.getOffsetX();
				double h = random.nextGaussian() * packet.getOffsetY();
				double j = random.nextGaussian() * packet.getOffsetZ();
				double k = random.nextGaussian() * packet.getSpeed();
				double l = random.nextGaussian() * packet.getSpeed();
				double m = random.nextGaussian() * packet.getSpeed();

				try {
					world
							.addParticleClient(
									packet.getParameters(),
									packet.shouldForceSpawn(),
									packet.isImportant(),
									packet.getX() + g,
									packet.getY() + h,
									packet.getZ() + j,
									k,
									l,
									m
							);
				}
				catch (Throwable var16) {
					LOGGER.warn("Could not spawn particle effect {}", packet.getParameters());
					return;
				}
			}
		}
	}

	/** Обновляет атрибуты сущности (здоровье, скорость и т.д.). */
	@Override
	public void onEntityAttributes(EntityAttributesS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.getEntityId());
		if (entity != null) {
			if (!(entity instanceof LivingEntity)) {
				throw new IllegalStateException(
						"Server tried to update attributes of a non-living entity (actually: " + entity + ")");
			}
			else {
				AttributeContainer attributeContainer = ((LivingEntity) entity).getAttributes();

				for (EntityAttributesS2CPacket.Entry entry : packet.getEntries()) {
					EntityAttributeInstance
							entityAttributeInstance =
							attributeContainer.getCustomInstance(entry.attribute());
					if (entityAttributeInstance == null) {
						LOGGER.warn("Entity {} does not have attribute {}", entity, entry.attribute().getIdAsString());
					}
					else {
						entityAttributeInstance.setBaseValue(entry.base());
						entityAttributeInstance.clearModifiers();

						for (EntityAttributeModifier entityAttributeModifier : entry.modifiers()) {
							entityAttributeInstance.addTemporaryModifier(entityAttributeModifier);
						}
					}
				}
			}
		}
	}

	/** Обрабатывает неудачную попытку крафта. */
	@Override
	public void onCraftFailedResponse(CraftFailedResponseS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ScreenHandler screenHandler = client.player.currentScreenHandler;
		if (screenHandler.syncId == packet.syncId()) {
			if (client.currentScreen instanceof RecipeBookProvider recipeBookProvider) {
				recipeBookProvider.onCraftFailed(packet.recipeDisplay());
			}
		}
	}

	/** Обновляет данные освещения в чанке. */
	@Override
	public void onLightUpdate(LightUpdateS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		int i = packet.getChunkX();
		int j = packet.getChunkZ();
		LightData lightData = packet.getData();
		world.enqueueChunkUpdate(() -> readLightData(i, j, lightData, true));
	}

	/** Читает и применяет данные освещения к чанку. */
	private void readLightData(int x, int z, LightData data, boolean scheduleBlockRenders) {
		LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
		BitSet bitSet = data.getInitedSky();
		BitSet bitSet2 = data.getUninitedSky();
		Iterator<byte[]> iterator = data.getSkyNibbles().iterator();
		updateLighting(x, z, lightingProvider, LightType.SKY, bitSet, bitSet2, iterator, scheduleBlockRenders);
		BitSet bitSet3 = data.getInitedBlock();
		BitSet bitSet4 = data.getUninitedBlock();
		Iterator<byte[]> iterator2 = data.getBlockNibbles().iterator();
		updateLighting(x, z, lightingProvider, LightType.BLOCK, bitSet3, bitSet4, iterator2, scheduleBlockRenders);
		lightingProvider.setColumnEnabled(new ChunkPos(x, z), true);
	}

	/** Обновляет список торговых предложений торговца. */
	@Override
	public void onSetTradeOffers(SetTradeOffersS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		ScreenHandler screenHandler = client.player.currentScreenHandler;
		if (packet.getSyncId() == screenHandler.syncId
				&& screenHandler instanceof MerchantScreenHandler merchantScreenHandler) {
			merchantScreenHandler.setOffers(packet.getOffers());
			merchantScreenHandler.setExperienceFromServer(packet.getExperience());
			merchantScreenHandler.setLevelProgress(packet.getLevelProgress());
			merchantScreenHandler.setLeveled(packet.isLeveled());
			merchantScreenHandler.setCanRefreshTrades(packet.isRefreshable());
		}
	}

	/** Устанавливает дистанцию загрузки чанков. */
	@Override
	public void onChunkLoadDistance(ChunkLoadDistanceS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		chunkLoadDistance = packet.getDistance();
		client.options.setServerViewDistance(chunkLoadDistance);
		world.getChunkManager().updateLoadDistance(packet.getDistance());
	}

	/** Устанавливает дистанцию симуляции. */
	@Override
	public void onSimulationDistance(SimulationDistanceS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		simulationDistance = packet.simulationDistance();
		world.setSimulationDistance(simulationDistance);
	}

	/** Обновляет центр рендеринга чанков. */
	@Override
	public void onChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.getChunkManager().setChunkMapCenter(packet.getChunkX(), packet.getChunkZ());
	}

	/** Обрабатывает ответ сервера на действие игрока с блоком. */
	@Override
	public void onPlayerActionResponse(PlayerActionResponseS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		world.handlePlayerActionResponse(packet.sequence());
	}

	@Override
	public void onBundle(BundleS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		for (Packet<? super ClientPlayPacketListener> packet2 : packet.getPackets()) {
			packet2.apply(this);
		}
	}

	/** Обновляет мощность снаряда. */
	@Override
	public void onProjectilePower(ProjectilePowerS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (world.getEntityById(packet.getEntityId()) instanceof ExplosiveProjectileEntity explosiveProjectileEntity) {
			explosiveProjectileEntity.accelerationPower = packet.getAccelerationPower();
		}
	}

	/** Начинает отправку батча чанков от сервера. */
	@Override
	public void onStartChunkSend(StartChunkSendS2CPacket packet) {
		chunkBatchSizeCalculator.onStartChunkSend();
	}

	/** Подтверждает получение батча чанков. */
	@Override
	public void onChunkSent(ChunkSentS2CPacket packet) {
		chunkBatchSizeCalculator.onChunkSent(packet.batchSize());
		sendPacket(new AcknowledgeChunksC2SPacket(chunkBatchSizeCalculator.getDesiredChunksPerTick()));
	}

	/** Обрабатывает отладочные данные производительности. */
	@Override
	public void onDebugSample(DebugSampleS2CPacket packet) {
		client.getDebugHud().set(packet.sample(), packet.debugSampleType());
	}

	/** Обрабатывает результат пинга. */
	@Override
	public void onPingResult(PingResultS2CPacket packet) {
		pingMeasurer.onPingResult(packet);
	}

	/** Обрабатывает статус тестового блока (только для разработки). */
	@Override
	public void onTestInstanceBlockStatus(TestInstanceBlockStatusS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		if (client.currentScreen instanceof TestInstanceBlockScreen testInstanceBlockScreen) {
			testInstanceBlockScreen.handleStatus(packet.status(), packet.size());
		}
	}

	/** Обрабатывает обновление путевой точки. */
	@Override
	public void onWaypoint(WaypointS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		packet.apply(waypointHandler);
	}

	/** Обрабатывает отладочные данные значений чанка. */
	@Override
	public void onChunkValueDebug(ChunkValueDebugS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		debugSubscriptionManager.updateChunk(world.getTime(), packet.chunkPos(), packet.update());
	}

	/** Обрабатывает отладочные данные значений блока. */
	@Override
	public void onBlockValueDebug(BlockValueDebugS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		debugSubscriptionManager.updateBlock(world.getTime(), packet.blockPos(), packet.update());
	}

	/** Обрабатывает отладочные данные значений сущности. */
	@Override
	public void onEntityValueDebug(EntityValueDebugS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		Entity entity = world.getEntityById(packet.entityId());
		if (entity != null) {
			debugSubscriptionManager.updateEntity(world.getTime(), entity, packet.update());
		}
	}

	/** Обрабатывает отладочное событие. */
	@Override
	public void onEventDebug(EventDebugS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		debugSubscriptionManager.addEvent(world.getTime(), packet.event());
	}

	/** Подсвечивает позицию в игровом тесте (только для разработки). */
	@Override
	public void onGameTestHighlightPos(GameTestHighlightPosS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		client.worldRenderer.gameTestDebugRenderer.addMarker(packet.absolutePos(), packet.relativePos());
	}

	/** Обновляет освещение в указанном чанке. */
	private void updateLighting(
			int chunkX,
			int chunkZ,
			LightingProvider provider,
			LightType type,
			BitSet inited,
			BitSet uninited,
			Iterator<byte[]> nibbles,
			boolean scheduleBlockRenders
	) {
		for (int i = 0; i < provider.getHeight(); i++) {
			int j = provider.getBottomY() + i;
			boolean bl = inited.get(i);
			boolean bl2 = uninited.get(i);
			if (bl || bl2) {
				provider.enqueueSectionData(
						type,
						ChunkSectionPos.from(chunkX, j, chunkZ),
						bl ? new ChunkNibbleArray((byte[]) nibbles.next().clone()) : new ChunkNibbleArray()
				);
				if (scheduleBlockRenders) {
					world.scheduleBlockRenders(chunkX, j, chunkZ);
				}
			}
		}
	}

	/** Возвращает активное сетевое соединение. */
	public ClientConnection getConnection() {
		return connection;
	}

	/** Возвращает true, если соединение открыто и мир не был выгружен. */
	@Override
	public boolean isConnectionOpen() {
		return connection.isOpen() && !worldCleared;
	}

	/** Возвращает коллекцию отображаемых записей списка игроков. */
	public Collection<PlayerListEntry> getListedPlayerListEntries() {
		return listedPlayerListEntries;
	}

	/** Возвращает коллекцию всех записей списка игроков (включая скрытых). */
	public Collection<PlayerListEntry> getPlayerList() {
		return playerListEntries.values();
	}

	/** Возвращает множество UUID всех игроков в списке. */
	public Collection<UUID> getPlayerUuids() {
		return playerListEntries.keySet();
	}

	/** Возвращает запись списка игроков по UUID. */
	public @Nullable PlayerListEntry getPlayerListEntry(UUID uuid) {
		return playerListEntries.get(uuid);
	}

	/** Возвращает запись списка игроков по имени профиля. */
	public @Nullable PlayerListEntry getPlayerListEntry(String profileName) {
		for (PlayerListEntry playerListEntry : playerListEntries.values()) {
			if (playerListEntry.getProfile().name().equals(profileName)) {
				return playerListEntry;
			}
		}

		return null;
	}

	/** Возвращает карту игроков, которых видел клиент в текущей сессии. */
	public Map<UUID, PlayerListEntry> getSeenPlayers() {
		return seenPlayers;
	}

	/** Возвращает запись списка игроков без учёта регистра имени. */
	public @Nullable PlayerListEntry getCaseInsensitivePlayerInfo(String name) {
		for (PlayerListEntry playerListEntry : playerListEntries.values()) {
			if (playerListEntry.getProfile().name().equalsIgnoreCase(name)) {
				return playerListEntry;
			}
		}

		return null;
	}

	/** Возвращает игровой профиль текущего игрока. */
	public GameProfile getProfile() {
		return profile;
	}

	/** Возвращает менеджер достижений. */
	public ClientAdvancementManager getAdvancementHandler() {
		return advancementHandler;
	}

	/** Возвращает диспетчер команд клиента. */
	public CommandDispatcher<ClientCommandSource> getCommandDispatcher() {
		return commandDispatcher;
	}

	/** Возвращает текущий клиентский мир. */
	public ClientWorld getWorld() {
		return world;
	}

	/** Возвращает обработчик NBT-запросов. */
	public DataQueryHandler getDataQueryHandler() {
		return dataQueryHandler;
	}

	/** Возвращает уникальный идентификатор текущей сессии. */
	public UUID getSessionId() {
		return sessionId;
	}

	/** Возвращает набор ключей всех доступных измерений. */
	public Set<RegistryKey<World>> getWorldKeys() {
		return worldKeys;
	}

	/** Возвращает иммутабельный менеджер динамических реестров. */
	public DynamicRegistryManager.Immutable getRegistryManager() {
		return combinedDynamicRegistries;
	}

	/** Подтверждает получение подписанного сообщения. */
	public void acknowledge(MessageSignatureData signature, boolean displayed) {
		if (lastSeenMessagesCollector.add(signature, displayed) && lastSeenMessagesCollector.getMessageCount() > 64) {
			sendAcknowledgment();
		}
	}

	/** Отправляет пакет подтверждения сообщений на сервер. */
	private void sendAcknowledgment() {
		int i = lastSeenMessagesCollector.resetMessageCount();
		if (i > 0) {
			sendPacket(new MessageAcknowledgmentC2SPacket(i));
		}
	}

	/** Отправляет сообщение в чат. */
	public void sendChatMessage(String content) {
		Instant instant = Instant.now();
		long l = NetworkEncryptionUtils.SecureRandomUtil.nextLong();
		LastSeenMessagesCollector.LastSeenMessages lastSeenMessages = lastSeenMessagesCollector.collect();
		MessageSignatureData
				messageSignatureData =
				messagePacker.pack(new MessageBody(content, instant, l, lastSeenMessages.lastSeen()));
		sendPacket(new ChatMessageC2SPacket(content, instant, l, messageSignatureData, lastSeenMessages.update()));
	}

	/** Отправляет команду чата с подписями аргументов. */
	public void sendChatCommand(String command) {
		SignedArgumentList<ClientCommandSource>
				signedArgumentList =
				SignedArgumentList.of(commandDispatcher.parse(command, commandSource));
		if (signedArgumentList.arguments().isEmpty()) {
			sendPacket(new CommandExecutionC2SPacket(command));
		}
		else {
			Instant instant = Instant.now();
			long l = NetworkEncryptionUtils.SecureRandomUtil.nextLong();
			LastSeenMessagesCollector.LastSeenMessages lastSeenMessages = lastSeenMessagesCollector.collect();
			ArgumentSignatureDataMap argumentSignatureDataMap = ArgumentSignatureDataMap.sign(
					signedArgumentList, value -> {
						MessageBody messageBody = new MessageBody(value, instant, l, lastSeenMessages.lastSeen());
						return messagePacker.pack(messageBody);
					}
			);
			sendPacket(new ChatCommandSignedC2SPacket(
					command,
					instant,
					l,
					argumentSignatureDataMap,
					lastSeenMessages.update()
			));
		}
	}

	/** Выполняет команду из события клика (ссылка в чате). */
	public void runClickEventCommand(String command, @Nullable Screen afterActionScreen) {
		switch (parseCommand(command)) {
			case NO_ISSUES:
				sendPacket(new CommandExecutionC2SPacket(command));
				client.setScreen(afterActionScreen);
				break;
			case PARSE_ERRORS:
				openConfirmRunCommandScreen(command, "multiplayer.confirm_command.parse_errors", afterActionScreen);
				break;
			case SIGNATURE_REQUIRED:
				suggestCommand(command, "multiplayer.confirm_command.signature_required", afterActionScreen);
				break;
			case PERMISSIONS_REQUIRED:
				openConfirmRunCommandScreen(
						command,
						"multiplayer.confirm_command.permissions_required",
						afterActionScreen
				);
		}
	}

	/** Разбирает команду и определяет, требует ли она подтверждения. */
	private ClientPlayNetworkHandler.CommandRunResult parseCommand(String command) {
		ParseResults<ClientCommandSource> parseResults = commandDispatcher.parse(command, commandSource);
		if (!validate(parseResults)) {
			return ClientPlayNetworkHandler.CommandRunResult.PARSE_ERRORS;
		}
		else if (SignedArgumentList.isNotEmpty(parseResults)) {
			return ClientPlayNetworkHandler.CommandRunResult.SIGNATURE_REQUIRED;
		}
		else {
			ParseResults<ClientCommandSource> parseResults2 = commandDispatcher.parse(command, restrictedCommandSource);
			return !validate(parseResults2) ? ClientPlayNetworkHandler.CommandRunResult.PERMISSIONS_REQUIRED
			                                : ClientPlayNetworkHandler.CommandRunResult.NO_ISSUES;
		}
	}

	/** Проверяет, что результат разбора команды не содержит ошибок и имеет исполняемую ветку. */
	private static boolean validate(ParseResults<?> parseResults) {
		return !parseResults.getReader().canRead() && parseResults.getExceptions().isEmpty()
				&& parseResults.getContext().getLastChild().getCommand() != null;
	}

	/** Открывает экран подтверждения выполнения команды. */
	private void openConfirmCommandScreen(String command, String message, Text yesText, Runnable action) {
		Screen screen = client.currentScreen;
		client
				.setScreen(
						new ConfirmScreen(
								confirmed -> {
									if (confirmed) {
										action.run();
									}
									else {
										client.setScreen(screen);
									}
								},
								CONFIRM_COMMAND_TITLE_TEXT,
								Text.translatable(message, Text.literal(command).formatted(Formatting.YELLOW)),
								yesText,
								screen != null ? ScreenTexts.BACK : ScreenTexts.CANCEL
						)
				);
	}

	/** Открывает экран подтверждения запуска команды. */
	private void openConfirmRunCommandScreen(String command, String message, @Nullable Screen screenAfterRun) {
		openConfirmCommandScreen(
				command, message, CONFIRM_RUN_COMMAND_TEXT, () -> {
					sendPacket(new CommandExecutionC2SPacket(command));
					client.setScreen(screenAfterRun);
				}
		);
	}

	/** Предлагает команду пользователю с возможностью подтверждения. */
	private void suggestCommand(String command, String message, @Nullable Screen afterActionScreen) {
		boolean bl = afterActionScreen == null && client.getChatRestriction().allowsChat(client.isInSingleplayer());
		openConfirmCommandScreen(
				command, message, bl ? CONFIRM_SUGGEST_COMMAND_TEXT : ScreenTexts.COPY, () -> {
					if (bl) {
						client.openChatScreen(ChatHud.ChatMethod.COMMAND);
						if (client.currentScreen instanceof ChatScreen chatScreen) {
							chatScreen.insertText(command, false);
						}
					}
					else {
						client.keyboard.setClipboard("/" + command);
						client.setScreen(afterActionScreen);
					}
				}
		);
	}

	/** Синхронизирует настройки клиента с сервером, если они изменились. */
	public void syncOptions(SyncedClientOptions newOptions) {
		if (syncedOptions.equals(newOptions)) {
			return;
		}

		sendPacket(new ClientOptionsC2SPacket(newOptions));
		syncedOptions = newOptions;
	}

	/** Выполняет тиковые операции: отправку пакетов движения, обновление пинга. */
	@Override
	public void tick() {
		if (session != null && client.getProfileKeys().isExpired()) {
			fetchProfileKey();
		}

		if (profileKeyPairFuture != null && profileKeyPairFuture.isDone()) {
			profileKeyPairFuture.join().ifPresent(this::updateKeyPair);
			profileKeyPairFuture = null;
		}

		sendQueuedPackets();
		if (client.getDebugHud().shouldShowPacketSizeAndPingCharts()) {
			pingMeasurer.ping();
		}

		if (world != null) {
			debugSubscriptionManager.startTick(world.getTime());
		}

		worldSession.tick();
		if (chunkLoadProgress != null) {
			chunkLoadProgress.tick();
			if (chunkLoadProgress.isDone()) {
				setPlayerLoaded();
				chunkLoadProgress = null;
			}
		}
	}

	/** Помечает игрока как загруженного и отправляет пакет серверу. */
	private void setPlayerLoaded() {
		if (isPlayerLoaded()) {
			LOGGER.debug("[WorldLoading] setPlayerLoaded() — игрок уже помечен как загруженный, пропускаем");
			return;
		}

		LOGGER.info("[WorldLoading] setPlayerLoaded() — отправляем PlayerLoadedC2SPacket, закрываем экран загрузки");
		connection.send(new PlayerLoadedC2SPacket());
		setPlayerLoadedState(true);

		if (client.currentScreen instanceof LevelLoadingScreen) {
			client.setScreen(null);
			client.mouse.lockCursor();
		}
	}

	/** Запрашивает ключевую пару профиля для подписи сообщений. */
	public void fetchProfileKey() {
		profileKeyPairFuture = client.getProfileKeys().fetchKeyPair();
	}

	/** Обновляет ключевую пару для подписи сообщений. */
	private void updateKeyPair(PlayerKeyPair keyPair) {
		if (client.uuidEquals(profile.id())) {
			if (session == null || !session.keyPair().equals(keyPair)) {
				session = ClientPlayerSession.create(keyPair);
				messagePacker = session.createPacker(profile.id());
				sendPacket(new PlayerSessionC2SPacket(session.toPublicSession().toSerialized()));
			}
		}
	}

	/** Возвращает объект доступа к сети для диалогов. */
	@Override
	protected DialogNetworkAccess createDialogNetworkAccess() {
		return new ClientCommonNetworkHandler.CommonDialogNetworkAccess() {
			@Override
			public void runClickEventCommand(String command, @Nullable Screen afterActionScreen) {
				ClientPlayNetworkHandler.this.runClickEventCommand(command, afterActionScreen);
			}
		};
	}

	/** Возвращает информацию о текущем сервере, или null для одиночной игры. */
	public @Nullable ServerInfo getServerInfo() {
		return serverInfo;
	}

	/** Возвращает набор включённых функций. */
	public FeatureSet getEnabledFeatures() {
		return enabledFeatures;
	}

	/** Возвращает true, если указанный набор функций является подмножеством включённых. */
	public boolean hasFeature(FeatureSet feature) {
		return feature.isSubsetOf(getEnabledFeatures());
	}

	/** Возвращает таблицу результатов текущей сессии. */
	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	/** Возвращает реестр рецептов варки зелий. */
	public BrewingRecipeRegistry getBrewingRecipeRegistry() {
		return brewingRecipeRegistry;
	}

	/** Возвращает реестр видов топлива. */
	public FuelRegistry getFuelRegistry() {
		return fuelRegistry;
	}

	/** Обновляет поисковые индексы (рецепты, предметы и т.д.). */
	public void refreshSearchManager() {
		searchManager.refresh();
	}

	/** Возвращает менеджер поиска. */
	public SearchManager getSearchManager() {
		return searchManager;
	}

	/** Регистрирует DataCache для очистки при выгрузке мира. */
	public void registerForCleaning(DataCache<?, ?> dataCache) {
		cachedData.add(new WeakReference<>(dataCache));
	}

	/** Возвращает хешер компонентов предметов. */
	public ComponentChangesHash.ComponentHasher getComponentHasher() {
		return componentHasher;
	}

	/** Возвращает обработчик путевых точек. */
	public ClientWaypointHandler getWaypointHandler() {
		return waypointHandler;
	}

	/** Возвращает хранилище отладочных данных для текущего мира. */
	public DebugDataStore getDebugDataStore() {
		return debugSubscriptionManager.createDebugDataStore(world);
	}

	/** Возвращает true, если игрок уже помечен как загруженный. */
	public boolean isPlayerLoaded() {
		return playerLoaded;
	}

	private void setPlayerLoadedState(boolean loaded) {
		playerLoaded = loaded;
	}

	/** Результат разбора команды перед отправкой на сервер. */
	@Environment(EnvType.CLIENT)
	static enum CommandRunResult {
		/** Команда корректна и может быть выполнена без подтверждения. */
		NO_ISSUES,
		/** Команда содержит синтаксические ошибки. */
		PARSE_ERRORS,
		/** Команда требует подписи (содержит подписываемые аргументы). */
		SIGNATURE_REQUIRED,
		/** Команда требует прав, которых нет у игрока. */
		PERMISSIONS_REQUIRED;
	}
}
