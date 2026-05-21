package net.minecraft.data.tag.vanilla;

import net.minecraft.data.DataOutput;
import net.minecraft.data.tag.SimpleTagProvider;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.DialogTags;

import java.util.concurrent.CompletableFuture;

/**
 * {@code VanillaDialogTagProvider}.
 */
public class VanillaDialogTagProvider extends SimpleTagProvider<Dialog> {

	public VanillaDialogTagProvider(
			DataOutput output,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		super(output, RegistryKeys.DIALOG, registriesFuture);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup registries) {
		this.builder(DialogTags.PAUSE_SCREEN_ADDITIONS);
		this.builder(DialogTags.QUICK_ACTIONS);
	}
}
