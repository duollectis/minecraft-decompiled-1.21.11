package net.minecraft.client.gui.hud.debug.chart;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.ProfilerTiming;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PieChart {
   public static final int field_52773 = 105;
   public static final int field_59836 = 10;
   private static final int field_52774 = 5;
   private final TextRenderer textRenderer;
   private @Nullable ProfileResult profileResult;
   private String currentPath = "root";
   private int bottomMargin = 0;

   public PieChart(TextRenderer textRenderer) {
      this.textRenderer = textRenderer;
   }

   public void setProfileResult(@Nullable ProfileResult profileResult) {
      this.profileResult = profileResult;
   }

   public void setBottomMargin(int bottomMargin) {
      this.bottomMargin = bottomMargin;
   }

   public void render(DrawContext context) {
      if (this.profileResult != null) {
         List<ProfilerTiming> list = this.profileResult.getTimings(this.currentPath);
         ProfilerTiming profilerTiming = list.removeFirst();
         int i = context.getScaledWindowWidth() - 105 - 10;
         int j = i - 105;
         int k = i + 105;
         int l = list.size() * 9;
         int m = context.getScaledWindowHeight() - this.bottomMargin - 5;
         int n = m - l;
         int o = 62;
         int p = n - 62 - 5;
         context.fill(j - 5, p - 62 - 5, k + 5, m + 5, -1873784752);
         context.addProfilerChart(list, j, p - 62 + 10, k, p + 62);
         DecimalFormat decimalFormat = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
         String string = ProfileResult.getHumanReadableName(profilerTiming.name);
         String string2 = "";
         if (!"unspecified".equals(string)) {
            string2 = string2 + "[0] ";
         }

         if (string.isEmpty()) {
            string2 = string2 + "ROOT ";
         } else {
            string2 = string2 + string + " ";
         }

         int q = -1;
         int r = p - 62;
         context.drawTextWithShadow(this.textRenderer, string2, j, r, -1);
         string2 = decimalFormat.format(profilerTiming.totalUsagePercentage) + "%";
         context.drawTextWithShadow(this.textRenderer, string2, k - this.textRenderer.getWidth(string2), r, -1);

         for (int s = 0; s < list.size(); s++) {
            ProfilerTiming profilerTiming2 = list.get(s);
            StringBuilder stringBuilder = new StringBuilder();
            if ("unspecified".equals(profilerTiming2.name)) {
               stringBuilder.append("[?] ");
            } else {
               stringBuilder.append("[").append(s + 1).append("] ");
            }

            String string3 = stringBuilder.append(profilerTiming2.name).toString();
            int t = n + s * 9;
            context.drawTextWithShadow(this.textRenderer, string3, j, t, profilerTiming2.getColor());
            string3 = decimalFormat.format(profilerTiming2.parentSectionUsagePercentage) + "%";
            context.drawTextWithShadow(this.textRenderer, string3, k - 50 - this.textRenderer.getWidth(string3), t, profilerTiming2.getColor());
            string3 = decimalFormat.format(profilerTiming2.totalUsagePercentage) + "%";
            context.drawTextWithShadow(this.textRenderer, string3, k - this.textRenderer.getWidth(string3), t, profilerTiming2.getColor());
         }
      }
   }

   public void select(int index) {
      if (this.profileResult != null) {
         List<ProfilerTiming> list = this.profileResult.getTimings(this.currentPath);
         if (!list.isEmpty()) {
            ProfilerTiming profilerTiming = list.remove(0);
            if (index == 0) {
               if (!profilerTiming.name.isEmpty()) {
                  int i = this.currentPath.lastIndexOf(30);
                  if (i >= 0) {
                     this.currentPath = this.currentPath.substring(0, i);
                  }
               }
            } else {
               index--;
               if (index < list.size() && !"unspecified".equals(list.get(index).name)) {
                  if (!this.currentPath.isEmpty()) {
                     this.currentPath = this.currentPath + "\u001e";
                  }

                  this.currentPath = this.currentPath + list.get(index).name;
               }
            }
         }
      }
   }
}
