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

@Environment(EnvType.CLIENT)
/**
 * {@code DebugHudProfile}.
 */
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
		this.codec =
				DataFixTypes.DEBUG_PROFILE.createDataFixingCodec(
						DebugHudProfile.Serialization.CODEC,
						MinecraftClient.getInstance().getDataFixer(),
						4649
				);
		this.readProfileFile();
	}

	/**
	 * Читает profile file.
	 */
	public void readProfileFile() {
		try {
			if (!this.file.isFile()) {
				this.setToDefault();
				this.updateVisibleEntries();
				return;
			}

			Dynamic<JsonElement>
					dynamic =
					new Dynamic(
							JsonOps.INSTANCE,
							StrictJsonParser.parse(FileUtils.readFileToString(this.file, StandardCharsets.UTF_8))
					);
			DebugHudProfile.Serialization serialization = (DebugHudProfile.Serialization) this.codec
					.parse(dynamic)
					.getOrThrow(error -> new IOException("Could not parse debug profile JSON: " + error));
			if (serialization.profile().isPresent()) {
				this.setProfileType(serialization.profile().get());
			}
			else {
				this.visibilityMap = new HashMap<>();
				if (serialization.custom().isPresent()) {
					this.visibilityMap.putAll(serialization.custom().get());
				}

				this.type = null;
			}
		}
		catch (JsonSyntaxException | IOException var3) {
			LOGGER.error("Couldn't read debug profile file {}, resetting to default", this.file, var3);
			this.setToDefault();
			this.saveProfileFile();
		}

		this.updateVisibleEntries();
	}

	public void setProfileType(DebugProfileType type) {
		this.type = type;
		Map<Identifier, DebugHudEntryVisibility> map = DebugHudEntries.PROFILES.get(type);
		this.visibilityMap = new HashMap<>(map);
		this.updateVisibleEntries();
	}

	private void setToDefault() {
		this.type = DebugProfileType.DEFAULT;
		this.visibilityMap = new HashMap<>(DebugHudEntries.PROFILES.get(DebugProfileType.DEFAULT));
	}

	public DebugHudEntryVisibility getVisibility(Identifier entryId) {
		DebugHudEntryVisibility debugHudEntryVisibility = this.visibilityMap.get(entryId);
		return debugHudEntryVisibility == null ? DebugHudEntryVisibility.NEVER : debugHudEntryVisibility;
	}

	public boolean isEntryVisible(Identifier entryId) {
		return this.visibleEntries.contains(entryId);
	}

	public void setEntryVisibility(Identifier entryId, DebugHudEntryVisibility visibility) {
		this.type = null;
		this.visibilityMap.put(entryId, visibility);
		this.updateVisibleEntries();
		this.saveProfileFile();
	}

	/**
	 * Toggle visibility.
	 *
	 * @param entryId entry id
	 *
	 * @return boolean — результат операции
	 */
	public boolean toggleVisibility(Identifier entryId) {
		switch ((DebugHudEntryVisibility) this.visibilityMap.get(entryId)) {
			case ALWAYS_ON:
				this.setEntryVisibility(entryId, DebugHudEntryVisibility.NEVER);
				return false;
			case IN_OVERLAY:
				if (this.f3Enabled) {
					this.setEntryVisibility(entryId, DebugHudEntryVisibility.NEVER);
					return false;
				}

				this.setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				return true;
			case NEVER:
				if (this.f3Enabled) {
					this.setEntryVisibility(entryId, DebugHudEntryVisibility.IN_OVERLAY);
				}
				else {
					this.setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				}

				return true;
			case null:
			default:
				this.setEntryVisibility(entryId, DebugHudEntryVisibility.ALWAYS_ON);
				return true;
		}
	}

	public Collection<Identifier> getVisibleEntries() {
		return this.visibleEntries;
	}

	/**
	 * Toggle f3 enabled.
	 */
	public void toggleF3Enabled() {
		this.setF3Enabled(!this.f3Enabled);
	}

	public void setF3Enabled(boolean f3Enabled) {
		if (this.f3Enabled != f3Enabled) {
			this.f3Enabled = f3Enabled;
			this.updateVisibleEntries();
		}
	}

	public boolean isF3Enabled() {
		return this.f3Enabled;
	}

	/**
	 * Обновляет visible entries.
	 */
	public void updateVisibleEntries() {
		this.visibleEntries.clear();
		boolean bl = MinecraftClient.getInstance().hasReducedDebugInfo();

		for (Entry<Identifier, DebugHudEntryVisibility> entry : this.visibilityMap.entrySet()) {
			if (entry.getValue() == DebugHudEntryVisibility.ALWAYS_ON
					|| this.f3Enabled && entry.getValue() == DebugHudEntryVisibility.IN_OVERLAY) {
				DebugHudEntry debugHudEntry = DebugHudEntries.get(entry.getKey());
				if (debugHudEntry != null && debugHudEntry.canShow(bl)) {
					this.visibleEntries.add(entry.getKey());
				}
			}
		}

		this.visibleEntries.sort(Identifier::compareTo);
		this.version++;
	}

	public long getVersion() {
		return this.version;
	}

	/**
	 * Profile type matches.
	 *
	 * @param type type
	 *
	 * @return boolean — результат операции
	 */
	public boolean profileTypeMatches(DebugProfileType type) {
		return this.type == type;
	}

	/**
	 * Сохраняет profile file.
	 */
	public void saveProfileFile() {
		DebugHudProfile.Serialization serialization = new DebugHudProfile.Serialization(
				Optional.ofNullable(this.type), this.type == null ? Optional.of(this.visibilityMap) : Optional.empty()
		);

		try {
			FileUtils.writeStringToFile(
					this.file,
					((JsonElement) this.codec.encodeStart(JsonOps.INSTANCE, serialization).getOrThrow()).toString(),
					StandardCharsets.UTF_8
			);
		}
		catch (IOException var3) {
			LOGGER.error("Failed to save debug profile file {}", this.file, var3);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Serialization}.
	 */
	record Serialization(
			Optional<DebugProfileType> profile,
			Optional<Map<Identifier, DebugHudEntryVisibility>> custom
	) {

		private static final Codec<Map<Identifier, DebugHudEntryVisibility>> VISIBILITY_MAP_CODEC = Codec.unboundedMap(
				Identifier.CODEC, DebugHudEntryVisibility.CODEC
		);
		public static final Codec<DebugHudProfile.Serialization> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    DebugProfileType.CODEC
								                    .optionalFieldOf("profile")
								                    .forGetter(DebugHudProfile.Serialization::profile),
						                    VISIBILITY_MAP_CODEC.optionalFieldOf("custom").forGetter(DebugHudProfile.Serialization::custom)
				                    )
				                    .apply(instance, DebugHudProfile.Serialization::new)
		);
	}
}
