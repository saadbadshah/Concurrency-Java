/**
 * 
 * THIS INTERFACE MUST NOT BE CHANGED EVEN BY ONE CHARACTER
 * OTHERWISE THE AUTO MARKING SCRIPTS WILL FAIL
 * 
 */

public interface Synchronisable {

	void waitForThreads();

	void waitForThreadsInGroup(int groupId); //groupId >= 0

	void finished(int groupId);

}

enum Phase {ONE, TWO, THREE}