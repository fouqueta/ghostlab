import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class InGameTCP implements Runnable {
    static final int MAX_BUFFER = 256;
    static final String MESS_ERROR = "Erreur : veuillez recommencer";
    private Client client;
    private byte[] bRep = new byte[MAX_BUFFER];
    private int bufSize = 0;
    private int lenMess = 0;
    private boolean inGame;
    private InputStream is;
    private OutputStream os;
    private Scanner scanner = new Scanner(System.in);
    public char[][] maze;
    int x;
    int y;

    public InGameTCP(Client c, InputStream i, OutputStream o) {
        this.client = c;
        synchronized(client) { 
            this.inGame = client.isInGame(); 
        }
        this.is = i;
        this.os = o;
        maze = new char[Client.lenX][Client.lenY];
        x = Client.posX;
        y = Client.posY;
        for(int k=0;k<maze.length; k++){
            for(int j=0;j<maze[k].length; j++){
                maze[k][j] = '?';
            }
        }
        maze[x][y] = ' ';
    }

    @Override
    public void run() {
        try {
            while (inGame) {
                printMaze();
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
                if (action.equals("1")){
                    inGameActionGLIS(is, os);
                }
                else if (action.equals("2")){
                    inGameActionSEND(is, os);
                }
                else if (action.equals("3")){
                    inGameActionMALL(is, os);
                }
                else if (action.equals("4")){
                    inGameActionIQUIT(is, os);
                }
                else if ((action.length()>1 && action.length()<=4) &&
                    (action.charAt(0)=='D' || action.charAt(0)=='G' || 
                    action.charAt(0)=='H' || action.charAt(0)=='B')) {
                    if (!action.substring(1).chars().allMatch(Character::isDigit)) {
                        System.out.println("Nombre de cases invalide");
                        synchronized(client) { this.inGame = client.isInGame(); }
                        continue;
                    }else{
                        inGameActionsMOV(action, is, os);
                    }
                }
                else {
                    System.out.println("Mauvaise commande");
                    synchronized(client) { this.inGame = client.isInGame(); }
                    continue;
                }
                // System.out.println("AVANT bRep : " + new String(bRep));
                // System.out.println("AVANT bufSize : " + bufSize);
                // System.out.println("AVANT lenMess : " + lenMess);
                if (!nextRep()) {
                    return;
                }
                // System.out.println("APRES bRep : " + new String(bRep));
                // System.out.println("APRES bufSize : " + bufSize);
                // System.out.println("APRES lenMess : " + lenMess);
                synchronized(client) { this.inGame = client.isInGame(); }
            }
            scanner.close();
        } catch (Exception e) {
            synchronized(client) { this.inGame = client.isInGame(); }
            if (this.inGame) { e.printStackTrace(); }
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
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(bRep, 0, 6);
            if(client.isVerbeux()) { System.out.print(action); }
            switch (action) {
                case "MOVE! ":
                    String posX = new String(bRep, 6, 3);
                    String posY = new String(bRep, 10, 3);
                    if(client.isVerbeux()) { System.out.println(posX + (new String(bRep, 9, 1)) + posY + (new String(bRep, 13, 3))); }
                    System.out.println("Vous etes maintenant en position (" + posX.replaceFirst("^0+(?!$)", "") 
                        + "," + posY.replaceFirst("^0+(?!$)", "") + ").");
                    updateMaze(instr, Integer.parseInt(posX), Integer.parseInt(posY));
                    break;
                case "MOVEF ":
                    posX = new String(bRep, 6, 3);
                    posY = new String(bRep, 10, 3);
                    String score = new String(bRep, 14, 4);
                    if(client.isVerbeux()) { System.out.println(posX + (new String(bRep, 9, 1)) + posY + (new String(bRep, 13, 1)) 
                            + score + (new String(bRep, 18, 3))); }
                    score = score.replaceFirst("^0+(?!$)", "");
                    System.out.println("Vous avez attrape un fantome !");
                    System.out.println("Vous etes maintenant en position (" + posX.replaceFirst("^0+(?!$)", "") + "," 
                        + posY.replaceFirst("^0+(?!$)", "") + ") avec " + score + " points.");
                    updateMaze(instr, Integer.parseInt(posX), Integer.parseInt(posY));
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
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(bRep, 0, 6);
            if(client.isVerbeux()) { System.out.print(action); }
            if (!action.equals("GLIS! ")) {
                System.out.println(MESS_ERROR);
                return;
            }
            int nbJoueurs = bRep[6] & 0xff;
            if(client.isVerbeux()) { System.out.println(nbJoueurs + (new String(bRep, 7, 3))); }
            if (!nextRep()) {
                return;
            }

            //Reception de s reponses [GPLYR id x y p***]
            for (; nbJoueurs != 0; nbJoueurs--) {
                bytesRead = readRep(is);
                if (bytesRead < 1) {
                    System.out.println(MESS_ERROR);
                    break;
                }
                action = new String(bRep, 0, 6);
                if(client.isVerbeux()) { System.out.print(action); }
                if (!action.equals("GPLYR ")) {
                    System.out.println(MESS_ERROR);
                    break;
                }
                String id = new String(bRep, 6, 8);
                String posX = new String(bRep, 15, 3);
                String posY = new String(bRep, 19, 3);
                String score = new String(bRep, 23, 4);
                if(client.isVerbeux()) { System.out.println(id + (new String(bRep, 14, 1)) + posX + (new String(bRep, 18, 1)) 
                        + posY + (new String(bRep, 22, 1)) + score + (new String(bRep, 27, 3))); }
                score = score.replaceFirst("^0+(?!$)", "");
                System.out.println(id + " est en position (" + posX.replaceFirst("^0+(?!$)", "") + "," 
                    + posY.replaceFirst("^0+(?!$)", "") + ")" + " et a " + score + " points.");
                if (nbJoueurs != 1 && !nextRep()) {
                    return;
                }
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
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(bRep, 0, 8);
            if(client.isVerbeux()) { System.out.println(action); }
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
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(bRep, 0, 8);
            if(client.isVerbeux()) { System.out.println(action); }
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
            int bytesRead = readRep(is);
            if (bytesRead < 1) {
                System.out.println(MESS_ERROR);
                return;
            }
            String action = new String(bRep, 0, 8);
            if(client.isVerbeux()) { System.out.println(action); }
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
            if(client.isVerbeux()) { System.out.println(new String(req)); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int readRep(InputStream is) throws IOException {
        while (true) {
            int tmp = Client.containsStars(bRep, bufSize);
            if(tmp != -1) {
                lenMess = tmp;
                break;
            }
            bufSize += is.read(bRep, bufSize, MAX_BUFFER-bufSize);
            if (bufSize < 1) {
                return 0;
            }
        }
        return 1;
    }

    public boolean nextRep() {
        bufSize -= lenMess;
        if (lenMess > bufSize+lenMess) {
            System.out.println("Erreur");
            return false;
        }
        byte[] repTmp = Arrays.copyOfRange(bRep, lenMess, bufSize+lenMess);
        bRep = Client.recupNextRep(repTmp);
        return true;
    }

    public void printMaze(){
        int minX = x < 4 ? 0 : x-4;
        int maxX = x > Client.lenX-4 ? Client.lenX : x+4 ;
        int minY = y < 4 ? 0 : y-4;
        int maxY = y > Client.lenY-4 ? Client.lenY : y+4 ;

        for(int i=minX;i< maxX; i++){
            for(int j=minY;j< maxY; j++){
                if(i==x && j==y)
                    System.out.print("X ");
                else
                    System.out.print(maze[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void updateMaze(String instr, int pos_X, int pos_Y){
        char dir = instr.charAt(0);
        int nb = Integer.parseInt(instr.substring(1));

        switch(dir){
            case 'D':
                for(int j = y; j <= pos_Y; j++){
                    maze[x][j] = ' ';
                }
                if(y+nb != pos_Y && pos_Y+1 < Client.lenY){
                    maze[x][pos_Y+1] = '#';
                }
                break;
            case 'G':
                for(int j = y; j>pos_Y-1; j--){
                    maze[x][j] = ' ';
                }
                if(y-nb != pos_Y && pos_Y-1 > -1){
                    maze[x][pos_Y-1] = '#';
                }
                break;
            case 'H':
                for(int i = x; i>pos_X-1; i--){
                    maze[i][y] = ' ';
                }
                if(x-nb != pos_X && pos_X-1 > -1){
                    maze[pos_X-1][y] = '#';
                }
                break;
            case 'B':
                for(int i = x; i <= pos_X; i++){
                    maze[i][y] = ' ';
                }
                if(x+nb != pos_X && pos_X+1 < Client.lenX){
                    maze[pos_X+1][y] = '#';
                }
                break;
        }
        x = pos_X;
        y = pos_Y;
    }
    
}
