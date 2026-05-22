package net.minecraft.client.gui.hud;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.Map;
import java.util.UUID;

/**
 * HUD-компонент для отображения полосок здоровья боссов в верхней части экрана.
 */
@Environment(EnvType.CLIENT)
public class BossBarHud {

	private static final int WIDTH = 182;
	private static final int HEIGHT = 5;
	private static final Identifier[] BACKGROUND_TEXTURES = new Identifier[]{
			Identifier.ofVanilla("boss_bar/pink_background"),
			Identifier.ofVanilla("boss_bar/blue_background"),
			Identifier.ofVanilla("boss_bar/red_background"),
			Identifier.ofVanilla("boss_bar/green_background"),
			Identifier.ofVanilla("boss_bar/yellow_background"),
			Identifier.ofVanilla("boss_bar/purple_background"),
			Identifier.ofVanilla("boss_bar/white_background")
	};
	private static final Identifier[] PROGRESS_TEXTURES = new Identifier[]{
			Identifier.ofVanilla("boss_bar/pink_progress"),
			Identifier.ofVanilla("boss_bar/blue_progress"),
			Identifier.ofVanilla("boss_bar/red_progress"),
			Identifier.ofVanilla("boss_bar/green_progress"),
			Identifier.ofVanilla("boss_bar/yellow_progress"),
			Identifier.ofVanilla("boss_bar/purple_progress"),
			Identifier.ofVanilla("boss_bar/white_progress")
	};
	private static final Identifier[] NOTCHED_BACKGROUND_TEXTURES = new Identifier[]{
			Identifier.ofVanilla("boss_bar/notched_6_background"),
			Identifier.ofVanilla("boss_bar/notched_10_background"),
			Identifier.ofVanilla("boss_bar/notched_12_background"),
			Identifier.ofVanilla("boss_bar/notched_20_background")
	};
	private static final Identifier[] NOTCHED_PROGRESS_TEXTURES = new Identifier[]{
			Identifier.ofVanilla("boss_bar/notched_6_progress"),
			Identifier.ofVanilla("boss_bar/notched_10_progress"),
			Identifier.ofVanilla("boss_bar/notched_12_progress"),
			Identifier.ofVanilla("boss_bar/notched_20_progress")
	};
	private final MinecraftClient client;
	final Map<UUID, ClientBossBar> bossBars = Maps.newLinkedHashMap();

	private static final int BAR_X_OFFSET = 91;
	private static final int BAR_Y_START = 12;
	private static final int BAR_Y_STEP = 19;
	private static final int TEXT_HEIGHT = 9;
	private static final int MAX_HEIGHT_DIVISOR = 3;
	private static final int COLOR_WHITE = -1;

	public BossBarHud(MinecraftClient client) {
		this.client = client;
	}

	public void render(DrawContext context) {
		if (bossBars.isEmpty()) {
			return;
		}

		context.createNewRootLayer();
		Profiler profiler = Profilers.get();
		profiler.push("bossHealth");

		int screenWidth = context.getScaledWindowWidth();
		int barY = BAR_Y_START;

		for (ClientBossBar bossBar : bossBars.values()) {
			int barX = screenWidth / 2 - BAR_X_OFFSET;
			renderBossBar(context, barX, barY, bossBar);

			Text name = bossBar.getName();
			int nameWidth = client.textRenderer.getWidth(name);
			int nameX = screenWidth / 2 - nameWidth / 2;
			int nameY = barY - TEXT_HEIGHT;

			context.drawTextWithShadow(client.textRenderer, name, nameX, nameY, COLOR_WHITE);

			barY += BAR_Y_STEP;

			if (barY >= context.getScaledWindowHeight() / MAX_HEIGHT_DIVISOR) {
				break;
			}
		}

		profiler.pop();
	}

	private void renderBossBar(DrawContext context, int x, int y, BossBar bossBar) {
		renderBossBar(context, x, y, bossBar, WIDTH, BACKGROUND_TEXTURES, NOTCHED_BACKGROUND_TEXTURES);

		int progressWidth = MathHelper.lerpPositive(bossBar.getPercent(), 0, WIDTH);

		if (progressWidth > 0) {
			renderBossBar(context, x, y, bossBar, progressWidth, PROGRESS_TEXTURES, NOTCHED_PROGRESS_TEXTURES);
		}
	}

	private void renderBossBar(
		DrawContext context,
		int x,
		int y,
		BossBar bossBar,
		int width,
		Identifier[] textures,
		Identifier[] notchedTextures
	) {
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			textures[bossBar.getColor().ordinal()],
			WIDTH,
			HEIGHT,
			0,
			0,
			x,
			y,
			width,
			HEIGHT
		);

		if (bossBar.getStyle() != BossBar.Style.PROGRESS) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				notchedTextures[bossBar.getStyle().ordinal() - 1],
				WIDTH,
				HEIGHT,
				0,
				0,
				x,
				y,
				width,
				HEIGHT
			);
		}
	}

	public void handlePacket(BossBarS2CPacket packet) {
		packet.accept(
				new BossBarS2CPacket.Consumer() {
					@Override
					public void add(
							UUID uuid,
							Text name,
							float percent,
							BossBar.Color color,
							BossBar.Style style,
							boolean darkenSky,
							boolean dragonMusic,
							boolean thickenFog
					) {
						BossBarHud.this.bossBars.put(
								uuid,
								new ClientBossBar(uuid, name, percent, color, style, darkenSky, dragonMusic, thickenFog)
						);
					}

					@Override
					public void remove(UUID uuid) {
						BossBarHud.this.bossBars.remove(uuid);
					}

					@Override
					public void updateProgress(UUID uuid, float percent) {
						BossBarHud.this.bossBars.get(uuid).setPercent(percent);
					}

					@Override
					public void updateName(UUID uuid, Text name) {
						BossBarHud.this.bossBars.get(uuid).setName(name);
					}

					@Override
					public void updateStyle(UUID id, BossBar.Color color, BossBar.Style style) {
						ClientBossBar clientBossBar = BossBarHud.this.bossBars.get(id);
						clientBossBar.setColor(color);
						clientBossBar.setStyle(style);
					}

					@Override
					public void updateProperties(
							UUID uuid,
							boolean darkenSky,
							boolean dragonMusic,
							boolean thickenFog
					) {
						ClientBossBar clientBossBar = BossBarHud.this.bossBars.get(uuid);
						clientBossBar.setDarkenSky(darkenSky);
						clientBossBar.setDragonMusic(dragonMusic);
						clientBossBar.setThickenFog(thickenFog);
					}
				}
		);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.bossBars.clear();
	}

	/**
	 * Определяет, следует ли play dragon music.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldPlayDragonMusic() {
		if (!this.bossBars.isEmpty()) {
			for (BossBar bossBar : this.bossBars.values()) {
				if (bossBar.hasDragonMusic()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Определяет, следует ли darken sky.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDarkenSky() {
		if (!this.bossBars.isEmpty()) {
			for (BossBar bossBar : this.bossBars.values()) {
				if (bossBar.shouldDarkenSky()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Определяет, следует ли thicken fog.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldThickenFog() {
		if (!this.bossBars.isEmpty()) {
			for (BossBar bossBar : this.bossBars.values()) {
				if (bossBar.shouldThickenFog()) {
					return true;
				}
			}
		}

		return false;
	}
}
