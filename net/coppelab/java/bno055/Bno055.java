package net.coppelab.java.bno055;

import processing.serial.*;
import java.util.function.Function;

public class Bno055 {
    static private void debug_println(String str) {
        System.out.println(str);
    }

    static public float convert_gyro_radian_reading(float r){
        return r / (float)900.0;
    }

    static private Function<String, Void> DEBUG_PRINTLN = (str) -> {
        // debug_println(str);
        return null;
    };

    static public final byte OPR_MODE_ADDR = 0x3D;
    // TODO: enum
    static public final byte OPR_MODE_CONFIG = 0x00;
    static public final byte OPR_MODE_GYROONLY = 0x03;
    static public final byte OPR_MODE_AMG = 0x07;

    static public final byte ACC_DATA_X_LSB_ADDR = 0x08;
    Serial port;

    public Bno055(Serial port) {
        this.port = port;
    }

    static final long TIMEOUT = 100; // ms
    static private int read_response_bytes(int datalen){
        return datalen + 1;     // Data bytes + len byte.
    }

    private boolean wait_for_response(){
        long start = System.currentTimeMillis();
        while (port.available() == 0) {
            if (System.currentTimeMillis() - start > TIMEOUT) {
                return false;
            }
            try {
                Thread.sleep(1);
            }catch (InterruptedException e){
            }
        }
        return true;
    }

    private void write(byte data[]){
        port.write(data);
        // for(int i = 0; i < data.length; i++){
        //     port.write(data[i]);
        // }
    }

    private int issue_write_command_sync(byte addr, byte len, byte data[]) {
        port.clear();
        Bno055.DEBUG_PRINTLN.apply("Writing " + addr + " " + len);
        final int buf_size = 4 + len;
        byte command[] = new byte[buf_size];
        command[0] = (byte) 0xAA;
        command[1] = (byte) 0x00;
        command[2] = addr;
        command[3] = len;
        for (int i = 0; i < len; i++) {
            command[i + 4] = data[i];
        }
        write(command);
        Bno055.DEBUG_PRINTLN.apply("Waiting for write acknoledgement header for " + command[2] + " " + command[3] + " "
                + command[4] + " len: " + command.length);
        if (!wait_for_response()) {
            Bno055.DEBUG_PRINTLN.apply("timeout");
            return -2;
        }
        int ack_header = port.read();
        if (ack_header != 0xEE) {
            return -1;
        }
        Bno055.DEBUG_PRINTLN.apply("Waiting for write acknoledgement status.");
        if (!wait_for_response()) {
            Bno055.DEBUG_PRINTLN.apply("timeout");
            return -3;
        }
        int res = port.read();
        return res;
    }

    private int[] issue_read_command_sync(byte addr, byte len) {
        port.clear();
        Bno055.DEBUG_PRINTLN.apply("Reading " + addr + " " + len);
        byte command[] = new byte[] {
                (byte) 0xAA,
                (byte) 0x01,
                addr,
                len,
        };
        write(command);
        Bno055.DEBUG_PRINTLN.apply("waiting first response");
        if (!wait_for_response()) {
            Bno055.DEBUG_PRINTLN.apply("timeout");
            return new int[] {};
        }
        int response_byte = port.read();
        if (response_byte != 0xBB) {
            if (response_byte == 0xEE) {
                Bno055.DEBUG_PRINTLN.apply("Error when reading a register: ");
                if(!wait_for_response()){
                    port.clear();
                    Bno055.DEBUG_PRINTLN.apply("Failed to receive read fault status.");
                }else{
                    int status = port.read();
                    Bno055.DEBUG_PRINTLN.apply("" + status);
                }
            } else {
                Bno055.DEBUG_PRINTLN.apply("Invalid response start byte: " + response_byte);
            }
            return new int[] {};
        }
        Bno055.DEBUG_PRINTLN.apply("waiting len response");
        if (!wait_for_response()) {
            Bno055.DEBUG_PRINTLN.apply("timeout");
            return new int[] {};
        }
        final int response_len = port.read();
        if ((byte) response_len != len) {
            Bno055.DEBUG_PRINTLN.apply("warning: response_len != len: " + response_len + " " + len);
        }
        int result[] = new int[response_len];
        for(int i = 0; i < response_len; i++){
            if(!wait_for_response()) {
                Bno055.DEBUG_PRINTLN.apply("timeout");
                port.clear();
                return new int[]{};
            }else{
                result[i] = port.read();
            }
        }
        return result;
    }

    private int select_register_page(byte page) {
        final byte PAGE_ID_ADDR = 0x07;
        int[] current_page = issue_read_command_sync(PAGE_ID_ADDR, (byte) 1);
        if (current_page.length != 1) {
            return -1;
        }
        int res = 0x01;
        if (current_page[0] != page) {
            res = issue_write_command_sync(PAGE_ID_ADDR, (byte) 1, new byte[] { page });
        }
        return res;
    }

    public int set_operation_mode(byte operation_mode) {
        final byte page = 0x00;
        if (select_register_page(page) != 0x01) {
            DEBUG_PRINTLN.apply("Failed to set page during setting operation mode.");
            return -1;
        }
        return issue_write_command_sync(OPR_MODE_ADDR, (byte) 1, new byte[] { operation_mode });
    }

    public int get_operation_mode() {
        final byte page = 0x00;
        if (select_register_page(page) != 0x01) {
            DEBUG_PRINTLN.apply("Failed to set page during setting operation mode.");
            return -1;
        }
        int[] recv = issue_read_command_sync(OPR_MODE_ADDR, (byte) 1);
        if (recv.length == 1) {
            return recv[0];
        } else {
            return -1;
        }
    }

    public int set_power_mode(byte power_mode) {
        final byte PWR_MODE_ADDR = 0x3E;
        int[] current_power_mode_buf = issue_read_command_sync(PWR_MODE_ADDR, (byte) 1);
        if (current_power_mode_buf.length != 1) {
            Bno055.DEBUG_PRINTLN.apply("Failed to read power mode register.");
            return -1;
        }
        int res = 0x01;
        if (current_power_mode_buf[0] != power_mode) {
            res = issue_write_command_sync(PWR_MODE_ADDR, (byte) 1, new byte[] { power_mode });
        }
        return res;
    }

    public int get_gyro_x() {
        final byte page = 0;
        final byte GYR_DATA_X_LSB = 0x14;
        final byte GYR_DATA_X_MSB = 0x15;
        // TODO: read two bytes at once by setting length 2.
        // int[] lower_byte = issue_read_command_sync(GYR_DATA_X_LSB, (byte)1);
        // int[] upper_byte = issue_read_command_sync(GYR_DATA_X_MSB, (byte)1);
        int page_select_res = select_register_page(page);
        if (page_select_res != 0x01) {
            Bno055.DEBUG_PRINTLN.apply("Failed to select page: " + page_select_res);
            return -1;
        }
        int[] recv = issue_read_command_sync(GYR_DATA_X_LSB, (byte) 2);
        if (recv.length != 2) {
            Bno055.DEBUG_PRINTLN.apply("Failed to read GYR_DATA_X_LSB.");
            return -1;
        }
        return recv[0] << 8 | recv[1];
    }

    public int set_unit_sel(byte unit_sel){
        final byte UNIT_SEL = 0x3B;
        int[] current_unit_sel_buf = issue_read_command_sync(UNIT_SEL, (byte)1);
        if (current_unit_sel_buf.length != 1){
            return -1;
        }
        int current_unit_sel = current_unit_sel_buf[0];
        System.out.println(current_unit_sel);
        if(unit_sel != (byte)current_unit_sel){
            return issue_write_command_sync(UNIT_SEL, (byte)1, new byte[]{unit_sel});
        }
        return 0x01;
    }

    public AMGRaw get_amg_raw() {
        final byte page = 0;
        final byte ACC_DATA_X_LSB = 0x08;
        final byte GYR_DATA_Z_MSB = 0x19;
        int page_select_res = select_register_page(page);
        if (page_select_res != 0x01) {
            Bno055.DEBUG_PRINTLN.apply("Failed to select page: " + page_select_res);
            return null;
        }
        byte len = GYR_DATA_Z_MSB - ACC_DATA_X_LSB + 1;
        int[] recv = issue_read_command_sync(ACC_DATA_X_LSB, len);
        if (recv.length != len) {
            Bno055.DEBUG_PRINTLN.apply("Failed to read AMG data.");
            return null;
        }
        AMGRaw amg = new AMGRaw();
        amg.acc.x = concat_word(recv[0], recv[1]);
        amg.acc.y = concat_word(recv[2], recv[3]);
        amg.acc.z = concat_word(recv[4], recv[5]);
        amg.mag.x = concat_word(recv[6], recv[7]);
        amg.mag.y = concat_word(recv[8], recv[9]);
        amg.mag.z = concat_word(recv[10], recv[11]);
        amg.gyr.x = concat_word(recv[12], recv[13]);
        amg.gyr.y = concat_word(recv[14], recv[15]);
        amg.gyr.z = concat_word(recv[16], recv[17]);
        return amg;
    }

    private int concat_word(int lower, int upper) {
        short result = (short)((upper << 8) | lower);
        return (int)result;
    }

    /**
     * Resets the sensor communication state.
     *
     * This function sends a bunch of invalid start byte (0x00).
     * According to the datasheet,
     *
     * The command is rejected and no acknowledgement is sent when an
     * invalid start byte is sent.
     */
    public void reset() {
        final int BUFLEN = 128;
        byte[] buf = new byte[BUFLEN];
        for (int i = 0; i < BUFLEN; i++) {
            buf[i] = 0x00;
        }
        Bno055.DEBUG_PRINTLN.apply("sending reset signal");
        write(buf);
        port.clear();
    }

    public void reset_system() {
        final byte SYS_TRIGGER_ADDR = 0x3F;
        final byte RST_SYS = 1 << 5;
        issue_write_command_sync(SYS_TRIGGER_ADDR, (byte)1, new byte[]{RST_SYS});
    }
}
