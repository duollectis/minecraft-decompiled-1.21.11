package net.minecraft.scoreboard;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.IntFunction;

/**
 * Абстрактное представление команды в системе скорборда.
 * Определяет базовый контракт для всех реализаций команд,
 * включая правила видимости, столкновений и оформления имён участников.
 */
public abstract class AbstractTeam {

	/**
	 * Проверяет, является ли переданная команда той же самой командой по ссылке.
	 *
	 * @param team команда для сравнения, может быть {@code null}
	 * @return {@code true}, если {@code team} — это та же самая команда
	 */
	public boolean isEqual(@Nullable AbstractTeam team) {
		return team != null && this == team;
	}

	public abstract String getName();

	public abstract MutableText decorateName(Text name);

	public abstract boolean shouldShowFriendlyInvisibles();

	public abstract boolean isFriendlyFireAllowed();

	public abstract VisibilityRule getNameTagVisibilityRule();

	public abstract Formatting getColor();

	public abstract Collection<String> getPlayerList();

	public abstract VisibilityRule getDeathMessageVisibilityRule();

	public abstract CollisionRule getCollisionRule();

	/**
	 * Правило физического столкновения между участниками команд.
	 * Определяет, могут ли игроки физически проходить сквозь друг друга.
	 */
	public enum CollisionRule implements StringIdentifiable {
		ALWAYS("always", 0),
		NEVER("never", 1),
		PUSH_OTHER_TEAMS("pushOtherTeams", 2),
		PUSH_OWN_TEAM("pushOwnTeam", 3);

		public static final Codec<CollisionRule> CODEC = StringIdentifiable.createCodec(CollisionRule::values);
		private static final IntFunction<CollisionRule> INDEX_MAPPER = ValueLists.<CollisionRule>createIndexToValueFunction(
				rule -> rule.index,
				values(),
				ValueLists.OutOfBoundsHandling.ZERO
		);
		public static final PacketCodec<ByteBuf, CollisionRule> PACKET_CODEC = PacketCodecs.indexed(
				INDEX_MAPPER,
				rule -> rule.index
		);

		public final String name;
		public final int index;

		CollisionRule(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public Text getDisplayName() {
			return Text.translatable("team.collision." + name);
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/**
	 * Правило видимости имён/сообщений о смерти участников команды.
	 * Управляет тем, кто может видеть теги имён или сообщения о гибели.
	 */
	public enum VisibilityRule implements StringIdentifiable {
		ALWAYS("always", 0),
		NEVER("never", 1),
		HIDE_FOR_OTHER_TEAMS("hideForOtherTeams", 2),
		HIDE_FOR_OWN_TEAM("hideForOwnTeam", 3);

		public static final Codec<VisibilityRule> CODEC = StringIdentifiable.createCodec(VisibilityRule::values);
		private static final IntFunction<VisibilityRule> INDEX_MAPPER = ValueLists.<VisibilityRule>createIndexToValueFunction(
				rule -> rule.index,
				values(),
				ValueLists.OutOfBoundsHandling.ZERO
		);
		public static final PacketCodec<ByteBuf, VisibilityRule> PACKET_CODEC = PacketCodecs.indexed(
				INDEX_MAPPER,
				rule -> rule.index
		);

		public final String name;
		public final int index;

		VisibilityRule(String name, int index) {
			this.name = name;
			this.index = index;
		}

		public Text getDisplayName() {
			return Text.translatable("team.visibility." + name);
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
