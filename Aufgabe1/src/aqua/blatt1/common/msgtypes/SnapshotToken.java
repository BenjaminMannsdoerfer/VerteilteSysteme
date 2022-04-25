package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class SnapshotToken implements Serializable {
    private int snapshot = 0;

    public int getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(int snapshot) {
        this.snapshot += snapshot;
    }

    // TODO Zum Einsammeln der lokalen Schnappschüsse soll ein zusätzliches Token verwendet
    //  werden (definieren Sie dazu eine geeignete neue Nachrichtenklasse), das vom Initiator
    //  des globalen Schnappschusses erzeugt und einmal durch den Ring geschickt
    //  wird. Dazu sendet der Initiator, sobald er seinen lokalen Schnappschuss erstellt
    //  hat, das Token an seinen linken Nachbarn. Jeder Klient, der das Token bekommt,
    //  hält es so lange, bis der lokale Schnappschuss vorliegt, addiert seinen Schnappschuss
    //  auf das Zwischenergebnis des Tokens und gibt dieses dann weiter im Ring
    //  bis es wieder beim Initiator angelangt.
}
