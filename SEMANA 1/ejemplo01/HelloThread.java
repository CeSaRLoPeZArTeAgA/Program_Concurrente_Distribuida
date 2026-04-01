package ejemplo01;
public class HelloThread extends Thread{
    public void run() {
        System.out.println("Hello from a thread03!");
    }
    public static void main(String args[]) {
        (new HelloThread()).start();
    }
}
