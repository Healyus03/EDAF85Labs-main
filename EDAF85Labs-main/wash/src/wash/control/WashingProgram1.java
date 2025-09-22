package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;

import static wash.control.WashingMessage.Order.*;

/**
 * Program 1 for washing machine. This is a copy of WashingProgram3.
 * You can modify this class to implement a different washing sequence.
 */
public class WashingProgram1 extends ActorThread<WashingMessage> {

    private WashingIO io;
    private ActorThread<WashingMessage> temp;
    private ActorThread<WashingMessage> water;
    private ActorThread<WashingMessage> spin;

    public WashingProgram1(WashingIO io,
                           ActorThread<WashingMessage> temp,
                           ActorThread<WashingMessage> water,
                           ActorThread<WashingMessage> spin)
    {
        this.io = io;
        this.temp = temp;
        this.water = water;
        this.spin = spin;
    }

    @Override
    public void run() {
        try {
            // Lock the hatch
            io.lock(true);

            // Fill to 10L
            water.send(new WashingMessage(this, WATER_FILL));
            receive(); // wait for ACK
            // Wait until water level >= 10L
            while (io.getWaterLevel() < 10) Thread.sleep(Math.max(50, 100 / Settings.SPEEDUP));

            // Start slow spin
            spin.send(new WashingMessage(this, SPIN_SLOW));
            receive();

            // Heat to 40C
            temp.send(new WashingMessage(this, TEMP_SET_40));
            receive();
            // Wait until temp >= 38C and < 40C (lower bound, but not upper bound)
            while (io.getTemperature() < 38 || io.getTemperature() >= 40) Thread.sleep(Math.max(50, 100 / Settings.SPEEDUP));

            // Keep temp for 30 min, but abort if temp >= 40C
            long washEnd = System.currentTimeMillis() + (30 * 60000 / Settings.SPEEDUP);
            while (System.currentTimeMillis() < washEnd) {
                if (io.getTemperature() >= 40) break;
                Thread.sleep(Math.max(50, 1000 / Settings.SPEEDUP));
            }

            // Stop heating
            temp.send(new WashingMessage(this, TEMP_IDLE));
            receive();

            // Drain
            water.send(new WashingMessage(this, WATER_DRAIN));
            receive();
            while (io.getWaterLevel() > 0) Thread.sleep(Math.max(50, 100 / Settings.SPEEDUP));

            // Rinse 5x: fill, spin slow, 2 min, drain
            for (int i = 0; i < 5; i++) {
                water.send(new WashingMessage(this, WATER_FILL));
                receive();
                while (io.getWaterLevel() < 10) Thread.sleep(Math.max(50, 100 / Settings.SPEEDUP));
                spin.send(new WashingMessage(this, SPIN_SLOW));
                receive();
                Thread.sleep(2 * 60000 / Settings.SPEEDUP);
                water.send(new WashingMessage(this, WATER_DRAIN));
                receive();
                while (io.getWaterLevel() > 0) Thread.sleep(Math.max(50, 100 / Settings.SPEEDUP));
            }

            // Centrifuge: spin fast, drain, 5 min
            spin.send(new WashingMessage(this, SPIN_FAST));
            receive();
            water.send(new WashingMessage(this, WATER_DRAIN_UNTIL_EMPTY));
            receive();
            long centrifugeEnd = System.currentTimeMillis() + (5 * 60000 / Settings.SPEEDUP);
            while (System.currentTimeMillis() < centrifugeEnd) {
                Thread.sleep(Math.max(50, 100 / Settings.SPEEDUP));
            }

            // Stop everything
            spin.send(new WashingMessage(this, SPIN_OFF));
            receive();
            water.send(new WashingMessage(this, WATER_IDLE));
            receive();

            // Unlock hatch
            io.lock(false);
        } catch (InterruptedException e) {
            temp.send(new WashingMessage(this, TEMP_IDLE));
            water.send(new WashingMessage(this, WATER_IDLE));
            spin.send(new WashingMessage(this, SPIN_OFF));
            System.out.println("washing program terminated");
        }
    }
}
