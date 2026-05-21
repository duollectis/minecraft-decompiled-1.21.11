package net.minecraft.client.gui.cursor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code StandardCursors}.
 */
public class StandardCursors {

	public static final Cursor ARROW = Cursor.createStandard(221185, "arrow", Cursor.DEFAULT);
	public static final Cursor IBEAM = Cursor.createStandard(221186, "ibeam", Cursor.DEFAULT);
	public static final Cursor CROSSHAIR = Cursor.createStandard(221187, "crosshair", Cursor.DEFAULT);
	public static final Cursor POINTING_HAND = Cursor.createStandard(221188, "pointing_hand", Cursor.DEFAULT);
	public static final Cursor RESIZE_NS = Cursor.createStandard(221190, "resize_ns", Cursor.DEFAULT);
	public static final Cursor RESIZE_EW = Cursor.createStandard(221189, "resize_ew", Cursor.DEFAULT);
	public static final Cursor RESIZE_ALL = Cursor.createStandard(221193, "resize_all", Cursor.DEFAULT);
	public static final Cursor NOT_ALLOWED = Cursor.createStandard(221194, "not_allowed", Cursor.DEFAULT);
}
