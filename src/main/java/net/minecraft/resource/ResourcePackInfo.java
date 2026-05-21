package net.minecraft.resource;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import java.util.Optional;

/**
 * {@code ResourcePackInfo}.
 */
public record ResourcePackInfo(
		String id,
		Text title,
		ResourcePackSource source,
		Optional<VersionedIdentifier> knownPackInfo
) {

	public Text getInformationText(boolean enabled, Text description) {
		return Texts.bracketed(this.source.decorate(Text.literal(this.id)))
		            .styled(
				            style -> style.withColor(enabled ? Formatting.GREEN : Formatting.RED)
				                          .withInsertion(StringArgumentType.escapeIfRequired(this.id))
				                          .withHoverEvent(new HoverEvent.ShowText(Text
						                          .empty()
						                          .append(this.title)
						                          .append("\n")
						                          .append(description)))
		            );
	}
}
