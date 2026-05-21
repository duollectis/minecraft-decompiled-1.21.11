package net.minecraft.component.type;

import net.minecraft.text.RawFilteredPair;

import java.util.List;

/**
 * {@code BookContent}.
 */
public interface BookContent<T, C> {

	List<RawFilteredPair<T>> pages();

	C withPages(List<RawFilteredPair<T>> pages);
}
