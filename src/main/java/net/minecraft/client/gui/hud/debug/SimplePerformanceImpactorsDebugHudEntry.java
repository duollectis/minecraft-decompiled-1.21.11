package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.TextureFilteringMode;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Запись отладочного HUD: простые настройки, влияющие на производительность
 * (прозрачность, облака, смешивание биомов, фильтрация текстур).
 */
@Environment(EnvType.CLIENT)
public class SimplePerformanceImpactorsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		GameOptions options = MinecraftClient.getInstance().options;
		CloudRenderMode cloudMode = options.getCloudRenderMode().getValue();
		String cloudLabel = cloudMode == CloudRenderMode.OFF ? "" : (cloudMode == CloudRenderMode.FAST ? " fast-clouds" : " fancy-clouds");
		String transparencyLabel = options.getImprovedTransparency().getValue() ? "improved-transparency" : "";

		lines.addLine(
			String.format(Locale.ROOT, "%s%s B: %d", transparencyLabel, cloudLabel, options.getBiomeBlendRadius().getValue())
		);

		TextureFilteringMode filteringMode = options.getTextureFiltering().getValue();

		if (filteringMode == TextureFilteringMode.ANISOTROPIC) {
			lines.addLine(
				String.format(Locale.ROOT, "Filtering: %s %dx", filteringMode.getText().getString(), options.getEffectiveAnisotropy())
			);
		} else {
			lines.addLine(String.format(Locale.ROOT, "Filtering: %s", filteringMode.getText().getString()));
		}
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
