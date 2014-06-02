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
			ownedmemory[i] = new PhysicalPageInfo();
		}
		memoryLock = new Lock();
		allPinned = new Condition(memoryLock);	
		//invertedPageTable = new HashMap<TEKey, ProcessTranslationEntry>();
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

	
	public TranslationEntry raisePagefault(UserProcess process, TranslationEntry entry){
		
		Coff coff = process.coff;
		if(numPins == ownedMemory.length){	// all being used
			allPinned.sleep();
		}
		while(freePages.isEmpty())
		{
			PhysicalPageInfo frame = ownedMemory[clockHand];
			if(frame.pinCount == 0){ // no processes using
				if(frame.te.used == true){
					frame.te.used = false;
				}else{
					if(frame.te.dirty == true){
						frame.te.vpn = SwapFile.insertPage(clockHand);
						frame.inSwap = true;
					}else{
						frame.te.used = true;
						frame.te.valid = false;
						freePages.add(clockHand);
					}
				}
				clockHand =  (clockHand+1)%ownedMemory.length;
			}
		}	
		int victim = freePages.remove(0).intValue();
		ownedMemory[victim].te = entry; 
		ownedMemory[victim].process = process;
		entry.ppn = victim;
		entry.valid = true;
		// we are switching the entry to this new physical space.
		if(ownedMemory[entry.ppn].inSwap){ 
			SwapFile.readPage(entry.vpn, entry.ppn);
		}else{
			int vpn = entry.vpn;
			for(int s = 0; i < coff.getNumSections(); i++){
				CoffSection section = coff.getSection(s);
				if(vpn < section.getFirstVPN()+section.getLength()){
					section.loadPage(vpn, entry.ppn);
					break;
				}
			}
		}
		return entry;
	}

	public static void pinPage(int ppn){
		ownedMemory[ppn].pinCount++;
		numPins++;
	}

	public static void pinPage(int ppn){
		ownedMemory[ppn].pinCount++;
		numPins--;
	}

	// This is the inverted table, indexed by physical page number.
	protected PhysicalPageInfo[] ownedMemory;
	private int numPins = 0;
	private Condition allPinned;
	private Lock memoryLock;
	private int clockHand = 0;
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
			this.processID = -1;
			this.freeWhenUnpinned = false;	
			this.used = true;
			this.inSwap = false;
		}
	}
	// public static HashTable<TEKey, ProcessTranslationEntry> invertedPageTable;
}
