package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;

import static wash.control.WashingMessage.Order.*;

/**
 * Program 2 (white wash):
 * - 20 min pre-wash at 40°C
 * - Drain and refill
 * - 30 min main wash at 60°C
 * - 5x rinse (2 min, cold)
 * - Centrifuge 5 min
 */
public class WashingProgram2 extends ActorThread<WashingMessage> {
    private WashingIO io;
    private ActorThread<WashingMessage> temp;
    private ActorThread<WashingMessage> water;
    private ActorThread<WashingMessage> spin;

    public WashingProgram2(WashingIO io,
                             ActorThread<WashingMessage> temp,
                             ActorThread<WashingMessage> water,
                             ActorThread<WashingMessage> spin) {
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

            // --- Pre-wash: fill, spin slow, heat to 40C, 20 min ---
            water.send(new WashingMessage(this, WATER_FILL));
            receive();
            while (io.getWaterLevel() < 10) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));
            spin.send(new WashingMessage(this, SPIN_SLOW));
            receive();
            temp.send(new WashingMessage(this, TEMP_SET_40));
            receive();
            while (io.getTemperature() < 38 || io.getTemperature() >= 40) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));
            long preWashEnd = System.currentTimeMillis() + (20 * 60000 / Settings.SPEEDUP);
            while (System.currentTimeMillis() < preWashEnd) {
                if (io.getTemperature() >= 40) break;
                Thread.sleep(1000);
            }
            temp.send(new WashingMessage(this, TEMP_IDLE));
            receive();

            // Drain pre-wash water
            water.send(new WashingMessage(this, WATER_DRAIN));
            receive();
            while (io.getWaterLevel() > 0) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));

            // --- Main wash: fill, spin slow, heat to 60C, 30 min ---
            water.send(new WashingMessage(this, WATER_FILL));
            receive();
            while (io.getWaterLevel() < 10) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));
            spin.send(new WashingMessage(this, SPIN_SLOW));
            receive();
            temp.send(new WashingMessage(this, TEMP_SET_60));
            receive();
            while (io.getTemperature() < 58 || io.getTemperature() >= 60) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));
            long mainWashEnd = System.currentTimeMillis() + (30 * 60000 / Settings.SPEEDUP);
            while (System.currentTimeMillis() < mainWashEnd) {
                if (io.getTemperature() >= 60) break;
                Thread.sleep(1000);
            }
            temp.send(new WashingMessage(this, TEMP_IDLE));
            receive();

            // Drain main wash water
            water.send(new WashingMessage(this, WATER_DRAIN));
            receive();
            while (io.getWaterLevel() > 0) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));

            // --- Rinse 5x: fill, spin slow, 2 min, drain ---
            for (int i = 0; i < 5; i++) {
                water.send(new WashingMessage(this, WATER_FILL));
                receive();
                while (io.getWaterLevel() < 10) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));
                spin.send(new WashingMessage(this, SPIN_SLOW));
                receive();
                Thread.sleep(2 * 60000 / Settings.SPEEDUP);
                water.send(new WashingMessage(this, WATER_DRAIN));
                receive();
                while (io.getWaterLevel() > 0) Thread.sleep(Math.max(1, 100 / Settings.SPEEDUP));
            }

            // --- Centrifuge: spin fast, drain, 5 min ---
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
