package lift;

public class LiftMonitor {
    private int currentFloor;
    private int direction; // 1 = up, -1 = down
    private int passengersInLift;
    private final int maxPassengers;
    private final int nbrFloors;
    private final int[] waitingToEnter;
    private final int[] waitingToExit;
    private final int[] passengersWantToExit;
    private boolean doorsOpen;
    private boolean doorsReady = false;
    private int enteringCount = 0;
    private int exitingCount = 0;

    public LiftMonitor(int nbrFloors, int maxPassengers) {
        this.nbrFloors = nbrFloors;
        this.maxPassengers = maxPassengers;
        this.currentFloor = 0;
        this.direction = 1;
        this.passengersInLift = 0;
        this.waitingToEnter = new int[nbrFloors];
        this.waitingToExit = new int[nbrFloors];
        this.passengersWantToExit = new int[nbrFloors];
        this.doorsOpen = false;
    }

    // Called by LiftThread after doors are open and animation is complete
    public synchronized void doorsFullyOpen() {
        doorsReady = true;
        notifyAll();
    }

    // Called by LiftThread before closing doors
    public synchronized void doorsReadyToClose() {
        doorsReady = false;
        notifyAll();
    }

    // Modified requestEnter to register destination
    public synchronized void requestEnter(int startFloor, int destFloor) throws InterruptedException {
        waitingToEnter[startFloor]++;
        while (!doorsOpen || !doorsReady || currentFloor != startFloor || passengersInLift >= maxPassengers) {
            wait();
        }
        waitingToEnter[startFloor]--;
        passengersInLift++;
        passengersWantToExit[destFloor]++;
        enteringCount++;
    }

    public synchronized void enterDone() {
        enteringCount--;
        notifyAll();
    }

    // Modified requestExit to unregister destination
    public synchronized void requestExit(int destFloor) throws InterruptedException {
        waitingToExit[destFloor]++;
        while (!doorsOpen || !doorsReady || currentFloor != destFloor) {
            wait();
        }
        waitingToExit[destFloor]--;
        passengersInLift--;
        passengersWantToExit[destFloor]--;
        exitingCount++;
    }

    public synchronized void exitDone() {
        exitingCount--;
        notifyAll();
    }

    public synchronized void liftArrived(int floor) {
        currentFloor = floor;
        doorsOpen = true;
        notifyAll();
    }

    public synchronized void liftReadyToMove() throws InterruptedException {
        while ((waitingToEnter[currentFloor] > 0 && passengersInLift < maxPassengers) ||
               waitingToExit[currentFloor] > 0 ||
               enteringCount > 0 ||
               exitingCount > 0) {
            wait();
        }
        doorsOpen = false;
    }

    public synchronized int getCurrentFloor() {
        return currentFloor;
    }

    public synchronized boolean isDoorsOpen() {
        return doorsOpen;
    }

    // Update shouldStopAtFloor to use passengersWantToExit
    public synchronized boolean shouldStopAtFloor(int floor) {
        boolean canEnter = waitingToEnter[floor] > 0 && passengersInLift < maxPassengers;
        boolean canExit = passengersWantToExit[floor] > 0;
        return canEnter || canExit;
    }
}
