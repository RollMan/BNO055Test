package net.coppelab.java.bno055;

import processing.serial.*;
import java.util.function.Function;

public class Bno055 {
    Serial port;
    public Bno055(Serial port) {
        this.port = port;
    }

    public void issue_write_command_sync(Serial port, byte addr, byte len, byte data[]) {
      final int buf_size = 4 + len;
      byte command[] = new byte[buf_size];
      command[0] = (byte)0xAA;
      command[1] = 0x00;
      command[2] = addr;
      command[3] = len;
      for (int i = 0; i < len; i++) {
        command[i+4] = data[i];
      }
      port.write(command);
    }

    public void issue_read_command_sync(Serial port, byte addr, byte len) {
      byte command[] = new byte[]{
        (byte)0xAA,
        0x01,
        addr,
        len,
      };
      port.write(command);
      while(port.available() == 0);
    }
}
