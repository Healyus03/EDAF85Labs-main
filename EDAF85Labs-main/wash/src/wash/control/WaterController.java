package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;

public class WaterController extends ActorThread<WashingMessage> {

    private final WashingIO io;
    private double targetLevel = 0;
    private Mode mode = Mode.IDLE;
    private boolean running = true;
    // Section 3.1.4: Periodic control, safe state, and ACKs
    private static final long PERIOD = Math.max(10, 300 / Settings.SPEEDUP); // 300ms simulated, min 10ms
    private static final double DEFAULT_TARGET = 10.0; // Default fill target

    private enum Mode { IDLE, FILL, DRAIN, DRAIN_UNTIL_EMPTY }

    public WaterController(WashingIO io) {
        this.io = io;
    }

    @Override
    public void run() {
        try {
            while (running) {
                WashingMessage msg = receiveWithTimeout(PERIOD);
                if (msg != null) {
                    switch (msg.order()) {
                        case WATER_FILL -> {
                            mode = Mode.FILL;
                            targetLevel = DEFAULT_TARGET; // Default, or parse from message if extended
                        }
                        case WATER_DRAIN -> {
                            mode = Mode.DRAIN;
                        }
                        case WATER_DRAIN_UNTIL_EMPTY -> {
                            mode = Mode.DRAIN_UNTIL_EMPTY;
                        }
                        case WATER_IDLE -> {
                            mode = Mode.IDLE;
                            io.fill(false);
                            io.drain(false);
                        }
                        default -> {}
                    }
                    // Always ACK
                    msg.sender().send(new WashingMessage(this, WashingMessage.Order.ACKNOWLEDGMENT));
                }

                double level = io.getWaterLevel();
                switch (mode) {
                    case FILL -> {
                        io.drain(false); // Never fill and drain at the same time
                        if (level < targetLevel) {
                            io.fill(true);
                        } else {
                            io.fill(false);
                        }
                    }
                    case DRAIN -> {
                        io.fill(false); // Never fill and drain at the same time
                        if (level > 0) {
                            io.drain(true);
                        } else {
                            io.drain(false);
                        }
                    }
                    case DRAIN_UNTIL_EMPTY -> {
                        io.fill(false);
                        io.drain(true);
                    }
                    case IDLE -> {
                        io.fill(false);
                        io.drain(false);
                    }
                }
            }
        } catch (InterruptedException e) {
            io.fill(false);
            io.drain(false);
        }
    }
}
