package aqua.blatt1.client;

import aqua.blatt1.common.FishModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private final TankModel tankModel;
    private final String fish;
    private final String fishTankModel;

    public ToggleController(TankModel tankModel, String fish, String fishTankModel) {
        this.tankModel = tankModel;
        this.fish = fish;
        this.fishTankModel = fishTankModel;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.locateFishGlobally(fish, fishTankModel);
    }
}
