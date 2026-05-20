package net.minecraft.util.packrat;

import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public abstract class ParsingStateImpl<S> implements ParsingState<S> {
   private ParsingStateImpl.@Nullable MemoizedData[] memoStack = new ParsingStateImpl.MemoizedData[256];
   private final ParseErrorList<S> errors;
   private final ParseResults results = new ParseResults();
   private ParsingStateImpl.@Nullable Cutter[] cutters = new ParsingStateImpl.Cutter[16];
   private int topCutterIndex;
   private final ParsingStateImpl<S>.ErrorSuppressing errorSuppressingState = new ParsingStateImpl.ErrorSuppressing();

   protected ParsingStateImpl(ParseErrorList<S> errors) {
      this.errors = errors;
   }

   @Override
   public ParseResults getResults() {
      return this.results;
   }

   @Override
   public ParseErrorList<S> getErrors() {
      return this.errors;
   }

   @Override
   public <T> @Nullable T parse(ParsingRuleEntry<S, T> rule) {
      int i = this.getCursor();
      ParsingStateImpl.MemoizedData memoizedData = this.pushMemoizedData(i);
      int j = memoizedData.get(rule.getSymbol());
      if (j != -1) {
         ParsingStateImpl.MemoizedValue<T> memoizedValue = memoizedData.get(j);
         if (memoizedValue != null) {
            if (memoizedValue == ParsingStateImpl.MemoizedValue.EMPTY) {
               return null;
            }

            this.setCursor(memoizedValue.markAfterParse);
            return memoizedValue.value;
         }
      } else {
         j = memoizedData.push(rule.getSymbol());
      }

      T object = rule.getRule().parse(this);
      ParsingStateImpl.MemoizedValue<T> memoizedValue2;
      if (object == null) {
         memoizedValue2 = ParsingStateImpl.MemoizedValue.empty();
      } else {
         int k = this.getCursor();
         memoizedValue2 = new ParsingStateImpl.MemoizedValue<>(object, k);
      }

      memoizedData.put(j, memoizedValue2);
      return object;
   }

   private ParsingStateImpl.MemoizedData pushMemoizedData(int cursor) {
      int i = this.memoStack.length;
      if (cursor >= i) {
         int j = Util.nextCapacity(i, cursor + 1);
         ParsingStateImpl.MemoizedData[] memoizedDatas = new ParsingStateImpl.MemoizedData[j];
         System.arraycopy(this.memoStack, 0, memoizedDatas, 0, i);
         this.memoStack = memoizedDatas;
      }

      ParsingStateImpl.MemoizedData memoizedData = this.memoStack[cursor];
      if (memoizedData == null) {
         memoizedData = new ParsingStateImpl.MemoizedData();
         this.memoStack[cursor] = memoizedData;
      }

      return memoizedData;
   }

   @Override
   public Cut pushCutter() {
      int i = this.cutters.length;
      if (this.topCutterIndex >= i) {
         int j = Util.nextCapacity(i, this.topCutterIndex + 1);
         ParsingStateImpl.Cutter[] cutters = new ParsingStateImpl.Cutter[j];
         System.arraycopy(this.cutters, 0, cutters, 0, i);
         this.cutters = cutters;
      }

      int j = this.topCutterIndex++;
      ParsingStateImpl.Cutter cutter = this.cutters[j];
      if (cutter == null) {
         cutter = new ParsingStateImpl.Cutter();
         this.cutters[j] = cutter;
      } else {
         cutter.reset();
      }

      return cutter;
   }

   @Override
   public void popCutter() {
      this.topCutterIndex--;
   }

   @Override
   public ParsingState<S> getErrorSuppressingState() {
      return this.errorSuppressingState;
   }

   static class Cutter implements Cut {
      private boolean cut;

      @Override
      public void cut() {
         this.cut = true;
      }

      @Override
      public boolean isCut() {
         return this.cut;
      }

      public void reset() {
         this.cut = false;
      }
   }

   class ErrorSuppressing implements ParsingState<S> {
      private final ParseErrorList<S> errors = new ParseErrorList.Noop<>();

      @Override
      public ParseErrorList<S> getErrors() {
         return this.errors;
      }

      @Override
      public ParseResults getResults() {
         return ParsingStateImpl.this.getResults();
      }

      @Override
      public <T> @Nullable T parse(ParsingRuleEntry<S, T> rule) {
         return ParsingStateImpl.this.parse(rule);
      }

      @Override
      public S getReader() {
         return ParsingStateImpl.this.getReader();
      }

      @Override
      public int getCursor() {
         return ParsingStateImpl.this.getCursor();
      }

      @Override
      public void setCursor(int cursor) {
         ParsingStateImpl.this.setCursor(cursor);
      }

      @Override
      public Cut pushCutter() {
         return ParsingStateImpl.this.pushCutter();
      }

      @Override
      public void popCutter() {
         ParsingStateImpl.this.popCutter();
      }

      @Override
      public ParsingState<S> getErrorSuppressingState() {
         return this;
      }
   }

   static class MemoizedData {
      public static final int SIZE_PER_SYMBOL = 2;
      private static final int MISSING = -1;
      private Object[] values = new Object[16];
      private int top;

      public int get(Symbol<?> symbol) {
         for (int i = 0; i < this.top; i += 2) {
            if (this.values[i] == symbol) {
               return i;
            }
         }

         return -1;
      }

      public int push(Symbol<?> symbol) {
         int i = this.top;
         this.top += 2;
         int j = i + 1;
         int k = this.values.length;
         if (j >= k) {
            int l = Util.nextCapacity(k, j + 1);
            Object[] objects = new Object[l];
            System.arraycopy(this.values, 0, objects, 0, k);
            this.values = objects;
         }

         this.values[i] = symbol;
         return i;
      }

      public <T> ParsingStateImpl.@Nullable MemoizedValue<T> get(int index) {
         return (ParsingStateImpl.MemoizedValue<T>)this.values[index + 1];
      }

      public void put(int index, ParsingStateImpl.MemoizedValue<?> value) {
         this.values[index + 1] = value;
      }
   }

   record MemoizedValue<T>(@Nullable T value, int markAfterParse) {
      public static final ParsingStateImpl.MemoizedValue<?> EMPTY = new ParsingStateImpl.MemoizedValue(null, -1);

      public static <T> ParsingStateImpl.MemoizedValue<T> empty() {
         return (ParsingStateImpl.MemoizedValue<T>)EMPTY;
      }
   }
}
