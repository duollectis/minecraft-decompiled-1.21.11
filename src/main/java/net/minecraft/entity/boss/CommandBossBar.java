package net.minecraft.entity.boss;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Boss bar, управляемый командой {@code /bossbar}. Хранит набор UUID игроков,
 * которые должны видеть полосу, и синхронизирует реальных онлайн-игроков при их подключении.
 */
public class CommandBossBar extends ServerBossBar {

	private static final int DEFAULT_MAX_VALUE = 100;

	private final Identifier id;
	private final Set<UUID> playerUuids = Sets.newHashSet();
	private int value;
	private int maxValue = DEFAULT_MAX_VALUE;

	public CommandBossBar(Identifier id, Text displayName) {
		super(displayName, BossBar.Color.WHITE, BossBar.Style.PROGRESS);
		this.id = id;
		setPercent(0.0F);
	}

	public Identifier getId() {
		return id;
	}

	@Override
	public void addPlayer(ServerPlayerEntity player) {
		super.addPlayer(player);
		playerUuids.add(player.getUuid());
	}

	public void addPlayer(UUID uuid) {
		playerUuids.add(uuid);
	}

	@Override
	public void removePlayer(ServerPlayerEntity player) {
		super.removePlayer(player);
		playerUuids.remove(player.getUuid());
	}

	@Override
	public void clearPlayers() {
		super.clearPlayers();
		playerUuids.clear();
	}

	public int getValue() {
		return value;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setValue(int value) {
		this.value = value;
		setPercent(MathHelper.clamp((float) value / maxValue, 0.0F, 1.0F));
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
		setPercent(MathHelper.clamp((float) value / maxValue, 0.0F, 1.0F));
	}

	/**
	 * Возвращает кликабельное текстовое представление boss bar'а для использования в чате.
	 * Цвет и hover-событие отражают текущий цвет и идентификатор полосы.
	 */
	public final Text toHoverableText() {
		return Texts.bracketed(getName())
				.styled(style -> style
						.withColor(getColor().getTextFormat())
						.withHoverEvent(new HoverEvent.ShowText(Text.literal(getId().toString())))
						.withInsertion(getId().toString())
				);
	}

	/**
	 * Синхронизирует набор подписанных игроков с переданной коллекцией.
	 * Игроки, которых нет в новом списке, удаляются; новые — добавляются.
	 *
	 * @return {@code true}, если набор игроков изменился
	 */
	public boolean addPlayers(Collection<ServerPlayerEntity> players) {
		Map<UUID, ServerPlayerEntity> playersByUuid = players.stream()
				.collect(Collectors.toMap(ServerPlayerEntity::getUuid, p -> p));

		// UUID, которые были в списке, но отсутствуют в новом наборе — нужно удалить
		Set<UUID> toRemove = Sets.difference(playerUuids, playersByUuid.keySet());
		// Игроки, которых ещё нет в подписке — нужно добавить
		Set<ServerPlayerEntity> toAdd = players.stream()
				.filter(p -> !playerUuids.contains(p.getUuid()))
				.collect(Collectors.toSet());

		boolean changed = !toRemove.isEmpty() || !toAdd.isEmpty();

		for (UUID uuid : Set.copyOf(toRemove)) {
			getPlayers().stream()
					.filter(p -> p.getUuid().equals(uuid))
					.findFirst()
					.ifPresent(this::removePlayer);
			playerUuids.remove(uuid);
		}

		for (ServerPlayerEntity player : toAdd) {
			addPlayer(player);
		}

		return changed;
	}

	/**
	 * Восстанавливает {@link CommandBossBar} из сериализованного представления (NBT/JSON).
	 */
	public static CommandBossBar fromSerialized(Identifier id, Serialized serialized) {
		CommandBossBar bossBar = new CommandBossBar(id, serialized.name);
		bossBar.setVisible(serialized.visible);
		bossBar.setValue(serialized.value);
		bossBar.setMaxValue(serialized.max);
		bossBar.setColor(serialized.color);
		bossBar.setStyle(serialized.overlay);
		bossBar.setDarkenSky(serialized.darkenScreen);
		bossBar.setDragonMusic(serialized.playBossMusic);
		bossBar.setThickenFog(serialized.createWorldFog);
		serialized.players.forEach(bossBar::addPlayer);
		return bossBar;
	}

	public Serialized toSerialized() {
		return new Serialized(
				getName(),
				isVisible(),
				getValue(),
				getMaxValue(),
				getColor(),
				getStyle(),
				shouldDarkenSky(),
				hasDragonMusic(),
				shouldThickenFog(),
				Set.copyOf(playerUuids)
		);
	}

	/**
	 * Добавляет игрока в реальный список подписчиков, если его UUID зарегистрирован в этом boss bar'е.
	 * Вызывается при подключении игрока к серверу.
	 */
	public void onPlayerConnect(ServerPlayerEntity player) {
		if (playerUuids.contains(player.getUuid())) {
			addPlayer(player);
		}
	}

	/**
	 * Удаляет игрока из реального списка подписчиков без изменения набора UUID.
	 * Вызывается при отключении игрока от сервера.
	 */
	public void onPlayerDisconnect(ServerPlayerEntity player) {
		super.removePlayer(player);
	}

	/**
	 * Сериализованное представление {@link CommandBossBar} для сохранения в NBT.
	 */
	public record Serialized(
			Text name,
			boolean visible,
			int value,
			int max,
			BossBar.Color color,
			BossBar.Style overlay,
			boolean darkenScreen,
			boolean playBossMusic,
			boolean createWorldFog,
			Set<UUID> players
	) {

		public static final Codec<Serialized> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						TextCodecs.CODEC.fieldOf("Name").forGetter(Serialized::name),
						Codec.BOOL.optionalFieldOf("Visible", false).forGetter(Serialized::visible),
						Codec.INT.optionalFieldOf("Value", 0).forGetter(Serialized::value),
						Codec.INT.optionalFieldOf("Max", DEFAULT_MAX_VALUE).forGetter(Serialized::max),
						BossBar.Color.CODEC.optionalFieldOf("Color", BossBar.Color.WHITE).forGetter(Serialized::color),
						BossBar.Style.CODEC.optionalFieldOf("Overlay", BossBar.Style.PROGRESS).forGetter(Serialized::overlay),
						Codec.BOOL.optionalFieldOf("DarkenScreen", false).forGetter(Serialized::darkenScreen),
						Codec.BOOL.optionalFieldOf("PlayBossMusic", false).forGetter(Serialized::playBossMusic),
						Codec.BOOL.optionalFieldOf("CreateWorldFog", false).forGetter(Serialized::createWorldFog),
						Uuids.SET_CODEC.optionalFieldOf("Players", Set.of()).forGetter(Serialized::players)
				).apply(instance, Serialized::new)
		);
	}
}
