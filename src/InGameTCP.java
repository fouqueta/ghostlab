import java.io.InputStream;
import java.io.OutputStream;

import javax.print.attribute.standard.OutputDeviceAssigned;

public class InGameTCP implements Runnable {
    private InputStream is;
    private OutputStream os;

    public InGameTCP(InputStream i, OutputStream o) {
        this.is = i;
        this.os = o;
    }

    @Override
    public void run() {
        
    }
    
}
