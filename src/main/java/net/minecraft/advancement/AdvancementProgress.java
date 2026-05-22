package net.minecraft.advancement;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Хранит прогресс выполнения достижения: состояние каждого критерия
 * и требования, которым должен соответствовать прогресс.
 */
public class AdvancementProgress implements Comparable<AdvancementProgress> {

	private static final int MIN_PROGRESS_BAR_REQUIREMENTS = 2;

	private static final DateTimeFormatter TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);

	private static final Codec<Instant> TIME_CODEC =
		Codecs.formattedTime(TIME_FORMATTER)
		      .xmap(Instant::from, instant -> instant.atZone(ZoneId.systemDefault()));

	private static final Codec<Map<String, CriterionProgress>> MAP_CODEC =
		Codec.unboundedMap(Codec.STRING, TIME_CODEC)
		     .xmap(
			     map -> Util.transformMapValues(map, CriterionProgress::new),
			     map -> {
				     Map<String, Instant> result = new HashMap<>();
				     map.forEach((key, progress) -> {
					     if (progress.isObtained()) {
						     result.put(key, Objects.requireNonNull(progress.getObtainedTime()));
					     }
				     });
				     return result;
			     }
		     );

	public static final Codec<AdvancementProgress> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			MAP_CODEC.optionalFieldOf("criteria", Map.of()).forGetter(p -> p.criteriaProgresses),
			Codec.BOOL.fieldOf("done").orElse(true).forGetter(AdvancementProgress::isDone)
		).apply(
			instance,
			(criteriaProgresses, done) -> new AdvancementProgress(new HashMap<>(criteriaProgresses))
		)
	);

	private final Map<String, CriterionProgress> criteriaProgresses;
	private AdvancementRequirements requirements = AdvancementRequirements.EMPTY;

	private AdvancementProgress(Map<String, CriterionProgress> criteriaProgresses) {
		this.criteriaProgresses = criteriaProgresses;
	}

	public AdvancementProgress() {
		criteriaProgresses = Maps.newHashMap();
	}

	/**
	 * Синхронизирует набор критериев с заданными требованиями:
	 * удаляет устаревшие критерии и добавляет недостающие.
	 */
	public void init(AdvancementRequirements requirements) {
		java.util.Set<String> names = requirements.getNames();
		criteriaProgresses.entrySet().removeIf(entry -> !names.contains(entry.getKey()));

		for (String name : names) {
			criteriaProgresses.putIfAbsent(name, new CriterionProgress());
		}

		this.requirements = requirements;
	}

	public boolean isDone() {
		return requirements.matches(this::isCriterionObtained);
	}

	public boolean isAnyObtained() {
		for (CriterionProgress progress : criteriaProgresses.values()) {
			if (progress.isObtained()) {
				return true;
			}
		}
		return false;
	}

	public boolean obtain(String name) {
		CriterionProgress progress = criteriaProgresses.get(name);
		if (progress == null || progress.isObtained()) {
			return false;
		}
		progress.obtain();
		return true;
	}

	public boolean reset(String name) {
		CriterionProgress progress = criteriaProgresses.get(name);
		if (progress == null || !progress.isObtained()) {
			return false;
		}
		progress.reset();
		return true;
	}

	@Override
	public String toString() {
		return "AdvancementProgress{criteria=" + criteriaProgresses + ", requirements=" + requirements + "}";
	}

	public void toPacket(PacketByteBuf buf) {
		buf.writeMap(
			criteriaProgresses,
			PacketByteBuf::writeString,
			(bufx, progress) -> progress.toPacket(bufx)
		);
	}

	public static AdvancementProgress fromPacket(PacketByteBuf buf) {
		Map<String, CriterionProgress> map = buf.readMap(PacketByteBuf::readString, CriterionProgress::fromPacket);
		return new AdvancementProgress(map);
	}

	public @Nullable CriterionProgress getCriterionProgress(String name) {
		return criteriaProgresses.get(name);
	}

	private boolean isCriterionObtained(String name) {
		CriterionProgress progress = getCriterionProgress(name);
		return progress != null && progress.isObtained();
	}

	public float getProgressBarPercentage() {
		if (criteriaProgresses.isEmpty()) {
			return 0.0F;
		}
		float total = requirements.getLength();
		float obtained = countObtainedRequirements();
		return obtained / total;
	}

	public @Nullable Text getProgressBarFraction() {
		if (criteriaProgresses.isEmpty()) {
			return null;
		}
		int total = requirements.getLength();
		if (total <= MIN_PROGRESS_BAR_REQUIREMENTS - 1) {
			return null;
		}
		int obtained = countObtainedRequirements();
		return Text.translatable("advancements.progress", obtained, total);
	}

	private int countObtainedRequirements() {
		return requirements.countMatches(this::isCriterionObtained);
	}

	public Iterable<String> getUnobtainedCriteria() {
		List<String> result = new ArrayList<>();
		criteriaProgresses.forEach((name, progress) -> {
			if (!progress.isObtained()) {
				result.add(name);
			}
		});
		return result;
	}

	public Iterable<String> getObtainedCriteria() {
		List<String> result = new ArrayList<>();
		criteriaProgresses.forEach((name, progress) -> {
			if (progress.isObtained()) {
				result.add(name);
			}
		});
		return result;
	}

	public @Nullable Instant getEarliestProgressObtainDate() {
		return criteriaProgresses.values()
		                         .stream()
		                         .map(CriterionProgress::getObtainedTime)
		                         .filter(Objects::nonNull)
		                         .min(Comparator.naturalOrder())
		                         .orElse(null);
	}

	@Override
	public int compareTo(AdvancementProgress other) {
		Instant thisTime = getEarliestProgressObtainDate();
		Instant otherTime = other.getEarliestProgressObtainDate();

		if (thisTime == null && otherTime != null) {
			return 1;
		}
		if (thisTime != null && otherTime == null) {
			return -1;
		}
		return thisTime == null ? 0 : thisTime.compareTo(otherTime);
	}
}
