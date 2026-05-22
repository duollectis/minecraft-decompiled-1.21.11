package net.minecraft.client.realms.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.util.UUIDTypeAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.CheckedGson;
import net.minecraft.client.realms.RealmsSerializable;
import net.minecraft.client.realms.util.DontSerialize;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO сервера Realms (MCO-сервера).
 * Содержит полное состояние сервера: слоты миров, список игроков, совместимость версий.
 * После десериализации через Gson необходимо вызвать {@link #replaceNullsWithDefaults(RealmsServer)}
 * для заполнения отсутствующих полей значениями по умолчанию.
 */
@Environment(EnvType.CLIENT)
public class RealmsServer extends ValueObject implements RealmsSerializable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final long NO_PARENT_ID = -1L;
	private static final int SLOT_COUNT = 3;

	public static final Text REALM_CLOSED_TEXT = Text.translatable("mco.play.button.realm.closed");

	@SerializedName("id")
	public long id = -1L;
	@SerializedName("remoteSubscriptionId")
	public @Nullable String remoteSubscriptionId;
	@SerializedName("name")
	public @Nullable String name;
	@SerializedName("motd")
	public String description = "";
	@SerializedName("state")
	public RealmsServer.State state = RealmsServer.State.CLOSED;
	@SerializedName("owner")
	public @Nullable String owner;
	@SerializedName("ownerUUID")
	@JsonAdapter(UUIDTypeAdapter.class)
	public UUID ownerUUID = Util.NIL_UUID;
	@SerializedName("players")
	public List<PlayerInfo> players = Lists.newArrayList();
	@SerializedName("slots")
	private List<RealmsSlot> emptySlots = getEmptySlots();
	@DontSerialize
	public Map<Integer, RealmsSlot> slots = new HashMap<>();
	@SerializedName("expired")
	public boolean expired;
	@SerializedName("expiredTrial")
	public boolean expiredTrial = false;
	@SerializedName("daysLeft")
	public int daysLeft;
	@SerializedName("worldType")
	public RealmsServer.WorldType worldType = RealmsServer.WorldType.NORMAL;
	@SerializedName("isHardcore")
	public boolean hardcore = false;
	@SerializedName("gameMode")
	public int gameMode = -1;
	@SerializedName("activeSlot")
	public int activeSlot = -1;
	@SerializedName("minigameName")
	public @Nullable String minigameName;
	@SerializedName("minigameId")
	public int minigameId = -1;
	@SerializedName("minigameImage")
	public @Nullable String minigameImage;
	@SerializedName("parentWorldId")
	public long parentWorldId = -1L;
	@SerializedName("parentWorldName")
	public @Nullable String parentWorldName;
	@SerializedName("activeVersion")
	public String activeVersion = "";
	@SerializedName("compatibility")
	public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
	@SerializedName("regionSelectionPreference")
	public @Nullable RealmsRegionSelectionPreference regionSelectionPreference;

	public String getDescription() {
		return description;
	}

	public @Nullable String getName() {
		return name;
	}

	public @Nullable String getMinigameName() {
		return minigameName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Парсит сервер Realms из JSON-строки.
	 * При ошибке возвращает пустой объект с дефолтными значениями.
	 *
	 * @param gson настроенный экземпляр Gson
	 * @param json JSON-строка с данными сервера
	 * @return распарсенный сервер или пустой объект при ошибке
	 */
	public static RealmsServer parse(CheckedGson gson, String json) {
		try {
			RealmsServer server = gson.fromJson(json, RealmsServer.class);

			if (server == null) {
				LOGGER.error("Could not parse McoServer: {}", json);
				return new RealmsServer();
			}

			replaceNullsWithDefaults(server);
			return server;
		} catch (Exception ex) {
			LOGGER.error("Could not parse McoServer", ex);
			return new RealmsServer();
		}
	}

	/**
	 * Заполняет null-поля сервера значениями по умолчанию после десериализации.
	 * Также сортирует список приглашённых и заполняет карту слотов.
	 *
	 * @param server сервер для нормализации
	 */
	public static void replaceNullsWithDefaults(RealmsServer server) {
		if (server.players == null) {
			server.players = Lists.newArrayList();
		}

		if (server.emptySlots == null) {
			server.emptySlots = getEmptySlots();
		}

		if (server.slots == null) {
			server.slots = new HashMap<>();
		}

		if (server.worldType == null) {
			server.worldType = WorldType.NORMAL;
		}

		if (server.activeVersion == null) {
			server.activeVersion = "";
		}

		if (server.compatibility == null) {
			server.compatibility = Compatibility.UNVERIFIABLE;
		}

		if (server.regionSelectionPreference == null) {
			server.regionSelectionPreference = RealmsRegionSelectionPreference.DEFAULT;
		}

		sortInvited(server);
		populateSlots(server);
	}

	private static void sortInvited(RealmsServer server) {
		server.players.sort(
				(a, b) -> ComparisonChain.start()
						.compareFalseFirst(b.accepted, a.accepted)
						.compare(a.name.toLowerCase(Locale.ROOT), b.name.toLowerCase(Locale.ROOT))
						.result()
		);
	}

	private static void populateSlots(RealmsServer server) {
		server.emptySlots.forEach(slot -> server.slots.put(slot.slotId, slot));

		for (int slotId = 1; slotId <= SLOT_COUNT; slotId++) {
			if (!server.slots.containsKey(slotId)) {
				server.slots.put(slotId, RealmsSlot.create(slotId));
			}
		}
	}

	private static List<RealmsSlot> getEmptySlots() {
		List<RealmsSlot> slots = new ArrayList<>();
		slots.add(RealmsSlot.create(1));
		slots.add(RealmsSlot.create(2));
		slots.add(RealmsSlot.create(3));
		return slots;
	}

	public boolean isCompatible() {
		return compatibility.isCompatible();
	}

	public boolean needsUpgrade() {
		return compatibility.needsUpgrade();
	}

	public boolean needsDowngrade() {
		return compatibility.needsDowngrade();
	}

	/**
	 * Определяет, разрешено ли игроку подключиться к серверу.
	 * Подключение разрешено если сервер открыт, не истёк срок подписки,
	 * и версия совместима (или требует апгрейда, или игрок — владелец).
	 *
	 * @return {@code true} если подключение разрешено
	 */
	public boolean shouldAllowPlay() {
		boolean openAndActive = !expired && state == State.OPEN;
		return openAndActive && (isCompatible() || needsUpgrade() || isPlayerOwner());
	}

	private boolean isPlayerOwner() {
		return MinecraftClient.getInstance().uuidEquals(ownerUUID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, state, owner, expired);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}

		if (other == this) {
			return true;
		}

		if (other.getClass() != getClass()) {
			return false;
		}

		RealmsServer that = (RealmsServer) other;
		return new EqualsBuilder()
				.append(id, that.id)
				.append(name, that.name)
				.append(description, that.description)
				.append(state, that.state)
				.append(owner, that.owner)
				.append(expired, that.expired)
				.append(worldType, worldType)
				.isEquals();
	}

	public RealmsServer copy() {
		RealmsServer copy = new RealmsServer();
		copy.id = id;
		copy.remoteSubscriptionId = remoteSubscriptionId;
		copy.name = name;
		copy.description = description;
		copy.state = state;
		copy.owner = owner;
		copy.players = players;
		copy.emptySlots = emptySlots.stream().map(RealmsSlot::copy).toList();
		copy.slots = cloneSlots(slots);
		copy.expired = expired;
		copy.expiredTrial = expiredTrial;
		copy.daysLeft = daysLeft;
		copy.worldType = worldType;
		copy.hardcore = hardcore;
		copy.gameMode = gameMode;
		copy.ownerUUID = ownerUUID;
		copy.minigameName = minigameName;
		copy.activeSlot = activeSlot;
		copy.minigameId = minigameId;
		copy.minigameImage = minigameImage;
		copy.parentWorldName = parentWorldName;
		copy.parentWorldId = parentWorldId;
		copy.activeVersion = activeVersion;
		copy.compatibility = compatibility;
		copy.regionSelectionPreference = regionSelectionPreference != null
				? regionSelectionPreference.copy()
				: null;
		return copy;
	}

	public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> source) {
		Map<Integer, RealmsSlot> result = Maps.newHashMap();

		for (Map.Entry<Integer, RealmsSlot> entry : source.entrySet()) {
			result.put(
					entry.getKey(),
					new RealmsSlot(entry.getKey(), entry.getValue().options.copy(), entry.getValue().settings)
			);
		}

		return result;
	}

	public boolean isPrerelease() {
		return parentWorldId != NO_PARENT_ID;
	}

	public boolean isMinigame() {
		return worldType == WorldType.MINIGAME;
	}

	public String getWorldName(int slotId) {
		return name == null
				? slots.get(slotId).options.getSlotName(slotId)
				: name + " (" + slots.get(slotId).options.getSlotName(slotId) + ")";
	}

	public ServerInfo createServerInfo(String address) {
		return new ServerInfo(
				Objects.requireNonNullElse(name, "unknown server"),
				address,
				ServerInfo.ServerType.REALM
		);
	}

	@Environment(EnvType.CLIENT)
	public enum Compatibility {
		UNVERIFIABLE,
		INCOMPATIBLE,
		RELEASE_TYPE_INCOMPATIBLE,
		NEEDS_DOWNGRADE,
		NEEDS_UPGRADE,
		COMPATIBLE;

		public boolean isCompatible() {
			return this == COMPATIBLE;
		}

		public boolean needsUpgrade() {
			return this == NEEDS_UPGRADE;
		}

		public boolean needsDowngrade() {
			return this == NEEDS_DOWNGRADE;
		}
	}

	/**
	 * Компаратор для сортировки серверов Realms в списке.
	 * Приоритет: пре-релизы → неинициализированные → пробные → собственные → не истёкшие → открытые → по ID.
	 */
	@Environment(EnvType.CLIENT)
	public static class McoServerComparator implements Comparator<RealmsServer> {

		private final String refOwner;

		public McoServerComparator(String owner) {
			this.refOwner = owner;
		}

		@Override
		public int compare(RealmsServer first, RealmsServer second) {
			return ComparisonChain.start()
					.compareTrueFirst(first.isPrerelease(), second.isPrerelease())
					.compareTrueFirst(first.state == State.UNINITIALIZED, second.state == State.UNINITIALIZED)
					.compareTrueFirst(first.expiredTrial, second.expiredTrial)
					.compareTrueFirst(Objects.equals(first.owner, refOwner), Objects.equals(second.owner, refOwner))
					.compareFalseFirst(first.expired, second.expired)
					.compareTrueFirst(first.state == State.OPEN, second.state == State.OPEN)
					.compare(first.id, second.id)
					.result();
		}
	}

	@Environment(EnvType.CLIENT)
	public enum State {
		CLOSED,
		OPEN,
		UNINITIALIZED
	}

	@Environment(EnvType.CLIENT)
	public enum WorldType {
		NORMAL("normal"),
		MINIGAME("minigame"),
		ADVENTUREMAP("adventureMap"),
		EXPERIENCE("experience"),
		INSPIRATION("inspiration"),
		UNKNOWN("unknown");

		private static final String TRANSLATION_PREFIX = "mco.backup.entry.worldType.";

		private final Text displayText;

		WorldType(String translationSuffix) {
			this.displayText = Text.translatable(TRANSLATION_PREFIX + translationSuffix);
		}

		public Text getDisplayText() {
			return displayText;
		}
	}
}
