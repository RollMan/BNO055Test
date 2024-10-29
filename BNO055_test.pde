import processing.serial.*;
import java.util.function.Function;

final String COMPORT = "COM5";

Serial port;

enum CommandState {
  IDLE,
    ACK,
    ACKEND,
    LEN,
    DATA,
    READY;
}

final byte WRITE = 0x00;
final byte READ = 0x01;
final byte OPR_MODE_ADDR = 0x3D;
final byte OPR_MODE_AMG[] = {0x07};
final byte ACC_DATA_X_LSB_ADDR = 0x08;

CommandState state = CommandState.IDLE;


class SerialRecvEventHandler {
  private CommandState state;
  private int dataidx;
  private int datalen;
  private byte buf[];
  public SerialRecvEventHandler() {
    state = CommandState.IDLE;
    datalen = 0;
    dataidx = 0;
    buf = new byte[134];
  }
  public void invoke(Serial p) {
    int recv = p.read();
    if (recv == -1) {
      println("Read failed.");
      return;
    }
    byte inByte = (byte)recv;
    println("recv something at " + state + ": " + hex(inByte));

    switch(state) {
    case IDLE:
      datalen = 0;
      dataidx = 0;
      if (recv == 0xEE) {
        state = CommandState.ACK;
      } else if (recv == 0xBB) {
        state = CommandState.LEN;
      } else {
        print("Unexpected byte " + recv + "received. at " + state + "\n");
        state = CommandState.IDLE;
      }
      break;
    case ACK:
      println("Ack received: " + inByte);
      state = CommandState.ACKEND;
      break;
    case LEN:
      datalen = inByte;
      state = CommandState.DATA;
      break;
    case DATA:
      buf[dataidx++] = inByte;
      if (dataidx >= datalen) {
        state = CommandState.READY;
      }
      break;
    case READY:
      break;
    }
  }
  public void reset(){
    state = CommandState.IDLE;
  }
  public CommandState get_state(){
    return state;
  }
  public boolean is_bufready() {
    return state == CommandState.READY;
  }
  public boolean is_idle() {
    return state == CommandState.IDLE;
  }
  public int len() {
    return datalen;
  }
  public byte[] read() {
    state = CommandState.IDLE;
    dataidx = 0;
    datalen = 0;
    return buf;
  }
}

SerialRecvEventHandler serial_handler;

public <T> boolean timeout_fn(Function <T, CommandState> fn, int millisecond, T args) {
  int start = millis();
  while (true) {
    CommandState ret = fn.apply(args);
    if (ret == CommandState.READY) {
      return true;
    }
    if (millis() - start > millisecond || ret == CommandState.ACKEND) {
      println(ret);

      return false;
    }
  }
}

void setup() {
  port = new Serial(this, COMPORT, 115200);
  serial_handler = new SerialRecvEventHandler();
  set_config_mode_amg();
}

void draw() {
  if (serial_handler.is_idle()) {
    get_acc();
  }
}

void issue_write_command(byte addr, byte len, byte data[]) {
  final int buf_size = 4 + len;
  byte command[] = new byte[buf_size];
  command[0] = (byte)0xAA;
  command[1] = 0x00;
  command[2] = addr;
  command[3] = len;
  for (int i = 0; i < len; i++) {
    command[i+4] = data[i];
  }
  port.write(command);  // May occur buffer overrun?
  /*
  for (byte b : command) {
   port.write(b);
   }
   */
     while(!(serial_handler.get_state() == CommandState.ACKEND));
  serial_handler.reset();
}

void issue_read_command(byte addr, byte len) {
  byte command[] = new byte[]{
    (byte)0xAA,
    0x01,
    addr,
    len,
  };
  port.write(command);
}

void set_config_mode_amg() {
  issue_write_command(OPR_MODE_ADDR, (byte)1, OPR_MODE_AMG);
}

void get_acc() {
  issue_read_command(ACC_DATA_X_LSB_ADDR, (byte)2);
  // wait for reading data
  println("waiting for ready");
  boolean read_success = timeout_fn(new Function<Void, CommandState>() {
    public CommandState apply(Void x) {
      return serial_handler.get_state();
    }
  }
  , 1000, null);
  if (!read_success) {
    serial_handler.reset();
    println("Failed to read.");
    // serial_handler.read();  // reset counters and states.
    return;
  }
  println("buf ready");
  int datalen = serial_handler.len();
  byte accx_bytewise[] = serial_handler.read();
  int accx = 0;
  for (int i = 0; i < datalen; i++) {
    accx |= (accx_bytewise[i] << (8 * i));
  }
  println("acc = " + accx);
  /*
  issue_command(READ, ACC_DATA_Y_ADDR, (byte)2, new byte[]{});
   // TODO: wait for reading data
   issue_command(READ, ACC_DATA_Z_ADDR, (byte)2, new byte[]{});
   */
}

void serialEvent(Serial p) {
  serial_handler.invoke(p);
}
