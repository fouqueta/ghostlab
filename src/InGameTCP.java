import java.io.*;
import java.util.Scanner;

public class InGameTCP implements Runnable {
    static final int MAX_BUFFER = 256;
    static final String MESS_ERROR = "Erreur : veuillez recommencer";
    private Client client;
    private boolean inGame;
    private InputStream is;
    private OutputStream os;
    private Scanner scanner = new Scanner(System.in);

    public InGameTCP(Client c, InputStream i, OutputStream o) {
        this.client = c;
        synchronized(client) { 
            this.inGame = client.isInGame(); 
        }
        this.is = i;
        this.os = o;
    }

    @Override
    public void run() {
        try {
            while (inGame) {
                System.out.println("\nTapez");
                System.out.println("Dn pour vous deplacer de n cases vers la droite (n <= 999)");
                System.out.println("Gn pour vous deplacer de n cases vers la gauche (n <= 999)");
                System.out.println("Hn pour vous deplacer de n cases vers le haut (n <= 999)");
                System.out.println("Bn pour vous deplacer de n cases vers le bas (n <= 999)");
                System.out.println("1 pour savoir la liste des joueurs");
                System.out.println("2 pour envoyer un message a un joueur");
                System.out.println("3 pour envoyer un message a tout le monde");
                System.out.println("4 pour quitter la partie");
                
                String action = scanner.nextLine();
                System.out.println("Vous avez saisi l'action : " + action);
                switch (action) {
                    case "1":
                        inGameActionGLIS(is, os);
                        break;
                    case "2":
                        inGameActionSEND(is, os);
                        break;
                    case "3":
                        inGameActionMALL(is, os);
                        break;
                    case "4":
                        inGameActionIQUIT(is, os);
                        break;
                }
                if ((action.length()>1 && action.length()<=4) &&
                    (action.charAt(0)=='D' || action.charAt(0)=='G' || action.charAt(0)=='H' || action.charAt(0)=='B')) {
                    if (!action.substring(1).chars().allMatch(Character::isDigit)) {
                        System.out.println("Nombre de cases invalide");
                        break;
                    }
                    inGameActionsMOV(action, is, os);
                }
                else {
                    System.out.println("Mauvaise commande");
                }
                synchronized(client) { 
                    this.inGame = client.isInGame(); 
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    // [UPMOV d***], [DOMOV d***], [LEMOV d***] ou [RIMOV d***] -> [MOVE! x y***] ou [MOVEF x y p***]
    public void inGameActionsMOV(String instr, InputStream is, OutputStream os) {
        try {
            //Envoi d'une requete [UPMOV d***], [DOMOV d***], [LEMOV d***] ou [RIMOV d***]
            String dir = "";
            switch (instr.charAt(0)) {
                case 'D':
                    dir = "RIMOV";
                    break;
                case 'G':
                    dir = "LEMOV";
                    break;
                case 'H':
                    dir = "UPMOV";
                    break;
                case 'B':
                    dir = "DOMOV";
                    break;
            }
            String sreq = dir + " " + insert0inDist(instr.substring(1)) + "***";
            byte[] req = sreq.getBytes();
            writeReq(os, req);

            //Reception de la reponse [MOVE! x y***] ou [MOVEF x y p***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 6);
            System.out.print(action);
            switch (action) {
                case "MOVE! ":
                    String posX = new String(rep, 6, 3);
                    String posY = new String(rep, 10, 3);
                    System.out.println(posX + (new String(rep, 9, 1)) + posY + (new String(rep, 13, 3)));
                    System.out.println("Vous etes maintenant en position (" + posX + "," + posY + ").");
                    break;
                case "MOVEF ":
                    posX = new String(rep, 6, 3);
                    posY = new String(rep, 10, 3);
                    String score = new String(rep, 14, 4);
                    System.out.println(posX + (new String(rep, 9, 1)) + posY + (new String(rep, 13, 1)) 
                            + score + (new String(rep, 18, 3)));
                    score = score.replaceFirst("^0+(?!$)", "");
                    System.out.println("Vous avez attrape un fantome !");
                    System.out.println("Vous etes maintenant en position (" + posX + "," + posY + ") avec " + score + " points.");
                    break;
                default: 
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inGameActionGLIS(InputStream is, OutputStream os) { // [GLIS?***] -> [GLIS! s***] et s messages [GPLYR id x y p***] 
        try {
            //Envoi de la requete [GLIS?***]
            byte[] req = "GLIS?***".getBytes();
            writeReq(os, req);

            //Reception de la reponse [GLIS! s***] et s messages [GPLYR id x y p***]
            byte[] rep = new byte[10];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 6);
            System.out.print(action);
            if (!action.equals("GLIS! ")) {
                System.out.println(MESS_ERROR);
                return;
            }
            int nbJoueurs = rep[6] & 0xff;
            System.out.println(nbJoueurs + (new String(rep, 7, 3)));
            //Reception de s reponses [GPLYR id x y p***]
            for (; nbJoueurs != 0; nbJoueurs--) {
                byte[] rep2 = new byte[30];
                bytesRead = is.read(rep2);
                if (bytesRead < 1) {
                    System.out.println(MESS_ERROR);
                    break;
                }
                action = new String(rep2, 0, 6);
                System.out.print(action);
                if (!action.equals("GPLYR ")) {
                    System.out.println(MESS_ERROR);
                    break;
                }
                String id = new String(rep2, 6, 8);
                String posX = new String(rep2, 15, 3);
                String posY = new String(rep2, 19, 3);
                String score = new String(rep2, 23, 4);
                System.out.println(id + (new String(rep2, 14, 1)) + posX + (new String(rep2, 18, 1)) 
                        + posY + (new String(rep2, 22, 1)) + score + (new String(rep2, 27, 3)));
                score = score.replaceFirst("^0+(?!$)", "");
                System.out.println(id + " est en position (" + posX + "," + posY + ")"
                    + " et a " + score + " points.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inGameActionSEND(InputStream is, OutputStream os) { // [SEND? id mess***] -> [SEND!***] ou [NSEND***]
        try {
            //Envoi de la requete [SEND? id mess***]
            //On demande un id
            byte[] bytesId = client.askId();
            //On demande un message
            byte[] bytesMess = askMess();
        
            byte[] req = "SEND? ".getBytes();
            req = Client.concatByteArrays(req, bytesId);
            req = Client.concatByteArrays(req, " ".getBytes());
            req = Client.concatByteArrays(req, bytesMess);
            req = Client.concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [SEND!***] ou [NSEND***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 8);
            System.out.println(action);
            switch (action) {
                case "SEND!***":
                    System.out.println("Votre message a bien ete envoye.");
                    break;
                case "NSEND***":
                    System.out.println("Votre message n'a pas pu etre envoye.");
                    break;
                case "GOBYE***":
                    System.out.println("Impossible d'envoyer le message : la partie est terminee.");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inGameActionMALL(InputStream is, OutputStream os) { // [MALL? mess***] -> [MALL!***]
        try {
            //Envoi de la requete [MALL? mess***]
            //On demande un message
            byte[] bytesMess = askMess();
        
            byte[] req = "MALL? ".getBytes();
            req = Client.concatByteArrays(req, bytesMess);
            req = Client.concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [MALL!***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 8);
            System.out.println(action);
            switch (action) {
                case "MALL!***":
                    System.out.println("Votre message a bien ete envoye.");
                    break;
                case "GOBYE***":
                    System.out.println("Impossible d'envoyer le message : la partie est terminee.");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inGameActionIQUIT(InputStream is, OutputStream os) { // [IQUIT***] -> [GOBYE***]
        try {
            //Envoi de la requete [IQUIT***]
            byte[] req = "IQUIT***".getBytes();
            writeReq(os, req);

            //Reception de la requete [GOBYE***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 8);
            System.out.println(action);
            if (action.equals("GOBYE***")) {
                synchronized(client) { 
                    client.setInGame(false); 
                }
                System.out.println("Vous avez quitte la partie.");
            }
            else {
                System.out.println(MESS_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public byte[] askMess() {
        System.out.println("Entrez un message de 200 caracteres maximum et ne contenant pas de chaines '***' ou '+++'");
        String mess = scanner.nextLine();
        while (mess.length()>200 || mess.contains("***") || mess.contains("+++")) {
            System.out.println("Message invalide, recommencez");
            mess = scanner.next();
        }
        return mess.getBytes();
    }

    public String insert0inDist(String dist) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3-dist.length(); i++){
            sb.append("0");
        }
        sb.append(dist);
        return sb.toString();
    }

    //Fonction auxiliaire pour envoyer et afficher une requete
    public void writeReq(OutputStream os, byte[] req) {
        try {
            os.write(req);
            os.flush();
            System.out.println(new String(req));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
