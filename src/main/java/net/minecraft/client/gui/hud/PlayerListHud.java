package net.minecraft.client.gui.hud;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerListHud}.
 */
public class PlayerListHud {

	private static final Identifier PING_UNKNOWN_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_unknown");
	private static final Identifier PING_1_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_1");
	private static final Identifier PING_2_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_2");
	private static final Identifier PING_3_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_3");
	private static final Identifier PING_4_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_4");
	private static final Identifier PING_5_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_5");
	private static final Identifier
			CONTAINER_HEART_BLINKING_TEXTURE =
			Identifier.ofVanilla("hud/heart/container_blinking");
	private static final Identifier CONTAINER_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/container");
	private static final Identifier FULL_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/full_blinking");
	private static final Identifier HALF_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/half_blinking");
	private static final Identifier
			ABSORBING_FULL_HEART_BLINKING_TEXTURE =
			Identifier.ofVanilla("hud/heart/absorbing_full_blinking");
	private static final Identifier FULL_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/full");
	private static final Identifier
			ABSORBING_HALF_HEART_BLINKING_TEXTURE =
			Identifier.ofVanilla("hud/heart/absorbing_half_blinking");
	private static final Identifier HALF_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/half");
	private static final Comparator<PlayerListEntry>
			ENTRY_ORDERING =
			Comparator.<PlayerListEntry>comparingInt(entry -> -entry.getListOrder())
			          .thenComparingInt(entry -> entry.getGameMode() == GameMode.SPECTATOR ? 1 : 0)
			          .thenComparing(entry -> Nullables.mapOrElse(entry.getScoreboardTeam(), Team::getName, ""))
			          .thenComparing(entry -> entry.getProfile().name(), String::compareToIgnoreCase);
	public static final int MAX_ROWS = 20;
	private final MinecraftClient client;
	private final InGameHud inGameHud;
	private @Nullable Text footer;
	private @Nullable Text header;
	private boolean visible;
	private final Map<UUID, PlayerListHud.Heart> hearts = new Object2ObjectOpenHashMap();

	public PlayerListHud(MinecraftClient client, InGameHud inGameHud) {
		this.client = client;
		this.inGameHud = inGameHud;
	}

	public Text getPlayerName(PlayerListEntry entry) {
		return entry.getDisplayName() != null
		       ? this.applyGameModeFormatting(entry, entry.getDisplayName().copy())
		       : this.applyGameModeFormatting(
				       entry,
				       Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().name()))
		       );
	}

	private Text applyGameModeFormatting(PlayerListEntry entry, MutableText name) {
		return entry.getGameMode() == GameMode.SPECTATOR ? name.formatted(Formatting.ITALIC) : name;
	}

	public void setVisible(boolean visible) {
		if (this.visible != visible) {
			this.hearts.clear();
			this.visible = visible;
			if (visible) {
				Text text = Texts.join(this.collectPlayerEntries(), Text.literal(", "), this::getPlayerName);
				this.client
						.getNarratorManager()
						.narrateSystemImmediately(Text.translatable("multiplayer.player.list.narration", text));
			}
		}
	}

	private List<PlayerListEntry> collectPlayerEntries() {
		return this.client.player.networkHandler
				.getListedPlayerListEntries()
				.stream()
				.sorted(ENTRY_ORDERING)
				.limit(80L)
				.toList();
	}

	public void render(
			DrawContext context,
			int scaledWindowWidth,
			Scoreboard scoreboard,
			@Nullable ScoreboardObjective objective
	) {
		List<PlayerListEntry> list = this.collectPlayerEntries();
		List<PlayerListHud.ScoreDisplayEntry> list2 = new ArrayList<>(list.size());
		int i = this.client.textRenderer.getWidth(" ");
		int j = 0;
		int k = 0;

		for (PlayerListEntry playerListEntry : list) {
			Text text = this.getPlayerName(playerListEntry);
			j = Math.max(j, this.client.textRenderer.getWidth(text));
			int l = 0;
			Text text2 = null;
			int m = 0;
			if (objective != null) {
				ScoreHolder scoreHolder = ScoreHolder.fromProfile(playerListEntry.getProfile());
				ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, objective);
				if (readableScoreboardScore != null) {
					l = readableScoreboardScore.getScore();
				}

				if (objective.getRenderType() != ScoreboardCriterion.RenderType.HEARTS) {
					NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.YELLOW);
					text2 = ReadableScoreboardScore.getFormattedScore(readableScoreboardScore, numberFormat);
					m = this.client.textRenderer.getWidth(text2);
					k = Math.max(k, m > 0 ? i + m : 0);
				}
			}

			list2.add(new PlayerListHud.ScoreDisplayEntry(text, l, text2, m));
		}

		if (!this.hearts.isEmpty()) {
			Set<UUID> set = list.stream().map(playerEntry -> playerEntry.getProfile().id()).collect(Collectors.toSet());
			this.hearts.keySet().removeIf(uuid -> !set.contains(uuid));
		}

		int n = list.size();
		int o = n;

		int p;
		for (p = 1; o > 20; o = (n + p - 1) / p) {
			p++;
		}

		boolean bl = this.client.isInSingleplayer() || this.client.getNetworkHandler().getConnection().isEncrypted();
		int q;
		if (objective != null) {
			if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
				q = 90;
			}
			else {
				q = k;
			}
		}
		else {
			q = 0;
		}

		int m = Math.min(p * ((bl ? 9 : 0) + j + q + 13), scaledWindowWidth - 50) / p;
		int r = scaledWindowWidth / 2 - (m * p + (p - 1) * 5) / 2;
		int s = 10;
		int t = m * p + (p - 1) * 5;
		List<OrderedText> list3 = null;
		if (this.header != null) {
			list3 = this.client.textRenderer.wrapLines(this.header, scaledWindowWidth - 50);

			for (OrderedText orderedText : list3) {
				t = Math.max(t, this.client.textRenderer.getWidth(orderedText));
			}
		}

		List<OrderedText> list4 = null;
		if (this.footer != null) {
			list4 = this.client.textRenderer.wrapLines(this.footer, scaledWindowWidth - 50);

			for (OrderedText orderedText2 : list4) {
				t = Math.max(t, this.client.textRenderer.getWidth(orderedText2));
			}
		}

		if (list3 != null) {
			context.fill(
					scaledWindowWidth / 2 - t / 2 - 1,
					s - 1,
					scaledWindowWidth / 2 + t / 2 + 1,
					s + list3.size() * 9,
					Integer.MIN_VALUE
			);

			for (OrderedText orderedText2 : list3) {
				int u = this.client.textRenderer.getWidth(orderedText2);
				context.drawTextWithShadow(
						this.client.textRenderer,
						orderedText2,
						scaledWindowWidth / 2 - u / 2,
						s,
						-1
				);
				s += 9;
			}

			s++;
		}

		context.fill(
				scaledWindowWidth / 2 - t / 2 - 1,
				s - 1,
				scaledWindowWidth / 2 + t / 2 + 1,
				s + o * 9,
				Integer.MIN_VALUE
		);
		int v = this.client.options.getTextBackgroundColor(553648127);

		for (int w = 0; w < n; w++) {
			int u = w / o;
			int x = w % o;
			int y = r + u * m + u * 5;
			int z = s + x * 9;
			context.fill(y, z, y + m, z + 8, v);
			if (w < list.size()) {
				PlayerListEntry playerListEntry2 = list.get(w);
				PlayerListHud.ScoreDisplayEntry scoreDisplayEntry = list2.get(w);
				GameProfile gameProfile = playerListEntry2.getProfile();
				if (bl) {
					PlayerEntity playerEntity = this.client.world.getPlayerByUuid(gameProfile.id());
					boolean bl2 = playerEntity != null && PlayerEntityRenderer.shouldFlipUpsideDown(playerEntity);
					PlayerSkinDrawer.draw(
							context,
							playerListEntry2.getSkinTextures().body().texturePath(),
							y,
							z,
							8,
							playerListEntry2.shouldShowHat(),
							bl2,
							-1
					);
					y += 9;
				}

				context.drawTextWithShadow(
						this.client.textRenderer,
						scoreDisplayEntry.name,
						y,
						z,
						playerListEntry2.getGameMode() == GameMode.SPECTATOR ? -1862270977 : -1
				);
				if (objective != null && playerListEntry2.getGameMode() != GameMode.SPECTATOR) {
					int aa = y + j + 1;
					int ab = aa + q;
					if (ab - aa > 5) {
						this.renderScoreboardObjective(
								objective,
								z,
								scoreDisplayEntry,
								aa,
								ab,
								gameProfile.id(),
								context
						);
					}
				}

				this.renderLatencyIcon(context, m, y - (bl ? 9 : 0), z, playerListEntry2);
			}
		}

		if (list4 != null) {
			s += o * 9 + 1;
			context.fill(
					scaledWindowWidth / 2 - t / 2 - 1,
					s - 1,
					scaledWindowWidth / 2 + t / 2 + 1,
					s + list4.size() * 9,
					Integer.MIN_VALUE
			);

			for (OrderedText orderedText3 : list4) {
				int x = this.client.textRenderer.getWidth(orderedText3);
				context.drawTextWithShadow(
						this.client.textRenderer,
						orderedText3,
						scaledWindowWidth / 2 - x / 2,
						s,
						-1
				);
				s += 9;
			}
		}
	}

	protected void renderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry) {
		Identifier identifier;
		if (entry.getLatency() < 0) {
			identifier = PING_UNKNOWN_ICON_TEXTURE;
		}
		else if (entry.getLatency() < 150) {
			identifier = PING_5_ICON_TEXTURE;
		}
		else if (entry.getLatency() < 300) {
			identifier = PING_4_ICON_TEXTURE;
		}
		else if (entry.getLatency() < 600) {
			identifier = PING_3_ICON_TEXTURE;
		}
		else if (entry.getLatency() < 1000) {
			identifier = PING_2_ICON_TEXTURE;
		}
		else {
			identifier = PING_1_ICON_TEXTURE;
		}

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, x + width - 11, y, 10, 8);
	}

	private void renderScoreboardObjective(
			ScoreboardObjective objective,
			int y,
			PlayerListHud.ScoreDisplayEntry scoreDisplayEntry,
			int left,
			int right,
			UUID uuid,
			DrawContext context
	) {
		if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
			this.renderHearts(y, left, right, uuid, context, scoreDisplayEntry.score);
		}
		else if (scoreDisplayEntry.formattedScore != null) {
			context.drawTextWithShadow(
					this.client.textRenderer,
					scoreDisplayEntry.formattedScore,
					right - scoreDisplayEntry.scoreWidth,
					y,
					-1
			);
		}
	}

	private void renderHearts(int y, int left, int right, UUID uuid, DrawContext context, int score) {
		PlayerListHud.Heart heart = this.hearts.computeIfAbsent(uuid, uuid2 -> new PlayerListHud.Heart(score));
		heart.tick(score, this.inGameHud.getTicks());
		int i = MathHelper.ceilDiv(Math.max(score, heart.getLastScore()), 2);
		int j = Math.max(score, Math.max(heart.getLastScore(), 20)) / 2;
		boolean bl = heart.useHighlighted(this.inGameHud.getTicks());
		if (i > 0) {
			int k = MathHelper.floor(Math.min((float) (right - left - 4) / j, 9.0F));
			if (k <= 3) {
				float f = MathHelper.clamp(score / 20.0F, 0.0F, 1.0F);
				int l = (int) ((1.0F - f) * 255.0F) << 16 | (int) (f * 255.0F) << 8;
				float g = score / 2.0F;
				Text text = Text.translatable("multiplayer.player.list.hp", g);
				Text text2;
				if (right - this.client.textRenderer.getWidth(text) >= left) {
					text2 = text;
				}
				else {
					text2 = Text.literal(Float.toString(g));
				}

				context.drawTextWithShadow(
						this.client.textRenderer,
						text2,
						(right + left - this.client.textRenderer.getWidth(text2)) / 2,
						y,
						ColorHelper.fullAlpha(l)
				);
			}
			else {
				Identifier identifier = bl ? CONTAINER_HEART_BLINKING_TEXTURE : CONTAINER_HEART_TEXTURE;

				for (int l = i; l < j; l++) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, left + l * k, y, 9, 9);
				}

				for (int l = 0; l < i; l++) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, left + l * k, y, 9, 9);
					if (bl) {
						if (l * 2 + 1 < heart.getLastScore()) {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									FULL_HEART_BLINKING_TEXTURE,
									left + l * k,
									y,
									9,
									9
							);
						}

						if (l * 2 + 1 == heart.getLastScore()) {
							context.drawGuiTexture(
									RenderPipelines.GUI_TEXTURED,
									HALF_HEART_BLINKING_TEXTURE,
									left + l * k,
									y,
									9,
									9
							);
						}
					}

					if (l * 2 + 1 < score) {
						context.drawGuiTexture(
								RenderPipelines.GUI_TEXTURED,
								l >= 10 ? ABSORBING_FULL_HEART_BLINKING_TEXTURE : FULL_HEART_TEXTURE,
								left + l * k,
								y,
								9,
								9
						);
					}

					if (l * 2 + 1 == score) {
						context.drawGuiTexture(
								RenderPipelines.GUI_TEXTURED,
								l >= 10 ? ABSORBING_HALF_HEART_BLINKING_TEXTURE : HALF_HEART_TEXTURE,
								left + l * k,
								y,
								9,
								9
						);
					}
				}
			}
		}
	}

	public void setFooter(@Nullable Text footer) {
		this.footer = footer;
	}

	public void setHeader(@Nullable Text header) {
		this.header = header;
	}

	public void clear() {
		this.header = null;
		this.footer = null;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Heart}.
	 */
	static class Heart {

		private static final long COOLDOWN_TICKS = 20L;
		private static final long SCORE_DECREASE_HIGHLIGHT_TICKS = 20L;
		private static final long SCORE_INCREASE_HIGHLIGHT_TICKS = 10L;
		private int score;
		private int lastScore;
		private long lastScoreChangeTick;
		private long highlightEndTick;

		public Heart(int score) {
			this.lastScore = score;
			this.score = score;
		}

		public void tick(int score, long currentTick) {
			if (score != this.score) {
				long l = score < this.score ? 20L : 10L;
				this.highlightEndTick = currentTick + l;
				this.score = score;
				this.lastScoreChangeTick = currentTick;
			}

			if (currentTick - this.lastScoreChangeTick > 20L) {
				this.lastScore = score;
			}
		}

		public int getLastScore() {
			return this.lastScore;
		}

		public boolean useHighlighted(long currentTick) {
			return this.highlightEndTick > currentTick && (this.highlightEndTick - currentTick) % 6L >= 3L;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ScoreDisplayEntry}.
	 */
	record ScoreDisplayEntry(Text name, int score, @Nullable Text formattedScore, int scoreWidth) {
	}
}
