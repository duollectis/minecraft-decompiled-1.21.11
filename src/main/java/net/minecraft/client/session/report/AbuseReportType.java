package net.minecraft.client.session.report;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code AbuseReportType}.
 */
public enum AbuseReportType {
	CHAT("chat"),
	SKIN("skin"),
	USERNAME("username");

	private final String name;

	private AbuseReportType(final String name) {
		this.name = name.toUpperCase(Locale.ROOT);
	}

	public String getName() {
		return this.name;
	}
}
