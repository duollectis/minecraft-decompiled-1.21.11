package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.MessageVerifier;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Запись об игроке в списке игроков (tab-list).
 * Хранит профиль, игровой режим, пинг, скин, сессию шифрования и отображаемое имя.
 */
@Environment(EnvType.CLIENT)
public class PlayerListEntry {

	private final GameProfile profile;
	private @Nullable Supplier<SkinTextures> texturesSupplier;
	private GameMode gameMode = GameMode.DEFAULT;
	private int latency;
	private @Nullable Text displayName;
	private boolean showHat = true;
	private @Nullable PublicPlayerSession session;
	private MessageVerifier messageVerifier;
	private int listOrder;

	/**
	 * @param profile            профиль игрока
	 * @param secureChatEnforced {@code true} если сервер требует подписанные сообщения
	 */
	public PlayerListEntry(GameProfile profile, boolean secureChatEnforced) {
		this.profile = profile;
		messageVerifier = getInitialVerifier(secureChatEnforced);
	}

	/**
	 * Возвращает профиль игрока.
	 *
	 * @return профиль Mojang
	 */
	public GameProfile getProfile() {
		return profile;
	}

	/**
	 * Возвращает активную публичную сессию игрока.
	 *
	 * @return сессия или {@code null} если не установлена
	 */
	public @Nullable PublicPlayerSession getSession() {
		return session;
	}

	/**
	 * Возвращает верификатор подписей сообщений.
	 *
	 * @return верификатор
	 */
	public MessageVerifier getMessageVerifier() {
		return messageVerifier;
	}

	/**
	 * Проверяет, есть ли у игрока публичный ключ шифрования.
	 *
	 * @return {@code true} если сессия установлена
	 */
	public boolean hasPublicKey() {
		return session != null;
	}

	/**
	 * Устанавливает публичную сессию и обновляет верификатор.
	 *
	 * @param session новая сессия
	 */
	protected void setSession(PublicPlayerSession session) {
		this.session = session;
		messageVerifier = session.createVerifier(PlayerPublicKey.EXPIRATION_GRACE_PERIOD);
	}

	/**
	 * Сбрасывает сессию и возвращает верификатор к начальному состоянию.
	 *
	 * @param secureChatEnforced {@code true} если сервер требует подписанные сообщения
	 */
	protected void resetSession(boolean secureChatEnforced) {
		session = null;
		messageVerifier = getInitialVerifier(secureChatEnforced);
	}

	/**
	 * Возвращает текущий игровой режим игрока.
	 *
	 * @return игровой режим
	 */
	public GameMode getGameMode() {
		return gameMode;
	}

	/**
	 * Устанавливает игровой режим игрока.
	 *
	 * @param gameMode новый игровой режим
	 */
	protected void setGameMode(GameMode gameMode) {
		this.gameMode = gameMode;
	}

	/**
	 * Возвращает задержку соединения игрока в миллисекундах.
	 *
	 * @return пинг
	 */
	public int getLatency() {
		return latency;
	}

	/**
	 * Устанавливает задержку соединения.
	 *
	 * @param latency пинг в миллисекундах
	 */
	protected void setLatency(int latency) {
		this.latency = latency;
	}

	/**
	 * Возвращает текстуры скина игрока, загружая их при первом обращении.
	 *
	 * @return текстуры скина
	 */
	public SkinTextures getSkinTextures() {
		if (texturesSupplier == null) {
			texturesSupplier = createTexturesSupplier(profile);
		}

		return texturesSupplier.get();
	}

	/**
	 * Возвращает команду скорборда, в которой состоит игрок.
	 *
	 * @return команда или {@code null}
	 */
	public @Nullable Team getScoreboardTeam() {
		return MinecraftClient.getInstance().world.getScoreboard().getScoreHolderTeam(profile.name());
	}

	/**
	 * Устанавливает отображаемое имя в списке игроков.
	 *
	 * @param displayName текст имени или {@code null} для сброса
	 */
	public void setDisplayName(@Nullable Text displayName) {
		this.displayName = displayName;
	}

	/**
	 * Возвращает отображаемое имя в списке игроков.
	 *
	 * @return текст имени или {@code null}
	 */
	public @Nullable Text getDisplayName() {
		return displayName;
	}

	/**
	 * Управляет отображением второго слоя скина (шляпа).
	 *
	 * @param showHat {@code true} чтобы показывать шляпу
	 */
	public void setShowHat(boolean showHat) {
		this.showHat = showHat;
	}

	/**
	 * Проверяет, нужно ли отображать второй слой скина.
	 *
	 * @return {@code true} если шляпа включена
	 */
	public boolean shouldShowHat() {
		return showHat;
	}

	/**
	 * Устанавливает порядок сортировки в списке игроков.
	 *
	 * @param listOrder порядковый номер
	 */
	public void setListOrder(int listOrder) {
		this.listOrder = listOrder;
	}

	/**
	 * Возвращает порядок сортировки в списке игроков.
	 *
	 * @return порядковый номер
	 */
	public int getListOrder() {
		return listOrder;
	}

	private static Supplier<SkinTextures> createTexturesSupplier(GameProfile profile) {
		MinecraftClient client = MinecraftClient.getInstance();
		boolean remote = client.uuidEquals(profile.id()) == false;
		return client.getSkinProvider().supplySkinTextures(profile, remote);
	}

	private static MessageVerifier getInitialVerifier(boolean secureChatEnforced) {
		return secureChatEnforced ? MessageVerifier.UNVERIFIED : MessageVerifier.NO_SIGNATURE;
	}
}
