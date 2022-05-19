import java.net.*;
import java.nio.channels.*;
import java.util.Iterator;
import java.nio.ByteBuffer;

public class InGameUDP implements Runnable {
    static final int MAX_BUFFER = 256;
    private Client client;
    private boolean inGame;

    public InGameUDP(Client c) {
        this.client = c;
        synchronized(client) { 
            this.inGame = client.isInGame(); 
        }
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            DatagramChannel dsc = DatagramChannel.open();
            dsc.configureBlocking(false);
            dsc.bind(new InetSocketAddress(client.getPortUDP()));
            dsc.register(selector, SelectionKey.OP_READ);
            
            while(inGame) {
                selector.select(30000);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while(it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();
                    if(sk.isReadable() && sk.channel() == dsc) {
                        ByteBuffer rep = ByteBuffer.allocate(MAX_BUFFER);
                        dsc.receive(rep);
                        String action = new String(rep.array(), 0, 6);
                        if(client.isVerbeux()) { System.out.print(action); }
                        if (action.equals("MESSP ")) { //[MESSP id2 mess+++]
                            String id = new String(rep.array(), 6, 8);
                            String mess = new String(rep.array(), 15, rep.array().length-18);
                            if(client.isVerbeux()) { System.out.println(id + (new String(rep.array(), 14, 1)) + mess 
                                + (new String(rep.array(), rep.array().length-3, 3))); }
                            System.out.println("=> " + id + " vous a dit : " + mess);
                        } 
                        else {
                            System.out.println("Erreur : message recu incorrect");
                        }
                        rep.clear();
                    }
                    else {
                        System.out.println("Erreur");
                    }
                    synchronized(client) { 
                        this.inGame = client.isInGame(); 
                    }
                }  
            }
            System.out.println("Deconnexion UDP");
            dsc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
