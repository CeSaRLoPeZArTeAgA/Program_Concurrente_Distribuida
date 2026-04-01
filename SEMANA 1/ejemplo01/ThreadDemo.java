package ejemplo01;
public class ThreadDemo implements Runnable{
    ThreadDemo() {
        Thread currThread = Thread.currentThread();
        Thread t = new Thread(this, "Admin_Thread");
        System.out.println("current thread = " + currThread);
        System.out.println("thread created = " + t);
        t.start();
    }
    public void run() {
        System.out.println("This is run() method");
    }
    public static void main(String args[]) {
        new ThreadDemo();
    }
}
