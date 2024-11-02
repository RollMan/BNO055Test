import processing.core.*;
import processing.serial.*;

import net.coppelab.java.bno055.Bno055;
import net.coppelab.java.bno055.AMGRaw;

public class Bno055Test extends PApplet {
    Bno055 bno055;
    PFont f;

    public void settings() {
        size(200, 200);
    }

    public void setup() {
        if (this.args == null || this.args.length != 1) {
            println("Usage: java Bno055Test <port path>");
            System.exit(1);
        }
        final String PORT_PATH = this.args[0];
        final int BAUD = 115200;
        final char PARITY = 'N';
        final char BITS = 8;
        final char STOP = 1;
        Serial serial = new Serial(this, PORT_PATH, BAUD, PARITY, BITS, STOP);
        bno055 = new Bno055(serial);
        delay(30);
        // bno055.reset_system();
        // delay(30);
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

        f = createFont("Noto Sans CJK JP", 16, true);
    }


    int cnt = 0;
    public void draw() {
        AMGRaw amg = bno055.get_amg_raw();
        background(255);
        textFont(f, 16);
        fill(0);
        if (amg == null) {
            // bno055.reset();
            text("placeholder", 10, 100);
        }else{
            text(amg.gyr.x + ", " + amg.gyr.y + ", " + amg.gyr.z, 10, 100);
        }
        cnt += 1;
        text("cnt: " + cnt, 10, 10);
        delay(100);
    }

    void serialEvent(Serial p){
        bno055.hook.apply(p);
    }

    public static void main(String[] args) {
        PApplet.main("Bno055Test", args);
    }
}
