package model.units;

import model.events.WorldListener;
import simulation.Address;

public class GasControlUnit extends FireUnit {

    public GasControlUnit(String id, Address location, int stepsPerCycle, WorldListener worldListener) {
        super(id, location, stepsPerCycle, worldListener);
    }
}
