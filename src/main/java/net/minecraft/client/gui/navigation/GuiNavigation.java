package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Базовый интерфейс навигации по GUI с помощью клавиатуры или мыши.
 * Каждая реализация описывает конкретный тип навигационного события.
 */
@Environment(EnvType.CLIENT)
public interface GuiNavigation {

	NavigationDirection getDirection();

	/**
	 * Навигация стрелочными клавишами. Вертикальная ось используется напрямую,
	 * горизонтальная — принудительно заменяется на {@link NavigationDirection#DOWN}.
	 */
	@Environment(EnvType.CLIENT)
	public record Arrow(NavigationDirection direction) implements GuiNavigation {

		@Override
		public NavigationDirection getDirection() {
			return direction.getAxis() == NavigationAxis.VERTICAL ? direction : NavigationDirection.DOWN;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Down implements GuiNavigation {

		@Override
		public NavigationDirection getDirection() {
			return NavigationDirection.DOWN;
		}
	}

	@Environment(EnvType.CLIENT)
	public record Tab(boolean forward) implements GuiNavigation {

		@Override
		public NavigationDirection getDirection() {
			return forward ? NavigationDirection.DOWN : NavigationDirection.UP;
		}
	}
}
