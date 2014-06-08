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
	public static List<Integer> allocatedPages;
	private static Lock swapLock;
	private static byte[] memory = Machine.processor().getMemory();

	public static void initialize(String filename){
		swapFile = ThreadedKernel.fileSystem.open(filename, true);
		swapName = filename;
		freePages = new LinkedList<Integer>();
		allocatedPages = new LinkedList<Integer>();
		swapLock = new Lock();
	}

	public static void close(){
		swapFile.close();
		ThreadedKernel.fileSystem.remove(swapName);
	}

	public static int insertPage(int spn, int ppn){
		swapLock.acquire();
		int numBits = swapFile.write(spn*PAGESIZE, memory, ppn * PAGESIZE, PAGESIZE);	
		// assert that numBits == PAGESIZE
		allocatedPages.add(spn);
		swapLock.release();
		return spn; 
	}

	/* we will try to allocate a free page from the freepages*/
	public static int insertPage(int ppn){		
		int numBits = 0;
		int spn = swapFile.length() / PAGESIZE;
		if(freePages.size() > 0){
			spn = freePages.remove(0);
		}
		return insertPage(spn, ppn);
	}
	
	public static void readPage(int spn, int ppn){
		if(allocatedPages.contains(spn)){
			swapLock.acquire();
			swapFile.read(spn*PAGESIZE, memory, ppn*PAGESIZE, PAGESIZE);
			swapLock.release();
		}else{
		}
	}

	public static void free(int page){
		swapLock.acquire();
		freePages.add(page);		
		swapLock.release();
	}
	
}
