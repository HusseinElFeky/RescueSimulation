package simulation;

import model.disasters.*;
import model.events.SOSListener;
import model.events.WorldListener;
import model.infrastructure.ResidentialBuilding;
import model.people.Citizen;
import model.people.CitizenState;
import model.units.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Simulator implements WorldListener {

    private ArrayList<ResidentialBuilding> buildings = new ArrayList<>();
    private ArrayList<Citizen> citizens = new ArrayList<>();

    private ArrayList<Unit> emergencyUnits = new ArrayList<>();
    private ArrayList<Disaster> plannedDisasters = new ArrayList<>();
    private ArrayList<Disaster> executedDisasters = new ArrayList<>();

    private Address[][] world = new Address[10][10];
    private int currentCycle;
    private SOSListener emergencyService;

    public Simulator(SOSListener sosListener) throws Exception {
        for (int i = 0; i < world.length; i++) {
            for (int j = 0; j < world[0].length; j++) {
                world[i][j] = new Address(i, j);
            }
        }
        loadBuildings("buildings.csv");
        loadCitizens("citizens.csv");
        loadUnits("units.csv");
        loadDisasters("disasters.csv");
        setSOSListener(sosListener);
    }

    private void loadBuildings(String filePath) throws Exception {
        Scanner input = new Scanner(new File(filePath));
        while (input.hasNext()) {
            String[] cells = input.nextLine().split(",");
            buildings.add(new ResidentialBuilding(world[Integer.parseInt(cells[0])][Integer.parseInt(cells[1])]));
        }
    }

    private void loadCitizens(String filePath) throws Exception {
        Scanner input = new Scanner(new File(filePath));
        while (input.hasNext()) {
            String[] cells = input.nextLine().split(",");
            Citizen citizen = new Citizen(world[Integer.parseInt(cells[0])][Integer.parseInt(cells[1])],
                    cells[2], cells[3], Integer.parseInt(cells[4]), this);
            citizens.add(citizen);
            for (ResidentialBuilding building : buildings) {
                if (building.getLocation().getX() == citizen.getLocation().getX() &&
                        building.getLocation().getY() == citizen.getLocation().getY()) {
                    building.getOccupants().add(citizen);
                }
            }
        }
    }

    private void loadUnits(String filePath) throws Exception {
        Scanner input = new Scanner(new File(filePath));
        while (input.hasNext()) {
            String[] cells = input.nextLine().split(",");
            switch (cells[0]) {
                case "AMB":
                    emergencyUnits.add(new Ambulance(cells[1], world[0][0], Integer.parseInt(cells[2]), this));
                    break;
                case "DCU":
                    emergencyUnits.add(new DiseaseControlUnit(cells[1], world[0][0], Integer.parseInt(cells[2]), this));
                    break;
                case "EVC":
                    emergencyUnits.add(new Evacuator(cells[1], world[0][0], Integer.parseInt(cells[2]), Integer.parseInt(cells[3]), this));
                    break;
                case "FTK":
                    emergencyUnits.add(new FireTruck(cells[1], world[0][0], Integer.parseInt(cells[2]), this));
                    break;
                case "GCU":
                    emergencyUnits.add(new GasControlUnit(cells[1], world[0][0], Integer.parseInt(cells[2]), this));
                    break;
            }
        }
    }

    private void loadDisasters(String filePath) throws Exception {
        Scanner input = new Scanner(new File(filePath));
        while (input.hasNext()) {
            String[] cells = input.nextLine().split(",");
            switch (cells[1]) {
                case "FIR":
                    plannedDisasters.add(new Fire(Integer.parseInt(cells[0]),
                            findBuilding(world[Integer.parseInt(cells[2])][Integer.parseInt(cells[3])])));
                    break;
                case "GLK":
                    plannedDisasters.add(new GasLeak(Integer.parseInt(cells[0]),
                            findBuilding(world[Integer.parseInt(cells[2])][Integer.parseInt(cells[3])])));
                    break;
                case "INF":
                    plannedDisasters.add(new Infection(Integer.parseInt(cells[0]), findCitizen(cells[2])));
                    break;
                case "INJ":
                    plannedDisasters.add(new Injury(Integer.parseInt(cells[0]), findCitizen(cells[2])));
                    break;
            }
        }
    }

    private ResidentialBuilding findBuilding(Address address) {
        for (ResidentialBuilding building : buildings) {
            Address location = building.getLocation();
            if (location.getX() == address.getX() && location.getY() == address.getY()) return building;
        }
        throw new IllegalArgumentException("No building found with the given Address.");
    }

    private Citizen findCitizen(String nationalID) {
        for (Citizen citizen : citizens) {
            String id = citizen.getNationalID();
            if (id.equals(nationalID)) return citizen;
        }
        throw new IllegalArgumentException("No citizen found with the given National ID.");
    }

    @Override
    public void assignAddress(Simulatable sim, int x, int y) {
        if (sim instanceof Citizen) {
            Citizen citizen = (Citizen) sim;
            citizen.setLocation(world[x][y]);
        } else if (sim instanceof Unit) {
            Unit unit = (Unit) sim;
            unit.setLocation(world[x][y]);
        }
    }

    public ArrayList<Unit> getEmergencyUnits() {
        return emergencyUnits;
    }

    public void setSOSListener(SOSListener sosListener) {
        this.emergencyService = sosListener;
    }

    private boolean checkGameOver() {
        if (plannedDisasters.size() > 0) return false;
        for (Citizen citizen : citizens) {
            Disaster disaster = citizen.getDisaster();
            if (disaster != null && disaster.isActive()) return false;
        }
        for (ResidentialBuilding building : buildings) {
            Disaster disaster = building.getDisaster();
            if (disaster != null && disaster.isActive()) return false;
        }
        for (Unit unit : emergencyUnits) {
            if (unit.getState() != UnitState.IDLE) return false;
        }
        return true;
    }

    private int calculateCasualties() {
        int casualties = 0;
        for (Citizen citizen : citizens) {
            if (citizen.getState() == CitizenState.DECEASED) casualties++;
        }
        return casualties;
    }

    public void nextCycle() {
        currentCycle++;
        for (Disaster disaster : plannedDisasters) {
            if (disaster.getStartCycle() == currentCycle) {
                plannedDisasters.remove(disaster);
                if (disaster.getTarget() instanceof ResidentialBuilding) {
                    ResidentialBuilding building = (ResidentialBuilding) disaster.getTarget();
                    if (building.getFireDamage() != 100) {
                        Disaster currentDisaster = building.getDisaster();
                        if (currentDisaster instanceof GasLeak && disaster instanceof Fire) {
                            int gasLevel = building.getGasLevel();
                            if (gasLevel == 0) {
                                disaster.strike();
                            } else if (0 < gasLevel && gasLevel < 70) {
                                Collapse collapse = new Collapse(currentCycle, building);
                                collapse.strike();
                                executedDisasters.add(collapse);
                            } else if (70 <= gasLevel) {
                                building.setStructuralIntegrity(0);
                            }
                        } else if (currentDisaster instanceof Fire && disaster instanceof GasLeak) {
                            Collapse collapse = new Collapse(currentCycle, building);
                            collapse.strike();
                            executedDisasters.add(collapse);
                        } else {
                            disaster.strike();
                            executedDisasters.add(disaster);
                        }
                    }
                } else {
                    disaster.strike();
                    executedDisasters.add(disaster);
                }
            }
        }
        for (ResidentialBuilding building : buildings) {
            if (building.getFireDamage() == 100) {
                Collapse collapse = new Collapse(currentCycle, building);
                collapse.strike();
                executedDisasters.add(collapse);
            }
        }
        for (Unit unit : emergencyUnits) {
            unit.cycleStep();
        }
        for (Disaster disaster : executedDisasters) {
            if (disaster.isActive() && disaster.getStartCycle() < currentCycle) disaster.cycleStep();
        }
        for (ResidentialBuilding building : buildings) {
            building.cycleStep();
        }
        for (Citizen citizen : citizens) {
            citizen.cycleStep();
        }
    }
}
