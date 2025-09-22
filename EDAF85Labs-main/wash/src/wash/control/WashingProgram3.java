package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;

import static wash.control.WashingMessage.Order.*;

/**
 * Program 3 for washing machine. This also serves as an example of how washing
 * programs can be structured.
 * 
 * This short program stops all regulation of temperature and water levels,
 * stops the barrel from spinning, and drains the machine of water.
 * 
 * It can be used after an emergency stop (program 0) or a power failure.
 */
public class WashingProgram3 extends ActorThread<WashingMessage> {

    private WashingIO io;
    private ActorThread<WashingMessage> temp;
    private ActorThread<WashingMessage> water;
    private ActorThread<WashingMessage> spin;
    
    public WashingProgram3(WashingIO io,
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
            System.out.println("washing program 3 started");
            
            // Deactivate heating and spinning
            temp.send(new WashingMessage(this, TEMP_IDLE));
            receive(); // Wait for ACK
            spin.send(new WashingMessage(this, SPIN_OFF));
            receive(); // Wait for ACK

            // Start draining
            water.send(new WashingMessage(this, WATER_DRAIN_UNTIL_EMPTY));
            receive(); // Wait for ACK

            // Wait until the barrel is empty.
            // The water level is checked periodically.
            while (io.getWaterLevel() > 0) {
                Thread.sleep(Math.max(50, 300 / Settings.SPEEDUP));
            }

            // Stop the drain pump
            water.send(new WashingMessage(this, WATER_IDLE));
            receive(); // Wait for ACK

            // Unlock hatch
            io.lock(false);
            
            System.out.println("washing program 3 finished");
        } catch (InterruptedException e) {
            
            // If we end up here, it means the program was interrupt()'ed:
            // set all controllers to idle

            temp.send(new WashingMessage(this, TEMP_IDLE));
            water.send(new WashingMessage(this, WATER_IDLE));
            spin.send(new WashingMessage(this, SPIN_OFF));
            System.out.println("washing program terminated");
        }
    }
}
