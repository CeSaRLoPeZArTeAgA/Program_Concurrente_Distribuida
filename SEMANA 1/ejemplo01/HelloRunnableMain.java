package ejemplo01;
public class HelloRunnableMain {
    public static void main(String args[]) {
        (new Thread(new HelloRunnable02())).start();
    }
}
//dividido en clases
class HelloRunnable02 implements Runnable {
    public void run() {
        System.out.println("Hello from a thread02!");
    }
}
