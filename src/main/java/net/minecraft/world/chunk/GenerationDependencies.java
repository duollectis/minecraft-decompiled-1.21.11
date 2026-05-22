package net.minecraft.world.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.Locale;

/**
 * Описывает зависимости шага генерации чанка: для каждого статуса хранит
 * минимальный радиус соседних чанков, которые должны достичь этого статуса
 * перед запуском текущего шага.
 */
public final class GenerationDependencies {

	private final ImmutableList<ChunkStatus> dependencies;
	private final int[] additionalLevelsByStatus;

	/**
	 * Строит таблицу {@link #additionalLevelsByStatus}: для каждого индекса статуса
	 * сохраняет позицию в списке зависимостей, определяющую необходимый радиус.
	 */
	public GenerationDependencies(ImmutableList<ChunkStatus> dependencies) {
		this.dependencies = dependencies;
		int statusCount = dependencies.isEmpty() ? 0 : dependencies.getFirst().getIndex() + 1;
		additionalLevelsByStatus = new int[statusCount];

		for (int depIndex = 0; depIndex < dependencies.size(); depIndex++) {
			ChunkStatus status = dependencies.get(depIndex);
			int statusIndex = status.getIndex();

			for (int i = 0; i <= statusIndex; i++) {
				additionalLevelsByStatus[i] = depIndex;
			}
		}
	}

	@VisibleForTesting
	public ImmutableList<ChunkStatus> getDependencies() {
		return dependencies;
	}

	public int size() {
		return dependencies.size();
	}

	public int getAdditionalLevel(ChunkStatus status) {
		int index = status.getIndex();
		if (index >= additionalLevelsByStatus.length) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Requesting a ChunkStatus(%s) outside of dependency range(%s)",
							status,
							dependencies
					)
			);
		}

		return additionalLevelsByStatus[index];
	}

	public int getMaxLevel() {
		return Math.max(0, dependencies.size() - 1);
	}

	public ChunkStatus get(int index) {
		return dependencies.get(index);
	}

	@Override
	public String toString() {
		return dependencies.toString();
	}
}
