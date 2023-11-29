package SistemaConcorrente;

import cpu.InterruptHandling;
import cpu.Opcode;
import cpu.SysCallHandling;
import cpu.VM;
import memory.GerenciaMemoria;
import memory.Programas;
import memory.Word;
import process.GerenciaProcessos;
import process.Interrupts;
import process.PCB;

import java.util.*;
import java.util.concurrent.Semaphore;

public class SistemaConcorrente {
	public static int limiteOverflow = 10000;


	private void loadProgram(Word[] p, Word[] m) {
		for (int i = 0; i < p.length; i++) {
			m[i].opc = p[i].opc;     m[i].r1 = p[i].r1;     m[i].r2 = p[i].r2;     m[i].p = p[i].p;
		}
	}

	private void loadProgram(Word[] p) {
		loadProgram(p, vm.m);
	}







	private void exec(int id){
		PCB pcb = pm.searchProcess(id);
			if(pcb != null){
				int end = mm.translateLogicalIndexToFisical(pcb.memAlo[0], 0);
				//System.out.println("---------------------------------- programa carregado na memoria");
				//vm.mem.dump(end, end + pcb.memLimit);            // dump da memoria nestas posicoes
		vm.cpu.setContext(0, vm.tamMem - 1, pcb.memAlo, pcb.memAlo[0], pcb.r);      // seta estado da cpu ]
				//System.out.println("---------------------------------- inicia execucao ");
		pm.ready.remove(pcb);
		pm.running.add(pcb);
		System.out.println("Processo com id "+ pm.running.get(0).id + " executando");
		//vm.cpu.run();
		if(vm.cpu.countex == 0){
			vm.cpu.countex++;
		vm.cpu.start();
		} else {
			vm.cpu.run();
		}
		//sysCall.start();
		//System.out.println("---------------------------------- memoria apos execucao ");
				//vm.mem.dump(end, end + pcb.memLimit);            // dump da memoria com resultado
		} else {
			System.out.println("Processo nao encontrado");
		}
	}

	public class ShellIO extends Thread{
		private Semaphore shellsem = new Semaphore(1);
	public void run(){
		int op = -1;
		int programs = -1;
		int prcs = -1;
		int memI = -1;
		int memF = -1;
		Scanner sc = new Scanner(System.in);
		while(op!=0){
			sysCall.iosem.release();
			try {
				shellsem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// handle the exception...
				// For example consider calling Thread.currentThread().interrupt(); here.
			}

				System.out.println("\n ---------------------------------");
				System.out.println("Seja bem vindo ao sistema!");
				System.out.println("Selecione a operacao desejada:");
				System.out.println("	1 - Criar processos");
				System.out.println("	2 - Dump processo");
				System.out.println("	3 - Desaloca processo");
				System.out.println("	4 - Dump Memória");
				System.out.println("	5 - Executar processos");
				System.out.println("	6 - TraceOn");
				System.out.println("	7 - TraceOff");
				System.out.println("	0 - Sair");
				System.out.print("Operacao: ");

				op = sc.nextInt();

			switch (op) {
						case 1:
							//pm.createProcess(progs.fibonacci10);
							pm.criaProcesso(progs.progMinimo);
							pm.criaProcesso(progs.fatorial);
							//pm.createProcess(progs.fatorialTRAP);
							pm.criaProcesso(progs.fibonacciTRAP);
							System.out.println("Processos criados!");
							exec(pm.ready.get(0).id);
							break;
						case 2:
							System.out.println("Digite o número do processo: ");
							prcs = sc.nextInt();
							pm.dumpProcess(prcs);
							break;

						case 3:
							System.out.println("Digite o número do processo: ");
							prcs = sc.nextInt();
							pm.desalocaProcesso(prcs);
							break;

						case 4:
							System.out.println("Digite a posicao de inicio da memoria: ");
							memI = sc.nextInt();
							System.out.println("Digite a posicao de fim da memoria: ");
							memF = sc.nextInt();
							vm.mem.dump(memI, memF+1);

							break;

						case 5:
							System.out.println("Digite o número do processo: ");
							int idprocess = pm.ready.get(0).id;
							exec(idprocess);
							break;

						case 6:  System.out.println("Trace ativo");
							vm.cpu.debug = true;
							break;

						case 7:
							System.out.println("Trace desativado");
							vm.cpu.debug = false;
						break;

						case 0: System.exit(0);
						break;
			}
			shellsem.release();
		}

	}
}


	public VM vm;
	public InterruptHandling ih;
	public SysCallHandling sysCall;
	public static Programas progs;
	public GerenciaMemoria mm;
	public GerenciaProcessos pm;
	public ShellIO sh;

    public SistemaConcorrente(){   // a VM com tratamento de interrupções
		 ih = new InterruptHandling();
         sysCall = new SysCallHandling();

		 vm = new VM(ih,sysCall);

		 progs = new Programas();

		mm = GerenciaMemoria.getInstance(); // Inicialize primeiro a GerenciaMemoria
//		vm = VM.getInstance(ih, sysCall);   // Então inicialize a VM com as dependências
		pm = GerenciaProcessos.getInstance();
		sysCall.setVM(vm);
		pm.setVm(vm);

		 sh = new ShellIO();
	}

	public static void main(String args[]) {

		SistemaConcorrente s = new SistemaConcorrente();

		s.sysCall.start();
		s.sh.start();


	}



}
