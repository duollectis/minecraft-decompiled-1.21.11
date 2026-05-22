package net.minecraft.entity.data;

/**
 * Дескриптор отслеживаемого поля сущности.
 * Хранит числовой идентификатор поля и его обработчик сериализации.
 * Равенство определяется исключительно по {@code id}.
 */
public record TrackedData<T>(int id, TrackedDataHandler<T> dataType) {

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TrackedData<?> other = (TrackedData<?>) o;
		return id == other.id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return "<entity data: " + id + ">";
	}
}
