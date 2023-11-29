package cpu;

import memory.Memory;
import memory.Word;

public class VM {
    private static VM instance;
    public int tamMem;
    public Word[] m;
    public Memory mem;
    public CPU cpu;


    public VM(InterruptHandling ih, SysCallHandling sysCall) {
        tamMem = 1024;
        mem = new Memory(tamMem);
        m = mem.m;
        cpu = new CPU(mem, ih, sysCall, true); // true liga debug
    }

    public static synchronized VM getInstance(InterruptHandling ih, SysCallHandling sysCall) {
        if (instance == null) {
            instance = new VM(ih, sysCall);
        }
        return instance;
    }

    public static VM getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance not initialized yet");
        }
        return instance;
    }

}