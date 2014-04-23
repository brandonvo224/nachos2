package nachos.threads;

import nachos.machine.*;

import java.util.*;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

	private List<AlarmThread> queue;

	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		queue = Collections.synchronizedList(new ArrayList<AlarmThread>());
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		Lib.assertTrue(Machine.interrupt().disabled());
List newlist = Collections.synchronizedList(new ArrayList<AlarmThread>());
    			for(AlarmThread at : queue){
        			if(Machine.timer().getTime() >= at.time)
				{
					at.thread.ready();
				}else{
					newlist.add(at);
				}
    			}
			this.queue = newlist;
		
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		Machine.interrupt().disable();
		AlarmThread newAlarmThread = new AlarmThread(KThread.currentThread(), Machine.timer().getTime() + x);
		queue.add(newAlarmThread);
		KThread.sleep();
		Machine.interrupt().enable();

		// KThread waitingThread = KThread.createIdleThread();

		// for now, cheat just to get something working (busy waiting is bad)
		//long wakeTime = Machine.timer().getTime() + x;
		//while (wakeTime > Machine.timer().getTime())
		//	KThread.yield();
	}

	public class AlarmThread
	{
		public KThread thread;
		public long time;
		
		public AlarmThread(KThread thread, long time)
		{
			this.thread = thread;
			this.time = time;
		} 
	}
}
