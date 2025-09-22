package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;
import wash.io.WashingIO.Spin;

public class SpinController extends ActorThread<WashingMessage> {

    private WashingIO io;
    private WashingIO.Spin currentSpin = WashingIO.Spin.IDLE;
    private boolean running = true;
    private boolean slowLeft = true;

    public SpinController(WashingIO io) {
        this.io = io;
    }

    @Override
    public void run() {
        try {
            while (running) {
                WashingMessage m = receiveWithTimeout(60000 / Settings.SPEEDUP);
                if (m != null) {
                    switch (m.order()) {
                        case SPIN_SLOW:
                            currentSpin = slowLeft ? WashingIO.Spin.LEFT : WashingIO.Spin.RIGHT;
                            io.setSpinMode(currentSpin);
                            slowLeft = !slowLeft;
                            m.sender().send(new WashingMessage(this, WashingMessage.Order.ACKNOWLEDGMENT));
                            break;
                        case SPIN_FAST:
                            currentSpin = WashingIO.Spin.FAST;
                            io.setSpinMode(currentSpin);
                            m.sender().send(new WashingMessage(this, WashingMessage.Order.ACKNOWLEDGMENT));
                            break;
                        case SPIN_OFF:
                            currentSpin = WashingIO.Spin.IDLE;
                            io.setSpinMode(currentSpin);
                            m.sender().send(new WashingMessage(this, WashingMessage.Order.ACKNOWLEDGMENT));
                            break;
                        default:
                            // ignore unknown commands
                    }
                } else {
                    // Timeout: if in SLOW mode, alternate direction
                    if (currentSpin == WashingIO.Spin.LEFT || currentSpin == WashingIO.Spin.RIGHT) {
                        currentSpin = (currentSpin == WashingIO.Spin.LEFT) ? WashingIO.Spin.RIGHT : WashingIO.Spin.LEFT;
                        io.setSpinMode(currentSpin);
                    }
                }
            }
        } catch (InterruptedException unexpected) {
            throw new Error(unexpected);
        }
    }
}
