package com.github.manolo8.darkbot.core.objects.facades;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.swf.PairArray;
import com.github.manolo8.darkbot.core.utils.ByteUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.github.manolo8.darkbot.Main.API;

public class SettingsProxy extends Updatable implements eu.darkbot.api.API.Singleton {

    private final Character[] keycodes = new Character[KeyBind.values().length];
    private final PairArray keycodesDictionary = PairArray.ofDictionary().setAutoUpdatable(true);

    /**
     * Get {@link Character} associated with given {@link KeyBind}.
     * Returns null if keybind is not assigned or doesnt exists.
     */
    @Nullable
    public Character getCharCode(KeyBind keyBind) {
        return keycodes[keyBind.ordinal()];
    }

    public boolean pressKeybind(KeyBind keyBind) {
        Character charCode = getCharCode(Objects.requireNonNull(keyBind, "KeyBind is null!"));
        if (charCode == null) return false;

        API.keyboardClick(charCode);
        return true;
    }

    public Optional<Character> getCharacterOf(KeyBind keyBind) {
        return Optional.ofNullable(getCharCode(keyBind));
    }

    public KeyBind getAtChar(Character c) {
        if (c == null) return null;

        for (int i = 0; i < keycodes.length; i++)
            if (c == keycodes[i])
                return KeyBind.of(i);

        return null;
    }

    @Nullable
    @Deprecated
    public KeyBind getKeyBind(Character ch) {
        return getAtChar(ch);
    }

    @Override
    public void update() {
        long data = API.readMemoryLong(address + 48) & ByteUtils.ATOM_MASK;

        PairArray keycodesDictionary = this.keycodesDictionary;
        keycodesDictionary.update(API.readMemoryLong(data + 240));

        Character[] keycodes = this.keycodes;
        int length = keycodes.length;
        for (int i = 0; i < length && i < keycodesDictionary.getSize(); i++) {
            // int vector size
            int arrSize = API.readMemoryInt(keycodesDictionary.getPtr(i) + 64);
            if (arrSize <= 0) keycodes[i] = null;

            //read first encounter in int vector
            int keycode = API.readMemoryInt(keycodesDictionary.getPtr(i), 48, 4);
            keycodes[i] = keycode <= 0 || keycode > 222 ? null : (char) keycode;
        }
    }

    public enum KeyBind {
        SLOTBAR_1(SlotBarsProxy.Type.DEFAULT_BAR, 0),
        SLOTBAR_2(SlotBarsProxy.Type.DEFAULT_BAR, 1),
        SLOTBAR_3(SlotBarsProxy.Type.DEFAULT_BAR, 2),
        SLOTBAR_4(SlotBarsProxy.Type.DEFAULT_BAR, 3),
        SLOTBAR_5(SlotBarsProxy.Type.DEFAULT_BAR, 4),
        SLOTBAR_6(SlotBarsProxy.Type.DEFAULT_BAR, 5),
        SLOTBAR_7(SlotBarsProxy.Type.DEFAULT_BAR, 6),
        SLOTBAR_8(SlotBarsProxy.Type.DEFAULT_BAR, 7),
        SLOTBAR_9(SlotBarsProxy.Type.DEFAULT_BAR, 8),
        SLOTBAR_0(SlotBarsProxy.Type.DEFAULT_BAR, 9),
        PREMIUM_1(SlotBarsProxy.Type.PREMIUM_BAR, 0),
        PREMIUM_2(SlotBarsProxy.Type.PREMIUM_BAR, 1),
        PREMIUM_3(SlotBarsProxy.Type.PREMIUM_BAR, 2),
        PREMIUM_4(SlotBarsProxy.Type.PREMIUM_BAR, 3),
        PREMIUM_5(SlotBarsProxy.Type.PREMIUM_BAR, 4),
        PREMIUM_6(SlotBarsProxy.Type.PREMIUM_BAR, 5),
        PREMIUM_7(SlotBarsProxy.Type.PREMIUM_BAR, 6),
        PREMIUM_8(SlotBarsProxy.Type.PREMIUM_BAR, 7),
        PREMIUM_9(SlotBarsProxy.Type.PREMIUM_BAR, 8),
        JUMP_GATE,
        TOGGLE_CONFIG,
        ATTACK_LASER,
        ATTACK_ROCKET,
        ACTIVE_PET,
        PET_GUARD_MODE,
        PET_COMBO_REPAIR,
        LOGOUT,
        TOGGLE_WINDOWS,
        TOGGLE_MONITORING,
        ZOOM_IN,
        ZOOM_OUT,
        FOCUS_CHAT,
        TOGGLE_CATEGORYBAR,
        PREMIUM_0(SlotBarsProxy.Type.PREMIUM_BAR, 9),
        TOGGLE_PRO_ACTION;

        private final SlotBarsProxy.Type type;
        private final int slotIdx;

        KeyBind() {
            this(null, -1);
        }

        KeyBind(SlotBarsProxy.Type type, int slotIdx) {
            this.type = type;
            this.slotIdx = slotIdx;
        }

        public static KeyBind of(int index) {
            if (index < 0 || index >= values().length) return null;
            return values()[index];
        }

        public static KeyBind of(SlotBarsProxy.Type slotType, int slotNumber) {
            return KeyBind.valueOf((slotType == SlotBarsProxy.Type.PREMIUM_BAR ? "PREMIUM_" : "SLOTBAR_") + slotNumber % 10);
        }

        public SlotBarsProxy.Type getType() {
            return type;
        }

        public int getSlotIdx() {
            return slotIdx;
        }
    }
}
