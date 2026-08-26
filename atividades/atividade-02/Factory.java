public class Factory {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" Atividade 02 - Programa Produtor-Consumidor");
        System.out.println(" Aluno: Gustavo Araujo Silva");
        System.out.println("=================================================");
        System.out.println();

        Buffer buffer = new BoundedBuffer();

        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);

        producer.start();
        consumer.start();
    }
}
