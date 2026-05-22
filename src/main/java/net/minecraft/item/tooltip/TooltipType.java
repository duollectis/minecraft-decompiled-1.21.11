package net.minecraft.item.tooltip;

/**
 * Тип подсказки предмета, определяющий режим отображения тултипа:
 * базовый, расширенный (с техническими данными) или творческий.
 */
public interface TooltipType {

	TooltipType.Default BASIC = new TooltipType.Default(false, false);

	TooltipType.Default ADVANCED = new TooltipType.Default(true, false);

	boolean isAdvanced();

	boolean isCreative();

	/**
	 * Стандартная реализация типа тултипа с флагами расширенного и творческого режимов.
	 */
	public record Default(boolean advanced, boolean creative) implements TooltipType {

		@Override
		public boolean isAdvanced() {
			return this.advanced;
		}

		@Override
		public boolean isCreative() {
			return this.creative;
		}

		public TooltipType.Default withCreative() {
			return new TooltipType.Default(this.advanced, true);
		}
	}
}
