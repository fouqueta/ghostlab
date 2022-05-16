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

int containsStars(char * buff, int len){
    for(int i=0;i<len-2;i++){
        if(buff[i] == buff[i+1] && buff[i+1]==buff[i+2] && buff[i]== '*'){
            return i+3;
        }
    }
    return -1;
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
    memset(message,0,1024);
    int len_message = 0;
    int buffer_size = 0;
    while(1){
        while(1){
            int tmp = containsStars(message, buffer_size);
            if(tmp != -1){
                len_message = tmp;
                break;
            }

            buffer_size += recv(sock, message+buffer_size, 1024-buffer_size, MSG_NOSIGNAL);
            if(buffer_size == -1){
                perror("Recv");
                break;
            }else if(buffer_size==0){
                printf("Error");
                break;
            }
        }


        char * action = malloc(6);
        memcpy(action, message, 5);
        action[5] = '\0';
        if(player_infos == NULL || player_infos->g->state_game == 1){
            //Cas si les joueurs n'est dans aucune partie ou la partie n'as pas commencée
            if(strncmp(action, "GAME?", 5) == 0 && len_message == 8){
                if(sendGames(sock) == -1) {
                    break;
                }
            }else if(strncmp(action, "NEWPL", 5) == 0 && player_infos == NULL){
                char pseudo[8];
                memcpy(pseudo, message+6, 8);
                char port[4];
                memcpy(port, message+15, 4);

                int8_t m = get_empty_game();
                player_infos = init_player(pseudo, port);
                if(m<0){
                    if(sendRegno(sock) == -1){
                        break;
                    }
                }else{
                    init_a_game(m);
                    if(add_player_game(player_infos, m) == -1){
                        if(sendRegno(sock) == -1){
                            break;
                        }
                    }
                    if(sendRegok(sock, m) == -1){
                        break;
                    }
                    //TODO: Lancer un thread qui va s'occuper de la partie
                }
            }else if(strncmp(action, "REGIS", 5) == 0 && player_infos == NULL){
                //REGIS 12345678 1234 m***
                char pseudo[8];
                memcpy(pseudo, message+6, 8);
                char port[4];
                memcpy(port, message+15, 4);
                int8_t m = message[20];

                if( m<0 || m>NB_GAMES
                        || game_list[m]->state_game != 1
                        || name_taken(game_list[m]->list.first, pseudo) == 1){
                    if(sendRegno(sock) == -1){
                        break;
                    }
                }else{
                    player_infos = init_player(pseudo, port);
                    if(add_player_game(player_infos, m) == -1){
                        if(sendRegno(sock) == -1){
                            break;
                        }
                    }
                    if(sendRegok(sock, m) == -1){
                        break;
                    }
                }

            }else if(strncmp(action, "UNREG", 5) == 0){
                if(player_infos == NULL){
                    if(sendDunno(sock) == -1){
                        break;
                    }
                }else{
                    int8_t m = player_infos->g->id_game;
                    remove_player_game(player_infos, m);
                    player_infos = NULL;
                    if(sendUnrok(sock, m) == -1){
                        break;
                    }
                }
            }else if(strncmp(action, "SIZE?", 5) == 0){
                int8_t m = message[6];
                if(m<0 || m>NB_GAMES || game_list[m]->state_game == 0){
                    if(sendDunno(sock) == -1){
                        break;
                    }
                }else if(sendSize(sock, game_list[m]) == -1){
                    break;
                }
            }else if(strncmp(action, "LIST?", 5) == 0){
                int8_t m = message[6];
                if(m<0 || m>NB_GAMES || game_list[m]->state_game == 0){
                    if(sendDunno(sock) == -1){
                        break;
                    }
                }else if(sendList(sock, game_list[m]) == -1){
                    break;
                }
            }else if(strncmp(action, "START", 5) == 0){
                //TODO: Joueur prêt
            }else{
                if(sendDunno(sock) == -1){
                    break;
                }
            }
        }else if(player_infos->g->state_game == 2){
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
                    break;
                }
            }else if(strncmp(action, "MALL?", 5) == 0) {
                //TODO: Envoye un message à tout les autres joueurs
            }else{
                if(sendDunno(sock) == -1){
                    break;
                }
            }
        }else{
            //Es-ce qu'il y a d'autres cas ?
            sendDunno(sock);
        }

        free(action);

        buffer_size = buffer_size-len_message;
        char * buff_tmp = malloc(buffer_size);
        memmove(buff_tmp, message+len_message, buffer_size);
        memset(message, 0, 1024);
        memmove(message, buff_tmp, buffer_size);
        free(buff_tmp);

    }
    /*TODO: Verifier si le joueur est dans une partie (commencée ou non), si c'est le cas il faut le desinscrire
    Si on arrive ici c'est que le joueur s'est deconnecté/Une erreur s'est produite/La partie est fini*/
    free(message);
    close(sock);


    return 0;
}
