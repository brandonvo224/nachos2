package nachos.vm;

import java.util.*;
import nachos.machine.*;
import nachos.userprog.*;
import nachos.threads.*;
import nachos.threads.Lock;

/* In addition to tracking free pages (which may be managed as in project 2),
 * there are now two extra pieces of memory information of relevance to all processes: 
 * which pages are pinned, and which process owns which pages.*/

public class ProcessTranslationEntry {
	public ProcessTranslationEntry(TranslationEntry entry){
		this.translationEntry = entry;
	}

	TranslationEntry translationEntry;
	int processID = -1;
	boolean pinned = false;
}
