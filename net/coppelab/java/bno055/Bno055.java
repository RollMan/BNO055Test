package net.coppelab.java.bno055;

import processing.serial.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.coppelab.java.bno055.SerialState;

import java.util.ArrayList;

public class Bno055 {
    static private void debug_println(String str) {
        System.out.println(str);
    }

    static private Function<String, Void> DEBUG_PRINTLN = (str) -> {
        // debug_println(str);
        return null;
    };

    static public final byte OPR_MODE_ADDR = 0x3D;
    static public final byte OPR_MODE_GYROONLY = 0x03;
    static public final byte OPR_MODE_AMG = 0x07;
    static public final byte ACC_DATA_X_LSB_ADDR = 0x08;
    Serial port;
    private AMGRaw amgraw;
    private SerialState state;
    private int len;
    private int addr;
    private ArrayList<Integer> recv_buf;
    private int recv_buf_idx;
    public Function<Serial, Void> hook;

    public Bno055(Serial port) {
        this.port = port;
        this.state = SerialState.IDLE;
        this.amgraw = new AMGRaw();
        this.hook = (p) -> {return null;};
    }

    private void register_hook(Function<Integer, Void> write_callback, BiFunction<Integer, ArrayList<Integer>, Void> read_callback){
        this.hook = (p) -> {
            hook_(
                p,
                write_callback,
                read_callback
            );
            return null;
        };
    }

    private void hook_(Serial port, Function<Integer, Void> write_callback, BiFunction<Integer, ArrayList<Integer>, Void> read_callback) {
        while (port.available() > 0) {
            int recv = port.read();
            switch (this.state) {
                case IDLE:
                    // unexpected error.
                    break;
                case WAITING_WRITE_RESPONSE_HEADER:
                    this.state = Handler.handle_write_response(port, recv);
                    break;
                case WAITING_WRITE_STATUS:
                    this.state = SerialState.IDLE;
                    write_callback.apply(recv);
                    break;
                case WAITING_READ_RESPONSE_HEADER:
                    this.state = Handler.handle_read_response(port, recv);
                    break;
                case WAITING_READ_STATUS:
                    this.state = SerialState.IDLE;
                    read_callback.apply(recv, new ArrayList<Integer>(0));
                    break;
                case WAITING_READ_LENGTH:
                    this.state = SerialState.WAITING_READ_DATA;
                    len = recv;
                    recv_buf_idx = 0;
                    recv_buf = new ArrayList<Integer>(len);
                    break;
                case WAITING_READ_DATA:
                    recv_buf.set(recv_buf_idx, recv);
                    recv_buf_idx += 1;
                    if (recv_buf_idx == len){
                        this.state = SerialState.IDLE;
                        read_callback.apply(0, recv_buf);
                    }
                    break;
            }
        }
    }

    static final long TIMEOUT = 100; // ms

    static private int read_response_bytes(int datalen) {
        return datalen + 1; // Data bytes + len byte.
    }

    private boolean wait_for_response() {
        long start = System.currentTimeMillis();
        while (port.available() == 0) {
            if (System.currentTimeMillis() - start > TIMEOUT) {
                return false;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        return true;
    }

    private void write(byte data[]) {
        port.write(data);
        // for(int i = 0; i < data.length; i++){
        // port.write(data[i]);
        // }
    }

    private void issue_write_command_sync(byte addr, byte len, byte data[]) {
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
    }

    private void issue_read_command_sync(byte addr, byte len) {
        port.clear();
        Bno055.DEBUG_PRINTLN.apply("Reading " + addr + " " + len);
        byte command[] = new byte[] {
                (byte) 0xAA,
                (byte) 0x01,
                addr,
                len,
        };
        write(command);
    }

    private void select_register_page(byte page, Function<Integer, Void> write_callback, BiFunction<Integer, ArrayList<Integer>, Void> read_callback_) {
        final byte PAGE_ID_ADDR = 0x07;
        BiFunction<Integer, ArrayList<Integer>, Void> read_callback = (err, dat) -> {
            if (err != 0){
                read_callback_.apply(err, dat);
            }else{
                if(dat != null && dat.size() == 1){
                    Integer read_page = dat.get(0);
                    if (read_page != page){
                        issue_write_command_sync(PAGE_ID_ADDR, (byte)1, new byte[] {page});
                    }
                }else{
                    read_callback_.apply(-1, dat);
                }
            }
            return null;
        };
        register_hook(write_callback, read_callback);
        issue_read_command_sync(PAGE_ID_ADDR, (byte) 1);
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
        short result = (short) ((upper << 8) | lower);
        return (int) result;
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
        issue_write_command_sync(SYS_TRIGGER_ADDR, (byte) 1, new byte[] { RST_SYS });
    }
}
