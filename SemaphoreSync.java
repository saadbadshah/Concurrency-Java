

/**
 * 
 * Groups must populate the stubs below in order to implement 
 * the 3 phases of the requirements for this class
 * Note that no other public methods or objects are allow.
 * 
 * Private methods and objects may be used
 * 
 */

import java.util.concurrent.Semaphore;

public class SemaphoreSync implements Synchronisable {

	Phase phase;
	final Semaphore arrayExpansion = new Semaphore(1,true);
	final Semaphore instantiationLock = new Semaphore(1,true);

	private class Group {

		final Semaphore outerLock = new Semaphore(4, true);
		final Semaphore syncIn = new Semaphore(1,true);
		final Semaphore syncOut = new Semaphore(1,true);
		final Semaphore errorGuard = new Semaphore(1, true);
		final Semaphore finalLock = new Semaphore(0, true);

		final Semaphore counterIn = new Semaphore(4, true);
		final Semaphore counterOut = new Semaphore(4, true);
		final Semaphore numberOfCalls = new Semaphore(4,true);
		final Semaphore p3Lock = new Semaphore(4,true);




		private int groupID;

		Group(int id) {this.groupID = id;}
		Group(){}

		private void waitThreads() {
			try {
				// four acquire A
				errorGuard.acquire();
				// phase 3 threads get locked here until 4 threads call finished
				if (phase == Phase.THREE){
					p3Lock.acquire();
				}
				errorGuard.release();
				// 4th releases first 4 from next lock B
				outerLock.acquire(); // 4 -> 0
				//
				syncIn.acquire();
				counterIn.acquire(); // 4
				while (counterIn.availablePermits() == 0 && finalLock.availablePermits() == 0){
					finalLock.release(4);
				}
				syncIn.release();

				finalLock.acquire();

				syncOut.acquire();
				counterOut.acquire();
				while (counterOut.availablePermits() == 0 && syncOut.availablePermits() == 0 && outerLock.availablePermits() == 0){
					if (finalLock.availablePermits() == 0){
						// drain all threads for safety before resetting
						finalLock.drainPermits();
						syncIn.drainPermits();
						counterIn.drainPermits();
						outerLock.drainPermits();

						counterOut.release(4);
						counterIn.release(4);
						syncIn.release(1);
						outerLock.release(4);

					}
				}
				syncOut.release();


			} catch (Exception e) {
				System.out.println("Semaphore exception " + e.toString());
			}
		}

		private void finished(){
			//

			try{
				numberOfCalls.acquire();
			}catch (Exception e){
				System.out.println(e);
			} finally {
				while (numberOfCalls.availablePermits() == 0) {
					p3Lock.release(4);
					numberOfCalls.release(4);
				}
			}

		}
	}


	SemaphoreSync (Phase p){ 
		this.phase = p; // Phase of testing being performed
	}

	private final Group fourThreads = new SemaphoreSync.Group();

	@Override
	public void waitForThreads() {
		fourThreads.waitThreads();
	}

	private Group group[] = new Group[1];

	@Override
	public void waitForThreadsInGroup(int groupId) {

		try {
			while (group.length < groupId+1){
				// 1 thread can modify the Group array at a given time
				try {
					arrayExpansion.acquire();

					// check if the group array can support the new group
					while (group.length < groupId+1) {
						// deepcopy the existing array
						Group tempArray[] = new Group[group.length];
						System.arraycopy(group, 0, tempArray, 0, group.length);

						// create the new array with a sufficient number of group allocations
						group = new Group[groupId + 1 * 2];

						// deepcopy the temp array back into the new one
						System.arraycopy(tempArray, 0, group, 0, tempArray.length);
					}
				} finally {
					arrayExpansion.release();
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());

		} finally {
			while (group[groupId] == null){
				try {
					instantiationLock.acquire();
					while (group[groupId] == null){
						group[groupId] = new SemaphoreSync.Group(groupId);
					}
				} catch (Exception e){
					System.out.println("Exception in instantiate new semaphore group");
				} finally {
					instantiationLock.release();
				}
			}
			group[groupId].waitThreads();
		}


	}
	@Override
	public void finished(int groupId) {
		group[groupId].finished();
	}
}
