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
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if(entry.valid == true){
				entry.valid = false;
				Machine.processor().writeTLBEntry(i,entry);
				syncTLB(entry);	
			}
		}
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
		return super.loadSections();
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
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
		// check for valid addresss here
		int vpn = Processor.pageFromAddress(vAddr);
		TranslationEntry entry = GetPageTableEntryFromVPN(vpn);
		if(entry.valid == false){
			entry = VMKernel.raisePagefault(this, entry);	
			pageTable[vpn] = entry;
		}
		int location  = allocateTLBEntry();
		Machine.processor().writeTLBEntry(location, entry);
	}

	private TranslationEntry GetPageTableEntryFromVPN(int vpn){
		for(int i = 0; i < pageTable.length; i++){
			if(pageTable[i].vpn == vpn){
				return pageTable[i];
			}		
		}
		return null;
	}

	private int  allocateTLBEntry(){
		TranslationEntry entry = null;
		for(int i = 0; i < Machine.processor().getTLBSize();i++){
			entry = Machine.processor().readTLBEntry(i);
			if(entry.valid == false){
				return i;
			}
		}
		// ok so all are valid
		int victim = Lib.random(Machine.processor().getTLBSize());
		/* Before we evict this guy, we need to sync. */
		entry = Machine.processor().readTLBEntry(victim);
		syncTLB(entry);
		return victim;
	}

	private void syncTLB(TranslationEntry entry){
		/* Chances are we may have to sync with swap file as well. */
		for(int i = 0; i < pageTable.length; i++){
			if(pageTable[i].ppn == entry.ppn && pageTable[i].vpn == entry.vpn){
				pageTable[i].valid = entry.valid;
				pageTable[i].readOnly = entry.readOnly;
				pageTable[i].used = entry.used;
				pageTable[i].dirty = entry.dirty;			
			}
		}
//		VMKernel.ownedMemory[entry.ppn].te.dirty = entry.dirty;
//		VMKernel.ownedMemory[entry.ppn].te.used = entry.used;
	}

	private TranslationEntry checkPageTables(int vAddr){
		return null;
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
