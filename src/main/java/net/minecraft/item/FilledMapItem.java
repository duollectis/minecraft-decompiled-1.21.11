package net.minecraft.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

public class FilledMapItem extends Item {
   public static final int field_30907 = 128;
   public static final int field_30908 = 128;

   public FilledMapItem(Item.Settings settings) {
      super(settings);
   }

   public static ItemStack createMap(ServerWorld world, int x, int z, byte scale, boolean showIcons, boolean unlimitedTracking) {
      ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
      MapIdComponent mapIdComponent = allocateMapId(world, x, z, scale, showIcons, unlimitedTracking, world.getRegistryKey());
      itemStack.set(DataComponentTypes.MAP_ID, mapIdComponent);
      return itemStack;
   }

   public static @Nullable MapState getMapState(@Nullable MapIdComponent id, World world) {
      return id == null ? null : world.getMapState(id);
   }

   public static @Nullable MapState getMapState(ItemStack map, World world) {
      MapIdComponent mapIdComponent = map.get(DataComponentTypes.MAP_ID);
      return getMapState(mapIdComponent, world);
   }

   private static MapIdComponent allocateMapId(
      ServerWorld world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, RegistryKey<World> dimension
   ) {
      MapState mapState = MapState.of(x, z, (byte)scale, showIcons, unlimitedTracking, dimension);
      MapIdComponent mapIdComponent = world.increaseAndGetMapId();
      world.putMapState(mapIdComponent, mapState);
      return mapIdComponent;
   }

   public void updateColors(World world, Entity entity, MapState state) {
      if (world.getRegistryKey() == state.dimension && entity instanceof PlayerEntity) {
         int i = 1 << state.scale;
         int j = state.centerX;
         int k = state.centerZ;
         int l = MathHelper.floor(entity.getX() - j) / i + 64;
         int m = MathHelper.floor(entity.getZ() - k) / i + 64;
         int n = 128 / i;
         if (world.getDimension().hasCeiling()) {
            n /= 2;
         }

         MapState.PlayerUpdateTracker playerUpdateTracker = state.getPlayerSyncData((PlayerEntity)entity);
         playerUpdateTracker.field_131++;
         BlockPos.Mutable mutable = new BlockPos.Mutable();
         BlockPos.Mutable mutable2 = new BlockPos.Mutable();
         boolean bl = false;

         for (int o = l - n + 1; o < l + n; o++) {
            if ((o & 15) == (playerUpdateTracker.field_131 & 15) || bl) {
               bl = false;
               double d = 0.0;

               for (int p = m - n - 1; p < m + n; p++) {
                  if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                     int q = MathHelper.square(o - l) + MathHelper.square(p - m);
                     boolean bl2 = q > (n - 2) * (n - 2);
                     int r = (j / i + o - 64) * i;
                     int s = (k / i + p - 64) * i;
                     Multiset<MapColor> multiset = LinkedHashMultiset.create();
                     WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(r), ChunkSectionPos.getSectionCoord(s));
                     if (!worldChunk.isEmpty()) {
                        int t = 0;
                        double e = 0.0;
                        if (world.getDimension().hasCeiling()) {
                           int u = r + s * 231871;
                           u = u * u * 31287121 + u * 11;
                           if ((u >> 20 & 1) == 0) {
                              multiset.add(Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 10);
                           } else {
                              multiset.add(Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 100);
                           }

                           e = 100.0;
                        } else {
                           for (int u = 0; u < i; u++) {
                              for (int v = 0; v < i; v++) {
                                 mutable.set(r + u, 0, s + v);
                                 int w = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;
                                 BlockState blockState;
                                 if (w <= world.getBottomY()) {
                                    blockState = Blocks.BEDROCK.getDefaultState();
                                 } else {
                                    do {
                                       mutable.setY(--w);
                                       blockState = worldChunk.getBlockState(mutable);
                                    } while (blockState.getMapColor(world, mutable) == MapColor.CLEAR && w > world.getBottomY());

                                    if (w > world.getBottomY() && !blockState.getFluidState().isEmpty()) {
                                       int x = w - 1;
                                       mutable2.set(mutable);

                                       BlockState blockState2;
                                       do {
                                          mutable2.setY(x--);
                                          blockState2 = worldChunk.getBlockState(mutable2);
                                          t++;
                                       } while (x > world.getBottomY() && !blockState2.getFluidState().isEmpty());

                                       blockState = this.getFluidStateIfVisible(world, blockState, mutable);
                                    }
                                 }

                                 state.removeBanner(world, mutable.getX(), mutable.getZ());
                                 e += (double)w / (i * i);
                                 multiset.add(blockState.getMapColor(world, mutable));
                              }
                           }
                        }

                        t /= i * i;
                        MapColor mapColor = (MapColor)Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR);
                        MapColor.Brightness brightness;
                        if (mapColor == MapColor.WATER_BLUE) {
                           double f = t * 0.1 + (o + p & 1) * 0.2;
                           if (f < 0.5) {
                              brightness = MapColor.Brightness.HIGH;
                           } else if (f > 0.9) {
                              brightness = MapColor.Brightness.LOW;
                           } else {
                              brightness = MapColor.Brightness.NORMAL;
                           }
                        } else {
                           double f = (e - d) * 4.0 / (i + 4) + ((o + p & 1) - 0.5) * 0.4;
                           if (f > 0.6) {
                              brightness = MapColor.Brightness.HIGH;
                           } else if (f < -0.6) {
                              brightness = MapColor.Brightness.LOW;
                           } else {
                              brightness = MapColor.Brightness.NORMAL;
                           }
                        }

                        d = e;
                        if (p >= 0 && q < n * n && (!bl2 || (o + p & 1) != 0)) {
                           bl |= state.putColor(o, p, mapColor.getRenderColorByte(brightness));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
      FluidState fluidState = state.getFluidState();
      return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
   }

   private static boolean isAquaticBiome(boolean[] biomes, int x, int z) {
      return biomes[z * 128 + x];
   }

   public static void fillExplorationMap(ServerWorld world, ItemStack map) {
      MapState mapState = getMapState(map, world);
      if (mapState != null) {
         if (world.getRegistryKey() == mapState.dimension) {
            int i = 1 << mapState.scale;
            int j = mapState.centerX;
            int k = mapState.centerZ;
            boolean[] bls = new boolean[16384];
            int l = j / i - 64;
            int m = k / i - 64;
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            for (int n = 0; n < 128; n++) {
               for (int o = 0; o < 128; o++) {
                  RegistryEntry<Biome> registryEntry = world.getBiome(mutable.set((l + o) * i, 0, (m + n) * i));
                  bls[n * 128 + o] = registryEntry.isIn(BiomeTags.WATER_ON_MAP_OUTLINES);
               }
            }

            for (int n = 1; n < 127; n++) {
               for (int o = 1; o < 127; o++) {
                  int p = 0;

                  for (int q = -1; q < 2; q++) {
                     for (int r = -1; r < 2; r++) {
                        if ((q != 0 || r != 0) && isAquaticBiome(bls, n + q, o + r)) {
                           p++;
                        }
                     }
                  }

                  MapColor.Brightness brightness = MapColor.Brightness.LOWEST;
                  MapColor mapColor = MapColor.CLEAR;
                  if (isAquaticBiome(bls, n, o)) {
                     mapColor = MapColor.ORANGE;
                     if (p > 7 && o % 2 == 0) {
                        switch ((n + (int)(MathHelper.sin(o + 0.0F) * 7.0F)) / 8 % 5) {
                           case 0:
                           case 4:
                              brightness = MapColor.Brightness.LOW;
                              break;
                           case 1:
                           case 3:
                              brightness = MapColor.Brightness.NORMAL;
                              break;
                           case 2:
                              brightness = MapColor.Brightness.HIGH;
                        }
                     } else if (p > 7) {
                        mapColor = MapColor.CLEAR;
                     } else if (p > 5) {
                        brightness = MapColor.Brightness.NORMAL;
                     } else if (p > 3) {
                        brightness = MapColor.Brightness.LOW;
                     } else if (p > 1) {
                        brightness = MapColor.Brightness.LOW;
                     }
                  } else if (p > 0) {
                     mapColor = MapColor.BROWN;
                     if (p > 3) {
                        brightness = MapColor.Brightness.NORMAL;
                     } else {
                        brightness = MapColor.Brightness.LOWEST;
                     }
                  }

                  if (mapColor != MapColor.CLEAR) {
                     mapState.setColor(n, o, mapColor.getRenderColorByte(brightness));
                  }
               }
            }
         }
      }
   }

   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
      MapState mapState = getMapState(stack, world);
      if (mapState != null) {
         if (entity instanceof PlayerEntity playerEntity) {
            mapState.update(playerEntity, stack);
         }

         if (!mapState.locked && slot != null && slot.getType() == EquipmentSlot.Type.HAND) {
            this.updateColors(world, entity, mapState);
         }
      }
   }

   @Override
   public void onCraft(ItemStack stack, World world) {
      MapPostProcessingComponent mapPostProcessingComponent = stack.remove(DataComponentTypes.MAP_POST_PROCESSING);
      if (mapPostProcessingComponent != null) {
         if (world instanceof ServerWorld serverWorld) {
            switch (mapPostProcessingComponent) {
               case LOCK:
                  copyMap(stack, serverWorld);
                  break;
               case SCALE:
                  scale(stack, serverWorld);
            }
         }
      }
   }

   private static void scale(ItemStack map, ServerWorld world) {
      MapState mapState = getMapState(map, world);
      if (mapState != null) {
         MapIdComponent mapIdComponent = world.increaseAndGetMapId();
         world.putMapState(mapIdComponent, mapState.zoomOut());
         map.set(DataComponentTypes.MAP_ID, mapIdComponent);
      }
   }

   private static void copyMap(ItemStack stack, ServerWorld world) {
      MapState mapState = getMapState(stack, world);
      if (mapState != null) {
         MapIdComponent mapIdComponent = world.increaseAndGetMapId();
         MapState mapState2 = mapState.copy();
         world.putMapState(mapIdComponent, mapState2);
         stack.set(DataComponentTypes.MAP_ID, mapIdComponent);
      }
   }

   @Override
   public ActionResult useOnBlock(ItemUsageContext context) {
      BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
      if (blockState.isIn(BlockTags.BANNERS)) {
         if (!context.getWorld().isClient()) {
            MapState mapState = getMapState(context.getStack(), context.getWorld());
            if (mapState != null && !mapState.addBanner(context.getWorld(), context.getBlockPos())) {
               return ActionResult.FAIL;
            }
         }

         return ActionResult.SUCCESS;
      } else {
         return super.useOnBlock(context);
      }
   }
}
