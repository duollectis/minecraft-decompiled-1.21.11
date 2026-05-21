package net.minecraft.server.network;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.block.entity.SculkShriekerWarningManager;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.scoreboard.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.sync.TrackedSlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.dynamic.HashCodeOps;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.*;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * {@code ServerPlayerEntity}.
 */
public class ServerPlayerEntity extends PlayerEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int FORGIVE_ANGER_RADIUS_XZ = 32;
	private static final int FORGIVE_ANGER_RADIUS_Y = 10;
	private static final int MIN_FLY_STAT_DISTANCE = 25;
	public static final double BED_RANGE_Y = 1.0;
	public static final double BED_RANGE_XZ = 3.0;
	public static final int DEFAULT_VIEW_DISTANCE = 2;
	public static final String ENDER_PEARLS_KEY = "ender_pearls";
	public static final String ENDER_PEARLS_DIMENSION_KEY = "ender_pearl_dimension";
	public static final String DIMENSION_KEY = "Dimension";
	private static final EntityAttributeModifier
			CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER =
			new EntityAttributeModifier(
					Identifier.ofVanilla("creative_mode_block_range"), 0.5, EntityAttributeModifier.Operation.ADD_VALUE
			);
	private static final EntityAttributeModifier
			CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER =
			new EntityAttributeModifier(
					Identifier.ofVanilla("creative_mode_entity_range"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE
			);
	private static final Text SET_SPAWN_TEXT = Text.translatable("block.minecraft.set_spawn");
	private static final EntityAttributeModifier WAYPOINT_TRANSMIT_RANGE_CROUCH_MODIFIER = new EntityAttributeModifier(
			Identifier.ofVanilla("waypoint_transmit_range_crouch"),
			-1.0,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);
	private static final boolean DEFAULT_SEEN_CREDITS = false;
	private static final boolean DEFAULT_SPAWN_EXTRA_PARTICLES_ON_FALL = false;
	public ServerPlayNetworkHandler networkHandler;
	private final MinecraftServer server;
	public final ServerPlayerInteractionManager interactionManager;
	private final PlayerAdvancementTracker advancementTracker;
	private final ServerStatHandler statHandler;
	private float lastHealthScore = Float.MIN_VALUE;
	private int lastFoodScore = Integer.MIN_VALUE;
	private int lastAirScore = Integer.MIN_VALUE;
	private int lastArmorScore = Integer.MIN_VALUE;
	private int lastLevelScore = Integer.MIN_VALUE;
	private int lastExperienceScore = Integer.MIN_VALUE;
	private float syncedHealth = -1.0E8F;
	private int syncedFoodLevel = -99999999;
	private boolean syncedSaturationIsZero = true;
	private int syncedExperience = -99999999;
	private ChatVisibility clientChatVisibility = ChatVisibility.FULL;
	private ParticlesMode particlesMode = ParticlesMode.ALL;
	private boolean clientChatColorsEnabled = true;
	private long lastActionTime = Util.getMeasuringTimeMs();
	private @Nullable Entity cameraEntity;
	private boolean inTeleportationState;
	public boolean seenCredits = false;
	private final ServerRecipeBook recipeBook;
	private @Nullable Vec3d levitationStartPos;
	private int levitationStartTick;
	private boolean disconnected;
	private int viewDistance = 2;
	private String language = "en_us";
	private @Nullable Vec3d fallStartPos;
	private @Nullable Vec3d enteredNetherPos;
	private @Nullable Vec3d vehicleInLavaRidingPos;
	private ChunkSectionPos watchedSection = ChunkSectionPos.from(0, 0, 0);
	private ChunkFilter chunkFilter = ChunkFilter.IGNORE_ALL;
	private ServerPlayerEntity.@Nullable Respawn respawn;
	private final TextStream textStream;
	private boolean filterText;
	private boolean allowServerListing;
	private boolean spawnExtraParticlesOnFall = false;
	private SculkShriekerWarningManager sculkShriekerWarningManager = new SculkShriekerWarningManager();
	private @Nullable BlockPos startRaidPos;
	private Vec3d movement = Vec3d.ZERO;
	private PlayerInput playerInput = PlayerInput.DEFAULT;
	private final Set<EnderPearlEntity> enderPearls = new HashSet<>();
	private long shoulderMountTime;
	private NbtCompound leftShoulderNbt = new NbtCompound();
	private NbtCompound rightShoulderNbt = new NbtCompound();
	private final ScreenHandlerSyncHandler screenHandlerSyncHandler = new ScreenHandlerSyncHandler() {
		private final LoadingCache<Component<?>, Integer> componentHashCache = CacheBuilder.newBuilder()
		                                                                                   .maximumSize(256L)
		                                                                                   .build(
				                                                                                   new CacheLoader<Component<?>, Integer>() {
					                                                                                   private final DynamicOps<HashCode>
							                                                                                   hashOps =
							                                                                                   ServerPlayerEntity.this
									                                                                                   .getRegistryManager()
									                                                                                   .getOps(HashCodeOps.INSTANCE);

					                                                                                   public Integer load(
							                                                                                   Component<?> component
					                                                                                   ) {
						                                                                                   return ((HashCode) component
								                                                                                   .encode(this.hashOps)
								                                                                                   .getOrThrow(
										                                                                                   error -> new IllegalArgumentException(
												                                                                                   "Failed to hash "
														                                                                                   + component
														                                                                                   + ": "
														                                                                                   + error))
						                                                                                   )
								                                                                                   .asInt();
					                                                                                   }
				                                                                                   }
		                                                                                   );

		@Override
		public void updateState(
				ScreenHandler handler,
				List<ItemStack> stacks,
				ItemStack cursorStack,
				int[] properties
		) {
			ServerPlayerEntity.this.networkHandler.sendPacket(new InventoryS2CPacket(
					handler.syncId,
					handler.nextRevision(),
					stacks,
					cursorStack
			));

			for (int i = 0; i < properties.length; i++) {
				this.sendPropertyUpdate(handler, i, properties[i]);
			}
		}

		@Override
		public void updateSlot(ScreenHandler handler, int slot, ItemStack stack) {
			ServerPlayerEntity.this.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
					handler.syncId,
					handler.nextRevision(),
					slot,
					stack
			));
		}

		@Override
		public void updateCursorStack(ScreenHandler handler, ItemStack stack) {
			ServerPlayerEntity.this.networkHandler.sendPacket(new SetCursorItemS2CPacket(stack));
		}

		@Override
		public void updateProperty(ScreenHandler handler, int property, int value) {
			this.sendPropertyUpdate(handler, property, value);
		}

		private void sendPropertyUpdate(ScreenHandler handler, int property, int value) {
			ServerPlayerEntity.this.networkHandler.sendPacket(new ScreenHandlerPropertyUpdateS2CPacket(
					handler.syncId,
					property,
					value
			));
		}

		@Override
		public TrackedSlot createTrackedSlot() {
			return new TrackedSlot.Impl(this.componentHashCache::getUnchecked);
		}
	};
	private final ScreenHandlerListener screenHandlerListener = new ScreenHandlerListener() {
		@Override
		public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
			Slot slot = handler.getSlot(slotId);
			if (!(slot instanceof CraftingResultSlot)) {
				if (slot.inventory == ServerPlayerEntity.this.getInventory()) {
					Criteria.INVENTORY_CHANGED.trigger(
							ServerPlayerEntity.this,
							ServerPlayerEntity.this.getInventory(),
							stack
					);
				}
			}
		}

		@Override
		public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
		}
	};
	private @Nullable PublicPlayerSession session;
	public final @Nullable Object fabricApiExtension;
	private final CommandOutput commandOutput = new CommandOutput() {
		@Override
		public boolean shouldReceiveFeedback() {
			return ServerPlayerEntity.this.getEntityWorld().getGameRules().getValue(GameRules.SEND_COMMAND_FEEDBACK);
		}

		@Override
		public boolean shouldTrackOutput() {
			return true;
		}

		@Override
		public boolean shouldBroadcastConsoleToOps() {
			return true;
		}

		@Override
		public void sendMessage(Text message) {
			ServerPlayerEntity.this.sendMessage(message);
		}
	};
	private Set<DebugSubscriptionType<?>> subscribedTypes = Set.of();
	private int screenHandlerSyncId;
	public boolean notInAnyWorld;

	public ServerPlayerEntity(
			MinecraftServer server,
			ServerWorld world,
			GameProfile profile,
			SyncedClientOptions clientOptions
	) {
		super(world, profile);
		this.server = server;
		this.textStream = server.createFilterer(this);
		this.interactionManager = server.getPlayerInteractionManager(this);
		this.interactionManager.setGameMode(this.getServerGameMode(null), null);
		this.recipeBook =
				new ServerRecipeBook((key, adder) -> server.getRecipeManager().forEachRecipeDisplay(key, adder));
		this.statHandler = server.getPlayerManager().createStatHandler(this);
		this.advancementTracker = server.getPlayerManager().getAdvancementTracker(this);
		this.setClientOptions(clientOptions);
		this.fabricApiExtension = null;
	}

	@Override
	public BlockPos getWorldSpawnPos(ServerWorld world, BlockPos basePos) {
		CompletableFuture<Vec3d> completableFuture = SpawnLocating.locateSpawnPos(world, basePos);
		this.server.runTasks(completableFuture::isDone);
		return BlockPos.ofFloored(completableFuture.join());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.sculkShriekerWarningManager =
				view.<SculkShriekerWarningManager>read("warden_spawn_tracker", SculkShriekerWarningManager.CODEC)
				    .orElseGet(SculkShriekerWarningManager::new);
		this.enteredNetherPos = view.<Vec3d>read("entered_nether_pos", Vec3d.CODEC).orElse(null);
		this.seenCredits = view.getBoolean("seenCredits", false);
		view.<ServerRecipeBook.Packed>read("recipeBook", ServerRecipeBook.Packed.CODEC)
		    .ifPresent(recipeBook -> this.recipeBook.unpack(
				    recipeBook,
				    recipeKey -> this.server.getRecipeManager().get(recipeKey).isPresent()
		    ));
		if (this.isSleeping()) {
			this.wakeUp();
		}

		this.respawn = view.<ServerPlayerEntity.Respawn>read("respawn", ServerPlayerEntity.Respawn.CODEC).orElse(null);
		this.spawnExtraParticlesOnFall = view.getBoolean("spawn_extra_particles_on_fall", false);
		this.startRaidPos = view.<BlockPos>read("raid_omen_position", BlockPos.CODEC).orElse(null);
		this.interactionManager.setGameMode(
				this.getServerGameMode(gameModeFromData(view, "playerGameType")),
				gameModeFromData(view, "previousPlayerGameType")
		);
		this.setLeftShoulderNbt(view
				.<NbtCompound>read("ShoulderEntityLeft", NbtCompound.CODEC)
				.orElseGet(NbtCompound::new));
		this.setRightShoulderNbt(view
				.<NbtCompound>read("ShoulderEntityRight", NbtCompound.CODEC)
				.orElseGet(NbtCompound::new));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("warden_spawn_tracker", SculkShriekerWarningManager.CODEC, this.sculkShriekerWarningManager);
		this.writeGameModeData(view);
		view.putBoolean("seenCredits", this.seenCredits);
		view.putNullable("entered_nether_pos", Vec3d.CODEC, this.enteredNetherPos);
		this.writeRootVehicle(view);
		view.put("recipeBook", ServerRecipeBook.Packed.CODEC, this.recipeBook.pack());
		view.putString("Dimension", this.getEntityWorld().getRegistryKey().getValue().toString());
		view.putNullable("respawn", ServerPlayerEntity.Respawn.CODEC, this.respawn);
		view.putBoolean("spawn_extra_particles_on_fall", this.spawnExtraParticlesOnFall);
		view.putNullable("raid_omen_position", BlockPos.CODEC, this.startRaidPos);
		this.writeEnderPearls(view);
		if (!this.getLeftShoulderNbt().isEmpty()) {
			view.put("ShoulderEntityLeft", NbtCompound.CODEC, this.getLeftShoulderNbt());
		}

		if (!this.getRightShoulderNbt().isEmpty()) {
			view.put("ShoulderEntityRight", NbtCompound.CODEC, this.getRightShoulderNbt());
		}
	}

	private void writeRootVehicle(WriteView view) {
		Entity entity = this.getRootVehicle();
		Entity entity2 = this.getVehicle();
		if (entity2 != null && entity != this && entity.hasPlayerRider()) {
			WriteView writeView = view.get("RootVehicle");
			writeView.put("Attach", Uuids.INT_STREAM_CODEC, entity2.getUuid());
			entity.saveData(writeView.get("Entity"));
		}
	}

	public void readRootVehicle(ReadView view) {
		Optional<ReadView> optional = view.getOptionalReadView("RootVehicle");
		if (!optional.isEmpty()) {
			ServerWorld serverWorld = this.getEntityWorld();
			Entity entity = EntityType.loadEntityWithPassengers(
					optional.get().getReadView("Entity"),
					serverWorld,
					SpawnReason.LOAD,
					entityx -> !serverWorld.tryLoadEntity(entityx) ? null : entityx
			);
			if (entity != null) {
				UUID uUID = optional.get().<UUID>read("Attach", Uuids.INT_STREAM_CODEC).orElse(null);
				if (entity.getUuid().equals(uUID)) {
					this.startRiding(entity, true, false);
				}
				else {
					for (Entity entity2 : entity.getPassengersDeep()) {
						if (entity2.getUuid().equals(uUID)) {
							this.startRiding(entity2, true, false);
							break;
						}
					}
				}

				if (!this.hasVehicle()) {
					LOGGER.warn("Couldn't reattach entity to player");
					entity.discard();

					for (Entity entity2x : entity.getPassengersDeep()) {
						entity2x.discard();
					}
				}
			}
		}
	}

	private void writeEnderPearls(WriteView view) {
		if (!this.enderPearls.isEmpty()) {
			WriteView.ListView listView = view.getList("ender_pearls");

			for (EnderPearlEntity enderPearlEntity : this.enderPearls) {
				if (enderPearlEntity.isRemoved()) {
					LOGGER.warn("Trying to save removed ender pearl, skipping");
				}
				else {
					WriteView writeView = listView.add();
					enderPearlEntity.saveData(writeView);
					writeView.put(
							"ender_pearl_dimension",
							World.CODEC,
							enderPearlEntity.getEntityWorld().getRegistryKey()
					);
				}
			}
		}
	}

	public void readEnderPearls(ReadView view) {
		view.getListReadView("ender_pearls").forEach(this::readEnderPearl);
	}

	private void readEnderPearl(ReadView view) {
		Optional<RegistryKey<World>> optional = view.read("ender_pearl_dimension", World.CODEC);
		if (!optional.isEmpty()) {
			ServerWorld serverWorld = this.getEntityWorld().getServer().getWorld(optional.get());
			if (serverWorld != null) {
				Entity entity = EntityType.loadEntityWithPassengers(
						view,
						serverWorld,
						SpawnReason.LOAD,
						enderPearl -> !serverWorld.tryLoadEntity(enderPearl) ? null : enderPearl
				);
				if (entity != null) {
					addEnderPearlTicket(serverWorld, entity.getChunkPos());
				}
				else {
					LOGGER.warn("Failed to spawn player ender pearl in level ({}), skipping", optional.get());
				}
			}
			else {
				LOGGER.warn("Trying to load ender pearl without level ({}) being loaded, skipping", optional.get());
			}
		}
	}

	public void setExperiencePoints(int points) {
		float f = this.getNextLevelExperience();
		float g = (f - 1.0F) / f;
		float h = MathHelper.clamp(points / f, 0.0F, g);
		if (h != this.experienceProgress) {
			this.experienceProgress = h;
			this.syncedExperience = -1;
		}
	}

	public void setExperienceLevel(int level) {
		if (level != this.experienceLevel) {
			this.experienceLevel = level;
			this.syncedExperience = -1;
		}
	}

	@Override
	public void addExperienceLevels(int levels) {
		if (levels != 0) {
			super.addExperienceLevels(levels);
			this.syncedExperience = -1;
		}
	}

	@Override
	public void applyEnchantmentCosts(ItemStack enchantedItem, int experienceLevels) {
		super.applyEnchantmentCosts(enchantedItem, experienceLevels);
		this.syncedExperience = -1;
	}

	private void onScreenHandlerOpened(ScreenHandler screenHandler) {
		screenHandler.addListener(this.screenHandlerListener);
		screenHandler.updateSyncHandler(this.screenHandlerSyncHandler);
	}

	public void onSpawn() {
		this.onScreenHandlerOpened(this.playerScreenHandler);
	}

	@Override
	public void enterCombat() {
		super.enterCombat();
		this.networkHandler.sendPacket(EnterCombatS2CPacket.INSTANCE);
	}

	@Override
	public void endCombat() {
		super.endCombat();
		this.networkHandler.sendPacket(new EndCombatS2CPacket(this.getDamageTracker()));
	}

	@Override
	public void onBlockCollision(BlockState state) {
		Criteria.ENTER_BLOCK.trigger(this, state);
	}

	@Override
	protected ItemCooldownManager createCooldownManager() {
		return new ServerItemCooldownManager(this);
	}

	@Override
	public void tick() {
		this.networkHandler.tickLoading();
		this.interactionManager.update();
		this.sculkShriekerWarningManager.tick();
		if (this.timeUntilRegen > 0) {
			this.timeUntilRegen--;
		}

		this.currentScreenHandler.sendContentUpdates();
		if (!this.currentScreenHandler.canUse(this)) {
			this.closeHandledScreen();
			this.currentScreenHandler = this.playerScreenHandler;
		}

		Entity entity = this.getCameraEntity();
		if (entity != this) {
			if (entity.isAlive()) {
				this.updatePositionAndAngles(
						entity.getX(),
						entity.getY(),
						entity.getZ(),
						entity.getYaw(),
						entity.getPitch()
				);
				this.getEntityWorld().getChunkManager().updatePosition(this);
				if (this.shouldDismount()) {
					this.setCameraEntity(this);
				}
			}
			else {
				this.setCameraEntity(this);
			}
		}

		Criteria.TICK.trigger(this);
		if (this.levitationStartPos != null) {
			Criteria.LEVITATION.trigger(this, this.levitationStartPos, this.age - this.levitationStartTick);
		}

		this.tickFallStartPos();
		this.tickVehicleInLavaRiding();
		this.updateCreativeInteractionRangeModifiers();
		this.advancementTracker.sendUpdate(this, true);
	}

	private void updateCreativeInteractionRangeModifiers() {
		EntityAttributeInstance
				entityAttributeInstance =
				this.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
		if (entityAttributeInstance != null) {
			if (this.isCreative()) {
				entityAttributeInstance.updateModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
			}
			else {
				entityAttributeInstance.removeModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
			}
		}

		EntityAttributeInstance
				entityAttributeInstance2 =
				this.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
		if (entityAttributeInstance2 != null) {
			if (this.isCreative()) {
				entityAttributeInstance2.updateModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
			}
			else {
				entityAttributeInstance2.removeModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
			}
		}

		EntityAttributeInstance
				entityAttributeInstance3 =
				this.getAttributeInstance(EntityAttributes.WAYPOINT_TRANSMIT_RANGE);
		if (entityAttributeInstance3 != null) {
			if (this.isInSneakingPose()) {
				entityAttributeInstance3.updateModifier(WAYPOINT_TRANSMIT_RANGE_CROUCH_MODIFIER);
			}
			else {
				entityAttributeInstance3.removeModifier(WAYPOINT_TRANSMIT_RANGE_CROUCH_MODIFIER);
			}
		}
	}

	public void playerTick() {
		try {
			if (!this.isSpectator() || !this.isRegionUnloaded()) {
				super.tick();
				if (!this.currentScreenHandler.canUse(this)) {
					this.closeHandledScreen();
					this.currentScreenHandler = this.playerScreenHandler;
				}

				this.hungerManager.update(this);
				this.incrementStat(Stats.PLAY_TIME);
				this.incrementStat(Stats.TOTAL_WORLD_TIME);
				if (this.isAlive()) {
					this.incrementStat(Stats.TIME_SINCE_DEATH);
				}

				if (this.isSneaky()) {
					this.incrementStat(Stats.SNEAK_TIME);
				}

				if (!this.isSleeping()) {
					this.incrementStat(Stats.TIME_SINCE_REST);
				}
			}

			for (int i = 0; i < this.getInventory().size(); i++) {
				ItemStack itemStack = this.getInventory().getStack(i);
				if (!itemStack.isEmpty()) {
					this.sendMapPacket(itemStack);
				}
			}

			if (this.getHealth() != this.syncedHealth
					|| this.syncedFoodLevel != this.hungerManager.getFoodLevel()
					|| this.hungerManager.getSaturationLevel() == 0.0F != this.syncedSaturationIsZero) {
				this.networkHandler
						.sendPacket(new HealthUpdateS2CPacket(
								this.getHealth(),
								this.hungerManager.getFoodLevel(),
								this.hungerManager.getSaturationLevel()
						));
				this.syncedHealth = this.getHealth();
				this.syncedFoodLevel = this.hungerManager.getFoodLevel();
				this.syncedSaturationIsZero = this.hungerManager.getSaturationLevel() == 0.0F;
			}

			if (this.getHealth() + this.getAbsorptionAmount() != this.lastHealthScore) {
				this.lastHealthScore = this.getHealth() + this.getAbsorptionAmount();
				this.updateScores(ScoreboardCriterion.HEALTH, MathHelper.ceil(this.lastHealthScore));
			}

			if (this.hungerManager.getFoodLevel() != this.lastFoodScore) {
				this.lastFoodScore = this.hungerManager.getFoodLevel();
				this.updateScores(ScoreboardCriterion.FOOD, MathHelper.ceil((float) this.lastFoodScore));
			}

			if (this.getAir() != this.lastAirScore) {
				this.lastAirScore = this.getAir();
				this.updateScores(ScoreboardCriterion.AIR, MathHelper.ceil((float) this.lastAirScore));
			}

			if (this.getArmor() != this.lastArmorScore) {
				this.lastArmorScore = this.getArmor();
				this.updateScores(ScoreboardCriterion.ARMOR, MathHelper.ceil((float) this.lastArmorScore));
			}

			if (this.totalExperience != this.lastExperienceScore) {
				this.lastExperienceScore = this.totalExperience;
				this.updateScores(ScoreboardCriterion.XP, MathHelper.ceil((float) this.lastExperienceScore));
			}

			if (this.experienceLevel != this.lastLevelScore) {
				this.lastLevelScore = this.experienceLevel;
				this.updateScores(ScoreboardCriterion.LEVEL, MathHelper.ceil((float) this.lastLevelScore));
			}

			if (this.totalExperience != this.syncedExperience) {
				this.syncedExperience = this.totalExperience;
				this.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(
						this.experienceProgress,
						this.totalExperience,
						this.experienceLevel
				));
			}

			if (this.age % 20 == 0) {
				Criteria.LOCATION.trigger(this);
			}
		}
		catch (Throwable var4) {
			CrashReport crashReport = CrashReport.create(var4, "Ticking player");
			CrashReportSection crashReportSection = crashReport.addElement("Player being ticked");
			this.populateCrashReport(crashReportSection);
			throw new CrashException(crashReport);
		}
	}

	private void sendMapPacket(ItemStack stack) {
		MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
		MapState mapState = FilledMapItem.getMapState(mapIdComponent, this.getEntityWorld());
		if (mapState != null) {
			Packet<?> packet = mapState.getPlayerMarkerPacket(mapIdComponent, this);
			if (packet != null) {
				this.networkHandler.sendPacket(packet);
			}
		}
	}

	@Override
	protected void tickHunger() {
		if (this.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL && this
				.getEntityWorld()
				.getGameRules()
				.getValue(GameRules.NATURAL_HEALTH_REGENERATION)) {
			if (this.age % 20 == 0) {
				if (this.getHealth() < this.getMaxHealth()) {
					this.heal(1.0F);
				}

				float f = this.hungerManager.getSaturationLevel();
				if (f < 20.0F) {
					this.hungerManager.setSaturationLevel(f + 1.0F);
				}
			}

			if (this.age % 10 == 0 && this.hungerManager.isNotFull()) {
				this.hungerManager.setFoodLevel(this.hungerManager.getFoodLevel() + 1);
			}
		}
	}

	@Override
	public void handleShoulderEntities() {
		this.playShoulderEntitySound(this.getLeftShoulderNbt());
		this.playShoulderEntitySound(this.getRightShoulderNbt());
		if (this.fallDistance > 0.5 || this.isTouchingWater() || this.getAbilities().flying || this.isSleeping()
				|| this.inPowderSnow) {
			this.dropShoulderEntities();
		}
	}

	private void playShoulderEntitySound(NbtCompound nbt) {
		if (!nbt.isEmpty() && !nbt.getBoolean("Silent", false)) {
			if (this.random.nextInt(200) == 0) {
				EntityType<?> entityType = nbt.<EntityType<?>>get("id", EntityType.CODEC).orElse(null);
				if (entityType == EntityType.PARROT && !ParrotEntity.imitateNearbyMob(this.getEntityWorld(), this)) {
					this.getEntityWorld()
					    .playSound(
							    null,
							    this.getX(),
							    this.getY(),
							    this.getZ(),
							    ParrotEntity.getRandomSound(this.getEntityWorld(), this.random),
							    this.getSoundCategory(),
							    1.0F,
							    ParrotEntity.getSoundPitch(this.random)
					    );
				}
			}
		}
	}

	public boolean mountOntoShoulder(NbtCompound shoulderNbt) {
		if (this.hasVehicle() || !this.isOnGround() || this.isTouchingWater() || this.inPowderSnow) {
			return false;
		}
		else if (this.getLeftShoulderNbt().isEmpty()) {
			this.setLeftShoulderNbt(shoulderNbt);
			this.shoulderMountTime = this.getEntityWorld().getTime();
			return true;
		}
		else if (this.getRightShoulderNbt().isEmpty()) {
			this.setRightShoulderNbt(shoulderNbt);
			this.shoulderMountTime = this.getEntityWorld().getTime();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	protected void dropShoulderEntities() {
		if (this.shoulderMountTime + 20L < this.getEntityWorld().getTime()) {
			this.spawnShoulderEntity(this.getLeftShoulderNbt());
			this.setLeftShoulderNbt(new NbtCompound());
			this.spawnShoulderEntity(this.getRightShoulderNbt());
			this.setRightShoulderNbt(new NbtCompound());
		}
	}

	private void spawnShoulderEntity(NbtCompound nbt) {
		ServerWorld logging = this.getEntityWorld();
		if (logging instanceof ServerWorld) {
			ServerWorld serverWorld = logging;
			if (!nbt.isEmpty()) {
				try (ErrorReporter.Logging loggingx = new ErrorReporter.Logging(
						this.getErrorReporterContext(),
						LOGGER
				)
				) {
					EntityType.getEntityFromData(
							          NbtReadView.create(
									          loggingx.makeChild(() -> ".shoulder"),
									          serverWorld.getRegistryManager(),
									          nbt
							          ), serverWorld, SpawnReason.LOAD
					          )
					          .ifPresent(entity -> {
						          if (entity instanceof TameableEntity tameableEntity) {
							          tameableEntity.setOwner(this);
						          }

						          entity.setPosition(this.getX(), this.getY() + 0.7F, this.getZ());
						          serverWorld.tryLoadEntity(entity);
					          });
				}
			}
		}
	}

	@Override
	public void onLanding() {
		if (this.getHealth() > 0.0F && this.fallStartPos != null) {
			Criteria.FALL_FROM_HEIGHT.trigger(this, this.fallStartPos);
		}

		this.fallStartPos = null;
		super.onLanding();
	}

	public void tickFallStartPos() {
		if (this.fallDistance > 0.0 && this.fallStartPos == null) {
			this.fallStartPos = this.getEntityPos();
			if (this.currentExplosionImpactPos != null && this.currentExplosionImpactPos.y <= this.fallStartPos.y) {
				Criteria.FALL_AFTER_EXPLOSION.trigger(this, this.currentExplosionImpactPos, this.explodedBy);
			}
		}
	}

	public void tickVehicleInLavaRiding() {
		if (this.getVehicle() != null && this.getVehicle().isInLava()) {
			if (this.vehicleInLavaRidingPos == null) {
				this.vehicleInLavaRidingPos = this.getEntityPos();
			}
			else {
				Criteria.RIDE_ENTITY_IN_LAVA.trigger(this, this.vehicleInLavaRidingPos);
			}
		}

		if (this.vehicleInLavaRidingPos != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
			this.vehicleInLavaRidingPos = null;
		}
	}

	private void updateScores(ScoreboardCriterion criterion, int score) {
		this.getEntityWorld().getScoreboard().forEachScore(criterion, this, innerScore -> innerScore.setScore(score));
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		this.emitGameEvent(GameEvent.ENTITY_DIE);
		boolean bl = this.getEntityWorld().getGameRules().getValue(GameRules.SHOW_DEATH_MESSAGES);
		if (bl) {
			Text text = this.getDamageTracker().getDeathMessage();
			this.networkHandler
					.send(
							new DeathMessageS2CPacket(this.getId(), text),
							PacketCallbacks.of(
									() -> {
										int i = 256;
										String string = text.asTruncatedString(256);
										Text
												text2 =
												Text.translatable(
														"death.attack.message_too_long",
														Text.literal(string).formatted(Formatting.YELLOW)
												);
										Text
												text3 =
												Text.translatable("death.attack.even_more_magic", this.getDisplayName())
												    .styled(style -> style.withHoverEvent(new HoverEvent.ShowText(text2)));
										return new DeathMessageS2CPacket(this.getId(), text3);
									}
							)
					);
			AbstractTeam abstractTeam = this.getScoreboardTeam();
			if (abstractTeam == null
					|| abstractTeam.getDeathMessageVisibilityRule() == AbstractTeam.VisibilityRule.ALWAYS) {
				this.server.getPlayerManager().broadcast(text, false);
			}
			else if (abstractTeam.getDeathMessageVisibilityRule() == AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS) {
				this.server.getPlayerManager().sendToTeam(this, text);
			}
			else if (abstractTeam.getDeathMessageVisibilityRule() == AbstractTeam.VisibilityRule.HIDE_FOR_OWN_TEAM) {
				this.server.getPlayerManager().sendToOtherTeams(this, text);
			}
		}
		else {
			this.networkHandler.sendPacket(new DeathMessageS2CPacket(this.getId(), ScreenTexts.EMPTY));
		}

		this.dropShoulderEntities();
		if (this.getEntityWorld().getGameRules().getValue(GameRules.FORGIVE_DEAD_PLAYERS)) {
			this.forgiveMobAnger();
		}

		if (!this.isSpectator()) {
			this.drop(this.getEntityWorld(), damageSource);
		}

		this
				.getEntityWorld()
				.getScoreboard()
				.forEachScore(ScoreboardCriterion.DEATH_COUNT, this, ScoreAccess::incrementScore);
		LivingEntity livingEntity = this.getPrimeAdversary();
		if (livingEntity != null) {
			this.incrementStat(Stats.KILLED_BY.getOrCreateStat(livingEntity.getType()));
			livingEntity.updateKilledAdvancementCriterion(this, damageSource);
			this.onKilledBy(livingEntity);
		}

		this.getEntityWorld().sendEntityStatus(this, (byte) 3);
		this.incrementStat(Stats.DEATHS);
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
		this.extinguish();
		this.setFrozenTicks(0);
		this.setOnFire(false);
		this.getDamageTracker().update();
		this.setLastDeathPos(Optional.of(GlobalPos.create(this.getEntityWorld().getRegistryKey(), this.getBlockPos())));
		this.networkHandler.markAsDead();
	}

	private void forgiveMobAnger() {
		Box box = new Box(this.getBlockPos()).expand(32.0, 10.0, 32.0);
		this.getEntityWorld()
		    .getEntitiesByClass(MobEntity.class, box, EntityPredicates.EXCEPT_SPECTATOR)
		    .stream()
		    .filter(entity -> entity instanceof Angerable)
		    .forEach(entity -> ((Angerable) entity).forgive(this.getEntityWorld(), this));
	}

	@Override
	public void updateKilledAdvancementCriterion(Entity entityKilled, DamageSource damageSource) {
		if (entityKilled != this) {
			super.updateKilledAdvancementCriterion(entityKilled, damageSource);
			Scoreboard scoreboard = this.getEntityWorld().getScoreboard();
			scoreboard.forEachScore(ScoreboardCriterion.TOTAL_KILL_COUNT, this, ScoreAccess::incrementScore);
			if (entityKilled instanceof PlayerEntity) {
				this.incrementStat(Stats.PLAYER_KILLS);
				scoreboard.forEachScore(ScoreboardCriterion.PLAYER_KILL_COUNT, this, ScoreAccess::incrementScore);
			}
			else {
				this.incrementStat(Stats.MOB_KILLS);
			}

			this.updateScoreboardScore(this, entityKilled, ScoreboardCriterion.TEAM_KILLS);
			this.updateScoreboardScore(entityKilled, this, ScoreboardCriterion.KILLED_BY_TEAMS);
			Criteria.PLAYER_KILLED_ENTITY.trigger(this, entityKilled, damageSource);
		}
	}

	private void updateScoreboardScore(
			ScoreHolder targetScoreHolder,
			ScoreHolder aboutScoreHolder,
			ScoreboardCriterion[] criterions
	) {
		Scoreboard scoreboard = this.getEntityWorld().getScoreboard();
		Team team = scoreboard.getScoreHolderTeam(aboutScoreHolder.getNameForScoreboard());
		if (team != null) {
			int i = team.getColor().getColorIndex();
			if (i >= 0 && i < criterions.length) {
				scoreboard.forEachScore(criterions[i], targetScoreHolder, ScoreAccess::incrementScore);
			}
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isInvulnerableTo(world, source)) {
			return false;
		}
		else {
			Entity entity = source.getAttacker();
			if (entity instanceof PlayerEntity playerEntity && !this.shouldDamagePlayer(playerEntity)) {
				return false;
			}
			else {
				return entity instanceof PersistentProjectileEntity persistentProjectileEntity
						       && persistentProjectileEntity.getOwner() instanceof PlayerEntity playerEntity2
						       && !this.shouldDamagePlayer(playerEntity2)
				       ? false
				       : super.damage(world, source, amount);
			}
		}
	}

	@Override
	public boolean shouldDamagePlayer(PlayerEntity player) {
		return !this.isPvpEnabled() ? false : super.shouldDamagePlayer(player);
	}

	private boolean isPvpEnabled() {
		return this.getEntityWorld().isPvpEnabled();
	}

	public TeleportTarget getRespawnTarget(
			boolean alive,
			TeleportTarget.PostDimensionTransition postDimensionTransition
	) {
		ServerPlayerEntity.Respawn respawn = this.getRespawn();
		ServerWorld serverWorld = this.server.getWorld(ServerPlayerEntity.Respawn.getDimension(respawn));
		if (serverWorld != null && respawn != null) {
			Optional<ServerPlayerEntity.RespawnPos> optional = findRespawnPosition(serverWorld, respawn, alive);
			if (optional.isPresent()) {
				ServerPlayerEntity.RespawnPos respawnPos = optional.get();
				return new TeleportTarget(
						serverWorld,
						respawnPos.pos(),
						Vec3d.ZERO,
						respawnPos.yaw(),
						respawnPos.pitch(),
						postDimensionTransition
				);
			}
			else {
				return TeleportTarget.missingSpawnBlock(this, postDimensionTransition);
			}
		}
		else {
			return TeleportTarget.noRespawnPointSet(this, postDimensionTransition);
		}
	}

	public boolean canReceiveWaypoints() {
		return this.getAttributeValue(EntityAttributes.WAYPOINT_RECEIVE_RANGE) > 0.0;
	}

	@Override
	protected void updateAttribute(RegistryEntry<EntityAttribute> attribute) {
		if (attribute.matches(EntityAttributes.WAYPOINT_RECEIVE_RANGE)) {
			ServerWaypointHandler serverWaypointHandler = this.getEntityWorld().getWaypointHandler();
			if (this.getAttributes().getValue(attribute) > 0.0) {
				serverWaypointHandler.addPlayer(this);
			}
			else {
				serverWaypointHandler.removePlayer(this);
			}
		}

		super.updateAttribute(attribute);
	}

	private static Optional<ServerPlayerEntity.RespawnPos> findRespawnPosition(
			ServerWorld world,
			ServerPlayerEntity.Respawn respawn,
			boolean bl
	) {
		WorldProperties.SpawnPoint spawnPoint = respawn.respawnData;
		BlockPos blockPos = spawnPoint.getPos();
		float f = spawnPoint.yaw();
		float g = spawnPoint.pitch();
		boolean bl2 = respawn.forced;
		BlockState blockState = world.getBlockState(blockPos);
		Block block = blockState.getBlock();
		if (block instanceof RespawnAnchorBlock && (bl2 || blockState.get(RespawnAnchorBlock.CHARGES) > 0)
				&& RespawnAnchorBlock.isUsable(world, blockPos)) {
			Optional<Vec3d> optional = RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, world, blockPos);
			if (!bl2 && bl && optional.isPresent()) {
				world.setBlockState(
						blockPos,
						blockState.with(RespawnAnchorBlock.CHARGES, blockState.get(RespawnAnchorBlock.CHARGES) - 1),
						3
				);
			}

			return optional.map(respawnPos -> ServerPlayerEntity.RespawnPos.fromCurrentPos(respawnPos, blockPos, 0.0F));
		}
		else if (block instanceof BedBlock
				&& world
				.getEnvironmentAttributes()
				.getAttributeValue(EnvironmentAttributes.BED_RULE_GAMEPLAY, blockPos)
				.canSetSpawn(world)) {
			return BedBlock.findWakeUpPosition(EntityType.PLAYER, world, blockPos, blockState.get(BedBlock.FACING), f)
			               .map(respawnPos -> ServerPlayerEntity.RespawnPos.fromCurrentPos(respawnPos, blockPos, 0.0F));
		}
		else if (!bl2) {
			return Optional.empty();
		}
		else {
			boolean bl3 = block.canMobSpawnInside(blockState);
			BlockState blockState2 = world.getBlockState(blockPos.up());
			boolean bl4 = blockState2.getBlock().canMobSpawnInside(blockState2);
			return bl3 && bl4
			       ? Optional.of(new ServerPlayerEntity.RespawnPos(
					new Vec3d(
							blockPos.getX() + 0.5,
							blockPos.getY() + 0.1,
							blockPos.getZ() + 0.5
					), f, g
			))
			       : Optional.empty();
		}
	}

	public void detachForDimensionChange() {
		this.detach();
		this.getEntityWorld().removePlayer(this, Entity.RemovalReason.CHANGED_DIMENSION);
		if (!this.notInAnyWorld) {
			this.notInAnyWorld = true;
			this.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_WON, 0.0F));
			this.seenCredits = true;
		}
	}

	public @Nullable ServerPlayerEntity teleportTo(TeleportTarget teleportTarget) {
		if (this.isRemoved()) {
			return null;
		}
		else {
			if (teleportTarget.missingRespawnBlock()) {
				this.networkHandler.sendPacket(new GameStateChangeS2CPacket(
						GameStateChangeS2CPacket.NO_RESPAWN_BLOCK,
						0.0F
				));
			}

			ServerWorld serverWorld = teleportTarget.world();
			ServerWorld serverWorld2 = this.getEntityWorld();
			RegistryKey<World> registryKey = serverWorld2.getRegistryKey();
			if (teleportTarget.asPassenger() == false) {
				this.dismountVehicle();
			}

			if (serverWorld.getRegistryKey() == registryKey) {
				this.networkHandler.requestTeleport(
						EntityPosition.fromTeleportTarget(teleportTarget),
						teleportTarget.relatives()
				);
				this.networkHandler.syncWithPlayerPosition();
				teleportTarget.postTeleportTransition().onTransition(this);
				return this;
			}
			else {
				this.inTeleportationState = true;
				WorldProperties worldProperties = serverWorld.getLevelProperties();
				this.networkHandler.sendPacket(new PlayerRespawnS2CPacket(
						this.createCommonPlayerSpawnInfo(serverWorld),
						(byte) 3
				));
				this.networkHandler.sendPacket(new DifficultyS2CPacket(
						worldProperties.getDifficulty(),
						worldProperties.isDifficultyLocked()
				));
				PlayerManager playerManager = this.server.getPlayerManager();
				playerManager.sendCommandTree(this);
				serverWorld2.removePlayer(this, Entity.RemovalReason.CHANGED_DIMENSION);
				this.unsetRemoved();
				Profiler profiler = Profilers.get();
				profiler.push("moving");
				if (registryKey == World.OVERWORLD && serverWorld.getRegistryKey() == World.NETHER) {
					this.enteredNetherPos = this.getEntityPos();
				}

				profiler.pop();
				profiler.push("placing");
				this.setServerWorld(serverWorld);
				this.networkHandler.requestTeleport(
						EntityPosition.fromTeleportTarget(teleportTarget),
						teleportTarget.relatives()
				);
				this.networkHandler.syncWithPlayerPosition();
				serverWorld.onDimensionChanged(this);
				profiler.pop();
				this.worldChanged(serverWorld2);
				this.clearActiveItem();
				this.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(this.getAbilities()));
				playerManager.sendWorldInfo(this, serverWorld);
				playerManager.sendPlayerStatus(this);
				playerManager.sendStatusEffects(this);
				teleportTarget.postTeleportTransition().onTransition(this);
				this.syncedExperience = -1;
				this.syncedHealth = -1.0F;
				this.syncedFoodLevel = -1;
				this.teleportSpectatingPlayers(teleportTarget, serverWorld2);
				return this;
			}
		}
	}

	@Override
	public void rotate(float yaw, boolean relativeYaw, float pitch, boolean relativePitch) {
		super.rotate(yaw, relativeYaw, pitch, relativePitch);
		this.networkHandler.sendPacket(new PlayerRotationS2CPacket(yaw, relativeYaw, pitch, relativePitch));
	}

	private void worldChanged(ServerWorld origin) {
		RegistryKey<World> registryKey = origin.getRegistryKey();
		RegistryKey<World> registryKey2 = this.getEntityWorld().getRegistryKey();
		Criteria.CHANGED_DIMENSION.trigger(this, registryKey, registryKey2);
		if (registryKey == World.NETHER && registryKey2 == World.OVERWORLD && this.enteredNetherPos != null) {
			Criteria.NETHER_TRAVEL.trigger(this, this.enteredNetherPos);
		}

		if (registryKey2 != World.NETHER) {
			this.enteredNetherPos = null;
		}
	}

	@Override
	public boolean canBeSpectated(ServerPlayerEntity spectator) {
		if (spectator.isSpectator()) {
			return this.getCameraEntity() == this;
		}
		else {
			return this.isSpectator() ? false : super.canBeSpectated(spectator);
		}
	}

	@Override
	public void sendPickup(Entity item, int count) {
		super.sendPickup(item, count);
		this.currentScreenHandler.sendContentUpdates();
	}

	@Override
	public Either<PlayerEntity.SleepFailureReason, Unit> trySleep(BlockPos pos) {
		Direction direction = this.getEntityWorld().getBlockState(pos).get(HorizontalFacingBlock.FACING);
		if (!this.isSleeping() && this.isAlive()) {
			BedRule
					bedRule =
					this
							.getEntityWorld()
							.getEnvironmentAttributes()
							.getAttributeValue(EnvironmentAttributes.BED_RULE_GAMEPLAY, pos);
			boolean bl = bedRule.canSleep(this.getEntityWorld());
			boolean bl2 = bedRule.canSetSpawn(this.getEntityWorld());
			if (!bl2 && !bl) {
				return Either.left(bedRule.getFailureReason());
			}
			else if (!this.isBedWithinRange(pos, direction)) {
				return Either.left(PlayerEntity.SleepFailureReason.TOO_FAR_AWAY);
			}
			else if (this.isBedObstructed(pos, direction)) {
				return Either.left(PlayerEntity.SleepFailureReason.OBSTRUCTED);
			}
			else {
				if (bl2) {
					this.setSpawnPoint(
							new ServerPlayerEntity.Respawn(
									WorldProperties.SpawnPoint.create(
											this.getEntityWorld().getRegistryKey(),
											pos,
											this.getYaw(),
											this.getPitch()
									), false
							),
							true
					);
				}

				if (!bl) {
					return Either.left(bedRule.getFailureReason());
				}
				else {
					if (!this.isCreative()) {
						double d = 8.0;
						double e = 5.0;
						Vec3d vec3d = Vec3d.ofBottomCenter(pos);
						List<HostileEntity> list = this.getEntityWorld()
						                               .getEntitiesByClass(
								                               HostileEntity.class,
								                               new Box(
										                               vec3d.getX() - 8.0,
										                               vec3d.getY() - 5.0,
										                               vec3d.getZ() - 8.0,
										                               vec3d.getX() + 8.0,
										                               vec3d.getY() + 5.0,
										                               vec3d.getZ() + 8.0
								                               ),
								                               entity -> entity.isAngryAt(this.getEntityWorld(), this)
						                               );
						if (!list.isEmpty()) {
							return Either.left(PlayerEntity.SleepFailureReason.NOT_SAFE);
						}
					}

					Either<PlayerEntity.SleepFailureReason, Unit> either = super.trySleep(pos).ifRight(unit -> {
						this.incrementStat(Stats.SLEEP_IN_BED);
						Criteria.SLEPT_IN_BED.trigger(this);
					});
					if (!this.getEntityWorld().isSleepingEnabled()) {
						this.sendMessage(Text.translatable("sleep.not_possible"), true);
					}

					this.getEntityWorld().updateSleepingPlayers();
					return either;
				}
			}
		}
		else {
			return Either.left(PlayerEntity.SleepFailureReason.OTHER);
		}
	}

	@Override
	public void sleep(BlockPos pos) {
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
		super.sleep(pos);
	}

	private boolean isBedWithinRange(BlockPos pos, Direction direction) {
		return this.isBedWithinRange(pos) || this.isBedWithinRange(pos.offset(direction.getOpposite()));
	}

	private boolean isBedWithinRange(BlockPos pos) {
		Vec3d vec3d = Vec3d.ofBottomCenter(pos);
		return Math.abs(this.getX() - vec3d.getX()) <= 3.0 && Math.abs(this.getY() - vec3d.getY()) <= 2.0
				&& Math.abs(this.getZ() - vec3d.getZ()) <= 3.0;
	}

	private boolean isBedObstructed(BlockPos pos, Direction direction) {
		BlockPos blockPos = pos.up();
		return !this.doesNotSuffocate(blockPos) || !this.doesNotSuffocate(blockPos.offset(direction.getOpposite()));
	}

	@Override
	public void wakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers) {
		if (this.isSleeping()) {
			this.getEntityWorld().getChunkManager().sendToNearbyPlayers(this, new EntityAnimationS2CPacket(this, 2));
		}

		super.wakeUp(skipSleepTimer, updateSleepingPlayers);
		if (this.networkHandler != null) {
			this.networkHandler.requestTeleport(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
		}
	}

	@Override
	public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
		return super.isInvulnerableTo(world, source)
				|| this.isInTeleportationState() && !source.isOf(DamageTypes.ENDER_PEARL)
				|| !this.networkHandler.canInteractWithGame();
	}

	@Override
	protected void applyMovementEffects(ServerWorld world, BlockPos pos) {
		if (!this.isSpectator()) {
			super.applyMovementEffects(world, pos);
		}
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
		if (this.spawnExtraParticlesOnFall && onGround && this.fallDistance > 0.0) {
			Vec3d vec3d = landedPosition.toCenterPos().add(0.0, 0.5, 0.0);
			int i = (int) MathHelper.clamp(50.0 * this.fallDistance, 0.0, 200.0);
			this
					.getEntityWorld()
					.spawnParticles(
							new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
							vec3d.x,
							vec3d.y,
							vec3d.z,
							i,
							0.3F,
							0.3F,
							0.3F,
							0.15F
					);
			this.spawnExtraParticlesOnFall = false;
		}

		super.fall(heightDifference, onGround, state, landedPosition);
	}

	@Override
	public void onExplodedBy(@Nullable Entity entity) {
		super.onExplodedBy(entity);
		this.currentExplosionImpactPos = this.getEntityPos();
		this.explodedBy = entity;
		this.setIgnoreFallDamageFromCurrentExplosion(entity != null && entity.getType() == EntityType.WIND_CHARGE);
	}

	@Override
	protected void tickCramming() {
		if (this.getEntityWorld().getTickManager().shouldTick()) {
			super.tickCramming();
		}
	}

	@Override
	public void openEditSignScreen(SignBlockEntity sign, boolean front) {
		this.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.getEntityWorld(), sign.getPos()));
		this.networkHandler.sendPacket(new SignEditorOpenS2CPacket(sign.getPos(), front));
	}

	@Override
	public void openDialog(RegistryEntry<Dialog> dialog) {
		this.networkHandler.sendPacket(new ShowDialogS2CPacket(dialog));
	}

	private void incrementScreenHandlerSyncId() {
		this.screenHandlerSyncId = this.screenHandlerSyncId % 100 + 1;
	}

	@Override
	public OptionalInt openHandledScreen(@Nullable NamedScreenHandlerFactory factory) {
		if (factory == null) {
			return OptionalInt.empty();
		}
		else {
			if (this.currentScreenHandler != this.playerScreenHandler) {
				this.closeHandledScreen();
			}

			this.incrementScreenHandlerSyncId();
			ScreenHandler screenHandler = factory.createMenu(this.screenHandlerSyncId, this.getInventory(), this);
			if (screenHandler == null) {
				if (this.isSpectator()) {
					this.sendMessage(Text.translatable("container.spectatorCantOpen").formatted(Formatting.RED), true);
				}

				return OptionalInt.empty();
			}
			else {
				this.networkHandler.sendPacket(new OpenScreenS2CPacket(
						screenHandler.syncId,
						screenHandler.getType(),
						factory.getDisplayName()
				));
				this.onScreenHandlerOpened(screenHandler);
				this.currentScreenHandler = screenHandler;
				return OptionalInt.of(this.screenHandlerSyncId);
			}
		}
	}

	@Override
	public void sendTradeOffers(
			int syncId,
			TradeOfferList offers,
			int levelProgress,
			int experience,
			boolean leveled,
			boolean refreshable
	) {
		this.networkHandler.sendPacket(new SetTradeOffersS2CPacket(
				syncId,
				offers,
				levelProgress,
				experience,
				leveled,
				refreshable
		));
	}

	@Override
	public void openHorseInventory(AbstractHorseEntity horse, Inventory inventory) {
		if (this.currentScreenHandler != this.playerScreenHandler) {
			this.closeHandledScreen();
		}

		this.incrementScreenHandlerSyncId();
		int i = horse.getInventoryColumns();
		this.networkHandler.sendPacket(new OpenMountScreenS2CPacket(this.screenHandlerSyncId, i, horse.getId()));
		this.currentScreenHandler =
				new HorseScreenHandler(this.screenHandlerSyncId, this.getInventory(), inventory, horse, i);
		this.onScreenHandlerOpened(this.currentScreenHandler);
	}

	@Override
	public void openNautilusInventory(AbstractNautilusEntity nautilus, Inventory inventory) {
		if (this.currentScreenHandler != this.playerScreenHandler) {
			this.closeHandledScreen();
		}

		this.incrementScreenHandlerSyncId();
		int i = nautilus.getInventoryColumns();
		this.networkHandler.sendPacket(new OpenMountScreenS2CPacket(this.screenHandlerSyncId, i, nautilus.getId()));
		this.currentScreenHandler =
				new NautilusScreenHandler(this.screenHandlerSyncId, this.getInventory(), inventory, nautilus, i);
		this.onScreenHandlerOpened(this.currentScreenHandler);
	}

	@Override
	public void useBook(ItemStack book, Hand hand) {
		if (book.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
			if (WrittenBookContentComponent.resolveInStack(book, this.getCommandSource(), this)) {
				this.currentScreenHandler.sendContentUpdates();
			}

			this.networkHandler.sendPacket(new OpenWrittenBookS2CPacket(hand));
		}
	}

	@Override
	public void openCommandBlockScreen(CommandBlockBlockEntity commandBlock) {
		this.networkHandler.sendPacket(BlockEntityUpdateS2CPacket.create(
				commandBlock,
				BlockEntity::createComponentlessNbt
		));
	}

	@Override
	public void closeHandledScreen() {
		this.networkHandler.sendPacket(new CloseScreenS2CPacket(this.currentScreenHandler.syncId));
		this.onHandledScreenClosed();
	}

	@Override
	public void onHandledScreenClosed() {
		this.currentScreenHandler.onClosed(this);
		this.playerScreenHandler.copySharedSlots(this.currentScreenHandler);
		this.currentScreenHandler = this.playerScreenHandler;
	}

	@Override
	public void tickRiding() {
		double d = this.getX();
		double e = this.getY();
		double f = this.getZ();
		super.tickRiding();
		this.increaseRidingMotionStats(this.getX() - d, this.getY() - e, this.getZ() - f);
	}

	public void increaseTravelMotionStats(double deltaX, double deltaY, double deltaZ) {
		if (!this.hasVehicle() && !isZero(deltaX, deltaY, deltaZ)) {
			if (this.isSwimming()) {
				int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 100.0F);
				if (i > 0) {
					this.increaseStat(Stats.SWIM_ONE_CM, i);
					this.addExhaustion(0.01F * i * 0.01F);
				}
			}
			else if (this.isSubmergedIn(FluidTags.WATER)) {
				int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 100.0F);
				if (i > 0) {
					this.increaseStat(Stats.WALK_UNDER_WATER_ONE_CM, i);
					this.addExhaustion(0.01F * i * 0.01F);
				}
			}
			else if (this.isTouchingWater()) {
				int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 100.0F);
				if (i > 0) {
					this.increaseStat(Stats.WALK_ON_WATER_ONE_CM, i);
					this.addExhaustion(0.01F * i * 0.01F);
				}
			}
			else if (this.isClimbing()) {
				if (deltaY > 0.0) {
					this.increaseStat(Stats.CLIMB_ONE_CM, (int) Math.round(deltaY * 100.0));
				}
			}
			else if (this.isOnGround()) {
				int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 100.0F);
				if (i > 0) {
					if (this.isSprinting()) {
						this.increaseStat(Stats.SPRINT_ONE_CM, i);
						this.addExhaustion(0.1F * i * 0.01F);
					}
					else if (this.isInSneakingPose()) {
						this.increaseStat(Stats.CROUCH_ONE_CM, i);
						this.addExhaustion(0.0F * i * 0.01F);
					}
					else {
						this.increaseStat(Stats.WALK_ONE_CM, i);
						this.addExhaustion(0.0F * i * 0.01F);
					}
				}
			}
			else if (this.isGliding()) {
				int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 100.0F);
				this.increaseStat(Stats.AVIATE_ONE_CM, i);
			}
			else {
				int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 100.0F);
				if (i > 25) {
					this.increaseStat(Stats.FLY_ONE_CM, i);
				}
			}
		}
	}

	private void increaseRidingMotionStats(double deltaX, double deltaY, double deltaZ) {
		if (this.hasVehicle() && !isZero(deltaX, deltaY, deltaZ)) {
			int i = Math.round((float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 100.0F);
			Entity entity = this.getVehicle();
			if (entity instanceof AbstractMinecartEntity) {
				this.increaseStat(Stats.MINECART_ONE_CM, i);
			}
			else if (entity instanceof AbstractBoatEntity) {
				this.increaseStat(Stats.BOAT_ONE_CM, i);
			}
			else if (entity instanceof PigEntity) {
				this.increaseStat(Stats.PIG_ONE_CM, i);
			}
			else if (entity instanceof AbstractHorseEntity) {
				this.increaseStat(Stats.HORSE_ONE_CM, i);
			}
			else if (entity instanceof StriderEntity) {
				this.increaseStat(Stats.STRIDER_ONE_CM, i);
			}
			else if (entity instanceof HappyGhastEntity) {
				this.increaseStat(Stats.HAPPY_GHAST_ONE_CM, i);
			}
			else if (entity instanceof AbstractNautilusEntity) {
				this.increaseStat(Stats.NAUTILUS_ONE_CM, i);
			}
		}
	}

	private static boolean isZero(double deltaX, double deltaY, double deltaZ) {
		return deltaX == 0.0 && deltaY == 0.0 && deltaZ == 0.0;
	}

	@Override
	public void increaseStat(Stat<?> stat, int amount) {
		this.statHandler.increaseStat(this, stat, amount);
		this.getEntityWorld().getScoreboard().forEachScore(stat, this, score -> score.incrementScore(amount));
	}

	@Override
	public void resetStat(Stat<?> stat) {
		this.statHandler.setStat(this, stat, 0);
		this.getEntityWorld().getScoreboard().forEachScore(stat, this, ScoreAccess::resetScore);
	}

	@Override
	public int unlockRecipes(Collection<RecipeEntry<?>> recipes) {
		return this.recipeBook.unlockRecipes(recipes, this);
	}

	@Override
	public void onRecipeCrafted(RecipeEntry<?> recipe, List<ItemStack> ingredients) {
		Criteria.RECIPE_CRAFTED.trigger(this, recipe.id(), ingredients);
	}

	@Override
	public void unlockRecipes(List<RegistryKey<Recipe<?>>> recipes) {
		List<RecipeEntry<?>> list = recipes.stream()
		                                   .flatMap(recipeKey -> this.server
				                                   .getRecipeManager()
				                                   .get((RegistryKey<Recipe<?>>) recipeKey)
				                                   .stream())
		                                   .collect(Collectors.toList());
		this.unlockRecipes(list);
	}

	@Override
	public int lockRecipes(Collection<RecipeEntry<?>> recipes) {
		return this.recipeBook.lockRecipes(recipes, this);
	}

	@Override
	public void jump() {
		super.jump();
		this.incrementStat(Stats.JUMP);
		if (this.isSprinting()) {
			this.addExhaustion(0.2F);
		}
		else {
			this.addExhaustion(0.05F);
		}
	}

	@Override
	public void addExperience(int experience) {
		if (experience != 0) {
			super.addExperience(experience);
			this.syncedExperience = -1;
		}
	}

	public void onDisconnect() {
		this.disconnected = true;
		this.removeAllPassengers();
		if (this.isSleeping()) {
			this.wakeUp(true, false);
		}
	}

	public boolean isDisconnected() {
		return this.disconnected;
	}

	public void markHealthDirty() {
		this.syncedHealth = -1.0E8F;
	}

	@Override
	public void sendMessage(Text message, boolean overlay) {
		this.sendMessageToClient(message, overlay);
	}

	@Override
	protected void consumeItem() {
		if (!this.activeItemStack.isEmpty() && this.isUsingItem()) {
			this.networkHandler.sendPacket(new EntityStatusS2CPacket(this, (byte) 9));
			super.consumeItem();
		}
	}

	@Override
	public void lookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
		super.lookAt(anchorPoint, target);
		this.networkHandler.sendPacket(new LookAtS2CPacket(anchorPoint, target.x, target.y, target.z));
	}

	public void lookAtEntity(
			EntityAnchorArgumentType.EntityAnchor anchorPoint,
			Entity targetEntity,
			EntityAnchorArgumentType.EntityAnchor targetAnchor
	) {
		Vec3d vec3d = targetAnchor.positionAt(targetEntity);
		super.lookAt(anchorPoint, vec3d);
		this.networkHandler.sendPacket(new LookAtS2CPacket(anchorPoint, targetEntity, targetAnchor));
	}

	public void copyFrom(ServerPlayerEntity oldPlayer, boolean alive) {
		this.sculkShriekerWarningManager = oldPlayer.sculkShriekerWarningManager;
		this.session = oldPlayer.session;
		this.interactionManager.setGameMode(
				oldPlayer.interactionManager.getGameMode(),
				oldPlayer.interactionManager.getPreviousGameMode()
		);
		this.sendAbilitiesUpdate();
		this.getAttributes().setBaseFrom(oldPlayer.getAttributes());
		if (alive) {
			this.getAttributes().addPersistentModifiersFrom(oldPlayer.getAttributes());
			this.setHealth(oldPlayer.getHealth());
			this.hungerManager = oldPlayer.hungerManager;

			for (StatusEffectInstance statusEffectInstance : oldPlayer.getStatusEffects()) {
				this.addStatusEffect(new StatusEffectInstance(statusEffectInstance));
			}

			this.copyInventoryAndExperience(oldPlayer);
			this.portalManager = oldPlayer.portalManager;
		}
		else {
			this.setHealth(this.getMaxHealth());
			if (this.getEntityWorld().getGameRules().getValue(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator()) {
				this.copyInventoryAndExperience(oldPlayer);
			}
		}

		this.enchantingTableSeed = oldPlayer.enchantingTableSeed;
		this.enderChestInventory = oldPlayer.enderChestInventory;
		this
				.getDataTracker()
				.set(PLAYER_MODE_CUSTOMIZATION_ID, oldPlayer.getDataTracker().get(PLAYER_MODE_CUSTOMIZATION_ID));
		this.syncedExperience = -1;
		this.syncedHealth = -1.0F;
		this.syncedFoodLevel = -1;
		this.recipeBook.copyFrom(oldPlayer.recipeBook);
		this.seenCredits = oldPlayer.seenCredits;
		this.enteredNetherPos = oldPlayer.enteredNetherPos;
		this.chunkFilter = oldPlayer.chunkFilter;
		this.subscribedTypes = oldPlayer.subscribedTypes;
		this.setLeftShoulderNbt(oldPlayer.getLeftShoulderNbt());
		this.setRightShoulderNbt(oldPlayer.getRightShoulderNbt());
		this.setLastDeathPos(oldPlayer.getLastDeathPos());
		this.getWaypointConfig().copyFrom(oldPlayer.getWaypointConfig());
	}

	private void copyInventoryAndExperience(PlayerEntity playerEntity) {
		this.getInventory().clone(playerEntity.getInventory());
		this.experienceLevel = playerEntity.experienceLevel;
		this.totalExperience = playerEntity.totalExperience;
		this.experienceProgress = playerEntity.experienceProgress;
		this.setScore(playerEntity.getScore());
	}

	@Override
	protected void onStatusEffectApplied(StatusEffectInstance effect, @Nullable Entity source) {
		super.onStatusEffectApplied(effect, source);
		this.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(this.getId(), effect, true));
		if (effect.equals(StatusEffects.LEVITATION)) {
			this.levitationStartTick = this.age;
			this.levitationStartPos = this.getEntityPos();
		}

		Criteria.EFFECTS_CHANGED.trigger(this, source);
	}

	@Override
	protected void onStatusEffectUpgraded(StatusEffectInstance effect, boolean reapplyEffect, @Nullable Entity source) {
		super.onStatusEffectUpgraded(effect, reapplyEffect, source);
		this.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(this.getId(), effect, false));
		Criteria.EFFECTS_CHANGED.trigger(this, source);
	}

	@Override
	protected void onStatusEffectsRemoved(Collection<StatusEffectInstance> effects) {
		super.onStatusEffectsRemoved(effects);

		for (StatusEffectInstance statusEffectInstance : effects) {
			this.networkHandler.sendPacket(new RemoveEntityStatusEffectS2CPacket(
					this.getId(),
					statusEffectInstance.getEffectType()
			));
			if (statusEffectInstance.equals(StatusEffects.LEVITATION)) {
				this.levitationStartPos = null;
			}
		}

		Criteria.EFFECTS_CHANGED.trigger(this, null);
	}

	@Override
	public void requestTeleport(double destX, double destY, double destZ) {
		this.networkHandler
				.requestTeleport(
						new EntityPosition(new Vec3d(destX, destY, destZ), Vec3d.ZERO, 0.0F, 0.0F),
						PositionFlag.combine(PositionFlag.DELTA, PositionFlag.ROT)
				);
	}

	@Override
	public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ) {
		this.networkHandler.requestTeleport(
				new EntityPosition(
						new Vec3d(offsetX, offsetY, offsetZ),
						Vec3d.ZERO,
						0.0F,
						0.0F
				), PositionFlag.VALUES
		);
	}

	@Override
	public boolean teleport(
			ServerWorld world,
			double destX,
			double destY,
			double destZ,
			Set<PositionFlag> flags,
			float yaw,
			float pitch,
			boolean resetCamera
	) {
		if (this.isSleeping()) {
			this.wakeUp(true, true);
		}

		if (resetCamera) {
			this.setCameraEntity(this);
		}

		boolean bl = super.teleport(world, destX, destY, destZ, flags, yaw, pitch, resetCamera);
		if (bl) {
			this.setHeadYaw(flags.contains(PositionFlag.Y_ROT) ? this.getHeadYaw() + yaw : yaw);
			this.networkHandler.resetFloatingTicks();
		}

		return bl;
	}

	@Override
	public void refreshPositionAfterTeleport(double x, double y, double z) {
		super.refreshPositionAfterTeleport(x, y, z);
		this.networkHandler.syncWithPlayerPosition();
	}

	@Override
	public void addCritParticles(Entity target) {
		this.getEntityWorld().getChunkManager().sendToNearbyPlayers(this, new EntityAnimationS2CPacket(target, 4));
	}

	@Override
	public void addEnchantedHitParticles(Entity target) {
		this.getEntityWorld().getChunkManager().sendToNearbyPlayers(this, new EntityAnimationS2CPacket(target, 5));
	}

	@Override
	public void sendAbilitiesUpdate() {
		if (this.networkHandler != null) {
			this.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(this.getAbilities()));
			this.updatePotionVisibility();
		}
	}

	public ServerWorld getEntityWorld() {
		return (ServerWorld) super.getEntityWorld();
	}

	public boolean changeGameMode(GameMode gameMode) {
		boolean bl = this.isSpectator();
		if (!this.interactionManager.changeGameMode(gameMode)) {
			return false;
		}
		else {
			this.networkHandler.sendPacket(new GameStateChangeS2CPacket(
					GameStateChangeS2CPacket.GAME_MODE_CHANGED,
					gameMode.getIndex()
			));
			if (gameMode == GameMode.SPECTATOR) {
				this.dropShoulderEntities();
				this.stopRiding();
				this.clearActiveItem();
				EnchantmentHelper.removeLocationBasedEffects(this);
			}
			else {
				this.setCameraEntity(this);
				if (bl) {
					EnchantmentHelper.applyLocationBasedEffects(this.getEntityWorld(), this);
				}
			}

			this.sendAbilitiesUpdate();
			this.markEffectsDirty();
			return true;
		}
	}

	@Override
	public GameMode getGameMode() {
		return this.interactionManager.getGameMode();
	}

	public CommandOutput getCommandOutput() {
		return this.commandOutput;
	}

	public ServerCommandSource getCommandSource() {
		return new ServerCommandSource(
				this.getCommandOutput(),
				this.getEntityPos(),
				this.getRotationClient(),
				this.getEntityWorld(),
				this.getPermissions(),
				this.getStringifiedName(),
				this.getDisplayName(),
				this.server,
				this
		);
	}

	public void sendMessage(Text message) {
		this.sendMessageToClient(message, false);
	}

	public void sendMessageToClient(Text message, boolean overlay) {
		if (this.acceptsMessage(overlay)) {
			this.networkHandler.send(
					new GameMessageS2CPacket(message, overlay), PacketCallbacks.of(() -> {
						if (this.acceptsMessage(false)) {
							int i = 256;
							String string = message.asTruncatedString(256);
							Text text2 = Text.literal(string).formatted(Formatting.YELLOW);
							return new GameMessageS2CPacket(
									Text
											.translatable("multiplayer.message_not_delivered", text2)
											.formatted(Formatting.RED), false
							);
						}
						else {
							return null;
						}
					})
			);
		}
	}

	public void sendChatMessage(SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params) {
		if (this.acceptsChatMessage()) {
			message.send(this, filterMaskEnabled, params);
		}
	}

	public String getIp() {
		return this.networkHandler.getConnectionAddress() instanceof InetSocketAddress inetSocketAddress
		       ? InetAddresses.toAddrString(inetSocketAddress.getAddress())
		       : "<unknown>";
	}

	public void setClientOptions(SyncedClientOptions clientOptions) {
		this.language = clientOptions.language();
		this.viewDistance = clientOptions.viewDistance();
		this.clientChatVisibility = clientOptions.chatVisibility();
		this.clientChatColorsEnabled = clientOptions.chatColorsEnabled();
		this.filterText = clientOptions.filtersText();
		this.allowServerListing = clientOptions.allowsServerListing();
		this.particlesMode = clientOptions.particleStatus();
		this.getDataTracker().set(PLAYER_MODE_CUSTOMIZATION_ID, (byte) clientOptions.playerModelParts());
		this.getDataTracker().set(MAIN_ARM_ID, clientOptions.mainArm());
	}

	public SyncedClientOptions getClientOptions() {
		int i = this.getDataTracker().get(PLAYER_MODE_CUSTOMIZATION_ID);
		return new SyncedClientOptions(
				this.language,
				this.viewDistance,
				this.clientChatVisibility,
				this.clientChatColorsEnabled,
				i,
				this.getMainArm(),
				this.filterText,
				this.allowServerListing,
				this.particlesMode
		);
	}

	public boolean areClientChatColorsEnabled() {
		return this.clientChatColorsEnabled;
	}

	public ChatVisibility getClientChatVisibility() {
		return this.clientChatVisibility;
	}

	private boolean acceptsMessage(boolean overlay) {
		return this.clientChatVisibility == ChatVisibility.HIDDEN ? overlay : true;
	}

	private boolean acceptsChatMessage() {
		return this.clientChatVisibility == ChatVisibility.FULL;
	}

	public int getViewDistance() {
		return this.viewDistance;
	}

	public void sendServerMetadata(ServerMetadata metadata) {
		this.networkHandler.sendPacket(new ServerMetadataS2CPacket(
				metadata.description(),
				metadata.favicon().map(ServerMetadata.Favicon::iconBytes)
		));
	}

	@Override
	public PermissionPredicate getPermissions() {
		return this.server.getPermissionLevel(this.getPlayerConfigEntry());
	}

	public void updateLastActionTime() {
		this.lastActionTime = Util.getMeasuringTimeMs();
	}

	public ServerStatHandler getStatHandler() {
		return this.statHandler;
	}

	public ServerRecipeBook getRecipeBook() {
		return this.recipeBook;
	}

	@Override
	protected void updatePotionVisibility() {
		if (this.isSpectator()) {
			this.clearPotionSwirls();
			this.setInvisible(true);
		}
		else {
			super.updatePotionVisibility();
		}
	}

	public Entity getCameraEntity() {
		return (Entity) (this.cameraEntity == null ? this : this.cameraEntity);
	}

	public void setCameraEntity(@Nullable Entity entity) {
		Entity entity2 = this.getCameraEntity();
		this.cameraEntity = (Entity) (entity == null ? this : entity);
		if (entity2 != this.cameraEntity) {
			if (this.cameraEntity.getEntityWorld() instanceof ServerWorld serverWorld) {
				this.teleport(
						serverWorld,
						this.cameraEntity.getX(),
						this.cameraEntity.getY(),
						this.cameraEntity.getZ(),
						Set.of(),
						this.getYaw(),
						this.getPitch(),
						false
				);
			}

			if (entity != null) {
				this.getEntityWorld().getChunkManager().updatePosition(this);
			}

			this.networkHandler.sendPacket(new SetCameraEntityS2CPacket(this.cameraEntity));
			this.networkHandler.syncWithPlayerPosition();
		}
	}

	@Override
	protected void tickPortalCooldown() {
		if (!this.inTeleportationState) {
			super.tickPortalCooldown();
		}
	}

	@Override
	public void attack(Entity target) {
		if (this.isSpectator()) {
			this.setCameraEntity(target);
		}
		else {
			super.attack(target);
		}
	}

	public long getLastActionTime() {
		return this.lastActionTime;
	}

	public @Nullable Text getPlayerListName() {
		return null;
	}

	public int getPlayerListOrder() {
		return 0;
	}

	@Override
	public void swingHand(Hand hand) {
		super.swingHand(hand);
		this.resetTicksSince();
	}

	public boolean isInTeleportationState() {
		return this.inTeleportationState;
	}

	public void onTeleportationDone() {
		this.inTeleportationState = false;
	}

	public PlayerAdvancementTracker getAdvancementTracker() {
		return this.advancementTracker;
	}

	public ServerPlayerEntity.@Nullable Respawn getRespawn() {
		return this.respawn;
	}

	public void setSpawnPointFrom(ServerPlayerEntity player) {
		this.setSpawnPoint(player.respawn, false);
	}

	public void setSpawnPoint(ServerPlayerEntity.@Nullable Respawn respawn, boolean sendMessage) {
		if (sendMessage && respawn != null && !respawn.posEquals(this.respawn)) {
			this.sendMessage(SET_SPAWN_TEXT);
		}

		this.respawn = respawn;
	}

	public ChunkSectionPos getWatchedSection() {
		return this.watchedSection;
	}

	public void setWatchedSection(ChunkSectionPos section) {
		this.watchedSection = section;
	}

	public ChunkFilter getChunkFilter() {
		return this.chunkFilter;
	}

	public void setChunkFilter(ChunkFilter chunkFilter) {
		this.chunkFilter = chunkFilter;
	}

	@Override
	public ItemEntity dropItem(ItemStack stack, boolean dropAtSelf, boolean retainOwnership) {
		ItemEntity itemEntity = super.dropItem(stack, dropAtSelf, retainOwnership);
		if (retainOwnership) {
			ItemStack itemStack = itemEntity != null ? itemEntity.getStack() : ItemStack.EMPTY;
			if (!itemStack.isEmpty()) {
				this.increaseStat(Stats.DROPPED.getOrCreateStat(itemStack.getItem()), stack.getCount());
				this.incrementStat(Stats.DROP);
			}
		}

		return itemEntity;
	}

	public TextStream getTextStream() {
		return this.textStream;
	}

	public void setServerWorld(ServerWorld world) {
		this.setWorld(world);
		this.interactionManager.setWorld(world);
	}

	private static @Nullable GameMode gameModeFromData(ReadView view, String key) {
		return view.<GameMode>read(key, GameMode.INDEX_CODEC).orElse(null);
	}

	private GameMode getServerGameMode(@Nullable GameMode backupGameMode) {
		GameMode gameMode = this.server.getForcedGameMode();
		if (gameMode != null) {
			return gameMode;
		}
		else {
			return backupGameMode != null ? backupGameMode : this.server.getDefaultGameMode();
		}
	}

	private void writeGameModeData(WriteView view) {
		view.put("playerGameType", GameMode.INDEX_CODEC, this.interactionManager.getGameMode());
		GameMode gameMode = this.interactionManager.getPreviousGameMode();
		view.putNullable("previousPlayerGameType", GameMode.INDEX_CODEC, gameMode);
	}

	@Override
	public boolean shouldFilterText() {
		return this.filterText;
	}

	public boolean shouldFilterMessagesSentTo(ServerPlayerEntity player) {
		return player == this ? false : this.filterText || player.filterText;
	}

	@Override
	public boolean canModifyAt(ServerWorld world, BlockPos pos) {
		return super.canModifyAt(world, pos) && world.canEntityModifyAt(this, pos);
	}

	@Override
	protected void tickItemStackUsage(ItemStack stack) {
		Criteria.USING_ITEM.trigger(this, stack);
		super.tickItemStackUsage(stack);
	}

	public void dropSelectedItem(boolean entireStack) {
		PlayerInventory playerInventory = this.getInventory();
		ItemStack itemStack = playerInventory.dropSelectedItem(entireStack);
		this.currentScreenHandler
				.getSlotIndex(playerInventory, playerInventory.getSelectedSlot())
				.ifPresent(index -> this.currentScreenHandler.setReceivedStack(
						index,
						playerInventory.getSelectedStack()
				));
		if (this.activeItemStack.isEmpty()) {
			this.clearActiveItem();
		}

		this.dropItem(itemStack, false, true);
	}

	@Override
	public void giveOrDropStack(ItemStack stack) {
		if (!this.getInventory().insertStack(stack)) {
			this.dropItem(stack, false);
		}
	}

	public boolean allowsServerListing() {
		return this.allowServerListing;
	}

	@Override
	public Optional<SculkShriekerWarningManager> getSculkShriekerWarningManager() {
		return Optional.of(this.sculkShriekerWarningManager);
	}

	public void setSpawnExtraParticlesOnFall(boolean spawnExtraParticlesOnFall) {
		this.spawnExtraParticlesOnFall = spawnExtraParticlesOnFall;
	}

	@Override
	public void triggerItemPickedUpByEntityCriteria(ItemEntity item) {
		super.triggerItemPickedUpByEntityCriteria(item);
		Entity entity = item.getOwner();
		if (entity != null) {
			Criteria.THROWN_ITEM_PICKED_UP_BY_PLAYER.trigger(this, item.getStack(), entity);
		}
	}

	public void setSession(PublicPlayerSession session) {
		this.session = session;
	}

	public @Nullable PublicPlayerSession getSession() {
		return this.session != null && this.session.isKeyExpired() ? null : this.session;
	}

	@Override
	public void tiltScreen(double deltaX, double deltaZ) {
		this.damageTiltYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0F / (float) Math.PI - this.getYaw());
		this.networkHandler.sendPacket(new DamageTiltS2CPacket(this));
	}

	@Override
	public boolean startRiding(Entity entity, boolean force, boolean emitEvent) {
		if (super.startRiding(entity, force, emitEvent)) {
			entity.updatePassengerPosition(this);
			this.networkHandler.requestTeleport(
					new EntityPosition(this.getEntityPos(), Vec3d.ZERO, 0.0F, 0.0F),
					PositionFlag.ROT
			);
			if (entity instanceof LivingEntity livingEntity) {
				this.server.getPlayerManager().sendStatusEffects(livingEntity, this.networkHandler);
			}

			this.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void dismountVehicle() {
		Entity entity = this.getVehicle();
		super.dismountVehicle();
		if (entity instanceof LivingEntity livingEntity) {
			for (StatusEffectInstance statusEffectInstance : livingEntity.getStatusEffects()) {
				this.networkHandler.sendPacket(new RemoveEntityStatusEffectS2CPacket(
						entity.getId(),
						statusEffectInstance.getEffectType()
				));
			}
		}

		if (entity != null) {
			this.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
		}
	}

	public CommonPlayerSpawnInfo createCommonPlayerSpawnInfo(ServerWorld world) {
		return new CommonPlayerSpawnInfo(
				world.getDimensionEntry(),
				world.getRegistryKey(),
				BiomeAccess.hashSeed(world.getSeed()),
				this.interactionManager.getGameMode(),
				this.interactionManager.getPreviousGameMode(),
				world.isDebugWorld(),
				world.isFlat(),
				this.getLastDeathPos(),
				this.getPortalCooldown(),
				world.getSeaLevel()
		);
	}

	public void setStartRaidPos(BlockPos startRaidPos) {
		this.startRaidPos = startRaidPos;
	}

	public void clearStartRaidPos() {
		this.startRaidPos = null;
	}

	public @Nullable BlockPos getStartRaidPos() {
		return this.startRaidPos;
	}

	@Override
	public Vec3d getMovement() {
		Entity entity = this.getVehicle();
		return entity != null && entity.getControllingPassenger() != this ? entity.getMovement() : this.movement;
	}

	@Override
	public Vec3d getKineticAttackMovement() {
		Entity entity = this.getVehicle();
		return entity != null && entity.getControllingPassenger() != this ? entity.getKineticAttackMovement()
		                                                                  : this.movement;
	}

	public void setMovement(Vec3d movement) {
		this.movement = movement;
	}

	@Override
	protected float getDamageAgainst(Entity target, float baseDamage, DamageSource damageSource) {
		return EnchantmentHelper.getDamage(
				this.getEntityWorld(),
				this.getWeaponStack(),
				target,
				damageSource,
				baseDamage
		);
	}

	@Override
	public void sendEquipmentBreakStatus(Item item, EquipmentSlot slot) {
		super.sendEquipmentBreakStatus(item, slot);
		this.incrementStat(Stats.BROKEN.getOrCreateStat(item));
	}

	public PlayerInput getPlayerInput() {
		return this.playerInput;
	}

	public void setPlayerInput(PlayerInput playerInput) {
		this.playerInput = playerInput;
	}

	public Vec3d getInputVelocityForMinecart() {
		float f = this.playerInput.left() == this.playerInput.right() ? 0.0F : (this.playerInput.left() ? 1.0F : -1.0F);
		float
				g =
				this.playerInput.forward() == this.playerInput.backward() ? 0.0F
				                                                          : (this.playerInput.forward() ? 1.0F : -1.0F);
		return movementInputToVelocity(new Vec3d(f, 0.0, g), 1.0F, this.getYaw());
	}

	public void addEnderPearl(EnderPearlEntity enderPearl) {
		this.enderPearls.add(enderPearl);
	}

	public void removeEnderPearl(EnderPearlEntity enderPearl) {
		this.enderPearls.remove(enderPearl);
	}

	public Set<EnderPearlEntity> getEnderPearls() {
		return this.enderPearls;
	}

	public NbtCompound getLeftShoulderNbt() {
		return this.leftShoulderNbt;
	}

	protected void setLeftShoulderNbt(NbtCompound leftShoulderNbt) {
		this.leftShoulderNbt = leftShoulderNbt;
		this.setLeftShoulderParrotVariant(readParrotVariant(leftShoulderNbt));
	}

	public NbtCompound getRightShoulderNbt() {
		return this.rightShoulderNbt;
	}

	protected void setRightShoulderNbt(NbtCompound rightShoulderNbt) {
		this.rightShoulderNbt = rightShoulderNbt;
		this.setRightShoulderParrotVariant(readParrotVariant(rightShoulderNbt));
	}

	public long handleThrownEnderPearl(EnderPearlEntity enderPearl) {
		if (enderPearl.getEntityWorld() instanceof ServerWorld serverWorld) {
			ChunkPos chunkPos = enderPearl.getChunkPos();
			this.addEnderPearl(enderPearl);
			serverWorld.resetIdleTimeout();
			return addEnderPearlTicket(serverWorld, chunkPos) - 1L;
		}
		else {
			return 0L;
		}
	}

	public static long addEnderPearlTicket(ServerWorld world, ChunkPos chunkPos) {
		world.getChunkManager().addTicket(ChunkTicketType.ENDER_PEARL, chunkPos, 2);
		return ChunkTicketType.ENDER_PEARL.expiryTicks();
	}

	public void setSubscribedTypes(Set<DebugSubscriptionType<?>> subscribedTypes) {
		this.subscribedTypes = Set.copyOf(subscribedTypes);
	}

	public Set<DebugSubscriptionType<?>> getSubscribedTypes() {
		return !this.server.getSubscriberTracker().canSubscribe(this) ? Set.of() : this.subscribedTypes;
	}

	/**
	 * {@code Respawn}.
	 */
	public record Respawn(WorldProperties.SpawnPoint respawnData, boolean forced) {

		public static final Codec<ServerPlayerEntity.Respawn> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    WorldProperties.SpawnPoint.MAP_CODEC.forGetter(ServerPlayerEntity.Respawn::respawnData),
						                    Codec.BOOL.optionalFieldOf("forced", false).forGetter(ServerPlayerEntity.Respawn::forced)
				                    )
				                    .apply(instance, ServerPlayerEntity.Respawn::new)
		);

		static RegistryKey<World> getDimension(ServerPlayerEntity.@Nullable Respawn respawn) {
			return respawn != null ? respawn.respawnData().getDimension() : World.OVERWORLD;
		}

		public boolean posEquals(ServerPlayerEntity.@Nullable Respawn respawn) {
			return respawn != null && this.respawnData.globalPos().equals(respawn.respawnData.globalPos());
		}
	}

	/**
	 * {@code RespawnPos}.
	 */
	record RespawnPos(Vec3d pos, float yaw, float pitch) {

		public static ServerPlayerEntity.RespawnPos fromCurrentPos(Vec3d respawnPos, BlockPos currentPos, float f) {
			return new ServerPlayerEntity.RespawnPos(respawnPos, getYaw(respawnPos, currentPos), f);
		}

		private static float getYaw(Vec3d respawnPos, BlockPos currentPos) {
			Vec3d vec3d = Vec3d.ofBottomCenter(currentPos).subtract(respawnPos).normalize();
			return (float) MathHelper.wrapDegrees(MathHelper.atan2(vec3d.z, vec3d.x) * 180.0F / (float) Math.PI - 90.0);
		}
	}

	/**
	 * {@code SavePos}.
	 */
	public record SavePos(Optional<RegistryKey<World>> dimension, Optional<Vec3d> position, Optional<Vec2f> rotation) {

		public static final MapCodec<ServerPlayerEntity.SavePos> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    World.CODEC.optionalFieldOf("Dimension").forGetter(ServerPlayerEntity.SavePos::dimension),
						                    Vec3d.CODEC.optionalFieldOf("Pos").forGetter(ServerPlayerEntity.SavePos::position),
						                    Vec2f.CODEC.optionalFieldOf("Rotation").forGetter(ServerPlayerEntity.SavePos::rotation)
				                    )
				                    .apply(instance, ServerPlayerEntity.SavePos::new)
		);
		public static final ServerPlayerEntity.SavePos
				EMPTY =
				new ServerPlayerEntity.SavePos(Optional.empty(), Optional.empty(), Optional.empty());
	}
}
