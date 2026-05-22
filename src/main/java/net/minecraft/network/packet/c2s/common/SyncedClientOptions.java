package net.minecraft.network.packet.c2s.common;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.Arm;

/**
 * Настройки клиента, синхронизируемые с сервером: язык, дальность прорисовки,
 * видимость чата, части скина, основная рука, фильтрация текста и частицы.
 */
public record SyncedClientOptions(
		String language,
		int viewDistance,
		ChatVisibility chatVisibility,
		boolean chatColorsEnabled,
		int playerModelParts,
		Arm mainArm,
		boolean filtersText,
		boolean allowsServerListing,
		ParticlesMode particleStatus
) {

	public static final int MAX_LANGUAGE_CODE_LENGTH = 16;

	public SyncedClientOptions(PacketByteBuf buf) {
		this(
				buf.readString(MAX_LANGUAGE_CODE_LENGTH),
				buf.readByte(),
				buf.readEnumConstant(ChatVisibility.class),
				buf.readBoolean(),
				buf.readUnsignedByte(),
				buf.readEnumConstant(Arm.class),
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readEnumConstant(ParticlesMode.class)
		);
	}

	public void write(PacketByteBuf buf) {
		buf.writeString(language);
		buf.writeByte(viewDistance);
		buf.writeEnumConstant(chatVisibility);
		buf.writeBoolean(chatColorsEnabled);
		buf.writeByte(playerModelParts);
		buf.writeEnumConstant(mainArm);
		buf.writeBoolean(filtersText);
		buf.writeBoolean(allowsServerListing);
		buf.writeEnumConstant(particleStatus);
	}

	public static SyncedClientOptions createDefault() {
		return new SyncedClientOptions(
				"en_us",
				2,
				ChatVisibility.FULL,
				true,
				0,
				PlayerEntity.MAIN_ARM,
				false,
				false,
				ParticlesMode.ALL
		);
	}
}
