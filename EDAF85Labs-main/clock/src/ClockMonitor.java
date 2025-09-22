import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClockMonitor {
    private int clockHours, clockMinutes, clockSeconds;
    private int alarmHours, alarmMinutes, alarmSeconds;
    private boolean alarmEnabled;
    private boolean alarmSounding;
    private boolean settingTime;
    private boolean settingAlarm;
    private int alarmBeepCount;
    private final Lock lock = new ReentrantLock();

    private int pendingSetTimeHours, pendingSetTimeMinutes, pendingSetTimeSeconds;
    private volatile boolean pendingSetTimeValid;

    public ClockMonitor(int h, int m, int s) {
        clockHours = h; clockMinutes = m; clockSeconds = s;
        alarmHours = 0; alarmMinutes = 0; alarmSeconds = 0;
        alarmEnabled = false;
        alarmSounding = false;
        settingTime = false;
        settingAlarm = false;
        alarmBeepCount = 0;
    }

    public void tick() {
        lock.lock();
        try {
            if (!settingTime && !settingAlarm) {
                clockSeconds++;
                if (clockSeconds >= 60) { clockSeconds = 0; clockMinutes++; }
                if (clockMinutes >= 60) { clockMinutes = 0; clockHours++; }
                if (clockHours >= 24) { clockHours = 0; }
            }
            // Always allow reading the current time, even if not ticking
        } finally { lock.unlock(); }
    }

    public int getClockHours() { lock.lock(); try { return clockHours; } finally { lock.unlock(); } }
    public int getClockMinutes() { lock.lock(); try { return clockMinutes; } finally { lock.unlock(); } }
    public int getClockSeconds() { lock.lock(); try { return clockSeconds; } finally { lock.unlock(); } }

    public void setClockTime(int h, int m, int s) {
        lock.lock();
        try {
            pendingSetTimeHours = h;
            pendingSetTimeMinutes = m;
            pendingSetTimeSeconds = s;
            pendingSetTimeValid = true;
        } finally { lock.unlock(); }
    }

    public boolean applyPendingSetTime() {
        lock.lock();
        try {
            if (pendingSetTimeValid) {
                clockHours = pendingSetTimeHours;
                clockMinutes = pendingSetTimeMinutes;
                clockSeconds = pendingSetTimeSeconds;
                pendingSetTimeValid = false;
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public void setAlarmTime(int h, int m, int s) {
        lock.lock();
        try {
            settingAlarm = true;
            alarmHours = h; alarmMinutes = m; alarmSeconds = s;
            settingAlarm = false;
        } finally { lock.unlock(); }
    }


    public boolean isAlarmEnabled() { lock.lock(); try { return alarmEnabled; } finally { lock.unlock(); } }
    public void setAlarmEnabled(boolean enabled) { lock.lock(); try { alarmEnabled = enabled; } finally { lock.unlock(); } }

    public boolean isAlarmSounding() { lock.lock(); try { return alarmSounding; } finally { lock.unlock(); } }

    public boolean shouldSoundAlarm() {
        lock.lock();
        try {
            return alarmEnabled && !alarmSounding &&
                clockHours == alarmHours && clockMinutes == alarmMinutes && clockSeconds == alarmSeconds;
        } finally { lock.unlock(); }
    }

    public void startAlarmBeeping() {
        lock.lock();
        try {
            alarmSounding = true;
            alarmBeepCount = 20;
        } finally { lock.unlock(); }
    }

    public void stopAlarmBeeping() {
        lock.lock();
        try {
            alarmSounding = false;
            alarmBeepCount = 0;
        } finally { lock.unlock(); }
    }

    public boolean shouldBeepAlarm() {
        lock.lock();
        try {
            return alarmSounding && alarmBeepCount > 0;
        } finally { lock.unlock(); }
    }

    public void decrementAlarmBeep() {
        lock.lock();
        try {
            if (alarmBeepCount > 0) alarmBeepCount--;
            if (alarmBeepCount == 0) alarmSounding = false;
        } finally { lock.unlock(); }
    }
}
