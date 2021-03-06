package exceptions;

import model.disasters.Disaster;

public class BuildingAlreadyCollapsedException extends DisasterException {

    public BuildingAlreadyCollapsedException(Disaster disaster) {
        this(disaster, "The ResidentialBuilding is already collapsed.");
    }

    public BuildingAlreadyCollapsedException(Disaster disaster, String message) {
        super(disaster, message);
    }
}
