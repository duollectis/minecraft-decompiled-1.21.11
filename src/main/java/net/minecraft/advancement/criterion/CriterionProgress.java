package net.minecraft.advancement.criterion;

import net.minecraft.network.PacketByteBuf;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Хранит прогресс выполнения одного критерия достижения для конкретного игрока.
 * Критерий считается выполненным, если {@code obtainedTime} не равен {@code null}.
 */
public class CriterionProgress {

	private @Nullable Instant obtainedTime;

	public CriterionProgress() {
	}

	public CriterionProgress(Instant obtainedTime) {
		this.obtainedTime = obtainedTime;
	}

	public boolean isObtained() {
		return obtainedTime != null;
	}

	public void obtain() {
		obtainedTime = Instant.now();
	}

	public void reset() {
		obtainedTime = null;
	}

	public @Nullable Instant getObtainedTime() {
		return obtainedTime;
	}

	@Override
	public String toString() {
		return "CriterionProgress{obtained=" + (obtainedTime == null ? "false" : obtainedTime) + "}";
	}

	public void toPacket(PacketByteBuf buf) {
		buf.writeNullable(obtainedTime, PacketByteBuf::writeInstant);
	}

	public static CriterionProgress fromPacket(PacketByteBuf buf) {
		CriterionProgress progress = new CriterionProgress();
		progress.obtainedTime = buf.readNullable(PacketByteBuf::readInstant);
		return progress;
	}
}
