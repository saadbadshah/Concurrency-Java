
/**
 * 
 * Groups must populate the stubs below in order to implement 
 * the 3 phases of the requirements for this class
 * Note that no other public methods or objects are allow.
 * 
 * Private methods and objects may be used
 * 
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicSync implements Synchronisable {

	Phase phase;
	private final AtomicBoolean groupLock = new AtomicBoolean(false);
	// class to run an instance of waitForThreads()
	private class Group {

		private final AtomicInteger syncCounter = new AtomicInteger(0);
		private final AtomicInteger leaveCounter = new AtomicInteger(4);
		private final AtomicInteger releasedCounter = new AtomicInteger(4);
		private final AtomicBoolean outLock = new AtomicBoolean(false);
		private final AtomicBoolean innerLock = new AtomicBoolean(true);
		private int groupID;

		Group(int id) {
			this.groupID = id;
		}
		Group(){
		}

		private void waitThreads() {
			// allow at most 1 thread past this point at a time, the rest busy wait here


			while(!outLock.compareAndSet(false, true)){
				if (Thread.currentThread().isInterrupted()){
					break;
				}
			}
			while(phase == Phase.THREE && releasedCounter.get() != 4 && syncCounter.get() == 4){
				if (Thread.currentThread().isInterrupted()){
					break;
				}
			}
			try{
				// test number of threads inside of the outLock area
				if (syncCounter.incrementAndGet() < 4) outLock.set(false);
				// when 4 threads are inside the outLock area release them all from the innerLock busy wait
				else innerLock.set(false);

				// busy wait for threads being syncronised
				while (innerLock.get() == true){
					if (Thread.currentThread().isInterrupted()){
						break;
					}
				}
			}catch (Exception e){
				System.out.println("Exception in waitForThreads: "+e.toString());
			} finally {
				// the last thread to leave will lock the innerLock boolean behind it
				// reset the counters
				// and finally unlock outLock
				if (leaveCounter.decrementAndGet() == 0){
					innerLock.set(true);
					syncCounter.set(0);
					leaveCounter.set(4);
					outLock.set(false);
				}
			}
		}

		private void finished() {
			while (releasedCounter.decrementAndGet() == 0){
				releasedCounter.set(4);
			}
		}
	}

	// Constructor 
	AtomicSync (Phase p){ 
		this.phase = p; // Phase of testing being performed
	}

	// instance of Group class with no groupId arg
	private final Group fourThreads = new Group();

	@Override
	public void waitForThreads() {
		// each thread accesses the waitThreads() method from the fourThreads Group class
		fourThreads.waitThreads();
	}



	private Group group[] = new Group[100];

	@Override
	public void waitForThreadsInGroup(int groupId) {

			try {
				// to relieve bottleneck caused by the lock on the next line
				// check if the group array can support the new group
				if (group.length < groupId+1) {
					try {

					// 1 thread can modify the Group array at a given time
					while (!groupLock.compareAndSet(false, true)) {
						if (Thread.currentThread().isInterrupted()){
							break;
						}
					}
					// check again incase the condition has changed since this thread traversed the lock
					if (group.length < groupId + 1) {
						// copy the existing array
						Group tempArray[] = new Group[group.length];
						System.arraycopy(group, 0, tempArray, 0, group.length);

						// create the new array with a sufficient number of group allocations
						//
						group = new Group[groupId + 1 * 2];

						// copy the temp array back into the new one
						System.arraycopy(tempArray, 0, group, 0, tempArray.length);
					}

					} finally {
						// release the lock
						groupLock.set(false);
					}
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			} finally {
				// instantiate a new thread if needed
				if (group[groupId] == null) {
					try {
						while (!groupLock.compareAndSet(false, true)) {
							if (Thread.currentThread().isInterrupted()){
								break;
							}
						}
						if (group[groupId] == null) {
							group[groupId] = new Group(groupId);
						}
					} finally {
						// release the lock
						groupLock.set(false);
					}
				}
			}

		group[groupId].waitThreads();
	}
	@Override
	public void finished(int groupId) {
		// call finished when thread finishes executing its "work"
		group[groupId].finished();

	}
}

