package com.github.unidbg.unwind;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.AbstractARMDebugger;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnicornPointer;
import de.fearlesstobi.demangler.Demangler;

public abstract class Unwinder {

    public abstract Frame createFrame(UnicornPointer ip, UnicornPointer fp);

    protected abstract Frame unw_step(Emulator<?> emulator, Frame frame);

    protected abstract String getBaseFormat();

    public final void unwind(Emulator<?> emulator) {
        Memory memory = emulator.getMemory();
        String maxLengthSoName = memory.getMaxLengthLibraryName();
        boolean hasTrace = false;

        Frame frame = null;
        while((frame = unw_step(emulator, frame)) != null) {
            if (frame.isFinish()) {
                if (!hasTrace) {
                    System.out.println("Decode backtrace finish");
                }
                return;
            }

            Module module = AbstractARMDebugger.findModuleByAddress(emulator, frame.ip.peer);
            hasTrace = true;
            StringBuilder sb = new StringBuilder();
            if (module != null) {
                sb.append(String.format(getBaseFormat(), module.base));
                sb.append(String.format("[%" + maxLengthSoName.length() + "s]", module.name));
                sb.append(String.format("[0x%0" + Long.toHexString(memory.getMaxSizeOfLibrary()).length() + "x]", frame.ip.peer - module.base));

                Symbol symbol = module.findNearestSymbolByAddress(frame.ip.peer);
                if (symbol != null) {
                    sb.append(" ").append(Demangler.parse(symbol.getName())).append(" + 0x").append(Long.toHexString(frame.ip.peer - symbol.getAddress()));
                }
            } else {
                sb.append(String.format(getBaseFormat(), 0));
                sb.append(String.format("[%" + maxLengthSoName.length() + "s]", "0x" + Long.toHexString(frame.ip.peer)));
                sb.append(String.format("[0x%0" + Long.toHexString(memory.getMaxSizeOfLibrary()).length() + "x]", frame.ip.peer - 0xfffe0000L));
            }
            System.out.println(sb);
        }

        if (!hasTrace) {
            System.err.println("Decode backtrace failed.");
        }
    }

}
