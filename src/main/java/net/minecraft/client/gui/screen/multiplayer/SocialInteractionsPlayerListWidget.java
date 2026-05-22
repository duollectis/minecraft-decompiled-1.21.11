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

/**
 * Виджет списка игроков для экрана социальных взаимодействий.
 * Поддерживает фильтрацию по поиску, сортировку и обновление онлайн/оффлайн статуса.
 */
@Environment(EnvType.CLIENT)
public class SocialInteractionsPlayerListWidget extends ElementListWidget<SocialInteractionsPlayerListEntry> {

	private static final int UUID_VERSION_NPC = 2;

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
		context.enableScissor(getX(), getY() + 4, getRight(), getBottom());
	}

	/**
	 * Обновляет список игроков по набору UUID. Опционально включает оффлайн-игроков
	 * из истории чата и помечает тех, кто отправлял сообщения.
	 */
	public void update(Collection<UUID> uuids, double scrollAmount, boolean includeOffline) {
		Map<UUID, SocialInteractionsPlayerListEntry> entriesByUuid = new HashMap<>();
		setPlayers(uuids, entriesByUuid);
		if (includeOffline) {
			collectOfflinePlayers(entriesByUuid);
		}

		markOfflineMembers(entriesByUuid, includeOffline);
		refresh(entriesByUuid.values(), scrollAmount);
	}

	private void setPlayers(Collection<UUID> playerUuids, Map<UUID, SocialInteractionsPlayerListEntry> entriesByUuid) {
		ClientPlayNetworkHandler networkHandler = client.player.networkHandler;
		for (UUID uuid : playerUuids) {
			PlayerListEntry playerEntry = networkHandler.getPlayerListEntry(uuid);
			if (playerEntry == null) {
				continue;
			}

			entriesByUuid.put(uuid, createListEntry(uuid, playerEntry));
		}
	}

	private void collectOfflinePlayers(Map<UUID, SocialInteractionsPlayerListEntry> entriesByUuid) {
		Map<UUID, PlayerListEntry> seenPlayers = client.player.networkHandler.getSeenPlayers();
		for (Map.Entry<UUID, PlayerListEntry> entry : seenPlayers.entrySet()) {
			entriesByUuid.computeIfAbsent(
				entry.getKey(), uuid -> {
					SocialInteractionsPlayerListEntry listEntry = createListEntry(uuid, entry.getValue());
					listEntry.setOffline(true);
					return listEntry;
				}
			);
		}
	}

	private SocialInteractionsPlayerListEntry createListEntry(UUID uuid, PlayerListEntry playerEntry) {
		return new SocialInteractionsPlayerListEntry(
			client,
			parent,
			uuid,
			playerEntry.getProfile().name(),
			playerEntry::getSkinTextures,
			playerEntry.hasPublicKey()
		);
	}

	private void markOfflineMembers(Map<UUID, SocialInteractionsPlayerListEntry> entries, boolean includeOffline) {
		Map<UUID, GameProfile> reportableProfiles = collectReportableProfiles(client.getAbuseReportContext().getChatLog());
		reportableProfiles.forEach(
			(uuid, profile) -> {
				SocialInteractionsPlayerListEntry entry;
				if (includeOffline) {
					entry = entries.computeIfAbsent(
						uuid,
						key -> {
							SocialInteractionsPlayerListEntry newEntry = new SocialInteractionsPlayerListEntry(
								client,
								parent,
								profile.id(),
								profile.name(),
								client.getSkinProvider().supplySkinTextures(profile, true),
								true
							);
							newEntry.setOffline(true);
							return newEntry;
						}
					);
				}
				else {
					entry = entries.get(uuid);
					if (entry == null) {
						return;
					}
				}

				entry.setSentMessage(true);
			}
		);
	}

	private static Map<UUID, GameProfile> collectReportableProfiles(ChatLog log) {
		Map<UUID, GameProfile> profiles = new Object2ObjectLinkedOpenHashMap<>();
		for (int index = log.getMaxIndex(); index >= log.getMinIndex(); index--) {
			if (log.get(index) instanceof ReceivedMessage.ChatMessage chatMessage && chatMessage.message().hasSignature()) {
				profiles.put(chatMessage.getSenderUuid(), chatMessage.profile());
			}
		}

		return profiles;
	}

	private void sortPlayers() {
		players.sort(
			Comparator.<SocialInteractionsPlayerListEntry, Integer>comparing(player -> {
				if (client.uuidEquals(player.getUuid())) {
					return 0;
				}
				else if (client.getAbuseReportContext().draftPlayerUuidEquals(player.getUuid())) {
					return 1;
				}
				else if (player.getUuid().version() == UUID_VERSION_NPC) {
					return 4;
				}

				return player.hasSentMessage() ? 2 : 3;
			}).thenComparing(player -> {
				if (!player.getName().isBlank()) {
					int codePoint = player.getName().codePointAt(0);
					if (codePoint == '_' || codePoint >= 'a' && codePoint <= 'z'
						|| codePoint >= 'A' && codePoint <= 'Z'
						|| codePoint >= '0' && codePoint <= '9') {
						return 0;
					}
				}

				return 1;
			}).thenComparing(SocialInteractionsPlayerListEntry::getName, String::compareToIgnoreCase)
		);
	}

	private void refresh(Collection<SocialInteractionsPlayerListEntry> newPlayers, double scrollAmount) {
		players.clear();
		players.addAll(newPlayers);
		sortPlayers();
		filterPlayers();
		replaceEntries(players);
		setScrollY(scrollAmount);
	}

	private void filterPlayers() {
		if (currentSearch == null) {
			return;
		}

		players.removeIf(player -> !player.getName().toLowerCase(Locale.ROOT).contains(currentSearch));
		replaceEntries(players);
	}

	public void setCurrentSearch(String currentSearch) {
		this.currentSearch = currentSearch;
	}

	public boolean isEmpty() {
		return players.isEmpty();
	}

	public void setPlayerOnline(PlayerListEntry player, SocialInteractionsScreen.Tab tab) {
		UUID uuid = player.getProfile().id();
		for (SocialInteractionsPlayerListEntry entry : players) {
			if (entry.getUuid().equals(uuid)) {
				entry.setOffline(false);
				return;
			}
		}

		boolean isRelevantTab = tab == SocialInteractionsScreen.Tab.ALL
			|| client.getSocialInteractionsManager().isPlayerMuted(uuid);
		boolean matchesSearch = Strings.isNullOrEmpty(currentSearch)
			|| player.getProfile().name().toLowerCase(Locale.ROOT).contains(currentSearch);
		if (isRelevantTab && matchesSearch) {
			SocialInteractionsPlayerListEntry newEntry = new SocialInteractionsPlayerListEntry(
				client,
				parent,
				player.getProfile().id(),
				player.getProfile().name(),
				player::getSkinTextures,
				player.hasPublicKey()
			);
			addEntry(newEntry);
			players.add(newEntry);
		}
	}

	public void setPlayerOffline(UUID uuid) {
		for (SocialInteractionsPlayerListEntry entry : players) {
			if (entry.getUuid().equals(uuid)) {
				entry.setOffline(true);
				return;
			}
		}
	}

	public void updateHasDraftReport() {
		players.forEach(player -> player.updateHasDraftReport(client.getAbuseReportContext()));
	}
}
