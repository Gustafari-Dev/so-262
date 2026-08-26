public class Consumer extends Thread {
    private static final int ITEMS = 10;

    private final Buffer buffer;

    public Consumer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        for (int i = 1; i <= ITEMS; i++) {
            SleepUtilities.nap();
            int value = buffer.get();
            System.out.println("Consumidor consumiu o valor: " + value);
        }
    }
}
