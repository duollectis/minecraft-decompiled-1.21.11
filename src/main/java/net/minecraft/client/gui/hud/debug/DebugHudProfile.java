package net.minecraft.client.gui.hud.debug;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * Профиль видимости записей отладочного HUD. Хранит состояние в JSON-файле
 * и поддерживает как предустановленные профили, так и пользовательские настройки.
 */
@Environment(EnvType.CLIENT)
public class DebugHudProfile {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int PROFILE_COLOR = 4649;

	private Map<Identifier, DebugHudEntryVisibility> visibilityMap;
	private final List<Identifier> visibleEntries = new ArrayList<>();
	private boolean f3Enabled = false;
	private @Nullable DebugProfileType type;
	private final File file;
	private long version;
	private final Codec<DebugHudProfile.Serialization> codec;

	public DebugHudProfile(File file) {
		this.file = new File(file, "debug-profile.json");
		codec = DataFixTypes.DEBUG_PROFILE.createDataFixingCodec(
			DebugHudProfile.Serialization.CODEC,
			MinecraftClient.getInstance().getDataFixer(),
			PROFILE_COLOR
		);
		readProfileFile();
	}

	/**
	 * Читает профиль из файла. При отсутствии файла или ошибке парсинга
	 * сбрасывает к профилю по умолчанию и сохраняет его.
	 */
	public void readProfileFile() {
		try {
			if (!file.isFile()) {
				setToDefault();
				updateVisibleEntries();
				return;
			}

			Dynamic<JsonElement> dynamic = new Dynamic<>(
				JsonOps.INSTANCE,
				StrictJsonParser.parse(FileUtils.readFileToString(file, StandardCharsets.UTF_8))
			);
			DebugHudProfile.Serialization serialization = (DebugHudProfile.Serialization) codec
				.parse(dynamic)
				.getOrThrow(error -> new IOException("Could not parse debug profile JSON: " + error));

			if (serialization.profile().isPresent()) {
				setProfileType(serialization.profile().get());
			} else {
				visibilityMap = new HashMap<>();
				serialization.custom().ifPresent(visibilityMap::putAll);
				type = null;
			}
		} catch (JsonSyntaxException | IOException error) {
			LOGGER.error("Couldn't read debug profile file {}, resetting to default", file, error);
			setToDefault();
			saveProfileFile();
		}

		updateVisibleEntries();
	}

	public void setProfileType(DebugProfileType profileType) {
		type = profileType;
		visibilityMap = new HashMap<>(DebugHudEntries.PROFILES.get(profileType));
		updateVisibleEntries();
	}

	private void setToDefault() {
		type = DebugProfileType.DEFAULT;
		visibilityMap = new HashMap<>(DebugHudEntries.PROFILES.get(DebugProfileType.DEFAULT));
	}

	public DebugHudEntryVisibility getVisibility(Identifier entryId) {
		DebugHudEntryVisibility visibility = visibilityMap.get(entryId);
		return visibility == null ? DebugHudEntryVisibility.NEVER : visibility;
	}

	public boolean isEntryVisible(Identifier entryId) {
		return visibleEntries.contains(entryId);
	}

	public void setEntryVisibility(Identifier entryId, DebugHudEntryVisibility visibility) {
		type = null;
		visibilityMap.put(entryId, visibility);
		updateVisibleEntries();
		saveProfileFile();
	}

	/**
	 * Переключает видимость записи по циклу: ALWAYS_ON → NEVER → IN_OVERLAY/ALWAYS_ON.
	 * Поведение зависит от того, открыт ли F3-оверлей.
	 *
	 * @return {@code true}, если запись стала видимой
	 */
	public boolean toggleVisibility(Identifier entryId) {
		switch ((DebugHudEntryVisibility) visibilityMap.get(entryId)) {
			case ALWAYS_ON:
				setEntryVisibility(entryId, DebugHudEntryVisibility.NEVER);
				return false;
			case IN_OVERLAY:
				if (f3Enabled) {
					setEntryVisibility(entryId, DebugHudEntryVisibility.NEVER);
					return false;
				}

				setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				return true;
			case NEVER:
				setEntryVisibility(entryId, f3Enabled ? DebugHudEntryVisibility.IN_OVERLAY : DebugHudEntryVisibility.ALWAYS_ON);
				return true;
			case null:
			default:
				setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				return true;
		}
	}

	public Collection<Identifier> getVisibleEntries() {
		return visibleEntries;
	}

	public void toggleF3Enabled() {
		setF3Enabled(!f3Enabled);
	}

	public void setF3Enabled(boolean enabled) {
		if (f3Enabled == enabled) {
			return;
		}

		f3Enabled = enabled;
		updateVisibleEntries();
	}

	public boolean isF3Enabled() {
		return f3Enabled;
	}

	/**
	 * Пересчитывает список видимых записей на основе текущей карты видимости
	 * и состояния F3-оверлея. Вызывается при любом изменении профиля.
	 */
	public void updateVisibleEntries() {
		visibleEntries.clear();
		boolean reducedDebugInfo = MinecraftClient.getInstance().hasReducedDebugInfo();

		for (Entry<Identifier, DebugHudEntryVisibility> entry : visibilityMap.entrySet()) {
			boolean alwaysOn = entry.getValue() == DebugHudEntryVisibility.ALWAYS_ON;
			boolean inOverlayAndOpen = f3Enabled && entry.getValue() == DebugHudEntryVisibility.IN_OVERLAY;

			if (alwaysOn || inOverlayAndOpen) {
				DebugHudEntry debugEntry = DebugHudEntries.get(entry.getKey());
				if (debugEntry != null && debugEntry.canShow(reducedDebugInfo)) {
					visibleEntries.add(entry.getKey());
				}
			}
		}

		visibleEntries.sort(Identifier::compareTo);
		version++;
	}

	public long getVersion() {
		return version;
	}

	public boolean profileTypeMatches(DebugProfileType profileType) {
		return type == profileType;
	}

	/** Сохраняет текущий профиль в JSON-файл. */
	public void saveProfileFile() {
		DebugHudProfile.Serialization serialization = new DebugHudProfile.Serialization(
			Optional.ofNullable(type),
			type == null ? Optional.of(visibilityMap) : Optional.empty()
		);

		try {
			FileUtils.writeStringToFile(
				file,
				((JsonElement) codec.encodeStart(JsonOps.INSTANCE, serialization).getOrThrow()).toString(),
				StandardCharsets.UTF_8
			);
		} catch (IOException error) {
			LOGGER.error("Failed to save debug profile file {}", file, error);
		}
	}

	/**
	 * Сериализуемое представление профиля: либо ссылка на предустановленный тип,
	 * либо пользовательская карта видимости.
	 */
	@Environment(EnvType.CLIENT)
	record Serialization(
		Optional<DebugProfileType> profile,
		Optional<Map<Identifier, DebugHudEntryVisibility>> custom
	) {

		private static final Codec<Map<Identifier, DebugHudEntryVisibility>> VISIBILITY_MAP_CODEC =
			Codec.unboundedMap(Identifier.CODEC, DebugHudEntryVisibility.CODEC);

		public static final Codec<DebugHudProfile.Serialization> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				DebugProfileType.CODEC.optionalFieldOf("profile").forGetter(DebugHudProfile.Serialization::profile),
				VISIBILITY_MAP_CODEC.optionalFieldOf("custom").forGetter(DebugHudProfile.Serialization::custom)
			).apply(instance, DebugHudProfile.Serialization::new)
		);
	}
}
