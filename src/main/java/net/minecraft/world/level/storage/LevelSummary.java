package net.minecraft.world.level.storage;

import net.minecraft.GameVersion;
import net.minecraft.SharedConstants;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.LevelInfo;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Краткая информация о сохранённом мире для отображения в списке миров.
 * Содержит имя, версию, режим игры, флаги доступности и путь к иконке.
 *
 * <p>Подклассы {@link RecoveryWarning} и {@link SymlinkLevelSummary} представляют
 * миры с повреждёнными данными и небезопасными симлинками соответственно.
 */
public class LevelSummary implements Comparable<LevelSummary> {

	public static final Text SELECT_WORLD_TEXT = Text.translatable("selectWorld.select");

	private final LevelInfo levelInfo;
	private final SaveVersionInfo versionInfo;
	private final String name;
	private final boolean requiresConversion;
	private final boolean locked;
	private final boolean experimental;
	private final Path iconPath;
	private @Nullable Text details;

	public LevelSummary(
		LevelInfo levelInfo,
		SaveVersionInfo versionInfo,
		String name,
		boolean requiresConversion,
		boolean locked,
		boolean experimental,
		Path iconPath
	) {
		this.levelInfo = levelInfo;
		this.versionInfo = versionInfo;
		this.name = name;
		this.locked = locked;
		this.experimental = experimental;
		this.iconPath = iconPath;
		this.requiresConversion = requiresConversion;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return StringUtils.isEmpty(levelInfo.getLevelName()) ? name : levelInfo.getLevelName();
	}

	public Path getIconPath() {
		return iconPath;
	}

	public boolean requiresConversion() {
		return requiresConversion;
	}

	public boolean isExperimental() {
		return experimental;
	}

	public long getLastPlayed() {
		return versionInfo.getLastPlayed();
	}

	@Override
	public int compareTo(LevelSummary other) {
		if (getLastPlayed() < other.getLastPlayed()) {
			return 1;
		}

		return getLastPlayed() > other.getLastPlayed() ? -1 : name.compareTo(other.name);
	}

	public LevelInfo getLevelInfo() {
		return levelInfo;
	}

	public GameMode getGameMode() {
		return levelInfo.getGameMode();
	}

	public boolean isHardcore() {
		return levelInfo.isHardcore();
	}

	public boolean hasCheats() {
		return levelInfo.areCommandsAllowed();
	}

	public MutableText getVersion() {
		return StringHelper.isEmpty(versionInfo.getVersionName())
			? Text.translatable("selectWorld.versionUnknown")
			: Text.literal(versionInfo.getVersionName());
	}

	public SaveVersionInfo getVersionInfo() {
		return versionInfo;
	}

	public boolean shouldPromptBackup() {
		return getConversionWarning().promptsBackup();
	}

	public boolean wouldBeDowngraded() {
		return getConversionWarning() == ConversionWarning.DOWNGRADE;
	}

	/**
	 * Определяет тип предупреждения о конвертации мира на основе сравнения
	 * датаверсии текущей игры и версии сохранения.
	 *
	 * @return тип предупреждения: понижение версии, обновление до снапшота или отсутствие
	 */
	public ConversionWarning getConversionWarning() {
		GameVersion gameVersion = SharedConstants.getGameVersion();
		int currentId = gameVersion.dataVersion().id();
		int savedId = versionInfo.getVersion().id();

		if (!gameVersion.stable() && savedId < currentId) {
			return ConversionWarning.UPGRADE_TO_SNAPSHOT;
		}

		return savedId > currentId ? ConversionWarning.DOWNGRADE : ConversionWarning.NONE;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isUnavailable() {
		return isLocked() || requiresConversion() || !isVersionAvailable();
	}

	public boolean isVersionAvailable() {
		return SharedConstants.getGameVersion().dataVersion().isAvailableTo(versionInfo.getVersion());
	}

	/**
	 * Возвращает кешированный текст деталей для отображения в списке миров.
	 * Вычисляется лениво при первом обращении.
	 */
	public Text getDetails() {
		if (details == null) {
			details = createDetails();
		}

		return details;
	}

	private Text createDetails() {
		if (isLocked()) {
			return Text.translatable("selectWorld.locked").formatted(Formatting.RED);
		}

		if (requiresConversion()) {
			return Text.translatable("selectWorld.conversion").formatted(Formatting.RED);
		}

		if (!isVersionAvailable()) {
			return Text.translatable("selectWorld.incompatible.info", getVersion()).formatted(Formatting.RED);
		}

		MutableText modeText = isHardcore()
			? Text.empty().append(Text.translatable("gameMode.hardcore").withColor(-65536))
			: Text.translatable("gameMode." + getGameMode().getId());

		if (hasCheats()) {
			modeText.append(", ").append(Text.translatable("selectWorld.commands"));
		}

		if (isExperimental()) {
			modeText.append(", ").append(Text.translatable("selectWorld.experimental").formatted(Formatting.YELLOW));
		}

		MutableText versionText = getVersion();
		MutableText versionLabel = Text.literal(", ")
			.append(Text.translatable("selectWorld.version"))
			.append(ScreenTexts.SPACE);

		if (shouldPromptBackup()) {
			versionLabel.append(versionText.formatted(wouldBeDowngraded() ? Formatting.RED : Formatting.ITALIC));
		} else {
			versionLabel.append(versionText);
		}

		modeText.append(versionLabel);

		return modeText;
	}

	public Text getSelectWorldText() {
		return SELECT_WORLD_TEXT;
	}

	public boolean isSelectable() {
		return !isUnavailable();
	}

	public boolean isImmediatelyLoadable() {
		return !requiresConversion() && !isLocked();
	}

	public boolean isEditable() {
		return !isUnavailable();
	}

	public boolean isRecreatable() {
		return !isUnavailable();
	}

	public boolean isDeletable() {
		return true;
	}

	/**
	 * Тип предупреждения о конвертации мира при открытии в другой версии игры.
	 */
	public enum ConversionWarning {
		NONE(false, false, ""),
		DOWNGRADE(true, true, "downgrade"),
		UPGRADE_TO_SNAPSHOT(true, false, "snapshot");

		private final boolean backup;
		private final boolean dangerous;
		private final String translationKeySuffix;

		ConversionWarning(boolean backup, boolean dangerous, String translationKeySuffix) {
			this.backup = backup;
			this.dangerous = dangerous;
			this.translationKeySuffix = translationKeySuffix;
		}

		public boolean promptsBackup() {
			return backup;
		}

		public boolean isDangerous() {
			return dangerous;
		}

		public String getTranslationKeySuffix() {
			return translationKeySuffix;
		}
	}

	/**
	 * Сводка для мира с повреждёнными данными level.dat.
	 * Предлагает восстановление из резервной копии вместо обычного открытия.
	 */
	public static class RecoveryWarning extends LevelSummary {

		private static final Text WARNING_TEXT = Text.translatable("recover_world.warning")
			.styled(style -> style.withColor(-65536));
		private static final Text BUTTON_TEXT = Text.translatable("recover_world.button");

		private final long lastPlayed;

		public RecoveryWarning(String name, Path iconPath, long lastPlayed) {
			super(null, null, name, false, false, false, iconPath);
			this.lastPlayed = lastPlayed;
		}

		@Override
		public String getDisplayName() {
			return getName();
		}

		@Override
		public Text getDetails() {
			return WARNING_TEXT;
		}

		@Override
		public long getLastPlayed() {
			return lastPlayed;
		}

		@Override
		public boolean isUnavailable() {
			return false;
		}

		@Override
		public Text getSelectWorldText() {
			return BUTTON_TEXT;
		}

		@Override
		public boolean isSelectable() {
			return true;
		}

		@Override
		public boolean isImmediatelyLoadable() {
			return false;
		}

		@Override
		public boolean isEditable() {
			return false;
		}

		@Override
		public boolean isRecreatable() {
			return false;
		}
	}

	/**
	 * Сводка для мира, директория которого содержит небезопасные символические ссылки.
	 * Открытие заблокировано до ручного подтверждения пользователем.
	 */
	public static class SymlinkLevelSummary extends LevelSummary {

		private static final Text MORE_INFO_TEXT = Text.translatable("symlink_warning.more_info");
		private static final Text TITLE_TEXT = Text.translatable("symlink_warning.title").withColor(-65536);

		public SymlinkLevelSummary(String name, Path iconPath) {
			super(null, null, name, false, false, false, iconPath);
		}

		@Override
		public String getDisplayName() {
			return getName();
		}

		@Override
		public Text getDetails() {
			return TITLE_TEXT;
		}

		@Override
		public long getLastPlayed() {
			return -1L;
		}

		@Override
		public boolean isUnavailable() {
			return false;
		}

		@Override
		public Text getSelectWorldText() {
			return MORE_INFO_TEXT;
		}

		@Override
		public boolean isSelectable() {
			return true;
		}

		@Override
		public boolean isImmediatelyLoadable() {
			return false;
		}

		@Override
		public boolean isEditable() {
			return false;
		}

		@Override
		public boolean isRecreatable() {
			return false;
		}
	}
}
