package net.minecraft.client.font;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteAtlasGlyphs;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.*;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Центральный менеджер шрифтов клиента. Управляет загрузкой, перезагрузкой и хранением
 * всех {@link FontStorage} по идентификаторам, а также предоставляет {@link TextRenderer}.
 */
@Environment(EnvType.CLIENT)
public class FontManager implements ResourceReloader, AutoCloseable {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final String FONTS_JSON = "fonts.json";
	public static final Identifier MISSING_STORAGE_ID = Identifier.ofVanilla("missing");
	private static final ResourceFinder FINDER = ResourceFinder.json("font");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	final FontStorage missingStorage;
	private final List<Font> fonts = new ArrayList<>();
	private final Map<Identifier, FontStorage> fontStorages = new HashMap<>();
	private final TextureManager textureManager;
	private final FontManager.Fonts anyFonts = new FontManager.Fonts(false);
	private final FontManager.Fonts advanceValidatedFonts = new FontManager.Fonts(true);
	private final AtlasManager atlasManager;
	private final Map<Identifier, SpriteAtlasGlyphs> spriteGlyphs = new HashMap<>();
	final PlayerHeadGlyphs playerHeadGlyphs;

	public FontManager(TextureManager textureManager, AtlasManager atlasManager, PlayerSkinCache playerSkinCache) {
		this.textureManager = textureManager;
		this.atlasManager = atlasManager;
		missingStorage = createFontStorage(MISSING_STORAGE_ID, List.of(createEmptyFont()), Set.of());
		playerHeadGlyphs = new PlayerHeadGlyphs(playerSkinCache);
	}

	private FontStorage createFontStorage(
			Identifier fontId,
			List<Font.FontFilterPair> allFonts,
			Set<FontFilterType> filters
	) {
		GlyphBaker glyphBaker = new GlyphBaker(textureManager, fontId);
		FontStorage fontStorage = new FontStorage(glyphBaker);
		fontStorage.setFonts(allFonts, filters);
		return fontStorage;
	}

	private static Font.FontFilterPair createEmptyFont() {
		return new Font.FontFilterPair(new BlankFont(), FontFilterType.FilterMap.NO_FILTER);
	}

	@Override
	public CompletableFuture<Void> reload(
			ResourceReloader.Store store,
			Executor executor,
			ResourceReloader.Synchronizer synchronizer,
			Executor executor2
	) {
		return loadIndex(store.getResourceManager(), executor)
				.thenCompose(synchronizer::whenPrepared)
				.thenAcceptAsync(index -> reload(index, Profilers.get()), executor2);
	}

	/**
	 * Асинхронно загружает индекс всех провайдеров шрифтов из ресурсов.
	 * Для каждого {@code fonts.json} создаёт {@link FontEntry}, разрешает зависимости
	 * между шрифтами через {@link DependencyTracker} и собирает итоговый {@link ProviderIndex}.
	 */
	private CompletableFuture<FontManager.ProviderIndex> loadIndex(ResourceManager resourceManager, Executor executor) {
		List<CompletableFuture<FontManager.FontEntry>> futures = new ArrayList<>();

		for (Entry<Identifier, List<Resource>> entry : FINDER.findAllResources(resourceManager).entrySet()) {
			Identifier fontId = FINDER.toResourceId(entry.getKey());
			futures.add(CompletableFuture.supplyAsync(
					() -> {
						List<Pair<FontManager.FontKey, FontLoader.Provider>> providers =
								loadFontProviders(entry.getValue(), fontId);
						FontManager.FontEntry fontEntry = new FontManager.FontEntry(fontId);

						for (Pair<FontManager.FontKey, FontLoader.Provider> pair : providers) {
							FontManager.FontKey fontKey = pair.getFirst();
							FontFilterType.FilterMap filterMap = pair.getSecond().filter();
							pair.getSecond().definition().build()
									.ifLeft(loadable -> {
										CompletableFuture<Optional<Font>> fontFuture =
												load(fontKey, loadable, resourceManager, executor);
										fontEntry.addBuilder(fontKey, filterMap, fontFuture);
									})
									.ifRight(reference -> fontEntry.addReferenceBuilder(fontKey, filterMap, reference));
						}

						return fontEntry;
					}, executor
			));
		}

		return Util.combineSafe(futures)
				.thenCompose(entries -> {
					List<CompletableFuture<Optional<Font>>> immediateFutures = entries.stream()
							.flatMap(FontManager.FontEntry::getImmediateProviders)
							.collect(Util.toArrayList());
					Font.FontFilterPair emptyFont = createEmptyFont();
					immediateFutures.add(CompletableFuture.completedFuture(Optional.of(emptyFont.provider())));

					return Util.combineSafe(immediateFutures)
							.thenCompose(resolvedFonts -> {
								Map<Identifier, List<Font.FontFilterPair>> fontSets =
										getRequiredFontProviders(entries);
								CompletableFuture<?>[] insertionTasks = fontSets.values()
										.stream()
										.map(dest -> CompletableFuture.runAsync(
												() -> insertFont(dest, emptyFont),
												executor
										))
										.toArray(CompletableFuture[]::new);

								return CompletableFuture.allOf(insertionTasks)
										.thenApply(ignored -> {
											List<Font> allFonts = resolvedFonts.stream()
													.flatMap(Optional::stream)
													.toList();
											return new FontManager.ProviderIndex(fontSets, allFonts);
										});
							});
				});
	}

	private CompletableFuture<Optional<Font>> load(
			FontManager.FontKey key,
			FontLoader.Loadable loadable,
			ResourceManager resourceManager,
			Executor executor
	) {
		return CompletableFuture.supplyAsync(
				() -> {
					try {
						return Optional.of(loadable.load(resourceManager));
					} catch (Exception exception) {
						LOGGER.warn("Failed to load builder {}, rejecting", key, exception);
						return Optional.empty();
					}
				}, executor
		);
	}

	private Map<Identifier, List<Font.FontFilterPair>> getRequiredFontProviders(List<FontManager.FontEntry> entries) {
		Map<Identifier, List<Font.FontFilterPair>> fontSets = new HashMap<>();
		DependencyTracker<Identifier, FontManager.FontEntry> dependencyTracker = new DependencyTracker<>();
		entries.forEach(entry -> dependencyTracker.add(entry.fontId, entry));
		dependencyTracker.traverse(
				(dependent, fontEntry) -> fontEntry
						.getRequiredFontProviders(fontSets::get)
						.ifPresent(resolved -> fontSets.put(dependent, resolved))
		);
		return fontSets;
	}

	private void insertFont(List<Font.FontFilterPair> fonts, Font.FontFilterPair font) {
		fonts.add(0, font);
		IntSet codePoints = new IntOpenHashSet();

		for (Font.FontFilterPair pair : fonts) {
			codePoints.addAll(pair.provider().getProvidedGlyphs());
		}

		codePoints.forEach(codePoint -> {
			if (codePoint != 32) {
				for (Font.FontFilterPair pair : Lists.reverse(fonts)) {
					if (pair.provider().getGlyph(codePoint) != null) {
						break;
					}
				}
			}
		});
	}

	private static Set<FontFilterType> getActiveFilters(GameOptions options) {
		Set<FontFilterType> activeFilters = EnumSet.noneOf(FontFilterType.class);

		if (options.getForceUnicodeFont().getValue()) {
			activeFilters.add(FontFilterType.UNIFORM);
		}

		if (options.getJapaneseGlyphVariants().getValue()) {
			activeFilters.add(FontFilterType.JAPANESE_VARIANTS);
		}

		return activeFilters;
	}

	private void reload(FontManager.ProviderIndex index, Profiler profiler) {
		profiler.push("closing");
		anyFonts.clear();
		advanceValidatedFonts.clear();
		fontStorages.values().forEach(FontStorage::close);
		fontStorages.clear();
		fonts.forEach(Font::close);
		fonts.clear();

		Set<FontFilterType> activeFilters = getActiveFilters(MinecraftClient.getInstance().options);
		profiler.swap("reloading");
		index.fontSets().forEach((id, fontList) -> fontStorages.put(
				id,
				createFontStorage(id, Lists.reverse(fontList), activeFilters)
		));
		fonts.addAll(index.allProviders);
		profiler.pop();

		if (!fontStorages.containsKey(MinecraftClient.DEFAULT_FONT_ID)) {
			throw new IllegalStateException("Default font failed to load");
		}

		spriteGlyphs.clear();
		atlasManager.acceptAtlasTextures((definitionId, atlasTexture) -> spriteGlyphs.put(
				definitionId,
				new SpriteAtlasGlyphs(atlasTexture)
		));
	}

	public void setActiveFilters(GameOptions options) {
		Set<FontFilterType> activeFilters = getActiveFilters(options);

		for (FontStorage fontStorage : fontStorages.values()) {
			fontStorage.setActiveFilters(activeFilters);
		}
	}

	private static List<Pair<FontManager.FontKey, FontLoader.Provider>> loadFontProviders(
			List<Resource> fontResources,
			Identifier id
	) {
		List<Pair<FontManager.FontKey, FontLoader.Provider>> result = new ArrayList<>();

		for (Resource resource : fontResources) {
			try (Reader reader = resource.getReader()) {
				JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
				FontManager.Providers providers = FontManager.Providers.CODEC
						.parse(JsonOps.INSTANCE, jsonElement)
						.getOrThrow(JsonParseException::new);
				List<FontLoader.Provider> providerList = providers.providers;

				for (int index = providerList.size() - 1; index >= 0; index--) {
					FontManager.FontKey fontKey = new FontManager.FontKey(id, resource.getPackId(), index);
					result.add(Pair.of(fontKey, providerList.get(index)));
				}
			} catch (Exception exception) {
				LOGGER.warn(
						"Unable to load font '{}' in {} in resourcepack: '{}'",
						id, FONTS_JSON, resource.getPackId(), exception
				);
			}
		}

		return result;
	}

	public TextRenderer createTextRenderer() {
		return new TextRenderer(anyFonts);
	}

	public TextRenderer createAdvanceValidatingTextRenderer() {
		return new TextRenderer(advanceValidatedFonts);
	}

	FontStorage getStorageInternal(Identifier id) {
		return fontStorages.getOrDefault(id, missingStorage);
	}

	GlyphProvider getSpriteGlyphs(StyleSpriteSource.Sprite description) {
		SpriteAtlasGlyphs atlasGlyphs = spriteGlyphs.get(description.atlasId());
		return atlasGlyphs == null
				? missingStorage.getGlyphs(false)
				: atlasGlyphs.getGlyphProvider(description.spriteId());
	}

	@Override
	public void close() {
		anyFonts.close();
		advanceValidatedFonts.close();
		fontStorages.values().forEach(FontStorage::close);
		fonts.forEach(Font::close);
		missingStorage.close();
	}

	/**
	 * Строитель списка провайдеров для одного шрифта. Хранит либо уже загружаемый
	 * {@link CompletableFuture}, либо ссылку на другой шрифт по идентификатору.
	 */
	@Environment(EnvType.CLIENT)
	record Builder(
			FontManager.FontKey id,
			FontFilterType.FilterMap filter,
			Either<CompletableFuture<Optional<Font>>, Identifier> result
	) {

		public Optional<List<Font.FontFilterPair>> build(Function<Identifier, @Nullable List<Font.FontFilterPair>> fontRetriever) {
			return result.map(
					future -> future.join().map(font -> List.of(new Font.FontFilterPair(font, filter))),
					referee -> {
						List<Font.FontFilterPair> resolved = fontRetriever.apply(referee);
						if (resolved == null) {
							FontManager.LOGGER.warn(
									"Can't find font {} referenced by builder {}, either because it's missing, failed to load or is part of loading cycle",
									referee,
									id
							);
							return Optional.empty();
						}

						return Optional.of(resolved.stream().map(this::applyFilter).toList());
					}
			);
		}

		private Font.FontFilterPair applyFilter(Font.FontFilterPair font) {
			return new Font.FontFilterPair(font.provider(), filter.apply(font.filter()));
		}
	}

	/**
	 * Запись, описывающая один шрифт и все его строители (провайдеры).
	 * Реализует {@link DependencyTracker.Dependencies} для разрешения ссылочных зависимостей между шрифтами.
	 */
	@Environment(EnvType.CLIENT)
	record FontEntry(
			Identifier fontId,
			List<FontManager.Builder> builders,
			Set<Identifier> dependencies
	) implements DependencyTracker.Dependencies<Identifier> {

		public FontEntry(Identifier fontId) {
			this(fontId, new ArrayList<>(), new HashSet<>());
		}

		public void addReferenceBuilder(
				FontManager.FontKey key,
				FontFilterType.FilterMap filters,
				FontLoader.Reference reference
		) {
			builders.add(new FontManager.Builder(key, filters, Either.right(reference.id())));
			dependencies.add(reference.id());
		}

		public void addBuilder(
				FontManager.FontKey key,
				FontFilterType.FilterMap filters,
				CompletableFuture<Optional<Font>> fontFuture
		) {
			builders.add(new FontManager.Builder(key, filters, Either.left(fontFuture)));
		}

		private Stream<CompletableFuture<Optional<Font>>> getImmediateProviders() {
			return builders.stream().flatMap(builder -> builder.result.left().stream());
		}

		public Optional<List<Font.FontFilterPair>> getRequiredFontProviders(Function<Identifier, List<Font.FontFilterPair>> fontRetriever) {
			List<Font.FontFilterPair> collected = new ArrayList<>();

			for (FontManager.Builder builder : builders) {
				Optional<List<Font.FontFilterPair>> resolved = builder.build(fontRetriever);
				if (resolved.isEmpty()) {
					return Optional.empty();
				}

				collected.addAll(resolved.get());
			}

			return Optional.of(collected);
		}

		@Override
		public void forDependencies(Consumer<Identifier> callback) {
			dependencies.forEach(callback);
		}

		@Override
		public void forOptionalDependencies(Consumer<Identifier> callback) {
		}
	}

	@Environment(EnvType.CLIENT)
	record FontKey(Identifier fontId, String pack, int index) {

		@Override
		public String toString() {
			return "(" + fontId + ": builder #" + index + " from pack " + pack + ")";
		}
	}

	@Environment(EnvType.CLIENT)
	class Fonts implements TextRenderer.GlyphsProvider, AutoCloseable {

		private final boolean advanceValidating;
		private volatile FontManager.Fonts.@Nullable Cached cached;
		private volatile @Nullable EffectGlyph rectangle;

		Fonts(final boolean advanceValidating) {
			this.advanceValidating = advanceValidating;
		}

		public void clear() {
			cached = null;
			rectangle = null;
		}

		@Override
		public void close() {
			clear();
		}

		private GlyphProvider getGlyphsImpl(StyleSpriteSource source) {
			return switch (source) {
				case StyleSpriteSource.Font font ->
						FontManager.this.getStorageInternal(font.id()).getGlyphs(advanceValidating);
				case StyleSpriteSource.Sprite sprite -> FontManager.this.getSpriteGlyphs(sprite);
				case StyleSpriteSource.Player player -> FontManager.this.playerHeadGlyphs.get(player);
				default -> FontManager.this.missingStorage.getGlyphs(advanceValidating);
			};
		}

		@Override
		public GlyphProvider getGlyphs(StyleSpriteSource source) {
			FontManager.Fonts.Cached snapshot = cached;
			if (snapshot != null && source.equals(snapshot.source)) {
				return snapshot.glyphs;
			}

			GlyphProvider glyphProvider = getGlyphsImpl(source);
			cached = new FontManager.Fonts.Cached(source, glyphProvider);
			return glyphProvider;
		}

		@Override
		public EffectGlyph getRectangleGlyph() {
			EffectGlyph effectGlyph = rectangle;
			if (effectGlyph == null) {
				effectGlyph = FontManager.this.getStorageInternal(StyleSpriteSource.DEFAULT.id()).getRectangleBakedGlyph();
				rectangle = effectGlyph;
			}

			return effectGlyph;
		}

		@Environment(EnvType.CLIENT)
		record Cached(StyleSpriteSource source, GlyphProvider glyphs) {
		}
	}

	@Environment(EnvType.CLIENT)
	record ProviderIndex(Map<Identifier, List<Font.FontFilterPair>> fontSets, List<Font> allProviders) {
	}

	@Environment(EnvType.CLIENT)
	record Providers(List<FontLoader.Provider> providers) {

		public static final Codec<FontManager.Providers> CODEC = RecordCodecBuilder.create(
				instance -> instance
						.group(FontLoader.Provider.CODEC
								.listOf()
								.fieldOf("providers")
								.forGetter(FontManager.Providers::providers))
						.apply(instance, FontManager.Providers::new)
		);
	}
}
