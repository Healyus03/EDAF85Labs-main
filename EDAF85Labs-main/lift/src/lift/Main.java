package lift;

public class Main {
    public static final int NBR_FLOORS = 30;
    public static final int MAX_PASSENGERS = 20;
    public static final int NBR_PASSENGER_THREADS = 200;

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
            Passenger passenger = view.createPassenger();
            // If Passenger implementation allows, set start/dest floors here. Otherwise, rely on view.createPassenger()
            passengerThreads[i] = new PassengerThread(monitor, view);
            passengerThreads[i].start();
        }
        // Optionally, join all passenger threads if you want to wait for them to finish
        for (Thread t : passengerThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}