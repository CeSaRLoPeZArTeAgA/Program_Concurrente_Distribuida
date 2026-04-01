package ejemplo01;
public class HelloRunnable implements Runnable{
    public void run() {
        System.out.println("Hello from a thread01!");
    }
    public static void main(String args[]) {
        (new Thread(new HelloRunnable())).start();
    }
}
