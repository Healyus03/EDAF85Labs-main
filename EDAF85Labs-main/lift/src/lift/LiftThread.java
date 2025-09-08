package lift;

import lift.LiftView;

public class LiftThread extends Thread {
    private final LiftMonitor monitor;
    private final LiftView view;
    private final int nbrFloors;

    public LiftThread(LiftMonitor monitor, LiftView view, int nbrFloors) {
        this.monitor = monitor;
        this.view = view;
        this.nbrFloors = nbrFloors;
    }

    @Override
    public void run() {
        int floor = 0;
        int direction = 1;
        while (true) {
            int nextFloor = floor + direction;
            if (nextFloor < 0 || nextFloor >= nbrFloors) {
                direction = -direction;
                nextFloor = floor + direction;
            }
            view.moveLift(floor, nextFloor);
            floor = nextFloor;
            if (monitor.shouldStopAtFloor(floor)) {
                monitor.liftArrived(floor);
                view.openDoors(floor);
                monitor.doorsFullyOpen(); // Signal passengers that doors are ready
                try {
                    monitor.liftReadyToMove(); // Wait for passengers to finish
                } catch (InterruptedException e) {
                    break;
                }
                monitor.doorsReadyToClose(); // Block passengers before closing doors
                view.closeDoors();
            }
        }
    }
}
