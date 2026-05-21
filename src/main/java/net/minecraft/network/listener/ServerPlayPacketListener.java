package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.c2s.play.*;

/**
 * Интерфейс server play packet listener.
 */
public interface ServerPlayPacketListener extends ServerCommonPacketListener, ServerQueryPingPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.PLAY;
	}

	void onHandSwing(HandSwingC2SPacket packet);

	void onChatMessage(ChatMessageC2SPacket packet);

	void onCommandExecution(CommandExecutionC2SPacket packet);

	void onChatCommandSigned(ChatCommandSignedC2SPacket packet);

	void onMessageAcknowledgment(MessageAcknowledgmentC2SPacket packet);

	void onClientStatus(ClientStatusC2SPacket packet);

	void onButtonClick(ButtonClickC2SPacket packet);

	void onClickSlot(ClickSlotC2SPacket packet);

	void onCraftRequest(CraftRequestC2SPacket packet);

	void onCloseHandledScreen(CloseHandledScreenC2SPacket packet);

	void onPlayerInteractEntity(PlayerInteractEntityC2SPacket packet);

	void onPlayerMove(PlayerMoveC2SPacket packet);

	void onUpdatePlayerAbilities(UpdatePlayerAbilitiesC2SPacket packet);

	void onPlayerAction(PlayerActionC2SPacket packet);

	void onClientCommand(ClientCommandC2SPacket packet);

	void onPlayerInput(PlayerInputC2SPacket packet);

	void onUpdateSelectedSlot(UpdateSelectedSlotC2SPacket packet);

	void onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet);

	void onUpdateSign(UpdateSignC2SPacket packet);

	void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet);

	void onPlayerInteractItem(PlayerInteractItemC2SPacket packet);

	void onSpectatorTeleport(SpectatorTeleportC2SPacket packet);

	void onBoatPaddleState(BoatPaddleStateC2SPacket packet);

	void onVehicleMove(VehicleMoveC2SPacket packet);

	void onTeleportConfirm(TeleportConfirmC2SPacket packet);

	void onPlayerLoaded(PlayerLoadedC2SPacket packet);

	void onRecipeBookData(RecipeBookDataC2SPacket packet);

	void onBundleItemSelected(BundleItemSelectedC2SPacket packet);

	void onRecipeCategoryOptions(RecipeCategoryOptionsC2SPacket packet);

	void onAdvancementTab(AdvancementTabC2SPacket packet);

	void onRequestCommandCompletions(RequestCommandCompletionsC2SPacket packet);

	void onUpdateCommandBlock(UpdateCommandBlockC2SPacket packet);

	void onUpdateCommandBlockMinecart(UpdateCommandBlockMinecartC2SPacket packet);

	void onPickItemFromBlock(PickItemFromBlockC2SPacket packet);

	void onPickItemFromEntity(PickItemFromEntityC2SPacket packet);

	void onRenameItem(RenameItemC2SPacket packet);

	void onUpdateBeacon(UpdateBeaconC2SPacket packet);

	void onUpdateStructureBlock(UpdateStructureBlockC2SPacket packet);

	void onSetTestBlock(SetTestBlockC2SPacket packet);

	void onTestInstanceBlockAction(TestInstanceBlockActionC2SPacket packet);

	void onSelectMerchantTrade(SelectMerchantTradeC2SPacket packet);

	void onBookUpdate(BookUpdateC2SPacket packet);

	void onQueryEntityNbt(QueryEntityNbtC2SPacket packet);

	void onSlotChangedState(SlotChangedStateC2SPacket packet);

	void onQueryBlockNbt(QueryBlockNbtC2SPacket packet);

	void onUpdateJigsaw(UpdateJigsawC2SPacket packet);

	void onJigsawGenerating(JigsawGeneratingC2SPacket packet);

	void onUpdateDifficulty(UpdateDifficultyC2SPacket packet);

	void onChangeGameMode(ChangeGameModeC2SPacket packet);

	void onUpdateDifficultyLock(UpdateDifficultyLockC2SPacket packet);

	void onPlayerSession(PlayerSessionC2SPacket packet);

	void onAcknowledgeReconfiguration(AcknowledgeReconfigurationC2SPacket packet);

	void onAcknowledgeChunks(AcknowledgeChunksC2SPacket packet);

	void onDebugSubscriptionRequest(DebugSubscriptionRequestC2SPacket packet);

	void onClientTickEnd(ClientTickEndC2SPacket packet);
}
