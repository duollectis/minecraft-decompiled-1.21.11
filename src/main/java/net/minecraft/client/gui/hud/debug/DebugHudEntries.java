package net.minecraft.client.gui.hud.debug;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugHudEntries {
   private static final Map<Identifier, DebugHudEntry> ENTRIES = new HashMap<>();
   public static final Identifier GAME_VERSION = registerVanilla("game_version", new GameVersionDebugHudEntry());
   public static final Identifier FPS = registerVanilla("fps", new FpsDebugHudEntry());
   public static final Identifier TPS = registerVanilla("tps", new TpsDebugHudEntry());
   public static final Identifier MEMORY = registerVanilla("memory", new MemoryDebugHudEntry());
   public static final Identifier SYSTEM_SPECS = registerVanilla("system_specs", new SystemSpecsDebugHudEntry());
   public static final Identifier LOOKING_AT_BLOCK = registerVanilla("looking_at_block", new LookingAtBlockDebugHudEntry());
   public static final Identifier LOOKING_AT_FLUID = registerVanilla("looking_at_fluid", new LookingAtFluidDebugHudEntry());
   public static final Identifier LOOKING_AT_ENTITY = registerVanilla("looking_at_entity", new LookingAtEntityDebugHudEntry());
   public static final Identifier CHUNK_RENDER_STATS = registerVanilla("chunk_render_stats", new ChunkRenderStatsDebugHudEntry());
   public static final Identifier CHUNK_GENERATION_STATS = registerVanilla("chunk_generation_stats", new ChunkGenerationStatsDebugHudEntry());
   public static final Identifier ENTITY_RENDER_STATS = registerVanilla("entity_render_stats", new EntityRenderStatsDebugHudEntry());
   public static final Identifier PARTICLE_RENDER_STATS = registerVanilla("particle_render_stats", new ParticleRenderStatsDebugHudEntry());
   public static final Identifier CHUNK_SOURCE_STATS = registerVanilla("chunk_source_stats", new ChunkSourceStatsDebugHudEntry());
   public static final Identifier PLAYER_POSITION = registerVanilla("player_position", new PlayerPositionDebugHudEntry());
   public static final Identifier PLAYER_SECTION_POSITION = registerVanilla("player_section_position", new PlayerSectionPositionDebugHudEntry());
   public static final Identifier LIGHT_LEVELS = registerVanilla("light_levels", new LightLevelsDebugHudEntry());
   public static final Identifier HEIGHTMAP = registerVanilla("heightmap", new HeightmapDebugHudEntry());
   public static final Identifier BIOME = registerVanilla("biome", new BiomeDebugHudEntry());
   public static final Identifier LOCAL_DIFFICULTY = registerVanilla("local_difficulty", new LocalDifficultyDebugHudEntry());
   public static final Identifier ENTITY_SPAWN_COUNTS = registerVanilla("entity_spawn_counts", new EntitySpawnCountsDebugHudEntry());
   public static final Identifier SOUND_MOOD = registerVanilla("sound_mood", new SoundMoodDebugHudEntry());
   public static final Identifier POST_EFFECT = registerVanilla("post_effect", new PostEffectDebugHudEntry());
   public static final Identifier ENTITY_HITBOXES = registerVanilla("entity_hitboxes", new RendererDebugHudEntry());
   public static final Identifier CHUNK_BORDERS = registerVanilla("chunk_borders", new RendererDebugHudEntry());
   public static final Identifier THREE_DIMENSIONAL_CROSSHAIR = registerVanilla("3d_crosshair", new RendererDebugHudEntry());
   public static final Identifier CHUNK_SECTION_PATHS = registerVanilla("chunk_section_paths", new RendererDebugHudEntry());
   public static final Identifier GPU_UTILIZATION = registerVanilla("gpu_utilization", new GpuUtilizationDebugHudEntry());
   public static final Identifier SIMPLE_PERFORMANCE_IMPACTORS = registerVanilla("simple_performance_impactors", new SimplePerformanceImpactorsDebugHudEntry());
   public static final Identifier CHUNK_SECTION_OCTREE = registerVanilla("chunk_section_octree", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_WATER_LEVELS = registerVanilla("visualize_water_levels", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_HEIGHTMAP = registerVanilla("visualize_heightmap", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_COLLISION_BOXES = registerVanilla("visualize_collision_boxes", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_ENTITY_SUPPORTING_BLOCKS = registerVanilla("visualize_entity_supporting_blocks", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_BLOCK_LIGHT_LEVELS = registerVanilla("visualize_block_light_levels", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_SKY_LIGHT_LEVELS = registerVanilla("visualize_sky_light_levels", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_SOLID_FACES = registerVanilla("visualize_solid_faces", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_CHUNKS_ON_SERVER = registerVanilla("visualize_chunks_on_server", new RendererDebugHudEntry());
   public static final Identifier VISUALIZE_SKY_LIGHT_SECTIONS = registerVanilla("visualize_sky_light_sections", new RendererDebugHudEntry());
   public static final Identifier CHUNK_SECTION_VISIBILITY = registerVanilla("chunk_section_visibility", new RendererDebugHudEntry());
   public static final Map<DebugProfileType, Map<Identifier, DebugHudEntryVisibility>> PROFILES;

   private static Identifier registerVanilla(String id, DebugHudEntry entry) {
      return register(Identifier.ofVanilla(id), entry);
   }

   public static Identifier register(Identifier id, DebugHudEntry entry) {
      ENTRIES.put(id, entry);
      return id;
   }

   public static Map<Identifier, DebugHudEntry> getEntries() {
      return Map.copyOf(ENTRIES);
   }

   public static @Nullable DebugHudEntry get(Identifier id) {
      return ENTRIES.get(id);
   }

   static {
      Map<Identifier, DebugHudEntryVisibility> map = Map.of(
         THREE_DIMENSIONAL_CROSSHAIR,
         DebugHudEntryVisibility.IN_OVERLAY,
         GAME_VERSION,
         DebugHudEntryVisibility.IN_OVERLAY,
         TPS,
         DebugHudEntryVisibility.IN_OVERLAY,
         FPS,
         DebugHudEntryVisibility.IN_OVERLAY,
         MEMORY,
         DebugHudEntryVisibility.IN_OVERLAY,
         SYSTEM_SPECS,
         DebugHudEntryVisibility.IN_OVERLAY,
         PLAYER_POSITION,
         DebugHudEntryVisibility.IN_OVERLAY,
         PLAYER_SECTION_POSITION,
         DebugHudEntryVisibility.IN_OVERLAY,
         SIMPLE_PERFORMANCE_IMPACTORS,
         DebugHudEntryVisibility.IN_OVERLAY
      );
      Map<Identifier, DebugHudEntryVisibility> map2 = Map.of(
         TPS,
         DebugHudEntryVisibility.IN_OVERLAY,
         FPS,
         DebugHudEntryVisibility.ALWAYS_ON,
         GPU_UTILIZATION,
         DebugHudEntryVisibility.IN_OVERLAY,
         MEMORY,
         DebugHudEntryVisibility.IN_OVERLAY,
         SIMPLE_PERFORMANCE_IMPACTORS,
         DebugHudEntryVisibility.IN_OVERLAY
      );
      PROFILES = Map.of(DebugProfileType.DEFAULT, map, DebugProfileType.PERFORMANCE, map2);
   }
}
