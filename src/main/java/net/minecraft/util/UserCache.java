package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.logging.LogUtils;
import net.minecraft.server.PlayerConfigEntry;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Кэш профилей игроков: хранит соответствие имя↔UUID с датой истечения срока действия.
 * Персистируется в JSON-файл; поддерживает до {@value #MAX_SAVED_ENTRIES} записей.
 */
public class UserCache implements NameToIdCache {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_SAVED_ENTRIES = 1000;
	private static final int EXPIRY_MONTHS = 1;

	private boolean offlineMode = true;
	private final Map<String, Entry> byName = new ConcurrentHashMap<>();
	private final Map<UUID, Entry> byUuid = new ConcurrentHashMap<>();
	private final GameProfileRepository profileRepository;
	private final Gson gson = new GsonBuilder().create();
	private final java.io.File cacheFile;
	private final AtomicLong accessCount = new AtomicLong();

	public UserCache(GameProfileRepository profileRepository, java.io.File cacheFile) {
		this.profileRepository = profileRepository;
		this.cacheFile = cacheFile;
		Lists.reverse(load()).forEach(this::add);
	}

	private void add(Entry entry) {
		PlayerConfigEntry player = entry.getPlayer();
		entry.setLastAccessed(incrementAndGetAccessCount());
		byName.put(player.name().toLowerCase(Locale.ROOT), entry);
		byUuid.put(player.id(), entry);
	}

	private Optional<PlayerConfigEntry> findProfileByName(GameProfileRepository repository, String name) {
		if (!StringHelper.isValidPlayerName(name)) {
			return getOfflinePlayerProfile(name);
		}

		Optional<PlayerConfigEntry> found = repository.findProfileByName(name).map(PlayerConfigEntry::new);
		return found.isEmpty() ? getOfflinePlayerProfile(name) : found;
	}

	private Optional<PlayerConfigEntry> getOfflinePlayerProfile(String name) {
		return offlineMode ? Optional.of(PlayerConfigEntry.fromNickname(name)) : Optional.empty();
	}

	@Override
	public void setOfflineMode(boolean offlineMode) {
		this.offlineMode = offlineMode;
	}

	@Override
	public void add(PlayerConfigEntry player) {
		addToCache(player);
	}

	private Entry addToCache(PlayerConfigEntry player) {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
		calendar.setTime(new Date());
		calendar.add(Calendar.MONTH, EXPIRY_MONTHS);
		Entry entry = new Entry(player, calendar.getTime());
		add(entry);
		save();
		return entry;
	}

	private long incrementAndGetAccessCount() {
		return accessCount.incrementAndGet();
	}

	@Override
	public Optional<PlayerConfigEntry> findByName(String name) {
		String lowerName = name.toLowerCase(Locale.ROOT);
		Entry entry = byName.get(lowerName);
		boolean expired = false;

		if (entry != null && new Date().getTime() >= entry.expirationDate.getTime()) {
			byUuid.remove(entry.getPlayer().id());
			byName.remove(entry.getPlayer().name().toLowerCase(Locale.ROOT));
			expired = true;
			entry = null;
		}

		if (entry != null) {
			entry.setLastAccessed(incrementAndGetAccessCount());
			return Optional.of(entry.getPlayer());
		}

		Optional<PlayerConfigEntry> found = findProfileByName(profileRepository, lowerName);

		if (found.isPresent()) {
			expired = false;
			return Optional.of(addToCache(found.get()).getPlayer());
		}

		if (expired) {
			save();
		}

		return Optional.empty();
	}

	@Override
	public Optional<PlayerConfigEntry> getByUuid(UUID uuid) {
		Entry entry = byUuid.get(uuid);

		if (entry == null) {
			return Optional.empty();
		}

		entry.setLastAccessed(incrementAndGetAccessCount());
		return Optional.of(entry.getPlayer());
	}

	private static DateFormat getDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
	}

	private List<Entry> load() {
		List<Entry> entries = Lists.newArrayList();

		try (Reader reader = Files.newReader(cacheFile, StandardCharsets.UTF_8)) {
			JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

			if (jsonArray == null) {
				return entries;
			}

			DateFormat dateFormat = getDateFormat();
			jsonArray.forEach(json -> entryFromJson(json, dateFormat).ifPresent(entries::add));
			return entries;
		}
		catch (FileNotFoundException ignored) {
		}
		catch (JsonParseException | IOException exception) {
			LOGGER.warn("Failed to load profile cache {}", cacheFile, exception);
		}

		return entries;
	}

	@Override
	public void save() {
		JsonArray jsonArray = new JsonArray();
		DateFormat dateFormat = getDateFormat();
		getLastAccessedEntries(MAX_SAVED_ENTRIES).forEach(entry -> jsonArray.add(entryToJson(entry, dateFormat)));
		String json = gson.toJson(jsonArray);

		try (Writer writer = Files.newWriter(cacheFile, StandardCharsets.UTF_8)) {
			writer.write(json);
		}
		catch (IOException ignored) {
		}
	}

	private Stream<Entry> getLastAccessedEntries(int limit) {
		return ImmutableList
				.copyOf(byUuid.values())
				.stream()
				.sorted(Comparator.comparing(Entry::getLastAccessed).reversed())
				.limit(limit);
	}

	private static JsonElement entryToJson(Entry entry, DateFormat dateFormat) {
		JsonObject jsonObject = new JsonObject();
		entry.getPlayer().write(jsonObject);
		jsonObject.addProperty("expiresOn", dateFormat.format(entry.getExpirationDate()));
		return jsonObject;
	}

	private static Optional<Entry> entryFromJson(JsonElement json, DateFormat dateFormat) {
		if (!json.isJsonObject()) {
			return Optional.empty();
		}

		JsonObject jsonObject = json.getAsJsonObject();
		PlayerConfigEntry player = PlayerConfigEntry.read(jsonObject);

		if (player == null) {
			return Optional.empty();
		}

		JsonElement expiresOnElement = jsonObject.get("expiresOn");

		if (expiresOnElement == null) {
			return Optional.empty();
		}

		String expiresOnString = expiresOnElement.getAsString();

		try {
			Date expirationDate = dateFormat.parse(expiresOnString);
			return Optional.of(new Entry(player, expirationDate));
		}
		catch (ParseException exception) {
			LOGGER.warn("Failed to parse date {}", expiresOnString, exception);
			return Optional.empty();
		}
	}

	static class Entry {

		private final PlayerConfigEntry player;
		final Date expirationDate;
		private volatile long lastAccessed;

		Entry(PlayerConfigEntry player, Date expirationDate) {
			this.player = player;
			this.expirationDate = expirationDate;
		}

		public PlayerConfigEntry getPlayer() {
			return player;
		}

		public Date getExpirationDate() {
			return expirationDate;
		}

		public void setLastAccessed(long lastAccessed) {
			this.lastAccessed = lastAccessed;
		}

		public long getLastAccessed() {
			return lastAccessed;
		}
	}
}
