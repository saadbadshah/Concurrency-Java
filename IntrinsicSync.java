
/**
 * 
 * Groups must populate the stubs below in order to implement 
 * the 3 phases of the requirements for this class
 * Note that no other public methods or objects are allow.
 * 
 * Private methods and objects may be used
 * 
 */

/**
 * This implementation must make use of the synchronized keyword,
 * together with the java.lang.Object  methods .wait()
 * and .notifyAll().
 * note that .notify() may also be used as an alternative
 * to .notifyAll().
 * No classes can be used from the java concurrent package
 * (java.util.concurrent).
 * *
 *
 */


public class IntrinsicSync implements Synchronisable {

	Phase phase;

	// Constructor
	IntrinsicSync (Phase p){
		this.phase = p; // Phase of testing being performed
	}

	int currentGroupID = -1;
	Group holder[] = new Group[100];
	int i = 1;

	private class Group{
		private final int threadLimit = 4;
		private int count = 0;
		private int released = 0;
		private int groupID;

		Group(int id) {
			this.groupID = id;
		}

		Group(){}

		private void waitThreads(){
			try {
				this.put();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private synchronized void put() throws InterruptedException {

			while(count == threadLimit){
				wait();
			}

			count++;
			//System.out.println("Passed put" + count);
			this.take();

			//when released is 4 signal to unblock threads until 4 more threads are inside take()
			if(phase != Phase.THREE){
				if(released == threadLimit){
					count = 0;
					released = 0;
					notifyAll();
				}
			}

			if(released == 4){
				count = 0;
				released = 0;
				notifyAll();
			}
		}

		private synchronized void take() throws InterruptedException {
			while(count < threadLimit){
				wait();
			}
			notifyAll();
			if(phase != Phase.THREE){
				released++;
			}
		}

		private synchronized void finished(){

				released++;
				if(released == threadLimit){
					count = 0;
					released = 0;
					notifyAll();
				}
		}

	}
	private final Group fourThreads = new Group();

	@Override
	public void waitForThreads() {
		fourThreads.waitThreads();
	}

	private Group[] group = new Group[1];

	@Override
	public void waitForThreadsInGroup(int groupId) {
		// TODO Auto-generated method stub
		call(groupId);
	}

	private void call(int groupId) {
		//Setting up new group if it doesn't exist in array
		try {
			// check if the group array can support the new group
			if (group.length < groupId + 1) {
				expandArray(groupId);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		} finally {
			if (group[groupId] == null) {
				newGroup(groupId);
			}
		}
		group[groupId].waitThreads();

	}

	@Override
	public void finished(int groupId) {
		group[groupId].finished();
	}

	private synchronized void expandArray(int groupId){
		// deepcopy the existing array
		if (group.length < groupId + 1) {
			IntrinsicSync.Group tempArray[] = new IntrinsicSync.Group[group.length];
			System.arraycopy(group, 0, tempArray, 0, group.length);
			// create the new array with a sufficient number of group allocations
			group = new IntrinsicSync.Group[groupId * 2];

			// deepcopy the temp array back into the new one
			System.arraycopy(tempArray, 0, group, 0, tempArray.length);
		}
	}

	private synchronized void newGroup(int groupId){
		if (group[groupId] == null) {
			group[groupId] = new IntrinsicSync.Group(groupId);
		}

	}
}
