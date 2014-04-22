package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	private Lock lock = new Lock();
	private int speakersActive = 0;
	int speakersWait = 0;
	int listenersActive = 0;
	int listenersWait = 0;
	Condition speakers;
	Condition listeners;
	Condition returning;

	int word;
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
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
		while(speakersActive > 0)
		{
			speakersWait++;
			speakers.sleep();
			speakersWait--;
		}

		speakersActive++;
		this.word = word;

		if(listenersActive > 0)
		{
			listeners.wake();
			lock.release();
			return;
		} else {
			if (listenersWait > 0)
			{
				returning.sleep();
				speakersActive--;
				listenersActive--;
				if(speakersWait > 0)
				{
					speakers.wake();
				}

				lock.release();
				return;
			}
		}
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();
		while(listenersActive > 0)
		{
			listenersWait++;
			listeners.sleep();
			listenersWait--;
		}

		listenersActive++;
		if(speakersActive > 0)
		{
			speakers.wake();
			
			// Store the word
			lock.release();
			return this.word;
		}else{
			if(speakersWait> 0)
			{
				speakers.wake();
			}
			
			returning.sleep();
			listenersActive--;
			speakersActive--;
			if(listenersWait > 0)
			{
				listeners.wake();
			}

			// Store word
			lock.release();
			return this.word;
		}
	}
}
