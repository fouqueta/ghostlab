import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private boolean start = false;
    private boolean inscrit = false;

    public static short byteArrayToShortSwap(byte[] buffer, int offset) {
        short value = 0;
        for (int i = 0; i < 2; i++) {
            value |= (buffer[i + offset] & 0x000000FF) << (i * 8);
        }            
        return value;
    }

    public static byte[] concatByteArrays(byte[] b1, byte[] b2) {
        byte[] res = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, res, 0, b1.length);
        System.arraycopy(b2, 0, res, b1.length, b2.length);
        return res;
    }

    public static byte[] addStars(byte[] b) {
        byte[] stars = "***".getBytes();
        return concatByteArrays(b, stars);
    }

    // pour les requetes de format [5char***]
    public String sendSimpleReq (OutputStream os, String action) throws IOException {
        byte[] byteAction = action.getBytes();
        byte[] req = addStars(byteAction);
        os.write(req);
        os.flush();
        return new String(req);
    }

    // pour les requetes de format [5char␣x***], avec x sur un octet
    public String send1ParamReq (OutputStream os, String action, short param) throws IOException {
        byte[] byteAction = action.getBytes();
        byte[] byteParam = { ' ', (byte)param }; //verifier comment marche le downcast
        byte[] req = addStars(concatByteArrays(byteAction, byteParam));
        os.write(req);
        os.flush();
        return new String(req);
    }


    public void preGameActionNEWPL(InputStream is, PrintWriter pw) { // [NEWPL␣id␣port***] -> [REGOK␣m***] ou [REGNO***]

    }

    public void preGameActionREGIS(InputStream is, PrintWriter pw) { // [REGIS␣id␣port␣m***] -> [REGOK␣m***] ou [REGNO***]
    
    }

    public void preGameActionUNREG(InputStream is, OutputStream os) { // [UNREG***] -> [UNROK␣m***] ou [DUNNO***]
        try {
            String req = sendSimpleReq(os, "UNREG");
            System.out.println(req);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void preGameActionSIZE(InputStream is, PrintWriter pw) { // [SIZE?␣m***] -> [SIZE!␣m␣h␣w***] ou [DUNNO***]

    }

    public void preGameActionLIST(InputStream is, PrintWriter pw) { // [LIST?␣m***] -> [LIST!␣m␣s***] ou [PLAYR␣id***] ou [DUNNO***]

    }

    public void preGameActionGAME(InputStream is, OutputStream os) { //[GAME?***] -> [GAMES␣n***] ou [OGAME␣m␣s***]
        try {
            String req = sendSimpleReq(os, "GAME?");
            System.out.println(req);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void preGameActionSTART(Socket fdSock) { //[START***]
        
    }



    public void doPreGameActions(InputStream is, OutputStream os) {
        Scanner sc = new Scanner(System.in);

        while (!this.start) { //tant que le joueur n'a pas envoyé start, le joueur peut faire certaines actions
            System.out.println("\nTapez");
            System.out.println("1 pour savoir la liste des parties non commencees");
            System.out.println("2 pour savoir la liste des joueurs d'une partie");
            System.out.println("3 pour savoir la taille du labyrinthe d'une partie");
            System.out.println("4 pour vous désinscrire de la partie ou vous etes inscrit");
            System.out.println("5 pour quitter le jeu");
            if (!this.inscrit) {
                System.out.println("6 pour creer une nouvelle partie");
                System.out.println("7 pour rejoindre une partie");
            }

            String numAction = sc.nextLine();
            System.out.println("Vous avez saisi le nombre : " + numAction);
            switch (numAction) {
                case "1":
                    System.out.println("liste des parties");
                    break;
                case "2":
                    System.out.println("liste des joueurs d'une partie");
                    break;
                case "3":
                    System.out.println("taille d'un labyrinthe d'une partie");
                    break;
                case "4":
                    System.out.println("desinscription");
                    this.inscrit = false; 
                    break;
                case "5":
                    System.out.println("quitter le jeu");
                    sc.close();
                    return;
                case "6":
                    if (this.inscrit) {
                        System.out.println("Mauvaise commande");
                        break;
                    }
                    System.out.println("creer nouvelle partie");
                    this.inscrit = true;
                    break;
                case "7":
                    if (this.inscrit) {
                        System.out.println("Mauvaise commande");
                        break;
                    }
                    System.out.println("rejoindre une partie");
                    this.inscrit = true;
                    break;  
                default:
                    System.out.println("Mauvaise commande");
                    break;
            }
        }
        sc.close();
    }


    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("Missing port number !");
            System.exit(0);
        }
        int portTCP = Integer.parseInt(args[0]);
        Client client = new Client();

        try{
            Socket fdSock = new Socket("127.0.0.1", portTCP);
            InputStream is = fdSock.getInputStream();
            OutputStream os = fdSock.getOutputStream();
            
            byte[] buffer = new byte[10];

            int bytesRead = is.read(buffer);
            System.out.print(new String(buffer, 0, 6)); //"GAMES "
            int nbParties = buffer[6]; 
            System.out.print(nbParties); //nb de parties non commencees
            System.out.println(new String(buffer, 7, 3)); //"***"

            for(; nbParties != 0; nbParties--){
                byte[] buffer2 = new byte[12];
                bytesRead = is.read(buffer2);
                System.out.print(new String(buffer2, 0, 6)); //"OGAME "
                System.out.print(buffer2[6]); //numero de la partie
                System.out.print(new String(buffer2, 7, 1)); //" "
                System.out.print(buffer2[8]); //nb de joueurs inscrits
                System.out.println(new String(buffer2, 9, 3)); //"***"
                if(bytesRead == -1) { break; }
            }

            client.doPreGameActions(is, os);
            
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