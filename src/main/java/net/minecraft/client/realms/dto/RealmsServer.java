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

import java.util.*;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsServer}.
 */
public class RealmsServer extends ValueObject implements RealmsSerializable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int NO_PARENT = -1;
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
		return this.description;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public @Nullable String getMinigameName() {
		return this.minigameName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static RealmsServer parse(CheckedGson gson, String json) {
		try {
			RealmsServer realmsServer = gson.fromJson(json, RealmsServer.class);
			if (realmsServer == null) {
				LOGGER.error("Could not parse McoServer: {}", json);
				return new RealmsServer();
			}
			else {
				replaceNullsWithDefaults(realmsServer);
				return realmsServer;
			}
		}
		catch (Exception var3) {
			LOGGER.error("Could not parse McoServer", var3);
			return new RealmsServer();
		}
	}

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
			server.worldType = RealmsServer.WorldType.NORMAL;
		}

		if (server.activeVersion == null) {
			server.activeVersion = "";
		}

		if (server.compatibility == null) {
			server.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
		}

		if (server.regionSelectionPreference == null) {
			server.regionSelectionPreference = RealmsRegionSelectionPreference.DEFAULT;
		}

		sortInvited(server);
		populateSlots(server);
	}

	private static void sortInvited(RealmsServer server) {
		server.players
				.sort(
						(a, b) -> ComparisonChain.start()
						                         .compareFalseFirst(b.accepted, a.accepted)
						                         .compare(
								                         a.name.toLowerCase(Locale.ROOT),
								                         b.name.toLowerCase(Locale.ROOT)
						                         )
						                         .result()
				);
	}

	private static void populateSlots(RealmsServer server) {
		server.emptySlots.forEach(slot -> server.slots.put(slot.slotId, slot));

		for (int i = 1; i <= 3; i++) {
			if (!server.slots.containsKey(i)) {
				server.slots.put(i, RealmsSlot.create(i));
			}
		}
	}

	private static List<RealmsSlot> getEmptySlots() {
		List<RealmsSlot> list = new ArrayList<>();
		list.add(RealmsSlot.create(1));
		list.add(RealmsSlot.create(2));
		list.add(RealmsSlot.create(3));
		return list;
	}

	public boolean isCompatible() {
		return this.compatibility.isCompatible();
	}

	public boolean needsUpgrade() {
		return this.compatibility.needsUpgrade();
	}

	public boolean needsDowngrade() {
		return this.compatibility.needsDowngrade();
	}

	public boolean shouldAllowPlay() {
		boolean bl = !this.expired && this.state == RealmsServer.State.OPEN;
		return bl && (this.isCompatible() || this.needsUpgrade() || this.isPlayerOwner());
	}

	private boolean isPlayerOwner() {
		return MinecraftClient.getInstance().uuidEquals(this.ownerUUID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.name, this.description, this.state, this.owner, this.expired);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		else if (o == this) {
			return true;
		}
		else if (o.getClass() != this.getClass()) {
			return false;
		}
		else {
			RealmsServer realmsServer = (RealmsServer) o;
			return new EqualsBuilder()
					.append(this.id, realmsServer.id)
					.append(this.name, realmsServer.name)
					.append(this.description, realmsServer.description)
					.append(this.state, realmsServer.state)
					.append(this.owner, realmsServer.owner)
					.append(this.expired, realmsServer.expired)
					.append(this.worldType, this.worldType)
					.isEquals();
		}
	}

	public RealmsServer copy() {
		RealmsServer realmsServer = new RealmsServer();
		realmsServer.id = this.id;
		realmsServer.remoteSubscriptionId = this.remoteSubscriptionId;
		realmsServer.name = this.name;
		realmsServer.description = this.description;
		realmsServer.state = this.state;
		realmsServer.owner = this.owner;
		realmsServer.players = this.players;
		realmsServer.emptySlots = this.emptySlots.stream().map(RealmsSlot::copy).toList();
		realmsServer.slots = this.cloneSlots(this.slots);
		realmsServer.expired = this.expired;
		realmsServer.expiredTrial = this.expiredTrial;
		realmsServer.daysLeft = this.daysLeft;
		realmsServer.worldType = this.worldType;
		realmsServer.hardcore = this.hardcore;
		realmsServer.gameMode = this.gameMode;
		realmsServer.ownerUUID = this.ownerUUID;
		realmsServer.minigameName = this.minigameName;
		realmsServer.activeSlot = this.activeSlot;
		realmsServer.minigameId = this.minigameId;
		realmsServer.minigameImage = this.minigameImage;
		realmsServer.parentWorldName = this.parentWorldName;
		realmsServer.parentWorldId = this.parentWorldId;
		realmsServer.activeVersion = this.activeVersion;
		realmsServer.compatibility = this.compatibility;
		realmsServer.regionSelectionPreference =
				this.regionSelectionPreference != null ? this.regionSelectionPreference.copy() : null;
		return realmsServer;
	}

	public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> slots) {
		Map<Integer, RealmsSlot> map = Maps.newHashMap();

		for (Entry<Integer, RealmsSlot> entry : slots.entrySet()) {
			map.put(
					entry.getKey(),
					new RealmsSlot(entry.getKey(), entry.getValue().options.copy(), entry.getValue().settings)
			);
		}

		return map;
	}

	public boolean isPrerelease() {
		return this.parentWorldId != -1L;
	}

	public boolean isMinigame() {
		return this.worldType == RealmsServer.WorldType.MINIGAME;
	}

	public String getWorldName(int slotId) {
		return this.name == null
		       ? this.slots.get(slotId).options.getSlotName(slotId)
		       : this.name + " (" + this.slots.get(slotId).options.getSlotName(slotId) + ")";
	}

	public ServerInfo createServerInfo(String address) {
		return new ServerInfo(
				Objects.requireNonNullElse(this.name, "unknown server"),
				address,
				ServerInfo.ServerType.REALM
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Compatibility}.
	 */
	public static enum Compatibility {
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code McoServerComparator}.
	 */
	public static class McoServerComparator implements Comparator<RealmsServer> {

		private final String refOwner;

		public McoServerComparator(String owner) {
			this.refOwner = owner;
		}

		public int compare(RealmsServer realmsServer, RealmsServer realmsServer2) {
			return ComparisonChain.start()
			                      .compareTrueFirst(realmsServer.isPrerelease(), realmsServer2.isPrerelease())
			                      .compareTrueFirst(
					                      realmsServer.state == RealmsServer.State.UNINITIALIZED,
					                      realmsServer2.state == RealmsServer.State.UNINITIALIZED
			                      )
			                      .compareTrueFirst(realmsServer.expiredTrial, realmsServer2.expiredTrial)
			                      .compareTrueFirst(
					                      Objects.equals(realmsServer.owner, this.refOwner),
					                      Objects.equals(realmsServer2.owner, this.refOwner)
			                      )
			                      .compareFalseFirst(realmsServer.expired, realmsServer2.expired)
			                      .compareTrueFirst(
					                      realmsServer.state == RealmsServer.State.OPEN,
					                      realmsServer2.state == RealmsServer.State.OPEN
			                      )
			                      .compare(realmsServer.id, realmsServer2.id)
			                      .result();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code State}.
	 */
	public static enum State {
		CLOSED,
		OPEN,
		UNINITIALIZED;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WorldType}.
	 */
	public static enum WorldType {
		NORMAL("normal"),
		MINIGAME("minigame"),
		ADVENTUREMAP("adventureMap"),
		EXPERIENCE("experience"),
		INSPIRATION("inspiration"),
		UNKNOWN("unknown");

		private static final String WORLD_TYPE_TRANSLATION_PREFIX = "mco.backup.entry.worldType.";
		private final Text displayText;

		private WorldType(final String string2) {
			this.displayText = Text.translatable("mco.backup.entry.worldType." + string2);
		}

		public Text getDisplayText() {
			return this.displayText;
		}
	}
}
