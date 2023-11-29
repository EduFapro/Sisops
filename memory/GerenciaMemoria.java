package memory;

public class GerenciaMemoria {

    private static GerenciaMemoria instance;
    private int maxMemorySize = 1024;
    public int pageSize = 16;
    private int numOfPages;
    private int numOfAvailablePages;
    private boolean[] logicalMemory;

    private GerenciaMemoria() {

        numOfPages = (int) Math.ceil((double) maxMemorySize / pageSize);
        numOfAvailablePages = numOfPages;
        logicalMemory = new boolean[numOfPages];
    }

    public static synchronized GerenciaMemoria getInstance() {
        if (instance == null) {
            instance = new GerenciaMemoria();
        }
        return instance;
    }

    public boolean allocable(int numOfWords) {
        int numOfPagesForProccess = (int) Math.ceil((double) numOfWords / pageSize);
        return numOfPagesForProccess <= numOfAvailablePages;
    }

    public int[] alocate(int numOfWords) {
        if(!allocable(numOfWords)) {
            return new int[] { -1 };
        }
        int numOfPagesForProccess = (numOfWords % pageSize == 0) ? (numOfWords / pageSize) : (numOfWords / pageSize) + 1;
        int[] pagesIndex = new int[numOfPagesForProccess];
        int pagesCount = 0;
        for(int i = 0; i < logicalMemory.length; i++) {
            if(numOfPagesForProccess <= 0) {
                break;
            }
            if (!logicalMemory[i]) {
                logicalMemory[i] = true;
                pagesIndex[pagesCount++] = i;
                numOfAvailablePages--;
                numOfPagesForProccess--;
            }
        }
        return pagesIndex;
    }

    public void dealocate(int[] pages) {
        for (int page : pages) {
            if(page >= 0 && page < logicalMemory.length) {
                logicalMemory[page] = false;
                numOfAvailablePages++;
            }
        }
    }

    public int translateLogicalIndexToFisical(int index, int offset) {
        if (index >= 0 && offset >= 0 && index < logicalMemory.length) {
            return (index * pageSize) + offset;
        }
        return -1;
    }
}
