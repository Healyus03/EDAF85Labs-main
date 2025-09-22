package lift;

public class Main {
    public static final int NBR_FLOORS = 7;
    public static final int MAX_PASSENGERS = 4;
    public static final int NBR_PASSENGER_THREADS = 20;

    public static void main(String[] args) {
        LiftView view = new LiftView(NBR_FLOORS, MAX_PASSENGERS);
        LiftMonitor monitor = new LiftMonitor(NBR_FLOORS, MAX_PASSENGERS);
        LiftThread liftThread = new LiftThread(monitor, view, NBR_FLOORS);
        liftThread.start();

        java.util.Random rand = new java.util.Random();
        Thread[] passengerThreads = new Thread[NBR_PASSENGER_THREADS];
        for (int i = 0; i < NBR_PASSENGER_THREADS; i++) {
            int startFloor = rand.nextInt(NBR_FLOORS);
            int destFloor;
            do {
                destFloor = rand.nextInt(NBR_FLOORS);
            } while (destFloor == startFloor);
            passengerThreads[i] = new PassengerThread(monitor, view);
            passengerThreads[i].start();
        }
    }
}