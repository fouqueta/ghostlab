import java.io.*;

public class InGameTCP implements Runnable {
    private InputStream is;
    private OutputStream os;

    public InGameTCP(InputStream i, OutputStream o) {
        this.is = i;
        this.os = o;
    }

    @Override
    public void run() {
        try {
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
