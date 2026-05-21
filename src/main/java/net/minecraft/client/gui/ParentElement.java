package net.minecraft.client.gui;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code ParentElement}.
 */
public interface ParentElement extends Element {

	List<? extends Element> children();

	default Optional<Element> hoveredElement(double mouseX, double mouseY) {
		for (Element element : this.children()) {
			if (element.isMouseOver(mouseX, mouseY)) {
				return Optional.of(element);
			}
		}

		return Optional.empty();
	}

	@Override
	default boolean mouseClicked(Click click, boolean doubled) {
		Optional<Element> optional = this.hoveredElement(click.x(), click.y());
		if (optional.isEmpty()) {
			return false;
		}
		else {
			Element element = optional.get();
			if (element.mouseClicked(click, doubled) && element.isClickable()) {
				this.setFocused(element);
				if (click.button() == 0) {
					this.setDragging(true);
				}
			}

			return true;
		}
	}

	@Override
	default boolean mouseReleased(Click click) {
		if (click.button() == 0 && this.isDragging()) {
			this.setDragging(false);
			if (this.getFocused() != null) {
				return this.getFocused().mouseReleased(click);
			}
		}

		return false;
	}

	@Override
	default boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return this.getFocused() != null && this.isDragging() && click.button() == 0 ? this
		                                                                               .getFocused()
		                                                                               .mouseDragged(
				                                                                               click,
				                                                                               offsetX,
				                                                                               offsetY
		                                                                               ) : false;
	}

	boolean isDragging();

	void setDragging(boolean dragging);

	@Override
	default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return this
				.hoveredElement(mouseX, mouseY)
				.filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
				.isPresent();
	}

	@Override
	default boolean keyPressed(KeyInput input) {
		return this.getFocused() != null && this.getFocused().keyPressed(input);
	}

	@Override
	default boolean keyReleased(KeyInput input) {
		return this.getFocused() != null && this.getFocused().keyReleased(input);
	}

	@Override
	default boolean charTyped(CharInput input) {
		return this.getFocused() != null && this.getFocused().charTyped(input);
	}

	@Nullable Element getFocused();

	void setFocused(@Nullable Element focused);

	@Override
	default void setFocused(boolean focused) {
	}

	@Override
	default boolean isFocused() {
		return this.getFocused() != null;
	}

	@Override
	default @Nullable GuiNavigationPath getFocusedPath() {
		Element element = this.getFocused();
		return element != null ? GuiNavigationPath.of(this, element.getFocusedPath()) : null;
	}

	@Override
	default @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		Element element = this.getFocused();
		if (element != null) {
			GuiNavigationPath guiNavigationPath = element.getNavigationPath(navigation);
			if (guiNavigationPath != null) {
				return GuiNavigationPath.of(this, guiNavigationPath);
			}
		}

		if (navigation instanceof GuiNavigation.Tab tab) {
			return this.computeNavigationPath(tab);
		}
		else {
			return navigation instanceof GuiNavigation.Arrow arrow ? this.computeNavigationPath(arrow) : null;
		}
	}

	private @Nullable GuiNavigationPath computeNavigationPath(GuiNavigation.Tab navigation) {
		boolean bl = navigation.forward();
		Element element = this.getFocused();
		List<? extends Element> list = new ArrayList<>(this.children());
		Collections.sort(list, Comparator.comparingInt(child -> child.getNavigationOrder()));
		int i = list.indexOf(element);
		int j;
		if (element != null && i >= 0) {
			j = i + (bl ? 1 : 0);
		}
		else if (bl) {
			j = 0;
		}
		else {
			j = list.size();
		}

		ListIterator<? extends Element> listIterator = list.listIterator(j);
		BooleanSupplier booleanSupplier = bl ? listIterator::hasNext : listIterator::hasPrevious;
		Supplier<? extends Element> supplier = bl ? listIterator::next : listIterator::previous;

		while (booleanSupplier.getAsBoolean()) {
			Element element2 = supplier.get();
			GuiNavigationPath guiNavigationPath = element2.getNavigationPath(navigation);
			if (guiNavigationPath != null) {
				return GuiNavigationPath.of(this, guiNavigationPath);
			}
		}

		return null;
	}

	private @Nullable GuiNavigationPath computeNavigationPath(GuiNavigation.Arrow navigation) {
		Element element = this.getFocused();
		if (element == null) {
			NavigationDirection navigationDirection = navigation.direction();
			ScreenRect screenRect = this.getBorder(navigationDirection.getOpposite());
			return GuiNavigationPath.of(this, this.computeChildPath(screenRect, navigationDirection, null, navigation));
		}
		else {
			ScreenRect screenRect2 = element.getNavigationFocus();
			return GuiNavigationPath.of(
					this,
					this.computeChildPath(screenRect2, navigation.direction(), element, navigation)
			);
		}
	}

	private @Nullable GuiNavigationPath computeChildPath(
			ScreenRect focus,
			NavigationDirection direction,
			@Nullable Element focused,
			GuiNavigation navigation
	) {
		NavigationAxis navigationAxis = direction.getAxis();
		NavigationAxis navigationAxis2 = navigationAxis.getOther();
		NavigationDirection navigationDirection = navigationAxis2.getPositiveDirection();
		int i = focus.getBoundingCoordinate(direction.getOpposite());
		List<Element> list = new ArrayList<>();

		for (Element element : this.children()) {
			if (element != focused) {
				ScreenRect screenRect = element.getNavigationFocus();
				if (screenRect.overlaps(focus, navigationAxis2)) {
					int j = screenRect.getBoundingCoordinate(direction.getOpposite());
					if (direction.isAfter(j, i)) {
						list.add(element);
					}
					else if (j == i && direction.isAfter(
							screenRect.getBoundingCoordinate(direction),
							focus.getBoundingCoordinate(direction)
					)) {
						list.add(element);
					}
				}
			}
		}

		Comparator<Element> comparator = Comparator.comparing(
				elementx -> elementx.getNavigationFocus().getBoundingCoordinate(direction.getOpposite()),
				direction.getComparator()
		);
		Comparator<Element> comparator2 = Comparator.comparing(
				elementx -> elementx.getNavigationFocus().getBoundingCoordinate(navigationDirection.getOpposite()),
				navigationDirection.getComparator()
		);
		list.sort(comparator.thenComparing(comparator2));

		for (Element element2 : list) {
			GuiNavigationPath guiNavigationPath = element2.getNavigationPath(navigation);
			if (guiNavigationPath != null) {
				return guiNavigationPath;
			}
		}

		return this.computeInitialChildPath(focus, direction, focused, navigation);
	}

	private @Nullable GuiNavigationPath computeInitialChildPath(
			ScreenRect focus, NavigationDirection direction, @Nullable Element focused, GuiNavigation navigation
	) {
		NavigationAxis navigationAxis = direction.getAxis();
		NavigationAxis navigationAxis2 = navigationAxis.getOther();
		List<Pair<Element, Long>> list = new ArrayList<>();
		ScreenPos
				screenPos =
				ScreenPos.of(navigationAxis, focus.getBoundingCoordinate(direction), focus.getCenter(navigationAxis2));

		for (Element element : this.children()) {
			if (element != focused) {
				ScreenRect screenRect = element.getNavigationFocus();
				ScreenPos screenPos2 = ScreenPos.of(
						navigationAxis,
						screenRect.getBoundingCoordinate(direction.getOpposite()),
						screenRect.getCenter(navigationAxis2)
				);
				if (direction.isAfter(
						screenPos2.getComponent(navigationAxis),
						screenPos.getComponent(navigationAxis)
				)) {
					long l = Vector2i.distanceSquared(screenPos.x(), screenPos.y(), screenPos2.x(), screenPos2.y());
					list.add(Pair.of(element, l));
				}
			}
		}

		list.sort(Comparator.comparingDouble(Pair::getSecond));

		for (Pair<Element, Long> pair : list) {
			GuiNavigationPath guiNavigationPath = ((Element) pair.getFirst()).getNavigationPath(navigation);
			if (guiNavigationPath != null) {
				return guiNavigationPath;
			}
		}

		return null;
	}
}
