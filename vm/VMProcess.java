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
				// sync here
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
		boolean valid = true; // assume no page faults yet
		if(valid){
			int location  = allocateTLBEntry();
			Machine.processor().writeTLBEntry(location, entry);	
			//sync here
		}else{
			// handle page fault
		}	
	}

	private TranslationEntry GetPageTableEntryFromVPN(int vpn){
		return null;
	}

	private int  allocateTLBEntry(){
		for(int i = 0; i < Machine.processor().getTLBSize();i++){
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if(entry.valid == false){
				return i;
			}
		}
		// ok so all are valid
		int victim = Lib.random(Machine.processor().getTLBSize());
		return victim;
	}

	private TranslationEntry checkPageTables(int vAddr){
		return null;
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
