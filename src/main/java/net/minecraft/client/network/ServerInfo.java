package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.PngMetadata;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Информация о сервере Minecraft в списке серверов.
 * Хранит адрес, имя, иконку, политику ресурс-паков и статус пинга.
 */
@Environment(EnvType.CLIENT)
public class ServerInfo {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_FAVICON_SIZE = 1024;
	private static final int NO_CODE_OF_CONDUCT = 0;

	public String name;
	public String address;
	public Text playerCountLabel;
	public Text label;
	public ServerMetadata.@Nullable Players players;
	public long ping;
	public int protocolVersion = SharedConstants.getGameVersion().protocolVersion();
	public Text version = Text.literal(SharedConstants.getGameVersion().name());
	public List<Text> playerListSummary = Collections.emptyList();

	private ServerInfo.ResourcePackPolicy resourcePackPolicy = ServerInfo.ResourcePackPolicy.PROMPT;
	private byte @Nullable [] favicon;
	private ServerInfo.ServerType serverType;
	private int acceptedCodeOfConduct;
	private ServerInfo.Status status = ServerInfo.Status.INITIAL;

	/**
	 * @param name       отображаемое имя сервера
	 * @param address    адрес сервера (host:port)
	 * @param serverType тип сервера (LAN, REALM, OTHER)
	 */
	public ServerInfo(String name, String address, ServerInfo.ServerType serverType) {
		this.name = name;
		this.address = address;
		this.serverType = serverType;
	}

	/**
	 * Сериализует информацию о сервере в NBT для сохранения в файл.
	 *
	 * @return NBT-тег с данными сервера
	 */
	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("name", name);
		nbt.putString("ip", address);
		nbt.putNullable("icon", Codecs.BASE_64, favicon);
		nbt.copyFromCodec(ServerInfo.ResourcePackPolicy.CODEC, resourcePackPolicy);

		if (acceptedCodeOfConduct != NO_CODE_OF_CONDUCT) {
			nbt.putInt("acceptedCodeOfConduct", acceptedCodeOfConduct);
		}

		return nbt;
	}

	/**
	 * Возвращает политику загрузки ресурс-паков сервера.
	 *
	 * @return политика ресурс-паков
	 */
	public ServerInfo.ResourcePackPolicy getResourcePackPolicy() {
		return resourcePackPolicy;
	}

	/**
	 * Устанавливает политику загрузки ресурс-паков.
	 *
	 * @param resourcePackPolicy новая политика
	 */
	public void setResourcePackPolicy(ServerInfo.ResourcePackPolicy resourcePackPolicy) {
		this.resourcePackPolicy = resourcePackPolicy;
	}

	/**
	 * Десериализует информацию о сервере из NBT.
	 *
	 * @param root NBT-тег с данными
	 * @return восстановленный объект {@code ServerInfo}
	 */
	public static ServerInfo fromNbt(NbtCompound root) {
		ServerInfo info = new ServerInfo(
				root.getString("name", ""),
				root.getString("ip", ""),
				ServerInfo.ServerType.OTHER
		);
		info.setFavicon(root.<byte[]>get("icon", Codecs.BASE_64).orElse(null));
		info.setResourcePackPolicy(
				root.<ServerInfo.ResourcePackPolicy>decode(ServerInfo.ResourcePackPolicy.CODEC)
				    .orElse(ServerInfo.ResourcePackPolicy.PROMPT)
		);
		info.acceptedCodeOfConduct = root.getInt("acceptedCodeOfConduct", NO_CODE_OF_CONDUCT);
		return info;
	}

	/**
	 * Возвращает иконку сервера в виде байтового массива PNG.
	 *
	 * @return байты иконки или {@code null}
	 */
	public byte @Nullable [] getFavicon() {
		return favicon;
	}

	/**
	 * Устанавливает иконку сервера.
	 *
	 * @param favicon байты PNG-иконки или {@code null}
	 */
	public void setFavicon(byte @Nullable [] favicon) {
		this.favicon = favicon;
	}

	/**
	 * Проверяет, является ли сервер локальным (LAN).
	 *
	 * @return {@code true} для LAN-сервера
	 */
	public boolean isLocal() {
		return serverType == ServerInfo.ServerType.LAN;
	}

	/**
	 * Проверяет, является ли сервер Realm.
	 *
	 * @return {@code true} для Realm
	 */
	public boolean isRealm() {
		return serverType == ServerInfo.ServerType.REALM;
	}

	/**
	 * Возвращает тип сервера.
	 *
	 * @return тип сервера
	 */
	public ServerInfo.ServerType getServerType() {
		return serverType;
	}

	/**
	 * Проверяет, принял ли игрок данный кодекс поведения.
	 *
	 * @param codeOfConductText текст кодекса поведения
	 * @return {@code true} если кодекс был принят
	 */
	public boolean hasAcceptedCodeOfConduct(String codeOfConductText) {
		return acceptedCodeOfConduct == codeOfConductText.hashCode();
	}

	/**
	 * Сохраняет факт принятия кодекса поведения.
	 *
	 * @param codeOfConductText текст кодекса поведения
	 */
	public void setAcceptedCodeOfConduct(String codeOfConductText) {
		acceptedCodeOfConduct = codeOfConductText.hashCode();
	}

	/**
	 * Сбрасывает принятие кодекса поведения.
	 */
	public void resetAcceptedCodeOfConduct() {
		acceptedCodeOfConduct = NO_CODE_OF_CONDUCT;
	}

	/**
	 * Копирует адрес, имя и иконку из другого объекта.
	 *
	 * @param other источник данных
	 */
	public void copyFrom(ServerInfo other) {
		address = other.address;
		name = other.name;
		favicon = other.favicon;
	}

	/**
	 * Копирует все данные включая настройки (политику ресурс-паков, тип сервера).
	 *
	 * @param other источник данных
	 */
	public void copyWithSettingsFrom(ServerInfo other) {
		copyFrom(other);
		setResourcePackPolicy(other.getResourcePackPolicy());
		serverType = other.serverType;
	}

	/**
	 * Возвращает текущий статус пинга сервера.
	 *
	 * @return статус
	 */
	public ServerInfo.Status getStatus() {
		return status;
	}

	/**
	 * Устанавливает статус пинга сервера.
	 *
	 * @param status новый статус
	 */
	public void setStatus(ServerInfo.Status status) {
		this.status = status;
	}

	/**
	 * Проверяет иконку сервера: декодирует PNG и проверяет размер.
	 * Возвращает {@code null} если иконка некорректна или превышает {@value #MAX_FAVICON_SIZE}px.
	 *
	 * @param favicon байты иконки или {@code null}
	 * @return валидные байты иконки или {@code null}
	 */
	public static byte @Nullable [] validateFavicon(byte @Nullable [] favicon) {
		if (favicon == null) {
			return null;
		}

		try {
			PngMetadata meta = PngMetadata.fromBytes(favicon);
			return (meta.width() <= MAX_FAVICON_SIZE && meta.height() <= MAX_FAVICON_SIZE)
			       ? favicon
			       : null;
		}
		catch (IOException e) {
			LOGGER.warn("Failed to decode server icon", e);
			return null;
		}
	}

	/**
	 * Политика загрузки ресурс-паков сервера.
	 */
	@Environment(EnvType.CLIENT)
	public enum ResourcePackPolicy {
		ENABLED("enabled"),
		DISABLED("disabled"),
		PROMPT("prompt");

		public static final MapCodec<ServerInfo.ResourcePackPolicy> CODEC = Codec.BOOL
				.optionalFieldOf("acceptTextures")
				.xmap(
						value -> value.<ServerInfo.ResourcePackPolicy>map(
								accept -> accept ? ENABLED : DISABLED
						).orElse(PROMPT),
						value -> switch (value) {
							case ENABLED -> Optional.of(true);
							case DISABLED -> Optional.of(false);
							case PROMPT -> Optional.empty();
						}
				);

		private final Text name;

		ResourcePackPolicy(final String key) {
			name = Text.translatable("manageServer.resourcePack." + key);
		}

		/**
		 * Возвращает локализованное название политики.
		 *
		 * @return текст названия
		 */
		public Text getName() {
			return name;
		}
	}

	/**
	 * Тип сервера в списке серверов.
	 */
	@Environment(EnvType.CLIENT)
	public enum ServerType {
		LAN,
		REALM,
		OTHER
	}

	/**
	 * Статус пинга сервера.
	 */
	@Environment(EnvType.CLIENT)
	public enum Status {
		INITIAL,
		PINGING,
		UNREACHABLE,
		INCOMPATIBLE,
		SUCCESSFUL
	}
}
