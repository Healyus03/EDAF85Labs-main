import clock.io.Choice;
import clock.io.ClockInput.UserInput;
import clock.io.ClockInput;
import clock.io.ClockOutput;
import clock.AlarmClockEmulator;

public class ClockMain {
    public static void main(String[] args) {
        AlarmClockEmulator emulator = new AlarmClockEmulator();
        ClockInput in = emulator.getInput();
        ClockOutput out = emulator.getOutput();
        ClockMonitor monitor = new ClockMonitor(15, 2, 37);
        out.displayTime(monitor.getClockHours(), monitor.getClockMinutes(), monitor.getClockSeconds());

        Thread clockThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    long start = System.currentTimeMillis();
                    boolean setApplied = monitor.applyPendingSetTime();
                    if (!setApplied) {
                        monitor.tick();
                    }
                    int ch = monitor.getClockHours();
                    int cm = monitor.getClockMinutes();
                    int cs = monitor.getClockSeconds();
                    out.displayTime(ch, cm, cs);
                    if (monitor.shouldSoundAlarm()) {
                        monitor.startAlarmBeeping();
                    }
                    if (monitor.shouldBeepAlarm()) {
                        out.alarm();
                        monitor.decrementAlarmBeep();
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    long sleepTime = 1000 - elapsed;
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        clockThread.start();

        Thread inputThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try { in.getSemaphore().acquire(); } catch (InterruptedException e) { break; }
                    UserInput userInput = in.getUserInput();
                    Choice c = userInput.choice();
                    int h = userInput.hours();
                    int m = userInput.minutes();
                    int s = userInput.seconds();
                    switch (c) {
                        case SET_TIME:
                            monitor.setClockTime(h, m, s);
                            break;
                        case SET_ALARM:
                            monitor.setAlarmTime(h, m, s);
                            out.setAlarmIndicator(true);
                            monitor.setAlarmEnabled(true);
                            break;
                        case TOGGLE_ALARM:
                            if (monitor.isAlarmSounding()) {
                                monitor.stopAlarmBeeping();
                            } else {
                                boolean enabled = !monitor.isAlarmEnabled();
                                monitor.setAlarmEnabled(enabled);
                                out.setAlarmIndicator(enabled);
                            }
                            break;
                    }
                }
            }
        });
        inputThread.start();
    }
}
