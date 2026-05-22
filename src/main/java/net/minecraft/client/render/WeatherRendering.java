package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;

/**
 * Рендерер осадков (дождь и снег): строит геометрию полос осадков и добавляет
 * звуковые эффекты и частицы на поверхности блоков.
 * Предварительно вычисляет нормали линий осадков для всей сетки в конструкторе.
 */
@Environment(EnvType.CLIENT)
public class WeatherRendering {

	private static final float RAIN_PARTICLE_DENSITY_FACTOR = 0.225F;
	private static final int SOUND_HEIGHT_RANGE = 10;
	private static final Identifier RAIN_TEXTURE = Identifier.ofVanilla("textures/environment/rain.png");
	private static final Identifier SNOW_TEXTURE = Identifier.ofVanilla("textures/environment/snow.png");
	private static final int PRECIPITATION_GRID_SIZE = 32;
	private static final int PRECIPITATION_GRID_HALF = 16;
	/** Маска для ограничения счётчика тиков дождя (2^17 - 1). */
	private static final int RAIN_TICK_MASK = 131071;
	/** Маска для ограничения счётчика тиков снега (2^9 - 1). */
	private static final int SNOW_TICK_MASK = 511;
	/** Делитель для вычисления V-координаты текстуры дождя. */
	private static final float RAIN_V_DIVISOR = 32.0F;
	/** Делитель для вычисления V-координаты текстуры снега. */
	private static final float SNOW_V_DIVISOR = 512.0F;

	private int soundChance;
	/** Предвычисленные X-компоненты нормалей линий осадков для каждой ячейки сетки. */
	private final float[] normalLineDx = new float[PRECIPITATION_GRID_SIZE * PRECIPITATION_GRID_SIZE];
	/** Предвычисленные Z-компоненты нормалей линий осадков для каждой ячейки сетки. */
	private final float[] normalLineDz = new float[PRECIPITATION_GRID_SIZE * PRECIPITATION_GRID_SIZE];

	public WeatherRendering() {
		for (int row = 0; row < PRECIPITATION_GRID_SIZE; row++) {
			for (int col = 0; col < PRECIPITATION_GRID_SIZE; col++) {
				float dx = col - PRECIPITATION_GRID_HALF;
				float dz = row - PRECIPITATION_GRID_HALF;
				float length = MathHelper.hypot(dx, dz);
				normalLineDx[row * PRECIPITATION_GRID_SIZE + col] = -dz / length;
				normalLineDz[row * PRECIPITATION_GRID_SIZE + col] = dx / length;
			}
		}
	}

	/**
	 * Строит список кусков осадков (дождь/снег) для текущего кадра.
	 * Обходит сетку блоков вокруг камеры и создаёт {@link Piece} для каждой колонки
	 * с активными осадками.
	 *
	 * @param world       текущий мир
	 * @param ticks       текущий тик мира
	 * @param tickProgress интерполяционный прогресс тика
	 * @param cameraPos   позиция камеры
	 * @param state       состояние рендеринга погоды для заполнения
	 */
	public void buildPrecipitationPieces(
			World world,
			int ticks,
			float tickProgress,
			Vec3d cameraPos,
			WeatherRenderState state
	) {
		state.intensity = world.getRainGradient(tickProgress);
		if (state.intensity <= 0.0F) {
			return;
		}

		state.radius = MinecraftClient.getInstance().options.getWeatherRadius().getValue();
		int camX = MathHelper.floor(cameraPos.x);
		int camY = MathHelper.floor(cameraPos.y);
		int camZ = MathHelper.floor(cameraPos.z);
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Random random = Random.create();

		for (int z = camZ - state.radius; z <= camZ + state.radius; z++) {
			for (int x = camX - state.radius; x <= camX + state.radius; x++) {
				int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
				int yMin = Math.max(camY - state.radius, surfaceY);
				int yMax = Math.max(camY + state.radius, surfaceY);

				if (yMax - yMin == 0) {
					continue;
				}

				Biome.Precipitation precipitation = getPrecipitationAt(world, mutable.set(x, camY, z));
				if (precipitation == Biome.Precipitation.NONE) {
					continue;
				}

				int seed = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
				random.setSeed(seed);
				int lightY = Math.max(camY, surfaceY);
				int light = WorldRenderer.getLightmapCoordinates(world, mutable.set(x, lightY, z));

				if (precipitation == Biome.Precipitation.RAIN) {
					state.rainPieces.add(createRainPiece(random, ticks, x, yMin, yMax, z, light, tickProgress));
				}
				else if (precipitation == Biome.Precipitation.SNOW) {
					state.snowPieces.add(createSnowPiece(random, ticks, x, yMin, yMax, z, light, tickProgress));
				}
			}
		}
	}

	public void renderPrecipitation(VertexConsumerProvider vertexConsumers, Vec3d pos, WeatherRenderState state) {
		if (!state.rainPieces.isEmpty()) {
			RenderLayer rainLayer = RenderLayers.weather(RAIN_TEXTURE, MinecraftClient.usesImprovedTransparency());
			renderPieces(vertexConsumers.getBuffer(rainLayer), state.rainPieces, pos, 1.0F, state.radius, state.intensity);
		}

		if (!state.snowPieces.isEmpty()) {
			RenderLayer snowLayer = RenderLayers.weather(SNOW_TEXTURE, MinecraftClient.usesImprovedTransparency());
			renderPieces(vertexConsumers.getBuffer(snowLayer), state.snowPieces, pos, 0.8F, state.radius, state.intensity);
		}
	}

	private WeatherRendering.Piece createRainPiece(
			Random random,
			int ticks,
			int x,
			int yMin,
			int yMax,
			int z,
			int light,
			float tickProgress
	) {
		int tickMasked = ticks & RAIN_TICK_MASK;
		int colorSeed = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
		float speed = 3.0F + random.nextFloat();
		float rawV = -(tickMasked + colorSeed + tickProgress) / RAIN_V_DIVISOR * speed;
		float vOffset = rawV % RAIN_V_DIVISOR;
		return new WeatherRendering.Piece(x, z, yMin, yMax, 0.0F, vOffset, light);
	}

	private WeatherRendering.Piece createSnowPiece(
			Random random,
			int ticks,
			int x,
			int yMin,
			int yMax,
			int z,
			int light,
			float tickProgress
	) {
		float time = ticks + tickProgress;
		float uOffset = (float) (random.nextDouble() + time * 0.01F * (float) random.nextGaussian());
		float vBase = (float) (random.nextDouble() + time * (float) random.nextGaussian() * 0.001F);
		float vOffset = -((ticks & SNOW_TICK_MASK) + tickProgress) / SNOW_V_DIVISOR;
		int boostedLight = LightmapTextureManager.pack(
				(LightmapTextureManager.getBlockLightCoordinates(light) * 3 + 15) / 4,
				(LightmapTextureManager.getSkyLightCoordinates(light) * 3 + 15) / 4
		);
		return new WeatherRendering.Piece(x, z, yMin, yMax, uOffset, vOffset + vBase, boostedLight);
	}

	private void renderPieces(
			VertexConsumer vertexConsumer,
			List<WeatherRendering.Piece> pieces,
			Vec3d pos,
			float intensity,
			int range,
			float gradient
	) {
		float rangeSquared = range * range;

		for (WeatherRendering.Piece piece : pieces) {
			float relX = (float) (piece.x + 0.5 - pos.x);
			float relZ = (float) (piece.z + 0.5 - pos.z);
			float distSquared = (float) MathHelper.squaredHypot(relX, relZ);
			float alpha = MathHelper.lerp(Math.min(distSquared / rangeSquared, 1.0F), intensity, 0.5F) * gradient;
			int color = ColorHelper.getWhite(alpha);
			int gridIndex = (piece.z - MathHelper.floor(pos.z) + PRECIPITATION_GRID_HALF) * PRECIPITATION_GRID_SIZE
					+ piece.x - MathHelper.floor(pos.x) + PRECIPITATION_GRID_HALF;
			float halfNormalX = normalLineDx[gridIndex] / 2.0F;
			float halfNormalZ = normalLineDz[gridIndex] / 2.0F;
			float x0 = relX - halfNormalX;
			float x1 = relX + halfNormalX;
			float topRelY = (float) (piece.topY - pos.y);
			float bottomRelY = (float) (piece.bottomY - pos.y);
			float z0 = relZ - halfNormalZ;
			float z1 = relZ + halfNormalZ;
			float uMin = piece.uOffset;
			float uMax = piece.uOffset + 1.0F;
			float vTop = piece.bottomY * 0.25F + piece.vOffset;
			float vBottom = piece.topY * 0.25F + piece.vOffset;
			vertexConsumer.vertex(x0, topRelY, z0).texture(uMin, vTop).color(color).light(piece.lightCoords);
			vertexConsumer.vertex(x1, topRelY, z1).texture(uMax, vTop).color(color).light(piece.lightCoords);
			vertexConsumer.vertex(x1, bottomRelY, z1).texture(uMax, vBottom).color(color).light(piece.lightCoords);
			vertexConsumer.vertex(x0, bottomRelY, z0).texture(uMin, vBottom).color(color).light(piece.lightCoords);
		}
	}

	/**
	 * Добавляет частицы дождя на поверхности блоков и воспроизводит звук дождя.
	 * Частицы заменяются дымом над лавой, магмой и кострами.
	 *
	 * @param world         клиентский мир
	 * @param camera        камера для определения позиции
	 * @param ticks         текущий тик
	 * @param particlesMode режим отображения частиц
	 * @param weatherRadius радиус зоны осадков
	 */
	public void addParticlesAndSound(
			ClientWorld world,
			Camera camera,
			int ticks,
			ParticlesMode particlesMode,
			int weatherRadius
	) {
		float rainGradient = world.getRainGradient(1.0F);
		if (rainGradient <= 0.0F) {
			return;
		}

		Random random = Random.create(ticks * 312987231L);
		BlockPos cameraBlock = BlockPos.ofFloored(camera.getCameraPos());
		BlockPos lastRainPos = null;
		int diameter = 2 * weatherRadius + 1;
		int area = diameter * diameter;
		int particleCount = (int) (RAIN_PARTICLE_DENSITY_FACTOR * area * rainGradient * rainGradient)
				/ (particlesMode == ParticlesMode.DECREASED ? 2 : 1);

		for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
			int offsetX = random.nextInt(diameter) - weatherRadius;
			int offsetZ = random.nextInt(diameter) - weatherRadius;
			BlockPos surfacePos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, cameraBlock.add(offsetX, 0, offsetZ));

			if (surfacePos.getY() <= world.getBottomY()
					|| surfacePos.getY() > cameraBlock.getY() + SOUND_HEIGHT_RANGE
					|| surfacePos.getY() < cameraBlock.getY() - SOUND_HEIGHT_RANGE
					|| getPrecipitationAt(world, surfacePos) != Biome.Precipitation.RAIN
			) {
				continue;
			}

			lastRainPos = surfacePos.down();
			if (particlesMode == ParticlesMode.MINIMAL) {
				break;
			}

			double particleX = random.nextDouble();
			double particleZ = random.nextDouble();
			BlockState blockState = world.getBlockState(lastRainPos);
			FluidState fluidState = world.getFluidState(lastRainPos);
			VoxelShape voxelShape = blockState.getCollisionShape(world, lastRainPos);
			double surfaceTopY = voxelShape.getEndingCoord(Direction.Axis.Y, particleX, particleZ);
			double fluidHeight = fluidState.getHeight(world, lastRainPos);
			double particleSurfaceY = Math.max(surfaceTopY, fluidHeight);
			ParticleEffect particleEffect = !fluidState.isIn(FluidTags.LAVA)
					&& !blockState.isOf(Blocks.MAGMA_BLOCK)
					&& !CampfireBlock.isLitCampfire(blockState)
					? ParticleTypes.RAIN
					: ParticleTypes.SMOKE;
			world.addParticleClient(
					particleEffect,
					lastRainPos.getX() + particleX,
					lastRainPos.getY() + particleSurfaceY,
					lastRainPos.getZ() + particleZ,
					0.0,
					0.0,
					0.0
			);
		}

		if (lastRainPos != null && random.nextInt(3) < soundChance++) {
			soundChance = 0;

			if (lastRainPos.getY() > cameraBlock.getY() + 1
					&& world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, cameraBlock).getY()
					> MathHelper.floor((float) cameraBlock.getY())
			) {
				world.playSoundAtBlockCenterClient(
						lastRainPos,
						SoundEvents.WEATHER_RAIN_ABOVE,
						SoundCategory.WEATHER,
						0.1F,
						0.5F,
						false
				);
			}
			else {
				world.playSoundAtBlockCenterClient(
						lastRainPos,
						SoundEvents.WEATHER_RAIN,
						SoundCategory.WEATHER,
						0.2F,
						1.0F,
						false
				);
			}
		}
	}

	private Biome.Precipitation getPrecipitationAt(World world, BlockPos pos) {
		if (!world.getChunkManager().isChunkLoaded(
				ChunkSectionPos.getSectionCoord(pos.getX()),
				ChunkSectionPos.getSectionCoord(pos.getZ())
		)) {
			return Biome.Precipitation.NONE;
		}

		Biome biome = world.getBiome(pos).value();
		return biome.getPrecipitation(pos, world.getSeaLevel());
	}

	/** Данные одной полосы осадков: позиция, диапазон высот, UV-смещения и освещение. */
	@Environment(EnvType.CLIENT)
	public record Piece(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
	}
}
