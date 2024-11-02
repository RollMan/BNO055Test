package net.coppelab.java.bno055;

import processing.serial.*;

public class Handler {
    public static SerialState handle_write_response(Serial p, int recv){
        if (recv == 0xEE){
            return SerialState.WAITING_WRITE_STATUS;
        }else{
            p.clear();
            return SerialState.IDLE;
        }
    }

    public static SerialState handle_read_response(Serial p, int recv){
        if (recv == 0xBB){
            return SerialState.WAITING_READ_LENGTH;
        }else if(recv == 0xEE){
            return SerialState.WAITING_READ_STATUS;
        }else{
            p.clear();
            return SerialState.IDLE;
        }
    }
}
