package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Фаза работы блока сенсора скалка (Sculk Sensor).
 * Определяет текущее состояние цикла обнаружения вибраций.
 */
public enum SculkSensorPhase implements StringIdentifiable {
	/** Сенсор ожидает вибрации и готов к срабатыванию. */
	INACTIVE("inactive"),
	/** Сенсор обнаружил вибрацию и передаёт редстоун-сигнал. */
	ACTIVE("active"),
	/** Сенсор завершил передачу сигнала и восстанавливается. */
	COOLDOWN("cooldown");

	private final String name;

	SculkSensorPhase(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String asString() {
		return name;
	}
}
