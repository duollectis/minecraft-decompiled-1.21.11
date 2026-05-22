package net.minecraft.network.state;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.PacketDecoderException;
import net.minecraft.network.handler.PacketEncoderException;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.*;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.common.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;

/**
 * Фабрики состояний протокола для фазы {@link net.minecraft.network.NetworkPhase#PLAY}.
 * Содержит регистрацию всех пакетов клиент→сервер ({@link #C2S}) и сервер→клиент ({@link #S2C}),
 * а также модификатор кодека для {@link CreativeInventoryActionC2SPacket},
 * который разрешён только в творческом режиме.
 */
public class PlayStateFactories {

	/**
	 * Модификатор кодека {@link CreativeInventoryActionC2SPacket}: отклоняет пакет,
	 * если игрок не находится в творческом режиме, выбрасывая исключение декодера/энкодера.
	 */
	public static final PacketCodecModifier<RegistryByteBuf, CreativeInventoryActionC2SPacket, PlayStateFactories.PacketCodecModifierContext>
			CREATIVE_INVENTORY_ACTION_C2S_MODIFIER = (packetCodec, context) ->
			new PacketCodec<>() {
				@Override
				public CreativeInventoryActionC2SPacket decode(RegistryByteBuf buf) {
					if (!context.isInCreativeMode()) {
						throw new PacketDecoderException("Not in creative mode");
					}

					return (CreativeInventoryActionC2SPacket) packetCodec.decode(buf);
				}

				@Override
				public void encode(RegistryByteBuf buf, CreativeInventoryActionC2SPacket packet) {
					if (!context.isInCreativeMode()) {
						throw new PacketEncoderException("Not in creative mode");
					}

					packetCodec.encode(buf, packet);
				}
			};

	public static final ContextAwareNetworkStateFactory<ServerPlayPacketListener, RegistryByteBuf, PlayStateFactories.PacketCodecModifierContext>
			C2S = NetworkStateBuilder.contextAwareC2S(
			NetworkPhase.PLAY,
			builder -> builder
					.add(PlayPackets.ACCEPT_TELEPORTATION, TeleportConfirmC2SPacket.CODEC)
					.add(PlayPackets.BLOCK_ENTITY_TAG_QUERY, QueryBlockNbtC2SPacket.CODEC)
					.add(PlayPackets.BUNDLE_ITEM_SELECTED, BundleItemSelectedC2SPacket.CODEC)
					.add(PlayPackets.CHANGE_DIFFICULTY_C2S, UpdateDifficultyC2SPacket.CODEC)
					.add(PlayPackets.CHANGE_GAME_MODE, ChangeGameModeC2SPacket.CODEC)
					.add(PlayPackets.CHAT_ACK, MessageAcknowledgmentC2SPacket.CODEC)
					.add(PlayPackets.CHAT_COMMAND, CommandExecutionC2SPacket.CODEC)
					.add(PlayPackets.CHAT_COMMAND_SIGNED, ChatCommandSignedC2SPacket.CODEC)
					.add(PlayPackets.CHAT, ChatMessageC2SPacket.CODEC)
					.add(PlayPackets.CHAT_SESSION_UPDATE, PlayerSessionC2SPacket.CODEC)
					.add(PlayPackets.CHUNK_BATCH_RECEIVED, AcknowledgeChunksC2SPacket.CODEC)
					.add(PlayPackets.CLIENT_COMMAND, ClientStatusC2SPacket.CODEC)
					.add(PlayPackets.CLIENT_TICK_END, ClientTickEndC2SPacket.CODEC)
					.add(CommonPackets.CLIENT_INFORMATION, ClientOptionsC2SPacket.CODEC)
					.add(PlayPackets.COMMAND_SUGGESTION, RequestCommandCompletionsC2SPacket.CODEC)
					.add(PlayPackets.CONFIGURATION_ACKNOWLEDGED, AcknowledgeReconfigurationC2SPacket.CODEC)
					.add(PlayPackets.CONTAINER_BUTTON_CLICK, ButtonClickC2SPacket.CODEC)
					.add(PlayPackets.CONTAINER_CLICK, ClickSlotC2SPacket.CODEC)
					.add(PlayPackets.CONTAINER_CLOSE_C2S, CloseHandledScreenC2SPacket.CODEC)
					.add(PlayPackets.CONTAINER_SLOT_STATE_CHANGED, SlotChangedStateC2SPacket.CODEC)
					.add(CookiePackets.COOKIE_RESPONSE, CookieResponseC2SPacket.CODEC)
					.add(CommonPackets.CUSTOM_PAYLOAD_C2S, CustomPayloadC2SPacket.CODEC)
					.add(PlayPackets.DEBUG_SUBSCRIPTION_REQUEST, DebugSubscriptionRequestC2SPacket.CODEC)
					.add(PlayPackets.EDIT_BOOK, BookUpdateC2SPacket.CODEC)
					.add(PlayPackets.ENTITY_TAG_QUERY, QueryEntityNbtC2SPacket.CODEC)
					.add(PlayPackets.INTERACT, PlayerInteractEntityC2SPacket.CODEC)
					.add(PlayPackets.JIGSAW_GENERATE, JigsawGeneratingC2SPacket.CODEC)
					.add(CommonPackets.KEEP_ALIVE_C2S, KeepAliveC2SPacket.CODEC)
					.add(PlayPackets.LOCK_DIFFICULTY, UpdateDifficultyLockC2SPacket.CODEC)
					.add(PlayPackets.MOVE_PLAYER_POS, PlayerMoveC2SPacket.PositionAndOnGround.CODEC)
					.add(PlayPackets.MOVE_PLAYER_POS_ROT, PlayerMoveC2SPacket.Full.CODEC)
					.add(PlayPackets.MOVE_PLAYER_ROT, PlayerMoveC2SPacket.LookAndOnGround.CODEC)
					.add(PlayPackets.MOVE_PLAYER_STATUS_ONLY, PlayerMoveC2SPacket.OnGroundOnly.CODEC)
					.add(PlayPackets.MOVE_VEHICLE_C2S, VehicleMoveC2SPacket.CODEC)
					.add(PlayPackets.PADDLE_BOAT, BoatPaddleStateC2SPacket.CODEC)
					.add(PlayPackets.PICK_ITEM_FROM_BLOCK, PickItemFromBlockC2SPacket.CODEC)
					.add(PlayPackets.PICK_ITEM_FROM_ENTITY, PickItemFromEntityC2SPacket.CODEC)
					.add(PingPackets.PING_REQUEST, QueryPingC2SPacket.CODEC)
					.add(PlayPackets.PLACE_RECIPE, CraftRequestC2SPacket.CODEC)
					.add(PlayPackets.PLAYER_ABILITIES_C2S, UpdatePlayerAbilitiesC2SPacket.CODEC)
					.add(PlayPackets.PLAYER_ACTION, PlayerActionC2SPacket.CODEC)
					.add(PlayPackets.PLAYER_COMMAND, ClientCommandC2SPacket.CODEC)
					.add(PlayPackets.PLAYER_INPUT, PlayerInputC2SPacket.CODEC)
					.add(PlayPackets.PLAYER_LOADED, PlayerLoadedC2SPacket.CODEC)
					.add(CommonPackets.PONG, CommonPongC2SPacket.CODEC)
					.add(PlayPackets.RECIPE_BOOK_CHANGE_SETTINGS, RecipeCategoryOptionsC2SPacket.CODEC)
					.add(PlayPackets.RECIPE_BOOK_SEEN_RECIPE, RecipeBookDataC2SPacket.CODEC)
					.add(PlayPackets.RENAME_ITEM, RenameItemC2SPacket.CODEC)
					.add(CommonPackets.RESOURCE_PACK, ResourcePackStatusC2SPacket.CODEC)
					.add(PlayPackets.SEEN_ADVANCEMENTS, AdvancementTabC2SPacket.CODEC)
					.add(PlayPackets.SELECT_TRADE, SelectMerchantTradeC2SPacket.CODEC)
					.add(PlayPackets.SET_BEACON, UpdateBeaconC2SPacket.CODEC)
					.add(PlayPackets.SET_CARRIED_ITEM_C2S, UpdateSelectedSlotC2SPacket.CODEC)
					.add(PlayPackets.SET_COMMAND_BLOCK, UpdateCommandBlockC2SPacket.CODEC)
					.add(PlayPackets.SET_COMMAND_MINECART, UpdateCommandBlockMinecartC2SPacket.CODEC)
					.add(
							PlayPackets.SET_CREATIVE_MODE_SLOT,
							CreativeInventoryActionC2SPacket.CODEC,
							CREATIVE_INVENTORY_ACTION_C2S_MODIFIER
					)
					.add(PlayPackets.SET_JIGSAW_BLOCK, UpdateJigsawC2SPacket.CODEC)
					.add(PlayPackets.SET_STRUCTURE_BLOCK, UpdateStructureBlockC2SPacket.CODEC)
					.add(PlayPackets.SET_TEST_BLOCK, SetTestBlockC2SPacket.CODEC)
					.add(PlayPackets.SIGN_UPDATE, UpdateSignC2SPacket.CODEC)
					.add(PlayPackets.SWING, HandSwingC2SPacket.CODEC)
					.add(PlayPackets.TELEPORT_TO_ENTITY, SpectatorTeleportC2SPacket.CODEC)
					.add(PlayPackets.TEST_INSTANCE_BLOCK_ACTION, TestInstanceBlockActionC2SPacket.CODEC)
					.add(PlayPackets.USE_ITEM_ON, PlayerInteractBlockC2SPacket.CODEC)
					.add(PlayPackets.USE_ITEM, PlayerInteractItemC2SPacket.CODEC)
					.add(CommonPackets.CUSTOM_CLICK_ACTION, CustomClickActionC2SPacket.CODEC)
	);

	public static final NetworkStateFactory<ClientPlayPacketListener, RegistryByteBuf> S2C = NetworkStateBuilder.s2c(
			NetworkPhase.PLAY,
			builder -> builder
					.addBundle(PlayPackets.BUNDLE, BundleS2CPacket::new, new BundleDelimiterS2CPacket())
					.add(PlayPackets.ADD_ENTITY, EntitySpawnS2CPacket.CODEC)
					.add(PlayPackets.ANIMATE, EntityAnimationS2CPacket.CODEC)
					.add(PlayPackets.AWARD_STATS, StatisticsS2CPacket.CODEC)
					.add(PlayPackets.BLOCK_CHANGED_ACK, PlayerActionResponseS2CPacket.CODEC)
					.add(PlayPackets.BLOCK_DESTRUCTION, BlockBreakingProgressS2CPacket.CODEC)
					.add(PlayPackets.BLOCK_ENTITY_DATA, BlockEntityUpdateS2CPacket.CODEC)
					.add(PlayPackets.BLOCK_EVENT, BlockEventS2CPacket.CODEC)
					.add(PlayPackets.BLOCK_UPDATE, BlockUpdateS2CPacket.CODEC)
					.add(PlayPackets.BOSS_EVENT, BossBarS2CPacket.CODEC)
					.add(PlayPackets.CHANGE_DIFFICULTY_S2C, DifficultyS2CPacket.CODEC)
					.add(PlayPackets.CHUNK_BATCH_FINISHED, ChunkSentS2CPacket.CODEC)
					.add(PlayPackets.CHUNK_BATCH_START, StartChunkSendS2CPacket.CODEC)
					.add(PlayPackets.CHUNKS_BIOMES, ChunkBiomeDataS2CPacket.CODEC)
					.add(PlayPackets.CLEAR_TITLES, ClearTitleS2CPacket.CODEC)
					.add(PlayPackets.COMMAND_SUGGESTIONS, CommandSuggestionsS2CPacket.CODEC)
					.add(PlayPackets.COMMANDS, CommandTreeS2CPacket.CODEC)
					.add(PlayPackets.CONTAINER_CLOSE_S2C, CloseScreenS2CPacket.CODEC)
					.add(PlayPackets.CONTAINER_SET_CONTENT, InventoryS2CPacket.CODEC)
					.add(PlayPackets.CONTAINER_SET_DATA, ScreenHandlerPropertyUpdateS2CPacket.CODEC)
					.add(PlayPackets.CONTAINER_SET_SLOT, ScreenHandlerSlotUpdateS2CPacket.CODEC)
					.add(CookiePackets.COOKIE_REQUEST, CookieRequestS2CPacket.CODEC)
					.add(PlayPackets.COOLDOWN, CooldownUpdateS2CPacket.CODEC)
					.add(PlayPackets.CUSTOM_CHAT_COMPLETIONS, ChatSuggestionsS2CPacket.CODEC)
					.add(CommonPackets.CUSTOM_PAYLOAD_S2C, CustomPayloadS2CPacket.PLAY_CODEC)
					.add(PlayPackets.DAMAGE_EVENT, EntityDamageS2CPacket.CODEC)
					.add(PlayPackets.BLOCK_VALUE_DEBUG, BlockValueDebugS2CPacket.PACKET_CODEC)
					.add(PlayPackets.CHUNK_VALUE_DEBUG, ChunkValueDebugS2CPacket.PACKET_CODEC)
					.add(PlayPackets.ENTITY_VALUE_DEBUG, EntityValueDebugS2CPacket.PACKET_CODEC)
					.add(PlayPackets.EVENT_DEBUG, EventDebugS2CPacket.PACKET_CODEC)
					.add(PlayPackets.DEBUG_SAMPLE, DebugSampleS2CPacket.CODEC)
					.add(PlayPackets.DELETE_CHAT, RemoveMessageS2CPacket.CODEC)
					.add(CommonPackets.DISCONNECT, DisconnectS2CPacket.CODEC)
					.add(PlayPackets.DISGUISED_CHAT, ProfilelessChatMessageS2CPacket.CODEC)
					.add(PlayPackets.ENTITY_EVENT, EntityStatusS2CPacket.CODEC)
					.add(PlayPackets.ENTITY_POSITION_SYNC, EntityPositionSyncS2CPacket.CODEC)
					.add(PlayPackets.EXPLODE, ExplosionS2CPacket.CODEC)
					.add(PlayPackets.FORGET_LEVEL_CHUNK, UnloadChunkS2CPacket.CODEC)
					.add(PlayPackets.GAME_EVENT, GameStateChangeS2CPacket.CODEC)
					.add(PlayPackets.GAME_TEST_HIGHLIGHT_POS, GameTestHighlightPosS2CPacket.PACKET_CODEC)
					.add(PlayPackets.MOUNT_SCREEN_OPEN, OpenMountScreenS2CPacket.CODEC)
					.add(PlayPackets.HURT_ANIMATION, DamageTiltS2CPacket.CODEC)
					.add(PlayPackets.INITIALIZE_BORDER, WorldBorderInitializeS2CPacket.CODEC)
					.add(CommonPackets.KEEP_ALIVE_S2C, KeepAliveS2CPacket.CODEC)
					.add(PlayPackets.LEVEL_CHUNK_WITH_LIGHT, ChunkDataS2CPacket.CODEC)
					.add(PlayPackets.LEVEL_EVENT, WorldEventS2CPacket.CODEC)
					.add(PlayPackets.LEVEL_PARTICLES, ParticleS2CPacket.CODEC)
					.add(PlayPackets.LIGHT_UPDATE, LightUpdateS2CPacket.CODEC)
					.add(PlayPackets.LOGIN, GameJoinS2CPacket.CODEC)
					.add(PlayPackets.MAP_ITEM_DATA, MapUpdateS2CPacket.CODEC)
					.add(PlayPackets.MERCHANT_OFFERS, SetTradeOffersS2CPacket.CODEC)
					.add(PlayPackets.MOVE_ENTITY_POS, EntityS2CPacket.MoveRelative.CODEC)
					.add(PlayPackets.MOVE_ENTITY_POS_ROT, EntityS2CPacket.RotateAndMoveRelative.CODEC)
					.add(PlayPackets.MOVE_MINECART_ALONG_TRACK, MoveMinecartAlongTrackS2CPacket.PACKET_CODEC)
					.add(PlayPackets.MOVE_ENTITY_ROT, EntityS2CPacket.Rotate.CODEC)
					.add(PlayPackets.MOVE_VEHICLE_S2C, VehicleMoveS2CPacket.CODEC)
					.add(PlayPackets.OPEN_BOOK, OpenWrittenBookS2CPacket.CODEC)
					.add(PlayPackets.OPEN_SCREEN, OpenScreenS2CPacket.CODEC)
					.add(PlayPackets.OPEN_SIGN_EDITOR, SignEditorOpenS2CPacket.CODEC)
					.add(CommonPackets.PING, CommonPingS2CPacket.CODEC)
					.add(PingPackets.PONG_RESPONSE, PingResultS2CPacket.CODEC)
					.add(PlayPackets.PLACE_GHOST_RECIPE, CraftFailedResponseS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_ABILITIES_S2C, PlayerAbilitiesS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_CHAT, ChatMessageS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_COMBAT_END, EndCombatS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_COMBAT_ENTER, EnterCombatS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_COMBAT_KILL, DeathMessageS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_INFO_REMOVE, PlayerRemoveS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_INFO_UPDATE, PlayerListS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_LOOK_AT, LookAtS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_POSITION, PlayerPositionLookS2CPacket.CODEC)
					.add(PlayPackets.PLAYER_ROTATION, PlayerRotationS2CPacket.CODEC)
					.add(PlayPackets.RECIPE_BOOK_ADD, RecipeBookAddS2CPacket.CODEC)
					.add(PlayPackets.RECIPE_BOOK_REMOVE, RecipeBookRemoveS2CPacket.CODEC)
					.add(PlayPackets.RECIPE_BOOK_SETTINGS, RecipeBookSettingsS2CPacket.CODEC)
					.add(PlayPackets.REMOVE_ENTITIES, EntitiesDestroyS2CPacket.CODEC)
					.add(PlayPackets.REMOVE_MOB_EFFECT, RemoveEntityStatusEffectS2CPacket.CODEC)
					.add(PlayPackets.RESET_SCORE, ScoreboardScoreResetS2CPacket.CODEC)
					.add(CommonPackets.RESOURCE_PACK_POP, ResourcePackRemoveS2CPacket.CODEC)
					.add(CommonPackets.RESOURCE_PACK_PUSH, ResourcePackSendS2CPacket.CODEC)
					.add(PlayPackets.RESPAWN, PlayerRespawnS2CPacket.CODEC)
					.add(PlayPackets.ROTATE_HEAD, EntitySetHeadYawS2CPacket.CODEC)
					.add(PlayPackets.SECTION_BLOCKS_UPDATE, ChunkDeltaUpdateS2CPacket.CODEC)
					.add(PlayPackets.SELECT_ADVANCEMENTS_TAB, SelectAdvancementTabS2CPacket.CODEC)
					.add(PlayPackets.SERVER_DATA, ServerMetadataS2CPacket.CODEC)
					.add(PlayPackets.SET_ACTION_BAR_TEXT, OverlayMessageS2CPacket.CODEC)
					.add(PlayPackets.SET_BORDER_CENTER, WorldBorderCenterChangedS2CPacket.CODEC)
					.add(PlayPackets.SET_BORDER_LERP_SIZE, WorldBorderInterpolateSizeS2CPacket.CODEC)
					.add(PlayPackets.SET_BORDER_SIZE, WorldBorderSizeChangedS2CPacket.CODEC)
					.add(PlayPackets.SET_BORDER_WARNING_DELAY, WorldBorderWarningTimeChangedS2CPacket.CODEC)
					.add(PlayPackets.SET_BORDER_WARNING_DISTANCE, WorldBorderWarningBlocksChangedS2CPacket.CODEC)
					.add(PlayPackets.SET_CAMERA, SetCameraEntityS2CPacket.CODEC)
					.add(PlayPackets.SET_CHUNK_CACHE_CENTER, ChunkRenderDistanceCenterS2CPacket.CODEC)
					.add(PlayPackets.SET_CHUNK_CACHE_RADIUS, ChunkLoadDistanceS2CPacket.CODEC)
					.add(PlayPackets.SET_CURSOR_ITEM, SetCursorItemS2CPacket.CODEC)
					.add(PlayPackets.SET_DEFAULT_SPAWN_POSITION, PlayerSpawnPositionS2CPacket.CODEC)
					.add(PlayPackets.SET_DISPLAY_OBJECTIVE, ScoreboardDisplayS2CPacket.CODEC)
					.add(PlayPackets.SET_ENTITY_DATA, EntityTrackerUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_ENTITY_LINK, EntityAttachS2CPacket.CODEC)
					.add(PlayPackets.SET_ENTITY_MOTION, EntityVelocityUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_EQUIPMENT, EntityEquipmentUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_EXPERIENCE, ExperienceBarUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_HEALTH, HealthUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_CARRIED_ITEM_S2C, UpdateSelectedSlotS2CPacket.CODEC)
					.add(PlayPackets.SET_OBJECTIVE, ScoreboardObjectiveUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_PASSENGERS, EntityPassengersSetS2CPacket.CODEC)
					.add(PlayPackets.SET_PLAYER_INVENTORY, SetPlayerInventoryS2CPacket.CODEC)
					.add(PlayPackets.SET_PLAYER_TEAM, TeamS2CPacket.CODEC)
					.add(PlayPackets.SET_SCORE, ScoreboardScoreUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_SIMULATION_DISTANCE, SimulationDistanceS2CPacket.CODEC)
					.add(PlayPackets.SET_SUBTITLE_TEXT, SubtitleS2CPacket.CODEC)
					.add(PlayPackets.SET_TIME, WorldTimeUpdateS2CPacket.CODEC)
					.add(PlayPackets.SET_TITLE_TEXT, TitleS2CPacket.CODEC)
					.add(PlayPackets.SET_TITLES_ANIMATION, TitleFadeS2CPacket.CODEC)
					.add(PlayPackets.SOUND_ENTITY, PlaySoundFromEntityS2CPacket.CODEC)
					.add(PlayPackets.SOUND, PlaySoundS2CPacket.CODEC)
					.add(PlayPackets.START_CONFIGURATION, EnterReconfigurationS2CPacket.CODEC)
					.add(PlayPackets.STOP_SOUND, StopSoundS2CPacket.CODEC)
					.add(CommonPackets.STORE_COOKIE, StoreCookieS2CPacket.CODEC)
					.add(PlayPackets.SYSTEM_CHAT, GameMessageS2CPacket.CODEC)
					.add(PlayPackets.TAB_LIST, PlayerListHeaderS2CPacket.CODEC)
					.add(PlayPackets.TAG_QUERY, NbtQueryResponseS2CPacket.CODEC)
					.add(PlayPackets.TAKE_ITEM_ENTITY, ItemPickupAnimationS2CPacket.CODEC)
					.add(PlayPackets.TELEPORT_ENTITY, EntityPositionS2CPacket.CODEC)
					.add(PlayPackets.TEST_INSTANCE_BLOCK_STATUS, TestInstanceBlockStatusS2CPacket.CODEC)
					.add(PlayPackets.TICKING_STATE, UpdateTickRateS2CPacket.CODEC)
					.add(PlayPackets.TICKING_STEP, TickStepS2CPacket.CODEC)
					.add(CommonPackets.TRANSFER, ServerTransferS2CPacket.CODEC)
					.add(PlayPackets.UPDATE_ADVANCEMENTS, AdvancementUpdateS2CPacket.CODEC)
					.add(PlayPackets.UPDATE_ATTRIBUTES, EntityAttributesS2CPacket.CODEC)
					.add(PlayPackets.UPDATE_MOB_EFFECT, EntityStatusEffectS2CPacket.CODEC)
					.add(PlayPackets.UPDATE_RECIPES, SynchronizeRecipesS2CPacket.CODEC)
					.add(CommonPackets.UPDATE_TAGS, SynchronizeTagsS2CPacket.CODEC)
					.add(PlayPackets.PROJECTILE_POWER, ProjectilePowerS2CPacket.CODEC)
					.add(CommonPackets.CUSTOM_REPORT_DETAILS, CustomReportDetailsS2CPacket.CODEC)
					.add(CommonPackets.SERVER_LINKS, ServerLinksS2CPacket.CODEC)
					.add(PlayPackets.WAYPOINT, WaypointS2CPacket.CODEC)
					.add(CommonPackets.CLEAR_DIALOG, ClearDialogS2CPacket.CODEC)
					.add(CommonPackets.SHOW_DIALOG, ShowDialogS2CPacket.REGISTRY_CODEC)
	);

	/**
	 * Контекст модификатора кодека для фазы PLAY.
	 * Предоставляет информацию о текущем режиме игры для валидации пакетов.
	 */
	public interface PacketCodecModifierContext {

		boolean isInCreativeMode();
	}
}
