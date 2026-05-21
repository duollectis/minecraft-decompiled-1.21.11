package net.minecraft.client.gui.hud;

import com.google.common.base.Strings;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.DataFixUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntry;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import net.minecraft.client.gui.hud.debug.chart.*;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.*;
import net.minecraft.util.profiler.log.DebugSampleType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkLoadMap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
/**
 * {@code DebugHud}.
 */
public class DebugHud {

	private static final float DEBUG_CROSSHAIR_SCALE = 0.01F;
	private static final int DEBUG_CROSSHAIR_INDEX_COUNT = 36;
	private static final int TEXT_LEFT_MARGIN = 2;
	private static final int TEXT_RIGHT_MARGIN = 2;
	private static final int TEXT_TOP_MARGIN = 2;
	private final MinecraftClient client;
	private final TextRenderer textRenderer;
	private final GpuBuffer debugCrosshairBuffer;
	private final RenderSystem.ShapeIndexBuffer
			debugCrosshairIndexBuffer =
			RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.LINES);
	private @Nullable ChunkPos pos;
	private @Nullable WorldChunk chunk;
	private @Nullable CompletableFuture<WorldChunk> chunkFuture;
	private boolean renderingChartVisible;
	private boolean renderingAndTickChartsVisible;
	private boolean packetSizeAndPingChartsVisible;
	private final MultiValueDebugSampleLogImpl frameNanosLog = new MultiValueDebugSampleLogImpl(1);
	private final MultiValueDebugSampleLogImpl
			tickNanosLog =
			new MultiValueDebugSampleLogImpl(ServerTickType.values().length);
	private final MultiValueDebugSampleLogImpl pingLog = new MultiValueDebugSampleLogImpl(1);
	private final MultiValueDebugSampleLogImpl packetSizeLog = new MultiValueDebugSampleLogImpl(1);
	private final Map<DebugSampleType, MultiValueDebugSampleLogImpl>
			receivedDebugSamples =
			Map.of(DebugSampleType.TICK_TIME, this.tickNanosLog);
	private final RenderingChart renderingChart;
	private final TickChart tickChart;
	private final PingChart pingChart;
	private final PacketSizeChart packetSizeChart;
	private final PieChart pieChart;

	public DebugHud(MinecraftClient client) {
		this.client = client;
		this.textRenderer = client.textRenderer;
		this.renderingChart = new RenderingChart(this.textRenderer, this.frameNanosLog);
		this.tickChart =
				new TickChart(
						this.textRenderer,
						this.tickNanosLog,
						() -> client.world == null ? 0.0F : client.world.getTickManager().getMillisPerTick()
				);
		this.pingChart = new PingChart(this.textRenderer, this.pingLog);
		this.packetSizeChart = new PacketSizeChart(this.textRenderer, this.packetSizeLog);
		this.pieChart = new PieChart(this.textRenderer);

		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH.getVertexSize() * 12 * 2)
		) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(
							bufferAllocator,
							VertexFormat.DrawMode.LINES,
							VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH
					);
			bufferBuilder.vertex(0.0F, 0.0F, 0.0F).color(-16777216).normal(1.0F, 0.0F, 0.0F).lineWidth(4.0F);
			bufferBuilder.vertex(1.0F, 0.0F, 0.0F).color(-16777216).normal(1.0F, 0.0F, 0.0F).lineWidth(4.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 0.0F).color(-16777216).normal(0.0F, 1.0F, 0.0F).lineWidth(4.0F);
			bufferBuilder.vertex(0.0F, 1.0F, 0.0F).color(-16777216).normal(0.0F, 1.0F, 0.0F).lineWidth(4.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 0.0F).color(-16777216).normal(0.0F, 0.0F, 1.0F).lineWidth(4.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 1.0F).color(-16777216).normal(0.0F, 0.0F, 1.0F).lineWidth(4.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 0.0F).color(-65536).normal(1.0F, 0.0F, 0.0F).lineWidth(2.0F);
			bufferBuilder.vertex(1.0F, 0.0F, 0.0F).color(-65536).normal(1.0F, 0.0F, 0.0F).lineWidth(2.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 0.0F).color(-16711936).normal(0.0F, 1.0F, 0.0F).lineWidth(2.0F);
			bufferBuilder.vertex(0.0F, 1.0F, 0.0F).color(-16711936).normal(0.0F, 1.0F, 0.0F).lineWidth(2.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 0.0F).color(-8421377).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
			bufferBuilder.vertex(0.0F, 0.0F, 1.0F).color(-8421377).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				this.debugCrosshairBuffer =
						RenderSystem
								.getDevice()
								.createBuffer(() -> "Crosshair vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
	}

	public void resetChunk() {
		this.chunkFuture = null;
		this.chunk = null;
	}

	public void render(DrawContext context) {
		GameOptions gameOptions = this.client.options;
		if (this.client.isFinishedLoading() && (!gameOptions.hudHidden || this.client.currentScreen != null)) {
			Collection<Identifier> collection = this.client.debugHudEntryList.getVisibleEntries();
			if (!collection.isEmpty()) {
				context.createNewRootLayer();
				Profiler profiler = Profilers.get();
				profiler.push("debug");
				ChunkPos chunkPos;
				if (this.client.getCameraEntity() != null && this.client.world != null) {
					BlockPos blockPos = this.client.getCameraEntity().getBlockPos();
					chunkPos = new ChunkPos(blockPos);
				}
				else {
					chunkPos = null;
				}

				if (!Objects.equals(this.pos, chunkPos)) {
					this.pos = chunkPos;
					this.resetChunk();
				}

				final List<String> list = new ArrayList<>();
				final List<String> list2 = new ArrayList<>();
				final Map<Identifier, Collection<String>> map = new LinkedHashMap<>();
				final List<String> list3 = new ArrayList<>();
				DebugHudLines debugHudLines = new DebugHudLines() {
					@Override
					public void addPriorityLine(String line) {
						if (list.size() > list2.size()) {
							list2.add(line);
						}
						else {
							list.add(line);
						}
					}

					@Override
					public void addLine(String line) {
						list3.add(line);
					}

					@Override
					public void addLinesToSection(Identifier sectionId, Collection<String> lines) {
						map.computeIfAbsent(sectionId, sectionLines -> new ArrayList<>()).addAll(lines);
					}

					@Override
					public void addLineToSection(Identifier sectionId, String line) {
						map.computeIfAbsent(sectionId, sectionLines -> new ArrayList<>()).add(line);
					}
				};
				World world = this.getWorld();

				for (Identifier identifier : collection) {
					DebugHudEntry debugHudEntry = DebugHudEntries.get(identifier);
					if (debugHudEntry != null) {
						debugHudEntry.render(debugHudLines, world, this.getClientChunk(), this.getChunk());
					}
				}

				if (!list.isEmpty()) {
					list.add("");
				}

				if (!list2.isEmpty()) {
					list2.add("");
				}

				if (!list3.isEmpty()) {
					int i = (list3.size() + 1) / 2;
					list.addAll(list3.subList(0, i));
					list2.addAll(list3.subList(i, list3.size()));
					list.add("");
					if (i < list3.size()) {
						list2.add("");
					}
				}

				List<Collection<String>> list4 = new ArrayList<>(map.values());
				if (!list4.isEmpty()) {
					int j = (list4.size() + 1) / 2;

					for (int k = 0; k < list4.size(); k++) {
						Collection<String> collection2 = list4.get(k);
						if (!collection2.isEmpty()) {
							if (k < j) {
								list.addAll(collection2);
								list.add("");
							}
							else {
								list2.addAll(collection2);
								list2.add("");
							}
						}
					}
				}

				if (this.client.debugHudEntryList.isF3Enabled()) {
					list.add("");
					boolean bl = this.client.getServer() != null;
					KeyBinding keyBinding = gameOptions.debugModifierKey;
					String string = keyBinding.getBoundKeyLocalizedText().getString();
					String string2 = "[" + (keyBinding.isUnbound() ? "" : string + "+");
					String
							string3 =
							string2 + gameOptions.debugProfilingChartKey.getBoundKeyLocalizedText().getString() + "]";
					String
							string4 =
							string2 + gameOptions.debugFpsChartsKey.getBoundKeyLocalizedText().getString() + "]";
					String
							string5 =
							string2 + gameOptions.debugNetworkChartsKey.getBoundKeyLocalizedText().getString() + "]";
					list.add(
							"Debug charts: "
									+ string3
									+ " Profiler "
									+ (this.renderingChartVisible ? "visible" : "hidden")
									+ "; "
									+ string4
									+ " "
									+ (bl ? "FPS + TPS " : "FPS ")
									+ (this.renderingAndTickChartsVisible ? "visible" : "hidden")
									+ "; "
									+ string5
									+ " "
									+ (!this.client.isInSingleplayer() ? "Bandwidth + Ping" : "Ping")
									+ (this.packetSizeAndPingChartsVisible ? " visible" : " hidden")
					);
					String string6 = string2 + gameOptions.debugOptionsKey.getBoundKeyLocalizedText().getString() + "]";
					list.add("To edit: press " + string6);
				}

				this.drawText(context, list, true);
				this.drawText(context, list2, false);
				context.createNewRootLayer();
				this.pieChart.setBottomMargin(10);
				if (this.shouldRenderTickCharts()) {
					int j = context.getScaledWindowWidth();
					int kx = j / 2;
					this.renderingChart.render(context, 0, this.renderingChart.getWidth(kx));
					if (this.tickNanosLog.getLength() > 0) {
						int l = this.tickChart.getWidth(kx);
						this.tickChart.render(context, j - l, l);
					}

					this.pieChart.setBottomMargin(this.tickChart.getHeight());
				}

				if (this.shouldShowPacketSizeAndPingCharts() && this.client.getNetworkHandler() != null) {
					int j = context.getScaledWindowWidth();
					int kx = j / 2;
					if (!this.client.isInSingleplayer()) {
						this.packetSizeChart.render(context, 0, this.packetSizeChart.getWidth(kx));
					}

					int l = this.pingChart.getWidth(kx);
					this.pingChart.render(context, j - l, l);
					this.pieChart.setBottomMargin(this.pingChart.getHeight());
				}

				if (this.client.debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
					IntegratedServer integratedServer = this.client.getServer();
					if (integratedServer != null && this.client.player != null) {
						ChunkLoadMap
								chunkLoadMap =
								integratedServer.createChunkLoadMap(16 + ChunkLevels.FULL_GENERATION_REQUIRED_LEVEL);
						chunkLoadMap.initSpawnPos(
								this.client.player.getEntityWorld().getRegistryKey(),
								this.client.player.getChunkPos()
						);
						LevelLoadingScreen.drawChunkMap(
								context,
								context.getScaledWindowWidth() / 2,
								context.getScaledWindowHeight() / 2,
								4,
								1,
								chunkLoadMap
						);
					}
				}

				try (ScopedProfiler scopedProfiler = profiler.scoped("profilerPie")) {
					this.pieChart.render(context);
				}

				profiler.pop();
			}
		}
	}

	private void drawText(DrawContext context, List<String> text, boolean left) {
		int i = 9;

		for (int j = 0; j < text.size(); j++) {
			String string = text.get(j);
			if (!Strings.isNullOrEmpty(string)) {
				int k = this.textRenderer.getWidth(string);
				int l = left ? 2 : context.getScaledWindowWidth() - 2 - k;
				int m = 2 + i * j;
				context.fill(l - 1, m - 1, l + k + 1, m + i - 1, -1873784752);
			}
		}

		for (int jx = 0; jx < text.size(); jx++) {
			String string = text.get(jx);
			if (!Strings.isNullOrEmpty(string)) {
				int k = this.textRenderer.getWidth(string);
				int l = left ? 2 : context.getScaledWindowWidth() - 2 - k;
				int m = 2 + i * jx;
				context.drawText(this.textRenderer, string, l, m, -2039584, false);
			}
		}
	}

	private @Nullable ServerWorld getServerWorld() {
		if (this.client.world == null) {
			return null;
		}
		else {
			IntegratedServer integratedServer = this.client.getServer();
			return integratedServer != null ? integratedServer.getWorld(this.client.world.getRegistryKey()) : null;
		}
	}

	private @Nullable World getWorld() {
		return this.client.world == null
		       ? null
		       : (World) DataFixUtils.orElse(
				       Optional
				       .ofNullable(this.client.getServer())
				       .flatMap(server -> Optional.ofNullable(server.getWorld(this.client.world.getRegistryKey()))),
				       this.client.world
		       );
	}

	private @Nullable WorldChunk getChunk() {
		if (this.client.world != null && this.pos != null) {
			if (this.chunkFuture == null) {
				ServerWorld serverWorld = this.getServerWorld();
				if (serverWorld == null) {
					return null;
				}

				this.chunkFuture = serverWorld.getChunkManager()
				                              .getChunkFutureSyncOnMainThread(
						                              this.pos.x,
						                              this.pos.z,
						                              ChunkStatus.FULL,
						                              false
				                              )
				                              .thenApply(chunk -> (WorldChunk) chunk.orElse(null));
			}

			return this.chunkFuture.getNow(null);
		}
		else {
			return null;
		}
	}

	private @Nullable WorldChunk getClientChunk() {
		if (this.client.world != null && this.pos != null) {
			if (this.chunk == null) {
				this.chunk = this.client.world.getChunk(this.pos.x, this.pos.z);
			}

			return this.chunk;
		}
		else {
			return null;
		}
	}

	public boolean shouldShowDebugHud() {
		DebugHudProfile debugHudProfile = this.client.debugHudEntryList;
		return (debugHudProfile.isF3Enabled() || !debugHudProfile.getVisibleEntries().isEmpty())
				&& (!this.client.options.hudHidden || this.client.currentScreen != null);
	}

	public boolean shouldShowRenderingChart() {
		return this.client.debugHudEntryList.isF3Enabled() && this.renderingChartVisible;
	}

	public boolean shouldShowPacketSizeAndPingCharts() {
		return this.client.debugHudEntryList.isF3Enabled() && this.packetSizeAndPingChartsVisible;
	}

	public boolean shouldRenderTickCharts() {
		return this.client.debugHudEntryList.isF3Enabled() && this.renderingAndTickChartsVisible;
	}

	public void togglePacketSizeAndPingCharts() {
		this.packetSizeAndPingChartsVisible =
				!this.client.debugHudEntryList.isF3Enabled() || !this.packetSizeAndPingChartsVisible;
		if (this.packetSizeAndPingChartsVisible) {
			this.client.debugHudEntryList.setF3Enabled(true);
			this.renderingAndTickChartsVisible = false;
		}
	}

	public void toggleRenderingAndTickCharts() {
		this.renderingAndTickChartsVisible =
				!this.client.debugHudEntryList.isF3Enabled() || !this.renderingAndTickChartsVisible;
		if (this.renderingAndTickChartsVisible) {
			this.client.debugHudEntryList.setF3Enabled(true);
			this.packetSizeAndPingChartsVisible = false;
		}
	}

	public void toggleRenderingChart() {
		this.renderingChartVisible = !this.client.debugHudEntryList.isF3Enabled() || !this.renderingChartVisible;
		if (this.renderingChartVisible) {
			this.client.debugHudEntryList.setF3Enabled(true);
		}
	}

	public void pushToFrameLog(long value) {
		this.frameNanosLog.push(value);
	}

	public MultiValueDebugSampleLogImpl getTickNanosLog() {
		return this.tickNanosLog;
	}

	public MultiValueDebugSampleLogImpl getPingLog() {
		return this.pingLog;
	}

	public MultiValueDebugSampleLogImpl getPacketSizeLog() {
		return this.packetSizeLog;
	}

	public PieChart getPieChart() {
		return this.pieChart;
	}

	public void set(long[] values, DebugSampleType type) {
		MultiValueDebugSampleLogImpl multiValueDebugSampleLogImpl = this.receivedDebugSamples.get(type);
		if (multiValueDebugSampleLogImpl != null) {
			multiValueDebugSampleLogImpl.set(values);
		}
	}

	public void clear() {
		this.tickNanosLog.clear();
		this.pingLog.clear();
		this.packetSizeLog.clear();
	}

	public void renderDebugCrosshair(Camera camera) {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.translate(0.0F, 0.0F, -1.0F);
		matrix4fStack.rotateX(camera.getPitch() * (float) (Math.PI / 180.0));
		matrix4fStack.rotateY(camera.getYaw() * (float) (Math.PI / 180.0));
		float f = 0.01F * this.client.getWindow().getScaleFactor();
		matrix4fStack.scale(-f, f, -f);
		RenderPipeline renderPipeline = RenderPipelines.LINES;
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
		GpuTextureView gpuTextureView2 = framebuffer.getDepthAttachmentView();
		GpuBuffer gpuBuffer = this.debugCrosshairIndexBuffer.getIndexBuffer(36);
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            matrix4fStack,
				                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "3d crosshair",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setVertexBuffer(0, this.debugCrosshairBuffer);
			renderPass.setIndexBuffer(gpuBuffer, this.debugCrosshairIndexBuffer.getIndexType());
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.drawIndexed(0, 0, 36, 1);
		}

		matrix4fStack.popMatrix();
	}
}
