import java.net.*;
import java.io.*;
import java.math.*;
import java.util.Scanner;

public class Client {
    private boolean start = false;
    private boolean inscrit = false;

    public void doPreGameActions(Socket fdSock) {
        Scanner sc = new Scanner(System.in);
        // TODO while(pas encore envoye start) : faire les 6 sysout avec les entrees clavier/switch correspondants
        // avec un if(inscrit dans une partie) ne pas afficher les 2 premiers sysout
        while (!this.start) { //tant que le joueur n'a pas envoyé start, le joueur peut faire certaines actions
            System.out.println("\nTapez");
            System.out.println("1 pour savoir la liste des parties non commencees");
            System.out.println("2 pour savoir la liste des joueurs d'une partie");
            System.out.println("3 pour savoir la taille du labyrinthe d'une partie");
            System.out.println("4 pour vous désinscrire d'une partie");
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
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(fdSock.getOutputStream()));
            
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

            client.doPreGameActions(fdSock);
            
            is.close();
            pw.close();
            fdSock.close();
        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }
}