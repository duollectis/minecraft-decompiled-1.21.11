package net.minecraft.server.world;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * {@code SleepManager}.
 */
public class SleepManager {

	private int total;
	private int sleeping;

	/**
	 * Проверяет возможность skip night.
	 *
	 * @param percentage percentage
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canSkipNight(int percentage) {
		return this.sleeping >= this.getNightSkippingRequirement(percentage);
	}

	/**
	 * Проверяет возможность reset time.
	 *
	 * @param percentage percentage
	 * @param players players
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canResetTime(int percentage, List<ServerPlayerEntity> players) {
		int i = (int) players.stream().filter(PlayerEntity::canResetTimeBySleeping).count();
		return i >= this.getNightSkippingRequirement(percentage);
	}

	public int getNightSkippingRequirement(int percentage) {
		return Math.max(1, MathHelper.ceil(this.total * percentage / 100.0F));
	}

	/**
	 * Очищает sleeping.
	 */
	public void clearSleeping() {
		this.sleeping = 0;
	}

	public int getSleeping() {
		return this.sleeping;
	}

	/**
	 * Update.
	 *
	 * @param players players
	 *
	 * @return boolean — результат операции
	 */
	public boolean update(List<ServerPlayerEntity> players) {
		int i = this.total;
		int j = this.sleeping;
		this.total = 0;
		this.sleeping = 0;

		for (ServerPlayerEntity serverPlayerEntity : players) {
			if (!serverPlayerEntity.isSpectator()) {
				this.total++;
				if (serverPlayerEntity.isSleeping()) {
					this.sleeping++;
				}
			}
		}

		return (j > 0 || this.sleeping > 0) && (i != this.total || j != this.sleeping);
	}
}
