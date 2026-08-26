public class BoundedBuffer implements Buffer {
    private static final int BUFFER_SIZE = 5;

    private final int[] buffer;
    private int count;
    private int in;
    private int out;

    public BoundedBuffer() {
        buffer = new int[BUFFER_SIZE];
        count = 0;
        in = 0;
        out = 0;
    }

    public synchronized void set(int value) {
        while (count == BUFFER_SIZE) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        buffer[in] = value;
        in = (in + 1) % BUFFER_SIZE;
        count++;

        notifyAll();
    }

    public synchronized int get() {
        while (count == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        int value = buffer[out];
        out = (out + 1) % BUFFER_SIZE;
        count--;

        notifyAll();

        return value;
    }
}
