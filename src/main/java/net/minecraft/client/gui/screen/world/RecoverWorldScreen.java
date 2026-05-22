package net.minecraft.client.gui.screen.world;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.nbt.NbtCrashException;
import net.minecraft.nbt.NbtException;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Urls;
import net.minecraft.world.level.storage.LevelStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Экран восстановления повреждённого мира из резервной копии {@code level.dat_old}.
 * Отображает статус обоих файлов сохранения и предлагает восстановить мир, если это возможно.
 */
@Environment(EnvType.CLIENT)
public class RecoverWorldScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int BUTTON_WIDTH = 120;
	private static final Text TITLE_TEXT = Text.translatable("recover_world.title").formatted(Formatting.BOLD);
	private static final Text BUG_TRACKER_TEXT = Text.translatable("recover_world.bug_tracker");
	private static final Text RESTORE_TEXT = Text.translatable("recover_world.restore");
	private static final Text NO_FALLBACK_TEXT = Text.translatable("recover_world.no_fallback");
	private static final Text DONE_TITLE_TEXT = Text.translatable("recover_world.done.title");
	private static final Text DONE_SUCCESS_TEXT = Text.translatable("recover_world.done.success");
	private static final Text DONE_FAILED_TEXT = Text.translatable("recover_world.done.failed");
	private static final Text ISSUE_NONE_TEXT = Text.translatable("recover_world.issue.none").formatted(Formatting.GREEN);
	private static final Text MISSING_FILE_TEXT = Text.translatable("recover_world.issue.missing_file").formatted(Formatting.RED);

	private final BooleanConsumer callback;
	private final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(8);
	private final Text message;
	private final MultilineTextWidget messageWidget;
	private final MultilineTextWidget exceptionWidget;
	private final LevelStorage.Session session;

	public RecoverWorldScreen(MinecraftClient client, BooleanConsumer callback, LevelStorage.Session session) {
		super(TITLE_TEXT);
		this.callback = callback;
		this.session = session;
		this.message = Text.translatable(
				"recover_world.message",
				Text.literal(session.getDirectoryName()).formatted(Formatting.GRAY)
		);
		this.messageWidget = new MultilineTextWidget(message, client.textRenderer);

		Exception currentException = getLoadingException(session, false);
		Exception oldException = getLoadingException(session, true);
		Text statusText = Text.empty()
				.append(toText(session, false, currentException))
				.append("\n")
				.append(toText(session, true, oldException));
		this.exceptionWidget = new MultilineTextWidget(statusText, client.textRenderer);

		boolean canRestore = currentException != null && oldException == null;

		layout.getMainPositioner().alignHorizontalCenter();
		layout.add(new TextWidget(title, client.textRenderer));
		layout.add(messageWidget.setCentered(true));
		layout.add(exceptionWidget);

		DirectionalLayoutWidget buttonRow = DirectionalLayoutWidget.horizontal().spacing(5);
		buttonRow.add(ButtonWidget
				.builder(BUG_TRACKER_TEXT, ConfirmLinkScreen.opening(this, Urls.SNAPSHOT_BUGS))
				.size(BUTTON_WIDTH, 20)
				.build());

		ButtonWidget restoreButton = buttonRow.add(
				ButtonWidget
						.builder(RESTORE_TEXT, button -> tryRestore(client))
						.size(BUTTON_WIDTH, 20)
						.tooltip(canRestore ? null : Tooltip.of(NO_FALLBACK_TEXT))
						.build()
		);
		restoreButton.active = canRestore;

		layout.add(buttonRow);
		layout.add(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).size(BUTTON_WIDTH, 20).build());
		layout.forEachChild(this::addDrawableChild);
	}

	private void tryRestore(MinecraftClient client) {
		Exception currentException = getLoadingException(session, false);
		Exception oldException = getLoadingException(session, true);

		if (currentException == null || oldException != null) {
			LOGGER.error(
					"Failed to recover world, files not as expected. level.dat: {}, level.dat_old: {}",
					currentException != null ? currentException.getMessage() : "no issues",
					oldException != null ? oldException.getMessage() : "no issues"
			);
			client.setScreen(new NoticeScreen(() -> callback.accept(false), DONE_TITLE_TEXT, DONE_FAILED_TEXT));
			return;
		}

		client.setScreenAndRender(new MessageScreen(Text.translatable("recover_world.restoring")));
		EditWorldScreen.backupLevel(session);

		if (session.tryRestoreBackup()) {
			client.setScreen(new ConfirmScreen(
					callback,
					DONE_TITLE_TEXT,
					DONE_SUCCESS_TEXT,
					ScreenTexts.CONTINUE,
					ScreenTexts.BACK
			));
		}
		else {
			client.setScreen(new NoticeScreen(
					() -> callback.accept(false),
					DONE_TITLE_TEXT,
					DONE_FAILED_TEXT
			));
		}
	}

	private Text toText(LevelStorage.Session session, boolean old, @Nullable Exception exception) {
		if (old && exception instanceof FileNotFoundException) {
			return Text.empty();
		}

		MutableText result = Text.empty();
		Instant lastModified = session.getLastModifiedTime(old);
		MutableText dateText = lastModified != null
				? Text.literal(WorldListWidget.DATE_FORMAT.format(ZonedDateTime.ofInstant(lastModified, ZoneId.systemDefault())))
				: Text.translatable("recover_world.state_entry.unknown");

		result.append(Text.translatable("recover_world.state_entry", dateText.formatted(Formatting.GRAY)));

		if (exception == null) {
			result.append(ISSUE_NONE_TEXT);
		}
		else if (exception instanceof FileNotFoundException) {
			result.append(MISSING_FILE_TEXT);
		}
		else if (exception instanceof NbtCrashException) {
			result.append(Text.literal(exception.getCause().toString()).formatted(Formatting.RED));
		}
		else {
			result.append(Text.literal(exception.toString()).formatted(Formatting.RED));
		}

		return result;
	}

	private @Nullable Exception getLoadingException(LevelStorage.Session session, boolean old) {
		try {
			if (old) {
				session.getLevelSummary(session.readOldLevelProperties());
			}
			else {
				session.getLevelSummary(session.readLevelProperties());
			}

			return null;
		}
		catch (NbtException | NbtCrashException | IOException exception) {
			return exception;
		}
	}

	@Override
	protected void init() {
		super.init();
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		exceptionWidget.setMaxWidth(width - 50);
		messageWidget.setMaxWidth(width - 50);
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), message);
	}

	@Override
	public void close() {
		callback.accept(false);
	}
}
