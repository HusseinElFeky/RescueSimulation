package model.disasters;

import exceptions.DisasterException;
import model.infrastructure.ResidentialBuilding;

public class Fire extends Disaster {

    private ResidentialBuilding residentialBuilding;

    public Fire(int cycle, ResidentialBuilding target) {
        super(cycle, target);
        this.residentialBuilding = target;
    }

    @Override
    public void cycleStep() {
        residentialBuilding.setFireDamage(residentialBuilding.getFireDamage() + 10);
    }

    @Override
    public void strike() throws DisasterException {
        super.strike();
        residentialBuilding.setFireDamage(residentialBuilding.getFireDamage() + 10);
    }
}
