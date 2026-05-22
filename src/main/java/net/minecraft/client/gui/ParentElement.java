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

/**
 * Расширение {@link Element} для контейнерных элементов GUI, управляющих дочерними элементами.
 * Реализует делегирование событий мыши и клавиатуры к сфокусированному/наведённому дочернему элементу.
 */
@Environment(EnvType.CLIENT)
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
		Optional<Element> optional = hoveredElement(click.x(), click.y());

		if (optional.isEmpty()) {
			return false;
		}

		Element element = optional.get();

		if (element.mouseClicked(click, doubled) && element.isClickable()) {
			setFocused(element);

			if (click.button() == 0) {
				setDragging(true);
			}
		}

		return true;
	}

	@Override
	default boolean mouseReleased(Click click) {
		if (click.button() == 0 && isDragging()) {
			setDragging(false);

			if (getFocused() != null) {
				return getFocused().mouseReleased(click);
			}
		}

		return false;
	}

	@Override
	default boolean mouseDragged(Click click, double offsetX, double offsetY) {
		return getFocused() != null && isDragging() && click.button() == 0
			? getFocused().mouseDragged(click, offsetX, offsetY)
			: false;
	}

	boolean isDragging();

	void setDragging(boolean dragging);

	@Override
	default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return hoveredElement(mouseX, mouseY)
			.filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
			.isPresent();
	}

	@Override
	default boolean keyPressed(KeyInput input) {
		return getFocused() != null && getFocused().keyPressed(input);
	}

	@Override
	default boolean keyReleased(KeyInput input) {
		return getFocused() != null && getFocused().keyReleased(input);
	}

	@Override
	default boolean charTyped(CharInput input) {
		return getFocused() != null && getFocused().charTyped(input);
	}

	@Nullable Element getFocused();

	void setFocused(@Nullable Element focused);

	@Override
	default void setFocused(boolean focused) {
	}

	@Override
	default boolean isFocused() {
		return getFocused() != null;
	}

	@Override
	default @Nullable GuiNavigationPath getFocusedPath() {
		Element element = getFocused();
		return element != null ? GuiNavigationPath.of(this, element.getFocusedPath()) : null;
	}

	@Override
	default @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
		Element focused = getFocused();

		if (focused != null) {
			GuiNavigationPath path = focused.getNavigationPath(navigation);

			if (path != null) {
				return GuiNavigationPath.of(this, path);
			}
		}

		if (navigation instanceof GuiNavigation.Tab tab) {
			return computeNavigationPath(tab);
		}

		return navigation instanceof GuiNavigation.Arrow arrow ? computeNavigationPath(arrow) : null;
	}

	private @Nullable GuiNavigationPath computeNavigationPath(GuiNavigation.Tab navigation) {
		boolean forward = navigation.forward();
		Element currentFocused = getFocused();
		List<? extends Element> sorted = new ArrayList<>(children());
		sorted.sort(Comparator.comparingInt(Element::getNavigationOrder));

		int currentIndex = sorted.indexOf(currentFocused);
		int startIndex;

		if (currentFocused != null && currentIndex >= 0) {
			startIndex = currentIndex + (forward ? 1 : 0);
		} else if (forward) {
			startIndex = 0;
		} else {
			startIndex = sorted.size();
		}

		ListIterator<? extends Element> iterator = sorted.listIterator(startIndex);
		BooleanSupplier hasMore = forward ? iterator::hasNext : iterator::hasPrevious;
		Supplier<? extends Element> next = forward ? iterator::next : iterator::previous;

		while (hasMore.getAsBoolean()) {
			GuiNavigationPath path = next.get().getNavigationPath(navigation);

			if (path != null) {
				return GuiNavigationPath.of(this, path);
			}
		}

		return null;
	}

	private @Nullable GuiNavigationPath computeNavigationPath(GuiNavigation.Arrow navigation) {
		Element currentFocused = getFocused();

		if (currentFocused == null) {
			NavigationDirection direction = navigation.direction();
			ScreenRect border = getBorder(direction.getOpposite());
			return GuiNavigationPath.of(this, computeChildPath(border, direction, null, navigation));
		}

		ScreenRect focusRect = currentFocused.getNavigationFocus();
		return GuiNavigationPath.of(this, computeChildPath(focusRect, navigation.direction(), currentFocused, navigation));
	}

	private @Nullable GuiNavigationPath computeChildPath(
		ScreenRect focus,
		NavigationDirection direction,
		@Nullable Element focused,
		GuiNavigation navigation
	) {
		NavigationAxis axis = direction.getAxis();
		NavigationAxis crossAxis = axis.getOther();
		NavigationDirection crossDirection = crossAxis.getPositiveDirection();
		int boundaryCoord = focus.getBoundingCoordinate(direction.getOpposite());
		List<Element> candidates = new ArrayList<>();

		for (Element element : children()) {
			if (element == focused) {
				continue;
			}

			ScreenRect elementRect = element.getNavigationFocus();

			if (elementRect.overlaps(focus, crossAxis)) {
				int elementCoord = elementRect.getBoundingCoordinate(direction.getOpposite());

				if (direction.isAfter(elementCoord, boundaryCoord)) {
					candidates.add(element);
				} else if (elementCoord == boundaryCoord && direction.isAfter(
					elementRect.getBoundingCoordinate(direction),
					focus.getBoundingCoordinate(direction)
				)) {
					candidates.add(element);
				}
			}
		}

		Comparator<Element> primaryOrder = Comparator.comparing(
			e -> e.getNavigationFocus().getBoundingCoordinate(direction.getOpposite()),
			direction.getComparator()
		);
		Comparator<Element> secondaryOrder = Comparator.comparing(
			e -> e.getNavigationFocus().getBoundingCoordinate(crossDirection.getOpposite()),
			crossDirection.getComparator()
		);
		candidates.sort(primaryOrder.thenComparing(secondaryOrder));

		for (Element candidate : candidates) {
			GuiNavigationPath path = candidate.getNavigationPath(navigation);

			if (path != null) {
				return path;
			}
		}

		return computeInitialChildPath(focus, direction, focused, navigation);
	}

	private @Nullable GuiNavigationPath computeInitialChildPath(
		ScreenRect focus,
		NavigationDirection direction,
		@Nullable Element focused,
		GuiNavigation navigation
	) {
		NavigationAxis axis = direction.getAxis();
		NavigationAxis crossAxis = axis.getOther();
		ScreenPos originPos = ScreenPos.of(axis, focus.getBoundingCoordinate(direction), focus.getCenter(crossAxis));
		List<Pair<Element, Long>> candidates = new ArrayList<>();

		for (Element element : children()) {
			if (element == focused) {
				continue;
			}

			ScreenRect elementRect = element.getNavigationFocus();
			ScreenPos elementPos = ScreenPos.of(
				axis,
				elementRect.getBoundingCoordinate(direction.getOpposite()),
				elementRect.getCenter(crossAxis)
			);

			if (direction.isAfter(elementPos.getComponent(axis), originPos.getComponent(axis))) {
				long distSq = Vector2i.distanceSquared(originPos.x(), originPos.y(), elementPos.x(), elementPos.y());
				candidates.add(Pair.of(element, distSq));
			}
		}

		candidates.sort(Comparator.comparingDouble(Pair::getSecond));

		for (Pair<Element, Long> pair : candidates) {
			GuiNavigationPath path = pair.getFirst().getNavigationPath(navigation);

			if (path != null) {
				return path;
			}
		}

		return null;
	}
}
