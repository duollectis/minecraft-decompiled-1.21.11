package net.minecraft.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface BlockView extends HeightLimitView, FabricBlockView {
   @Nullable BlockEntity getBlockEntity(BlockPos pos);

   default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
      BlockEntity blockEntity = this.getBlockEntity(pos);
      return blockEntity != null && blockEntity.getType() == type ? Optional.of((T)blockEntity) : Optional.empty();
   }

   BlockState getBlockState(BlockPos pos);

   FluidState getFluidState(BlockPos pos);

   default int getLuminance(BlockPos pos) {
      return this.getBlockState(pos).getLuminance();
   }

   default Stream<BlockState> getStatesInBox(Box box) {
      return BlockPos.stream(box).map(this::getBlockState);
   }

   default BlockHitResult raycast(BlockStateRaycastContext context) {
      return raycast(
         context.getStart(),
         context.getEnd(),
         context,
         (innerContext, pos) -> {
            BlockState blockState = this.getBlockState(pos);
            Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
            return innerContext.getStatePredicate().test(blockState)
               ? new BlockHitResult(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()), false)
               : null;
         },
         innerContext -> {
            Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
            return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
         }
      );
   }

   default BlockHitResult raycast(RaycastContext context) {
      return raycast(context.getStart(), context.getEnd(), context, (innerContext, pos) -> {
         BlockState blockState = this.getBlockState(pos);
         FluidState fluidState = this.getFluidState(pos);
         Vec3d vec3d = innerContext.getStart();
         Vec3d vec3d2 = innerContext.getEnd();
         VoxelShape voxelShape = innerContext.getBlockShape(blockState, this, pos);
         BlockHitResult blockHitResult = this.raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);
         VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, this, pos);
         BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
         double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult.getPos());
         double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
         return d <= e ? blockHitResult : blockHitResult2;
      }, innerContext -> {
         Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
         return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
      });
   }

   default @Nullable BlockHitResult raycastBlock(Vec3d start, Vec3d end, BlockPos pos, VoxelShape shape, BlockState state) {
      BlockHitResult blockHitResult = shape.raycast(start, end, pos);
      if (blockHitResult != null) {
         BlockHitResult blockHitResult2 = state.getRaycastShape(this, pos).raycast(start, end, pos);
         if (blockHitResult2 != null && blockHitResult2.getPos().subtract(start).lengthSquared() < blockHitResult.getPos().subtract(start).lengthSquared()) {
            return blockHitResult.withSide(blockHitResult2.getSide());
         }
      }

      return blockHitResult;
   }

   default double getDismountHeight(VoxelShape blockCollisionShape, Supplier<VoxelShape> belowBlockCollisionShapeGetter) {
      if (!blockCollisionShape.isEmpty()) {
         return blockCollisionShape.getMax(Direction.Axis.Y);
      } else {
         double d = belowBlockCollisionShapeGetter.get().getMax(Direction.Axis.Y);
         return d >= 1.0 ? d - 1.0 : Double.NEGATIVE_INFINITY;
      }
   }

   default double getDismountHeight(BlockPos pos) {
      return this.getDismountHeight(this.getBlockState(pos).getCollisionShape(this, pos), () -> {
         BlockPos blockPos2 = pos.down();
         return this.getBlockState(blockPos2).getCollisionShape(this, blockPos2);
      });
   }

   static <T, C> T raycast(Vec3d start, Vec3d end, C context, BiFunction<C, BlockPos, @Nullable T> blockHitFactory, Function<C, T> missFactory) {
      if (start.equals(end)) {
         return missFactory.apply(context);
      } else {
         double d = MathHelper.lerp(-1.0E-7, end.x, start.x);
         double e = MathHelper.lerp(-1.0E-7, end.y, start.y);
         double f = MathHelper.lerp(-1.0E-7, end.z, start.z);
         double g = MathHelper.lerp(-1.0E-7, start.x, end.x);
         double h = MathHelper.lerp(-1.0E-7, start.y, end.y);
         double i = MathHelper.lerp(-1.0E-7, start.z, end.z);
         int j = MathHelper.floor(g);
         int k = MathHelper.floor(h);
         int l = MathHelper.floor(i);
         BlockPos.Mutable mutable = new BlockPos.Mutable(j, k, l);
         T object = blockHitFactory.apply(context, mutable);
         if (object != null) {
            return object;
         } else {
            double m = d - g;
            double n = e - h;
            double o = f - i;
            int p = MathHelper.sign(m);
            int q = MathHelper.sign(n);
            int r = MathHelper.sign(o);
            double s = p == 0 ? Double.MAX_VALUE : p / m;
            double t = q == 0 ? Double.MAX_VALUE : q / n;
            double u = r == 0 ? Double.MAX_VALUE : r / o;
            double v = s * (p > 0 ? 1.0 - MathHelper.fractionalPart(g) : MathHelper.fractionalPart(g));
            double w = t * (q > 0 ? 1.0 - MathHelper.fractionalPart(h) : MathHelper.fractionalPart(h));
            double x = u * (r > 0 ? 1.0 - MathHelper.fractionalPart(i) : MathHelper.fractionalPart(i));

            while (v <= 1.0 || w <= 1.0 || x <= 1.0) {
               if (v < w) {
                  if (v < x) {
                     j += p;
                     v += s;
                  } else {
                     l += r;
                     x += u;
                  }
               } else if (w < x) {
                  k += q;
                  w += t;
               } else {
                  l += r;
                  x += u;
               }

               T object2 = blockHitFactory.apply(context, mutable.set(j, k, l));
               if (object2 != null) {
                  return object2;
               }
            }

            return missFactory.apply(context);
         }
      }
   }

   static boolean collectCollisionsBetween(Vec3d from, Vec3d to, Box box, BlockView.CollisionVisitor visitor) {
      Vec3d vec3d = to.subtract(from);
      if (vec3d.lengthSquared() < MathHelper.square(1.0E-5F)) {
         for (BlockPos blockPos : BlockPos.iterate(box)) {
            if (!visitor.visit(blockPos, 0)) {
               return false;
            }
         }

         return true;
      } else {
         LongSet longSet = new LongOpenHashSet();

         for (BlockPos blockPos2 : BlockPos.iterateCollisionOrder(box.offset(vec3d.multiply(-1.0)), vec3d)) {
            if (!visitor.visit(blockPos2, 0)) {
               return false;
            }

            longSet.add(blockPos2.asLong());
         }

         int i = collectCollisionsBetween(longSet, vec3d, box, visitor);
         if (i < 0) {
            return false;
         } else {
            for (BlockPos blockPos3 : BlockPos.iterateCollisionOrder(box, vec3d)) {
               if (longSet.add(blockPos3.asLong()) && !visitor.visit(blockPos3, i + 1)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private static int collectCollisionsBetween(LongSet visited, Vec3d delta, Box box, BlockView.CollisionVisitor visitor) {
      double d = box.getLengthX();
      double e = box.getLengthY();
      double f = box.getLengthZ();
      Vec3i vec3i = method_73110(delta);
      Vec3d vec3d = box.getCenter();
      Vec3d vec3d2 = new Vec3d(vec3d.getX() + d * 0.5 * vec3i.getX(), vec3d.getY() + e * 0.5 * vec3i.getY(), vec3d.getZ() + f * 0.5 * vec3i.getZ());
      Vec3d vec3d3 = vec3d2.subtract(delta);
      int i = MathHelper.floor(vec3d3.x);
      int j = MathHelper.floor(vec3d3.y);
      int k = MathHelper.floor(vec3d3.z);
      int l = MathHelper.sign(delta.x);
      int m = MathHelper.sign(delta.y);
      int n = MathHelper.sign(delta.z);
      double g = l == 0 ? Double.MAX_VALUE : l / delta.x;
      double h = m == 0 ? Double.MAX_VALUE : m / delta.y;
      double o = n == 0 ? Double.MAX_VALUE : n / delta.z;
      double p = g * (l > 0 ? 1.0 - MathHelper.fractionalPart(vec3d3.x) : MathHelper.fractionalPart(vec3d3.x));
      double q = h * (m > 0 ? 1.0 - MathHelper.fractionalPart(vec3d3.y) : MathHelper.fractionalPart(vec3d3.y));
      double r = o * (n > 0 ? 1.0 - MathHelper.fractionalPart(vec3d3.z) : MathHelper.fractionalPart(vec3d3.z));
      int s = 0;

      while (p <= 1.0 || q <= 1.0 || r <= 1.0) {
         if (p < q) {
            if (p < r) {
               i += l;
               p += g;
            } else {
               k += n;
               r += o;
            }
         } else if (q < r) {
            j += m;
            q += h;
         } else {
            k += n;
            r += o;
         }

         Optional<Vec3d> optional = Box.raycast(i, j, k, i + 1, j + 1, k + 1, vec3d3, vec3d2);
         if (!optional.isEmpty()) {
            s++;
            Vec3d vec3d4 = optional.get();
            double t = MathHelper.clamp(vec3d4.x, i + 1.0E-5F, i + 1.0 - 1.0E-5F);
            double u = MathHelper.clamp(vec3d4.y, j + 1.0E-5F, j + 1.0 - 1.0E-5F);
            double v = MathHelper.clamp(vec3d4.z, k + 1.0E-5F, k + 1.0 - 1.0E-5F);
            int w = MathHelper.floor(t - d * vec3i.getX());
            int x = MathHelper.floor(u - e * vec3i.getY());
            int y = MathHelper.floor(v - f * vec3i.getZ());
            int z = s;

            for (BlockPos blockPos : BlockPos.iterateCollisionOrder(i, j, k, w, x, y, delta)) {
               if (visited.add(blockPos.asLong()) && !visitor.visit(blockPos, z)) {
                  return -1;
               }
            }
         }
      }

      return s;
   }

   private static Vec3i method_73110(Vec3d vec3d) {
      double d = Math.abs(Vec3d.X.dotProduct(vec3d));
      double e = Math.abs(Vec3d.Y.dotProduct(vec3d));
      double f = Math.abs(Vec3d.Z.dotProduct(vec3d));
      int i = vec3d.x >= 0.0 ? 1 : -1;
      int j = vec3d.y >= 0.0 ? 1 : -1;
      int k = vec3d.z >= 0.0 ? 1 : -1;
      if (d <= e && d <= f) {
         return new Vec3i(-i, -k, j);
      } else {
         return e <= f ? new Vec3i(k, -j, -i) : new Vec3i(-j, i, -k);
      }
   }

   @FunctionalInterface
   public interface CollisionVisitor {
      boolean visit(BlockPos pos, int version);
   }
}
