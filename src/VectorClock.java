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

    public VectorClock(String data) {
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

    public void initialize(String data) {
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

    public String serialize() {
        String data = this.vector.size() + "";
        for(Short processNum : this.vector.keySet()) {
            data += "|" + processNum + "|" + this.vector.get(processNum);
        }
        return data;
    }

    public static HashMap<Short, Integer> deserialize(String data) throws IOException {
        String[] tokens = data.trim().split("\\|");
        if(tokens.length < 3) {
            throw new IOException("Invalid vector clock data");
        }

        int size = Integer.parseInt(tokens[0]);
        HashMap<Short, Integer> o = new HashMap<>();

        for(int i = 1; i < (2 * size); i+=2) {
            o.put(Short.parseShort(tokens[i]), Integer.parseInt(tokens[i+1]));
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
