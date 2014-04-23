package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	private Lock lock;
	
	Condition speakers;
	Condition listeners;
	Condition returners;

	int speaking=0;
	int listening =0;	
	
	int word;
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();
		speakers = new Condition(lock);
		listeners = new Condition(lock);
		returners = new Condition(lock);
	
	}


	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		speaking++;
		listeners.wake();
		while(listening==0)
		     speakers.sleep();
		this.word = word;
		returners.wake();
		speaking--;
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();
		listening++;
		speakers.wake();
		while(speaking == 0){ // nothing to listen to
			listeners.sleep();
		}
		returners.sleep();
		int word = this.word;
		listening--;
		lock.release();
		return word;
	}
}
