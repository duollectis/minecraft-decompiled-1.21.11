package net.minecraft.client.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

/**
 * Клиентская реализация полоски здоровья босса с плавной анимацией изменения здоровья.
 */
@Environment(EnvType.CLIENT)
public class ClientBossBar extends BossBar {

	private static final long HEALTH_CHANGE_ANIMATION_MS = 100L;

	protected float healthLatest;
	protected long timeHealthSet;

	public ClientBossBar(
		UUID uuid,
		Text name,
		float percent,
		BossBar.Color color,
		BossBar.Style style,
		boolean darkenSky,
		boolean dragonMusic,
		boolean thickenFog
	) {
		super(uuid, name, color, style);
		healthLatest = percent;
		this.percent = percent;
		timeHealthSet = Util.getMeasuringTimeMs();
		setDarkenSky(darkenSky);
		setDragonMusic(dragonMusic);
		setThickenFog(thickenFog);
	}

	@Override
	public void setPercent(float percent) {
		this.percent = getPercent();
		healthLatest = percent;
		timeHealthSet = Util.getMeasuringTimeMs();
	}

	/**
	 * Возвращает интерполированное значение здоровья для плавной анимации.
	 * Анимация длится {@value HEALTH_CHANGE_ANIMATION_MS} мс после изменения значения.
	 */
	@Override
	public float getPercent() {
		long elapsed = Util.getMeasuringTimeMs() - timeHealthSet;
		float progress = MathHelper.clamp((float) elapsed / HEALTH_CHANGE_ANIMATION_MS, 0.0F, 1.0F);
		return MathHelper.lerp(progress, this.percent, healthLatest);
	}
}
