import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZonedDateTime;
import java.util.*;

public class IntrinsicSyncTest {

	// time limit for each thread to run
	private int timeout = 200;
	private int p3Timeout = timeout * 100;

	private int largestMultipleFour(int x){
		while (x % 4 != 0){
			x--;
		}
		return x;
	}

	private Thread[] initP1Test(int initThreads){
		final IntrinsicSync intrinsic = new IntrinsicSync(Phase.ONE);
		Thread threads[] = new Thread[initThreads];
		for (int i = 0 ; i < initThreads; i++){
			threads[i] = new Thread(new ThreadTester(intrinsic));
			threads[i].start();
		}
		return threads;
	}

	private int terminateThreads (Thread threads[], int expectedThreads){
		// counter for the number of terminated threads
		int termThreads = 0;
		int threadsSize = threads.length;

		//Iterator<Thread> it = new ArrayList(Arrays.asList(threads)).iterator();
		//List<Thread> finished = new ArrayList<Thread>();

		// timer variables, start time, updating time and max time to wait.
		final long waitLimit = (threadsSize*timeout)+(timeout*20);
		final long beginWait = ZonedDateTime.now().toInstant().toEpochMilli();

		long timeNow = ZonedDateTime.now().toInstant().toEpochMilli();

		//termThreads = finished.size();
		//while not terminated or timed out
		while (termThreads != expectedThreads && (timeNow - beginWait) <= waitLimit){
			// sleep here to reduce cpu cycles spent checking if threads are terminated
			try{ Thread.sleep(50);} catch (Exception e){System.out.println("Exception "+e.toString());}
			termThreads = 0;

            /* Too much overhead
            while (it.hasNext()){
                Thread temp = it.next();
                if (temp.getState() == Thread.State.TERMINATED){
                    finished.add(temp);
                    it.remove();
                }
            }
            */

			// looping over all the threads every time is much faster
			for (Thread t : threads){
				if (t.getState() == Thread.State.TERMINATED) termThreads++;
			}
			timeNow = ZonedDateTime.now().toInstant().toEpochMilli();
		}

		if((timeNow - beginWait) > waitLimit){
			System.out.println("Time out! consider increasing the timeout variable");
		}

		return termThreads;
	}

	private void tidy(Thread threads[]){
		for(Thread thread : threads){
			thread.interrupt();
		}
	}

	private void tidyStack(Stack<Thread> threads){
		for (Thread thread : threads){
			thread.interrupt();
		}
	}

	@Test
	@Tag("Non-random")
	void testPhase1a() {
		final int initThreads = 100;
		final int expectedThreads = largestMultipleFour(initThreads);

		Thread threads[] = initP1Test(initThreads);

		// wait until all threads terminate
		int termThreads = terminateThreads(threads, expectedThreads);

		Stack threadStack = new Stack<Thread>();

		for (Thread startedThreads : threads){
			if (startedThreads.getState() == Thread.State.TERMINATED) threadStack.push(startedThreads);
		}

		assertEquals (0, threadStack.size() % 4, "Number of terminated threads should be a multiple of 4");
		assertEquals(expectedThreads, termThreads, "Unexpected number of threads have terminated");
		tidy(threads);
	}

	// paramterized test to test a range of thread numbers
	@ParameterizedTest(name="Run {index}")
	@ValueSource(ints = {0, 1, 3, 4, 7, 8, 10, 12, 25, 36, 50, 100, 123})
	@Tag("Non-random")
	public void testPhase1b(int initThreads) throws Throwable {
		final int expectedThreads = largestMultipleFour(initThreads);
		Thread threads[] = initP1Test(initThreads);

		int termThreads = terminateThreads(threads, expectedThreads);

		Stack threadStack = new Stack<Thread>();

		for (Thread startedThreads : threads){
			if (startedThreads.getState() == Thread.State.TERMINATED) threadStack.push(startedThreads);
		}
		assertEquals (0, threadStack.size() % 4, "Number of terminated threads should be a multiple of 4");
		assertEquals(expectedThreads, termThreads, "Unexpected number of threads have terminated");
		tidy(threads);
	}

	@Test
	@Tag("Non-random")
	void testPhase2a() {
		// testing 2 groups

		//Random rand = new Random();
		//int x = rand.nextInt(50);

		// instantiate a random (even) number of threads
		//final int initThreads = (x%2 == 0 ? x : ++x);
		int x = 9;
		final int initThreads = (x%2 == 0 ? x : ++x);
		final int expectedThreadsPerGroup = largestMultipleFour(initThreads/2);
		System.out.println("Number of threads: "+initThreads);
		//final int initThreads = 50;
		IntrinsicSync intrinsic = new IntrinsicSync(Phase.TWO);
		Thread threads[] = new Thread[initThreads];

		// Even number required for this thread assignment strategy
		for (int i = 0 ; i < initThreads/2; i++){
			threads[i] = new Thread(new ThreadTester(intrinsic, 0));
			threads[i+(initThreads/2)] = new Thread((new ThreadTester(intrinsic, 1)));
			threads[i].start();
			threads[i+(initThreads/2)].start();
		}
		Thread threadG1[] = new Thread[initThreads/2];
		Thread threadG2[] = new Thread[initThreads/2];
		System.arraycopy(threads, 0, threadG1, 0, threadG1.length);
		System.arraycopy(threads, initThreads/2, threadG2, 0, threadG2.length);

		int termThreadsG1 = terminateThreads(threadG1, expectedThreadsPerGroup);
		int termThreadsG2 = terminateThreads(threadG2, expectedThreadsPerGroup);

		Stack threadStack = new Stack<Thread>();
		Stack groupZeroStack = new Stack<Thread>();
		Stack groupOneStack = new Stack<Thread>();

		for (Thread startedThreads : threads){
			if (startedThreads.getState() == Thread.State.TERMINATED) threadStack.push(startedThreads);
		}

		for (int i = 0 ; i < initThreads/2; i++){
			if (threads[i].getState() == Thread.State.TERMINATED) groupZeroStack.push(threads[i]);
			if (threads[i+(initThreads/2)].getState() == Thread.State.TERMINATED) groupOneStack.push(threads[i+(initThreads/2)]);
		}

		assertEquals(expectedThreadsPerGroup, termThreadsG1, "Group 0 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, termThreadsG2, "Group 1 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, groupZeroStack.size(), "Wrong number of threads in group 0");
		assertEquals(expectedThreadsPerGroup, groupOneStack.size(), "Wrong number of threads in group 1");
		assertEquals (0, threadStack.size() % 4, "Number of terminated threads should be a multiple of 4");
		tidy(threads);
		tidy(threadG1);
		tidy(threadG2);
		threadStack.clear();
		groupZeroStack.clear();
		groupOneStack.clear();

		for (Thread startedThreads : threads){

			if (startedThreads.getState() == Thread.State.BLOCKED){
				System.out.println("Blocked threads: "+startedThreads.getName());
			}
			if (startedThreads.getState() == Thread.State.RUNNABLE){
				System.out.println("Runnable threads: "+startedThreads.getName() );
				if (startedThreads.isInterrupted()){
					System.out.println("This thread is Interrupted");
				}

			}

			if (startedThreads.getState() == Thread.State.TERMINATED){
				System.out.println("Terminated threads: "+startedThreads.getName());
			}
		}
	}
	//etc

	@ParameterizedTest(name="Run {index}")
	@ValueSource(ints = {1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 25, 26, 36, 50, 100, 122})
	void testPhase2b(int threadBound){
		Random rand = new Random();

		int x = rand.nextInt(threadBound);

		// instantiate a random (even) number of threads
		final int initThreads = (x%2 == 0 ? x : ++x);
		final int expectedThreadsPerGroup = largestMultipleFour(initThreads/2);
		System.out.println("Number of threads: "+initThreads);
		//final int initThreads = 50;
		IntrinsicSync intrinsic = new IntrinsicSync(Phase.TWO);
		Thread threads[] = new Thread[initThreads];

		// Even number required for this thread assignment strategy
		for (int i = 0 ; i < initThreads/2; i++){
			threads[i] = new Thread(new ThreadTester(intrinsic, 0));
			threads[i+(initThreads/2)] = new Thread((new ThreadTester(intrinsic, 1)));
			threads[i].start();
			threads[i+(initThreads/2)].start();
		}

		Thread threadG1[] = new Thread[initThreads/2];
		Thread threadG2[] = new Thread[initThreads/2];
		System.arraycopy(threads, 0, threadG1, 0, threadG1.length);
		System.arraycopy(threads, initThreads/2, threadG2, 0, threadG2.length);

		int termThreadsG1 = terminateThreads(threadG1, expectedThreadsPerGroup);
		int termThreadsG2 = terminateThreads(threadG2, expectedThreadsPerGroup);

		Stack threadStack = new Stack<Thread>();
		Stack groupZeroStack = new Stack<Thread>();
		Stack groupOneStack = new Stack<Thread>();

		for (Thread startedThreads : threads){
			if (startedThreads.getState() == Thread.State.TERMINATED) threadStack.push(startedThreads);
		}

		for (int i = 0 ; i < initThreads/2; i++){
			if (threads[i].getState() == Thread.State.TERMINATED) groupZeroStack.push(threads[i]);
			if (threads[i+(initThreads/2)].getState() == Thread.State.TERMINATED) groupOneStack.push(threads[i+(initThreads/2)]);
		}

		assertEquals(expectedThreadsPerGroup, termThreadsG1, "Group 0 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, termThreadsG2, "Group 1 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, groupZeroStack.size(), "Wrong number of threads in group 0");
		assertEquals(expectedThreadsPerGroup, groupOneStack.size(), "Wrong number of threads in group 1");
		assertEquals (0, threadStack.size() % 4, "Number of terminated threads should be a multiple of 4");
		tidy(threads);
		tidy(threadG1);
		tidy(threadG2);
		threadStack.clear();
		groupZeroStack.clear();
		groupOneStack.clear();
	}

	@ParameterizedTest(name="Run {index}")
	@ValueSource(ints = {4, 7, 10, 12, 19, 20, 21, 27, 100/* HAMMER YOUR CPU, 500 */})
	void testPhase2c(int threadBound){
		Random rand = new Random();
		int x = rand.nextInt(threadBound);
		final int numOfGroups = x > 0 ? x : 1;
		final int initThreads = rand.nextInt(threadBound*15);
		System.out.println("Number of threads: "+initThreads);

		IntrinsicSync intrinsic = new IntrinsicSync(Phase.TWO);
		Thread threads[] = new Thread[initThreads];
		int groups[] = new int[numOfGroups + 1];

		for (int i = 0; i < initThreads; i++){
			int gid = rand.nextInt(numOfGroups);

			threads[i] = new Thread(new ThreadTester(intrinsic, gid));
			System.out.println("Thread "+threads[i]+" assigned to group "+gid);
			threads[i].start();
			groups[gid]++;
		}

		int expectedThreads = 0;

		for (int i = 0; i < numOfGroups; i++){
			System.out.println("Threads terminating from group " + i +": "+ largestMultipleFour(groups[i]) );
			expectedThreads += largestMultipleFour(groups[i]);
		}

		int termThreads = terminateThreads(threads, expectedThreads);

		Stack threadStack = new Stack<Thread>();

		for (Thread startedThreads : threads){
			if (startedThreads.getState() == Thread.State.TERMINATED) threadStack.push(startedThreads);
		}

		assertEquals (0, threadStack.size() % 4, "Number of terminated threads should be a multiple of 4");
		tidy(threads);
		threadStack.clear();
	}

	@ParameterizedTest(name="Run {index}")
	@ValueSource(ints = {1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 25, 26, 36, 50, 100, 122})
	@Tag("Non-random")
	void testPhase2d(int threadBound){

		final int initThreads = (threadBound%2 == 0 ? threadBound : ++threadBound);
		final int expectedThreadsPerGroup = largestMultipleFour(initThreads);
		System.out.println("Number of threads: "+initThreads);

		IntrinsicSync intrinsic = new IntrinsicSync(Phase.TWO);
		Thread threadG1[] = new Thread[initThreads];
		Thread threadG2[] = new Thread[initThreads];

		// Even number required for this thread assignment strategy
		for (int i = 0 ; i < initThreads; i++){
			threadG1[i] = new Thread(new ThreadTester(intrinsic, 0));
			threadG2[i] = new Thread(new ThreadTester(intrinsic, 1));
			threadG1[i].start();
			threadG2[i].start();
		}

		int termThreadsG1 = terminateThreads(threadG1, expectedThreadsPerGroup);
		int termThreadsG2 = terminateThreads(threadG2, expectedThreadsPerGroup);

		Stack groupZeroStack = new Stack<Thread>();
		Stack groupOneStack = new Stack<Thread>();

		for (int i = 0 ; i < initThreads; i++){

			if (threadG1[i].getState() == Thread.State.TERMINATED) groupZeroStack.push(threadG1[i]);
			if (threadG2[i].getState() == Thread.State.TERMINATED) groupOneStack.push(threadG2[i]);
		}

		assertEquals(expectedThreadsPerGroup, termThreadsG1, "Group 0 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, termThreadsG2, "Group 1 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, groupZeroStack.size(), "Wrong number of threads in group 0");
		assertEquals(expectedThreadsPerGroup, groupOneStack.size(), "Wrong number of threads in group 1");
		tidy(threadG1);
		tidy(threadG2);
		groupZeroStack.clear();
		groupOneStack.clear();

	}

	@ParameterizedTest(name="Run {index}")
	@ValueSource(ints = {0, 2, 3, 4, 5, 6, 8, 10, 11, 12, 25, 26, 36, 50, 100, 122})
	@Tag("Non-random")
	void testPhase3a(int threadBound){

		final int initThreads = (threadBound%2 == 0 ? threadBound : ++threadBound);
		final int expectedThreadsPerGroup = largestMultipleFour(initThreads);
		System.out.println("Number of threads per group: "+initThreads);

		IntrinsicSync intrinsic = new IntrinsicSync(Phase.THREE);
		Thread threadG1[] = new Thread[initThreads];
		Thread threadG2[] = new Thread[initThreads];

		// Even number required for this thread assignment strategy
		for (int i = 0 ; i < initThreads; i++){
			threadG1[i] = new Thread(new ThreadTester(intrinsic, 0));
			threadG2[i] = new Thread(new ThreadTester(intrinsic, 1));
			threadG1[i].start();
			threadG2[i].start();
		}

		int termThreadsG1 = terminateThreads(threadG1, expectedThreadsPerGroup);
		int termThreadsG2 = terminateThreads(threadG2, expectedThreadsPerGroup);

		Stack groupZeroStack = new Stack<Thread>();
		Stack groupOneStack = new Stack<Thread>();

		for (int i = 0 ; i < initThreads; i++){
			if (threadG1[i].getState() == Thread.State.TERMINATED) groupZeroStack.push(threadG1[i]);
			if (threadG2[i].getState() == Thread.State.TERMINATED) groupOneStack.push(threadG2[i]);
		}

		assertEquals(expectedThreadsPerGroup, termThreadsG1, "Group 0 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, termThreadsG2, "Group 1 wrong number of terminated threads");
		assertEquals(expectedThreadsPerGroup, groupZeroStack.size(), "Wrong number of threads in group 0");
		assertEquals(expectedThreadsPerGroup, groupOneStack.size(), "Wrong number of threads in group 1");
		tidy(threadG1);
		tidy(threadG2);
		groupZeroStack.clear();
		groupOneStack.clear();
	}

	@ParameterizedTest(name="Run {index}")
	@ValueSource(ints = {4, 7, 10, 12, 19, 20, 21, 27 /* HAMMER YOUR CPU, 500 */})
	void testPhase3b(int threadBound){
		Random rand = new Random();
		int x = rand.nextInt(threadBound);
		final int numOfGroups = x > 0 ? x : 1;
		final int initThreads = rand.nextInt(threadBound*10);
		System.out.println("Number of threads: "+initThreads);
		System.out.println("Number of groups: "+numOfGroups);

		IntrinsicSync intrinsic = new IntrinsicSync(Phase.THREE);
		Thread threads[] = new Thread[initThreads];
		int groups[] = new int[numOfGroups + 1];

		for (int i = 0; i < initThreads; i++){
			int gid = rand.nextInt(numOfGroups);

			threads[i] = new Thread(new ThreadTester(intrinsic, gid));
			//System.out.println("Thread "+threads[i]+" assigned to group "+gid);
			threads[i].start();
			groups[gid]++;
		}

		int expectedThreads = 0;

		for (int i = 0; i < numOfGroups; i++){
			//System.out.println("Threads terminating from group " + i +": "+ largestMultipleFour(groups[i]) );
			expectedThreads += largestMultipleFour(groups[i]);
		}

		int termThreads = terminateThreads(threads, expectedThreads);

		Stack threadStack = new Stack<Thread>();

		for (Thread startedThreads : threads){
			if (startedThreads.getState() == Thread.State.TERMINATED) threadStack.push(startedThreads);
		}

		assertEquals (0, threadStack.size() % 4, "Number of terminated threads should be a multiple of 4");
		tidy(threads);
		threadStack.clear();
	}

	@Test
	void testPhase3x() throws InterruptedException {
		IntrinsicSync intrinsic = new IntrinsicSync(Phase.THREE);
		final int initThreads = 8;
		Thread threads[] = new Thread[initThreads];
		int groups[] = new int[1];

		Stack waitingStack = new Stack<Thread>();
		Stack runnableStack = new Stack<Thread>();
		Stack terminatedStack = new Stack<Thread>();

		for (int i = 0; i < initThreads; i++){
			ThreadTester test = new ThreadTester(intrinsic, 1);
			test.setWaitTime(5000);
			threads[i] = new Thread(test);
			//System.out.println("Thread "+threads[i]+" assigned to group "+gid);
			threads[i].start();
		}

		doStuff(2000);
		for (int i = 0; i < initThreads; i++){
			System.out.println("State: "+threads[i].getState());
			if (threads[i].getState().equals(Thread.State.WAITING)) waitingStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.RUNNABLE)) runnableStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.TERMINATED)) terminatedStack.push(threads[i]);
		}
		assertEquals(4, waitingStack.size(), "Wrong number of waiting threads before 1st sleep");
		assertEquals(4, runnableStack.size(), "Wrong number of runnable threads before sleep");
		assertEquals(0, terminatedStack.size(), "Wrong number of terminated threads before sleep");

		Thread.sleep(1400);

		System.out.println("====After 3.5 sec===");

		waitingStack = new Stack<Thread>();
		runnableStack = new Stack<Thread>();
		terminatedStack = new Stack<Thread>();
		for (int i = 0; i < initThreads; i++){
			System.out.println("State: "+threads[i].getState());
			if (threads[i].getState().equals(Thread.State.WAITING)) waitingStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.RUNNABLE)) runnableStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.TERMINATED)) terminatedStack.push(threads[i]);
		}
		assertEquals(4, waitingStack.size(), "Wrong number of waiting threads after 1.4s");
		assertEquals(4, runnableStack.size(), "Wrong number of runnable threads after 1.4s");
		assertEquals(0, terminatedStack.size(), "Wrong number of terminated threads after 1.4s");

		Thread.sleep(3500);
		System.out.println("====After 7sec===");

		waitingStack = new Stack<Thread>();
		runnableStack = new Stack<Thread>();
		terminatedStack = new Stack<Thread>();
		for (int i = 0; i < initThreads; i++){
			System.out.println("State: "+threads[i].getState());
			if (threads[i].getState().equals(Thread.State.WAITING)) waitingStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.RUNNABLE)) runnableStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.TERMINATED)) terminatedStack.push(threads[i]);
		}
		assertEquals(0, waitingStack.size(), "Wrong number of waiting threads after 7s");
		assertEquals(4, runnableStack.size(), "Wrong number of runnable threads after 7s");
		assertEquals(4, terminatedStack.size(), "Wrong number of terminated threads after 7s");

		Thread.sleep(6000);
		System.out.println("====After 12sec===");

		waitingStack = new Stack<Thread>();
		runnableStack = new Stack<Thread>();
		terminatedStack = new Stack<Thread>();
		for (int i = 0; i < initThreads; i++){
			System.out.println("State: "+threads[i].getState());
			if (threads[i].getState().equals(Thread.State.WAITING)) waitingStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.RUNNABLE)) runnableStack.push(threads[i]);
			if (threads[i].getState().equals(Thread.State.TERMINATED)) terminatedStack.push(threads[i]);
		}
		assertEquals(0, waitingStack.size(), "Wrong number of waiting threads at end");
		assertEquals(0, runnableStack.size(), "Wrong number of runnable threads at end");
		assertEquals(8, terminatedStack.size(), "Wrong number of terminated threads at end");
	}

	private void doStuff(int howLong) {
		//try{ Thread.sleep(howLong);} catch (Exception e){System.out.println("Exception "+e.toString());}

		final long waitLimit = howLong;
		final long beginWait = ZonedDateTime.now().toInstant().toEpochMilli();

		long timeNow = ZonedDateTime.now().toInstant().toEpochMilli();

		//termThreads = finished.size();
		//while not terminated or timed out
		while ((timeNow - beginWait) <= waitLimit) {
			timeNow = ZonedDateTime.now().toInstant().toEpochMilli();
		}
	}

}
