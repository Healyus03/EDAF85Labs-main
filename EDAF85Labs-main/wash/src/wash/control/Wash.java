package wash.control;

import actor.ActorThread;
import wash.io.WashingIO;
import wash.simulation.WashingSimulator;

public class Wash {

    public static void main(String[] args) throws InterruptedException {
        WashingSimulator sim = new WashingSimulator(Settings.SPEEDUP);

        WashingIO io = sim.startSimulation();

        ActorThread<WashingMessage> temp = new TemperatureController(io);
        ActorThread<WashingMessage> water = new WaterController(io);
        ActorThread<WashingMessage> spin = new SpinController(io);

        temp.start();
        water.start();
        spin.start();

        ActorThread<WashingMessage> washingProgram = null;

        while (true) {
            int n = io.awaitButton();
            System.out.println("user selected program " + n);

            if (n == 0) { // STOP button
                washingProgram = stopWashingProgram(washingProgram);
                continue;
            }

            ActorThread<WashingMessage> newProgram = createWashingProgram(n, io, temp, water, spin);
            if (newProgram != null) {
                if (washingProgram == null || !washingProgram.isAlive()) {
                    washingProgram = newProgram;
                    washingProgram.start();
                } else {
                    System.out.println("A washing program is already running.");
                }
            } else {
                System.out.println("No program assigned to this button.");
            }
        }
    }

    private static ActorThread<WashingMessage> createWashingProgram(int n, WashingIO io, ActorThread<WashingMessage> temp, ActorThread<WashingMessage> water, ActorThread<WashingMessage> spin) {
        switch (n) {
            case 3:
                return new WashingProgram3(io, temp, water, spin);
             case 1:
                 return new WashingProgram1(io, temp, water, spin);
             case 2:
                 return new WashingProgram2(io, temp, water, spin);
            default:
                return null;
        }
    }

    private static ActorThread<WashingMessage> stopWashingProgram(ActorThread<WashingMessage> currentProgram) throws InterruptedException {
        if (currentProgram != null && currentProgram.isAlive()) {
            currentProgram.interrupt();
            currentProgram.join();
            System.out.println("Washing program stopped.");
            return null;
        } else {
            System.out.println("No washing program is running.");
            return currentProgram;
        }
    }
};
