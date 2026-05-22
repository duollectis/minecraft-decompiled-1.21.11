package net.minecraft.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.AssetInfo;

import java.util.Optional;

/**
 * Описывает визуальное представление достижения: иконку, заголовок, описание,
 * фоновое изображение, тип рамки и флаги видимости.
 * <p>
 * Позиция {@code x}/{@code y} на дереве достижений задаётся отдельно через {@link #setPos}.
 */
public class AdvancementDisplay {

	// Битовые флаги для сериализации в пакет
	private static final int FLAG_HAS_BACKGROUND = 1;
	private static final int FLAG_SHOW_TOAST = 2;
	private static final int FLAG_HIDDEN = 4;

	public static final Codec<AdvancementDisplay> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			ItemStack.VALIDATED_CODEC.fieldOf("icon").forGetter(AdvancementDisplay::getIcon),
			TextCodecs.CODEC.fieldOf("title").forGetter(AdvancementDisplay::getTitle),
			TextCodecs.CODEC.fieldOf("description").forGetter(AdvancementDisplay::getDescription),
			AssetInfo.TextureAssetInfo.CODEC.optionalFieldOf("background").forGetter(AdvancementDisplay::getBackground),
			AdvancementFrame.CODEC.optionalFieldOf("frame", AdvancementFrame.TASK).forGetter(AdvancementDisplay::getFrame),
			Codec.BOOL.optionalFieldOf("show_toast", true).forGetter(AdvancementDisplay::shouldShowToast),
			Codec.BOOL.optionalFieldOf("announce_to_chat", true).forGetter(AdvancementDisplay::shouldAnnounceToChat),
			Codec.BOOL.optionalFieldOf("hidden", false).forGetter(AdvancementDisplay::isHidden)
		).apply(instance, AdvancementDisplay::new)
	);

	public static final PacketCodec<RegistryByteBuf, AdvancementDisplay> PACKET_CODEC =
		PacketCodec.of(AdvancementDisplay::toPacket, AdvancementDisplay::fromPacket);

	private final Text title;
	private final Text description;
	private final ItemStack icon;
	private final Optional<AssetInfo.TextureAssetInfo> background;
	private final AdvancementFrame frame;
	private final boolean showToast;
	private final boolean announceToChat;
	private final boolean hidden;
	private float x;
	private float y;

	public AdvancementDisplay(
		ItemStack icon,
		Text title,
		Text description,
		Optional<AssetInfo.TextureAssetInfo> background,
		AdvancementFrame frame,
		boolean showToast,
		boolean announceToChat,
		boolean hidden
	) {
		this.title = title;
		this.description = description;
		this.icon = icon;
		this.background = background;
		this.frame = frame;
		this.showToast = showToast;
		this.announceToChat = announceToChat;
		this.hidden = hidden;
	}

	public void setPos(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Text getTitle() {
		return title;
	}

	public Text getDescription() {
		return description;
	}

	public ItemStack getIcon() {
		return icon;
	}

	public Optional<AssetInfo.TextureAssetInfo> getBackground() {
		return background;
	}

	public AdvancementFrame getFrame() {
		return frame;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public boolean shouldShowToast() {
		return showToast;
	}

	public boolean shouldAnnounceToChat() {
		return announceToChat;
	}

	public boolean isHidden() {
		return hidden;
	}

	private void toPacket(RegistryByteBuf buf) {
		TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.encode(buf, title);
		TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.encode(buf, description);
		ItemStack.PACKET_CODEC.encode(buf, icon);
		buf.writeEnumConstant(frame);

		int flags = 0;
		if (background.isPresent()) {
			flags |= FLAG_HAS_BACKGROUND;
		}
		if (showToast) {
			flags |= FLAG_SHOW_TOAST;
		}
		if (hidden) {
			flags |= FLAG_HIDDEN;
		}

		buf.writeInt(flags);
		background.map(AssetInfo::id).ifPresent(buf::writeIdentifier);
		buf.writeFloat(x);
		buf.writeFloat(y);
	}

	private static AdvancementDisplay fromPacket(RegistryByteBuf buf) {
		Text title = TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.decode(buf);
		Text description = TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.decode(buf);
		ItemStack icon = ItemStack.PACKET_CODEC.decode(buf);
		AdvancementFrame frame = buf.readEnumConstant(AdvancementFrame.class);
		int flags = buf.readInt();

		Optional<AssetInfo.TextureAssetInfo> background = (flags & FLAG_HAS_BACKGROUND) != 0
			? Optional.of(new AssetInfo.TextureAssetInfo(buf.readIdentifier()))
			: Optional.empty();

		boolean showToast = (flags & FLAG_SHOW_TOAST) != 0;
		boolean hidden = (flags & FLAG_HIDDEN) != 0;

		AdvancementDisplay display = new AdvancementDisplay(icon, title, description, background, frame, showToast, false, hidden);
		display.setPos(buf.readFloat(), buf.readFloat());
		return display;
	}
}
