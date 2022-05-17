import java.net.*;

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
            DatagramSocket dso = new DatagramSocket(client.getPortUDP());
            
            while(inGame) {
                byte[] rep = new byte[MAX_BUFFER];
                DatagramPacket dpacket = new DatagramPacket(rep, rep.length);
                dso.receive(dpacket);
                String action = new String(dpacket.getData(), 0, 6);
                System.out.print(action);
                if (action.equals("MESSP ")) { //[MESSP␣id2␣mess+++]
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
