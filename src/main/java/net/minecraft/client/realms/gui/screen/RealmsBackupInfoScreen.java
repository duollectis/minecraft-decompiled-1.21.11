package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.realms.dto.Backup;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsBackupInfoScreen}.
 */
public class RealmsBackupInfoScreen extends RealmsScreen {

	private static final Text TITLE = Text.translatable("mco.backup.info.title");
	private static final Text UNKNOWN = Text.translatable("mco.backup.unknown");
	private final Screen parent;
	final Backup backup;
	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private RealmsBackupInfoScreen.BackupInfoList backupInfoList;

	public RealmsBackupInfoScreen(Screen parent, Backup backup) {
		super(TITLE);
		this.parent = parent;
		this.backup = backup;
	}

	@Override
	public void init() {
		this.layout.addHeader(TITLE, this.textRenderer);
		this.backupInfoList = this.layout.addBody(new RealmsBackupInfoScreen.BackupInfoList(this.client));
		this.layout.addFooter(ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).build());
		this.refreshWidgetPositions();
		this.layout.forEachChild(child -> {
			ClickableWidget var10000 = this.addDrawableChild(child);
		});
	}

	@Override
	protected void refreshWidgetPositions() {
		this.backupInfoList.position(this.width, this.layout);
		this.layout.refreshPositions();
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	Text checkForSpecificMetadata(String key, String string) {
		String string2 = key.toLowerCase(Locale.ROOT);
		if (string2.contains("game") && string2.contains("mode")) {
			return this.gameModeMetadata(string);
		}
		else if (string2.contains("game") && string2.contains("difficulty")) {
			return this.gameDifficultyMetadata(string);
		}
		else {
			return (Text) (key.equals("world_type") ? this.getWorldTypeText(string) : Text.literal(string));
		}
	}

	private Text gameDifficultyMetadata(String value) {
		try {
			return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(value)).getTranslatableName();
		}
		catch (Exception var3) {
			return UNKNOWN;
		}
	}

	private Text gameModeMetadata(String value) {
		try {
			return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(value)).getSimpleTranslatableName();
		}
		catch (Exception var3) {
			return UNKNOWN;
		}
	}

	private Text getWorldTypeText(String string) {
		try {
			return RealmsServer.WorldType.valueOf(string.toUpperCase(Locale.ROOT)).getDisplayText();
		}
		catch (Exception var3) {
			return RealmsServer.WorldType.UNKNOWN.getDisplayText();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BackupInfoList}.
	 */
	class BackupInfoList extends AlwaysSelectedEntryListWidget<RealmsBackupInfoScreen.BackupInfoListEntry> {

		public BackupInfoList(final MinecraftClient client) {
			super(
					client,
					RealmsBackupInfoScreen.this.width,
					RealmsBackupInfoScreen.this.layout.getContentHeight(),
					RealmsBackupInfoScreen.this.layout.getHeaderHeight(),
					36
			);
			if (RealmsBackupInfoScreen.this.backup.changeList != null) {
				RealmsBackupInfoScreen.this.backup
						.changeList
						.forEach((key, value) -> this.addEntry(RealmsBackupInfoScreen.this.new BackupInfoListEntry(
								key,
								value
						)));
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BackupInfoListEntry}.
	 */
	class BackupInfoListEntry extends AlwaysSelectedEntryListWidget.Entry<RealmsBackupInfoScreen.BackupInfoListEntry> {

		private static final Text TEMPLATE_NAME_TEXT = Text.translatable("mco.backup.entry.templateName");
		private static final Text GAME_DIFFICULTY_TEXT = Text.translatable("mco.backup.entry.gameDifficulty");
		private static final Text NAME_TEXT = Text.translatable("mco.backup.entry.name");
		private static final Text GAME_SERVER_VERSION_TEXT = Text.translatable("mco.backup.entry.gameServerVersion");
		private static final Text UPLOADED_TEXT = Text.translatable("mco.backup.entry.uploaded");
		private static final Text ENABLED_PACK_TEXT = Text.translatable("mco.backup.entry.enabledPack");
		private static final Text DESCRIPTION_TEXT = Text.translatable("mco.backup.entry.description");
		private static final Text GAME_MODE_TEXT = Text.translatable("mco.backup.entry.gameMode");
		private static final Text SEED_TEXT = Text.translatable("mco.backup.entry.seed");
		private static final Text WORLD_TYPE_TEXT = Text.translatable("mco.backup.entry.worldType");
		private static final Text UNDEFINED_TEXT = Text.translatable("mco.backup.entry.undefined");
		private final String key;
		private final String value;
		private final Text keyText;
		private final Text valueText;

		public BackupInfoListEntry(final String key, final String value) {
			this.key = key;
			this.value = value;
			this.keyText = this.getTextFromKey(key);
			this.valueText = RealmsBackupInfoScreen.this.checkForSpecificMetadata(key, value);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawTextWithShadow(
					RealmsBackupInfoScreen.this.textRenderer,
					this.keyText,
					this.getContentX(),
					this.getContentY(),
					-6250336
			);
			context.drawTextWithShadow(
					RealmsBackupInfoScreen.this.textRenderer,
					this.valueText,
					this.getContentX(),
					this.getContentY() + 12,
					-1
			);
		}

		private Text getTextFromKey(String key) {
			return switch (key) {
				case "template_name" -> TEMPLATE_NAME_TEXT;
				case "game_difficulty" -> GAME_DIFFICULTY_TEXT;
				case "name" -> NAME_TEXT;
				case "game_server_version" -> GAME_SERVER_VERSION_TEXT;
				case "uploaded" -> UPLOADED_TEXT;
				case "enabled_packs" -> ENABLED_PACK_TEXT;
				case "description" -> DESCRIPTION_TEXT;
				case "game_mode" -> GAME_MODE_TEXT;
				case "seed" -> SEED_TEXT;
				case "world_type" -> WORLD_TYPE_TEXT;
				default -> UNDEFINED_TEXT;
			};
		}

		@Override
		public Text getNarration() {
			return Text.translatable("narrator.select", this.key + " " + this.value);
		}
	}
}
