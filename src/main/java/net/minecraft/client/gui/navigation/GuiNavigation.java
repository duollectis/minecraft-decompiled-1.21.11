package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code GuiNavigation}.
 */
public interface GuiNavigation {

	NavigationDirection getDirection();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Arrow}.
	 */
	public record Arrow(NavigationDirection direction) implements GuiNavigation {

		@Override
		public NavigationDirection getDirection() {
			return this.direction.getAxis() == NavigationAxis.VERTICAL ? this.direction : NavigationDirection.DOWN;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Down}.
	 */
	public static class Down implements GuiNavigation {

		@Override
		public NavigationDirection getDirection() {
			return NavigationDirection.DOWN;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Tab}.
	 */
	public record Tab(boolean forward) implements GuiNavigation {

		@Override
		public NavigationDirection getDirection() {
			return this.forward ? NavigationDirection.DOWN : NavigationDirection.UP;
		}
	}
}
