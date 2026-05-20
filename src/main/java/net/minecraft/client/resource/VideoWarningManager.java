package net.minecraft.client.resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ScopedProfiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class VideoWarningManager extends SinglePreparationResourceReloader<VideoWarningManager.WarningPatternLoader> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier GPU_WARNLIST_ID = Identifier.ofVanilla("gpu_warnlist.json");
   private ImmutableMap<String, String> warnings = ImmutableMap.of();
   private boolean warningScheduled;
   private boolean warned;

   public boolean hasWarning() {
      return !this.warnings.isEmpty();
   }

   public boolean canWarn() {
      return this.hasWarning() && !this.warned;
   }

   public void scheduleWarning() {
      this.warningScheduled = true;
   }

   public void acceptAfterWarnings() {
      this.warned = true;
   }

   public boolean shouldWarn() {
      return this.warningScheduled && !this.warned;
   }

   public void reset() {
      this.warningScheduled = false;
      this.warned = false;
   }

   public @Nullable String getRendererWarning() {
      return (String)this.warnings.get("renderer");
   }

   public @Nullable String getVersionWarning() {
      return (String)this.warnings.get("version");
   }

   public @Nullable String getVendorWarning() {
      return (String)this.warnings.get("vendor");
   }

   public @Nullable String getWarningsAsString() {
      StringBuilder stringBuilder = new StringBuilder();
      this.warnings.forEach((key, value) -> stringBuilder.append(key).append(": ").append(value));
      return stringBuilder.isEmpty() ? null : stringBuilder.toString();
   }

   protected VideoWarningManager.WarningPatternLoader prepare(ResourceManager resourceManager, Profiler profiler) {
      List<Pattern> list = Lists.newArrayList();
      List<Pattern> list2 = Lists.newArrayList();
      List<Pattern> list3 = Lists.newArrayList();
      JsonObject jsonObject = loadWarnlist(resourceManager, profiler);
      if (jsonObject != null) {
         try (ScopedProfiler scopedProfiler = profiler.scoped("compile_regex")) {
            compilePatterns(jsonObject.getAsJsonArray("renderer"), list);
            compilePatterns(jsonObject.getAsJsonArray("version"), list2);
            compilePatterns(jsonObject.getAsJsonArray("vendor"), list3);
         }
      }

      return new VideoWarningManager.WarningPatternLoader(list, list2, list3);
   }

   protected void apply(VideoWarningManager.WarningPatternLoader warningPatternLoader, ResourceManager resourceManager, Profiler profiler) {
      this.warnings = warningPatternLoader.buildWarnings();
   }

   private static void compilePatterns(JsonArray array, List<Pattern> patterns) {
      array.forEach(json -> patterns.add(Pattern.compile(json.getAsString(), 2)));
   }

   private static @Nullable JsonObject loadWarnlist(ResourceManager resourceManager, Profiler profiler) {
      try {
         JsonObject var4;
         try (
            ScopedProfiler scopedProfiler = profiler.scoped("parse_json");
            Reader reader = resourceManager.openAsReader(GPU_WARNLIST_ID);
         ) {
            var4 = StrictJsonParser.parse(reader).getAsJsonObject();
         }

         return var4;
      } catch (JsonSyntaxException | IOException var10) {
         LOGGER.warn("Failed to load GPU warnlist", var10);
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   protected static final class WarningPatternLoader {
      private final List<Pattern> rendererPatterns;
      private final List<Pattern> versionPatterns;
      private final List<Pattern> vendorPatterns;

      WarningPatternLoader(List<Pattern> rendererPatterns, List<Pattern> versionPatterns, List<Pattern> vendorPatterns) {
         this.rendererPatterns = rendererPatterns;
         this.versionPatterns = versionPatterns;
         this.vendorPatterns = vendorPatterns;
      }

      private static String buildWarning(List<Pattern> warningPattern, String info) {
         List<String> list = Lists.newArrayList();

         for (Pattern pattern : warningPattern) {
            Matcher matcher = pattern.matcher(info);

            while (matcher.find()) {
               list.add(matcher.group());
            }
         }

         return String.join(", ", list);
      }

      ImmutableMap<String, String> buildWarnings() {
         Builder<String, String> builder = new Builder();
         GpuDevice gpuDevice = RenderSystem.getDevice();
         if (gpuDevice.getBackendName().equals("OpenGL")) {
            String string = buildWarning(this.rendererPatterns, gpuDevice.getRenderer());
            if (!string.isEmpty()) {
               builder.put("renderer", string);
            }

            String string2 = buildWarning(this.versionPatterns, gpuDevice.getVersion());
            if (!string2.isEmpty()) {
               builder.put("version", string2);
            }

            String string3 = buildWarning(this.vendorPatterns, gpuDevice.getVendor());
            if (!string3.isEmpty()) {
               builder.put("vendor", string3);
            }
         }

         return builder.build();
      }
   }
}
