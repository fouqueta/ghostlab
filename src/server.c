#include "../includes/server.h"

game ** game_list;

pthread_mutex_t verrou_main = PTHREAD_MUTEX_INITIALIZER;

int main(int argc, char ** argv) {
    if(argc < 2){
        printf("Missing port number !\n");
        exit(EXIT_FAILURE);
    }

    //Creation de la socket
    int sock_server = socket(PF_INET, SOCK_STREAM, 0);
    if(sock_server == -1){
        perror("Socket");
        exit(EXIT_FAILURE);
    }

    struct sockaddr_in addr_sock = {
        .sin_family = AF_INET,
        .sin_port = htons(atoi(argv[1])),
        .sin_addr.s_addr = htonl(INADDR_ANY)
    };

    //Bind socket
    int count = bind(sock_server, (struct sockaddr *)&addr_sock, sizeof(addr_sock));
    if(count == -1){
        perror("bind");
        exit(EXIT_FAILURE);
    }

    //Listen au port
    count = listen(sock_server, 0);
    if(count!=0){
        perror("listen");
        exit(EXIT_FAILURE);
    }

    init_game_list(game_list);

    //Boucle principale du serveur
    while(1) {
        int * sock_player = malloc(sizeof(int));

        struct sockaddr_in c;
        socklen_t size = sizeof(c);
        *sock_player = accept(sock_server, (struct sockaddr *)&c, &size);
        if(*sock_player<0){
            perror("accept");
            exit(EXIT_FAILURE);
        }
        pthread_t th;
        pthread_create(&th, NULL, listen_player, sock_player);
    }

    close(sock_server);
    return 0;
}

void* listen_player(void* args){
    int sock = *(int *) args;

    player * player_infos = NULL;

    if(sendGames(sock) == -1){
        printf("Error in sendGames\nConnexion with a player is stopped !");
        close(sock);
        return NULL;
    }

    char * message = malloc(1024);
    while(1){
        memset(message, 0, 1024);
        //TODO: recv while not recv "***"
        int buffer_size = recv(sock, message, 1024, MSG_NOSIGNAL);
        if(buffer_size == -1){
            perror("Recv");
            break;
        }else if(buffer_size<0){
            printf("Error");
            break;
        }

        char * action = malloc(6);
        memcpy(action, message, 5);
        action[5] = '\0';
        if(player_infos == NULL || player_infos->g->is_start == 0){
            //Cas si le joueurs n'est dans aucune partie ou la partie n'as pas commancée
            if(strncmp(action, "GAME?", 5) == 0 && buffer_size == 8){
                if(sendGames(sock) == -1) {
                    if(player_infos->g->is_start == 0){
                        //TODO: Une erreur à eu lieu lors de l'envoie, il faut le desincrire
                    }
                    break;
                }
            }else if(strncmp(action, "NEWPL", 5) == 0 && player_infos == NULL){
                //TODO: Creation d'une game
            }else if(strncmp(action, "REGIS", 5) == 0 && player_infos == NULL){
                //TODO: Rejoindre une game
            }else if(strncmp(action, "UNREG", 5) == 0){
                //TODO: Desincrire
            }else if(strncmp(action, "SIZE?", 5) == 0){
                //TODO: Taille du labyrinthe
            }else if(strncmp(action, "LIST?", 5) == 0){
                //TODO: Liste des joueurs de la partie
            }else if(strncmp(action, "START", 5) == 0){
                //TODO: Joueur prêt
            }else{
                if(sendDunno(sock) == -1){
                    if(player_infos->g->is_start == 0){
                        //TODO: Une erreur à eu lieu lors de l'envoie, il faut le desincrire
                    }
                    break;
                }
            }
        }else if(player_infos->g->is_start == 1){
            //Cas si la partie a commencée
            if(strncmp(action, "UPMOV", 5) == 0){
                //TODO: Se deplace vers le haut
            }else if(strncmp(action, "DOMOV", 5) == 0){
                //TODO: Se deplace vers le bas
            }else if(strncmp(action, "LEMOV", 5) == 0){
                //TODO: Se deplace vers la gauche
            }else if(strncmp(action, "RIMOV", 5) == 0){
                //TODO: Se deplace vers la droite
            }else if(strncmp(action, "IQUIT", 5) == 0){
                //TODO: Quitter la partie
            }else if(strncmp(action, "GLIS?", 5) == 0){
                //Liste des joueurs dans la partie du joueur
                if(sendGList(sock, player_infos->g) == -1){
                    //TODO: Une erreur à eu lieu lors de l'envoie, il faut le desincrire
                    break;
                }
            }else if(strncmp(action, "MALL?", 5) == 0) {
                //TODO: Envoye un message à tout les autres joueurs
            }else{
                if(sendDunno(sock) == -1){
                    //TODO: Une erreur à eu lieu lors de l'envoie, il faut le desincrire
                    break;
                }

            }
        }else{
            //Es-ce qu'il y a d'autres cas ?
            sendDunno(sock);
        }

        free(action);
    }
    free(message);
    close(sock);


    return 0;
}
