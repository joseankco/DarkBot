package com.github.manolo8.darkbot.core.objects.swf;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.utils.ByteUtils;
import com.github.manolo8.darkbot.core.utils.Lazy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.manolo8.darkbot.Main.API;

/**
 * Reads arrays with pair of values
 * Instead of EntryArray & Dictionary
 */
public abstract class PairArray extends Updatable implements SwfPtrCollection {
    private boolean autoUpdatable, ignoreEmpty = true;

    public int size;

    private Pair[] pairs = new Pair[0];
    private Map<String, Lazy<Long>> lazy = new HashMap<>();

    /**
     * Reads pairs of {@code Array} type
     */
    public static PairArray ofArray() {
        return new PairArray.Pairs();
    }

    /**
     * Reads pairs of {@code Dictionary} type
     */
    public static PairArray ofDictionary() {
        return new PairArray.Dictionary();
    }

    public PairArray setAutoUpdatable(boolean updatable) {
        this.autoUpdatable = updatable;
        return this;
    }

    public PairArray setIgnoreEmpty(boolean ignoreEmpty) {
        this.ignoreEmpty = ignoreEmpty;
        return this;
    }

    public void addLazy(String key, Consumer<Long> consumer) {
        this.lazy.computeIfAbsent(key, k -> new Lazy<>()).add(consumer);
    }

    public int getSize() {
        return Math.min(size, pairs.length);
    }

    public long getPtr(int idx) {
        return get(idx).value;
    }

    public long getPtr(String key) {
        return Optional.ofNullable(get(key))
                .map(pair -> pair.value).orElse(0L);
    }

    public Pair get(int idx) {
        return idx >= 0 && idx < size && idx < pairs.length ? pairs[idx] : null;
    }

    public Pair get(String key) {
        for (Pair pair : pairs) if (pair.key.equals(key)) return pair;
        return null;
    }

    public boolean hasKey(String key) {
        for (int i = 0; i < size && i < pairs.length; i++)
            if (pairs[i] != null && pairs[i].key != null && pairs[i].key.equals(key)) return true;
        return false;
    }

    public abstract void update();

    @Override
    public void update(long address) {
        super.update(address);
        if (autoUpdatable) update();
    }

    private static class Pairs extends PairArray {
        public boolean isInvalid(long addr) {
            return addr == 0;
        }

        public String getKey(long addr) {
            if (isInvalid(addr)) return null;
            String key = API.readMemoryString(addr);
            return key == null || key.trim().isEmpty() || key.equals("ERROR") ? null : key;
        }

        public void update() {
            if (super.lazy.isEmpty() && super.ignoreEmpty) return;

            size = API.readMemoryInt(address + 0x50);

            if (size < 0 || size > 1024) return;
            if (super.pairs.length != size) super.pairs = Arrays.copyOf(super.pairs, size);

            long table = API.readMemoryLong(address + 0x48) & ByteUtils.FIX;

            String key = null;
            for (int offset = 8, i = 0; offset < 8192 && i < size; offset += 8) {
                if (key == null && (key = getKey(API.readMemoryLong(table + offset) & ByteUtils.FIX)) == null) continue;

                long value = API.readMemoryLong(table + (offset += 8));
                if (isInvalid(value)) continue;

                if (super.pairs[i] == null) super.pairs[i] = new Pair();
                super.pairs[i++].set(key, value & ByteUtils.FIX);
                key = null;
            }
        }

    }

    private static class Dictionary extends PairArray {
        @Override
        public void update() {
            if (address == 0) return;

            long tableInfo = API.readMemoryLong(address + 32);

            if (tableInfo == 0) return;

            int  size   = API.readMemoryInt(tableInfo + 16);
            long table  = align8(API.readMemoryLong(tableInfo + 8));
            int  exp    = API.readMemoryInt(tableInfo + 20);
            int  length = (int) (Math.pow(2, exp) * 4);

            if (length > 4096 || length < 0 || size < 0 || size > 1024) return;

            if (super.pairs.length < size) super.pairs = Arrays.copyOf(super.pairs, size);

            byte[] bytes = API.readMemory(table, length);

            int i = 0;
            for (int offset = 0; offset < length && i < size; offset += 16) {
                long keyAddr = ByteUtils.getLong(bytes, offset) - 2, valAddr = ByteUtils.getLong(bytes, offset + 8) - 1;

                if (keyAddr == -2 || (valAddr >= -2 && valAddr <= 9)) continue;

                if (super.pairs[i] == null) super.pairs[i] = new Pair();
                super.pairs[i++].set(API.readMemoryString(keyAddr), valAddr);
            }
            this.size = i;

            super.lazy.entrySet().stream()
                    .filter(l -> l.getValue() != null && l.getValue().value != null
                            && l.getValue().value != 0 && !hasKey(l.getKey()))
                    .forEach(l -> l.getValue().send(0L));
        }

        private long align8(long value) {
            long aligned = value + 8 - (value & 0b1111);
            return aligned <= value ? aligned + 8 : aligned;
        }
    }

    public class Pair {
        public String key;
        public long value;

        private void set(String index, long value) {
            this.key   = index;
            this.value = value;

            Lazy<Long> l = lazy.get(index);
            if (l != null) l.send(value);
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    '}';
        }
    }
}
