import processing.serial.*;
import processing.core.*;

import net.coppelab.java.bno055.Bno055;


public class Bno055Test extends PApplet {
    Bno055 bno055;
    public void settings(){
    }

    public void setup(){
        if (this.args == null || this.args.length != 1){
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
    }

    public void draw() {
        println("hi");
    }

    public static void main(String[] args){
        PApplet.main("Bno055Test", args);
    }
}
