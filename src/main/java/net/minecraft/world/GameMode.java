package net.minecraft.world;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * {@code GameMode}.
 */
public enum GameMode implements StringIdentifiable {
	SURVIVAL(0, "survival"),
	CREATIVE(1, "creative"),
	ADVENTURE(2, "adventure"),
	SPECTATOR(3, "spectator");

	public static final GameMode DEFAULT = SURVIVAL;
	public static final StringIdentifiable.EnumCodec<GameMode> CODEC = StringIdentifiable.createCodec(GameMode::values);
	private static final IntFunction<GameMode> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
			GameMode::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
	);
	public static final PacketCodec<ByteBuf, GameMode>
			PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, GameMode::getIndex);
	@Deprecated
	public static final Codec<GameMode> INDEX_CODEC = Codec.INT.xmap(GameMode::byIndex, GameMode::getIndex);
	private static final int UNKNOWN = -1;
	private final int index;
	private final String id;
	private final Text simpleTranslatableName;
	private final Text translatableName;

	private GameMode(final int index, final String id) {
		this.index = index;
		this.id = id;
		this.simpleTranslatableName = Text.translatable("selectWorld.gameMode." + id);
		this.translatableName = Text.translatable("gameMode." + id);
	}

	public int getIndex() {
		return this.index;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String asString() {
		return this.id;
	}

	public Text getTranslatableName() {
		return this.translatableName;
	}

	public Text getSimpleTranslatableName() {
		return this.simpleTranslatableName;
	}

	public void setAbilities(PlayerAbilities abilities) {
		if (this == CREATIVE) {
			abilities.allowFlying = true;
			abilities.creativeMode = true;
			abilities.invulnerable = true;
		}
		else if (this == SPECTATOR) {
			abilities.allowFlying = true;
			abilities.creativeMode = false;
			abilities.invulnerable = true;
			abilities.flying = true;
		}
		else {
			abilities.allowFlying = false;
			abilities.creativeMode = false;
			abilities.invulnerable = false;
			abilities.flying = false;
		}

		abilities.allowModifyWorld = !this.isBlockBreakingRestricted();
	}

	public boolean isBlockBreakingRestricted() {
		return this == ADVENTURE || this == SPECTATOR;
	}

	public boolean isCreative() {
		return this == CREATIVE;
	}

	public boolean isSurvivalLike() {
		return this == SURVIVAL || this == ADVENTURE;
	}

	public static GameMode byIndex(int index) {
		return INDEX_MAPPER.apply(index);
	}

	public static GameMode byId(String id) {
		return byId(id, SURVIVAL);
	}

	@Contract("_,!null->!null;_,null->_")
	public static @Nullable GameMode byId(String id, @Nullable GameMode fallback) {
		GameMode gameMode = CODEC.byId(id);
		return gameMode != null ? gameMode : fallback;
	}

	public static int getId(@Nullable GameMode gameMode) {
		return gameMode != null ? gameMode.index : -1;
	}

	public static @Nullable GameMode getOrNull(int index) {
		return index == -1 ? null : byIndex(index);
	}

	public static boolean isValid(int index) {
		return Arrays.stream(values()).anyMatch(gameMode -> gameMode.index == index);
	}
}
