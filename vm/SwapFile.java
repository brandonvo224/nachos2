package nachos.vm;

import java.util.*;
import nachos.machine.*;
import nachos.userprog.*;
import nachos.threads.*;
import nachos.threads.Lock;

public class SwapFile{

	public static OpenFile swapFile;
	private static String swapName;
	private static int PAGESIZE  = Machine.processor().pageSize;
	public static List<Integer> freePages;
	private static Lock swapLock;
	private static byte[] memory = Machine.processor().getMemory();

	public static void initialize(String filename){
		swapFile = ThreadedKernel.fileSystem.open(filename, true);
		swapName = filename;
		freePages = new LinkedList<Integer>();
		swapLock = new Lock();
	}

	public static void close(){
		swapFile = null; 
		ThreadedKernel.fileSystem.remove(swapName);
	}

	public static int insertPage(int spn, int ppn){
		swapLock.acquire();
		// do some validation here?
			
		int numBits = swapFile.write(spn*PAGESIZE, memory, ppn * PAGESIZE, PAGESIZE);	
		swapLock.release();
		return spn;	// uh
	}
	
	public static void readPage(int spn, int ppn){
		swapLock.acquire();
		swapFile.read(spn*PAGESIZE, memory, ppn*PAGESIZE, PAGESIZE);
		swapLock.release();
	}
	
}
