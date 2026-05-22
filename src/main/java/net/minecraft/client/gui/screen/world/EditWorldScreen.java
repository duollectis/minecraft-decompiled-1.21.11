package net.minecraft.client.gui.screen.world;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.nbt.NbtCrashException;
import net.minecraft.nbt.NbtException;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Экран редактирования параметров существующего мира.
 * Позволяет переименовать мир, сбросить иконку, создать резервную копию и запустить оптимизацию.
 */
@Environment(EnvType.CLIENT)
public class EditWorldScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int BUTTON_WIDTH = 200;
	private static final int HALF_BUTTON_WIDTH = 98;
	private static final int BUTTON_SPACING = 4;
	private static final int TITLE_Y = 15;
	private static final Text ENTER_NAME_TEXT = Text.translatable("selectWorld.enterName").formatted(Formatting.GRAY);
	private static final Text RESET_ICON_TEXT = Text.translatable("selectWorld.edit.resetIcon");
	private static final Text OPEN_FOLDER_TEXT = Text.translatable("selectWorld.edit.openFolder");
	private static final Text BACKUP_TEXT = Text.translatable("selectWorld.edit.backup");
	private static final Text BACKUP_FOLDER_TEXT = Text.translatable("selectWorld.edit.backupFolder");
	private static final Text OPTIMIZE_TEXT = Text.translatable("selectWorld.edit.optimize");
	private static final Text CONFIRM_TITLE_TEXT = Text.translatable("optimizeWorld.confirm.title");
	private static final Text CONFIRM_DESCRIPTION_TEXT = Text.translatable("optimizeWorld.confirm.description");
	private static final Text CONFIRM_PROCEED_TEXT = Text.translatable("optimizeWorld.confirm.proceed");
	private static final Text SAVE_TEXT = Text.translatable("selectWorld.edit.save");

	private final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(5);
	private final BooleanConsumer callback;
	private final LevelStorage.Session storageSession;
	private final TextFieldWidget nameFieldWidget;

	public static EditWorldScreen create(MinecraftClient client, LevelStorage.Session session, BooleanConsumer callback)
	throws IOException {
		LevelSummary levelSummary = session.getLevelSummary(session.readLevelProperties());
		return new EditWorldScreen(client, session, levelSummary.getDisplayName(), callback);
	}

	private EditWorldScreen(
			MinecraftClient client,
			LevelStorage.Session session,
			String levelName,
			BooleanConsumer callback
	) {
		super(Text.translatable("selectWorld.edit.title"));
		this.callback = callback;
		this.storageSession = session;

		TextRenderer textRenderer = client.textRenderer;
		layout.add(new EmptyWidget(BUTTON_WIDTH, 20));
		layout.add(new TextWidget(ENTER_NAME_TEXT, textRenderer));

		nameFieldWidget = layout.add(new TextFieldWidget(textRenderer, BUTTON_WIDTH, 20, ENTER_NAME_TEXT));
		nameFieldWidget.setText(levelName);

		DirectionalLayoutWidget saveRow = DirectionalLayoutWidget.horizontal().spacing(BUTTON_SPACING);
		ButtonWidget saveButton = saveRow.add(
				ButtonWidget.builder(SAVE_TEXT, button -> commit(nameFieldWidget.getText())).width(HALF_BUTTON_WIDTH).build()
		);
		saveRow.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).width(HALF_BUTTON_WIDTH).build());
		nameFieldWidget.setChangedListener(name -> saveButton.active = !StringHelper.isBlank(name));

		layout.add(ButtonWidget.builder(
				RESET_ICON_TEXT, button -> {
					session.getIconFile().ifPresent(path -> FileUtils.deleteQuietly(path.toFile()));
					button.active = false;
				}
		).width(BUTTON_WIDTH).build()).active = session.getIconFile().filter(Files::isRegularFile).isPresent();

		layout.add(ButtonWidget
				.builder(
						OPEN_FOLDER_TEXT,
						button -> Util.getOperatingSystem().open(session.getDirectory(WorldSavePath.ROOT))
				)
				.width(BUTTON_WIDTH)
				.build());

		layout.add(ButtonWidget.builder(
				BACKUP_TEXT, button -> {
					boolean backupFailed = backupLevel(session);
					callback.accept(!backupFailed);
				}
		).width(BUTTON_WIDTH).build());

		layout.add(ButtonWidget.builder(
				BACKUP_FOLDER_TEXT, button -> {
					LevelStorage levelStorage = client.getLevelStorage();
					Path backupsDir = levelStorage.getBackupsDirectory();

					try {
						PathUtil.createDirectories(backupsDir);
					}
					catch (IOException exception) {
						throw new RuntimeException(exception);
					}

					Util.getOperatingSystem().open(backupsDir);
				}
		).width(BUTTON_WIDTH).build());

		layout.add(ButtonWidget.builder(
				OPTIMIZE_TEXT, button -> client.setScreen(new BackupPromptScreen(
						() -> client.setScreen(this),
						(backup, eraseCache) -> {
							if (backup) {
								backupLevel(session);
							}

							client.setScreen(OptimizeWorldScreen.create(
									client,
									callback,
									client.getDataFixer(),
									session,
									eraseCache
							));
						},
						CONFIRM_TITLE_TEXT,
						CONFIRM_DESCRIPTION_TEXT,
						CONFIRM_PROCEED_TEXT,
						true
				))
		).width(BUTTON_WIDTH).build());

		layout.add(new EmptyWidget(BUTTON_WIDTH, 20));
		layout.add(saveRow);
		layout.forEachChild(this::addDrawableChild);
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(nameFieldWidget);
	}

	@Override
	protected void init() {
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (nameFieldWidget.isFocused() && input.isEnter()) {
			commit(nameFieldWidget.getText());
			close();
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public void close() {
		callback.accept(false);
	}

	private void commit(String levelName) {
		try {
			storageSession.save(levelName);
		}
		catch (NbtException | NbtCrashException | IOException exception) {
			LOGGER.error("Failed to access world '{}'", storageSession.getDirectoryName(), exception);
			SystemToast.addWorldAccessFailureToast(client, storageSession.getDirectoryName());
		}

		callback.accept(true);
	}

	/**
	 * Создаёт резервную копию мира и отображает тост с результатом операции.
	 * Возвращает {@code true} если резервная копия была успешно создана.
	 */
	public static boolean backupLevel(LevelStorage.Session storageSession) {
		long backupSize = 0L;
		IOException backupException = null;

		try {
			backupSize = storageSession.createBackup();
		}
		catch (IOException exception) {
			backupException = exception;
		}

		if (backupException != null) {
			Text title = Text.translatable("selectWorld.edit.backupFailed");
			Text message = Text.literal(backupException.getMessage());
			MinecraftClient.getInstance().getToastManager().add(
					new SystemToast(SystemToast.Type.WORLD_BACKUP, title, message)
			);
			return false;
		}

		Text title = Text.translatable("selectWorld.edit.backupCreated", storageSession.getDirectoryName());
		Text message = Text.translatable("selectWorld.edit.backupSize", MathHelper.ceil(backupSize / 1048576.0));
		MinecraftClient.getInstance().getToastManager().add(
				new SystemToast(SystemToast.Type.WORLD_BACKUP, title, message)
		);
		return true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);
	}
}
