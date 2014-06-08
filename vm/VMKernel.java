package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		SwapFile.initialize("nachos.swp");	
		
		ownedMemory = new PhysicalPageInfo[Machine.processor().getNumPhysPages()];
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++){
			ownedMemory[i] = new PhysicalPageInfo();
		}
		memoryLock = new Lock();
		allPinned = new Condition(memoryLock);	
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		SwapFile.close();
		super.terminate();
	}

	
	public static TranslationEntry raisePageFault(VMProcess process, TranslationEntry entry, boolean isCoff){
	//	System.out.println("freePages size is " + freePages.size());
		Coff coff = process.getCoff();
		if(numPins == ownedMemory.length){	// all being used
			memoryLock.acquire();
			allPinned.sleep();
			memoryLock.release();
		}
		while(freePages.isEmpty())
		{
			PhysicalPageInfo frame = ownedMemory[clockHand];
			if(frame.pinCount == 0){ // no processes using
				if(frame.te.used == true){
					frame.te.used = false;
				}else{
					if(frame.process != null){
						frame.process.invalidateEntry(frame.te);
					} 	
					if(frame.te.dirty == true){
						frame.te.vpn = SwapFile.insertPage(clockHand);
					}	
					freePages.add(clockHand);
					
				}
			}
			clockHand = (clockHand+1)%ownedMemory.length;
		}	

		int victim = freePages.remove(0).intValue();
		ownedMemory[victim].te = entry; 
		ownedMemory[victim].process = process;
		entry.ppn = victim;
		entry.valid = true;
		// we are switching the entry to this new physical space.
		if(isCoff == false){
		//	if(entry.vpn != Integer.MAX_VALUE){ 
				SwapFile.readPage(entry.vpn, entry.ppn);
		//	}
		}else{	// we set the coff vpn to negative if it belonged to a coff
			for(int s = 0; s < coff.getNumSections(); s++){
				CoffSection section = coff.getSection(s);
				for(int j = 0; j < section.getLength(); j++){
					if(section.getFirstVPN() + j == entry.vpn){
						section.loadPage(j, entry.ppn);
					}
				}
			}
		}
		return entry;
	}

	public static void pinPage(int ppn){
		ownedMemory[ppn].pinCount++;
		numPins++;
	}

	public static void unpinPage(int ppn){
		ownedMemory[ppn].pinCount++;
		numPins--;
		memoryLock.acquire();
		allPinned.wakeAll();
		memoryLock.release();
	}

	// This is the inverted table, indexed by physical page number.
	protected static PhysicalPageInfo[] ownedMemory;
	private static int numPins = 0;
	private static Condition allPinned;
	private static Lock memoryLock;
	private static int clockHand = 0;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';

	private class PhysicalPageInfo
	{
		public VMProcess process;
		public TranslationEntry te;
		public int pinCount;
		public boolean freeWhenUnpinned;
		public boolean used;
		public boolean inSwap;

		public PhysicalPageInfo()
		{
			this.te = new TranslationEntry();
			this.pinCount = 0;
			this.process = null;
			this.freeWhenUnpinned = false;	
			this.used = true;
			this.inSwap = false;
		}
	}
	// public static HashTable<TEKey, ProcessTranslationEntry> invertedPageTable;
}
