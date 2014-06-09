package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
		this.pageTableLock = new Lock();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		System.out.println("Process " + this.processID() + " IS CALLING SAVE STATE");
		boolean status = Machine.interrupt().disable();
		TLBLock.acquire();
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if(entry.valid == true){
				// Sync
				this.syncTLBEntry(entry);

				// Flush
				entry.valid = false;
				Machine.processor().writeTLBEntry(i,entry);
			}
		}
		
		TLBLock.release();
		Machine.interrupt().setStatus(status);
		System.out.println("Process " + this.processID() + " is finished saving state.");
	}
		
	public void invalidateEntry(TranslationEntry e){
		//this.pageTableLock.acquire();
		e.valid = false;
		//this.pageTableLock.release();
		
		// TLBLock.acquire();
		//System.out.println("INVALIDATING ENTRY " + e.vpn + "=>"+e.ppn);
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if(entry.ppn == e.ppn){
				entry.valid = false;

				Machine.processor().writeTLBEntry(i,entry);
			}
		}
		// TLBLock.release();
	}
	
	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
	//	super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		//VMKernel.memoryLock.acquire();
		pageTable = new TranslationEntry[numPages];
		this.pageTableLock.acquire();
		for(int vpn = 0; vpn < numPages; vpn++){
			pageTable[vpn] = new TranslationEntry();
		}

		// maps out coff sections
		for(int s = 0; s < coff.getNumSections(); s++){
			CoffSection section = coff.getSection(s);
			for(int j = 0; j < section.getLength(); j++){
				int vpn = section.getFirstVPN() + j;
				pageTable[vpn].used = false;
				pageTable[vpn].dirty = false;
				pageTable[vpn].valid = false;
				pageTable[vpn].readOnly = section.isReadOnly();
				pageTable[vpn].vpn = vpn;
			}
		}

		// maps out stack pages on top 
		// rule - vpns are negative for coffs
		for(int s = numPages - (stackPages + 1); s < numPages; s++){
			pageTable[s].valid = false;
			pageTable[s].readOnly = false;
			pageTable[s].dirty = false;
			pageTable[s].vpn = s;
		}
		//VMKernel.memoryLock.release();
		this.pageTableLock.release();
		return true;
//		return super.loadSections();
	}


	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		this.pageTableLock.acquire();
		for(int i = 0; i < pageTable.length; i++){
			if(pageTable[i].readOnly == false && pageTable[i].valid == false){
				SwapFile.free(pageTable[i].vpn);	// free swap file space
			}
		}

		super.unloadSections();
		this.pageTableLock.release();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionumnZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionTLBMiss:
			handleTLBMiss(processor.readRegister
				(Processor.regBadVAddr));
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	public void handleTLBMiss(int vAddr){
		System.out.println("Process " + this.processID() + " is handling TLB miss at vAddr " + vAddr);
		boolean status = Machine.interrupt().disable();
                TLBLock.acquire();
		int vpn = Processor.pageFromAddress(vAddr);
		TranslationEntry entry = handleTLE(vpn);
		
		int location = this.allocateTLBEntry();
		Machine.processor().writeTLBEntry(location, entry);
		TLBLock.release();
		Machine.interrupt().setStatus(status);
		System.out.println("Process " + this.processID() + " finished handling TLB miss at vpn " + vpn + " at location " + location);
	}

	public TranslationEntry handleTLE(int vpn){
		this.pageTableLock.acquire();
		TranslationEntry entry = pageTable[vpn];
		//System.out.println("ENTRY HAD A THING OF " + entry.vpn + "=>" + entry.ppn + " AND IT IS " + entry.valid);
		if(entry.valid == false){
			if(vpn < (numPages - stackPages - 1) && entry.dirty == false){
				entry = VMKernel.raisePageFault(this,entry, vpn, true);
			}else{
				entry = VMKernel.raisePageFault(this, entry, vpn, false);	
			}
			entry.vpn = vpn;
			pageTable[vpn] = entry;
			pageTable[vpn].valid = true;
		//	System.out.println("HANDLED TLE WITH A " + pageTable[vpn].vpn + "=>" + pageTable[vpn].ppn + " AND IT IS " + pageTable[vpn].valid);
		}
		
		this.pageTableLock.release();
		return entry;
	}


	private int allocateTLBEntry(){
		TranslationEntry entry = null;
		// TLBLock.acquire();
		for(int i = 0; i < Machine.processor().getTLBSize();i++){
			entry = Machine.processor().readTLBEntry(i);
			if(entry.valid == false){
				return i;
			}
		}

		int victim = Lib.random(Machine.processor().getTLBSize());
		entry = Machine.processor().readTLBEntry(victim);
		this.syncTLBEntry(entry);
		// TLBLock.release();
		return victim;
	}

	public void syncTLBEntry(TranslationEntry entry){
	//	System.out.println("SYNC TLB for " + entry.vpn + "=>" + entry.ppn + " AND IT IS NOW " + entry.valid);
		/* Chances are we may have to sync with swap file as well. */
		// this.pageTableLock.acquire();
		for(int i = 0; i < pageTable.length; i++){
			if(pageTable[i].ppn == entry.ppn && pageTable[i].vpn == entry.vpn){
				//pageTable[i].valid = entry.valid;
				pageTable[i].readOnly = entry.readOnly;
				pageTable[i].used = entry.used;
				pageTable[i].dirty = entry.dirty;			
			}
		}

		// this.pageTableLock.release();
	//	VMKernel.ownedMemory[entry.ppn].te.dirty = entry.dirty;
	//	VMKernel.ownedMemory[entry.ppn].te.used = entry.used;
	}

	protected int pinVirtualPage(int vpn, boolean isUserWrite){
		System.out.println("Process " + this.processID() + " is trying to pin vpn " + vpn);
		//Machine.interrupt().disable();
		//TLBLock.acquire();
		this.handleTLE(vpn);
		//TLBLock.acquire();
		//Machine.interrupt().enable();
		System.out.println("Process " + this.processID() + " pinned vpn " + vpn);
		VMKernel.pinPage(pageTable[vpn].ppn);
		return super.pinVirtualPage(vpn,isUserWrite);
	}

	protected void unpinVirtualPage(int vpn){
		VMKernel.unpinPage(pageTable[vpn].ppn);
	}

	public Coff getCoff(){
		return this.coff;
	}

	private Lock pageTableLock;

	public static final Lock TLBLock = new Lock();	

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
