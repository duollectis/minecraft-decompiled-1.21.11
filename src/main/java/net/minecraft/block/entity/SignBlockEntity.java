package net.minecraft.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Блок-сущность таблички (Sign). Хранит текст двух сторон ({@code frontText} и {@code backText}),
 * поддерживает режим редактирования с привязкой к UUID игрока, восковую защиту от изменений
 * и выполнение команд по клику на текст.
 */
public class SignBlockEntity extends BlockEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_TEXT_WIDTH = 90;
	private static final int TEXT_LINE_HEIGHT = 10;
	private static final int LINE_COUNT = 4;
	private static final double EDIT_REACH_DISTANCE = 4.0;
	private @Nullable UUID editor;
	private SignText frontText;
	private SignText backText;
	private boolean waxed = false;

	public SignBlockEntity(BlockPos pos, BlockState state) {
		this(BlockEntityType.SIGN, pos, state);
	}

	public SignBlockEntity(BlockEntityType blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
		frontText = createText();
		backText = createText();
	}

	protected SignText createText() {
		return new SignText();
	}

	/**
	 * Определяет, смотрит ли игрок на лицевую сторону таблички.
	 * Вычисляется через угол между направлением таблички и вектором от таблички к игроку.
	 */
	public boolean isPlayerFacingFront(PlayerEntity player) {
		if (!(getCachedState().getBlock() instanceof AbstractSignBlock signBlock)) {
			return false;
		}

		Vec3d center = signBlock.getCenter(getCachedState());
		double deltaX = player.getX() - (getPos().getX() + center.x);
		double deltaZ = player.getZ() - (getPos().getZ() + center.z);
		float signRotation = signBlock.getRotationDegrees(getCachedState());
		float playerAngle = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0F / (float) Math.PI) - 90.0F;
		return MathHelper.angleBetween(signRotation, playerAngle) <= 90.0F;
	}

	public SignText getText(boolean front) {
		return front ? frontText : backText;
	}

	public SignText getFrontText() {
		return frontText;
	}

	public SignText getBackText() {
		return backText;
	}

	public int getTextLineHeight() {
		return TEXT_LINE_HEIGHT;
	}

	public int getMaxTextWidth() {
		return MAX_TEXT_WIDTH;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.put("front_text", SignText.CODEC, frontText);
		view.put("back_text", SignText.CODEC, backText);
		view.putBoolean("is_waxed", waxed);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		frontText = view.<SignText>read("front_text", SignText.CODEC).map(this::parseLines).orElseGet(SignText::new);
		backText = view.<SignText>read("back_text", SignText.CODEC).map(this::parseLines).orElseGet(SignText::new);
		waxed = view.getBoolean("is_waxed", false);
	}

	private SignText parseLines(SignText signText) {
		for (int line = 0; line < LINE_COUNT; line++) {
			Text raw = parseLine(signText.getMessage(line, false));
			Text filtered = parseLine(signText.getMessage(line, true));
			signText = signText.withMessage(line, raw, filtered);
		}

		return signText;
	}

	private Text parseLine(Text text) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return text;
		}

		try {
			return Texts.parse(createCommandSource(null, serverWorld, pos), text, null, 0);
		} catch (CommandSyntaxException ignored) {
			return text;
		}
	}

	/**
	 * Применяет список отфильтрованных сообщений к указанной стороне таблички.
	 * Разрешено только текущему редактору (по UUID) и только если табличка не запечатана воском.
	 */
	public void tryChangeText(PlayerEntity player, boolean front, List<FilteredMessage> messages) {
		if (isWaxed() || !player.getUuid().equals(getEditor()) || world == null) {
			LOGGER.warn("Player {} just tried to change non-editable sign", player.getStringifiedName());
			return;
		}

		changeText(text -> getTextWithMessages(player, messages, text), front);
		setEditor(null);
		world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}

	public boolean changeText(UnaryOperator<SignText> textChanger, boolean front) {
		return setText(textChanger.apply(getText(front)), front);
	}

	private SignText getTextWithMessages(PlayerEntity player, List<FilteredMessage> messages, SignText text) {
		for (int i = 0; i < messages.size(); i++) {
			FilteredMessage filteredMessage = messages.get(i);
			Style style = text.getMessage(i, player.shouldFilterText()).getStyle();
			if (player.shouldFilterText()) {
				text = text.withMessage(i, Text.literal(filteredMessage.getString()).setStyle(style));
			} else {
				text = text.withMessage(
					i,
					Text.literal(filteredMessage.raw()).setStyle(style),
					Text.literal(filteredMessage.getString()).setStyle(style)
				);
			}
		}

		return text;
	}

	public boolean setText(SignText text, boolean front) {
		return front ? setFrontText(text) : setBackText(text);
	}

	private boolean setBackText(SignText newBackText) {
		if (newBackText == backText) {
			return false;
		}

		backText = newBackText;
		updateListeners();
		return true;
	}

	private boolean setFrontText(SignText newFrontText) {
		if (newFrontText == frontText) {
			return false;
		}

		frontText = newFrontText;
		updateListeners();
		return true;
	}

	public boolean canRunCommandClickEvent(boolean front, PlayerEntity player) {
		return isWaxed() && getText(front).hasRunCommandClickEvent(player);
	}

	/**
	 * Выполняет все команды и диалоги, привязанные к кликабельным событиям строк таблички.
	 * Обрабатывает {@code RUN_COMMAND}, {@code ShowDialog} и кастомные действия.
	 *
	 * @return {@code true} если хотя бы одно событие было обработано
	 */
	public boolean runCommandClickEvent(ServerWorld world, PlayerEntity player, BlockPos pos, boolean front) {
		boolean executed = false;

		for (Text text : getText(front).getMessages(player.shouldFilterText())) {
			Style style = text.getStyle();
			switch (style.getClickEvent()) {
				case ClickEvent.RunCommand runCommand:
					world.getServer()
						.getCommandManager()
						.parseAndExecute(createCommandSource(player, world, pos), runCommand.command());
					executed = true;
					break;
				case ClickEvent.ShowDialog showDialog:
					player.openDialog(showDialog.dialog());
					executed = true;
					break;
				case ClickEvent.Custom custom:
					world.getServer().handleCustomClickAction(custom.id(), custom.payload());
					executed = true;
					break;
				case null:
				default:
			}
		}

		return executed;
	}

	private static ServerCommandSource createCommandSource(
		@Nullable PlayerEntity player,
		ServerWorld world,
		BlockPos pos
	) {
		String name = player == null ? "Sign" : player.getStringifiedName();
		Text displayName = player == null ? Text.literal("Sign") : player.getDisplayName();
		return new ServerCommandSource(
			CommandOutput.DUMMY,
			Vec3d.ofCenter(pos),
			Vec2f.ZERO,
			world,
			LeveledPermissionPredicate.GAMEMASTERS,
			name,
			displayName,
			world.getServer(),
			player
		);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	public void setEditor(@Nullable UUID editor) {
		this.editor = editor;
	}

	public @Nullable UUID getEditor() {
		return editor;
	}

	private void updateListeners() {
		markDirty();
		world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
	}

	public boolean isWaxed() {
		return waxed;
	}

	public boolean setWaxed(boolean newWaxed) {
		if (waxed == newWaxed) {
			return false;
		}

		waxed = newWaxed;
		updateListeners();
		return true;
	}

	public boolean isPlayerTooFarToEdit(UUID uuid) {
		PlayerEntity player = world.getPlayerByUuid(uuid);
		return player == null || !player.canInteractWithBlockAt(getPos(), EDIT_REACH_DISTANCE);
	}

	public static void tick(World world, BlockPos pos, BlockState state, SignBlockEntity blockEntity) {
		UUID editorUuid = blockEntity.getEditor();
		if (editorUuid == null) {
			return;
		}

		if (blockEntity.isPlayerTooFarToEdit(editorUuid)) {
			blockEntity.setEditor(null);
		}
	}

	public SoundEvent getInteractionFailSound() {
		return SoundEvents.BLOCK_SIGN_WAXED_INTERACT_FAIL;
	}
}
