package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;

public class SnapshotController implements ActionListener {
    private final TankModel tankModel;

    public SnapshotController(TankModel tankModel) {
        this.tankModel = tankModel;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.initiateSnapshot(new InetSocketAddress("Snapshot", 1));
    }
}
