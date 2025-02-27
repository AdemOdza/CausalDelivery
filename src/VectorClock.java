import java.io.Serializable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class VectorClock implements Serializable {
    final HashMap<Short, Integer> vector;

    public VectorClock() {
        this.vector = new HashMap<>();
    }

    public VectorClock(short[] processNums) {
        this.vector = new HashMap<>();
        for (short processNum : processNums) {
            this.vector.put(processNum, 0);
        }
    }

    public VectorClock(byte[] data) {
        try {
            this.vector = this.deserialize(data);
        } catch (IOException e) {
            throw new RuntimeException("Constructor - Error creating vector clock from data: " + e.getMessage());
        }
    }

    public void initialize(short[] processNums) {
        for (short processNum : processNums) {
            this.vector.put(processNum, 0);
        }
    }

    public void initialize(byte[] data) {
        this.vector.clear();
        try {
            HashMap<Short, Integer> o = this.deserialize(data);
            this.vector.clear();
            this.vector.putAll(o);
        } catch (IOException e) {
            throw new RuntimeException("Error creating vector clock from data: " + e.getMessage());
        }

        if(this.vector.isEmpty()) {
            throw new RuntimeException("Error initializing from bytes: vector clock is empty");
        }
    }

    public void put(Short processNum, Integer value) {
        if(this.vector.isEmpty()) {
            throw new RuntimeException("Cannot put to empty vector clock. Did you initialize the vector clock?");
        }
        this.vector.put(processNum, value);
    }

    public void update(HashMap<Short, Integer> newVector) {
        if(this.vector.isEmpty()) {
            throw new RuntimeException("Cannot update empty vector clock. Did you initialize the vector clock?");
        }
        for( short processNum : newVector.keySet()) {
            this.vector.put(
                    processNum,
                    Math.max(
                            this.vector.getOrDefault(processNum, Integer.MIN_VALUE),
                            newVector.get(processNum)
                    )
            );
        }
    }

    public byte[] serialize() {
        if(this.vector.isEmpty()) {
            throw new RuntimeException("Cannot serialize empty vector clock. Did you initialize the vector clock?");
        }
        ByteBuffer buf = ByteBuffer.allocate(
        Integer.BYTES
                + (Short.BYTES * this.vector.size())
                + (Integer.BYTES * this.vector.size())
        );

        buf.putInt(this.vector.size());
        for( short processNum : this.vector.keySet()) {
            buf.putShort(processNum);
            buf.putInt(this.vector.get(processNum));
        }
        return buf.array();
    }

    public HashMap<Short, Integer> deserialize(byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(data);
        int size = buf.getInt();
        HashMap<Short, Integer> o = new HashMap<>();
        for(int i = 0; i < size; i++) {
            o.put(buf.getShort(), buf.getInt());
        }
        if(buf.hasRemaining()) {
            throw new IOException("Invalid vector clock: extra bytes found in buffer");
        }

        return o;
    }

}
