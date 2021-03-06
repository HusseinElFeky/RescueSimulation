package controller;

import exceptions.DisasterException;
import exceptions.UnitException;
import model.events.SOSListener;
import model.infrastructure.ResidentialBuilding;
import model.people.Citizen;
import model.units.Unit;
import simulation.Rescuable;
import simulation.Simulator;
import view.GameView;
import view.UnitBlock;
import view.WorldBlock;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;

public class CommandCenter implements SOSListener {

    private Simulator engine;
    private ArrayList<ResidentialBuilding> visibleBuildings = new ArrayList<>();
    private ArrayList<Citizen> visibleCitizens = new ArrayList<>();
    private ArrayList<Unit> emergencyUnits;
    private GameView gameView;

    private UnitBlock selectedUnit;

    public CommandCenter() throws Exception {
        engine = new Simulator(this);
        emergencyUnits = engine.getEmergencyUnits();
        gameView = new GameView();
        engine.setGameView(gameView);

        for (Unit unit : emergencyUnits) {
            UnitBlock unitBlock = new UnitBlock(unit);
            unitBlock.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    int state = e.getStateChange();
                    if (state == ItemEvent.SELECTED) {
                        unitBlock.removeItemListener(this);
                        gameView.deselectAllUnits();
                        unitBlock.setSelected(true);
                        unitBlock.addItemListener(this);
                        selectedUnit = unitBlock;
                    } else {
                        selectedUnit = null;
                    }
                }
            });
            gameView.getAvailableUnits().add(unitBlock);
            gameView.addSimulatableOnWorldMap(unit);
        }
        gameView.getAvailableUnits().validate();

        gameView.getNextCycle().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!engine.checkGameOver()) {
                    try {
                        engine.nextCycle();
                        gameView.recommendMoves(visibleBuildings, visibleCitizens, emergencyUnits);
                    } catch (DisasterException de) {
                        de.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(gameView, "Game Over! Casualties: " + engine.calculateCasualties());
                }
            }
        });

        for (int i = 0; i < gameView.getGridPanel().getComponentCount(); i++) {
            WorldBlock worldBlock = (WorldBlock) gameView.getGridPanel().getComponent(i);
            worldBlock.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedUnit != null) {
                        Rescuable rescuable = worldBlock.getMainRescuable(selectedUnit.getUnit());
                        if (rescuable != null) {
                            try {
                                selectedUnit.getUnit().respond(rescuable);
                                gameView.invalidateUnitsPanel();
                                selectedUnit.setSelected(false);
                                selectedUnit = null;
                            } catch (UnitException ue) {
                                JOptionPane.showMessageDialog(gameView, ue.getMessage());
                            }
                        }
                    }
                }
            });
        }

        initWorldMap();

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sfx/metropolis.wav"));
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
    }

    @Override
    public void receiveSOSCall(Rescuable r) {
        if (r instanceof Citizen) {
            if (!visibleCitizens.contains(r)) visibleCitizens.add((Citizen) r);
        } else if (r instanceof ResidentialBuilding) {
            if (!visibleBuildings.contains(r)) visibleBuildings.add((ResidentialBuilding) r);
        }
    }

    private void initWorldMap() {
        for (ResidentialBuilding building : engine.getBuildings()) {
            gameView.addSimulatableOnWorldMap(building);
        }
        for (Citizen citizen : engine.getCitizens()) {
            gameView.addSimulatableOnWorldMap(citizen);
        }
        for (Unit unit : engine.getEmergencyUnits()) {
            gameView.addSimulatableOnWorldMap(unit);
        }
    }

    public static void main(String[] args) throws Exception {
        new CommandCenter();
    }
}
