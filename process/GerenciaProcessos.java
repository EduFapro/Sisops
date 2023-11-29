package process;
import cpu.Opcode;
import cpu.VM;
import memory.GerenciaMemoria;
import memory.Word;

import java.util.ArrayList;

public class GerenciaProcessos {
    private static GerenciaProcessos instance;
    private static GerenciaMemoria mm = GerenciaMemoria.getInstance();
    private static VM vm;
    public ArrayList<PCB> pcbA;
    public ArrayList<PCB> running;
    public ArrayList<PCB> interrupted;
    public ArrayList<PCB> ready;
    public ArrayList<PCB> blocked;
    public int id = 0;

    public GerenciaProcessos(){
        pcbA = new ArrayList<PCB>();
        running = new ArrayList<PCB>();
        interrupted = new ArrayList<PCB>();
        ready = new ArrayList<PCB>();
        blocked = new ArrayList<PCB>();
    }

    public static synchronized GerenciaProcessos getInstance() {
        if (instance == null) {
            instance = new GerenciaProcessos();
        }
        return instance;
    }
    public boolean criaProcesso(Word[] w){
        PCB pcb;
        if(mm.allocable(w.length)){
            int[] memA = mm.alocate(w.length);
            pcb = new PCB(id, memA, w.length);
            pcbA.add(pcb);
            ready.add(pcb);
            //running.add(pcb);
            loadPrograms(w, memA[0], pcb);

        } else{
            System.out.println("Sem espaço na memoria");
            return false;
        }
        //System.out.println("Processo criado com id: "+(id));
        id++;
        return true;
    }

    private void loadPrograms(Word [] p, int indexPart, PCB pcb){
        int count = 0;
        int count2;
        int pag;
        for(int z = 0; z < pcb.memAlo.length; z++){
            count2 = 0;
            pag = pcb.memAlo[z];
            for(int x = (mm.translateLogicalIndexToFisical(pag, count2)); (x < mm.translateLogicalIndexToFisical(pag, pcb.memLimit)) && (x < (mm.translateLogicalIndexToFisical(pag, 0)+ mm.pageSize)); x++){
                if(count < pcb.memLimit){
                    vm.m[x].opc = p[count].opc;
                    vm.m[x].r1 = p[count].r1;
                    vm.m[x].r2 = p[count].r2;
                    vm.m[x].p = p[count].p;
                }
                count++;
                count2++;
            }
        }
    }

    public void desalocaProcesso(int id){
        for(PCB pcbs : pcbA){
            if(pcbs.id == id){
                mm.dealocate(pcbs.memAlo);
                pcbA.remove(pcbs);
                running.remove(0);
                cleanPartition(pcbs);
                System.out.println("Processo com ID "+id+" desalocado");
                return;
            }
        }
        System.out.println("Processo noo existe");
    }

    private void cleanPartition(PCB pcb){
        int count = 0;
        int endIni = mm.translateLogicalIndexToFisical(pcb.memAlo[0] , count);
        for(int x = endIni; x < (endIni + (GerenciaMemoria.getInstance().pageSize*pcb.memAlo.length)); x++){
            vm.m[x].opc = Opcode.___;
            vm.m[x].r1 = -1;
            vm.m[x].r2 = -1;
            vm.m[x].p = -1;
            count++;
        }
    }

    public PCB searchProcess(int id){
        for(PCB pcbs : pcbA){
            if(pcbs.id == id){
                return pcbs;
            }
        }
        return null;
    }

    public void dumpProcess(int id){
        boolean cntrl = false;
        for(PCB pcbs : pcbA){
            if(pcbs.id == id){
                System.out.println("ID: "+ pcbs.id);
                System.out.println("Indice de pagina: "+ pcbs.memAlo[0]);
                System.out.println("PC: "+ pcbs.programCounter);
                VM.getInstance().mem.dump(mm.translateLogicalIndexToFisical(pcbs.memAlo[0], 0), (mm.translateLogicalIndexToFisical(pcbs.memAlo[0], 0)) + pcbs.memLimit);
                cntrl = true;
            }
        }
        if(!cntrl) System.out.println("Processo nao existe");
    }

    public void setVm(VM vm) {
        GerenciaProcessos.vm = vm;
    }
}
