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
            /*
            Selector selector = Selector.open();
            DatagramChannel dsc = DatagramChannel.open();
            dsc.configureBlocking(false);
            dsc.bind(new InetSocketAddress(client.getPortUDP()));
            dsc.register(selector, SelectionKey.OP_READ);
            
            while(inGame) {
                selector.select(5000);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while(it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();
                    if(sk.isReadable() && sk.channel() == dsc) {
                        ByteBuffer rep = ByteBuffer.allocate(MAX_BUFFER);
                        dsc.receive(rep);
                        String action = new String(rep.array(), 0, 6);
                        System.out.print(action);
                        if (action.equals("MESSP ")) { //[MESSP id2 mess+++]
                            String id = new String(rep.array(), 6, 8);
                            String mess = new String(rep.array(), 15, rep.array().length-18);
                            System.out.println(id + (new String(rep.array(), 14, 1)) + mess 
                                + (new String(rep.array(), rep.array().length-3, 3)));
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
            dsc.close();*/
            DatagramSocket dso = new DatagramSocket(client.getPortUDP());

            while(inGame) {
                byte[] rep = new byte[MAX_BUFFER];
                DatagramPacket dpacket = new DatagramPacket(rep, rep.length);
                dso.receive(dpacket);
                String action = new String(dpacket.getData(), 0, 6);
                System.out.print(action);
                if (action.equals("MESSP ")) { //[MESSP id2 mess+++]
                    String id = new String(dpacket.getData(), 6, 8);
                    String mess = new String(dpacket.getData(), 15, dpacket.getLength()-18);
                    System.out.println(id + (new String(dpacket.getData(), 14, 1)) + mess 
                        + (new String(dpacket.getData(), dpacket.getLength()-3, 3)));
                    System.out.println("=> " + id + " vous a dit : " + mess);
                }
                else {
                    System.out.println("Erreur : message recu incorrect");
                }
                synchronized(client) { 
                    this.inGame = client.isInGame(); 
                }
            }
            // dso.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
