package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Portal;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.sound.*;
import net.minecraft.client.util.ClientPlayerTickable;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.UseEffectsComponent;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.CommandBlockMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.StatHandler;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * Клиентская сущность игрока — главный персонаж, управляемый локальным пользователем.
 * Отвечает за отправку пакетов движения, взаимодействия, прыжков и синхронизацию
 * состояния с сервером. Содержит логику спринта, автопрыжка, полёта и тошноты.
 */
@Environment(EnvType.CLIENT)
public class ClientPlayerEntity extends AbstractClientPlayerEntity {

	public static final Logger LOGGER = LogUtils.getLogger();

	private static final int POSITION_PACKET_INTERVAL = 20;
	private static final int UNDERWATER_VISIBILITY_MAX = 600;
	private static final int UNDERWATER_VISIBILITY_RAMP = 100;
	private static final float SNEAKING_SPEED_FACTOR = 0.6F;
	private static final double SOFT_COLLISION_PUSH_DISTANCE = 0.35;
	private static final double MAX_SOFT_COLLISION_RADIANS = 0.13962634F;
	private static final int XP_BAR_DISPLAY_UNSET = Integer.MIN_VALUE;
	private static final int XP_BAR_DISPLAY_PENDING = -2147483647;

	public final ClientPlayNetworkHandler networkHandler;
	private final StatHandler statHandler;
	private final ClientRecipeBook recipeBook;
	private final Cooldown itemDropCooldown = new Cooldown(20, 1280);
	private final List<ClientPlayerTickable> tickables = Lists.newArrayList();

	private PermissionPredicate permissions = PermissionPredicate.NONE;
	private double lastXClient;
	private double lastYClient;
	private double lastZClient;
	private float lastYawClient;
	private float lastPitchClient;
	private boolean lastOnGround;
	private boolean lastHorizontalCollision;
	private boolean inSneakingPose;
	private boolean lastSprinting;
	private int ticksSinceLastPositionPacketSent;
	private boolean healthInitialized;
	public Input input = new Input();
	private PlayerInput lastPlayerInput;
	protected final MinecraftClient client;
	protected int ticksLeftToDoubleTapSprint;
	public int experienceBarDisplayStartTime = XP_BAR_DISPLAY_UNSET;
	public float renderYaw;
	public float renderPitch;
	public float lastRenderYaw;
	public float lastRenderPitch;
	private int mountJumpTicks;
	private float mountJumpStrength;
	public float nauseaIntensity;
	public float lastNauseaIntensity;
	private boolean usingItem;
	private @Nullable Hand activeHand;
	private boolean riding;
	private boolean autoJumpEnabled = true;
	private int ticksToNextAutoJump;
	private boolean falling;
	private int underwaterVisibilityTicks;
	private boolean showsDeathScreen = true;
	private boolean limitedCraftingEnabled = false;

	/**
	 * @param client          клиент Minecraft
	 * @param world           клиентский мир
	 * @param networkHandler  сетевой обработчик
	 * @param stats           обработчик статистики
	 * @param recipeBook      книга рецептов
	 * @param lastPlayerInput последний ввод игрока
	 * @param lastSprinting   был ли игрок в спринте
	 */
	public ClientPlayerEntity(
			MinecraftClient client,
			ClientWorld world,
			ClientPlayNetworkHandler networkHandler,
			StatHandler stats,
			ClientRecipeBook recipeBook,
			PlayerInput lastPlayerInput,
			boolean lastSprinting
	) {
		super(world, networkHandler.getProfile());
		this.client = client;
		this.networkHandler = networkHandler;
		this.statHandler = stats;
		this.recipeBook = recipeBook;
		this.lastPlayerInput = lastPlayerInput;
		this.lastSprinting = lastSprinting;
		tickables.add(new AmbientSoundPlayer(this, client.getSoundManager()));
		tickables.add(new BubbleColumnSoundPlayer(this));
		tickables.add(new BiomeEffectSoundPlayer(this, client.getSoundManager()));
	}

	@Override
	public void heal(float amount) {
	}

	@Override
	public boolean startRiding(Entity entity, boolean force, boolean emitEvent) {
		if (super.startRiding(entity, force, emitEvent) == false) {
			return false;
		}

		if (entity instanceof AbstractMinecartEntity minecart) {
			client.getSoundManager().play(new MinecartInsideSoundInstance(
					this, minecart, true, SoundEvents.ENTITY_MINECART_INSIDE_UNDERWATER, 0.0F, 0.75F, 1.0F
			));
			client.getSoundManager().play(new MinecartInsideSoundInstance(
					this, minecart, false, SoundEvents.ENTITY_MINECART_INSIDE, 0.0F, 0.75F, 1.0F
			));
		}
		else if (entity instanceof HappyGhastEntity happyGhast) {
			client.getSoundManager().play(new HappyGhastRidingSoundInstance(
					this,
					happyGhast,
					false,
					SoundEvents.ENTITY_HAPPY_GHAST_RIDING,
					happyGhast.getSoundCategory(),
					0.0F,
					1.0F,
					5.0F
			));
		}
		else if (entity instanceof AbstractNautilusEntity nautilus) {
			client.getSoundManager().play(new HappyGhastRidingSoundInstance(
					this,
					nautilus,
					true,
					SoundEvents.ENTITY_NAUTILUS_RIDING,
					nautilus.getSoundCategory(),
					0.0F,
					1.0F,
					5.0F
			));
		}

		return true;
	}

	@Override
	public void dismountVehicle() {
		super.dismountVehicle();
		riding = false;
	}

	@Override
	public float getPitch(float tickProgress) {
		return getPitch();
	}

	@Override
	public float getYaw(float tickProgress) {
		return hasVehicle() ? super.getYaw(tickProgress) : getYaw();
	}

	@Override
	public void tick() {
		if (networkHandler.isPlayerLoaded() == false) {
			return;
		}

		itemDropCooldown.tick();
		super.tick();

		if (lastPlayerInput.equals(input.playerInput) == false) {
			networkHandler.sendPacket(new PlayerInputC2SPacket(input.playerInput));
			lastPlayerInput = input.playerInput;
		}

		if (hasVehicle()) {
			networkHandler.sendPacket(
					new PlayerMoveC2SPacket.LookAndOnGround(getYaw(), getPitch(), isOnGround(), horizontalCollision)
			);
			Entity rootVehicle = getRootVehicle();
			if (rootVehicle != this && rootVehicle.isLogicalSideForUpdatingMovement()) {
				networkHandler.sendPacket(VehicleMoveC2SPacket.fromVehicle(rootVehicle));
				sendSprintingPacket();
			}
		}
		else {
			sendMovementPackets();
		}

		for (ClientPlayerTickable tickable : tickables) {
			tickable.tick();
		}
	}

	/**
	 * Возвращает процент настроения биома (используется для звуков биома).
	 *
	 * @return значение от 0.0 до 1.0
	 */
	public float getMoodPercentage() {
		for (ClientPlayerTickable tickable : tickables) {
			if (tickable instanceof BiomeEffectSoundPlayer soundPlayer) {
				return soundPlayer.getMoodPercentage();
			}
		}

		return 0.0F;
	}

	private void sendMovementPackets() {
		sendSprintingPacket();
		if (isCamera() == false) {
			return;
		}

		double dx = getX() - lastXClient;
		double dy = getY() - lastYClient;
		double dz = getZ() - lastZClient;
		double dyaw = getYaw() - lastYawClient;
		double dpitch = getPitch() - lastPitchClient;
		ticksSinceLastPositionPacketSent++;

		boolean positionChanged = MathHelper.squaredMagnitude(dx, dy, dz) > MathHelper.square(2.0E-4)
				|| ticksSinceLastPositionPacketSent >= POSITION_PACKET_INTERVAL;
		boolean rotationChanged = dyaw != 0.0 || dpitch != 0.0;

		if (positionChanged && rotationChanged) {
			networkHandler.sendPacket(
					new PlayerMoveC2SPacket.Full(
							getEntityPos(),
							getYaw(),
							getPitch(),
							isOnGround(),
							horizontalCollision
					)
			);
		}
		else if (positionChanged) {
			networkHandler.sendPacket(
					new PlayerMoveC2SPacket.PositionAndOnGround(getEntityPos(), isOnGround(), horizontalCollision)
			);
		}
		else if (rotationChanged) {
			networkHandler.sendPacket(
					new PlayerMoveC2SPacket.LookAndOnGround(getYaw(), getPitch(), isOnGround(), horizontalCollision)
			);
		}
		else if (lastOnGround != isOnGround() || lastHorizontalCollision != horizontalCollision) {
			networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(isOnGround(), horizontalCollision));
		}

		if (positionChanged) {
			lastXClient = getX();
			lastYClient = getY();
			lastZClient = getZ();
			ticksSinceLastPositionPacketSent = 0;
		}

		if (rotationChanged) {
			lastYawClient = getYaw();
			lastPitchClient = getPitch();
		}

		lastOnGround = isOnGround();
		lastHorizontalCollision = horizontalCollision;
		autoJumpEnabled = client.options.getAutoJump().getValue();
	}

	private void sendSprintingPacket() {
		boolean sprinting = isSprinting();
		if (sprinting == lastSprinting) {
			return;
		}

		ClientCommandC2SPacket.Mode mode = sprinting
		                                   ? ClientCommandC2SPacket.Mode.START_SPRINTING
		                                   : ClientCommandC2SPacket.Mode.STOP_SPRINTING;
		networkHandler.sendPacket(new ClientCommandC2SPacket(this, mode));
		lastSprinting = sprinting;
	}

	private void startRidingJump() {
		networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.START_RIDING_JUMP));
	}

	/**
	 * Выбрасывает выбранный предмет из инвентаря.
	 *
	 * @param entireStack {@code true} — выбросить весь стак, {@code false} — один предмет
	 * @return {@code true} если предмет был выброшен
	 */
	public boolean dropSelectedItem(boolean entireStack) {
		PlayerActionC2SPacket.Action action = entireStack
		                                      ? PlayerActionC2SPacket.Action.DROP_ALL_ITEMS
		                                      : PlayerActionC2SPacket.Action.DROP_ITEM;
		ItemStack dropped = getInventory().dropSelectedItem(entireStack);
		networkHandler.sendPacket(new PlayerActionC2SPacket(action, BlockPos.ORIGIN, Direction.DOWN));
		return dropped.isEmpty() == false;
	}

	@Override
	public void swingHand(Hand hand) {
		super.swingHand(hand);
		networkHandler.sendPacket(new HandSwingC2SPacket(hand));
	}

	/**
	 * Отправляет запрос на возрождение после смерти.
	 */
	public void requestRespawn() {
		networkHandler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
		KeyBinding.untoggleStickyKeys();
	}

	@Override
	public void closeHandledScreen() {
		networkHandler.sendPacket(new CloseHandledScreenC2SPacket(currentScreenHandler.syncId));
		closeScreen();
	}

	/**
	 * Закрывает экран инвентаря без отправки пакета (только клиентская сторона).
	 */
	public void closeScreen() {
		super.closeHandledScreen();
		client.setScreen(null);
	}

	/**
	 * Обновляет здоровье игрока с учётом анимации урона.
	 *
	 * @param health новое значение здоровья
	 */
	public void updateHealth(float health) {
		if (healthInitialized == false) {
			setHealth(health);
			healthInitialized = true;
			return;
		}

		float delta = getHealth() - health;
		if (delta <= 0.0F) {
			setHealth(health);
			if (delta < 0.0F) {
				timeUntilRegen = 10;
			}
		}
		else {
			lastDamageTaken = delta;
			timeUntilRegen = 20;
			setHealth(health);
			maxHurtTime = 10;
			hurtTime = maxHurtTime;
		}
	}

	@Override
	public void sendAbilitiesUpdate() {
		networkHandler.sendPacket(new UpdatePlayerAbilitiesC2SPacket(getAbilities()));
	}

	@Override
	public void setReducedDebugInfo(boolean reducedDebugInfo) {
		super.setReducedDebugInfo(reducedDebugInfo);
		client.debugHudEntryList.updateVisibleEntries();
	}

	@Override
	public boolean isMainPlayer() {
		return true;
	}

	@Override
	public boolean isHoldingOntoLadder() {
		return getAbilities().flying == false && super.isHoldingOntoLadder();
	}

	@Override
	public boolean shouldSpawnSprintingParticles() {
		return getAbilities().flying == false && super.shouldSpawnSprintingParticles();
	}

	/**
	 * Открывает инвентарь верхового животного.
	 */
	public void openRidingInventory() {
		networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.OPEN_INVENTORY));
	}

	/**
	 * Возвращает обработчик статистики игрока.
	 */
	public StatHandler getStatHandler() {
		return statHandler;
	}

	/**
	 * Возвращает книгу рецептов игрока.
	 */
	public ClientRecipeBook getRecipeBook() {
		return recipeBook;
	}

	/**
	 * Снимает подсветку рецепта при его отображении и уведомляет сервер.
	 *
	 * @param recipeId идентификатор рецепта
	 */
	public void onRecipeDisplayed(NetworkRecipeId recipeId) {
		if (recipeBook.isHighlighted(recipeId) == false) {
			return;
		}

		recipeBook.unmarkHighlighted(recipeId);
		networkHandler.sendPacket(new RecipeBookDataC2SPacket(recipeId));
	}

	@Override
	public PermissionPredicate getPermissions() {
		return permissions;
	}

	/**
	 * Устанавливает разрешения игрока.
	 *
	 * @param permissions новый предикат разрешений
	 */
	public void setPermissions(PermissionPredicate permissions) {
		this.permissions = permissions;
	}

	@Override
	public void sendMessage(Text message, boolean overlay) {
		client.getMessageHandler().onGameMessage(message, overlay);
	}

	/**
	 * Устанавливает прогресс опыта, общий опыт и уровень.
	 * Запускает анимацию XP-бара при изменении прогресса.
	 *
	 * @param progress прогресс до следующего уровня (0.0–1.0)
	 * @param total    суммарный опыт
	 * @param level    текущий уровень
	 */
	public void setExperience(float progress, int total, int level) {
		if (progress != experienceProgress) {
			updateExperienceBarDisplayStartTime();
		}

		experienceProgress = progress;
		totalExperience = total;
		experienceLevel = level;
	}

	private void pushOutOfBlocks(double x, double z) {
		BlockPos pos = BlockPos.ofFloored(x, getY(), z);
		if (wouldCollideAt(pos) == false) {
			return;
		}

		double localX = x - pos.getX();
		double localZ = z - pos.getZ();
		Direction bestDir = null;
		double minPenetration = Double.MAX_VALUE;
		Direction[] candidates = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

		for (Direction dir : candidates) {
			double component = dir.getAxis().choose(localX, 0.0, localZ);
			double penetration = dir.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - component : component;
			if (penetration < minPenetration && wouldCollideAt(pos.offset(dir)) == false) {
				minPenetration = penetration;
				bestDir = dir;
			}
		}

		if (bestDir == null) {
			return;
		}

		Vec3d velocity = getVelocity();
		if (bestDir.getAxis() == Direction.Axis.X) {
			setVelocity(0.1 * bestDir.getOffsetX(), velocity.y, velocity.z);
		}
		else {
			setVelocity(velocity.x, velocity.y, 0.1 * bestDir.getOffsetZ());
		}
	}

	private boolean wouldCollideAt(BlockPos pos) {
		Box bounds = getBoundingBox();
		Box checkBox = new Box(
				pos.getX(), bounds.minY, pos.getZ(),
				pos.getX() + 1.0, bounds.maxY, pos.getZ() + 1.0
		).contract(1.0E-7);
		return getEntityWorld().canCollide(this, checkBox);
	}

	private void updateExperienceBarDisplayStartTime() {
		experienceBarDisplayStartTime = experienceBarDisplayStartTime == XP_BAR_DISPLAY_UNSET
		                                ? XP_BAR_DISPLAY_PENDING
		                                : age;
	}

	/**
	 * Обрабатывает статусные события сущности: обновляет уровень прав игрока.
	 */
	@Override
	public void handleStatus(byte status) {
		switch (status) {
			case 24 -> setPermissions(PermissionPredicate.NONE);
			case 25 -> setPermissions(LeveledPermissionPredicate.MODERATORS);
			case 26 -> setPermissions(LeveledPermissionPredicate.GAMEMASTERS);
			case 27 -> setPermissions(LeveledPermissionPredicate.ADMINS);
			case 28 -> setPermissions(LeveledPermissionPredicate.OWNERS);
			default -> super.handleStatus(status);
		}
	}

	/**
	 * Устанавливает, показывать ли экран смерти при гибели игрока.
	 */
	public void setShowsDeathScreen(boolean showsDeathScreen) {
		this.showsDeathScreen = showsDeathScreen;
	}

	/**
	 * Возвращает, показывается ли экран смерти при гибели.
	 */
	public boolean showsDeathScreen() {
		return showsDeathScreen;
	}

	/**
	 * Устанавливает режим ограниченного крафта.
	 */
	public void setLimitedCraftingEnabled(boolean limitedCraftingEnabled) {
		this.limitedCraftingEnabled = limitedCraftingEnabled;
	}

	/**
	 * Возвращает, включён ли режим ограниченного крафта.
	 */
	public boolean isLimitedCraftingEnabled() {
		return limitedCraftingEnabled;
	}

	/**
	 * Воспроизводит звук в позиции игрока на клиентской стороне.
	 */
	@Override
	public void playSound(SoundEvent sound, float volume, float pitch) {
		getEntityWorld().playSoundClient(
				getX(),
				getY(),
				getZ(),
				sound,
				getSoundCategory(),
				volume,
				pitch,
				false
		);
	}

	/**
	 * Начинает использование предмета в указанной руке, если рука не пуста и предмет ещё не используется.
	 */
	@Override
	public void setCurrentHand(Hand hand) {
		ItemStack itemStack = getStackInHand(hand);
		if (itemStack.isEmpty() || isUsingItem()) {
			return;
		}

		super.setCurrentHand(hand);
		usingItem = true;
		activeHand = hand;
	}

	/**
	 * Возвращает, использует ли игрок предмет в данный момент.
	 */
	@Override
	public boolean isUsingItem() {
		return usingItem;
	}

	private boolean isBlockedFromSprinting() {
		return isUsingItem()
				&& activeItemStack
				.getOrDefault(DataComponentTypes.USE_EFFECTS, UseEffectsComponent.DEFAULT)
				.canSprint() == false;
	}

	private float getActiveItemSpeedMultiplier() {
		return activeItemStack
				.getOrDefault(DataComponentTypes.USE_EFFECTS, UseEffectsComponent.DEFAULT)
				.speedMultiplier();
	}

	/**
	 * Сбрасывает активный предмет и снимает флаг использования.
	 */
	@Override
	public void clearActiveItem() {
		super.clearActiveItem();
		usingItem = false;
	}

	/**
	 * Возвращает активную руку игрока, или основную руку по умолчанию.
	 */
	@Override
	public Hand getActiveHand() {
		return Objects.requireNonNullElse(activeHand, Hand.MAIN_HAND);
	}

	/**
	 * Реагирует на изменение отслеживаемых данных: синхронизирует использование предмета и запускает звук элитры.
	 */
	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);

		if (LIVING_FLAGS.equals(data)) {
			boolean serverUsingItem = (dataTracker.get(LIVING_FLAGS) & 1) > 0;
			Hand hand = (dataTracker.get(LIVING_FLAGS) & 2) > 0 ? Hand.OFF_HAND : Hand.MAIN_HAND;

			if (serverUsingItem && usingItem == false) {
				setCurrentHand(hand);
			}
			else if (serverUsingItem == false && usingItem) {
				clearActiveItem();
			}
		}

		if (FLAGS.equals(data) && isGliding() && falling == false) {
			client.getSoundManager().play(new ElytraSoundInstance(this));
		}
	}

	/**
	 * Возвращает верховое животное с поддержкой прыжка, если оно может прыгать.
	 */
	public @Nullable JumpingMount getJumpingMount() {
		return getControllingVehicle() instanceof JumpingMount jumpingMount && jumpingMount.canJump()
		       ? jumpingMount
		       : null;
	}

	/**
	 * Возвращает текущую силу прыжка верхового животного.
	 */
	public float getMountJumpStrength() {
		return mountJumpStrength;
	}

	/**
	 * Возвращает, нужно ли фильтровать текст согласно настройкам клиента.
	 */
	@Override
	public boolean shouldFilterText() {
		return client.shouldFilterText();
	}

	/**
	 * Открывает экран редактирования таблички (обычной или висячей).
	 */
	@Override
	public void openEditSignScreen(SignBlockEntity sign, boolean front) {
		if (sign instanceof HangingSignBlockEntity hangingSign) {
			client.setScreen(new HangingSignEditScreen(hangingSign, front, client.shouldFilterText()));
		}
		else {
			client.setScreen(new SignEditScreen(sign, front, client.shouldFilterText()));
		}
	}

	/**
	 * Открывает экран командного блока в вагонетке.
	 */
	@Override
	public void openCommandBlockMinecartScreen(CommandBlockMinecartEntity minecart) {
		client.setScreen(new MinecartCommandBlockScreen(minecart));
	}

	/**
	 * Открывает экран командного блока.
	 */
	@Override
	public void openCommandBlockScreen(CommandBlockBlockEntity commandBlock) {
		client.setScreen(new CommandBlockScreen(commandBlock));
	}

	/**
	 * Открывает экран блока структуры.
	 */
	@Override
	public void openStructureBlockScreen(StructureBlockBlockEntity structureBlock) {
		client.setScreen(new StructureBlockScreen(structureBlock));
	}

	/**
	 * Открывает экран тестового блока.
	 */
	@Override
	public void openTestBlockScreen(TestBlockEntity testBlock) {
		client.setScreen(new TestBlockScreen(testBlock));
	}

	/**
	 * Открывает экран блока тестового экземпляра.
	 */
	@Override
	public void openTestInstanceBlockScreen(TestInstanceBlockEntity testInstanceBlock) {
		client.setScreen(new TestInstanceBlockScreen(testInstanceBlock));
	}

	/**
	 * Открывает экран блока пазла (Jigsaw).
	 */
	@Override
	public void openJigsawScreen(JigsawBlockEntity jigsaw) {
		client.setScreen(new JigsawBlockScreen(jigsaw));
	}

	/**
	 * Открывает диалоговое окно из реестра диалогов.
	 */
	@Override
	public void openDialog(RegistryEntry<Dialog> dialog) {
		networkHandler.showDialog(dialog, client.currentScreen);
	}

	/**
	 * Открывает экран редактирования книги, если у предмета есть компонент записываемого содержимого.
	 */
	@Override
	public void useBook(ItemStack book, Hand hand) {
		WritableBookContentComponent content = book.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
		if (content == null) {
			return;
		}

		client.setScreen(new BookEditScreen(this, book, hand, content));
	}

	/**
	 * Добавляет критические частицы над целью.
	 */
	@Override
	public void addCritParticles(Entity target) {
		client.particleManager.addEmitter(target, ParticleTypes.CRIT);
	}

	/**
	 * Добавляет частицы зачарованного удара над целью.
	 */
	@Override
	public void addEnchantedHitParticles(Entity target) {
		client.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT);
	}

	/**
	 * Возвращает, зажата ли клавиша приседания.
	 */
	@Override
	public boolean isSneaking() {
		return input.playerInput.sneak();
	}

	/**
	 * Возвращает, находится ли игрок в позе приседания.
	 */
	@Override
	public boolean isInSneakingPose() {
		return inSneakingPose;
	}

	/**
	 * Возвращает, должен ли игрок замедляться (приседание или ползание).
	 */
	public boolean shouldSlowDown() {
		return isInSneakingPose() || isCrawling();
	}

	/**
	 * Обновляет входные данные движения: для камеры применяет множители скорости и интерполирует углы рендера.
	 */
	@Override
	public void tickMovementInput() {
		if (isCamera() == false) {
			super.tickMovementInput();
			return;
		}

		Vec2f movement = applyMovementSpeedFactors(input.getMovementInput());
		sidewaysSpeed = movement.x;
		forwardSpeed = movement.y;
		jumping = input.playerInput.jump();
		lastRenderYaw = renderYaw;
		lastRenderPitch = renderPitch;
		renderPitch = renderPitch + (getPitch() - renderPitch) * 0.5F;
		renderYaw = renderYaw + (getYaw() - renderYaw) * 0.5F;
	}

	private Vec2f applyMovementSpeedFactors(Vec2f rawInput) {
		if (rawInput.lengthSquared() == 0.0F) {
			return rawInput;
		}

		Vec2f scaled = rawInput.multiply(0.98F);

		if (isUsingItem() && hasVehicle() == false) {
			scaled = scaled.multiply(getActiveItemSpeedMultiplier());
		}

		if (shouldSlowDown()) {
			float sneakSpeed = (float) getAttributeValue(EntityAttributes.SNEAKING_SPEED);
			scaled = scaled.multiply(sneakSpeed);
		}

		return applyDirectionalMovementSpeedFactors(scaled);
	}

	private static Vec2f applyDirectionalMovementSpeedFactors(Vec2f vec) {
		float length = vec.length();
		if (length <= 0.0F) {
			return vec;
		}

		Vec2f normalized = vec.multiply(1.0F / length);
		float multiplier = getDirectionalMovementSpeedMultiplier(normalized);
		float clamped = Math.min(length * multiplier, 1.0F);
		return normalized.multiply(clamped);
	}

	/**
	 * Вычисляет множитель скорости в зависимости от направления движения (диагональное vs прямое).
	 */
	private static float getDirectionalMovementSpeedMultiplier(Vec2f vec) {
		float absX = Math.abs(vec.x);
		float absY = Math.abs(vec.y);
		float ratio = absY > absX ? absX / absY : absY / absX;
		return MathHelper.sqrt(1.0F + MathHelper.square(ratio));
	}

	/**
	 * Возвращает, является ли этот игрок текущей камерой.
	 */
	protected boolean isCamera() {
		return client.getCameraEntity() == this;
	}

	/**
	 * Инициализирует игрока при спавне: находит свободную позицию, сбрасывает скорость и здоровье.
	 */
	public void init() {
		setPose(EntityPose.STANDING);

		if (getEntityWorld() != null) {
			for (double y = getY();
			     y > getEntityWorld().getBottomY() && y <= getEntityWorld().getTopYInclusive();
			     y++
			) {
				setPosition(getX(), y, getZ());
				if (getEntityWorld().isSpaceEmpty(this)) {
					break;
				}
			}

			setVelocity(Vec3d.ZERO);
			setPitch(0.0F);
		}

		setHealth(getMaxHealth());
		deathTime = 0;
	}

	/**
	 * Обновляет движение игрока: спринт, полёт, прыжок на верховом животном, видимость под водой.
	 */
	@Override
	public void tickMovement() {
		if (ticksLeftToDoubleTapSprint > 0) {
			ticksLeftToDoubleTapSprint--;
		}

		if ((client.currentScreen instanceof LevelLoadingScreen) == false) {
			tickNausea(getCurrentPortalEffect() == Portal.Effect.CONFUSION);
			tickPortalCooldown();
		}

		boolean wasJumping = input.playerInput.jump();
		boolean isSneaking = input.playerInput.sneak();
		boolean hasForward = input.hasForwardMovement();
		PlayerAbilities abilities = getAbilities();

		inSneakingPose = abilities.flying == false
				&& isSwimming() == false
				&& hasVehicle() == false
				&& canChangeIntoPose(EntityPose.CROUCHING)
				&& (isSneaking() || isSleeping() == false && canChangeIntoPose(EntityPose.STANDING) == false);

		input.tick();
		client.getTutorialManager().onMovement(input);

		boolean autoJumping = false;
		if (ticksToNextAutoJump > 0) {
			ticksToNextAutoJump--;
			autoJumping = true;
			input.jump();
		}

		if (noClip == false) {
			double halfWidth = getWidth() * 0.35;
			pushOutOfBlocks(getX() - halfWidth, getZ() + halfWidth);
			pushOutOfBlocks(getX() - halfWidth, getZ() - halfWidth);
			pushOutOfBlocks(getX() + halfWidth, getZ() - halfWidth);
			pushOutOfBlocks(getX() + halfWidth, getZ() + halfWidth);
		}

		if (isSneaking || isBlockedFromSprinting() && hasVehicle() == false || input.playerInput.backward()) {
			ticksLeftToDoubleTapSprint = 0;
		}

		if (canStartSprinting()) {
			if (hasForward == false) {
				if (ticksLeftToDoubleTapSprint > 0) {
					setSprinting(true);
				}
				else {
					ticksLeftToDoubleTapSprint = client.options.getSprintWindow().getValue();
				}
			}

			if (input.playerInput.sprint()) {
				setSprinting(true);
			}
		}

		if (isSprinting()) {
			if (isSwimming()) {
				if (shouldStopSwimSprinting()) {
					setSprinting(false);
				}
			}
			else if (shouldStopSprinting()) {
				setSprinting(false);
			}
		}

		boolean flyingToggled = false;
		if (abilities.allowFlying) {
			if (client.interactionManager.isFlyingLocked()) {
				if (abilities.flying == false) {
					abilities.flying = true;
					flyingToggled = true;
					sendAbilitiesUpdate();
				}
			}
			else if (wasJumping == false && input.playerInput.jump() && autoJumping == false) {
				if (abilityResyncCountdown == 0) {
					abilityResyncCountdown = 7;
				}
				else if (isSwimming() == false
						&& (getVehicle() == null || getJumpingMount() != null)
				) {
					abilities.flying = abilities.flying == false;
					if (abilities.flying && isOnGround()) {
						jump();
					}

					flyingToggled = true;
					sendAbilitiesUpdate();
					abilityResyncCountdown = 0;
				}
			}
		}

		if (input.playerInput.jump() && flyingToggled == false && wasJumping == false
				&& isClimbing() == false && checkGliding()
		) {
			networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
		}

		falling = isGliding();

		if (isTouchingWater() && input.playerInput.sneak() && shouldSwimInFluids()) {
			knockDownwards();
		}

		if (isSubmergedIn(FluidTags.WATER)) {
			int visibilityIncrement = isSpectator() ? 10 : 1;
			underwaterVisibilityTicks = MathHelper.clamp(
					underwaterVisibilityTicks + visibilityIncrement,
					0,
					UNDERWATER_VISIBILITY_MAX
			);
		}
		else if (underwaterVisibilityTicks > 0) {
			isSubmergedIn(FluidTags.WATER);
			underwaterVisibilityTicks = MathHelper.clamp(underwaterVisibilityTicks - 10, 0, UNDERWATER_VISIBILITY_MAX);
		}

		if (abilities.flying && isCamera()) {
			int verticalDir = 0;
			if (input.playerInput.sneak()) {
				verticalDir--;
			}

			if (input.playerInput.jump()) {
				verticalDir++;
			}

			if (verticalDir != 0) {
				setVelocity(getVelocity().add(0.0, verticalDir * abilities.getFlySpeed() * 3.0F, 0.0));
			}
		}

		JumpingMount jumpingMount = getJumpingMount();
		if (jumpingMount != null && jumpingMount.getJumpCooldown() == 0) {
			if (mountJumpTicks < 0) {
				mountJumpTicks++;
				if (mountJumpTicks == 0) {
					mountJumpStrength = 0.0F;
				}
			}

			if (wasJumping && input.playerInput.jump() == false) {
				mountJumpTicks = -10;
				jumpingMount.setJumpStrength(MathHelper.floor(getMountJumpStrength() * 100.0F));
				startRidingJump();
			}
			else if (wasJumping == false && input.playerInput.jump()) {
				mountJumpTicks = 0;
				mountJumpStrength = 0.0F;
			}
			else if (wasJumping) {
				mountJumpTicks++;
				mountJumpStrength = mountJumpTicks < 10
				                    ? mountJumpTicks * 0.1F
				                    : 0.8F + 2.0F / (mountJumpTicks - 9) * 0.1F;
			}
		}
		else {
			mountJumpStrength = 0.0F;
		}

		super.tickMovement();

		if (isOnGround() && abilities.flying && client.interactionManager.isFlyingLocked() == false) {
			abilities.flying = false;
			sendAbilitiesUpdate();
		}
	}

	private boolean shouldStopSprinting() {
		return canSprint(getAbilities().flying) == false
				|| input.hasForwardMovement() == false
				|| horizontalCollision && collidedSoftly == false;
	}

	private boolean shouldStopSwimSprinting() {
		return canSprint(true) == false
				|| isTouchingWater() == false
				|| input.hasForwardMovement() == false && isOnGround() == false && input.playerInput.sneak() == false;
	}

	/**
	 * Возвращает текущий эффект портала, или {@link Portal.Effect#NONE} если менеджер портала не инициализирован.
	 */
	public Portal.Effect getCurrentPortalEffect() {
		return portalManager == null ? Portal.Effect.NONE : portalManager.getEffect();
	}

	/**
	 * Обновляет таймер смерти и удаляет сущность после 20 тиков.
	 */
	@Override
	protected void updatePostDeath() {
		deathTime++;
		if (deathTime == 20) {
			remove(Entity.RemovalReason.KILLED);
		}
	}

	/**
	 * Обновляет интенсивность тошноты от портала: нарастает при нахождении в портале, убывает вне его.
	 */
	private void tickNausea(boolean fromPortalEffect) {
		lastNauseaIntensity = nauseaIntensity;
		float delta = 0.0F;

		if (fromPortalEffect && portalManager != null && portalManager.isInPortal()) {
			if (client.currentScreen != null && client.currentScreen.keepOpenThroughPortal() == false) {
				if (client.currentScreen instanceof HandledScreen) {
					closeHandledScreen();
				}

				client.setScreen(null);
			}

			if (nauseaIntensity == 0.0F) {
				client.getSoundManager().play(PositionedSoundInstance.ambient(
						SoundEvents.BLOCK_PORTAL_TRIGGER,
						random.nextFloat() * 0.4F + 0.8F,
						0.25F
				));
			}

			delta = 0.0125F;
			portalManager.setInPortal(false);
		}
		else if (nauseaIntensity > 0.0F) {
			delta = -0.05F;
		}

		nauseaIntensity = MathHelper.clamp(nauseaIntensity + delta, 0.0F, 1.0F);
	}

	/**
	 * Обновляет езду: передаёт управляющие входы лодке и обновляет флаг активного управления.
	 */
	@Override
	public void tickRiding() {
		super.tickRiding();
		riding = false;

		if (getControllingVehicle() instanceof AbstractBoatEntity boat) {
			boat.setInputs(
					input.playerInput.left(),
					input.playerInput.right(),
					input.playerInput.forward(),
					input.playerInput.backward()
			);
			riding = input.playerInput.left()
					|| input.playerInput.right()
					|| input.playerInput.forward()
					|| input.playerInput.backward();
		}
	}

	/**
	 * Возвращает, управляет ли игрок транспортным средством в данный момент.
	 */
	public boolean isRiding() {
		return riding;
	}

	/**
	 * Перемещает игрока и обновляет пройденное расстояние, а также проверяет автопрыжок.
	 */
	@Override
	public void move(MovementType type, Vec3d movement) {
		double prevX = getX();
		double prevZ = getZ();
		super.move(type, movement);
		float deltaX = (float) (getX() - prevX);
		float deltaZ = (float) (getZ() - prevZ);
		autoJump(deltaX, deltaZ);
		addDistanceMoved(MathHelper.hypot(deltaX, deltaZ) * 0.6F);
	}

	/**
	 * Возвращает, включён ли автопрыжок.
	 */
	public boolean isAutoJumpEnabled() {
		return autoJumpEnabled;
	}

	/**
	 * Возвращает, должен ли игрок поворачиваться вместе с вагонеткой.
	 */
	@Override
	public boolean shouldRotateWithMinecart() {
		return client.options.getRotateWithMinecart().getValue();
	}

	/**
	 * Проверяет возможность автопрыжка через препятствие и планирует его на следующий тик.
	 */
	protected void autoJump(float dx, float dz) {
		if (shouldAutoJump() == false) {
			return;
		}

		Vec3d pos = getEntityPos();
		Vec3d targetPos = pos.add(dx, 0.0, dz);
		Vec3d moveDir = new Vec3d(dx, 0.0, dz);
		float speed = getMovementSpeed();
		float lengthSq = (float) moveDir.lengthSquared();

		if (lengthSq <= 0.001F) {
			Vec2f movInput = input.getMovementInput();
			float inputX = speed * movInput.x;
			float inputY = speed * movInput.y;
			float sinYaw = MathHelper.sin(getYaw() * (float) (Math.PI / 180.0));
			float cosYaw = MathHelper.cos(getYaw() * (float) (Math.PI / 180.0));
			moveDir = new Vec3d(inputX * cosYaw - inputY * sinYaw, moveDir.y, inputY * cosYaw + inputX * sinYaw);
			lengthSq = (float) moveDir.lengthSquared();
			if (lengthSq <= 0.001F) {
				return;
			}
		}

		float invLength = MathHelper.inverseSqrt(lengthSq);
		Vec3d normalDir = moveDir.multiply(invLength);
		Vec3d lookDir = getRotationVecClient();
		float dotProduct = (float) (lookDir.x * normalDir.x + lookDir.z * normalDir.z);

		if (dotProduct < -0.15F) {
			return;
		}

		ShapeContext shapeContext = ShapeContext.of(this);
		BlockPos headPos = BlockPos.ofFloored(getX(), getBoundingBox().maxY, getZ());
		BlockState headBlock = getEntityWorld().getBlockState(headPos);

		if (headBlock.getCollisionShape(getEntityWorld(), headPos, shapeContext).isEmpty() == false) {
			return;
		}

		BlockPos aboveHead = headPos.up();
		BlockState aboveHeadBlock = getEntityWorld().getBlockState(aboveHead);

		if (aboveHeadBlock.getCollisionShape(getEntityWorld(), aboveHead, shapeContext).isEmpty() == false) {
			return;
		}

		float maxJumpHeight = 1.2F;
		if (hasStatusEffect(StatusEffects.JUMP_BOOST)) {
			maxJumpHeight += (getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
		}

		float lookAhead = Math.max(speed * 7.0F, 1.0F / invLength);
		Vec3d lookAheadPos = targetPos.add(normalDir.multiply(lookAhead));
		float width = getWidth();
		float height = getHeight();
		Box sweepBox = new Box(pos, lookAheadPos.add(0.0, height, 0.0)).expand(width, 0.0, width);

		Vec3d leftEdge = pos.add(0.0, 0.51F, 0.0);
		Vec3d rightEdge = lookAheadPos.add(0.0, 0.51F, 0.0);
		Vec3d perpDir = normalDir.crossProduct(new Vec3d(0.0, 1.0, 0.0));
		Vec3d halfWidth = perpDir.multiply(width * 0.5F);

		Vec3d leftNear = leftEdge.subtract(halfWidth);
		Vec3d leftFar = rightEdge.subtract(halfWidth);
		Vec3d rightNear = leftEdge.add(halfWidth);
		Vec3d rightFar = rightEdge.add(halfWidth);

		Iterator<Box> collisionBoxes = StreamSupport
				.stream(getEntityWorld().getCollisions(this, sweepBox).spliterator(), false)
				.flatMap(shape -> shape.getBoundingBoxes().stream())
				.iterator();

		float maxObstacleY = Float.MIN_VALUE;

		while (collisionBoxes.hasNext()) {
			Box obstacle = collisionBoxes.next();
			if (obstacle.intersects(leftNear, leftFar) == false && obstacle.intersects(rightNear, rightFar) == false) {
				continue;
			}

			maxObstacleY = (float) obstacle.maxY;
			BlockPos obstaclePos = BlockPos.ofFloored(obstacle.getCenter());

			for (int step = 1; step < maxJumpHeight; step++) {
				BlockPos checkPos = obstaclePos.up(step);
				BlockState checkBlock = getEntityWorld().getBlockState(checkPos);
				VoxelShape checkShape = checkBlock.getCollisionShape(getEntityWorld(), checkPos, shapeContext);

				if (checkShape.isEmpty() == false) {
					maxObstacleY = (float) checkShape.getMax(Direction.Axis.Y) + checkPos.getY();
					if (maxObstacleY - getY() > maxJumpHeight) {
						return;
					}
				}

				if (step > 1) {
					headPos = headPos.up();
					BlockState headCheck = getEntityWorld().getBlockState(headPos);
					if (headCheck.getCollisionShape(getEntityWorld(), headPos, shapeContext).isEmpty() == false) {
						return;
					}
				}
			}

			break;
		}

		if (maxObstacleY != Float.MIN_VALUE) {
			float heightDiff = (float) (maxObstacleY - getY());
			if (heightDiff > 0.5F && heightDiff <= maxJumpHeight) {
				ticksToNextAutoJump = 1;
			}
		}
	}

	/**
	 * Проверяет, произошло ли мягкое столкновение: угол между направлением движения и фактическим смещением
	 * меньше порогового значения {@link #MAX_SOFT_COLLISION_RADIANS}.
	 */
	@Override
	protected boolean hasCollidedSoftly(Vec3d adjustedMovement) {
		float yawRad = getYaw() * (float) (Math.PI / 180.0);
		double sinYaw = MathHelper.sin(yawRad);
		double cosYaw = MathHelper.cos(yawRad);
		double worldX = sidewaysSpeed * cosYaw - forwardSpeed * sinYaw;
		double worldZ = forwardSpeed * cosYaw + sidewaysSpeed * sinYaw;
		double inputLenSq = MathHelper.square(worldX) + MathHelper.square(worldZ);
		double moveLenSq = MathHelper.square(adjustedMovement.x) + MathHelper.square(adjustedMovement.z);

		if (inputLenSq < 1.0E-5F || moveLenSq < 1.0E-5F) {
			return false;
		}

		double dot = worldX * adjustedMovement.x + worldZ * adjustedMovement.z;
		double angle = Math.acos(dot / Math.sqrt(inputLenSq * moveLenSq));
		return angle < MAX_SOFT_COLLISION_RADIANS;
	}

	private boolean shouldAutoJump() {
		return isAutoJumpEnabled()
				&& ticksToNextAutoJump <= 0
				&& isOnGround()
				&& clipAtLedge() == false
				&& hasVehicle() == false
				&& hasMovementInput()
				&& getJumpVelocityMultiplier() >= 1.0;
	}

	private boolean hasMovementInput() {
		return input.getMovementInput().lengthSquared() > 0.0F;
	}

	private boolean canSprint(boolean allowTouchingWater) {
		return hasBlindnessEffect() == false
				&& (hasVehicle() ? canVehicleSprint(getVehicle()) : canSprintOrFly())
				&& (allowTouchingWater || isPartlyTouchingWater() == false);
	}

	private boolean canStartSprinting() {
		return isSprinting() == false
				&& input.hasForwardMovement()
				&& canSprint(getAbilities().flying)
				&& isBlockedFromSprinting() == false
				&& (isGliding() == false || isSubmergedInWater())
				&& (shouldSlowDown() == false || isSubmergedInWater());
	}

	private boolean canVehicleSprint(Entity vehicle) {
		return vehicle.canSprintAsVehicle() && vehicle.isLogicalSideForUpdatingMovement();
	}

	/**
	 * Возвращает коэффициент видимости под водой от 0.0 до 1.0 в зависимости от времени нахождения под водой.
	 */
	public float getUnderwaterVisibility() {
		if (isSubmergedIn(FluidTags.WATER) == false) {
			return 0.0F;
		}

		if (underwaterVisibilityTicks >= UNDERWATER_VISIBILITY_MAX) {
			return 1.0F;
		}

		float rampFactor = MathHelper.clamp(underwaterVisibilityTicks / (float) UNDERWATER_VISIBILITY_RAMP, 0.0F, 1.0F);
		float fullFactor = underwaterVisibilityTicks < UNDERWATER_VISIBILITY_RAMP
		                   ? 0.0F
		                   : MathHelper.clamp(
				                   (underwaterVisibilityTicks - UNDERWATER_VISIBILITY_RAMP) / (float) (
						                   UNDERWATER_VISIBILITY_MAX - UNDERWATER_VISIBILITY_RAMP
				                   ),
				                   0.0F,
				                   1.0F
		                   );
		return rampFactor * 0.6F + fullFactor * 0.4F;
	}

	/**
	 * Реагирует на смену режима игры: в режиме наблюдателя обнуляет вертикальную скорость.
	 */
	public void onGameModeChanged(GameMode gameMode) {
		if (gameMode == GameMode.SPECTATOR) {
			setVelocity(getVelocity().withAxis(Direction.Axis.Y, 0.0));
		}
	}

	/**
	 * Возвращает, погружён ли игрок в воду.
	 */
	@Override
	public boolean isSubmergedInWater() {
		return isSubmergedInWater;
	}

	/**
	 * Обновляет состояние погружения в воду и воспроизводит звуки входа/выхода из воды.
	 */
	@Override
	protected boolean updateWaterSubmersionState() {
		boolean wasSubmerged = isSubmergedInWater;
		boolean nowSubmerged = super.updateWaterSubmersionState();

		if (isSpectator()) {
			return isSubmergedInWater;
		}

		if (wasSubmerged == false && nowSubmerged) {
			getEntityWorld().playSoundClient(
					getX(), getY(), getZ(),
					SoundEvents.AMBIENT_UNDERWATER_ENTER,
					SoundCategory.AMBIENT,
					1.0F, 1.0F, false
			);
			client.getSoundManager().play(new AmbientSoundLoops.Underwater(this));
		}

		if (wasSubmerged && nowSubmerged == false) {
			getEntityWorld().playSoundClient(
					getX(), getY(), getZ(),
					SoundEvents.AMBIENT_UNDERWATER_EXIT,
					SoundCategory.AMBIENT,
					1.0F, 1.0F, false
			);
		}

		return isSubmergedInWater;
	}

	/**
	 * Возвращает позицию привязи: в режиме от первого лица — рассчитывается относительно камеры.
	 */
	@Override
	public Vec3d getLeashPos(float tickProgress) {
		if (client.options.getPerspective().isFirstPerson() == false) {
			return super.getLeashPos(tickProgress);
		}

		float yawRad = MathHelper.lerp(tickProgress * 0.5F, getYaw(), lastYaw) * (float) (Math.PI / 180.0);
		float pitchRad = MathHelper.lerp(tickProgress * 0.5F, getPitch(), lastPitch) * (float) (Math.PI / 180.0);
		double armSide = getMainArm() == Arm.RIGHT ? -1.0 : 1.0;
		Vec3d offset = new Vec3d(0.39 * armSide, -0.6, 0.3);
		return offset.rotateX(-pitchRad).rotateY(-yawRad).add(getCameraPosVec(tickProgress));
	}

	/**
	 * Уведомляет менеджер обучения о клике по слоту подбора предмета.
	 */
	@Override
	public void onPickupSlotClick(ItemStack cursorStack, ItemStack slotStack, ClickType clickType) {
		client.getTutorialManager().onPickupSlotClick(cursorStack, slotStack, clickType);
	}

	/**
	 * Возвращает угол поворота тела, равный углу поворота взгляда.
	 */
	@Override
	public float getBodyYaw() {
		return getYaw();
	}

	/**
	 * Выбрасывает предмет из творческого инвентаря через менеджер взаимодействий.
	 */
	@Override
	public void dropCreativeStack(ItemStack stack) {
		client.interactionManager.dropCreativeStack(stack);
	}

	/**
	 * Возвращает, можно ли сейчас выбросить предмет (кулдаун истёк).
	 */
	@Override
	public boolean canDropItems() {
		return itemDropCooldown.canUse();
	}

	/**
	 * Возвращает объект кулдауна выброса предметов.
	 */
	public Cooldown getItemDropCooldown() {
		return itemDropCooldown;
	}

	/**
	 * Возвращает последний зафиксированный ввод игрока.
	 */
	public PlayerInput getLastPlayerInput() {
		return lastPlayerInput;
	}

	/**
	 * Вычисляет результат попадания с учётом компонента дальности атаки предмета в руке.
	 * Если компонент задаёт дальность — используется он, иначе стандартный рейкаст.
	 */
	public HitResult computeHitResult(float tickDelta, Entity entity) {
		ItemStack heldStack = getActiveOrMainHandStack();
		AttackRangeComponent attackRange = heldStack.get(DataComponentTypes.ATTACK_RANGE);
		double blockRange = getBlockInteractionRange();
		HitResult hitResult = null;

		if (attackRange != null) {
			hitResult = attackRange.getHitResult(entity, tickDelta, EntityPredicates.CAN_HIT);
			if (hitResult instanceof BlockHitResult) {
				hitResult = clampHitResultToRange(hitResult, entity.getCameraPosVec(tickDelta), blockRange);
			}
		}

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			double entityRange = getEntityInteractionRange();
			hitResult = raycastWithEntityDetection(entity, blockRange, entityRange, tickDelta);
		}

		return hitResult;
	}

	private static HitResult raycastWithEntityDetection(
			Entity entity,
			double blockRange,
			double entityRange,
			float tickDelta
	) {
		double maxRange = Math.max(blockRange, entityRange);
		double maxRangeSq = MathHelper.square(maxRange);
		Vec3d cameraPos = entity.getCameraPosVec(tickDelta);
		HitResult blockHit = entity.raycast(maxRange, tickDelta, false);
		double blockHitDistSq = blockHit.getPos().squaredDistanceTo(cameraPos);

		if (blockHit.getType() != HitResult.Type.MISS) {
			maxRangeSq = blockHitDistSq;
			maxRange = Math.sqrt(blockHitDistSq);
		}

		Vec3d lookVec = entity.getRotationVec(tickDelta);
		Vec3d endPos = cameraPos.add(lookVec.x * maxRange, lookVec.y * maxRange, lookVec.z * maxRange);
		Box sweepBox = entity.getBoundingBox().stretch(lookVec.multiply(maxRange)).expand(1.0, 1.0, 1.0);
		EntityHitResult
				entityHit =
				ProjectileUtil.raycast(entity, cameraPos, endPos, sweepBox, EntityPredicates.CAN_HIT, maxRangeSq);

		return entityHit != null && entityHit.getPos().squaredDistanceTo(cameraPos) < blockHitDistSq
		       ? clampHitResultToRange(entityHit, cameraPos, entityRange)
		       : clampHitResultToRange(blockHit, cameraPos, blockRange);
	}

	private static HitResult clampHitResultToRange(HitResult hitResult, Vec3d origin, double range) {
		Vec3d hitPos = hitResult.getPos();
		if (hitPos.isInRange(origin, range)) {
			return hitResult;
		}

		Direction direction = Direction.getFacing(hitPos.x - origin.x, hitPos.y - origin.y, hitPos.z - origin.z);
		return BlockHitResult.createMissed(hitPos, direction, BlockPos.ofFloored(hitPos));
	}
}
