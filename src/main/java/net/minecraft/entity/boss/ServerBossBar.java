package net.minecraft.entity.boss;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * Серверная реализация полосы здоровья босса. Управляет набором подписанных игроков
 * и автоматически рассылает сетевые пакеты при изменении любого свойства.
 */
public class ServerBossBar extends BossBar {

	private final Set<ServerPlayerEntity> players = Sets.newHashSet();
	private final Set<ServerPlayerEntity> unmodifiablePlayers = Collections.unmodifiableSet(players);
	private boolean visible = true;

	public ServerBossBar(Text displayName, Color color, Style style) {
		super(MathHelper.randomUuid(), displayName, color, style);
	}

	@Override
	public void setPercent(float percent) {
		if (percent == this.percent) {
			return;
		}

		super.setPercent(percent);
		sendPacket(BossBarS2CPacket::updateProgress);
	}

	@Override
	public void setColor(Color color) {
		if (color == this.color) {
			return;
		}

		super.setColor(color);
		sendPacket(BossBarS2CPacket::updateStyle);
	}

	@Override
	public void setStyle(Style style) {
		if (style == this.style) {
			return;
		}

		super.setStyle(style);
		sendPacket(BossBarS2CPacket::updateStyle);
	}

	@Override
	public BossBar setDarkenSky(boolean darkenSky) {
		if (darkenSky == this.darkenSky) {
			return this;
		}

		super.setDarkenSky(darkenSky);
		sendPacket(BossBarS2CPacket::updateProperties);
		return this;
	}

	@Override
	public BossBar setDragonMusic(boolean dragonMusic) {
		if (dragonMusic == this.dragonMusic) {
			return this;
		}

		super.setDragonMusic(dragonMusic);
		sendPacket(BossBarS2CPacket::updateProperties);
		return this;
	}

	@Override
	public BossBar setThickenFog(boolean thickenFog) {
		if (thickenFog == this.thickenFog) {
			return this;
		}

		super.setThickenFog(thickenFog);
		sendPacket(BossBarS2CPacket::updateProperties);
		return this;
	}

	@Override
	public void setName(Text name) {
		if (Objects.equal(name, this.name)) {
			return;
		}

		super.setName(name);
		sendPacket(BossBarS2CPacket::updateName);
	}

	/**
	 * Рассылает пакет всем подписанным игрокам, если boss bar видим.
	 */
	private void sendPacket(Function<BossBar, BossBarS2CPacket> packetFactory) {
		if (!visible) {
			return;
		}

		BossBarS2CPacket packet = packetFactory.apply(this);
		for (ServerPlayerEntity player : players) {
			player.networkHandler.sendPacket(packet);
		}
	}

	public void addPlayer(ServerPlayerEntity player) {
		if (players.add(player) && visible) {
			player.networkHandler.sendPacket(BossBarS2CPacket.add(this));
		}
	}

	public void removePlayer(ServerPlayerEntity player) {
		if (players.remove(player) && visible) {
			player.networkHandler.sendPacket(BossBarS2CPacket.remove(getUuid()));
		}
	}

	public void clearPlayers() {
		if (players.isEmpty()) {
			return;
		}

		for (ServerPlayerEntity player : new ArrayList<>(players)) {
			removePlayer(player);
		}
	}

	public boolean isVisible() {
		return visible;
	}

	/**
	 * Переключает видимость boss bar'а. При включении отправляет ADD-пакет, при выключении — REMOVE-пакет
	 * всем подписанным игрокам.
	 */
	public void setVisible(boolean visible) {
		if (visible == this.visible) {
			return;
		}

		this.visible = visible;
		for (ServerPlayerEntity player : players) {
			player.networkHandler.sendPacket(
					visible ? BossBarS2CPacket.add(this) : BossBarS2CPacket.remove(getUuid())
			);
		}
	}

	public Collection<ServerPlayerEntity> getPlayers() {
		return unmodifiablePlayers;
	}
}
