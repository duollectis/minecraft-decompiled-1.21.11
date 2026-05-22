package net.minecraft.advancement;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.PacketByteBuf;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Описывает логику завершения достижения: список групп критериев,
 * где каждая группа — это OR-условие, а все группы объединяются через AND.
 */
public record AdvancementRequirements(List<List<String>> requirements) {

	public static final Codec<AdvancementRequirements> CODEC = Codec.STRING
		.listOf()
		.listOf()
		.xmap(AdvancementRequirements::new, AdvancementRequirements::requirements);

	public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());

	public AdvancementRequirements(PacketByteBuf buf) {
		this(buf.readList(bufx -> bufx.readList(PacketByteBuf::readString)));
	}

	public void writeRequirements(PacketByteBuf buf) {
		buf.writeCollection(
			requirements,
			(bufx, group) -> bufx.writeCollection(group, PacketByteBuf::writeString)
		);
	}

	/** Создаёт требования, где каждый критерий должен быть выполнен (AND по всем). */
	public static AdvancementRequirements allOf(Collection<String> criteria) {
		return new AdvancementRequirements(criteria.stream().map(List::of).toList());
	}

	/** Создаёт требования, где достаточно выполнить любой из критериев (OR по всем). */
	public static AdvancementRequirements anyOf(Collection<String> criteria) {
		return new AdvancementRequirements(List.of(List.copyOf(criteria)));
	}

	public int getLength() {
		return requirements.size();
	}

	/**
	 * Возвращает {@code true}, если все группы требований удовлетворены предикатом.
	 * Пустой список требований всегда возвращает {@code false}.
	 */
	public boolean matches(Predicate<String> predicate) {
		if (requirements.isEmpty()) {
			return false;
		}
		for (List<String> group : requirements) {
			if (!anyMatch(group, predicate)) {
				return false;
			}
		}
		return true;
	}

	public int countMatches(Predicate<String> predicate) {
		int count = 0;
		for (List<String> group : requirements) {
			if (anyMatch(group, predicate)) {
				count++;
			}
		}
		return count;
	}

	private static boolean anyMatch(List<String> group, Predicate<String> predicate) {
		for (String criterion : group) {
			if (predicate.test(criterion)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Проверяет, что набор критериев в требованиях точно совпадает с переданным множеством.
	 */
	public DataResult<AdvancementRequirements> validate(Set<String> criteria) {
		Set<String> mentioned = new ObjectOpenHashSet<>();

		for (List<String> group : requirements) {
			if (group.isEmpty() && criteria.isEmpty()) {
				return DataResult.error(() -> "Requirement entry cannot be empty");
			}
			mentioned.addAll(group);
		}

		if (!criteria.equals(mentioned)) {
			Set<String> missing = Sets.difference(criteria, mentioned);
			Set<String> unknown = Sets.difference(mentioned, criteria);
			return DataResult.error(() ->
				"Advancement completion requirements did not exactly match specified criteria. Missing: "
					+ missing + ". Unknown: " + unknown
			);
		}

		return DataResult.success(this);
	}

	public boolean isEmpty() {
		return requirements.isEmpty();
	}

	@Override
	public String toString() {
		return requirements.toString();
	}

	public Set<String> getNames() {
		Set<String> names = new ObjectOpenHashSet<>();
		for (List<String> group : requirements) {
			names.addAll(group);
		}
		return names;
	}

	public interface CriterionMerger {

		CriterionMerger AND = AdvancementRequirements::allOf;
		CriterionMerger OR = AdvancementRequirements::anyOf;

		AdvancementRequirements create(Collection<String> requirements);
	}
}
