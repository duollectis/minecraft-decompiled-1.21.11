package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.OperatorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

/**
 * Менеджер взаимодействий клиентского игрока с миром.
 * Обрабатывает ломание блоков, взаимодействие с блоками и сущностями,
 * управление инвентарём и синхронизацию выбранного слота с сервером.
 */
@Environment(EnvType.CLIENT)
public class ClientPlayerInteractionManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int CREATIVE_BREAK_COOLDOWN = 5;
	private static final float BREAK_PROGRESS_COMPLETE = 1.0F;
	private static final float BREAK_PROGRESS_SCALE = 10.0F;
	private static final float SOUND_COOLDOWN_INTERVAL = 4.0F;

	private final MinecraftClient client;
	private final ClientPlayNetworkHandler networkHandler;
	private BlockPos currentBreakingPos = new BlockPos(-1, -1, -1);
	private ItemStack selectedStack = ItemStack.EMPTY;
	private float currentBreakingProgress;
	private float blockBreakingSoundCooldown;
	private int blockBreakingCooldown;
	private boolean breakingBlock;
	private GameMode gameMode = GameMode.DEFAULT;
	private @Nullable GameMode previousGameMode;
	private int lastSelectedSlot;

	/**
	 * @param client         клиент Minecraft
	 * @param networkHandler сетевой обработчик для отправки пакетов
	 */
	public ClientPlayerInteractionManager(MinecraftClient client, ClientPlayNetworkHandler networkHandler) {
		this.client = client;
		this.networkHandler = networkHandler;
	}

	/**
	 * Копирует способности игрока из текущего игрового режима.
	 *
	 * @param player игрок
	 */
	public void copyAbilities(PlayerEntity player) {
		gameMode.setAbilities(player.getAbilities());
	}

	/**
	 * Устанавливает текущий и предыдущий игровые режимы.
	 *
	 * @param gameMode         новый игровой режим
	 * @param previousGameMode предыдущий игровой режим или {@code null}
	 */
	public void setGameModes(GameMode gameMode, @Nullable GameMode previousGameMode) {
		this.gameMode = gameMode;
		this.previousGameMode = previousGameMode;
		gameMode.setAbilities(client.player.getAbilities());
	}

	/**
	 * Устанавливает игровой режим, сохраняя предыдущий.
	 *
	 * @param gameMode новый игровой режим
	 */
	public void setGameMode(GameMode gameMode) {
		if (gameMode == this.gameMode) {
			return;
		}

		previousGameMode = this.gameMode;
		this.gameMode = gameMode;
		gameMode.setAbilities(client.player.getAbilities());
	}

	/**
	 * Проверяет, отображаются ли полоски здоровья/голода (режим выживания).
	 *
	 * @return {@code true} в режиме выживания
	 */
	public boolean hasStatusBars() {
		return gameMode.isSurvivalLike();
	}

	/**
	 * Ломает блок на стороне клиента без отправки пакета.
	 *
	 * @param pos позиция блока
	 * @return {@code true} если блок был сломан
	 */
	public boolean breakBlock(BlockPos pos) {
		if (client.player.isBlockBreakingRestricted(client.world, pos, gameMode)) {
			return false;
		}

		World world = client.world;
		BlockState blockState = world.getBlockState(pos);

		if (client.player.getMainHandStack().canMine(blockState, world, pos, client.player) == false) {
			return false;
		}

		Block block = blockState.getBlock();

		if (block instanceof OperatorBlock && client.player.isCreativeLevelTwoOp() == false) {
			return false;
		}

		if (blockState.isAir()) {
			return false;
		}

		block.onBreak(world, pos, blockState, client.player);
		FluidState fluidState = world.getFluidState(pos);
		boolean broken = world.setBlockState(pos, fluidState.getBlockState(), 11);

		if (broken) {
			block.onBroken(world, pos, blockState);
		}

		if (SharedConstants.BLOCK_BREAK) {
			LOGGER.error("client broke {} {} -> {}", pos, blockState, world.getBlockState(pos));
		}

		return broken;
	}

	/**
	 * Начинает или продолжает ломание блока при нажатии кнопки атаки.
	 *
	 * @param pos       позиция блока
	 * @param direction сторона блока
	 * @return {@code true} если взаимодействие обработано
	 */
	public boolean attackBlock(BlockPos pos, Direction direction) {
		if (client.player.isBlockBreakingRestricted(client.world, pos, gameMode)) {
			return false;
		}

		if (client.world.getWorldBorder().contains(pos) == false) {
			return false;
		}

		if (client.player.getAbilities().creativeMode) {
			BlockState blockState = client.world.getBlockState(pos);
			client.getTutorialManager().onBlockBreaking(client.world, pos, blockState, 1.0F);

			if (SharedConstants.BLOCK_BREAK) {
				LOGGER.info("Creative start {} {}", pos, blockState);
			}

			sendSequencedPacket(
					client.world, sequence -> {
						breakBlock(pos);
						return new PlayerActionC2SPacket(
								PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
								pos,
								direction,
								sequence
						);
					}
			);
			blockBreakingCooldown = CREATIVE_BREAK_COOLDOWN;
		}
		else if (breakingBlock == false || isCurrentlyBreaking(pos) == false) {
			if (breakingBlock) {
				if (SharedConstants.BLOCK_BREAK) {
					LOGGER.info("Abort old break {} {}", pos, client.world.getBlockState(pos));
				}

				networkHandler.sendPacket(new PlayerActionC2SPacket(
						PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
						currentBreakingPos,
						direction
				));
			}

			BlockState blockState = client.world.getBlockState(pos);
			client.getTutorialManager().onBlockBreaking(client.world, pos, blockState, 0.0F);

			if (SharedConstants.BLOCK_BREAK) {
				LOGGER.info("Start break {} {}", pos, blockState);
			}

			sendSequencedPacket(
					client.world, sequence -> {
						boolean notAir = blockState.isAir() == false;

						if (notAir && currentBreakingProgress == 0.0F) {
							blockState.onBlockBreakStart(client.world, pos, client.player);
						}

						if (notAir
								&& blockState.calcBlockBreakingDelta(client.player, client.player.getEntityWorld(), pos)
								>= BREAK_PROGRESS_COMPLETE) {
							breakBlock(pos);
						}
						else {
							breakingBlock = true;
							currentBreakingPos = pos;
							selectedStack = client.player.getMainHandStack();
							currentBreakingProgress = 0.0F;
							blockBreakingSoundCooldown = 0.0F;
							client.world.setBlockBreakingInfo(
									client.player.getId(),
									currentBreakingPos,
									getBlockBreakingProgress()
							);
						}

						return new PlayerActionC2SPacket(
								PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
								pos,
								direction,
								sequence
						);
					}
			);
		}

		return true;
	}

	/**
	 * Отменяет текущее ломание блока.
	 */
	public void cancelBlockBreaking() {
		if (breakingBlock == false) {
			return;
		}

		BlockState blockState = client.world.getBlockState(currentBreakingPos);
		client.getTutorialManager().onBlockBreaking(client.world, currentBreakingPos, blockState, -1.0F);

		if (SharedConstants.BLOCK_BREAK) {
			LOGGER.info("Stop dest {} {}", currentBreakingPos, blockState);
		}

		networkHandler.sendPacket(new PlayerActionC2SPacket(
				PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
				currentBreakingPos,
				Direction.DOWN
		));
		breakingBlock = false;
		currentBreakingProgress = 0.0F;
		client.world.setBlockBreakingInfo(client.player.getId(), currentBreakingPos, -1);
		client.player.resetTicksSince();
	}

	/**
	 * Обновляет прогресс ломания блока за тик.
	 *
	 * @param pos       позиция блока
	 * @param direction сторона блока
	 * @return {@code true} если ломание продолжается
	 */
	public boolean updateBlockBreakingProgress(BlockPos pos, Direction direction) {
		syncSelectedSlot();

		if (blockBreakingCooldown > 0) {
			blockBreakingCooldown--;
			return true;
		}

		if (client.player.getAbilities().creativeMode && client.world.getWorldBorder().contains(pos)) {
			blockBreakingCooldown = CREATIVE_BREAK_COOLDOWN;
			BlockState blockState = client.world.getBlockState(pos);
			client.getTutorialManager().onBlockBreaking(client.world, pos, blockState, 1.0F);

			if (SharedConstants.BLOCK_BREAK) {
				LOGGER.info("Creative cont {} {}", pos, blockState);
			}

			sendSequencedPacket(
					client.world, sequence -> {
						breakBlock(pos);
						return new PlayerActionC2SPacket(
								PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
								pos,
								direction,
								sequence
						);
					}
			);
			return true;
		}

		if (isCurrentlyBreaking(pos)) {
			BlockState blockState = client.world.getBlockState(pos);

			if (blockState.isAir()) {
				breakingBlock = false;
				return false;
			}

			currentBreakingProgress += blockState.calcBlockBreakingDelta(
					client.player,
					client.player.getEntityWorld(),
					pos
			);

			if (blockBreakingSoundCooldown % SOUND_COOLDOWN_INTERVAL == 0.0F) {
				BlockSoundGroup soundGroup = blockState.getSoundGroup();
				client.getSoundManager().play(new PositionedSoundInstance(
						soundGroup.getHitSound(),
						SoundCategory.BLOCKS,
						(soundGroup.getVolume() + 1.0F) / 8.0F,
						soundGroup.getPitch() * 0.5F,
						SoundInstance.createRandom(),
						pos
				));
			}

			blockBreakingSoundCooldown++;
			client.getTutorialManager().onBlockBreaking(
					client.world,
					pos,
					blockState,
					MathHelper.clamp(currentBreakingProgress, 0.0F, 1.0F)
			);

			if (currentBreakingProgress >= BREAK_PROGRESS_COMPLETE) {
				breakingBlock = false;

				if (SharedConstants.BLOCK_BREAK) {
					LOGGER.info("Finished breaking {} {}", pos, blockState);
				}

				sendSequencedPacket(
						client.world, sequence -> {
							breakBlock(pos);
							return new PlayerActionC2SPacket(
									PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
									pos,
									direction,
									sequence
							);
						}
				);
				currentBreakingProgress = 0.0F;
				blockBreakingSoundCooldown = 0.0F;
				blockBreakingCooldown = CREATIVE_BREAK_COOLDOWN;
			}

			client.world.setBlockBreakingInfo(client.player.getId(), currentBreakingPos, getBlockBreakingProgress());
			return true;
		}

		return attackBlock(pos, direction);
	}

	/**
	 * Отправляет пакет с порядковым номером для оптимистичного обновления блоков.
	 *
	 * @param world         клиентский мир
	 * @param packetCreator создатель пакета, принимающий порядковый номер
	 */
	public final void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator) {
		try (PendingUpdateManager manager = world.getPendingUpdateManager().incrementSequence()) {
			int sequence = manager.getSequence();
			Packet<ServerPlayPacketListener> packet = packetCreator.predict(sequence);
			networkHandler.sendPacket(packet);
		}
	}

	/**
	 * Обновляет соединение за тик.
	 */
	public void tick() {
		syncSelectedSlot();

		if (networkHandler.getConnection().isOpen()) {
			networkHandler.getConnection().tick();
		}
		else {
			networkHandler.getConnection().handleDisconnection();
		}
	}

	/**
	 * Синхронизирует выбранный слот хотбара с сервером при его изменении.
	 */
	public final void syncSelectedSlot() {
		int selected = client.player.getInventory().getSelectedSlot();

		if (selected == lastSelectedSlot) {
			return;
		}

		lastSelectedSlot = selected;
		networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(lastSelectedSlot));
	}

	/**
	 * Взаимодействует с блоком (правая кнопка мыши).
	 *
	 * @param player    игрок
	 * @param hand      рука
	 * @param hitResult результат попадания по блоку
	 * @return результат взаимодействия
	 */
	public ActionResult interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		syncSelectedSlot();

		if (client.world.getWorldBorder().contains(hitResult.getBlockPos()) == false) {
			return ActionResult.FAIL;
		}

		MutableObject<ActionResult> result = new MutableObject<>();
		sendSequencedPacket(
				client.world, sequence -> {
					result.setValue(interactBlockInternal(player, hand, hitResult));
					return new PlayerInteractBlockC2SPacket(hand, hitResult, sequence);
				}
		);
		return result.get();
	}

	/**
	 * Использует предмет в руке (правая кнопка мыши без блока).
	 *
	 * @param player игрок
	 * @param hand   рука
	 * @return результат взаимодействия
	 */
	public ActionResult interactItem(PlayerEntity player, Hand hand) {
		if (gameMode == GameMode.SPECTATOR) {
			return ActionResult.PASS;
		}

		syncSelectedSlot();
		MutableObject<ActionResult> result = new MutableObject<>();

		sendSequencedPacket(
				client.world, sequence -> {
					PlayerInteractItemC2SPacket
							packet =
							new PlayerInteractItemC2SPacket(hand, sequence, player.getYaw(), player.getPitch());
					ItemStack stack = player.getStackInHand(hand);

					if (player.getItemCooldownManager().isCoolingDown(stack)) {
						result.setValue(ActionResult.PASS);
						return packet;
					}

					ActionResult actionResult = stack.use(client.world, player, hand);
					ItemStack newStack;

					if (actionResult instanceof ActionResult.Success success) {
						newStack =
								Objects.requireNonNullElseGet(
										success.getNewHandStack(),
										() -> player.getStackInHand(hand)
								);
					}
					else {
						newStack = player.getStackInHand(hand);
					}

					if (newStack != stack) {
						player.setStackInHand(hand, newStack);
					}

					result.setValue(actionResult);
					return packet;
				}
		);

		return result.get();
	}

	/**
	 * Создаёт клиентского игрока с настройками по умолчанию.
	 *
	 * @param world       клиентский мир
	 * @param statHandler обработчик статистики
	 * @param recipeBook  книга рецептов
	 * @return новый клиентский игрок
	 */
	public ClientPlayerEntity createPlayer(ClientWorld world, StatHandler statHandler, ClientRecipeBook recipeBook) {
		return createPlayer(world, statHandler, recipeBook, PlayerInput.DEFAULT, false);
	}

	/**
	 * Создаёт клиентского игрока с восстановленным состоянием ввода.
	 *
	 * @param world           клиентский мир
	 * @param statHandler     обработчик статистики
	 * @param recipeBook      книга рецептов
	 * @param lastPlayerInput последний ввод игрока
	 * @param lastSprinting   был ли игрок в спринте
	 * @return новый клиентский игрок
	 */
	public ClientPlayerEntity createPlayer(
			ClientWorld world,
			StatHandler statHandler,
			ClientRecipeBook recipeBook,
			PlayerInput lastPlayerInput,
			boolean lastSprinting
	) {
		return new ClientPlayerEntity(
				client,
				world,
				networkHandler,
				statHandler,
				recipeBook,
				lastPlayerInput,
				lastSprinting
		);
	}

	/**
	 * Атакует сущность.
	 *
	 * @param player игрок
	 * @param target цель атаки
	 */
	public void attackEntity(PlayerEntity player, Entity target) {
		syncSelectedSlot();
		networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));

		if (gameMode == GameMode.SPECTATOR) {
			return;
		}

		player.attack(target);
		player.resetTicksSince();
	}

	/**
	 * Взаимодействует с сущностью (правая кнопка мыши).
	 *
	 * @param player игрок
	 * @param entity сущность
	 * @param hand   рука
	 * @return результат взаимодействия
	 */
	public ActionResult interactEntity(PlayerEntity player, Entity entity, Hand hand) {
		syncSelectedSlot();
		networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, player.isSneaking(), hand));
		return gameMode == GameMode.SPECTATOR ? ActionResult.PASS : player.interact(entity, hand);
	}

	/**
	 * Взаимодействует с сущностью в конкретной точке.
	 *
	 * @param player    игрок
	 * @param entity    сущность
	 * @param hitResult результат попадания по сущности
	 * @param hand      рука
	 * @return результат взаимодействия
	 */
	public ActionResult interactEntityAtLocation(
			PlayerEntity player,
			Entity entity,
			EntityHitResult hitResult,
			Hand hand
	) {
		syncSelectedSlot();
		Vec3d offset = hitResult.getPos().subtract(entity.getX(), entity.getY(), entity.getZ());
		networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, player.isSneaking(), hand, offset));
		return gameMode == GameMode.SPECTATOR ? ActionResult.PASS : entity.interactAt(player, offset, hand);
	}

	/**
	 * Кликает по слоту инвентаря.
	 *
	 * @param syncId     идентификатор контейнера
	 * @param slotId     индекс слота
	 * @param button     кнопка мыши
	 * @param actionType тип действия
	 * @param player     игрок
	 */
	public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
		ScreenHandler handler = player.currentScreenHandler;

		if (syncId != handler.syncId) {
			LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", syncId, handler.syncId);
			return;
		}

		DefaultedList<Slot> slots = handler.slots;
		int slotCount = slots.size();
		List<ItemStack> before = Lists.newArrayListWithCapacity(slotCount);

		for (Slot slot : slots) {
			before.add(slot.getStack().copy());
		}

		handler.onSlotClick(slotId, button, actionType, player);
		Int2ObjectMap<ItemStackHash> changedSlots = new Int2ObjectOpenHashMap<>();

		for (int i = 0; i < slotCount; i++) {
			ItemStack oldStack = before.get(i);
			ItemStack newStack = slots.get(i).getStack();

			if (ItemStack.areEqual(oldStack, newStack) == false) {
				changedSlots.put(i, ItemStackHash.fromItemStack(newStack, networkHandler.getComponentHasher()));
			}
		}

		ItemStackHash
				cursorHash =
				ItemStackHash.fromItemStack(handler.getCursorStack(), networkHandler.getComponentHasher());
		networkHandler.sendPacket(new ClickSlotC2SPacket(
				syncId,
				handler.getRevision(),
				Shorts.checkedCast(slotId),
				SignedBytes.checkedCast(button),
				actionType,
				changedSlots,
				cursorHash
		));
	}

	/**
	 * Отправляет запрос на крафт рецепта.
	 *
	 * @param syncId   идентификатор контейнера
	 * @param recipeId идентификатор рецепта
	 * @param craftAll {@code true} для крафта максимального количества
	 */
	public void clickRecipe(int syncId, NetworkRecipeId recipeId, boolean craftAll) {
		networkHandler.sendPacket(new CraftRequestC2SPacket(syncId, recipeId, craftAll));
	}

	/**
	 * Нажимает кнопку в контейнере.
	 *
	 * @param syncId   идентификатор контейнера
	 * @param buttonId идентификатор кнопки
	 */
	public void clickButton(int syncId, int buttonId) {
		networkHandler.sendPacket(new ButtonClickC2SPacket(syncId, buttonId));
	}

	/**
	 * Кликает по стаку в творческом режиме.
	 *
	 * @param stack  стак предмета
	 * @param slotId индекс слота
	 */
	public void clickCreativeStack(ItemStack stack, int slotId) {
		if (client.player.isInCreativeMode() && networkHandler.hasFeature(stack.getItem().getRequiredFeatures())) {
			networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slotId, stack));
		}
	}

	/**
	 * Выбрасывает стак в творческом режиме.
	 *
	 * @param stack стак предмета
	 */
	public void dropCreativeStack(ItemStack stack) {
		boolean inHandledScreen = client.currentScreen instanceof HandledScreen
				&& (client.currentScreen instanceof CreativeInventoryScreen) == false;

		if (client.player.isInCreativeMode()
				&& inHandledScreen == false
				&& stack.isEmpty() == false
				&& networkHandler.hasFeature(stack.getItem().getRequiredFeatures())
		) {
			networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(-1, stack));
			client.player.getItemDropCooldown().increment();
		}
	}

	/**
	 * Прекращает использование предмета.
	 *
	 * @param player игрок
	 */
	public void stopUsingItem(PlayerEntity player) {
		syncSelectedSlot();
		networkHandler.sendPacket(new PlayerActionC2SPacket(
				PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
				BlockPos.ORIGIN,
				Direction.DOWN
		));
		player.stopUsingItem();
	}

	/**
	 * Выполняет атаку пронизывающим оружием (колющий удар).
	 *
	 * @param piercingWeaponComponent компонент пронизывающего оружия
	 */
	public void attackWithPiercingWeapon(PiercingWeaponComponent piercingWeaponComponent) {
		syncSelectedSlot();
		networkHandler.sendPacket(new PlayerActionC2SPacket(
				PlayerActionC2SPacket.Action.STAB,
				BlockPos.ORIGIN,
				Direction.DOWN
		));
		client.player.beforePlayerAttack();
		client.player.useAttackEnchantmentEffects();
		piercingWeaponComponent.playSound(client.player);
	}

	/**
	 * Проверяет, отображается ли полоска опыта.
	 *
	 * @return {@code true} в режиме выживания
	 */
	public boolean hasExperienceBar() {
		return gameMode.isSurvivalLike();
	}

	/**
	 * Проверяет, ограничена ли скорость атаки (не творческий режим).
	 *
	 * @return {@code true} если скорость атаки ограничена
	 */
	public boolean hasLimitedAttackSpeed() {
		return gameMode.isCreative() == false;
	}

	/**
	 * Проверяет, есть ли у игрока инвентарь верхового животного.
	 *
	 * @return {@code true} если игрок едет на существе с инвентарём
	 */
	public boolean hasRidingInventory() {
		return client.player.hasVehicle() && client.player.getVehicle() instanceof RideableInventory;
	}

	/**
	 * Проверяет, заблокирован ли полёт (режим наблюдателя).
	 *
	 * @return {@code true} в режиме наблюдателя
	 */
	public boolean isFlyingLocked() {
		return gameMode == GameMode.SPECTATOR;
	}

	/**
	 * Возвращает предыдущий игровой режим.
	 *
	 * @return предыдущий режим или {@code null}
	 */
	public @Nullable GameMode getPreviousGameMode() {
		return previousGameMode;
	}

	/**
	 * Возвращает текущий игровой режим.
	 *
	 * @return текущий режим
	 */
	public GameMode getCurrentGameMode() {
		return gameMode;
	}

	/**
	 * Проверяет, идёт ли сейчас ломание блока.
	 *
	 * @return {@code true} если блок ломается
	 */
	public boolean isBreakingBlock() {
		return breakingBlock;
	}

	/**
	 * Возвращает прогресс ломания блока в диапазоне 0–10, или -1 если блок не ломается.
	 *
	 * @return прогресс ломания или -1
	 */
	public int getBlockBreakingProgress() {
		return currentBreakingProgress > 0.0F
		       ? (int) (currentBreakingProgress * BREAK_PROGRESS_SCALE)
		       : -1;
	}

	/**
	 * Отправляет пакет подбора предмета из блока (средняя кнопка мыши).
	 *
	 * @param pos         позиция блока
	 * @param includeData включать ли NBT-данные блока
	 */
	public void pickItemFromBlock(BlockPos pos, boolean includeData) {
		networkHandler.sendPacket(new PickItemFromBlockC2SPacket(pos, includeData));
	}

	/**
	 * Отправляет пакет подбора предмета из сущности (средняя кнопка мыши).
	 *
	 * @param entity      целевая сущность
	 * @param includeData включать ли NBT-данные сущности
	 */
	public void pickItemFromEntity(Entity entity, boolean includeData) {
		networkHandler.sendPacket(new PickItemFromEntityC2SPacket(entity.getId(), includeData));
	}

	/**
	 * Отправляет пакет изменения состояния слота (например, переключение рецепта).
	 *
	 * @param slot            индекс слота
	 * @param screenHandlerId идентификатор экрана
	 * @param newState        новое состояние слота
	 */
	public void slotChangedState(int slot, int screenHandlerId, boolean newState) {
		networkHandler.sendPacket(new SlotChangedStateC2SPacket(slot, screenHandlerId, newState));
	}

	/**
	 * Проверяет, продолжает ли игрок ломать тот же блок тем же предметом.
	 * Используется для определения непрерывности процесса ломания.
	 *
	 * @param pos позиция блока
	 * @return {@code true} если позиция и предмет в руке совпадают с текущим ломанием
	 */
	private boolean isCurrentlyBreaking(BlockPos pos) {
		ItemStack stack = client.player.getMainHandStack();
		return pos.equals(currentBreakingPos) && ItemStack.areItemsAndComponentsEqual(stack, selectedStack);
	}

	/**
	 * Внутренняя реализация взаимодействия с блоком на стороне клиента.
	 * Вызывается внутри {@link #sendSequencedPacket} — пакет уже создаётся снаружи.
	 * Если {@code onUseWithItem} возвращает {@code PASS_TO_DEFAULT_BLOCK_ACTION},
	 * вызывается {@code onUse} — стандартное действие блока (открытие сундука, нажатие кнопки и т.д.).
	 *
	 * @param player    игрок
	 * @param hand      рука
	 * @param hitResult результат попадания по блоку
	 * @return результат взаимодействия
	 */
	private ActionResult interactBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		BlockState blockState = client.world.getBlockState(hitResult.getBlockPos());
		ActionResult actionResult = blockState.onUseWithItem(player.getMainHandStack(), client.world, player, hand, hitResult);

		if (actionResult instanceof ActionResult.PassToDefaultBlockAction) {
			ActionResult useResult = blockState.onUse(client.world, player, hitResult);

			if (useResult.isAccepted()) {
				return useResult;
			}
		} else if (actionResult.isAccepted()) {
			return actionResult;
		}

		ItemStack stack = player.getStackInHand(hand);

		if (stack.isEmpty() || player.getItemCooldownManager().isCoolingDown(stack)) {
			return ActionResult.PASS;
		}

		return stack.useOnBlock(new ItemUsageContext(player, hand, hitResult));
	}

	/**
	 * Функциональный интерфейс для создания пакетов с порядковым номером.
	 * Используется в {@link #sendSequencedPacket} для оптимистичных обновлений блоков.
	 */
	@FunctionalInterface
	public interface SequencedPacketCreator {

		/**
		 * Создаёт пакет с заданным порядковым номером.
		 *
		 * @param sequence порядковый номер для синхронизации с сервером
		 * @return пакет для отправки
		 */
		Packet<ServerPlayPacketListener> predict(int sequence);
	}
}
