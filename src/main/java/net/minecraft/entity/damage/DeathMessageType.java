package net.minecraft.entity.damage;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

/**
 * Тип сообщения о смерти, определяющий, как формируется текст в чате при гибели сущности.
 */
public enum DeathMessageType implements StringIdentifiable {
	DEFAULT("default"),
	FALL_VARIANTS("fall_variants"),
	INTENTIONAL_GAME_DESIGN("intentional_game_design");

	public static final Codec<DeathMessageType> CODEC = StringIdentifiable.createCodec(DeathMessageType::values);

	private final String id;

	DeathMessageType(String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return id;
	}
}
