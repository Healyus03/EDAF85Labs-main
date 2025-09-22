package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;

public class TemperatureController extends ActorThread<WashingMessage> {

    private final WashingIO io;
    private double targetTemp = 0;
    private boolean heaterOn = false;
    private boolean running = true;
    // Section 3.1.4: Margins with extra safety
    private static final double MU_UPPER = 0.68; // μu, further increased for more safety
    private static final double MU_LOWER = 0.30; // μl = 0.095 + 0.2
    private static final long PERIOD = 10000 / Settings.SPEEDUP; // 10s simulated

    public TemperatureController(WashingIO io) {
        this.io = io;
    }

    @Override
    public void run() {
        try {
            while (running) {
                WashingMessage msg = receiveWithTimeout(PERIOD);
                if (msg != null) {
                    switch (msg.order()) {
                        case TEMP_SET_40 -> targetTemp = 40;
                        case TEMP_SET_60 -> targetTemp = 60;
                        case TEMP_IDLE -> {
                            targetTemp = 0;
                            io.heat(false);
                        }
                        default -> {}
                    }
                    // Always ACK
                    msg.sender().send(new WashingMessage(this, WashingMessage.Order.ACKNOWLEDGMENT));
                }

                if (targetTemp > 0 && io.getWaterLevel() > 0) {
                    double t = io.getTemperature();
                    double lowerBound = targetTemp - 2;
                    double upperBound = targetTemp;
                    // Hysteresis: ON when T <= (lowerBound + μl), OFF when T >= (upperBound - μu)
                    if (!heaterOn && t <= lowerBound + MU_LOWER) {
                        io.heat(true);
                        heaterOn = true;
                    } else if (heaterOn && t >= upperBound - MU_UPPER) {
                        io.heat(false);
                        heaterOn = false;
                    }
                    // Otherwise, keep previous heater state
                } else {
                    io.heat(false);
                    heaterOn = false;
                }
            }
        } catch (InterruptedException e) {
            io.heat(false);
        }
    }
}
