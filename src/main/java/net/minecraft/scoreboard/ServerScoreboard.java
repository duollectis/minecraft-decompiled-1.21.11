package net.minecraft.scoreboard;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Серверная реализация скорборда с поддержкой сетевой синхронизации.
 * <p>
 * Расширяет {@link Scoreboard}, добавляя отправку пакетов всем игрокам
 * при изменении целей, очков и команд. Также управляет набором
 * «синхронизируемых» целей ({@code syncableObjectives}), которые
 * активно транслируются клиентам.
 */
public class ServerScoreboard extends Scoreboard {

	private static final int PACKET_MODE_ADD = 0;
	private static final int PACKET_MODE_REMOVE = 1;
	private static final int PACKET_MODE_UPDATE = 2;

	private final MinecraftServer server;
	private final Set<ScoreboardObjective> syncableObjectives = Sets.newHashSet();
	private boolean dirty;

	public ServerScoreboard(MinecraftServer server) {
		this.server = server;
	}

	/**
	 * Загружает состояние скорборда из упакованного снимка.
	 * Вызывается при старте сервера для восстановления данных с диска.
	 */
	public void read(ScoreboardState.Packed packed) {
		packed.objectives().forEach(this::addObjective);
		packed.scores().forEach(this::addEntry);
		packed.displaySlots().forEach((slot, objectiveName) -> {
			ScoreboardObjective objective = getNullableObjective(objectiveName);
			setObjectiveSlot(slot, objective);
		});
		packed.teams().forEach(this::addTeam);
	}

	private ScoreboardState.Packed toPacked() {
		return new ScoreboardState.Packed(
				getPackedObjectives(),
				pack(),
				getObjectivesBySlots(),
				getPackedTeams()
		);
	}

	@Override
	protected void updateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, ScoreboardScore score) {
		super.updateScore(scoreHolder, objective, score);
		if (syncableObjectives.contains(objective)) {
			server.getPlayerManager().sendToAll(
					new ScoreboardScoreUpdateS2CPacket(
							scoreHolder.getNameForScoreboard(),
							objective.getName(),
							score.getScore(),
							Optional.ofNullable(score.getDisplayText()),
							Optional.ofNullable(score.getNumberFormat())
					)
			);
		}

		markDirty();
	}

	@Override
	protected void resetScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		super.resetScore(scoreHolder, objective);
		markDirty();
	}

	@Override
	public void onScoreHolderRemoved(ScoreHolder scoreHolder) {
		super.onScoreHolderRemoved(scoreHolder);
		server.getPlayerManager().sendToAll(
				new ScoreboardScoreResetS2CPacket(scoreHolder.getNameForScoreboard(), null)
		);
		markDirty();
	}

	@Override
	public void onScoreRemoved(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		super.onScoreRemoved(scoreHolder, objective);
		if (syncableObjectives.contains(objective)) {
			server.getPlayerManager().sendToAll(
					new ScoreboardScoreResetS2CPacket(scoreHolder.getNameForScoreboard(), objective.getName())
			);
		}

		markDirty();
	}

	/**
	 * Переназначает цель на слот отображения, управляя синхронизацией старой и новой цели.
	 * Если старая цель больше не отображается ни в одном слоте — останавливает её синхронизацию.
	 */
	@Override
	public void setObjectiveSlot(ScoreboardDisplaySlot slot, @Nullable ScoreboardObjective objective) {
		ScoreboardObjective previousObjective = getObjectiveForSlot(slot);
		super.setObjectiveSlot(slot, objective);

		if (previousObjective != objective && previousObjective != null) {
			if (countDisplaySlots(previousObjective) > 0) {
				server.getPlayerManager().sendToAll(new ScoreboardDisplayS2CPacket(slot, objective));
			} else {
				stopSyncing(previousObjective);
			}
		}

		if (objective == null) {
			return;
		}

		if (syncableObjectives.contains(objective)) {
			server.getPlayerManager().sendToAll(new ScoreboardDisplayS2CPacket(slot, objective));
		} else {
			startSyncing(objective);
		}

		markDirty();
	}

	@Override
	public boolean addScoreHolderToTeam(String scoreHolderName, Team team) {
		if (!super.addScoreHolderToTeam(scoreHolderName, team)) {
			return false;
		}

		server.getPlayerManager().sendToAll(
				TeamS2CPacket.changePlayerTeam(team, scoreHolderName, TeamS2CPacket.Operation.ADD)
		);
		refreshWaypointTrackingFor(scoreHolderName);
		markDirty();
		return true;
	}

	@Override
	public void removeScoreHolderFromTeam(String scoreHolderName, Team team) {
		super.removeScoreHolderFromTeam(scoreHolderName, team);
		server.getPlayerManager().sendToAll(
				TeamS2CPacket.changePlayerTeam(team, scoreHolderName, TeamS2CPacket.Operation.REMOVE)
		);
		refreshWaypointTrackingFor(scoreHolderName);
		markDirty();
	}

	@Override
	public void updateObjective(ScoreboardObjective objective) {
		super.updateObjective(objective);
		markDirty();
	}

	@Override
	public void updateExistingObjective(ScoreboardObjective objective) {
		super.updateExistingObjective(objective);
		if (syncableObjectives.contains(objective)) {
			server.getPlayerManager().sendToAll(
					new ScoreboardObjectiveUpdateS2CPacket(objective, PACKET_MODE_UPDATE)
			);
		}

		markDirty();
	}

	@Override
	public void updateRemovedObjective(ScoreboardObjective objective) {
		super.updateRemovedObjective(objective);
		if (syncableObjectives.contains(objective)) {
			stopSyncing(objective);
		}

		markDirty();
	}

	@Override
	public void updateScoreboardTeamAndPlayers(Team team) {
		super.updateScoreboardTeamAndPlayers(team);
		server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, true));
		markDirty();
	}

	@Override
	public void updateScoreboardTeam(Team team) {
		super.updateScoreboardTeam(team);
		server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, false));
		refreshWaypointTrackingFor(team);
		markDirty();
	}

	@Override
	public void updateRemovedTeam(Team team) {
		super.updateRemovedTeam(team);
		server.getPlayerManager().sendToAll(TeamS2CPacket.updateRemovedTeam(team));
		refreshWaypointTrackingFor(team);
		markDirty();
	}

	protected void markDirty() {
		dirty = true;
	}

	/**
	 * Записывает текущее состояние скорборда в {@link ScoreboardState}, если оно изменилось.
	 * Вызывается периодически сервером для сохранения данных на диск.
	 */
	public void writeTo(ScoreboardState state) {
		if (!dirty) {
			return;
		}

		dirty = false;
		state.set(toPacked());
	}

	/**
	 * Создаёт список пакетов для добавления цели клиенту:
	 * пакет создания цели, пакеты назначения слотов и пакеты всех текущих очков.
	 */
	public List<Packet<?>> createChangePackets(ScoreboardObjective objective) {
		List<Packet<?>> packets = Lists.newArrayList();
		packets.add(new ScoreboardObjectiveUpdateS2CPacket(objective, PACKET_MODE_ADD));

		for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
			if (getObjectiveForSlot(slot) == objective) {
				packets.add(new ScoreboardDisplayS2CPacket(slot, objective));
			}
		}

		for (ScoreboardEntry entry : getScoreboardEntries(objective)) {
			packets.add(new ScoreboardScoreUpdateS2CPacket(
					entry.owner(),
					objective.getName(),
					entry.value(),
					Optional.ofNullable(entry.display()),
					Optional.ofNullable(entry.numberFormatOverride())
			));
		}

		return packets;
	}

	/**
	 * Начинает синхронизацию цели: отправляет все текущие данные всем игрокам
	 * и добавляет цель в набор синхронизируемых.
	 */
	public void startSyncing(ScoreboardObjective objective) {
		List<Packet<?>> packets = createChangePackets(objective);

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			for (Packet<?> packet : packets) {
				player.networkHandler.sendPacket(packet);
			}
		}

		syncableObjectives.add(objective);
	}

	/**
	 * Создаёт список пакетов для удаления цели у клиента:
	 * пакет удаления цели и пакеты очистки слотов отображения.
	 */
	public List<Packet<?>> createRemovePackets(ScoreboardObjective objective) {
		List<Packet<?>> packets = Lists.newArrayList();
		packets.add(new ScoreboardObjectiveUpdateS2CPacket(objective, PACKET_MODE_REMOVE));

		for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
			if (getObjectiveForSlot(slot) == objective) {
				packets.add(new ScoreboardDisplayS2CPacket(slot, objective));
			}
		}

		return packets;
	}

	/**
	 * Останавливает синхронизацию цели: отправляет пакеты удаления всем игрокам
	 * и убирает цель из набора синхронизируемых.
	 */
	public void stopSyncing(ScoreboardObjective objective) {
		List<Packet<?>> packets = createRemovePackets(objective);

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			for (Packet<?> packet : packets) {
				player.networkHandler.sendPacket(packet);
			}
		}

		syncableObjectives.remove(objective);
	}

	/**
	 * Подсчитывает количество слотов отображения, на которые назначена данная цель.
	 */
	public int countDisplaySlots(ScoreboardObjective objective) {
		int count = 0;

		for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
			if (getObjectiveForSlot(slot) == objective) {
				count++;
			}
		}

		return count;
	}

	private void refreshWaypointTrackingFor(String playerName) {
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
		if (player == null) {
			return;
		}

		player.getEntityWorld().getWaypointHandler().refreshTracking(player);
	}

	private void refreshWaypointTrackingFor(Team team) {
		for (ServerWorld world : server.getWorlds()) {
			team.getPlayerList()
					.stream()
					.map(playerName -> server.getPlayerManager().getPlayer(playerName))
					.filter(Objects::nonNull)
					.forEach(player -> world.getWaypointHandler().refreshTracking(player));
		}
	}
}
