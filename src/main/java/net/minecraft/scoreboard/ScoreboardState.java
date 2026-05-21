package net.minecraft.scoreboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.List;
import java.util.Map;

/**
 * {@code ScoreboardState}.
 */
public class ScoreboardState extends PersistentState {

	public static final PersistentStateType<ScoreboardState> TYPE = new PersistentStateType<>(
			"scoreboard",
			ScoreboardState::new,
			ScoreboardState.Packed.CODEC.xmap(ScoreboardState::new, ScoreboardState::getPackedState),
			DataFixTypes.SAVED_DATA_SCOREBOARD
	);
	private ScoreboardState.Packed packedState;

	private ScoreboardState() {
		this(ScoreboardState.Packed.EMPTY);
	}

	public ScoreboardState(ScoreboardState.Packed packedState) {
		this.packedState = packedState;
	}

	public ScoreboardState.Packed getPackedState() {
		return this.packedState;
	}

	public void set(ScoreboardState.Packed packed) {
		if (!packed.equals(this.packedState)) {
			this.packedState = packed;
			this.markDirty();
		}
	}

	/**
	 * {@code Packed}.
	 */
	public record Packed(
			List<ScoreboardObjective.Packed> objectives,
			List<Scoreboard.PackedEntry> scores,
			Map<ScoreboardDisplaySlot, String> displaySlots,
			List<Team.Packed> teams
	) {

		public static final ScoreboardState.Packed
				EMPTY =
				new ScoreboardState.Packed(List.of(), List.of(), Map.of(), List.of());
		public static final Codec<ScoreboardState.Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    ScoreboardObjective.Packed.CODEC
								                    .listOf()
								                    .optionalFieldOf("Objectives", List.of())
								                    .forGetter(ScoreboardState.Packed::objectives),
						                    Scoreboard.PackedEntry.CODEC
								                    .listOf()
								                    .optionalFieldOf("PlayerScores", List.of())
								                    .forGetter(ScoreboardState.Packed::scores),
						                    Codec.unboundedMap(ScoreboardDisplaySlot.CODEC, Codec.STRING)
						                         .optionalFieldOf("DisplaySlots", Map.of())
						                         .forGetter(ScoreboardState.Packed::displaySlots),
						                    Team.Packed.CODEC
								                    .listOf()
								                    .optionalFieldOf("Teams", List.of())
								                    .forGetter(ScoreboardState.Packed::teams)
				                    )
				                    .apply(instance, ScoreboardState.Packed::new)
		);
	}
}
