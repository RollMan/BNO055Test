import processing.serial.*;
import static processing.core.*;

import net.coppelab.java.bno055.Bno055;
import net.coppelab.java.bno055.AMGRaw;

public class Bno055Test extends PApplet {
    Bno055 bno055;

    public void settings() {
    }

    public void setup() {
        if (this.args == null || this.args.length != 1) {
            println("Usage: java Bno055Test <port path>");
            System.exit(1);
        }
        final String PORT_PATH = this.args[0];
        final int BAUD = 115200;
        final char PARITY = 0;
        final char BITS = 8;
        final char STOP = 1;
        Serial serial = new Serial(this, PORT_PATH, BAUD, PARITY, BITS, STOP);
        bno055 = new Bno055(serial);
        // bno055.reset();
        int current_operation_mode = bno055.get_operation_mode();
        if (current_operation_mode < 0) {
            println("Failed to get the current operation mode: " + hex(current_operation_mode));
            System.exit(1);
        } else if (current_operation_mode != Bno055.OPR_MODE_GYROONLY) {
            int set_operation_mode_res = bno055.set_operation_mode(Bno055.OPR_MODE_GYROONLY);
            if (set_operation_mode_res != 0x01) {
                println("Failed to set operation mode: " + hex(set_operation_mode_res));
                System.exit(1);
            }
        }
        int set_pwr_mode_res = bno055.set_power_mode((byte) 0x00); // TODO: enum
        if (set_pwr_mode_res != 0x01) {
            println("Failed to set power mode: " + hex(set_pwr_mode_res));
            System.exit(1);
        }
    }

    public void draw() {
        AMGRaw amg = bno055.get_amg_raw();
        if (amg == null) {
            println("read failed");
            bno055.reset();
            return;
        }
        println("" + amg.gyr.x + " " + amg.gyr.y + " " + amg.gyr.z);
        delay(100);
    }

    public static void main(String[] args) {
        PApplet.main("Bno055Test", args);
    }
}
