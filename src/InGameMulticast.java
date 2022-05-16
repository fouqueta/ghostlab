import java.net.*;

public class InGameMulticast implements Runnable {
    static final int MAX_BUFFER = 256;
    private boolean inGame = true;
    private String ipMulti;
    private int portMulti;

    public InGameMulticast(String ip, int port) {
        this.ipMulti = ip;
        this.portMulti = port;
    }

    @Override
    public void run() {
        try {
            MulticastSocket mso = new MulticastSocket(portMulti);
            mso.joinGroup(InetAddress.getByName(ipMulti));
        
            while(inGame) {
                byte[] rep = new byte[MAX_BUFFER];
                DatagramPacket dpacket = new DatagramPacket(rep, rep.length);
                mso.receive(dpacket);
                String action = new String(dpacket.getData(), 0, 6);
                System.out.print(action);
                switch (action) {
                    case "GHOST ": //[GHOST␣x␣y+++]
                        String posX = new String(dpacket.getData(), 6, 3);
                        String posY = new String(dpacket.getData(), 10, 3);
                        System.out.println(posX + (new String(dpacket.getData(), 9, 1)) + posY + (new String(rep, 13, 3)));
                        System.out.println("-> Un fantome s'est cache en position (" + posX + "," + posY + ").");
                        break;
                    case "SCORE ": //[SCORE␣id␣p␣x␣y+++]
                        String id = new String(dpacket.getData(), 6, 8);
                        String score = new String(dpacket.getData(), 15, 4);
                        posX = new String(dpacket.getData(), 20, 3);
                        posY = new String(dpacket.getData(), 24, 3);
                        score = score.replaceFirst("^0+(?!$)", ""); //Pour enlever les 0 de debut
                        System.out.println(id + (new String(dpacket.getData(), 14, 1)) + score + (new String(dpacket.getData(), 19, 1)) 
                            + posX + (new String(rep, 23, 1)) + posY + (new String(rep, 28, 3)));
                        System.out.println("-> " + id + " a attrape le fantome qui etait en position ("+ posX + "," + posY + ") !");
                        System.out.println("-> " + id + " a maintenant " + score + " points.");
                        break;
                    case "MESSA ": //[MESSA␣id␣mess+++]
                        id = new String(dpacket.getData(), 6, 8);
                        String mess = new String(dpacket.getData(), 15, dpacket.getLength()-18);
                        System.out.println(id + (new String(dpacket.getData(), 14, 1)) + mess 
                            + (new String(dpacket.getData(), dpacket.getLength()-3, 3)));
                        System.out.println("-> " + id + " a dit a tout le monde : " + mess);
                        break;
                    case "ENDGA ": //[ENDGA␣id␣p+++]
                        id = new String(dpacket.getData(), 6, 8);
                        score = new String(dpacket.getData(), 15, 4);
                        score = score.replaceFirst("^0+(?!$)", "");
                        System.out.println(id + (new String(dpacket.getData(), 14, 1)) + score + (new String(dpacket.getData(), 19, 1)));
                        System.out.println("-> Il n'y a plus de fantomes !");
                        System.out.println("-> " + id + " a gagne la partie avec un score de " + score + " points.");
                        inGame = false;
                        break;
                    default:
                        System.out.println("Erreur : message incorrect recu");
                        break;
                }

            }
            mso.leaveGroup(InetAddress.getByName(ipMulti));
            mso.close();
        } catch (Exception e) {
            e.printStackTrace();
        }  
    }
    
}
