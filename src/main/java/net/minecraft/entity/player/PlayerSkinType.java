package net.minecraft.entity.player;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Тип модели скина игрока: тонкая (slim) или широкая (wide/default).
 * Определяет форму рук в 3D-модели.
 */
public enum PlayerSkinType implements StringIdentifiable {

	SLIM("slim", "slim"),
	WIDE("wide", "default");

	public static final Codec<PlayerSkinType> CODEC = StringIdentifiable.createCodec(PlayerSkinType::values);
	public static final PacketCodec<ByteBuf, PlayerSkinType> PACKET_CODEC =
		PacketCodecs.BOOLEAN.xmap(slim -> slim ? SLIM : WIDE, model -> model == SLIM);

	private static final Function<String, PlayerSkinType> BY_MODEL_METADATA =
		StringIdentifiable.createMapper(values(), playerSkinType -> playerSkinType.modelMetadata);

	private final String name;
	private final String modelMetadata;

	PlayerSkinType(String name, String modelMetadata) {
		this.name = name;
		this.modelMetadata = modelMetadata;
	}

	/**
	 * Возвращает тип скина по строке метаданных модели из текстур профиля.
	 * Если метаданные не распознаны или {@code null} — возвращает {@link #WIDE}.
	 *
	 * @param modelMetadata строка метаданных (например, {@code "slim"} или {@code "default"})
	 * @return соответствующий тип скина
	 */
	public static PlayerSkinType byModelMetadata(@Nullable String modelMetadata) {
		return Objects.requireNonNullElse(BY_MODEL_METADATA.apply(modelMetadata), WIDE);
	}

	@Override
	public String asString() {
		return name;
	}
}
