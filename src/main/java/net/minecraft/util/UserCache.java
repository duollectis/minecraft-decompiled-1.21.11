package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.*;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.logging.LogUtils;
import net.minecraft.server.PlayerConfigEntry;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * {@code UserCache}.
 */
public class UserCache implements NameToIdCache {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_SAVED_ENTRIES = 1000;
	private static final int CACHE_VERSION = 1;
	private boolean offlineMode = true;
	private final Map<String, UserCache.Entry> byName = new ConcurrentHashMap<>();
	private final Map<UUID, UserCache.Entry> byUuid = new ConcurrentHashMap<>();
	private final GameProfileRepository profileRepository;
	private final Gson gson = new GsonBuilder().create();
	private final File cacheFile;
	private final AtomicLong accessCount = new AtomicLong();

	public UserCache(GameProfileRepository profileRepository, File cacheFile) {
		this.profileRepository = profileRepository;
		this.cacheFile = cacheFile;
		Lists.reverse(this.load()).forEach(this::add);
	}

	private void add(UserCache.Entry entry) {
		PlayerConfigEntry playerConfigEntry = entry.getPlayer();
		entry.setLastAccessed(this.incrementAndGetAccessCount());
		this.byName.put(playerConfigEntry.name().toLowerCase(Locale.ROOT), entry);
		this.byUuid.put(playerConfigEntry.id(), entry);
	}

	private Optional<PlayerConfigEntry> findProfileByName(GameProfileRepository repository, String string) {
		if (!StringHelper.isValidPlayerName(string)) {
			return this.getOfflinePlayerProfile(string);
		}
		else {
			Optional<PlayerConfigEntry> optional = repository.findProfileByName(string).map(PlayerConfigEntry::new);
			return optional.isEmpty() ? this.getOfflinePlayerProfile(string) : optional;
		}
	}

	private Optional<PlayerConfigEntry> getOfflinePlayerProfile(String string) {
		return this.offlineMode ? Optional.of(PlayerConfigEntry.fromNickname(string)) : Optional.empty();
	}

	@Override
	public void setOfflineMode(boolean offlineMode) {
		this.offlineMode = offlineMode;
	}

	@Override
	public void add(PlayerConfigEntry player) {
		this.addToCache(player);
	}

	private UserCache.Entry addToCache(PlayerConfigEntry player) {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
		calendar.setTime(new Date());
		calendar.add(2, 1);
		Date date = calendar.getTime();
		UserCache.Entry entry = new UserCache.Entry(player, date);
		this.add(entry);
		this.save();
		return entry;
	}

	private long incrementAndGetAccessCount() {
		return this.accessCount.incrementAndGet();
	}

	@Override
	public Optional<PlayerConfigEntry> findByName(String name) {
		String string = name.toLowerCase(Locale.ROOT);
		UserCache.Entry entry = this.byName.get(string);
		boolean bl = false;
		if (entry != null && new Date().getTime() >= entry.expirationDate.getTime()) {
			this.byUuid.remove(entry.getPlayer().id());
			this.byName.remove(entry.getPlayer().name().toLowerCase(Locale.ROOT));
			bl = true;
			entry = null;
		}

		Optional<PlayerConfigEntry> optional;
		if (entry != null) {
			entry.setLastAccessed(this.incrementAndGetAccessCount());
			optional = Optional.of(entry.getPlayer());
		}
		else {
			Optional<PlayerConfigEntry> optional2 = this.findProfileByName(this.profileRepository, string);
			if (optional2.isPresent()) {
				optional = Optional.of(this.addToCache(optional2.get()).getPlayer());
				bl = false;
			}
			else {
				optional = Optional.empty();
			}
		}

		if (bl) {
			this.save();
		}

		return optional;
	}

	@Override
	public Optional<PlayerConfigEntry> getByUuid(UUID uuid) {
		UserCache.Entry entry = this.byUuid.get(uuid);
		if (entry == null) {
			return Optional.empty();
		}
		else {
			entry.setLastAccessed(this.incrementAndGetAccessCount());
			return Optional.of(entry.getPlayer());
		}
	}

	private static DateFormat getDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
	}

	private List<UserCache.Entry> load() {
		List<UserCache.Entry> list = Lists.newArrayList();

		try {
			Object var9;
			try (Reader reader = Files.newReader(this.cacheFile, StandardCharsets.UTF_8)) {
				JsonArray jsonArray = (JsonArray) this.gson.fromJson(reader, JsonArray.class);
				if (jsonArray != null) {
					DateFormat dateFormat = getDateFormat();
					jsonArray.forEach(json -> entryFromJson(json, dateFormat).ifPresent(list::add));
					return list;
				}

				var9 = list;
			}

			return (List<UserCache.Entry>) var9;
		}
		catch (FileNotFoundException var7) {
		}
		catch (JsonParseException | IOException var8) {
			LOGGER.warn("Failed to load profile cache {}", this.cacheFile, var8);
		}

		return list;
	}

	@Override
	public void save() {
		JsonArray jsonArray = new JsonArray();
		DateFormat dateFormat = getDateFormat();
		this.getLastAccessedEntries(1000).forEach(entry -> jsonArray.add(entryToJson(entry, dateFormat)));
		String string = this.gson.toJson(jsonArray);

		try (Writer writer = Files.newWriter(this.cacheFile, StandardCharsets.UTF_8)) {
			writer.write(string);
		}
		catch (IOException var9) {
		}
	}

	private Stream<UserCache.Entry> getLastAccessedEntries(int limit) {
		return ImmutableList
				.copyOf(this.byUuid.values())
				.stream()
				.sorted(Comparator.comparing(UserCache.Entry::getLastAccessed).reversed())
				.limit(limit);
	}

	private static JsonElement entryToJson(UserCache.Entry entry, DateFormat dateFormat) {
		JsonObject jsonObject = new JsonObject();
		entry.getPlayer().write(jsonObject);
		jsonObject.addProperty("expiresOn", dateFormat.format(entry.getExpirationDate()));
		return jsonObject;
	}

	private static Optional<UserCache.Entry> entryFromJson(JsonElement json, DateFormat dateFormat) {
		if (json.isJsonObject()) {
			JsonObject jsonObject = json.getAsJsonObject();
			PlayerConfigEntry playerConfigEntry = PlayerConfigEntry.read(jsonObject);
			if (playerConfigEntry != null) {
				JsonElement jsonElement = jsonObject.get("expiresOn");
				if (jsonElement != null) {
					String string = jsonElement.getAsString();

					try {
						Date date = dateFormat.parse(string);
						return Optional.of(new UserCache.Entry(playerConfigEntry, date));
					}
					catch (ParseException var7) {
						LOGGER.warn("Failed to parse date {}", string, var7);
					}
				}
			}
		}

		return Optional.empty();
	}

	/**
	 * {@code Entry}.
	 */
	static class Entry {

		private final PlayerConfigEntry player;
		final Date expirationDate;
		private volatile long lastAccessed;

		Entry(PlayerConfigEntry player, Date expirationDate) {
			this.player = player;
			this.expirationDate = expirationDate;
		}

		public PlayerConfigEntry getPlayer() {
			return this.player;
		}

		public Date getExpirationDate() {
			return this.expirationDate;
		}

		public void setLastAccessed(long lastAccessed) {
			this.lastAccessed = lastAccessed;
		}

		public long getLastAccessed() {
			return this.lastAccessed;
		}
	}
}
