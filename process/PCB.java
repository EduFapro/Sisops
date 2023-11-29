package process;

import memory.GerenciaMemoria;

// GERENTE DE PROCESSOS
public class PCB {
    public int id;
    public int programCounter = 0;
    public int[] memAlo;
    public int memLimit;
    public int r[];

    public PCB(int id, int memAlo[], int memLimit){
        this.memAlo = memAlo;
        this.id = id;
        this.memLimit = memLimit;
        r = new int[10];
        programCounter = GerenciaMemoria.getInstance().translateLogicalIndexToFisical(memAlo[0], 0);
    }
}