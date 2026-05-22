package net.minecraft.scoreboard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.List;
import java.util.Map;

/**
 * Персистентное состояние скорборда, сохраняемое на диск через систему {@link PersistentState}.
 * <p>
 * Хранит упакованный снимок всего скорборда: цели, очки, слоты отображения и команды.
 * Изменения применяются через {@link #set(Packed)}, который помечает состояние грязным
 * только при реальном изменении данных.
 */
public class ScoreboardState extends PersistentState {

	public static final PersistentStateType<ScoreboardState> TYPE = new PersistentStateType<>(
			"scoreboard",
			ScoreboardState::new,
			Packed.CODEC.xmap(ScoreboardState::new, ScoreboardState::getPackedState),
			DataFixTypes.SAVED_DATA_SCOREBOARD
	);

	private Packed packedState;

	private ScoreboardState() {
		this(Packed.EMPTY);
	}

	public ScoreboardState(Packed packedState) {
		this.packedState = packedState;
	}

	public Packed getPackedState() {
		return packedState;
	}

	/**
	 * Обновляет упакованное состояние и помечает его грязным только при реальном изменении.
	 * Предотвращает лишние записи на диск при отсутствии изменений.
	 */
	public void set(Packed packed) {
		if (packed.equals(packedState)) {
			return;
		}

		packedState = packed;
		markDirty();
	}

	/**
	 * Упакованный снимок всего скорборда для сериализации в NBT.
	 */
	public record Packed(
			List<ScoreboardObjective.Packed> objectives,
			List<Scoreboard.PackedEntry> scores,
			Map<ScoreboardDisplaySlot, String> displaySlots,
			List<Team.Packed> teams
	) {

		public static final Packed EMPTY = new Packed(List.of(), List.of(), Map.of(), List.of());

		public static final Codec<Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						ScoreboardObjective.Packed.CODEC
								.listOf()
								.optionalFieldOf("Objectives", List.of())
								.forGetter(Packed::objectives),
						Scoreboard.PackedEntry.CODEC
								.listOf()
								.optionalFieldOf("PlayerScores", List.of())
								.forGetter(Packed::scores),
						Codec.unboundedMap(ScoreboardDisplaySlot.CODEC, Codec.STRING)
								.optionalFieldOf("DisplaySlots", Map.of())
								.forGetter(Packed::displaySlots),
						Team.Packed.CODEC
								.listOf()
								.optionalFieldOf("Teams", List.of())
								.forGetter(Packed::teams)
				).apply(instance, Packed::new)
		);
	}
}
