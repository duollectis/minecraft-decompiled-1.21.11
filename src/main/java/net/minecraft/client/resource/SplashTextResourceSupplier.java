package net.minecraft.client.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.MonthDay;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.session.Session;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Holidays;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SplashTextResourceSupplier extends SinglePreparationResourceReloader<List<Text>> {
   private static final Style SPLASH_TEXT_STYLE = Style.EMPTY.withColor(-256);
   public static final Text MERRY_X_MAS_ = create("Merry X-mas!");
   public static final Text HAPPY_NEW_YEAR_ = create("Happy new year!");
   public static final Text OOOOO_O_O_OOOOO__SPOOKY_ = create("OOoooOOOoooo! Spooky!");
   private static final Identifier RESOURCE_ID = Identifier.ofVanilla("texts/splashes.txt");
   private static final Random RANDOM = Random.create();
   private List<Text> splashTexts = List.of();
   private final Session session;

   public SplashTextResourceSupplier(Session session) {
      this.session = session;
   }

   private static Text create(String text) {
      return Text.literal(text).setStyle(SPLASH_TEXT_STYLE);
   }

   protected List<Text> prepare(ResourceManager resourceManager, Profiler profiler) {
      try {
         List var4;
         try (BufferedReader bufferedReader = MinecraftClient.getInstance().getResourceManager().openAsReader(RESOURCE_ID)) {
            var4 = bufferedReader.lines()
               .map(String::trim)
               .filter(splashText -> splashText.hashCode() != 125780783)
               .map(SplashTextResourceSupplier::create)
               .toList();
         }

         return var4;
      } catch (IOException var8) {
         return List.of();
      }
   }

   protected void apply(List<Text> list, ResourceManager resourceManager, Profiler profiler) {
      this.splashTexts = List.copyOf(list);
   }

   public @Nullable SplashTextRenderer get() {
      MonthDay monthDay = Holidays.now();
      if (monthDay.equals(Holidays.CHRISTMAS_EVE)) {
         return SplashTextRenderer.MERRY_X_MAS;
      } else if (monthDay.equals(Holidays.NEW_YEARS_DAY)) {
         return SplashTextRenderer.HAPPY_NEW_YEAR;
      } else if (monthDay.equals(Holidays.HALLOWEEN)) {
         return SplashTextRenderer.OOOOO_O_O_OOOOO__SPOOKY;
      } else if (this.splashTexts.isEmpty()) {
         return null;
      } else {
         return this.session != null && RANDOM.nextInt(this.splashTexts.size()) == 42
            ? new SplashTextRenderer(create(this.session.getUsername().toUpperCase(Locale.ROOT) + " IS YOU"))
            : new SplashTextRenderer(this.splashTexts.get(RANDOM.nextInt(this.splashTexts.size())));
      }
   }
}
