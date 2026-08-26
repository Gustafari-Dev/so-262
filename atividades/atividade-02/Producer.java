public class Producer extends Thread {
    private static final int ITEMS = 10;

    private final Buffer buffer;

    public Producer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        for (int i = 1; i <= ITEMS; i++) {
            SleepUtilities.nap();
            System.out.println("Produtor produziu o valor: " + i);
            buffer.set(i);
        }
    }
}
