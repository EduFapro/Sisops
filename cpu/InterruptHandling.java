package cpu;

import process.GerenciaProcessos;
import process.Interrupts;
import process.PCB;

import java.util.ArrayList;

import static cpu.CPU.changeContext;
import static process.Interrupts.intInstrucaoInvalida;

public class InterruptHandling {
    private GerenciaProcessos pm = GerenciaProcessos.getInstance();
    private VM vm = VM.getInstance();

    public void handle(Interrupts irpt, int pc) {   // apenas avisa - todas interrupcoes neste momento finalizam o programa
        System.out.println("                                               Interrupcao "+ irpt+ "   pc: "+pc);
        switch(irpt){
            case intSTOP:
                System.out.println("Interrupcao: O programa chegou ao fim");
                pm.desalocaProcesso(pm.running.get(0).id);
                //pm.running.remove(pm.running.get(0).id);
                if(pm.ready.size() > 0){
                    changeContext();
                    vm.cpu.run();
                }
                break;
            case intEnderecoInvalido:
                System.out.println("Interrupcao: Acesso a endereco de memoria invalido");
                pm.desalocaProcesso(pm.running.get(0).id);
                break;
            case intInstrucaoInvalida:
                System.out.println("Interrupcao: Instrucao de programa invalida");
                pm.desalocaProcesso(pm.running.get(0).id);
                break;
            case intOverflow:
                System.out.println("Interrupcao Overflow");
                pm.desalocaProcesso(pm.running.get(0).id);
                break;
            case intEscl:
                //System.out.println("Interrupcao Escalonamento");
                //System.out.println(pm.running.get(0).id);

                //vm.cpu.setContext(0, vm.tamMem - 1, pm.running.get(0).memAlo, pm.running.get(0).programCounter, pm.running.get(0).r);
                changeContext();
                //System.out.println(pm.running.get(0).programCounter);
                break;
            case intIO:
                //System.out.println("testeio 1");
                ArrayList<PCB> aux = new ArrayList<PCB>();
                pm.ready.add(pm.blocked.get(0));
                pm.blocked.remove(0);
                for(PCB auxp: pm.blocked){
                    aux.add(auxp);
                }
                pm.blocked = aux;
                vm.cpu.irpt = Interrupts.noInterrupt;
                //System.out.println("testeio 2");
                changeContext();
                break;
        }
    }
}