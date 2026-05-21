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
 * {@code AbstractTeam}.
 */
public abstract class AbstractTeam {

	public boolean isEqual(@Nullable AbstractTeam team) {
		return team == null ? false : this == team;
	}

	public abstract String getName();

	public abstract MutableText decorateName(Text name);

	public abstract boolean shouldShowFriendlyInvisibles();

	public abstract boolean isFriendlyFireAllowed();

	public abstract AbstractTeam.VisibilityRule getNameTagVisibilityRule();

	public abstract Formatting getColor();

	public abstract Collection<String> getPlayerList();

	public abstract AbstractTeam.VisibilityRule getDeathMessageVisibilityRule();

	public abstract AbstractTeam.CollisionRule getCollisionRule();

	/**
	 * {@code CollisionRule}.
	 */
	public static enum CollisionRule implements StringIdentifiable {
		ALWAYS("always", 0),
		NEVER("never", 1),
		PUSH_OTHER_TEAMS("pushOtherTeams", 2),
		PUSH_OWN_TEAM("pushOwnTeam", 3);

		public static final Codec<AbstractTeam.CollisionRule>
				CODEC =
				StringIdentifiable.createCodec(AbstractTeam.CollisionRule::values);
		private static final IntFunction<AbstractTeam.CollisionRule>
				INDEX_MAPPER =
				ValueLists.<AbstractTeam.CollisionRule>createIndexToValueFunction(
						collisionRule -> collisionRule.index, values(), ValueLists.OutOfBoundsHandling.ZERO
				);
		public static final PacketCodec<ByteBuf, AbstractTeam.CollisionRule> PACKET_CODEC = PacketCodecs.indexed(
				INDEX_MAPPER, collisionRule -> collisionRule.index
		);
		public final String name;
		public final int index;

		private CollisionRule(final String name, final int index) {
			this.name = name;
			this.index = index;
		}

		public Text getDisplayName() {
			return Text.translatable("team.collision." + this.name);
		}

		@Override
		public String asString() {
			return this.name;
		}
	}

	/**
	 * {@code VisibilityRule}.
	 */
	public static enum VisibilityRule implements StringIdentifiable {
		ALWAYS("always", 0),
		NEVER("never", 1),
		HIDE_FOR_OTHER_TEAMS("hideForOtherTeams", 2),
		HIDE_FOR_OWN_TEAM("hideForOwnTeam", 3);

		public static final Codec<AbstractTeam.VisibilityRule>
				CODEC =
				StringIdentifiable.createCodec(AbstractTeam.VisibilityRule::values);
		private static final IntFunction<AbstractTeam.VisibilityRule>
				INDEX_MAPPER =
				ValueLists.<AbstractTeam.VisibilityRule>createIndexToValueFunction(
						visibilityRule -> visibilityRule.index, values(), ValueLists.OutOfBoundsHandling.ZERO
				);
		public static final PacketCodec<ByteBuf, AbstractTeam.VisibilityRule> PACKET_CODEC = PacketCodecs.indexed(
				INDEX_MAPPER, visibilityRule -> visibilityRule.index
		);
		public final String name;
		public final int index;

		private VisibilityRule(final String name, final int index) {
			this.name = name;
			this.index = index;
		}

		public Text getDisplayName() {
			return Text.translatable("team.visibility." + this.name);
		}

		@Override
		public String asString() {
			return this.name;
		}
	}
}
