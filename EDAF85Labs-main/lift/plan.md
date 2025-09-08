


### **Task Summary**

You need to implement a **simulation of a passenger lift (elevator)** in Java.

* The lift moves up and down between floors.
* Passengers arrive randomly, enter the lift, and ride to their destination floor.
* Each passenger and the lift itself run as separate threads.
* Synchronization between passenger threads and the lift thread must be handled using a **monitor** (`synchronized` methods with `wait()`/`notifyAll()`).
* The GUI is provided by the class **LiftView**.
* **Passenger** objects are used to visualize the passengers’ journey.

---

### **Available Classes and Methods**

#### **LiftView**

```java
public class LiftView {
    public LiftView(int nbrFloors, int maxPassengers);
    public void moveLift(int here, int next);
    public Passenger createPassenger();
    public void openDoors(int floor);
    public void closeDoors();
    public void showDebugInfo(int[] nbrEntry, int[] nbrExit);
}
```

#### **Passenger**

```java
public interface Passenger {
    int getStartFloor();
    int getDestinationFloor();
    void begin();     // walk in from the left
    void enterLift(); // enter the lift
    void exitLift();  // exit the lift
    void end();       // walk out to the right
}
```

---

### **Requirements**

1. **Threads**

    * A single thread for the lift (`LiftThread`).
    * One thread per passenger (`PassengerThread`).

2. **Monitor (LiftMonitor)**

    * Tracks shared state:

        * Current floor.
        * Direction (up/down).
        * Number of passengers inside the lift.
        * Passengers waiting to enter/exit at each floor.
        * Whether doors are open or closed.
    * Provides synchronized methods, for example:

        * `requestEnter()`, `enterDone()`
        * `requestExit()`, `exitDone()`
        * `liftArrived()`, `liftReadyToMove()`
    * Uses `wait()`/`notifyAll()` for synchronization.

3. **Lift Behavior**

    * Starts at floor 0.
    * Moves up to the top floor, then down, continuously.
    * At each floor:

        * Open doors.
        * Wait until passengers finish entering/exiting.
        * Close doors.
    * Must not move while passengers are entering or exiting.
    * Extended version: The lift should stop if no passengers are waiting or inside.

4. **Passenger Behavior**

    * Created continuously using `view.createPassenger()`, but only a limited number of active passengers (e.g., 20).
    * Each passenger thread:

        1. Calls `begin()` (walks toward the lift).
        2. Waits until allowed to enter via `monitor.requestEnter()`.
        3. Calls `enterLift()`.
        4. Signals completion with `monitor.enterDone()`.
        5. Waits until destination floor is reached.
        6. Calls `monitor.requestExit()`.
        7. Calls `exitLift()`.
        8. Signals completion with `monitor.exitDone()`.
        9. Calls `end()` (walks out of view).

---

### **Design Guidelines**

* Always use `wait()` inside a `while` loop to re-check conditions when the thread is woken up.
* Do not call `moveLift()` inside the monitor (it takes a long time to execute and would block other threads).
* Start with a simple version: only one passenger can enter/exit at a time. Then extend to allow multiple passengers simultaneously.
* Use `showDebugInfo()` for debugging state (waiting/entering/exiting counts per floor).

---

### **Your Task**

Implement the following:

* `LiftMonitor`: synchronized monitor that controls access to the shared state.
* `LiftThread`: simulates the lift moving between floors.
* `PassengerThread`: simulates a passenger’s journey.
* `Main`: creates the lift system, starts the lift thread, and spawns passenger threads.

Follow the requirements above and ensure proper synchronization between threads.

---

Do you want me to also provide a **Java skeleton with empty class/method definitions** that Copilot can then complete?
