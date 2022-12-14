import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    static final int MAX_BUFFER = 256;
    static final String MESS_ERROR = "Erreur : veuillez recommencer";
    private boolean verbeux = false;
    private byte[] rep = new byte[MAX_BUFFER];
    private int bufferSize = 0;
    private int lenMessage = 0;
    private int portTCP;
    private int portUDP;
    private int portMult;
    private String ipMult;
    private boolean inGame = false;
    private boolean start = false;
    private boolean inscrit = false;
    private Scanner scanner = new Scanner(System.in);

    static int lenX;
    static int lenY;
    static int posX;
    static int posY;

    public static void main(String[] args){
        if (args.length < 1 || args.length > 2) {
            System.out.println("Wrong number of arguments !");
            System.out.println("Usage : java Client nb_port (ip)");
            System.exit(0);
        }   

        String ip = "lulu.informatique.univ-paris-diderot.fr";
        if (args.length == 2) {
            ip = args[1];
        } 
        
        Client client = new Client();
        client.portTCP = Integer.parseInt(args[0]);

        try{
            Socket fdSock = new Socket(ip, client.portTCP);
            InputStream is = fdSock.getInputStream();
            OutputStream os = fdSock.getOutputStream();
            
            System.out.println("Taper v pour lancer le jeu en mode verbeux, n'importe quelle touche sinon :");
            if (client.scanner.nextLine().equals("v")) {
                client.verbeux = true;
            }

            //Des qu'on se connecte, on doit recevoir les messages GAMES et OGAME
            int bytesRead = client.readRep(is);
            if (bytesRead < 1) {
                client.close(fdSock, is, os, client.scanner);
            }
            if (!client.doGAMESandOGAME(is)) { //S'il y a eu une erreur
                client.close(fdSock, is, os, client.scanner);
            }
            if (!client.nextRep()) { 
                client.close(fdSock, is, os, client.scanner);
            }

            //On gere les actions possibles avant une partie
            client.doPreGameActions(is, os);

            //On gere les actions possibles pendant une partie
            client.doInGameActions(client, is, os);

            client.scanner.close();
            is.close();
            os.close();
            fdSock.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void doPreGameActions(InputStream is, OutputStream os) {
        while (!this.start) { //tant que le joueur n'a pas envoye start, le joueur peut faire certaines actions
            System.out.println("\nTapez");
            System.out.println("1 pour savoir la liste des parties non commencees");
            System.out.println("2 pour savoir la liste des joueurs d'une partie");
            System.out.println("3 pour savoir la taille du labyrinthe d'une partie");
            System.out.println("4 pour vous desinscrire de la partie ou vous etes inscrit actuellement");
            if (!this.inscrit) {
                System.out.println("5 pour creer une nouvelle partie");
                System.out.println("6 pour rejoindre une partie");
            }
            System.out.println("NG pour savoir le nombre de fantomes d'une partie");
            if (this.inscrit && !this.start) {
                System.out.println("CM pour changer la taille de votre labyrinthe");
                System.out.println("CG pour changer le nombre de fantomes");
                System.out.println("START pour commencer la partie");
            }

            String numAction = scanner.nextLine();
            System.out.println("Vous avez saisi l'action : " + numAction);
            boolean err = false;
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
                        err = true;
                        System.out.println("Mauvaise commande");
                    }
                    else {
                        preGameActionNEWPL(is, os);
                    }
                    break;
                case "6":
                    if (this.inscrit) {
                        err = true;
                        System.out.println("Mauvaise commande");
                    }
                    else { 
                        preGameActionREGIS(is, os);
                    }
                    break;  
                case "START":
                    if (!this.inscrit || this.start) {
                        err = true;
                        System.out.println("Mauvaise commande");
                    }
                    else { 
                        doSTART(is, os);
                    }
                    break;
                case "CM":
                    if (!this.inscrit || this.start) {
                        err = true;
                        System.out.println("Mauvaise commande");
                    }
                    else { 
                        preGameActionSIZEM(is, os);
                    }
                    break;
                case "CG":
                    if (!this.inscrit || this.start) {
                        err = true;
                        System.out.println("Mauvaise commande");
                    }
                    else { 
                        preGameActionNUMGH(is, os);
                    }
                    break;
                case "NG":
                    preGameActionNBHG(is, os);
                    break;
                default:
                    err = true;
                    System.out.println("Mauvaise commande");
                    break;
            }
            if (!err && !nextRep()) {
                return;
            }
        }
    }

    public void doInGameActions(Client client, InputStream is, OutputStream os) {
        try {
            Thread threadTCP = new Thread(new InGameTCP(client, is, os));
            Thread threadUDP = new Thread(new InGameUDP(client));
            Thread threadMulticast = new Thread(new InGameMulticast(client));

            threadTCP.start();
            threadUDP.start();
            threadMulticast.start();
            threadTCP.join();

            InGameUDP.stop();
            InGameMulticast.stop();
            inGame = false;
            start = false;
            inscrit = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void preGameActionNEWPL(InputStream is, OutputStream os) { // [NEWPL id port***] -> [REGOK m***] ou [REGNO***]
        try {
            //Envoi de la requete [NEWPL id port***]
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

            //Reception de la reponse [REGOK m***] ou [REGNO***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            doCaseREGOKorREGNO();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionREGIS(InputStream is, OutputStream os) { // [REGIS id port m***] -> [REGOK m***] ou [REGNO***]
        try {
            //Envoi de la requete [REGIS id port m***]
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
            os.write(req);
            os.flush();
            if(verbeux) { System.out.println("REGIS " + new String(bytesId) + " " + (new String(bytesPort) + " "
                    + (bytePartie[0] & 0xff) + "***")); }

            //Reception de la reponse [REGOK m***] ou [REGNO***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            doCaseREGOKorREGNO();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionUNREG(InputStream is, OutputStream os) { // [UNREG***] -> [UNROK m***] ou [DUNNO***]
        try {
            //Envoi de la requete [UNREG***]
            byte[] req = "UNREG".getBytes();
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);
            
            //Reception de la reponse [UNROK m***] ou [DUNNO***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            switch (action) {
                case "UNROK":
                    int numPartie = rep[6] & 0xff;
                    if(verbeux) { System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 3))); }
                    System.out.println("Vous avez bien ete desinscrit de la partie " + numPartie);
                    this.inscrit = false; 
                    break;
                case "DUNNO":
                    if(verbeux) { System.out.println(new String(rep, 5, 3)); }
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

    public void preGameActionSIZE(InputStream is, OutputStream os) { // [SIZE? m***] -> [SIZE! m h w***] ou [DUNNO***]
        try {
            //Envoi de la requete [SIZE? m***]
            //On demande un numero de partie
            byte[] bytePartie = askNumGame();

            byte[] req = "SIZE? ".getBytes();
            req = concatByteArrays(req, bytePartie);
            req = concatByteArrays(req, "***".getBytes());
            os.write(req);
            os.flush();
            if(verbeux) { System.out.println("SIZE? " + (bytePartie[0] & 0xff) + "***"); }

            //Reception de la reponse [SIZE! m h w***] ou [DUNNO***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            switch (action) {
                case "SIZE!":
                    int numPartie = rep[6] & 0xff;
                    short height = byteArrayToShortSwap(rep, 8);
                    short width = byteArrayToShortSwap(rep, 11);
                    if(verbeux) { System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                        + height + (new String(rep, 10, 1)) + width + (new String(rep, 13, 3))); }
                    System.out.println("Le labyrinthe de la partie " + numPartie + " a pour hauteur " + height
                        + " et pour largeur " + width);
                    break;
                case "DUNNO":
                    if(verbeux) { System.out.println(new String(rep, 5, 3)); }
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

    public void preGameActionNBHG(InputStream is, OutputStream os) { // [NBGH? m***] -> [NBGH! m f***] ou [DUNNO***]
        try {
            //Envoi de la requete [NBGH? m***]
            //On demande un numero de partie
            byte[] bytePartie = askNumGame();

            byte[] req = "NBGH? ".getBytes();
            req = concatByteArrays(req, bytePartie);
            req = concatByteArrays(req, "***".getBytes());
            os.write(req);
            os.flush();
            if(verbeux) { System.out.println("NBGH? " + (bytePartie[0] & 0xff) + "***"); }

            //Reception de la reponse [NBGH! m f***] ou [DUNNO***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            switch (action) {
                case "NBGH!":
                    int numPartie = rep[6] & 0xff;
                    int nbFantomes = rep[8] & 0xff;
                    if(verbeux) { System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                        + nbFantomes+ (new String(rep, 9, 3))); }
                    System.out.println("Le labyrinthe de la partie " + numPartie + " a " + nbFantomes
                        + " fantomes");
                    break;
                case "DUNNO":
                    if(verbeux) { System.out.println(new String(rep, 5, 3)); }
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

    public void preGameActionSIZEM(InputStream is, OutputStream os) { // [SIZEM h w***] -> [SIZEO***] ou [SIZEN***]
        try {
            //Envoi de la requete [SIZEM m***]
            //On demande une hauteur
            byte[] byteHeight = askTaille("hauteur");
            //On demande une largeur
            byte[] byteWidth = askTaille("largeur");

            byte[] req = "SIZEM ".getBytes();
            req = concatByteArrays(req, byteHeight);
            req = concatByteArrays(req, " ".getBytes());
            req = concatByteArrays(req, byteWidth);
            req = concatByteArrays(req, "***".getBytes());
            os.write(req);
            os.flush();
            short height = byteArrayToShortSwap(byteHeight, 0);
            short width = byteArrayToShortSwap(byteWidth, 0);
            if(verbeux) { System.out.println("SIZEM " + height + " " + width + "***"); }

            //Reception de la reponse [SIZEO***] ou [SIZEN***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 8);
            if(verbeux) { System.out.println(action); }
            switch (action) {
                case "SIZEO***":
                    System.out.println("Hauteur et largeur du labyrinthe modifiees");
                    break;
                case "SIZEN***":
                    System.out.println("Erreur : modification impossible");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void preGameActionNUMGH(InputStream is, OutputStream os) { // [NUMGH f***] -> [NUMGO***] ou [NUMGN***]
        try {
            //Envoi de la requete [NUMGH f***]
            //On demande un nombre de fantomes
            byte[] byteValue = askValue();

            byte[] req = "NUMGH ".getBytes();
            req = concatByteArrays(req, byteValue);
            req = concatByteArrays(req, "***".getBytes());
            os.write(req);
            os.flush();
            if(verbeux) { System.out.println("NUMGH " + (byteValue[0] & 0xfff) + "***"); }

            //Reception de la reponse [NUMGO***] ou [NUMGN***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 8);
            if(verbeux) { System.out.println(action); }
            switch (action) {
                case "NUMGO***":
                    System.out.println("Nombre de fantomes modifie");
                    break;
                case "NUMGN***":
                    System.out.println("Erreur : modification impossible");
                    break;
                default:
                    System.out.println(MESS_ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void preGameActionLIST(InputStream is, OutputStream os) { // [LIST? m***] -> ([LIST! m s***] et [PLAYR id***]) ou [DUNNO***]
        try {
            //Envoi de la requete [LIST? m***]
            //On demande un numero de partie
            byte[] bytePartie = askNumGame();

            byte[] req = "LIST? ".getBytes();
            req = concatByteArrays(req, bytePartie);
            req = concatByteArrays(req, "***".getBytes());
            os.write(req);
            os.flush();
            if(verbeux) { System.out.println("LIST? " + (bytePartie[0] & 0xff) + "***"); }

            //Reception de la reponse ([LIST! m s***] et s reponses [PLAYR id***]) ou [DUNNO***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            switch (action) {
                case "LIST!":
                    int numPartie = rep[6] & 0xff;
                    int nbJoueurs = rep[8] & 0xff;
                    if(verbeux) { System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                        + nbJoueurs + (new String(rep, 9, 3))); }
                    System.out.print("Partie " + numPartie + " : ");
                    System.out.println(nbJoueurs + " joueur(s) inscrit(s)");
                    if (!nextRep()) {
                        return;
                    }

                    //Reception de s reponse(s) [PLAYR id***]
                    for (; nbJoueurs != 0; nbJoueurs--) {
                        bytesRead = readRep(is);
                        if (bytesRead < 1) {
                            System.out.println(MESS_ERROR);
                            break;
                        }
                        action = new String(rep, 0, 5);
                        if(verbeux) { System.out.print(action); }
                        if (!action.equals("PLAYR")) {
                            System.out.println(MESS_ERROR);
                            break;
                        }
                        String id = new String(rep, 6, 8);
                        if(verbeux) { System.out.println((new String(rep, 5, 1)) + id + (new String(rep, 14, 3))); }
                        System.out.println(id);
                        if (nbJoueurs!=1) { 
                            if (!nextRep()) {
                                return;
                            }
                        }
                    }
                    break;
                case "DUNNO":
                    if(verbeux) { System.out.println(new String(rep, 5, 3)); }
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

    public void preGameActionGAME(InputStream is, OutputStream os) { //[GAME?***] -> [GAMES n***] et [OGAME m s***]
        try {
            //Envoi de la requete [GAME?***]
            byte[] req = "GAME?".getBytes();
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception de la reponse [GAMES n***] et de n reponse(s) [OGAME m s***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            doGAMESandOGAME(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doSTART(InputStream is, OutputStream os) { //[START***] -> [WELCO m h w f ip port***] et [POSIT id x y***]
        try {
            //Envoi de la requete [START***]
            byte[] req = "START".getBytes();
            req = concatByteArrays(req, "***".getBytes());
            writeReq(os, req);

            //Reception des reponses [WELCO m h w f ip port***] et [POSIT id x y***]
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            //On s'occupe de [WELCO m h w f ip port***]
            String action = new String(rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            if (!action.equals("WELCO")) {
                System.out.println(MESS_ERROR);
                return;
            }
            int numPartie = rep[6] & 0xff;
            short height = byteArrayToShortSwap(rep, 8);
            short width = byteArrayToShortSwap(rep, 11);
            lenX = height;
            lenY = width;
            int nbGhosts = rep[14] & 0xff;
            String ipMultiDiff = new String(rep, 16, 15);
            String portMultiDiff = new String(rep, 32, 4);
            if(verbeux) { System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 1))
                + height + (new String(rep, 10, 1)) + width + (new String(rep, 13, 1))
                + nbGhosts + (new String(rep, 15, 1)) + ipMultiDiff + (new String(rep, 31, 1))
                + portMultiDiff + (new String(rep, 36, 3))); }
            System.out.println("\nBienvenue !\nLe labyrinthe a pour hauteur " + height
                + ", pour largeur " + width + " et il y a " + nbGhosts + " fantomes a capturer !\nBONNE CHANCE\n");
            if (!nextRep()) {
                return;
            }
            
            //On s'occupe de [POSIT id x y***]
            int bytesRead2 = readRep(is);
            if (bytesRead2 < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            action = new String(rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            if (!action.equals("POSIT")) {
                System.out.println(MESS_ERROR);
                return;
            }
            String id = new String(rep, 6, 8); //45
            String pX = new String(rep, 15, 3); //54
            String pY = new String(rep, 19, 3); //58
            this.posX = Integer.parseInt(pX);
            this.posY = Integer.parseInt(pY);
            if(verbeux) { System.out.println((new String(rep, 5, 1)) + id + (new String(rep, 14, 1)) + pX
                + (new String(rep, 18, 1)) + pY + (new String(rep, 22, 3))); }
            System.out.println(id + ", vous etes en position (" + pX.replaceFirst("^0+(?!$)", "") + "," 
                + pY.replaceFirst("^0+(?!$)", "") + ")");
                
            this.start = true;
            this.inGame = true;
            this.ipMult = ipMultiDiff.replace("#", "");
            this.portMult = Integer.parseInt(portMultiDiff);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doCaseREGOKorREGNO() {
        String action = new String(rep, 0, 5);
        if(verbeux) { System.out.print(action); }
        switch (action) {
            case "REGOK":
                int numPartie = rep[6] & 0xff;
                if(verbeux) { System.out.println((new String(rep, 5, 1)) + numPartie + (new String(rep, 7, 3))); }
                System.out.println("Vous avez bien ete inscrit a la partie " + numPartie);
                this.inscrit = true;
                break;
            case "REGNO":
                if(verbeux) { System.out.println(new String(rep, 5, 3)); }
                System.out.println("Erreur : inscription a cette partie impossible");
                break;
            default:
                System.out.println(MESS_ERROR);
                break;
        }
    }

    //gere la reception des reponses GAMES et OGAME et renvoie false s'il y a eu une erreur, true sinon
    public boolean doGAMESandOGAME(InputStream is) throws IOException {
        String action = new String(this.rep, 0, 5);
        if(verbeux) { System.out.print(action); }
        if (!action.equals("GAMES")) {
            System.out.println(MESS_ERROR);
            return false;
        }
        int nbParties = this.rep[6] & 0xff;
        if(verbeux) { System.out.println((new String(this.rep, 5, 1)) + nbParties + (new String(this.rep, 7, 3))); }
        if (nbParties == 0) { 
            System.out.println("Aucune partie en attente");
            return true;
        }
        if (!nextRep()) {
            return false;
        }

        //Reception des reponses [OGAME m s***]
        for (; nbParties != 0; nbParties--) {
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return false;
            }
            action = new String(this.rep, 0, 5);
            if(verbeux) { System.out.print(action); }
            if (!action.equals("OGAME")) {
                System.out.println(MESS_ERROR);
                return false;
            }
            int numPartie = this.rep[6] & 0xff;
            int nbJoueurs = this.rep[8] & 0xff;
            if(verbeux) { System.out.println((new String(this.rep, 5, 1)) + numPartie + (new String(this.rep, 7, 1))
                + nbJoueurs + (new String(this.rep, 9, 3))); }
            System.out.print("Partie " + numPartie + " : ");
            System.out.println(nbJoueurs + " joueur(s) inscrit(s)");
            if (nbParties != 1 && !nextRep()) { 
                return false;
            }
        }
        return true;  
    }


    public byte[] askTaille(String type) {
        System.out.println("Entrez une " + type + " de labyrinthe < 1000");
        String taille = scanner.nextLine();
        while (true) {
            if (taille.chars().allMatch(Character::isDigit)) {
                int t = Integer.parseInt(taille);
                if (t >= 0 && t <= 9999) {
                    break;
                }
            }
            System.out.println(type + " invalide, recommencez");
            taille = scanner.nextLine();
        }
        short res = Short.parseShort(taille);
        byte[] tailleRes = new byte[2];
        tailleRes[0] = (byte)(res & 0xff);
        tailleRes[1] = (byte)((res >> 8) & 0xff);
        return tailleRes;
    }

    public byte[] askValue() {
        System.out.println("Entrez un nombre de fantomes < 256");
        String value = scanner.nextLine();
        while (true) {
            if (value.chars().allMatch(Character::isDigit)) {
                int t = Integer.parseInt(value);
                if (t >= 0 && t <= 255) {
                    break;
                }
            }
            System.out.println("Valeur invalide, recommencez");
            value = scanner.nextLine();
        }
        return new byte[] { (byte) Integer.parseInt(value) };
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


    public static byte[] recupNextRep(byte[] nextRep) {
        byte[] rep = new byte[MAX_BUFFER];
        for (int i = 0; i < nextRep.length; i++) {
            rep[i] = nextRep[i];
        }
        return rep;
    }

    public boolean nextRep() {
        bufferSize -= lenMessage;
        if (lenMessage > bufferSize+lenMessage) {
            System.out.println("Erreur");
            return false;
        }
        byte[] repTmp = Arrays.copyOfRange(rep, lenMessage, bufferSize+lenMessage);
        rep = recupNextRep(repTmp);
        return true;
    }

    public int readRep(InputStream is) throws IOException {
        while (true) {
            int tmp = containsStars(rep, bufferSize);
            if(tmp != -1) {
                lenMessage = tmp;
                break;
            }
            bufferSize += is.read(rep, bufferSize, MAX_BUFFER-bufferSize);
            if (bufferSize < 1) {
                return 0;
            }
        }
        return 1;
    }


    public static int containsStars(byte[] buff, int len) {
        for (int i = 0; i < len-2; i++){
            if(buff[i]==buff[i+1] && buff[i+1]==buff[i+2] && buff[i]=='*'){
                return i+3;
            }
        }
        return -1;
    }


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
            if(verbeux) { System.out.println(new String(req)); }
        } catch (IOException e) {
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


    public boolean isInGame() {
        return this.inGame;
    }

    public void setInGame(boolean inG) {
        this.inGame = inG;
    }

    public boolean isVerbeux() {
        return this.verbeux;
    }

    public int getPortUDP() {
        return this.portUDP;
    }

    public int getPortMult() {
        return this.portMult;
    }

    public String getIPMult() {
        return this.ipMult;
    }
}