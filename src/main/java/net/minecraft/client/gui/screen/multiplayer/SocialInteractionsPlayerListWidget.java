package net.minecraft.client.gui.screen.multiplayer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.session.report.log.ChatLog;
import net.minecraft.client.session.report.log.ReceivedMessage;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code SocialInteractionsPlayerListWidget}.
 */
public class SocialInteractionsPlayerListWidget extends ElementListWidget<SocialInteractionsPlayerListEntry> {

	private final SocialInteractionsScreen parent;
	private final List<SocialInteractionsPlayerListEntry> players = Lists.newArrayList();
	private @Nullable String currentSearch;

	public SocialInteractionsPlayerListWidget(
			SocialInteractionsScreen parent,
			MinecraftClient client,
			int width,
			int height,
			int y,
			int itemHeight
	) {
		super(client, width, height, y, itemHeight);
		this.parent = parent;
	}

	@Override
	protected void drawMenuListBackground(DrawContext context) {
	}

	@Override
	protected void drawHeaderAndFooterSeparators(DrawContext context) {
	}

	@Override
	protected void enableScissor(DrawContext context) {
		context.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
	}

	public void update(Collection<UUID> uuids, double scrollAmount, boolean includeOffline) {
		Map<UUID, SocialInteractionsPlayerListEntry> map = new HashMap<>();
		this.setPlayers(uuids, map);
		if (includeOffline) {
			this.collectOfflinePlayers(map);
		}

		this.markOfflineMembers(map, includeOffline);
		this.refresh(map.values(), scrollAmount);
	}

	private void setPlayers(Collection<UUID> playerUuids, Map<UUID, SocialInteractionsPlayerListEntry> entriesByUuids) {
		ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.player.networkHandler;

		for (UUID uUID : playerUuids) {
			PlayerListEntry playerListEntry = clientPlayNetworkHandler.getPlayerListEntry(uUID);
			if (playerListEntry != null) {
				SocialInteractionsPlayerListEntry
						socialInteractionsPlayerListEntry =
						this.createListEntry(uUID, playerListEntry);
				entriesByUuids.put(uUID, socialInteractionsPlayerListEntry);
			}
		}
	}

	private void collectOfflinePlayers(Map<UUID, SocialInteractionsPlayerListEntry> entriesByUuids) {
		Map<UUID, PlayerListEntry> map = this.client.player.networkHandler.getSeenPlayers();

		for (Map.Entry<UUID, PlayerListEntry> entry : map.entrySet()) {
			entriesByUuids.computeIfAbsent(
					entry.getKey(), uuid -> {
						SocialInteractionsPlayerListEntry
								socialInteractionsPlayerListEntry =
								this.createListEntry(uuid, entry.getValue());
						socialInteractionsPlayerListEntry.setOffline(true);
						return socialInteractionsPlayerListEntry;
					}
			);
		}
	}

	private SocialInteractionsPlayerListEntry createListEntry(UUID uuid, PlayerListEntry playerListEntry) {
		return new SocialInteractionsPlayerListEntry(
				this.client,
				this.parent,
				uuid,
				playerListEntry.getProfile().name(),
				playerListEntry::getSkinTextures,
				playerListEntry.hasPublicKey()
		);
	}

	private void markOfflineMembers(Map<UUID, SocialInteractionsPlayerListEntry> entries, boolean includeOffline) {
		Map<UUID, GameProfile> map = collectReportableProfiles(this.client.getAbuseReportContext().getChatLog());
		map.forEach(
				(uuid, profile) -> {
					SocialInteractionsPlayerListEntry socialInteractionsPlayerListEntry;
					if (includeOffline) {
						socialInteractionsPlayerListEntry = entries.computeIfAbsent(
								uuid,
								uuidx -> {
									SocialInteractionsPlayerListEntry
											socialInteractionsPlayerListEntryx =
											new SocialInteractionsPlayerListEntry(
													this.client,
													this.parent,
													profile.id(),
													profile.name(),
													this.client.getSkinProvider().supplySkinTextures(profile, true),
													true
											);
									socialInteractionsPlayerListEntryx.setOffline(true);
									return socialInteractionsPlayerListEntryx;
								}
						);
					}
					else {
						socialInteractionsPlayerListEntry = entries.get(uuid);
						if (socialInteractionsPlayerListEntry == null) {
							return;
						}
					}

					socialInteractionsPlayerListEntry.setSentMessage(true);
				}
		);
	}

	private static Map<UUID, GameProfile> collectReportableProfiles(ChatLog log) {
		Map<UUID, GameProfile> map = new Object2ObjectLinkedOpenHashMap();

		for (int i = log.getMaxIndex(); i >= log.getMinIndex(); i--) {
			if (log.get(i) instanceof ReceivedMessage.ChatMessage chatMessage && chatMessage.message().hasSignature()) {
				map.put(chatMessage.getSenderUuid(), chatMessage.profile());
			}
		}

		return map;
	}

	private void sortPlayers() {
		this.players.sort(Comparator.<SocialInteractionsPlayerListEntry, Integer>comparing(player -> {
			if (this.client.uuidEquals(player.getUuid())) {
				return 0;
			}
			else if (this.client.getAbuseReportContext().draftPlayerUuidEquals(player.getUuid())) {
				return 1;
			}
			else if (player.getUuid().version() == 2) {
				return 4;
			}
			else {
				return player.hasSentMessage() ? 2 : 3;
			}
		}).thenComparing(player -> {
			if (!player.getName().isBlank()) {
				int i = player.getName().codePointAt(0);
				if (i == 95 || i >= 97 && i <= 122 || i >= 65 && i <= 90 || i >= 48 && i <= 57) {
					return 0;
				}
			}

			return 1;
		}).thenComparing(SocialInteractionsPlayerListEntry::getName, String::compareToIgnoreCase));
	}

	private void refresh(Collection<SocialInteractionsPlayerListEntry> players, double scrollAmount) {
		this.players.clear();
		this.players.addAll(players);
		this.sortPlayers();
		this.filterPlayers();
		this.replaceEntries(this.players);
		this.setScrollY(scrollAmount);
	}

	private void filterPlayers() {
		if (this.currentSearch != null) {
			this.players.removeIf(player -> !player.getName().toLowerCase(Locale.ROOT).contains(this.currentSearch));
			this.replaceEntries(this.players);
		}
	}

	public void setCurrentSearch(String currentSearch) {
		this.currentSearch = currentSearch;
	}

	public boolean isEmpty() {
		return this.players.isEmpty();
	}

	public void setPlayerOnline(PlayerListEntry player, SocialInteractionsScreen.Tab tab) {
		UUID uUID = player.getProfile().id();

		for (SocialInteractionsPlayerListEntry socialInteractionsPlayerListEntry : this.players) {
			if (socialInteractionsPlayerListEntry.getUuid().equals(uUID)) {
				socialInteractionsPlayerListEntry.setOffline(false);
				return;
			}
		}

		if ((tab == SocialInteractionsScreen.Tab.ALL || this.client.getSocialInteractionsManager().isPlayerMuted(uUID))
				&& (Strings.isNullOrEmpty(this.currentSearch) || player
				.getProfile()
				.name()
				.toLowerCase(Locale.ROOT)
				.contains(this.currentSearch)
		)) {
			boolean bl = player.hasPublicKey();
			SocialInteractionsPlayerListEntry
					socialInteractionsPlayerListEntryx =
					new SocialInteractionsPlayerListEntry(
							this.client,
							this.parent,
							player.getProfile().id(),
							player.getProfile().name(),
							player::getSkinTextures,
							bl
					);
			this.addEntry(socialInteractionsPlayerListEntryx);
			this.players.add(socialInteractionsPlayerListEntryx);
		}
	}

	public void setPlayerOffline(UUID uuid) {
		for (SocialInteractionsPlayerListEntry socialInteractionsPlayerListEntry : this.players) {
			if (socialInteractionsPlayerListEntry.getUuid().equals(uuid)) {
				socialInteractionsPlayerListEntry.setOffline(true);
				return;
			}
		}
	}

	public void updateHasDraftReport() {
		this.players.forEach(player -> player.updateHasDraftReport(this.client.getAbuseReportContext()));
	}
}
