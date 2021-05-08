import java.time.ZonedDateTime;

public class ThreadTester implements Runnable {
    AtomicSync atomic = null;
    SemaphoreSync semp = null;
    ExtrinsicSync extS = null;
    IntrinsicSync intS = null;
    public int gid = 0;
    public int waitTime = 5;
    Phase phase;
    Init concept;
    public Boolean terminated = false;

    enum Init {ATOMIC, SEMAPHORE, EXTRINSIC, INTRINSIC}

    public ThreadTester(AtomicSync a){
        this.atomic = a;
        this.concept = Init.ATOMIC;
    }

    public ThreadTester(SemaphoreSync s){
        this.semp = s;
        this.concept = Init.SEMAPHORE;
    }

    public ThreadTester(ExtrinsicSync e){
        this.extS = e;
        this.concept = Init.EXTRINSIC;
    }

    public ThreadTester(IntrinsicSync i){
        this.intS = i;
        this.concept = Init.INTRINSIC;
    }


    public ThreadTester(AtomicSync a, int id){
        this.atomic = a;
        this.concept = Init.ATOMIC;
        this.gid = id;
        this.phase = a.phase;
    }

    public ThreadTester(SemaphoreSync s, int id){
        this.semp = s;
        this.concept = Init.SEMAPHORE;
        this.gid = id;
        this.phase = s.phase;
    }

    public ThreadTester(ExtrinsicSync e, int id){
        this.extS = e;
        this.concept = Init.EXTRINSIC;
        this.gid = id;
        this.phase = e.phase;
    }

    public ThreadTester(IntrinsicSync i, int id){
        this.intS = i;
        this.concept = Init.INTRINSIC;
        this.gid = id;
        this.phase = i.phase;
    }

    private void pickMethod(){
        if (phase == Phase.ONE){
            switch (concept){
                case ATOMIC: atomic.waitForThreads();
                    break;
                case SEMAPHORE: semp.waitForThreads();
                    break;
                case EXTRINSIC: extS.waitForThreads();
                    break;
                case INTRINSIC: intS.waitForThreads();
                    break;
            }
        } else {
            switch (concept){
                case ATOMIC: atomic.waitForThreadsInGroup(gid);
                    break;
                case SEMAPHORE: semp.waitForThreadsInGroup(gid);
                    break;
                case EXTRINSIC: extS.waitForThreadsInGroup(gid);
                    break;
                case INTRINSIC: intS.waitForThreadsInGroup(gid);
                    break;
            }
        }
    }

    public void run(){
        this.pickMethod();
        //System.out.println("This is a thread from group: "+gid+" executing");
        this.doStuff(waitTime);
        this.done();
        terminated = true;
    }

    public void setWaitTime(int ms){
        this.waitTime = ms;
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

    private void done(){
        if(phase == Phase.THREE){
            switch (concept){
                case ATOMIC: atomic.finished(gid);
                    break;
                case SEMAPHORE: semp.finished(gid);
                    break;
                case EXTRINSIC: extS.finished(gid);
                    break;
                case INTRINSIC: intS.finished(gid);
                    break;
            }
        }
    }
}
