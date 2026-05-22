package net.minecraft.client.toast;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Системное уведомление (toast) для отображения коротких сообщений об ошибках,
 * предупреждениях и событиях (сохранение мира, нехватка диска, сбой чанков и т.д.).
 *
 * <p>Поддерживает обновление содержимого без пересоздания через {@link #setContent},
 * а также принудительное скрытие через {@link #hide()}.
 */
@Environment(EnvType.CLIENT)
public class SystemToast implements Toast {

	private static final Identifier TEXTURE = Identifier.ofVanilla("toast/system");
	private static final int MIN_WIDTH = 200;
	private static final int LINE_HEIGHT = 12;
	private static final int PADDING_Y = 10;
	private static final long DEFAULT_DISPLAY_DURATION_MS = 5000L;

	private final Type type;
	private Text title;
	private List<OrderedText> lines;
	private long startTime;
	private boolean justUpdated;
	private final int width;
	private boolean hidden;
	private Toast.Visibility visibility = Toast.Visibility.HIDE;

	public SystemToast(Type type, Text title, @Nullable Text description) {
		this(
			type,
			title,
			getTextAsList(description),
			Math.max(
				Toast.BASE_WIDTH,
				30 + Math.max(
					MinecraftClient.getInstance().textRenderer.getWidth(title),
					description == null ? 0 : MinecraftClient.getInstance().textRenderer.getWidth(description)
				)
			)
		);
	}

	/**
	 * Создаёт toast с переносом строк описания по ширине {@link #MIN_WIDTH}.
	 * Ширина toast подстраивается под самую длинную строку.
	 */
	public static SystemToast create(MinecraftClient client, Type type, Text title, Text description) {
		TextRenderer textRenderer = client.textRenderer;
		List<OrderedText> wrappedLines = textRenderer.wrapLines(description, MIN_WIDTH);
		int maxLineWidth = Math.max(MIN_WIDTH, wrappedLines.stream().mapToInt(textRenderer::getWidth).max().orElse(MIN_WIDTH));
		return new SystemToast(type, title, wrappedLines, maxLineWidth + 30);
	}

	private SystemToast(Type type, Text title, List<OrderedText> lines, int width) {
		this.type = type;
		this.title = title;
		this.lines = lines;
		this.width = width;
	}

	private static ImmutableList<OrderedText> getTextAsList(@Nullable Text text) {
		return text == null ? ImmutableList.of() : ImmutableList.of(text.asOrderedText());
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return 20 + Math.max(lines.size(), 1) * LINE_HEIGHT;
	}

	public void hide() {
		hidden = true;
	}

	@Override
	public Toast.Visibility getVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long time) {
		if (justUpdated) {
			startTime = time;
			justUpdated = false;
		}

		double displayDuration = type.displayDuration * manager.getNotificationDisplayTimeMultiplier();
		long elapsed = time - startTime;
		visibility = !hidden && elapsed < displayDuration
			? Toast.Visibility.SHOW
			: Toast.Visibility.HIDE;
	}

	@Override
	public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, getWidth(), getHeight());

		if (lines.isEmpty()) {
			context.drawText(textRenderer, title, 18, LINE_HEIGHT, -256, false);
		} else {
			context.drawText(textRenderer, title, 18, 7, -256, false);

			for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
				context.drawText(textRenderer, lines.get(lineIndex), 18, 18 + lineIndex * LINE_HEIGHT, -1, false);
			}
		}
	}

	public void setContent(Text title, @Nullable Text description) {
		this.title = title;
		lines = getTextAsList(description);
		justUpdated = true;
	}

	public Type getType() {
		return type;
	}

	public static void add(ToastManager manager, Type type, Text title, @Nullable Text description) {
		manager.add(new SystemToast(type, title, description));
	}

	/**
	 * Показывает toast данного типа: если уже отображается — обновляет содержимое,
	 * иначе добавляет новый в очередь.
	 */
	public static void show(ToastManager manager, Type type, Text title, @Nullable Text description) {
		SystemToast existing = manager.getToast(SystemToast.class, type);

		if (existing == null) {
			add(manager, type, title, description);
		} else {
			existing.setContent(title, description);
		}
	}

	public static void hide(ToastManager manager, Type type) {
		SystemToast existing = manager.getToast(SystemToast.class, type);

		if (existing != null) {
			existing.hide();
		}
	}

	public static void addWorldAccessFailureToast(MinecraftClient client, String worldName) {
		add(
			client.getToastManager(),
			Type.WORLD_ACCESS_FAILURE,
			Text.translatable("selectWorld.access_failure"),
			Text.literal(worldName)
		);
	}

	public static void addWorldDeleteFailureToast(MinecraftClient client, String worldName) {
		add(
			client.getToastManager(),
			Type.WORLD_ACCESS_FAILURE,
			Text.translatable("selectWorld.delete_failure"),
			Text.literal(worldName)
		);
	}

	public static void addPackCopyFailure(MinecraftClient client, String directory) {
		add(
			client.getToastManager(),
			Type.PACK_COPY_FAILURE,
			Text.translatable("pack.copyFailure"),
			Text.literal(directory)
		);
	}

	public static void addFileDropFailure(MinecraftClient client, int count) {
		add(
			client.getToastManager(),
			Type.FILE_DROP_FAILURE,
			Text.translatable("gui.fileDropFailure.title"),
			Text.translatable("gui.fileDropFailure.detail", count)
		);
	}

	public static void addLowDiskSpace(MinecraftClient client) {
		show(
			client.getToastManager(),
			Type.LOW_DISK_SPACE,
			Text.translatable("chunk.toast.lowDiskSpace"),
			Text.translatable("chunk.toast.lowDiskSpace.description")
		);
	}

	public static void addChunkLoadFailure(MinecraftClient client, ChunkPos pos) {
		show(
			client.getToastManager(),
			Type.CHUNK_LOAD_FAILURE,
			Text.translatable("chunk.toast.loadFailure", Text.of(pos)).formatted(Formatting.RED),
			Text.translatable("chunk.toast.checkLog")
		);
	}

	public static void addChunkSaveFailure(MinecraftClient client, ChunkPos pos) {
		show(
			client.getToastManager(),
			Type.CHUNK_SAVE_FAILURE,
			Text.translatable("chunk.toast.saveFailure", Text.of(pos)).formatted(Formatting.RED),
			Text.translatable("chunk.toast.checkLog")
		);
	}

	/** Тип системного toast, используемый для дедупликации в {@link ToastManager}. */
	@Environment(EnvType.CLIENT)
	public static class Type {

		public static final Type NARRATOR_TOGGLE = new Type();
		public static final Type WORLD_BACKUP = new Type();
		public static final Type PACK_LOAD_FAILURE = new Type();
		public static final Type WORLD_ACCESS_FAILURE = new Type();
		public static final Type PACK_COPY_FAILURE = new Type();
		public static final Type FILE_DROP_FAILURE = new Type();
		public static final Type PERIODIC_NOTIFICATION = new Type();
		public static final Type LOW_DISK_SPACE = new Type(10000L);
		public static final Type CHUNK_LOAD_FAILURE = new Type();
		public static final Type CHUNK_SAVE_FAILURE = new Type();
		public static final Type UNSECURE_SERVER_WARNING = new Type(10000L);

		final long displayDuration;

		public Type(long displayDuration) {
			this.displayDuration = displayDuration;
		}

		public Type() {
			this(DEFAULT_DISPLAY_DURATION_MS);
		}
	}
}
