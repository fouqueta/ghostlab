import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    static final int MAX_BUFFER = 256;
    static final String MESS_ERROR = "Erreur : veuillez recommencer";
    private int portTCP;
    private int portUDP;
    private int portMult;
    private String ipMult;
    private boolean start = false;
    private boolean inscrit = false;
    private Scanner scanner = new Scanner(System.in);



    //Convertit 2 bytes du buffer a partir de l'offset en un short et en inversant l'ordre des bytes
    public static short byteArrayToShortSwap(byte[] buffer, int offset) {
        short value = 0;
        for (int i = 0; i < 2; i++) {
            value |= (buffer[i + offset] & 0xff) << (i * 8);
        }            
        return value;
    }

    //Retourne la fusion de 2 tableaux de bytes
    public static byte[] concatByteArrays(byte[] b1, byte[] b2) {
        byte[] res = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, res, 0, b1.length);
        System.arraycopy(b2, 0, res, b1.length, b2.length);
        return res;
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

    public byte[] askId() {
        System.out.println("Entrez un pseudo de 8 caracteres maximum");
        String id = scanner.nextLine();
        while (id.length() > 8) {
            System.out.println("Pseudo invalide, recommencez");
            id = scanner.next();
        }
        byte[] bytesId = new byte[8];
        for (int i = 0; i < id.length(); i++) {
            bytesId[i] = (byte)id.charAt(i);
        }
        return bytesId;
    }

    public byte[] askNumPort() {
        System.out.println("Entrez un numero de port compris entre 1024 et 9999");
        String numPort = scanner.nextLine();
        while (true) {
            if (numPort.chars().allMatch(Character::isDigit)) {
                int port = Integer.parseInt(numPort);
                if (port >= 1024 && port <= 9999) {
                    break;
                }
            }
            System.out.println("numero de port invalide, recommencez");
            numPort = scanner.nextLine();
        }
        this.portUDP = Integer.parseInt(numPort);
        return numPort.getBytes();
    }

    public byte[] askNumGame() {
        System.out.println("Entrez le numero d'une partie");
        String numPartie = scanner.nextLine();
        while (true) {
            if (numPartie.chars().allMatch(Character::isDigit)) {
                int partie = Integer.parseInt(numPartie);
                if (partie >= 0 && partie <= 255) {
                    break;
                }
            }
            System.out.println("numero de partie invalide, recommencez");
            numPartie = scanner.nextLine();
        }
        return new byte[] { (byte)Integer.parseInt(numPartie) };
    }

    public void doCaseREGOKorREGNO(byte[] rep) {
        String action = new String(rep, 0, 5);
        System.out.print(action);
        switch (action) {
            case "REGOK":
                int numPartie = rep[6] & 0xff;
                System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 3)));
                System.out.println("Vous avez bien ete inscrit a la partie " + numPartie);
                this.inscrit = true;
                break;
            case "REGNO":
                System.out.println(new String(rep, 5, 3));
                System.out.println("Erreur : inscription a cette partie impossible");
                break;
            default:
                System.out.println(MESS_ERROR);
                break;
        }
    }

    //gere la reception des reponses GAMES et OGAME et renvoie false s'il y a eu une erreur, true sinon
    public boolean doGAMESandOGAME(InputStream is, byte[] rep) throws IOException {
        String action = new String(rep, 0, 5);
        System.out.print(action);
        if (!action.equals("GAMES")) {
            System.out.println(MESS_ERROR);
            return false;
        }
        int nbParties = rep[6] & 0xff;
        System.out.println((new String(rep, 5, 1)) + nbParties + (new String(rep, 7, 3)));
        if (nbParties == 0) { 
            System.out.println("Aucune partie en attente");
        }
        //Reception des reponses [OGAME␣m␣s***]
        for (; nbParties != 0; nbParties--) {
            byte[] rep2 = new byte[12];
            int bytesRead = is.read(rep2);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return false;
            }
            action = new String(rep2, 0, 5);
            System.out.print(action);
            if (!action.equals("OGAME")) {
                System.out.println(MESS_ERROR);
                return false;
            }
            int numPartie = rep2[6] & 0xff;
            int nbJoueurs = rep2[8] & 0xff;
            System.out.println((new String(rep2, 5, 1)) + numPartie + (new String(rep2, 7, 1))
                + nbJoueurs + (new String(rep2, 9, 3)));
            System.out.print("Partie n°" + numPartie + " : ");
            System.out.println(nbJoueurs + " joueurs inscrits");
            // if(bytesRead == -1) { break; }
        }
        return true;  
    }

    public void preGameActionNEWPL(InputStream is, OutputStream os) { // [NEWPL␣id␣port***] -> [REGOK␣m***] ou [REGNO***]
        try {
            //Envoi de la requete [NEWPL␣id␣port***]
            //On demande un id
            byte[] bytesId = askId();

            //On demande un numero de port
            byte[] bytesPort = askNumPort();

            byte[] req = "NEWPL ".getBytes();
            req = concatByteArrays(req, bytesId);
            req = concatByteArrays(req, " ".getBytes());
            req = concatByteArrays(req, bytesPort);
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [REGOK␣m***] ou [REGNO***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            doCaseREGOKorREGNO(rep);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionREGIS(InputStream is, OutputStream os) { // [REGIS␣id␣port␣m***] -> [REGOK␣m***] ou [REGNO***]
        try {
            //Envoi de la requete [REGIS␣id␣port␣m***]
            //On demande un id
            byte[] bytesId = askId();

            //On demande un numero de port
            byte[] bytesPort = askNumPort();

            //On demande un numero de partie
            byte[] bytePartie = askNumGame();

            byte[] req = "REGIS ".getBytes();
            req = concatByteArrays(req, bytesId);
            req = concatByteArrays(req, " ".getBytes());
            req = concatByteArrays(req, bytesPort);
            req = concatByteArrays(req, " ".getBytes());
            req = concatByteArrays(req, bytePartie);
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [REGOK␣m***] ou [REGNO***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            doCaseREGOKorREGNO(rep);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionUNREG(InputStream is, OutputStream os) { // [UNREG***] -> [UNROK␣m***] ou [DUNNO***]
        try {
            //Envoi de la requete [UNREG***]
            byte[] req = "UNREG".getBytes();
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);
            
            //Reception de la reponse [UNROK␣m***] ou [DUNNO***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            System.out.print(action);
            switch (action) {
                case "UNROK":
                    int numPartie = rep[6] & 0xff;
                    System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 3)));
                    System.out.println("Vous avez bien ete desinscrit de la partie " + numPartie);
                    this.inscrit = false; 
                    break;
                case "DUNNO":
                    System.out.println(new String(rep, 5, 3));
                    System.out.println("Erreur : vous n'etiez pas inscrit dans une partie");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void preGameActionSIZE(InputStream is, OutputStream os) { // [SIZE?␣m***] -> [SIZE!␣m␣h␣w***] ou [DUNNO***]
        try {
            //Envoi de la requete [SIZE?␣m***]
            //On demande un numero de partie
            byte[] bytePartie = askNumGame();

            byte[] req = "SIZE? ".getBytes();
            req = concatByteArrays(req, bytePartie);
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [SIZE!␣m␣h␣w***] ou [DUNNO***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            System.out.print(action);
            switch (action) {
                case "SIZE!":
                    int numPartie = rep[6] & 0xff;
                    short height = byteArrayToShortSwap(rep, 8);
                    short width = byteArrayToShortSwap(rep, 11);
                    System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                        + height + (new String(rep, 10, 1)) + width + (new String(rep, 13, 3)));
                    System.out.println("Le labyrinthe de la partie n°" + numPartie + " a pour hauteur " + height
                        + " et pour largeur " + width);
                    break;
                case "DUNNO":
                    System.out.println(new String(rep, 5, 3));
                    System.out.println("Erreur : ce numero ne correspond a aucune partie");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionLIST(InputStream is, OutputStream os) { // [LIST?␣m***] -> ([LIST!␣m␣s***] et [PLAYR␣id***]) ou [DUNNO***]
        try {
            //Envoi de la requete [LIST?␣m***]
            //On demande un numero de partie
            byte[] bytePartie = askNumGame();

            byte[] req = "LIST? ".getBytes();
            req = concatByteArrays(req, bytePartie);
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse ([LIST!␣m␣s***] et s reponses [PLAYR␣id***]) ou [DUNNO***]
            byte[] rep = new byte[12];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            System.out.print(action);
            switch (action) {
                case "LIST!":
                    int numPartie = rep[6] & 0xff;
                    int nbJoueurs = rep[8] & 0xff;
                    System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                        + nbJoueurs + (new String(rep, 9, 3)));
                    System.out.print("Partie n°" + numPartie + " : ");
                    System.out.println(nbJoueurs + " joueurs inscrits");

                    //Reception de s reponse(s) [PLAYR␣id***]
                    for (; nbJoueurs != 0; nbJoueurs--) {
                        byte[] rep2 = new byte[17];
                        bytesRead = is.read(rep2);
                        if (bytesRead < 1) {
                            System.out.println(MESS_ERROR);
                            break;
                        }
                        action = new String(rep2, 0, 5);
                        System.out.print(action);
                        if (!action.equals("PLAYR")) {
                            System.out.println(MESS_ERROR);
                            break;
                        }
                        String id = new String(rep2, 6, 8);
                        System.out.println((new String(rep2, 5, 1)) + id + (new String(rep2, 14, 3)));
                        System.out.println(id);
                        // if(bytesRead == -1) { break; }
                    }
                    break;
                case "DUNNO":
                    System.out.println(new String(rep, 5, 3));
                    System.out.println("Erreur : ce numero ne correspond a aucune partie");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionGAME(InputStream is, OutputStream os) { //[GAME?***] -> [GAMES␣n***] et [OGAME␣m␣s***]
        try {
            //Envoi de la requete [GAME?***]
            byte[] req = "GAME?".getBytes();
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [GAMES␣n***] et de n reponse(s) [OGAME␣m␣s***]
            byte[] rep = new byte[10];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            doGAMESandOGAME(is, rep);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doSTART(InputStream is, OutputStream os) { //[START***] -> [WELCO␣m␣h␣w␣f␣ip␣port***] et [POSIT␣id␣x␣y***]
        try {
            //Envoi de la requete [START***]
            byte[] req = "START".getBytes();
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception des reponses [WELCO␣m␣h␣w␣f␣ip␣port***] et [POSIT␣id␣x␣y***]
            byte[] rep = new byte[MAX_BUFFER];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            //On s'occupe de [WELCO␣m␣h␣w␣f␣ip␣port***]
            String action = new String(rep, 0, 5);
            System.out.print(action);
            if (!action.equals("WELCO")) {
                System.out.println(MESS_ERROR);
                return;
            }
            int numPartie = rep[6] & 0xff;
            short height = byteArrayToShortSwap(rep, 8);
            short width = byteArrayToShortSwap(rep, 11);
            int nbGhosts = rep[14] & 0xff;
            String ipMultiDiff = new String(rep, 16, 15);
            String portMultiDiff = new String(rep, 32, 4);
            System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                + height + (new String(rep, 10, 1)) + width + (new String(rep, 13, 1))
                + nbGhosts + (new String(rep, 15, 1)) + ipMultiDiff + (new String(rep, 31, 1))
                + portMultiDiff + (new String(rep, 36, 3)));
            System.out.println("Bienvenue !\nLe labyrinthe a pour hauteur " + height
                + ", pour largeur " + width + " et il y a " + nbGhosts + " a capturer !\nBONNE CHANCE\n");
            
            //On s'occupe de [POSIT␣id␣x␣y***]
            action = new String(rep, 39, 5);
            System.out.print(action);
            if (!action.equals("POSIT")) {
                System.out.println(MESS_ERROR);
                return;
            }
            String id = new String(rep, 45, 8);
            String posX = new String(rep, 54, 3);
            String posY = new String(rep, 58, 3);
            System.out.println((new String(rep, 44, 1)) + id + (new String(rep, 53, 1)) + posX
                + (new String(rep, 57, 1)) + posY + (new String(rep, 61, 3)));
            System.out.println(id + ", vous etes en position (" + posX + "," + posY + ")");

            this.start = true;
            this.ipMult = ipMultiDiff.replace("#", "");
            this.portMult = Integer.parseInt(portMultiDiff);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

 
    public void doPreGameActions(InputStream is, OutputStream os) {
        while (!this.start) { //tant que le joueur n'a pas envoyé start, le joueur peut faire certaines actions
            System.out.println("\nTapez");
            System.out.println("1 pour savoir la liste des parties non commencees");
            System.out.println("2 pour savoir la liste des joueurs d'une partie");
            System.out.println("3 pour savoir la taille du labyrinthe d'une partie");
            System.out.println("4 pour vous desinscrire de la partie ou vous etes inscrit actuellement");
            if (!this.inscrit) {
                System.out.println("5 pour creer une nouvelle partie");
                System.out.println("6 pour rejoindre une partie");
            }
            if (this.inscrit && !this.start) {
                System.out.println("START pour commencer la partie");
            }

            String numAction = scanner.nextLine();
            System.out.println("Vous avez saisi le nombre : " + numAction);
            switch (numAction) {
                case "1":
                    preGameActionGAME(is, os);
                    break;
                case "2":
                    preGameActionLIST(is, os);
                    break;
                case "3":
                    preGameActionSIZE(is, os);
                    break;
                case "4":
                    preGameActionUNREG(is, os);
                    break;
                case "5":
                    if (this.inscrit) {
                        System.out.println("Mauvaise commande");
                    }
                    else {
                        preGameActionNEWPL(is, os);
                    }
                    break;
                case "6":
                    if (this.inscrit) {
                        System.out.println("Mauvaise commande");
                    }
                    else { 
                        preGameActionREGIS(is, os);
                    }
                    break;  
                case "START":
                    if (!this.inscrit || this.start) {
                        System.out.println("Mauvaise commande");
                    }
                    else { 
                        doSTART(is, os);
                    }
                    break;
                default:
                    System.out.println("Mauvaise commande");
                    break;
            }
        }
    }


    public void doInGameActions(InputStream is, OutputStream os) {
        Thread threadTCP = new Thread(new InGameTCP(is, os));
        Thread threadUDP = new Thread(new InGameUDP(this.portUDP));
        Thread threadMulticast = new Thread(new InGameMulticast(this.ipMult, this.portMult));

        threadTCP.start();
        threadUDP.start();
        threadMulticast.start();
        try {
            threadMulticast.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }


    public void close(Socket fdSock, InputStream is, OutputStream os, Scanner scanner) throws IOException {
        scanner.close();
        is.close();
        os.close();
        fdSock.close();
        System.exit(0);
    }

    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("Missing port number !");
            System.exit(0);
        }
        
        Client client = new Client();
        client.portTCP = Integer.parseInt(args[0]);

        try{
            Socket fdSock = new Socket("127.0.0.1", client.portTCP);
            InputStream is = fdSock.getInputStream();
            OutputStream os = fdSock.getOutputStream();
            
            //Des qu'on se connecte, on doit recevoir les messages GAMES et OGAME
            byte[] rep = new byte[10];
            int bytesRead = is.read(rep);
            if (bytesRead < 1) {
                client.close(fdSock, is, os, client.scanner);
            }
            if (!client.doGAMESandOGAME(is, rep)) { //S'il y a eu une erreur
                client.close(fdSock, is, os, client.scanner);
            }

            //On gere les actions possibles avant une partie
            client.doPreGameActions(is, os);

            //On gere les actions possibles pendant une partie
            client.doInGameActions(is, os);
            
            client.scanner.close();
            is.close();
            os.close();
            fdSock.close();
        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }
}