import java.io.Serializable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class VectorClock implements Serializable, Comparable<VectorClock> {
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
            this.vector = deserialize(data);
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
            HashMap<Short, Integer> o = deserialize(data);
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
        Integer.BYTES // Number of processes
                + (Short.BYTES * this.vector.size()) // Process numbers
                + (Integer.BYTES * this.vector.size()) // Clock values
        );

        buf.putInt(this.vector.size());
        for( short processNum : this.vector.keySet()) {
            buf.putShort(processNum);
            buf.putInt(this.vector.get(processNum));
        }
        return buf.array();
    }

    public static HashMap<Short, Integer> deserialize(byte[] data) throws IOException {
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

    @Override
    public int compareTo(VectorClock other) {
        boolean isLessOrEqualToForAll = true;
        boolean existsOneLessThan = false;

        for (Short processNum : this.vector.keySet()) {
            int thisValue = this.vector.getOrDefault(processNum, 0);
            int otherValue = other.vector.getOrDefault(processNum, 0);

            if (thisValue > otherValue) {
                isLessOrEqualToForAll = false;
            }

            if (thisValue < otherValue) {
                existsOneLessThan = true;
            }
        }

        if (isLessOrEqualToForAll && existsOneLessThan) {
            return -1; // This vector clock is less than the other
        } else if (isLessOrEqualToForAll) {
            return 0; // The vector clocks are equal
        } else {
            return 1; // This vector clock is greater than the other
        }
    }

}
