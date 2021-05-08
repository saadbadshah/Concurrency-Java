/**
 *
 * Groups must populate the stubs below in order to implement 
 * the 3 phases of the requirements for this class
 * Note that no other public methods or objects are allow.
 *
 * Private methods and objects may be used
 *
 */

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class ExtrinsicSync implements Synchronisable {
	Phase phase;
	private final ReentrantLock lock = new ReentrantLock();

	// Constructor
	ExtrinsicSync (Phase p){
		this.phase = p; // Phase of testing being performed
	}

	private class Group {
		// from: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/Condition.html
		private final ReentrantLock lock = new ReentrantLock();
		private final Condition notFull  = lock.newCondition();
		private final Condition notEmpty = lock.newCondition();

		private final int threadLimit = 4;
		private int count, realesed = 0;

		private int groupID;

		Group(int id) {
			this.groupID = id;
		}

		Group(){}

		private void waitThreads(){
			try {
				put();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		private void put() throws InterruptedException {
			lock.lock();
			try {
				//block thread if 4 threads already waiting
				while (count == threadLimit)
					notFull.await();

				++count;
				//System.out.println("Passed put" + count);
				take();

				//when released is 4 signal to unblock threads until 4 more threads are inside take()
				if(phase != Phase.THREE){
					if(realesed == threadLimit){
						count = 0;
						realesed = 0;
						notFull.signalAll();
					}
				}
			} finally {
				lock.unlock();
			}
		}

		private void take() throws InterruptedException {
			lock.lock();
			try {
				//wait until all 4 threads can be taken/released
				while (count < threadLimit)
					notEmpty.await();

				notEmpty.signalAll();
				if(phase != Phase.THREE){
					realesed++;
				}

				//System.out.println("After signal" + realesed);
			} finally {
				lock.unlock();
			}
		}


		private void finished(){
			lock.lock();
			try {
				realesed++;
				if(realesed == threadLimit){
					count = 0;
					realesed = 0;
					notFull.signalAll();
				}
			} finally {
				lock.unlock();
			}

		}


	}

	private final Group fourThreads = new Group();

	@Override
	public void waitForThreads() {
		// each thread accesses the waitThreads() method from the fourThreads Group class
		fourThreads.waitThreads();
	}

	private Group group[] = new Group[1];

	@Override
	public void waitForThreadsInGroup(int groupId) {
		// 1 thread can modify the Group array at a given time
		lock.lock();
		try {
			// check if the group array can support the new group
			if (group.length < groupId+1) {
				// deepcopy the existing array
				Group tempArray[] = new Group[group.length];
				System.arraycopy(group, 0, tempArray, 0, group.length);

				// create the new array with a sufficient number of group allocations
				group = new Group[groupId + 1];

				// deepcopy the temp array back into the new one
				System.arraycopy(tempArray, 0, group, 0, tempArray.length);
			}

		} finally {
			if(group[groupId] == null){
				group[groupId] = new Group(groupId);
			}

			lock.unlock();
			group[groupId].waitThreads();
		}

	}

	@Override
	public void finished(int groupId) {
		group[groupId].finished();
	}
}
