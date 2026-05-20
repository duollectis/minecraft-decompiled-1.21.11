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
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.texture.SpriteAtlasGlyphs;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.DependencyTracker;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

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
      this.missingStorage = this.createFontStorage(MISSING_STORAGE_ID, List.of(createEmptyFont()), Set.of());
      this.playerHeadGlyphs = new PlayerHeadGlyphs(playerSkinCache);
   }

   private FontStorage createFontStorage(Identifier fontId, List<Font.FontFilterPair> allFonts, Set<FontFilterType> filters) {
      GlyphBaker glyphBaker = new GlyphBaker(this.textureManager, fontId);
      FontStorage fontStorage = new FontStorage(glyphBaker);
      fontStorage.setFonts(allFonts, filters);
      return fontStorage;
   }

   private static Font.FontFilterPair createEmptyFont() {
      return new Font.FontFilterPair(new BlankFont(), FontFilterType.FilterMap.NO_FILTER);
   }

   @Override
   public CompletableFuture<Void> reload(ResourceReloader.Store store, Executor executor, ResourceReloader.Synchronizer synchronizer, Executor executor2) {
      return this.loadIndex(store.getResourceManager(), executor)
         .thenCompose(synchronizer::whenPrepared)
         .thenAcceptAsync(index -> this.reload(index, Profilers.get()), executor2);
   }

   private CompletableFuture<FontManager.ProviderIndex> loadIndex(ResourceManager resourceManager, Executor executor) {
      List<CompletableFuture<FontManager.FontEntry>> list = new ArrayList<>();

      for (Entry<Identifier, List<Resource>> entry : FINDER.findAllResources(resourceManager).entrySet()) {
         Identifier identifier = FINDER.toResourceId(entry.getKey());
         list.add(CompletableFuture.supplyAsync(() -> {
            List<Pair<FontManager.FontKey, FontLoader.Provider>> listx = loadFontProviders(entry.getValue(), identifier);
            FontManager.FontEntry fontEntry = new FontManager.FontEntry(identifier);

            for (Pair<FontManager.FontKey, FontLoader.Provider> pair : listx) {
               FontManager.FontKey fontKey = (FontManager.FontKey)pair.getFirst();
               FontFilterType.FilterMap filterMap = ((FontLoader.Provider)pair.getSecond()).filter();
               ((FontLoader.Provider)pair.getSecond()).definition().build().ifLeft(loadable -> {
                  CompletableFuture<Optional<Font>> completableFuture = this.load(fontKey, loadable, resourceManager, executor);
                  fontEntry.addBuilder(fontKey, filterMap, completableFuture);
               }).ifRight(reference -> fontEntry.addReferenceBuilder(fontKey, filterMap, reference));
            }

            return fontEntry;
         }, executor));
      }

      return Util.combineSafe(list)
         .thenCompose(
            entries -> {
               List<CompletableFuture<Optional<Font>>> listx = entries.stream()
                  .flatMap(FontManager.FontEntry::getImmediateProviders)
                  .collect(Util.toArrayList());
               Font.FontFilterPair fontFilterPair = createEmptyFont();
               listx.add(CompletableFuture.completedFuture(Optional.of(fontFilterPair.provider())));
               return Util.combineSafe(listx)
                  .thenCompose(
                     providers -> {
                        Map<Identifier, List<Font.FontFilterPair>> map = this.getRequiredFontProviders(entries);
                        CompletableFuture<?>[] completableFutures = map.values()
                           .stream()
                           .map(dest -> CompletableFuture.runAsync(() -> this.insertFont(dest, fontFilterPair), executor))
                           .toArray(CompletableFuture[]::new);
                        return CompletableFuture.allOf(completableFutures).thenApply(ignored -> {
                           List<Font> list2 = providers.stream().flatMap(Optional::stream).toList();
                           return new FontManager.ProviderIndex(map, list2);
                        });
                     }
                  );
            }
         );
   }

   private CompletableFuture<Optional<Font>> load(FontManager.FontKey key, FontLoader.Loadable loadable, ResourceManager resourceManager, Executor executor) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return Optional.of(loadable.load(resourceManager));
         } catch (Exception var4) {
            LOGGER.warn("Failed to load builder {}, rejecting", key, var4);
            return Optional.empty();
         }
      }, executor);
   }

   private Map<Identifier, List<Font.FontFilterPair>> getRequiredFontProviders(List<FontManager.FontEntry> entries) {
      Map<Identifier, List<Font.FontFilterPair>> map = new HashMap<>();
      DependencyTracker<Identifier, FontManager.FontEntry> dependencyTracker = new DependencyTracker<>();
      entries.forEach(entry -> dependencyTracker.add(entry.fontId, entry));
      dependencyTracker.traverse(
         (dependent, fontEntry) -> fontEntry.getRequiredFontProviders(map::get).ifPresent(fonts -> map.put(dependent, (List<Font.FontFilterPair>)fonts))
      );
      return map;
   }

   private void insertFont(List<Font.FontFilterPair> fonts, Font.FontFilterPair font) {
      fonts.add(0, font);
      IntSet intSet = new IntOpenHashSet();

      for (Font.FontFilterPair fontFilterPair : fonts) {
         intSet.addAll(fontFilterPair.provider().getProvidedGlyphs());
      }

      intSet.forEach(codePoint -> {
         if (codePoint != 32) {
            for (Font.FontFilterPair fontFilterPairx : Lists.reverse(fonts)) {
               if (fontFilterPairx.provider().getGlyph(codePoint) != null) {
                  break;
               }
            }
         }
      });
   }

   private static Set<FontFilterType> getActiveFilters(GameOptions options) {
      Set<FontFilterType> set = EnumSet.noneOf(FontFilterType.class);
      if (options.getForceUnicodeFont().getValue()) {
         set.add(FontFilterType.UNIFORM);
      }

      if (options.getJapaneseGlyphVariants().getValue()) {
         set.add(FontFilterType.JAPANESE_VARIANTS);
      }

      return set;
   }

   private void reload(FontManager.ProviderIndex index, Profiler profiler) {
      profiler.push("closing");
      this.anyFonts.clear();
      this.advanceValidatedFonts.clear();
      this.fontStorages.values().forEach(FontStorage::close);
      this.fontStorages.clear();
      this.fonts.forEach(Font::close);
      this.fonts.clear();
      Set<FontFilterType> set = getActiveFilters(MinecraftClient.getInstance().options);
      profiler.swap("reloading");
      index.fontSets().forEach((id, fonts) -> this.fontStorages.put(id, this.createFontStorage(id, Lists.reverse(fonts), set)));
      this.fonts.addAll(index.allProviders);
      profiler.pop();
      if (!this.fontStorages.containsKey(MinecraftClient.DEFAULT_FONT_ID)) {
         throw new IllegalStateException("Default font failed to load");
      } else {
         this.spriteGlyphs.clear();
         this.atlasManager.acceptAtlasTextures((definitionId, atlasTexture) -> this.spriteGlyphs.put(definitionId, new SpriteAtlasGlyphs(atlasTexture)));
      }
   }

   public void setActiveFilters(GameOptions options) {
      Set<FontFilterType> set = getActiveFilters(options);

      for (FontStorage fontStorage : this.fontStorages.values()) {
         fontStorage.setActiveFilters(set);
      }
   }

   private static List<Pair<FontManager.FontKey, FontLoader.Provider>> loadFontProviders(List<Resource> fontResources, Identifier id) {
      List<Pair<FontManager.FontKey, FontLoader.Provider>> list = new ArrayList<>();

      for (Resource resource : fontResources) {
         try (Reader reader = resource.getReader()) {
            JsonElement jsonElement = (JsonElement)GSON.fromJson(reader, JsonElement.class);
            FontManager.Providers providers = (FontManager.Providers)FontManager.Providers.CODEC
               .parse(JsonOps.INSTANCE, jsonElement)
               .getOrThrow(JsonParseException::new);
            List<FontLoader.Provider> list2 = providers.providers;

            for (int i = list2.size() - 1; i >= 0; i--) {
               FontManager.FontKey fontKey = new FontManager.FontKey(id, resource.getPackId(), i);
               list.add(Pair.of(fontKey, list2.get(i)));
            }
         } catch (Exception var13) {
            LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", new Object[]{id, "fonts.json", resource.getPackId(), var13});
         }
      }

      return list;
   }

   public TextRenderer createTextRenderer() {
      return new TextRenderer(this.anyFonts);
   }

   public TextRenderer createAdvanceValidatingTextRenderer() {
      return new TextRenderer(this.advanceValidatedFonts);
   }

   FontStorage getStorageInternal(Identifier id) {
      return this.fontStorages.getOrDefault(id, this.missingStorage);
   }

   GlyphProvider getSpriteGlyphs(StyleSpriteSource.Sprite description) {
      SpriteAtlasGlyphs spriteAtlasGlyphs = this.spriteGlyphs.get(description.atlasId());
      return spriteAtlasGlyphs == null ? this.missingStorage.getGlyphs(false) : spriteAtlasGlyphs.getGlyphProvider(description.spriteId());
   }

   @Override
   public void close() {
      this.anyFonts.close();
      this.advanceValidatedFonts.close();
      this.fontStorages.values().forEach(FontStorage::close);
      this.fonts.forEach(Font::close);
      this.missingStorage.close();
   }

   @Environment(EnvType.CLIENT)
   record Builder(FontManager.FontKey id, FontFilterType.FilterMap filter, Either<CompletableFuture<Optional<Font>>, Identifier> result) {

      public Optional<List<Font.FontFilterPair>> build(Function<Identifier, @Nullable List<Font.FontFilterPair>> fontRetriever) {
         return (Optional<List<Font.FontFilterPair>>)this.result
            .map(
               future -> ((Optional<Font>)future.join()).map(font -> List.of(new Font.FontFilterPair(font, this.filter))),
               referee -> {
                  List<Font.FontFilterPair> list = fontRetriever.apply(referee);
                  if (list == null) {
                     FontManager.LOGGER
                        .warn(
                           "Can't find font {} referenced by builder {}, either because it's missing, failed to load or is part of loading cycle",
                           referee,
                           this.id
                        );
                     return Optional.empty();
                  } else {
                     return Optional.of(list.stream().map(this::applyFilter).toList());
                  }
               }
            );
      }

      private Font.FontFilterPair applyFilter(Font.FontFilterPair font) {
         return new Font.FontFilterPair(font.provider(), this.filter.apply(font.filter()));
      }
   }

   @Environment(EnvType.CLIENT)
   record FontEntry(Identifier fontId, List<FontManager.Builder> builders, Set<Identifier> dependencies) implements DependencyTracker.Dependencies<Identifier> {

      public FontEntry(Identifier fontId) {
         this(fontId, new ArrayList<>(), new HashSet<>());
      }

      public void addReferenceBuilder(FontManager.FontKey key, FontFilterType.FilterMap filters, FontLoader.Reference reference) {
         this.builders.add(new FontManager.Builder(key, filters, Either.right(reference.id())));
         this.dependencies.add(reference.id());
      }

      public void addBuilder(FontManager.FontKey key, FontFilterType.FilterMap filters, CompletableFuture<Optional<Font>> fontFuture) {
         this.builders.add(new FontManager.Builder(key, filters, Either.left(fontFuture)));
      }

      private Stream<CompletableFuture<Optional<Font>>> getImmediateProviders() {
         return this.builders.stream().flatMap(builder -> builder.result.left().stream());
      }

      public Optional<List<Font.FontFilterPair>> getRequiredFontProviders(Function<Identifier, List<Font.FontFilterPair>> fontRetriever) {
         List<Font.FontFilterPair> list = new ArrayList<>();

         for (FontManager.Builder builder : this.builders) {
            Optional<List<Font.FontFilterPair>> optional = builder.build(fontRetriever);
            if (!optional.isPresent()) {
               return Optional.empty();
            }

            list.addAll(optional.get());
         }

         return Optional.of(list);
      }

      @Override
      public void forDependencies(Consumer<Identifier> callback) {
         this.dependencies.forEach(callback);
      }

      @Override
      public void forOptionalDependencies(Consumer<Identifier> callback) {
      }
   }

   @Environment(EnvType.CLIENT)
   record FontKey(Identifier fontId, String pack, int index) {
      @Override
      public String toString() {
         return "(" + this.fontId + ": builder #" + this.index + " from pack " + this.pack + ")";
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
         this.cached = null;
         this.rectangle = null;
      }

      @Override
      public void close() {
         this.clear();
      }

      private GlyphProvider getGlyphsImpl(StyleSpriteSource source) {
         return switch (source) {
            case StyleSpriteSource.Font font -> FontManager.this.getStorageInternal(font.id()).getGlyphs(this.advanceValidating);
            case StyleSpriteSource.Sprite sprite -> FontManager.this.getSpriteGlyphs(sprite);
            case StyleSpriteSource.Player player -> FontManager.this.playerHeadGlyphs.get(player);
            default -> FontManager.this.missingStorage.getGlyphs(this.advanceValidating);
         };
      }

      @Override
      public GlyphProvider getGlyphs(StyleSpriteSource source) {
         FontManager.Fonts.Cached cached = this.cached;
         if (cached != null && source.equals(cached.source)) {
            return cached.glyphs;
         } else {
            GlyphProvider glyphProvider = this.getGlyphsImpl(source);
            this.cached = new FontManager.Fonts.Cached(source, glyphProvider);
            return glyphProvider;
         }
      }

      @Override
      public EffectGlyph getRectangleGlyph() {
         EffectGlyph effectGlyph = this.rectangle;
         if (effectGlyph == null) {
            effectGlyph = FontManager.this.getStorageInternal(StyleSpriteSource.DEFAULT.id()).getRectangleBakedGlyph();
            this.rectangle = effectGlyph;
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
         instance -> instance.group(FontLoader.Provider.CODEC.listOf().fieldOf("providers").forGetter(FontManager.Providers::providers))
            .apply(instance, FontManager.Providers::new)
      );
   }
}
