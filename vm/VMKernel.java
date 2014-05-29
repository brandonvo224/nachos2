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
		int victim = -1;
		/* if free pageList is not empty*/
		if(freePages.size() > 0){
			victim = freePages.remove(0).intValue();
		}else{
	
		// Perform clock algorithm to select page to swap
			while(ownedMemory[clockHand].referenced == true)
			{
				ownedMemory[clockHand].referenced = false;
				clockHand = (clockHand+1)%ownedMemory.length;
			}
			victim = clockHand;
			clockHand = (clockHand+1)%ownedMemory.length;
		}
		// Swap pages
		//if(!ownedMemory[clockHand].te.readOnly) // its in the coff
	//	{
			if(ownedMemory[victim].te.dirty == true) // we gotta swap in 
			{
				//SwapFile.insertPage(ownedMemory[clockHand].te.vpn, clockHand);
				ownedMemory[victim].te.vpn = SwapFile.insertPage(victim);
				ownedMemory[victim].inSwap = true;
			
			}
		/*	else
			{
				 
				ownedMemory[clockHand].te.valid = false;
			}*/
	//	}
		ownedMemory[victim].te = entry; // we are switching the entry to this new physical space.
		ownedMemory[entry.ppn].te.valid = false; // the old space is now meh
		if(ownedMemory[entry.ppn].inSwap){
			SwapFile.readPage(entry.vpn, victim);
		}
		entry.ppn = victim;
		entry.valid = true;
		return entry;
	}

	// This is the inverted table, indexed by physical page number.
	protected PhysicalPageInfo[] ownedMemory;
	private Condition allPinned;
	private Lock memoryLock;
	private int clockHand = 0;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';

	private class PhysicalPageInfo
	{
		public int processID;
		public TranslationEntry te;
		public boolean referenced;
		public boolean pinned;
		public boolean inSwap;

		public PhysicalPageInfo()
		{
			this.te = new TranslationEntry();
			this.referenced = true;
			this.pinned = false;
			this.inSwap = false;
		}
	}
	// public static HashTable<TEKey, ProcessTranslationEntry> invertedPageTable;
}
