package net.minecraft.server;

import net.minecraft.network.packet.s2c.play.TickStepS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateTickRateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TimeHelper;
import net.minecraft.world.tick.TickManager;

import java.util.Locale;

/**
 * Серверная реализация менеджера тиков с поддержкой спринта (ускоренного выполнения тиков),
 * пошагового режима и синхронизации состояния с клиентами через пакеты.
 */
public class ServerTickManager extends TickManager {

	private long sprintTicks = 0L;
	private long sprintStartTime = 0L;
	private long sprintTime = 0L;
	private long scheduledSprintTicks = 0L;
	private boolean wasFrozen = false;
	private final MinecraftServer server;

	public ServerTickManager(MinecraftServer server) {
		this.server = server;
	}

	public boolean isSprinting() {
		return scheduledSprintTicks > 0L;
	}

	@Override
	public void setFrozen(boolean frozen) {
		super.setFrozen(frozen);
		sendUpdateTickRatePacket();
	}

	private void sendUpdateTickRatePacket() {
		server.getPlayerManager().sendToAll(UpdateTickRateS2CPacket.create(this));
	}

	private void sendStepPacket() {
		server.getPlayerManager().sendToAll(TickStepS2CPacket.create(this));
	}

	public boolean step(int ticks) {
		if (!isFrozen()) {
			return false;
		}

		stepTicks = ticks;
		sendStepPacket();
		return true;
	}

	public boolean stopStepping() {
		if (stepTicks <= 0) {
			return false;
		}

		stepTicks = 0;
		sendStepPacket();
		return true;
	}

	public boolean stopSprinting() {
		if (sprintTicks <= 0L) {
			return false;
		}

		finishSprinting();
		return true;
	}

	/**
	 * Запускает ускоренное выполнение указанного количества тиков.
	 * Если спринт уже активен — перезапускает его с новым значением.
	 *
	 * @param ticks количество тиков для ускоренного выполнения
	 * @return {@code true} если спринт уже был активен до вызова
	 */
	public boolean startSprint(int ticks) {
		boolean wasAlreadySprinting = sprintTicks > 0L;
		sprintTime = 0L;
		scheduledSprintTicks = ticks;
		sprintTicks = ticks;
		wasFrozen = isFrozen();
		setFrozen(false);
		return wasAlreadySprinting;
	}

	private void finishSprinting() {
		long completedTicks = scheduledSprintTicks - sprintTicks;
		double elapsedMillis = Math.max(1.0, (double) sprintTime) / TimeHelper.MILLI_IN_NANOS;
		int ticksPerSecond = (int) (TimeHelper.SECOND_IN_MILLIS * completedTicks / elapsedMillis);
		String msPerTick = String.format(Locale.ROOT, "%.2f", completedTicks == 0L ? getMillisPerTick() : elapsedMillis / completedTicks);
		scheduledSprintTicks = 0L;
		sprintTime = 0L;
		server
				.getCommandSource()
				.sendFeedback(() -> Text.translatable("commands.tick.sprint.report", ticksPerSecond, msPerTick), true);
		sprintTicks = 0L;
		setFrozen(wasFrozen);
		server.updateAutosaveTicks();
	}

	public boolean sprint() {
		if (!shouldTick) {
			return false;
		}

		if (sprintTicks <= 0L) {
			finishSprinting();
			return false;
		}

		sprintStartTime = System.nanoTime();
		sprintTicks--;
		return true;
	}

	public void updateSprintTime() {
		sprintTime = sprintTime + (System.nanoTime() - sprintStartTime);
	}

	@Override
	public void setTickRate(float tickRate) {
		super.setTickRate(tickRate);
		server.updateAutosaveTicks();
		sendUpdateTickRatePacket();
	}

	public void sendPackets(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(UpdateTickRateS2CPacket.create(this));
		player.networkHandler.sendPacket(TickStepS2CPacket.create(this));
	}
}
