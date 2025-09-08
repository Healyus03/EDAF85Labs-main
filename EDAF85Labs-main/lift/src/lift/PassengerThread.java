package lift;

public class PassengerThread extends Thread {
    private final LiftMonitor monitor;
    private final LiftView view;

    public PassengerThread(LiftMonitor monitor, LiftView view) {
        this.monitor = monitor;
        this.view = view;
    }

    @Override
    public void run() {
        while (true) {
            Passenger passenger = view.createPassenger();
            int startFloor = passenger.getStartFloor();
            int destFloor = passenger.getDestinationFloor();
            passenger.begin();
            try {
                monitor.requestEnter(startFloor, destFloor);
            } catch (InterruptedException e) {
                return;
            }
            passenger.enterLift();
            monitor.enterDone();
            // Wait until lift reaches destination and doors are open
            while (true) {
                synchronized (monitor) {
                    if (monitor.getCurrentFloor() == destFloor && monitor.isDoorsOpen()) {
                        break;
                    }
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            try {
                monitor.requestExit(destFloor);
            } catch (InterruptedException e) {
                return;
            }
            passenger.exitLift();
            monitor.exitDone();
            passenger.end();
        }
    }
}