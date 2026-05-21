package net.minecraft.scoreboard.number;

import net.minecraft.text.MutableText;

/**
 * {@code NumberFormat}.
 */
public interface NumberFormat {

	MutableText format(int number);

	NumberFormatType<? extends NumberFormat> getType();
}
