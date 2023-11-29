package cpu;

import SistemaConcorrente.SistemaConcorrente;
import memory.GerenciaMemoria;
import process.GerenciaProcessos;
import process.Interrupts;
import process.PCB;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class SysCallHandling extends Thread{
    public Semaphore iosem;
    public ArrayList<Integer> processId;
    private VM vm;
    private GerenciaProcessos pm = GerenciaProcessos.getInstance();
    private GerenciaMemoria mm = GerenciaMemoria.getInstance();
    public void setVM(VM _vm){
        vm = _vm;
    }
    public void run() {   // apenas avisa - todas interrupcoes neste momento finalizam o programa
        iosem = new Semaphore(1);
        processId = new ArrayList<Integer>();
        while(true){
            try {
                iosem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                // handle the exception...
                // For example consider calling Thread.currentThread().interrupt(); here.
            }

            if(pm.blocked.size() > 0){
                PCB pcb = pm.blocked.get(0);
                //System.out.println("                                               Chamada de Sistema com op  /  par:  "+ pcb.r[8] + " / " + pcb.r[9]);
                if(pcb.r[8] == 1){

                    int r9 = (mm.translateLogicalIndexToFisical(pm.blocked.get(0).memAlo[0], 0)) + pm.blocked.get(0).r[9];
                    System.out.println("TRAP: Processo de id "+ pm.blocked.get(0).id + " solicitando dados");
                    System.out.println("Digite um numero inteiro: ");
                    //Thread.sleep(2000);
                    Scanner sc = new Scanner(System.in);
                    int op = sc.nextInt();
                    vm.m[r9].p = op;
                    vm.cpu.irpt = Interrupts.intIO;
                    processId.remove(Integer.valueOf(pcb.id));
                    vm.cpu.run();
                }
                else if(pcb.r[8]  == 2){
                    int r9 = (mm.translateLogicalIndexToFisical(pm.blocked.get(0).memAlo[0], 0)) + pm.blocked.get(0).r[9];
                    System.out.println("TRAP: Mostrando na tela:");
                    System.out.println(vm.m[r9].p);
                    vm.cpu.irpt = Interrupts.intIO;
                    processId.remove(Integer.valueOf(pcb.id));
                }


            }
            iosem.release();
        }
    }
}