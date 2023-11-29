package cpu;

import memory.GerenciaMemoria;
import memory.Memory;
import memory.Word;
import process.GerenciaProcessos;
import process.Interrupts;
import process.PCB;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import static SistemaConcorrente.SistemaConcorrente.limiteOverflow;

public class CPU extends Thread {
    private Semaphore semcpu;

    private int maxInt; // valores maximo e minimo para inteiros nesta cpu
    private int minInt;
    private int indexpart;			// característica do processador: contexto da CPU ...
    public int pc; 			// ... composto de program counter,
    private Word ir; 			// instruction register,
    public int[] reg;
    private int[] pag;      	// registradores da CPU
    public Interrupts irpt; 	// durante instrucao, interrupcao pode ser sinalizada
    private int base;   		// base e limite de acesso na memoria
    private int limite; // por enquanto toda memoria pode ser acessada pelo processo rodando
    public int cycles;
    public boolean flag;
    public int countex = 0;				// ATE AQUI: contexto da CPU - tudo que precisa sobre o estado de um processo para executa-lo
    // nas proximas versoes isto pode modificar

    private Memory mem;               // mem tem funcoes de dump e o array m de memória 'fisica'
    private Word[] m;                 // CPU acessa MEMORIA, guarda referencia a 'm'. m nao muda. semre será um array de palavras
    private static VM vm = VM.getInstance();
    private InterruptHandling ih;     // significa desvio para rotinas de tratamento de  Int - se int ligada, desvia
    private static SysCallHandling sysCall;  // significa desvio para tratamento de chamadas de sistema - trap
    public boolean debug;            // se true entao mostra cada instrucao em execucao
    private static GerenciaProcessos pm = GerenciaProcessos.getInstance();
    private static GerenciaMemoria mm = GerenciaMemoria.getInstance();

    public CPU(Memory _mem, InterruptHandling _ih, SysCallHandling _sysCall, boolean _debug) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
        maxInt =  32767;        // capacidade de representacao modelada
        minInt = -32767;        // se exceder deve gerar interrupcao de overflow
        mem = _mem;	            // usa mem para acessar funcoes auxiliares (dump)
        m = mem.m; 				// usa o atributo 'm' para acessar a memoria.
        reg = new int[10]; 		// aloca o espaço dos registradores - regs 8 e 9 usados somente para IO
        ih = _ih;               // aponta para rotinas de tratamento de int
        sysCall = _sysCall;     // aponta para rotinas de tratamento de chamadas de sistema
        debug =  _debug;        // se true, print da instrucao em execucao
        semcpu = new Semaphore(1);
    }

		/*
		private boolean legal(int e) {                             // todo acesso a memoria tem que ser verificado
			// ????
			return true;
		}
		*/

    // teste se houve overflow
    private boolean testOverflow(int v) {                       // toda operacao matematica deve avaliar se ocorre overflow
        if (v < limiteOverflow) {
            return false;
        };
        irpt = Interrupts.intOverflow;
        //System.out.println("Interrupção: Overflow");
        return true;
    }

    // testa se o endereco e invalido
		/*private boolean legal(int v){
			int endP = mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], v);
			int x;
			int runningLen = pm.running.get(0).memAlo.length;
			for(x = 0; x < runningLen; x++){
				int endLimit = (mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[x], 0)) + mm.pageSize;
				if( endP <= endLimit && endP >= (endLimit - mm.pageSize) ){
					System.out.println(pm.running.get(0).memAlo[x]);
				System.out.println(pm.running.get(0).memAlo.length);
				System.out.println(endP);
				System.out.println(endLimit);
					return true;
				}
				//System.out.println(mm.pageSize);
			}
			irpt = Interrupts.intEnderecoInvalido;

			return false;
		}*/

    private boolean legal(int pag, int v){

        int endP = mm.translateLogicalIndexToFisical(pag, v%mm.pageSize);
        int pagLength = pm.running.get(0).memAlo.length;
        //System.out.println(mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], 0)+"Test End");
        int endLimit = mm.translateLogicalIndexToFisical(pag, 0) + mm.pageSize;
        if( endP > endLimit ){
            irpt = Interrupts.intEnderecoInvalido;
            //System.out.println("Interrupção: Endereço Inválido");
            return false;
        }
        //System.out.println(endP);
        //System.out.println(endLimit);
        return true;
    }

    // testa se a instrucao e valida
    private boolean testInstrucaoInv(Opcode v){
        for(Opcode p: Opcode.values()){
            if(p == v){
                return false;
            }
        }
        //System.out.println("Interrupção: Instrução Inválida");
        irpt = Interrupts.intInstrucaoInvalida;
        return true;
    }

    // testa se o programa parou
    private boolean testParada(Opcode v){
        if(v == ir.opc.STOP){

            //System.out.println("Interrupção: Stop");
            irpt = Interrupts.intSTOP;
            return true;
        }
        return false;
    }

    public void setContext(int _base, int _limite, int[] _pag, int _indexpart, int _reg[]) {  // no futuro esta funcao vai ter que ser
        base = _base;                                          // expandida para setar todo contexto de execucao,
        limite = _limite;									   // agora,  setamos somente os registradores base,
        pc = mm.translateLogicalIndexToFisical(_pag[0], 0);
        pag = _pag;                                           // limite e pc (deve ser zero nesta versao)
        irpt = Interrupts.noInterrupt;
        indexpart = _indexpart;
        reg = _reg;
        flag = false;                    // reset da interrupcao registrada
    }

    public void run(){
        int count = 0;
        int paglim = (pc + mm.pageSize) -1 ; 		// execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente setado
        //System.out.println("Processo com id "+ pm.running.get(0).id + " executando");
        while (true) { 			// ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
            // --------------------------------------------------------------------------------------------------
            // FETCH

            try {
                semcpu.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                // handle the exception...
                // For example consider calling Thread.currentThread().interrupt(); here.
            }

            if(irpt == Interrupts.intIO){
                ih.handle(irpt,pc);
            }

            indexpart = pag[count];
            if(pc > paglim){
                count++;
                if(count >= pag.length) { break; }
                indexpart = pag[count];
                pc = mm.translateLogicalIndexToFisical(indexpart, 0);
            }
            if (legal(indexpart, pc)) { 	// pc valido
                ir = m[pc]; 	// <<<<<<<<<<<<           busca posicao da memoria apontada por pc, guarda em ir
                pm.running.get(0).programCounter = pc;
                if (debug) { System.out.print("                               pc: "+pc+"       exec: ");  mem.dump(ir); }
                // --------------------------------------------------------------------------------------------------
                // EXECUTA INSTRUCAO NO ir
                switch (ir.opc) {   // conforme o opcode (código de operação) executa

                    // Instrucoes de Busca e Armazenamento em Memoria
                    case LDI: // Rd ← k
                        reg[ir.r1] = ir.p;
                        pc++;

                        break;

                    case LDD: // Rd <- [A]
                        if (legal(indexpart, ir.p)) {
                            reg[ir.r1] = m[ir.p].p;
                            pc++;
                        }
                        break;

                    case LDX: // RD <- [RS] // NOVA
                        if (legal(indexpart, reg[ir.r2])) {
                            reg[ir.r1] = m[reg[ir.r2]].p;
                            pc++;
                        }
                        break;

                    case STD: // [A] ← Rs
                        if (legal(indexpart, ir.p)) {
                            m[(mm.translateLogicalIndexToFisical(indexpart, ir.p))].opc = Opcode.DATA;
                            m[(mm.translateLogicalIndexToFisical(indexpart, ir.p))].p = reg[ir.r1];
                            pc++;
                            //mem.dump(16, 48);
                        };
                        break;

                    case STX: // [Rd] ←Rs
                        if (legal(indexpart, reg[ir.r1])) {
                            m[(mm.translateLogicalIndexToFisical(indexpart, reg[ir.r1]))].opc = Opcode.DATA;
                            m[(mm.translateLogicalIndexToFisical(indexpart, reg[ir.r1]))].p = reg[ir.r2];
                            pc++;
                        };
                        break;

                    case MOVE: // RD <- RS
                        reg[ir.r1] = reg[ir.r2];
                        pc++;
                        break;

                    // Instrucoes Aritmeticas
                    case ADD: // Rd ← Rd + Rs
                        reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
                        testOverflow(reg[ir.r1]);
                        pc++;
                        break;

                    case ADDI: // Rd ← Rd + k
                        reg[ir.r1] = reg[ir.r1] + ir.p;
                        testOverflow(reg[ir.r1]);
                        pc++;
                        break;

                    case SUB: // Rd ← Rd - Rs
                        reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
                        testOverflow(reg[ir.r1]);
                        pc++;
                        break;

                    case SUBI: // RD <- RD - k // NOVA
                        reg[ir.r1] = reg[ir.r1] - ir.p;
                        testOverflow(reg[ir.r1]);
                        pc++;
                        break;

                    case MULT: // Rd <- Rd * Rs
                        reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
                        testOverflow(reg[ir.r1]);
                        pc++;
                        break;

                    // Instrucoes JUMP
                    case JMP: // PC <- k
                        pc = mm.translateLogicalIndexToFisical(indexpart, ir.p);
                        break;

                    case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                        if (reg[ir.r2] > 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,reg[ir.r1]);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIGK: // If RC > 0 then PC <- k else PC++
                        if (reg[ir.r2] > 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,ir.p);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPILK: // If RC < 0 then PC <- k else PC++
                        if (reg[ir.r2] < 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,ir.p);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIEK: // If RC = 0 then PC <- k else PC++
                        if (reg[ir.r2] == 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,ir.p);
                        } else {
                            pc++;
                        }
                        break;


                    case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
                        if (reg[ir.r2] < 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,reg[ir.r1]);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
                        if (reg[ir.r2] == 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,reg[ir.r1]);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIM: // PC <- [A]
                        pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
                        break;

                    case JMPIGM: // If RC > 0 then PC <- [A] else PC++
                        if (reg[ir.r2] > 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPILM: // If RC < 0 then PC <- k else PC++
                        if (reg[ir.r2] < 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIEM: // If RC = 0 then PC <- k else PC++
                        if (reg[ir.r2] == 0) {
                            pc = mm.translateLogicalIndexToFisical(indexpart,m[ir.p].p);
                        } else {
                            pc++;
                        }
                        break;

                    case JMPIGT: // If RS>RC then PC <- k else PC++
                        if (reg[ir.r1] > reg[ir.r2]) {
                            pc = mm.translateLogicalIndexToFisical(indexpart, ir.p);
                        } else {
                            pc++;
                        }
                        break;

                    // outras
                    case STOP: // por enquanto, para execucao
                        testParada(ir.opc);
                        break;

                    case DATA:
                        testInstrucaoInv(ir.opc);
                        break;

                    // Chamada de sistema
                    case TRAP:
                        irpt = Interrupts.intTrap;
                        pc++;

                        break;

                    // Inexistente
                    default:
                        testInstrucaoInv(ir.opc);
                        break;
                }
            }

            cycles++;
            VM.getInstance().cpu.semcpu.release();
            if(irpt == Interrupts.intTrap){
                trapCalling();            // <<<<< aqui desvia para rotina de chamada de sistema, no momento so temos IO
                irpt = Interrupts.noInterrupt;
            }
            if(cycles >= 5 ){
                irpt = Interrupts.intEscl;
                ih.handle(irpt,pc);
                count = 0;
                paglim = (pc + mm.pageSize) -1 ;
            }


            // --------------------------------------------------------------------------------------------------
            // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
            if (!(irpt == Interrupts.noInterrupt  || irpt == Interrupts.intTrap || irpt ==  Interrupts.intEscl)) {   // existe interrupção
                ih.handle(irpt,pc);                       // desvia para rotina de tratamento
                break; // break sai do loop da cpu
            }
        }  // FIM DO CICLO DE UMA INSTRUÇÃO
    }

    public void trapCalling(){
        VM.getInstance().cpu.flag = true;
        changeContext();
    }

    static void changeContext(){
        ArrayList<PCB> aux = new ArrayList<PCB>();
        if(pm.running.size() > 0 && vm.cpu.flag == false){
            pm.running.get(0).programCounter = vm.cpu.pc;
            pm.running.get(0).r = vm.cpu.reg;
            pm.ready.add(pm.running.get(0));
            pm.running.remove(0);
        } else if(pm.running.size() > 0 && vm.cpu.flag == true) {
            int r9 = (mm.translateLogicalIndexToFisical(pm.running.get(0).memAlo[0], 0)) + vm.cpu.reg[9];
            //System.out.println(vm.m[r9].p);
            pm.running.get(0).programCounter = vm.cpu.pc;
            pm.running.get(0).r = vm.cpu.reg;
            sysCall.processId.add(pm.running.get(0).id);
            pm.blocked.add(pm.running.get(0));
            pm.running.remove(0);

        }
        pm.running.add(pm.ready.get(0));
        pm.ready.remove(0);
        for(PCB auxp: pm.ready){
            aux.add(auxp);
            //System.out.println(auxp.id);
        }
        pm.ready = aux;
        if(!(vm.cpu.irpt == Interrupts.intSTOP)){
            vm.cpu.cycles = 0;
        }
        vm.cpu.setContext(0, vm.tamMem - 1, pm.running.get(0).memAlo, pm.running.get(0).memAlo[0], pm.running.get(0).r);
        vm.cpu.pc = pm.running.get(0).programCounter;
        //System.out.println(vm.cpu.pc);
        vm.cpu.flag = false;
        System.out.println("Processo com id "+ pm.running.get(0).id + " executando");
        //vm.cpu.semcpu.release();
        //vm.cpu.pag = pm.running.get(0).memAlo;
        //vm.cpu.indexpart = pm.running.get(0).memAlo[0];
        //vm.cpu.reg = pm.running.get(0).r;
        //vm.cpu.irpt = Interrupts.noInterrupt;

    }
}