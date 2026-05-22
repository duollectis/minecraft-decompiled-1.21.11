package net.minecraft.client.gui.hud;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * HUD-компонент субтитров: отображает названия воспроизводимых звуков со стрелками направления.
 */
@Environment(EnvType.CLIENT)
public class SubtitlesHud implements SoundInstanceListener {

	private static final long REMOVE_DELAY_MS = 3000L;
	private static final float BACKGROUND_OPACITY = 0.8F;
	private static final int SUBTITLE_BOTTOM_OFFSET = 35;
	private static final int LINE_HEIGHT = 9;
	private static final int ARROW_MARGIN = 2;
	private static final float FORWARD_THRESHOLD = 0.5F;
	private static final int ALPHA_MAX = 255;
	private static final int ALPHA_MIN = 75;

	private final MinecraftClient client;
	private final List<SubtitleEntry> entries = Lists.newArrayList();
	private final List<SubtitleEntry> audibleEntries = new ArrayList<>();
	private boolean enabled;

	public SubtitlesHud(MinecraftClient client) {
		this.client = client;
	}

	public void render(DrawContext context) {
		SoundManager soundManager = client.getSoundManager();
		boolean subtitlesEnabled = client.options.getShowSubtitles().getValue();

		if (enabled != subtitlesEnabled) {
			if (subtitlesEnabled) {
				soundManager.registerListener(this);
			} else {
				soundManager.unregisterListener(this);
			}

			enabled = subtitlesEnabled;
		}

		if (!enabled) {
			return;
		}

		SoundListenerTransform listenerTransform = soundManager.getListenerTransform();
		Vec3d listenerPos = listenerTransform.position();
		Vec3d forwardDir = listenerTransform.forward();
		Vec3d rightDir = listenerTransform.right();

		audibleEntries.clear();

		for (SubtitleEntry entry : entries) {
			if (entry.canHearFrom(listenerPos)) {
				audibleEntries.add(entry);
			}
		}

		if (audibleEntries.isEmpty()) {
			return;
		}

		double displayTime = client.options.getNotificationDisplayTime().getValue();
		int maxTextWidth = 0;
		Iterator<SubtitleEntry> iterator = audibleEntries.iterator();

		while (iterator.hasNext()) {
			SubtitleEntry entry = iterator.next();
			entry.removeExpired(REMOVE_DELAY_MS * displayTime);

			if (!entry.hasSounds()) {
				iterator.remove();
			} else {
				maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(entry.getText()));
			}
		}

		maxTextWidth += client.textRenderer.getWidth("<")
			+ client.textRenderer.getWidth(" ")
			+ client.textRenderer.getWidth(">")
			+ client.textRenderer.getWidth(" ");

		if (audibleEntries.isEmpty()) {
			return;
		}

		context.createNewRootLayer();

		int rowIndex = 0;
		int halfWidth = maxTextWidth / 2;
		int halfLine = LINE_HEIGHT / 2;

		for (SubtitleEntry entry : audibleEntries) {
			Text text = entry.getText();
			SoundEntry soundEntry = entry.getNearestSound(listenerPos);

			if (soundEntry == null) {
				continue;
			}

			Vec3d soundDir = soundEntry.location.subtract(listenerPos).normalize();
			double rightDot = rightDir.dotProduct(soundDir);
			double forwardDot = forwardDir.dotProduct(soundDir);
			boolean isInFront = forwardDot > FORWARD_THRESHOLD;
			int textWidth = client.textRenderer.getWidth(text);
			int alpha = MathHelper.floor(MathHelper.clampedLerp(
				(float) (Util.getMeasuringTimeMs() - soundEntry.time) / (float) (REMOVE_DELAY_MS * displayTime),
				ALPHA_MAX,
				ALPHA_MIN
			));

			context.getMatrices().pushMatrix();
			context.getMatrices().translate(
				context.getScaledWindowWidth() - halfWidth * 1.0F - ARROW_MARGIN,
				context.getScaledWindowHeight() - SUBTITLE_BOTTOM_OFFSET - rowIndex * (LINE_HEIGHT + 1) * 1.0F
			);
			context.getMatrices().scale(1.0F, 1.0F);
			context.fill(-halfWidth - 1, -halfLine - 1, halfWidth + 1, halfLine + 1, client.options.getTextBackgroundColor(BACKGROUND_OPACITY));

			int color = ColorHelper.getArgb(ALPHA_MAX, alpha, alpha, alpha);

			if (!isInFront) {
				if (rightDot > 0.0) {
					context.drawTextWithShadow(client.textRenderer, ">", halfWidth - client.textRenderer.getWidth(">"), -halfLine, color);
				} else if (rightDot < 0.0) {
					context.drawTextWithShadow(client.textRenderer, "<", -halfWidth, -halfLine, color);
				}
			}

			context.drawTextWithShadow(client.textRenderer, text, -textWidth / 2, -halfLine, color);
			context.getMatrices().popMatrix();
			rowIndex++;
		}
	}

	@Override
	public void onSoundPlayed(SoundInstance sound, WeightedSoundSet soundSet, float range) {
		if (soundSet.getSubtitle() == null) {
			return;
		}

		Text subtitle = soundSet.getSubtitle();
		Vec3d soundPos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());

		for (SubtitleEntry entry : entries) {
			if (entry.getText().equals(subtitle)) {
				entry.reset(soundPos);
				return;
			}
		}

		entries.add(new SubtitleEntry(subtitle, range, soundPos));
	}

	/**
	 * Запись о конкретном воспроизведении звука: позиция и время.
	 */
	@Environment(EnvType.CLIENT)
	record SoundEntry(Vec3d location, long time) {
	}

	/**
	 * Группа звуков с одним субтитром. Хранит список позиций воспроизведения для определения направления.
	 */
	@Environment(EnvType.CLIENT)
	static class SubtitleEntry {

		private final Text text;
		private final float range;
		private final List<SoundEntry> sounds = new ArrayList<>();

		public SubtitleEntry(Text text, float range, Vec3d pos) {
			this.text = text;
			this.range = range;
			sounds.add(new SoundEntry(pos, Util.getMeasuringTimeMs()));
		}

		public Text getText() {
			return text;
		}

		public @Nullable SoundEntry getNearestSound(Vec3d pos) {
			if (sounds.isEmpty()) {
				return null;
			}

			return sounds.size() == 1
				? sounds.getFirst()
				: sounds.stream()
					.min(Comparator.comparingDouble(s -> s.location().distanceTo(pos)))
					.orElse(null);
		}

		public void reset(Vec3d pos) {
			sounds.removeIf(s -> pos.equals(s.location()));
			sounds.add(new SoundEntry(pos, Util.getMeasuringTimeMs()));
		}

		public boolean canHearFrom(Vec3d pos) {
			if (Float.isInfinite(range)) {
				return true;
			}

			if (sounds.isEmpty()) {
				return false;
			}

			SoundEntry nearest = getNearestSound(pos);
			return nearest != null && pos.isInRange(nearest.location, range);
		}

		public void removeExpired(double expiry) {
			long now = Util.getMeasuringTimeMs();
			sounds.removeIf(s -> now - s.time() > expiry);
		}

		public boolean hasSounds() {
			return !sounds.isEmpty();
		}
	}
}
