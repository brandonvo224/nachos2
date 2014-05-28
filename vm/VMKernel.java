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

	int clockHand = 0;
	public TranslationEntry raisePagefault(UserProcess process, TranslationEntry entry){
		
		// If no empty page frames exist
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++)
		{
			// Find an unused physical memory page
			if(ownedMemory[i].process.getProcessID() == null)
			{
				ownedMemory[i].process.getProcessID() = process.getProcessID();
				TranslationEntry newEntry = new TranslationEntry
					(
						entry.vpn,
						i,
						true, 
						false, 
						false, 
						false
					);
				ownedMemory[i].te = newEntry;
				return newEntry;
			}
		}

		// Perform clock algorithm to select page to swap
		while(ownedMemory[clockHand].referenced == true)
		{
			ownedMemory[clockHand].referenced = false;
			clockHand++;
			if(clockHand >= ownedMemory.length)
			{
				clockHand = 0;
			}
		}

		// Swap pages
		if(!ownedMemory[clockHand].te.readOnly)
		{
			if(ownedMemory[clockHand].te.dirty == true)
			{
				//SwapFile.insertPage(ownedMemory[clockHand].te.vpn, clockHand);
				ownedMemory[clockHand].spn = SwapFileinsertPage(clockHand);
			}
			else
			{
				ownedMemory[clockHand].te.valid = false;
			}
		}

		
		if(ownedMemory[entry.ppn].spn != null)
		{
			SwapFile.readPage(ownedMemory[entry.ppn].spn , clockHand);
		}
	}

	// This is the inverted table, indexed by physical page number.
	private PhysicalPageInfo[] ownedMemory;

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';

	private class PhysicalPageInfo
	{
		public UserProcess process;
		public TranslationEntry te;
		public boolean referenced;
		public boolean pinned;
		public PhysicalPageInfo()
		{
			this.process = null;
			this.te = null;
			this.referenced = true;
			this.pinned = false;

		}
	}
	// public static HashTable<TEKey, ProcessTranslationEntry> invertedPageTable;
}
