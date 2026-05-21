package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.s2c.play.*;

/**
 * Интерфейс client play packet listener.
 */
public interface ClientPlayPacketListener extends ClientCommonPacketListener, ClientPingResultPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.PLAY;
	}

	void onEntitySpawn(EntitySpawnS2CPacket packet);

	void onScoreboardObjectiveUpdate(ScoreboardObjectiveUpdateS2CPacket packet);

	void onEntityAnimation(EntityAnimationS2CPacket packet);

	void onDamageTilt(DamageTiltS2CPacket packet);

	void onStatistics(StatisticsS2CPacket packet);

	void onRecipeBookAdd(RecipeBookAddS2CPacket packet);

	void onRecipeBookRemove(RecipeBookRemoveS2CPacket packet);

	void onRecipeBookSettings(RecipeBookSettingsS2CPacket packet);

	void onBlockBreakingProgress(BlockBreakingProgressS2CPacket packet);

	void onSignEditorOpen(SignEditorOpenS2CPacket packet);

	void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet);

	void onBlockEvent(BlockEventS2CPacket packet);

	void onBlockUpdate(BlockUpdateS2CPacket packet);

	void onGameMessage(GameMessageS2CPacket packet);

	void onChatMessage(ChatMessageS2CPacket packet);

	void onProfilelessChatMessage(ProfilelessChatMessageS2CPacket packet);

	void onRemoveMessage(RemoveMessageS2CPacket packet);

	void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet);

	void onMapUpdate(MapUpdateS2CPacket packet);

	void onCloseScreen(CloseScreenS2CPacket packet);

	void onInventory(InventoryS2CPacket packet);

	void onOpenMountScreen(OpenMountScreenS2CPacket packet);

	void onScreenHandlerPropertyUpdate(ScreenHandlerPropertyUpdateS2CPacket packet);

	void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet);

	void onEntityStatus(EntityStatusS2CPacket packet);

	void onEntityAttach(EntityAttachS2CPacket packet);

	void onEntityPassengersSet(EntityPassengersSetS2CPacket packet);

	void onExplosion(ExplosionS2CPacket packet);

	void onGameStateChange(GameStateChangeS2CPacket packet);

	void onChunkData(ChunkDataS2CPacket packet);

	void onChunkBiomeData(ChunkBiomeDataS2CPacket packet);

	void onUnloadChunk(UnloadChunkS2CPacket packet);

	void onWorldEvent(WorldEventS2CPacket packet);

	void onGameJoin(GameJoinS2CPacket packet);

	void onEntity(EntityS2CPacket packet);

	void onMoveMinecartAlongTrack(MoveMinecartAlongTrackS2CPacket packet);

	void onPlayerPositionLook(PlayerPositionLookS2CPacket packet);

	void onPlayerRotation(PlayerRotationS2CPacket packet);

	void onParticle(ParticleS2CPacket packet);

	void onPlayerAbilities(PlayerAbilitiesS2CPacket packet);

	void onPlayerRemove(PlayerRemoveS2CPacket packet);

	void onPlayerList(PlayerListS2CPacket packet);

	void onEntitiesDestroy(EntitiesDestroyS2CPacket packet);

	void onRemoveEntityStatusEffect(RemoveEntityStatusEffectS2CPacket packet);

	void onPlayerRespawn(PlayerRespawnS2CPacket packet);

	void onEntitySetHeadYaw(EntitySetHeadYawS2CPacket packet);

	void onUpdateSelectedSlot(UpdateSelectedSlotS2CPacket packet);

	void onScoreboardDisplay(ScoreboardDisplayS2CPacket packet);

	void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet);

	void onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet);

	void onEntityEquipmentUpdate(EntityEquipmentUpdateS2CPacket packet);

	void onExperienceBarUpdate(ExperienceBarUpdateS2CPacket packet);

	void onHealthUpdate(HealthUpdateS2CPacket packet);

	void onTeam(TeamS2CPacket packet);

	void onScoreboardScoreUpdate(ScoreboardScoreUpdateS2CPacket packet);

	void onScoreboardScoreReset(ScoreboardScoreResetS2CPacket packet);

	void onPlayerSpawnPosition(PlayerSpawnPositionS2CPacket packet);

	void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet);

	void onPlaySound(PlaySoundS2CPacket packet);

	void onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet);

	void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet);

	void onEntityPositionSync(EntityPositionSyncS2CPacket packet);

	void onEntityPosition(EntityPositionS2CPacket packet);

	void onUpdateTickRate(UpdateTickRateS2CPacket packet);

	void onTickStep(TickStepS2CPacket packet);

	void onEntityAttributes(EntityAttributesS2CPacket packet);

	void onEntityStatusEffect(EntityStatusEffectS2CPacket packet);

	void onEndCombat(EndCombatS2CPacket packet);

	void onEnterCombat(EnterCombatS2CPacket packet);

	void onDeathMessage(DeathMessageS2CPacket packet);

	void onDifficulty(DifficultyS2CPacket packet);

	void onSetCameraEntity(SetCameraEntityS2CPacket packet);

	void onWorldBorderInitialize(WorldBorderInitializeS2CPacket packet);

	void onWorldBorderInterpolateSize(WorldBorderInterpolateSizeS2CPacket packet);

	void onWorldBorderSizeChanged(WorldBorderSizeChangedS2CPacket packet);

	void onWorldBorderWarningTimeChanged(WorldBorderWarningTimeChangedS2CPacket packet);

	void onWorldBorderWarningBlocksChanged(WorldBorderWarningBlocksChangedS2CPacket packet);

	void onWorldBorderCenterChanged(WorldBorderCenterChangedS2CPacket packet);

	void onPlayerListHeader(PlayerListHeaderS2CPacket packet);

	void onBossBar(BossBarS2CPacket packet);

	void onCooldownUpdate(CooldownUpdateS2CPacket packet);

	void onVehicleMove(VehicleMoveS2CPacket packet);

	void onAdvancements(AdvancementUpdateS2CPacket packet);

	void onSelectAdvancementTab(SelectAdvancementTabS2CPacket packet);

	void onCraftFailedResponse(CraftFailedResponseS2CPacket packet);

	void onCommandTree(CommandTreeS2CPacket packet);

	void onStopSound(StopSoundS2CPacket packet);

	void onCommandSuggestions(CommandSuggestionsS2CPacket packet);

	void onSynchronizeRecipes(SynchronizeRecipesS2CPacket packet);

	void onLookAt(LookAtS2CPacket packet);

	void onNbtQueryResponse(NbtQueryResponseS2CPacket packet);

	void onLightUpdate(LightUpdateS2CPacket packet);

	void onOpenWrittenBook(OpenWrittenBookS2CPacket packet);

	void onOpenScreen(OpenScreenS2CPacket packet);

	void onSetTradeOffers(SetTradeOffersS2CPacket packet);

	void onChunkLoadDistance(ChunkLoadDistanceS2CPacket packet);

	void onSimulationDistance(SimulationDistanceS2CPacket packet);

	void onChunkRenderDistanceCenter(ChunkRenderDistanceCenterS2CPacket packet);

	void onPlayerActionResponse(PlayerActionResponseS2CPacket packet);

	void onOverlayMessage(OverlayMessageS2CPacket packet);

	void onSubtitle(SubtitleS2CPacket packet);

	void onTitle(TitleS2CPacket packet);

	void onTitleFade(TitleFadeS2CPacket packet);

	void onTitleClear(ClearTitleS2CPacket packet);

	void onServerMetadata(ServerMetadataS2CPacket packet);

	void onChatSuggestions(ChatSuggestionsS2CPacket packet);

	void onBundle(BundleS2CPacket packet);

	void onEntityDamage(EntityDamageS2CPacket packet);

	void onEnterReconfiguration(EnterReconfigurationS2CPacket packet);

	void onStartChunkSend(StartChunkSendS2CPacket packet);

	void onChunkSent(ChunkSentS2CPacket packet);

	void onDebugSample(DebugSampleS2CPacket packet);

	void onProjectilePower(ProjectilePowerS2CPacket packet);

	void onSetCursorItem(SetCursorItemS2CPacket packet);

	void onSetPlayerInventory(SetPlayerInventoryS2CPacket packet);

	void onTestInstanceBlockStatus(TestInstanceBlockStatusS2CPacket packet);

	void onWaypoint(WaypointS2CPacket packet);

	void onChunkValueDebug(ChunkValueDebugS2CPacket packet);

	void onBlockValueDebug(BlockValueDebugS2CPacket packet);

	void onEntityValueDebug(EntityValueDebugS2CPacket packet);

	void onEventDebug(EventDebugS2CPacket packet);

	void onGameTestHighlightPos(GameTestHighlightPosS2CPacket packet);
}
