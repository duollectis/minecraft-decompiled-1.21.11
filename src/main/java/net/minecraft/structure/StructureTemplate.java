package net.minecraft.structure;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplate {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String PALETTE_KEY = "palette";
   public static final String PALETTES_KEY = "palettes";
   public static final String ENTITIES_KEY = "entities";
   public static final String BLOCKS_KEY = "blocks";
   public static final String BLOCKS_POS_KEY = "pos";
   public static final String BLOCKS_STATE_KEY = "state";
   public static final String BLOCKS_NBT_KEY = "nbt";
   public static final String ENTITIES_POS_KEY = "pos";
   public static final String ENTITIES_BLOCK_POS_KEY = "blockPos";
   public static final String ENTITIES_NBT_KEY = "nbt";
   public static final String SIZE_KEY = "size";
   private final List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = Lists.newArrayList();
   private final List<StructureTemplate.StructureEntityInfo> entities = Lists.newArrayList();
   private Vec3i size = Vec3i.ZERO;
   private String author = "?";

   public Vec3i getSize() {
      return this.size;
   }

   public void setAuthor(String author) {
      this.author = author;
   }

   public String getAuthor() {
      return this.author;
   }

   public void saveFromWorld(World world, BlockPos start, Vec3i dimensions, boolean includeEntities, List<Block> ignoredBlocks) {
      if (dimensions.getX() >= 1 && dimensions.getY() >= 1 && dimensions.getZ() >= 1) {
         BlockPos blockPos = start.add(dimensions).add(-1, -1, -1);
         List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
         List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
         BlockPos blockPos2 = new BlockPos(
            Math.min(start.getX(), blockPos.getX()), Math.min(start.getY(), blockPos.getY()), Math.min(start.getZ(), blockPos.getZ())
         );
         BlockPos blockPos3 = new BlockPos(
            Math.max(start.getX(), blockPos.getX()), Math.max(start.getY(), blockPos.getY()), Math.max(start.getZ(), blockPos.getZ())
         );
         this.size = dimensions;

         try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
            for (BlockPos blockPos4 : BlockPos.iterate(blockPos2, blockPos3)) {
               BlockPos blockPos5 = blockPos4.subtract(blockPos2);
               BlockState blockState = world.getBlockState(blockPos4);
               if (!ignoredBlocks.stream().anyMatch(blockState::isOf)) {
                  BlockEntity blockEntity = world.getBlockEntity(blockPos4);
                  StructureTemplate.StructureBlockInfo structureBlockInfo;
                  if (blockEntity != null) {
                     NbtWriteView nbtWriteView = NbtWriteView.create(logging, world.getRegistryManager());
                     blockEntity.writeDataWithId(nbtWriteView);
                     structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, nbtWriteView.getNbt());
                  } else {
                     structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos5, blockState, null);
                  }

                  categorize(structureBlockInfo, list, list2, list3);
               }
            }

            List<StructureTemplate.StructureBlockInfo> list4 = combineSorted(list, list2, list3);
            this.blockInfoLists.clear();
            this.blockInfoLists.add(new StructureTemplate.PalettedBlockInfoList(list4));
            if (includeEntities) {
               this.addEntitiesFromWorld(world, blockPos2, blockPos3, logging);
            } else {
               this.entities.clear();
            }
         }
      }
   }

   private static void categorize(
      StructureTemplate.StructureBlockInfo blockInfo,
      List<StructureTemplate.StructureBlockInfo> fullBlocks,
      List<StructureTemplate.StructureBlockInfo> blocksWithNbt,
      List<StructureTemplate.StructureBlockInfo> otherBlocks
   ) {
      if (blockInfo.nbt != null) {
         blocksWithNbt.add(blockInfo);
      } else if (!blockInfo.state.getBlock().hasDynamicBounds() && blockInfo.state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
         fullBlocks.add(blockInfo);
      } else {
         otherBlocks.add(blockInfo);
      }
   }

   private static List<StructureTemplate.StructureBlockInfo> combineSorted(
      List<StructureTemplate.StructureBlockInfo> fullBlocks,
      List<StructureTemplate.StructureBlockInfo> blocksWithNbt,
      List<StructureTemplate.StructureBlockInfo> otherBlocks
   ) {
      Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
            blockInfo -> blockInfo.pos.getY()
         )
         .thenComparingInt(blockInfo -> blockInfo.pos.getX())
         .thenComparingInt(blockInfo -> blockInfo.pos.getZ());
      fullBlocks.sort(comparator);
      otherBlocks.sort(comparator);
      blocksWithNbt.sort(comparator);
      List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
      list.addAll(fullBlocks);
      list.addAll(otherBlocks);
      list.addAll(blocksWithNbt);
      return list;
   }

   private void addEntitiesFromWorld(World world, BlockPos firstCorner, BlockPos secondCorner, ErrorReporter errorReporter) {
      List<Entity> list = world.getEntitiesByClass(Entity.class, Box.enclosing(firstCorner, secondCorner), entityx -> !(entityx instanceof PlayerEntity));
      this.entities.clear();

      for (Entity entity : list) {
         Vec3d vec3d = new Vec3d(entity.getX() - firstCorner.getX(), entity.getY() - firstCorner.getY(), entity.getZ() - firstCorner.getZ());
         NbtWriteView nbtWriteView = NbtWriteView.create(errorReporter.makeChild(entity.getErrorReporterContext()), entity.getRegistryManager());
         entity.saveData(nbtWriteView);
         BlockPos blockPos;
         if (entity instanceof PaintingEntity paintingEntity) {
            blockPos = paintingEntity.getAttachedBlockPos().subtract(firstCorner);
         } else {
            blockPos = BlockPos.ofFloored(vec3d);
         }

         this.entities.add(new StructureTemplate.StructureEntityInfo(vec3d, blockPos, nbtWriteView.getNbt().copy()));
      }
   }

   public List<StructureTemplate.StructureBlockInfo> getInfosForBlock(BlockPos pos, StructurePlacementData placementData, Block block) {
      return this.getInfosForBlock(pos, placementData, block, true);
   }

   public List<StructureTemplate.JigsawBlockInfo> getJigsawInfos(BlockPos pos, BlockRotation rotation) {
      if (this.blockInfoLists.isEmpty()) {
         return new ArrayList<>();
      } else {
         StructurePlacementData structurePlacementData = new StructurePlacementData().setRotation(rotation);
         List<StructureTemplate.JigsawBlockInfo> list = structurePlacementData.getRandomBlockInfos(this.blockInfoLists, pos).getOrCreateJigsawBlockInfos();
         List<StructureTemplate.JigsawBlockInfo> list2 = new ArrayList<>(list.size());

         for (StructureTemplate.JigsawBlockInfo jigsawBlockInfo : list) {
            StructureTemplate.StructureBlockInfo structureBlockInfo = jigsawBlockInfo.info;
            list2.add(
               jigsawBlockInfo.withInfo(
                  new StructureTemplate.StructureBlockInfo(
                     transform(structurePlacementData, structureBlockInfo.pos()).add(pos),
                     structureBlockInfo.state.rotate(structurePlacementData.getRotation()),
                     structureBlockInfo.nbt
                  )
               )
            );
         }

         return list2;
      }
   }

   public ObjectArrayList<StructureTemplate.StructureBlockInfo> getInfosForBlock(
      BlockPos pos, StructurePlacementData placementData, Block block, boolean transformed
   ) {
      ObjectArrayList<StructureTemplate.StructureBlockInfo> objectArrayList = new ObjectArrayList();
      BlockBox blockBox = placementData.getBoundingBox();
      if (this.blockInfoLists.isEmpty()) {
         return objectArrayList;
      } else {
         for (StructureTemplate.StructureBlockInfo structureBlockInfo : placementData.getRandomBlockInfos(this.blockInfoLists, pos).getAllOf(block)) {
            BlockPos blockPos = transformed ? transform(placementData, structureBlockInfo.pos).add(pos) : structureBlockInfo.pos;
            if (blockBox == null || blockBox.contains(blockPos)) {
               objectArrayList.add(
                  new StructureTemplate.StructureBlockInfo(blockPos, structureBlockInfo.state.rotate(placementData.getRotation()), structureBlockInfo.nbt)
               );
            }
         }

         return objectArrayList;
      }
   }

   public BlockPos transformBox(StructurePlacementData placementData1, BlockPos pos1, StructurePlacementData placementData2, BlockPos pos2) {
      BlockPos blockPos = transform(placementData1, pos1);
      BlockPos blockPos2 = transform(placementData2, pos2);
      return blockPos.subtract(blockPos2);
   }

   public static BlockPos transform(StructurePlacementData placementData, BlockPos pos) {
      return transformAround(pos, placementData.getMirror(), placementData.getRotation(), placementData.getPosition());
   }

   public boolean place(
      ServerWorldAccess world, BlockPos pos, BlockPos pivot, StructurePlacementData placementData, Random random, @Block.SetBlockStateFlag int flags
   ) {
      if (this.blockInfoLists.isEmpty()) {
         return false;
      } else {
         List<StructureTemplate.StructureBlockInfo> list = placementData.getRandomBlockInfos(this.blockInfoLists, pos).getAll();
         if ((!list.isEmpty() || !placementData.shouldIgnoreEntities() && !this.entities.isEmpty())
            && this.size.getX() >= 1
            && this.size.getY() >= 1
            && this.size.getZ() >= 1) {
            BlockBox blockBox = placementData.getBoundingBox();
            List<BlockPos> list2 = Lists.newArrayListWithCapacity(placementData.shouldApplyWaterlogging() ? list.size() : 0);
            List<BlockPos> list3 = Lists.newArrayListWithCapacity(placementData.shouldApplyWaterlogging() ? list.size() : 0);
            List<Pair<BlockPos, NbtCompound>> list4 = Lists.newArrayListWithCapacity(list.size());
            int i = Integer.MAX_VALUE;
            int j = Integer.MAX_VALUE;
            int k = Integer.MAX_VALUE;
            int l = Integer.MIN_VALUE;
            int m = Integer.MIN_VALUE;
            int n = Integer.MIN_VALUE;
            List<StructureTemplate.StructureBlockInfo> list5 = process(world, pos, pivot, placementData, list);

            try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
               for (StructureTemplate.StructureBlockInfo structureBlockInfo : list5) {
                  BlockPos blockPos = structureBlockInfo.pos;
                  if (blockBox == null || blockBox.contains(blockPos)) {
                     FluidState fluidState = placementData.shouldApplyWaterlogging() ? world.getFluidState(blockPos) : null;
                     BlockState blockState = structureBlockInfo.state.mirror(placementData.getMirror()).rotate(placementData.getRotation());
                     if (structureBlockInfo.nbt != null) {
                        world.setBlockState(blockPos, Blocks.BARRIER.getDefaultState(), 820);
                     }

                     if (world.setBlockState(blockPos, blockState, flags)) {
                        i = Math.min(i, blockPos.getX());
                        j = Math.min(j, blockPos.getY());
                        k = Math.min(k, blockPos.getZ());
                        l = Math.max(l, blockPos.getX());
                        m = Math.max(m, blockPos.getY());
                        n = Math.max(n, blockPos.getZ());
                        list4.add(Pair.of(blockPos, structureBlockInfo.nbt));
                        if (structureBlockInfo.nbt != null) {
                           BlockEntity blockEntity = world.getBlockEntity(blockPos);
                           if (blockEntity != null) {
                              if (!SharedConstants.STRUCTURE_EDIT_MODE && blockEntity instanceof LootableInventory) {
                                 structureBlockInfo.nbt.putLong("LootTableSeed", random.nextLong());
                              }

                              blockEntity.read(
                                 NbtReadView.create(logging.makeChild(blockEntity.getReporterContext()), world.getRegistryManager(), structureBlockInfo.nbt)
                              );
                           }
                        }

                        if (fluidState != null) {
                           if (blockState.getFluidState().isStill()) {
                              list3.add(blockPos);
                           } else if (blockState.getBlock() instanceof FluidFillable) {
                              ((FluidFillable)blockState.getBlock()).tryFillWithFluid(world, blockPos, blockState, fluidState);
                              if (!fluidState.isStill()) {
                                 list2.add(blockPos);
                              }
                           }
                        }
                     }
                  }
               }

               boolean bl = true;
               Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

               while (bl && !list2.isEmpty()) {
                  bl = false;
                  Iterator<BlockPos> iterator = list2.iterator();

                  while (iterator.hasNext()) {
                     BlockPos blockPos2 = iterator.next();
                     FluidState fluidState2 = world.getFluidState(blockPos2);

                     for (int o = 0; o < directions.length && !fluidState2.isStill(); o++) {
                        BlockPos blockPos3 = blockPos2.offset(directions[o]);
                        FluidState fluidState3 = world.getFluidState(blockPos3);
                        if (fluidState3.isStill() && !list3.contains(blockPos3)) {
                           fluidState2 = fluidState3;
                        }
                     }

                     if (fluidState2.isStill()) {
                        BlockState blockState2 = world.getBlockState(blockPos2);
                        Block block = blockState2.getBlock();
                        if (block instanceof FluidFillable) {
                           ((FluidFillable)block).tryFillWithFluid(world, blockPos2, blockState2, fluidState2);
                           bl = true;
                           iterator.remove();
                        }
                     }
                  }
               }

               if (i <= l) {
                  if (!placementData.shouldUpdateNeighbors()) {
                     VoxelSet voxelSet = new BitSetVoxelSet(l - i + 1, m - j + 1, n - k + 1);
                     int p = i;
                     int q = j;
                     int ox = k;

                     for (Pair<BlockPos, NbtCompound> pair : list4) {
                        BlockPos blockPos4 = (BlockPos)pair.getFirst();
                        voxelSet.set(blockPos4.getX() - p, blockPos4.getY() - q, blockPos4.getZ() - ox);
                     }

                     updateCorner(world, flags, voxelSet, p, q, ox);
                  }

                  for (Pair<BlockPos, NbtCompound> pair2 : list4) {
                     BlockPos blockPos5 = (BlockPos)pair2.getFirst();
                     if (!placementData.shouldUpdateNeighbors()) {
                        BlockState blockState2 = world.getBlockState(blockPos5);
                        BlockState blockState3 = Block.postProcessState(blockState2, world, blockPos5);
                        if (blockState2 != blockState3) {
                           world.setBlockState(blockPos5, blockState3, flags & -2 | 16);
                        }

                        world.updateNeighbors(blockPos5, blockState3.getBlock());
                     }

                     if (pair2.getSecond() != null) {
                        BlockEntity blockEntity = world.getBlockEntity(blockPos5);
                        if (blockEntity != null) {
                           blockEntity.markDirty();
                        }
                     }
                  }
               }

               if (!placementData.shouldIgnoreEntities()) {
                  this.spawnEntities(
                     world,
                     pos,
                     placementData.getMirror(),
                     placementData.getRotation(),
                     placementData.getPosition(),
                     blockBox,
                     placementData.shouldInitializeMobs(),
                     logging
                  );
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public static void updateCorner(WorldAccess world, @Block.SetBlockStateFlag int flags, VoxelSet set, BlockPos startPos) {
      updateCorner(world, flags, set, startPos.getX(), startPos.getY(), startPos.getZ());
   }

   public static void updateCorner(WorldAccess world, @Block.SetBlockStateFlag int flags, VoxelSet set, int startX, int startY, int startZ) {
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      BlockPos.Mutable mutable2 = new BlockPos.Mutable();
      set.forEachDirection(
         (direction, x, y, z) -> {
            mutable.set(startX + x, startY + y, startZ + z);
            mutable2.set(mutable, direction);
            BlockState blockState = world.getBlockState(mutable);
            BlockState blockState2 = world.getBlockState(mutable2);
            BlockState blockState3 = blockState.getStateForNeighborUpdate(world, world, mutable, direction, mutable2, blockState2, world.getRandom());
            if (blockState != blockState3) {
               world.setBlockState(mutable, blockState3, flags & -2);
            }

            BlockState blockState4 = blockState2.getStateForNeighborUpdate(
               world, world, mutable2, direction.getOpposite(), mutable, blockState3, world.getRandom()
            );
            if (blockState2 != blockState4) {
               world.setBlockState(mutable2, blockState4, flags & -2);
            }
         }
      );
   }

   public static List<StructureTemplate.StructureBlockInfo> process(
      ServerWorldAccess world, BlockPos pos, BlockPos pivot, StructurePlacementData placementData, List<StructureTemplate.StructureBlockInfo> infos
   ) {
      List<StructureTemplate.StructureBlockInfo> list = new ArrayList<>();
      List<StructureTemplate.StructureBlockInfo> list2 = new ArrayList<>();

      for (StructureTemplate.StructureBlockInfo structureBlockInfo : infos) {
         BlockPos blockPos = transform(placementData, structureBlockInfo.pos).add(pos);
         StructureTemplate.StructureBlockInfo structureBlockInfo2 = new StructureTemplate.StructureBlockInfo(
            blockPos, structureBlockInfo.state, structureBlockInfo.nbt != null ? structureBlockInfo.nbt.copy() : null
         );
         Iterator<StructureProcessor> iterator = placementData.getProcessors().iterator();

         while (structureBlockInfo2 != null && iterator.hasNext()) {
            structureBlockInfo2 = iterator.next().process(world, pos, pivot, structureBlockInfo, structureBlockInfo2, placementData);
         }

         if (structureBlockInfo2 != null) {
            list2.add(structureBlockInfo2);
            list.add(structureBlockInfo);
         }
      }

      for (StructureProcessor structureProcessor : placementData.getProcessors()) {
         list2 = structureProcessor.reprocess(world, pos, pivot, list, list2, placementData);
      }

      return list2;
   }

   private void spawnEntities(
      ServerWorldAccess world,
      BlockPos pos,
      BlockMirror mirror,
      BlockRotation rotation,
      BlockPos pivot,
      @Nullable BlockBox area,
      boolean initializeMobs,
      ErrorReporter errorReporter
   ) {
      for (StructureTemplate.StructureEntityInfo structureEntityInfo : this.entities) {
         BlockPos blockPos = transformAround(structureEntityInfo.blockPos, mirror, rotation, pivot).add(pos);
         if (area == null || area.contains(blockPos)) {
            NbtCompound nbtCompound = structureEntityInfo.nbt.copy();
            Vec3d vec3d = transformAround(structureEntityInfo.pos, mirror, rotation, pivot);
            Vec3d vec3d2 = vec3d.add(pos.getX(), pos.getY(), pos.getZ());
            NbtList nbtList = new NbtList();
            nbtList.add(NbtDouble.of(vec3d2.x));
            nbtList.add(NbtDouble.of(vec3d2.y));
            nbtList.add(NbtDouble.of(vec3d2.z));
            nbtCompound.put("Pos", nbtList);
            nbtCompound.remove("UUID");
            getEntity(errorReporter, world, nbtCompound).ifPresent(entity -> {
               float f = entity.applyRotation(rotation);
               f += entity.applyMirror(mirror) - entity.getYaw();
               entity.refreshPositionAndAngles(vec3d2.x, vec3d2.y, vec3d2.z, f, entity.getPitch());
               entity.setBodyYaw(f);
               entity.setHeadYaw(f);
               if (initializeMobs && entity instanceof MobEntity mobEntity) {
                  mobEntity.initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(vec3d2)), SpawnReason.STRUCTURE, null);
               }

               world.spawnEntityAndPassengers(entity);
            });
         }
      }
   }

   private static Optional<Entity> getEntity(ErrorReporter errorReporter, ServerWorldAccess world, NbtCompound nbt) {
      try {
         return EntityType.getEntityFromData(NbtReadView.create(errorReporter, world.getRegistryManager(), nbt), world.toServerWorld(), SpawnReason.STRUCTURE);
      } catch (Exception var4) {
         return Optional.empty();
      }
   }

   public Vec3i getRotatedSize(BlockRotation rotation) {
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
         default:
            return this.size;
      }
   }

   public static BlockPos transformAround(BlockPos pos, BlockMirror mirror, BlockRotation rotation, BlockPos pivot) {
      int i = pos.getX();
      int j = pos.getY();
      int k = pos.getZ();
      boolean bl = true;
      switch (mirror) {
         case LEFT_RIGHT:
            k = -k;
            break;
         case FRONT_BACK:
            i = -i;
            break;
         default:
            bl = false;
      }

      int l = pivot.getX();
      int m = pivot.getZ();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
            return new BlockPos(l - m + k, j, l + m - i);
         case CLOCKWISE_90:
            return new BlockPos(l + m - k, j, m - l + i);
         case CLOCKWISE_180:
            return new BlockPos(l + l - i, j, m + m - k);
         default:
            return bl ? new BlockPos(i, j, k) : pos;
      }
   }

   public static Vec3d transformAround(Vec3d point, BlockMirror mirror, BlockRotation rotation, BlockPos pivot) {
      double d = point.x;
      double e = point.y;
      double f = point.z;
      boolean bl = true;
      switch (mirror) {
         case LEFT_RIGHT:
            f = 1.0 - f;
            break;
         case FRONT_BACK:
            d = 1.0 - d;
            break;
         default:
            bl = false;
      }

      int i = pivot.getX();
      int j = pivot.getZ();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
            return new Vec3d(i - j + f, e, i + j + 1 - d);
         case CLOCKWISE_90:
            return new Vec3d(i + j + 1 - f, e, j - i + d);
         case CLOCKWISE_180:
            return new Vec3d(i + i + 1 - d, e, j + j + 1 - f);
         default:
            return bl ? new Vec3d(d, e, f) : point;
      }
   }

   public BlockPos offsetByTransformedSize(BlockPos pos, BlockMirror mirror, BlockRotation rotation) {
      return applyTransformedOffset(pos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
   }

   public static BlockPos applyTransformedOffset(BlockPos pos, BlockMirror mirror, BlockRotation rotation, int offsetX, int offsetZ) {
      offsetX--;
      offsetZ--;
      int i = mirror == BlockMirror.FRONT_BACK ? offsetX : 0;
      int j = mirror == BlockMirror.LEFT_RIGHT ? offsetZ : 0;
      BlockPos blockPos = pos;
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
            blockPos = pos.add(j, 0, offsetX - i);
            break;
         case CLOCKWISE_90:
            blockPos = pos.add(offsetZ - j, 0, i);
            break;
         case CLOCKWISE_180:
            blockPos = pos.add(offsetX - i, 0, offsetZ - j);
            break;
         case NONE:
            blockPos = pos.add(i, 0, j);
      }

      return blockPos;
   }

   public BlockBox calculateBoundingBox(StructurePlacementData placementData, BlockPos pos) {
      return this.calculateBoundingBox(pos, placementData.getRotation(), placementData.getPosition(), placementData.getMirror());
   }

   public BlockBox calculateBoundingBox(BlockPos pos, BlockRotation rotation, BlockPos pivot, BlockMirror mirror) {
      return createBox(pos, rotation, pivot, mirror, this.size);
   }

   @VisibleForTesting
   protected static BlockBox createBox(BlockPos pos, BlockRotation rotation, BlockPos pivot, BlockMirror mirror, Vec3i dimensions) {
      Vec3i vec3i = dimensions.add(-1, -1, -1);
      BlockPos blockPos = transformAround(BlockPos.ORIGIN, mirror, rotation, pivot);
      BlockPos blockPos2 = transformAround(BlockPos.ORIGIN.add(vec3i), mirror, rotation, pivot);
      return BlockBox.create(blockPos, blockPos2).move(pos);
   }

   public NbtCompound writeNbt(NbtCompound nbt) {
      if (this.blockInfoLists.isEmpty()) {
         nbt.put("blocks", new NbtList());
         nbt.put("palette", new NbtList());
      } else {
         List<StructureTemplate.Palette> list = Lists.newArrayList();
         StructureTemplate.Palette palette = new StructureTemplate.Palette();
         list.add(palette);

         for (int i = 1; i < this.blockInfoLists.size(); i++) {
            list.add(new StructureTemplate.Palette());
         }

         NbtList nbtList = new NbtList();
         List<StructureTemplate.StructureBlockInfo> list2 = this.blockInfoLists.get(0).getAll();

         for (int j = 0; j < list2.size(); j++) {
            StructureTemplate.StructureBlockInfo structureBlockInfo = list2.get(j);
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.put("pos", this.createNbtIntList(structureBlockInfo.pos.getX(), structureBlockInfo.pos.getY(), structureBlockInfo.pos.getZ()));
            int k = palette.getId(structureBlockInfo.state);
            nbtCompound.putInt("state", k);
            if (structureBlockInfo.nbt != null) {
               nbtCompound.put("nbt", structureBlockInfo.nbt);
            }

            nbtList.add(nbtCompound);

            for (int l = 1; l < this.blockInfoLists.size(); l++) {
               StructureTemplate.Palette palette2 = list.get(l);
               palette2.set(this.blockInfoLists.get(l).getAll().get(j).state, k);
            }
         }

         nbt.put("blocks", nbtList);
         if (list.size() == 1) {
            NbtList nbtList2 = new NbtList();

            for (BlockState blockState : palette) {
               nbtList2.add(NbtHelper.fromBlockState(blockState));
            }

            nbt.put("palette", nbtList2);
         } else {
            NbtList nbtList2 = new NbtList();

            for (StructureTemplate.Palette palette3 : list) {
               NbtList nbtList3 = new NbtList();

               for (BlockState blockState2 : palette3) {
                  nbtList3.add(NbtHelper.fromBlockState(blockState2));
               }

               nbtList2.add(nbtList3);
            }

            nbt.put("palettes", nbtList2);
         }
      }

      NbtList nbtList4 = new NbtList();

      for (StructureTemplate.StructureEntityInfo structureEntityInfo : this.entities) {
         NbtCompound nbtCompound2 = new NbtCompound();
         nbtCompound2.put("pos", this.createNbtDoubleList(structureEntityInfo.pos.x, structureEntityInfo.pos.y, structureEntityInfo.pos.z));
         nbtCompound2.put(
            "blockPos", this.createNbtIntList(structureEntityInfo.blockPos.getX(), structureEntityInfo.blockPos.getY(), structureEntityInfo.blockPos.getZ())
         );
         if (structureEntityInfo.nbt != null) {
            nbtCompound2.put("nbt", structureEntityInfo.nbt);
         }

         nbtList4.add(nbtCompound2);
      }

      nbt.put("entities", nbtList4);
      nbt.put("size", this.createNbtIntList(this.size.getX(), this.size.getY(), this.size.getZ()));
      return NbtHelper.putDataVersion(nbt);
   }

   public void readNbt(RegistryEntryLookup<Block> blockLookup, NbtCompound nbt) {
      this.blockInfoLists.clear();
      this.entities.clear();
      NbtList nbtList = nbt.getListOrEmpty("size");
      this.size = new Vec3i(nbtList.getInt(0, 0), nbtList.getInt(1, 0), nbtList.getInt(2, 0));
      NbtList nbtList2 = nbt.getListOrEmpty("blocks");
      Optional<NbtList> optional = nbt.getList("palettes");
      if (optional.isPresent()) {
         for (int i = 0; i < optional.get().size(); i++) {
            this.loadPalettedBlockInfo(blockLookup, optional.get().getListOrEmpty(i), nbtList2);
         }
      } else {
         this.loadPalettedBlockInfo(blockLookup, nbt.getListOrEmpty("palette"), nbtList2);
      }

      nbt.getListOrEmpty("entities").streamCompounds().forEach(nbtx -> {
         NbtList nbtListx = nbtx.getListOrEmpty("pos");
         Vec3d vec3d = new Vec3d(nbtListx.getDouble(0, 0.0), nbtListx.getDouble(1, 0.0), nbtListx.getDouble(2, 0.0));
         NbtList nbtList2x = nbtx.getListOrEmpty("blockPos");
         BlockPos blockPos = new BlockPos(nbtList2x.getInt(0, 0), nbtList2x.getInt(1, 0), nbtList2x.getInt(2, 0));
         nbtx.getCompound("nbt").ifPresent(blockEntityNbt -> this.entities.add(new StructureTemplate.StructureEntityInfo(vec3d, blockPos, blockEntityNbt)));
      });
   }

   private void loadPalettedBlockInfo(RegistryEntryLookup<Block> blockLookup, NbtList palette, NbtList blocks) {
      StructureTemplate.Palette palette2 = new StructureTemplate.Palette();

      for (int i = 0; i < palette.size(); i++) {
         palette2.set(NbtHelper.toBlockState(blockLookup, palette.getCompoundOrEmpty(i)), i);
      }

      List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
      List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
      blocks.streamCompounds().forEach(nbt -> {
         NbtList nbtList = nbt.getListOrEmpty("pos");
         BlockPos blockPos = new BlockPos(nbtList.getInt(0, 0), nbtList.getInt(1, 0), nbtList.getInt(2, 0));
         BlockState blockState = palette2.getState(nbt.getInt("state", 0));
         NbtCompound nbtCompound = nbt.getCompound("nbt").orElse(null);
         StructureTemplate.StructureBlockInfo structureBlockInfo = new StructureTemplate.StructureBlockInfo(blockPos, blockState, nbtCompound);
         categorize(structureBlockInfo, list, list2, list3);
      });
      List<StructureTemplate.StructureBlockInfo> list4 = combineSorted(list, list2, list3);
      this.blockInfoLists.add(new StructureTemplate.PalettedBlockInfoList(list4));
   }

   private NbtList createNbtIntList(int... ints) {
      NbtList nbtList = new NbtList();

      for (int i : ints) {
         nbtList.add(NbtInt.of(i));
      }

      return nbtList;
   }

   private NbtList createNbtDoubleList(double... doubles) {
      NbtList nbtList = new NbtList();

      for (double d : doubles) {
         nbtList.add(NbtDouble.of(d));
      }

      return nbtList;
   }

   public static JigsawBlockEntity.Joint readJoint(NbtCompound nbt, BlockState state) {
      return nbt.<JigsawBlockEntity.Joint>get("joint", JigsawBlockEntity.Joint.CODEC).orElseGet(() -> getJointFromFacing(state));
   }

   public static JigsawBlockEntity.Joint getJointFromFacing(BlockState state) {
      return JigsawBlock.getFacing(state).getAxis().isHorizontal() ? JigsawBlockEntity.Joint.ALIGNED : JigsawBlockEntity.Joint.ROLLABLE;
   }

   public record JigsawBlockInfo(
      StructureTemplate.StructureBlockInfo info,
      JigsawBlockEntity.Joint jointType,
      Identifier name,
      RegistryKey<StructurePool> pool,
      Identifier target,
      int placementPriority,
      int selectionPriority
   ) {

      public static StructureTemplate.JigsawBlockInfo of(StructureTemplate.StructureBlockInfo structureBlockInfo) {
         NbtCompound nbtCompound = Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null");
         return new StructureTemplate.JigsawBlockInfo(
            structureBlockInfo,
            StructureTemplate.readJoint(nbtCompound, structureBlockInfo.state()),
            nbtCompound.<Identifier>get("name", Identifier.CODEC).orElse(JigsawBlockEntity.DEFAULT_NAME),
            nbtCompound.<RegistryKey<StructurePool>>get("pool", JigsawBlockEntity.STRUCTURE_POOL_KEY_CODEC).orElse(StructurePools.EMPTY),
            nbtCompound.<Identifier>get("target", Identifier.CODEC).orElse(JigsawBlockEntity.DEFAULT_NAME),
            nbtCompound.getInt("placement_priority", 0),
            nbtCompound.getInt("selection_priority", 0)
         );
      }

      @Override
      public String toString() {
         return String.format(
            Locale.ROOT,
            "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>",
            this.info.pos,
            this.info.state,
            this.name,
            this.pool.getValue(),
            this.target,
            this.placementPriority,
            this.selectionPriority,
            this.info.nbt
         );
      }

      public StructureTemplate.JigsawBlockInfo withInfo(StructureTemplate.StructureBlockInfo structureBlockInfo) {
         return new StructureTemplate.JigsawBlockInfo(
            structureBlockInfo, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority
         );
      }
   }

   static class Palette implements Iterable<BlockState> {
      public static final BlockState AIR = Blocks.AIR.getDefaultState();
      private final IdList<BlockState> ids = new IdList<>(16);
      private int currentIndex;

      public int getId(BlockState state) {
         int i = this.ids.getRawId(state);
         if (i == -1) {
            i = this.currentIndex++;
            this.ids.set(state, i);
         }

         return i;
      }

      public @Nullable BlockState getState(int id) {
         BlockState blockState = this.ids.get(id);
         return blockState == null ? AIR : blockState;
      }

      @Override
      public Iterator<BlockState> iterator() {
         return this.ids.iterator();
      }

      public void set(BlockState state, int id) {
         this.ids.set(state, id);
      }
   }

   public static final class PalettedBlockInfoList {
      private final List<StructureTemplate.StructureBlockInfo> infos;
      private final Map<Block, List<StructureTemplate.StructureBlockInfo>> blockToInfos = Maps.newHashMap();
      private @Nullable List<StructureTemplate.JigsawBlockInfo> jigsawBlockInfos;

      PalettedBlockInfoList(List<StructureTemplate.StructureBlockInfo> infos) {
         this.infos = infos;
      }

      public List<StructureTemplate.JigsawBlockInfo> getOrCreateJigsawBlockInfos() {
         if (this.jigsawBlockInfos == null) {
            this.jigsawBlockInfos = this.getAllOf(Blocks.JIGSAW).stream().map(StructureTemplate.JigsawBlockInfo::of).toList();
         }

         return this.jigsawBlockInfos;
      }

      public List<StructureTemplate.StructureBlockInfo> getAll() {
         return this.infos;
      }

      public List<StructureTemplate.StructureBlockInfo> getAllOf(Block block) {
         return this.blockToInfos.computeIfAbsent(block, block2 -> this.infos.stream().filter(info -> info.state.isOf(block2)).collect(Collectors.toList()));
      }
   }

   public record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable NbtCompound nbt) {

      @Override
      public String toString() {
         return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
      }
   }

   public static class StructureEntityInfo {
      public final Vec3d pos;
      public final BlockPos blockPos;
      public final NbtCompound nbt;

      public StructureEntityInfo(Vec3d pos, BlockPos blockPos, NbtCompound nbt) {
         this.pos = pos;
         this.blockPos = blockPos;
         this.nbt = nbt;
      }
   }
}
