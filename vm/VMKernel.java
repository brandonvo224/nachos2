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
		
		invertedPhysicalPageTable = new PhysicalPageInfo[Machine.processor().getNumPhysPages()];
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++){
			invertedPhysicalPageTable[i] = new PhysicalPageInfo();
		}
		memoryLock = new Lock();
		clockLock = new Lock();
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


	
	public static TranslationEntry raisePageFault(VMProcess process, TranslationEntry entry, int accessedVpn, boolean isCoff){
		
		Coff coff = process.getCoff();
		if(numPins == invertedPhysicalPageTable.length){	// all being used
			memoryLock.acquire();
			allPinned.sleep();
			memoryLock.release();
		}

		// Run clock algorithm. Choose a victim.
		//System.out.println("RUNNING CLOCK ALGORITHM");
		while(freePages.isEmpty())
		{
			clockLock.acquire();
			PhysicalPageInfo selectedPhysicalPage = invertedPhysicalPageTable[clockHand];
	
			// If process did not pin the selected physical page.
			if(selectedPhysicalPage.pinCount == 0){
				if(selectedPhysicalPage.translationEntry.used == true){
					selectedPhysicalPage.translationEntry.used = false;
				}else{
					if(selectedPhysicalPage.process != null){
						selectedPhysicalPage.process.invalidateEntry(selectedPhysicalPage.translationEntry);
					}
 
					// If selected page has been modified, write the modified page to disk.
					if(selectedPhysicalPage.translationEntry.dirty == true){
						//System.out.println("WRITING TO SWAP " + clockHand);
						selectedPhysicalPage.translationEntry.vpn = SwapFile.insertPage(clockHand); // Should change in the process' page table as well.
					}

					// Finally, add the selected page number to free pages.
					freePages.add(clockHand);		
				}
			}

			clockHand = (clockHand+1)%invertedPhysicalPageTable.length; // Increment the clock.
			clockLock.release();
		}

		//System.out.println("FREE PAGES ARE : ");
		//for(Integer i : freePages)
		//{
		//	System.out.print(i + ", ");
		//}

		// Get a free page and set the entry's ppn to it.
		int victimPageNumber = freePages.remove(0).intValue();
		//System.out.println("FREE PAGES SIZE IS : " + freePages.size());
		//System.out.println("VICTIM IS " + victimPageNumber);
		invertedPhysicalPageTable[victimPageNumber].translationEntry = entry; 
		invertedPhysicalPageTable[victimPageNumber].process = process;
		entry.ppn = victimPageNumber;
		entry.valid = true;
		entry.dirty = false;

		// we are switching the entry to this new physical space.
		if(isCoff == false && entry.vpn != accessedVpn)
		{
			SwapFile.readPage(entry.vpn, entry.ppn); // Replace chosen physical page with swap page.
			SwapFile.free(entry.vpn);
			entry.vpn = accessedVpn;
		}
		else
		{	
			// we set the coff vpn to negative if it belonged to a coff
			//System.out.println("READING FROM COFF " + entry.vpn);
			for(int s = 0; s < coff.getNumSections(); s++)
			{
				CoffSection section = coff.getSection(s);
				for(int j = 0; j < section.getLength(); j++)
				{
					if(section.getFirstVPN() + j == entry.vpn)
					{
						section.loadPage(j, entry.ppn);
					}
				}
			}
		}

		return entry;
	}

	public static void pinPage(int ppn){
		invertedPhysicalPageTable[ppn].pinCount++;
		numPins++;
	}

	public static void unpinPage(int ppn){
		invertedPhysicalPageTable[ppn].pinCount--;
		numPins--;
		memoryLock.acquire();
		allPinned.wakeAll();
		memoryLock.release();
	}

	// This is the inverted table, indexed by physical page number.
	protected static PhysicalPageInfo[] invertedPhysicalPageTable;
	private static int numPins = 0;
	private static Condition allPinned;
	private static Lock memoryLock;
	private static int clockHand = 0;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
	static Lock clockLock;
	private class PhysicalPageInfo
	{
		public VMProcess process;
		public TranslationEntry translationEntry;
		public int pinCount;
		public boolean freeWhenUnpinned;
		public boolean used;

		public PhysicalPageInfo()
		{
			this.translationEntry = new TranslationEntry();
			this.pinCount = 0;
			this.process = null;
			this.freeWhenUnpinned = false;	
			this.used = true;
		}
	}
	// public static HashTable<TEKey, ProcessTranslationEntry> invertedPageTable;
}

