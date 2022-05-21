#include "../includes/server.h"

typedef struct thread_args {
    int fd;
    char *ip;
} thread_args;

game ** game_list;

pthread_mutex_t verrou_main = PTHREAD_MUTEX_INITIALIZER;

int main(int argc, char ** argv) {
    if(argc < 2){
        printf("Missing port number !\n");
        exit(EXIT_FAILURE);
    }
    int port = atoi(argv[1]);
    if(port < 1024 || port > 49151){
        printf("Wrong port number !\n");
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
        .sin_port = htons(port),
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
            continue;
        }
        thread_args *th_args = malloc(sizeof(thread_args));
        th_args->fd = *sock_player;
        th_args->ip = inet_ntoa(c.sin_addr);

        pthread_t th;
        pthread_create(&th, NULL, listen_player, th_args);
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

int getDistance(char *message){
    char buffer[4];
    memcpy(buffer, message, 3);
    buffer[3] = '\0';
    return atoi(buffer);
}

void* listen_player(void* args){
    thread_args *th_args = (thread_args *) args;
    int sock = th_args->fd;
    char *ip = th_args->ip;

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
                len_message = -1;
                break;
            }else if(buffer_size==0){
                len_message = -1;
                break;
            }
        }
        if(len_message == -1)
            break;

        char * action = malloc(6);
        memcpy(action, message, 5);
        action[5] = '\0';
        if(player_infos == NULL || player_infos->g->state_game == 1){
            //Cas si les joueurs n'est dans aucune partie ou la partie n'as pas commencée
            if(strncmp(action, "GAME?", 5) == 0 && len_message == 8){
                if(sendGames(sock) == -1) {
                    break;
                }
            }else if(strncmp(action, "SIZEM", 5) == 0 && player_infos != NULL){
                int h = (message[7]<<8) + message[6];
                int w = (message[10]<<8) + message[9];
                h = le16toh(h);
                w = le16toh(w);

                time_t seconds = time(NULL);
                if(player_infos->g->last_update == 0 || difftime(seconds, player_infos->g->last_update) >= 10){
                    if(h == 0){
                        player_infos->g->laby->lenX = X_DEFAULT;
                    }else{
                        player_infos->g->laby->lenX = h;
                    }

                    if(w == 0){
                        player_infos->g->laby->lenY = Y_DEFAULT;
                    }else{
                        player_infos->g->laby->lenY = w;
                    }

                    if(sendSizeo(sock) == -1){
                        break;
                    }
                    player_infos->g->last_update = seconds;

                }else{
                    if(sendSizen(sock)== -1){
                        break;
                    }
                }
            }else if(strncmp(action, "NUMGH", 5) == 0 && player_infos != NULL){
                uint8_t f = message[6];
                time_t seconds = time(NULL);
                if(player_infos->g->last_update == 0 || difftime(seconds, player_infos->g->last_update) >= 10){
                    if(f == 0){
                        player_infos->g->nb_ghosts = NB_GHOSTS_DEFAULT;
                    }
                    else{
                        player_infos->g->nb_ghosts = f;
                    }
                    if(sendNumgo(sock) == -1){
                        break;
                    }
                    player_infos->g->last_update = seconds;

                }else{
                    if(sendNumgn(sock)== -1){
                        break;
                    }
                }
            }else if(strncmp(action, "NEWPL", 5) == 0 && player_infos == NULL){
                char pseudo[8];
                memcpy(pseudo, message+6, 8);
                char port[4];
                memcpy(port, message+15, 4);
                char ip_player[15];
                memset(ip_player, 0, 15);
                memcpy(ip_player, ip, strlen(ip));

                int8_t m = get_empty_game();
                player_infos = init_player(pseudo, port, ip_player);
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

                }
            }else if(strncmp(action, "REGIS", 5) == 0 && player_infos == NULL){
                //REGIS 12345678 1234 m***
                char pseudo[8];
                memcpy(pseudo, message+6, 8);
                char port[4];
                memcpy(port, message+15, 4);
                char ip_player[15];
                memset(ip_player, 0, 15);
                memcpy(ip_player, ip, strlen(ip));
                int8_t m = message[20];

                if( m<0 || m>NB_GAMES
                        || game_list[m]->state_game != 1
                        || name_taken(game_list[m]->list.first, pseudo) == 1){
                    if(sendRegno(sock) == -1){
                        break;
                    }
                }else{
                    player_infos = init_player(pseudo, port, ip_player);
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
            }else if(strncmp(action, "NBGH?", 5) == 0){
                int8_t m = message[6];
                if(m < 0 || m > NB_GAMES || game_list[m]->state_game == 0){
                    if(sendDunno(sock) == -1){
                        break;
                    }
                }else if(sendNbgh(sock, game_list[m]) == -1){
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
                pthread_mutex_lock(&(player_infos->g->verrou_for_cond));
                pthread_mutex_lock(&(player_infos->g->verrou_server));
                player_infos->g->nb_ready++;
                if(player_infos->g->nb_ready == player_infos->g->nb_players) { //&& player_infos->g->nb_players > 1){
                    getAMaze(player_infos->g->laby);
                    initGhosts(player_infos->g->laby, player_infos->g->nb_ghosts);
                    placePlayers(player_infos->g);
                    set_port(player_infos->g);
                    player_infos->g->state_game = 2;
                    pthread_mutex_unlock(&(player_infos->g->verrou_server));

                    pthread_cond_signal(&(player_infos->g->cond));
                    pthread_t th;
                    pthread_create(&th, NULL, gameFunc, player_infos->g);
                }else{
                    int nb_players = player_infos->g->nb_players;
                    int nb_ready = player_infos->g->nb_ready;
                    pthread_mutex_unlock(&(player_infos->g->verrou_server));
                    while(nb_ready != nb_players) { //|| nb_players < 2){
                        pthread_cond_wait(&(player_infos->g->cond), &(player_infos->g->verrou_for_cond));
                        pthread_mutex_lock(&(player_infos->g->verrou_server));
                        nb_players = player_infos->g->nb_players;
                        nb_ready = player_infos->g->nb_ready;
                        pthread_mutex_unlock(&(player_infos->g->verrou_server));
                    }
                }

                pthread_mutex_unlock(&(player_infos->g->verrou_for_cond));

                if(sendStart(sock, player_infos)==-1){
                    break;
                }
            }else{
                if(sendDunno(sock) == -1){
                    break;
                }
            }
        }else if(player_infos->g->state_game == 2){
            //Cas si la partie a commencée
            int distance = getDistance(message+6);
            char **lab = player_infos->g->laby->maze;
            int flag_ghost = 0;
            int res_move;
            if(strncmp(action, "UPMOV", 5) == 0){
                pthread_mutex_lock(&(player_infos->verrou_player));
                for(int i = 0; i<distance; i++){
                    int x = player_infos->x;
                    int y = player_infos->y;
                    if(x-1 < 0 || lab[x-1][y] == CHARWALL){
                        break;
                    }
                    else {
                        player * col = player_meet(player_infos, x-1, y);
                        if(col != NULL){
                            if(sendCol(player_infos->g, player_infos->name, col->name, x-1, y) == -1){
                                break;
                            }
                            sleep(5);
                        }
                        res_move = move_player(player_infos, x-1, y);
                        if(res_move == 1){
                            flag_ghost = flag_ghost + 1;
                            player_infos->g->nb_ghosts--;
                            if(sendScore(player_infos) == -1){
                                break;
                            }
                        }
                    }
                }
                pthread_mutex_unlock(&(player_infos->verrou_player));
                if(sendMove(sock, player_infos, flag_ghost) == -1){
                    break;
                }
                pthread_mutex_lock(&(player_infos->g->verrou_server));
                if(player_infos->g->nb_ghosts == 0){
                    sendEnd(player_infos->g);
                    player_infos->g->state_game = 3;
                }
                pthread_mutex_unlock(&(player_infos->g->verrou_server));
            }else if(strncmp(action, "DOMOV", 5) == 0){
                pthread_mutex_lock(&(player_infos->verrou_player));
                for(int i = 0; i<distance; i++){
                    int x = player_infos->x;
                    int y = player_infos->y;
                    if(x+1 >= player_infos->g->laby->lenX || lab[x+1][y] == CHARWALL){
                        break;
                    }
                    else {
                        player * col = player_meet(player_infos, x+1, y);
                        if(col != NULL){
                            if(sendCol(player_infos->g, player_infos->name, col->name, x-1, y) == -1){
                                break;
                            }
                            sleep(5);
                        }
                        res_move = move_player(player_infos, x+1, y);
                        if(res_move == 1){
                            flag_ghost = flag_ghost + 1;
                            player_infos->g->nb_ghosts--;
                            if(sendScore(player_infos) == -1){
                                break;
                            }
                        }
                    }
                }
                pthread_mutex_unlock(&(player_infos->verrou_player));
                if(sendMove(sock, player_infos, flag_ghost) == -1){
                    break;
                }
                pthread_mutex_lock(&(player_infos->g->verrou_server));
                if(player_infos->g->nb_ghosts == 0){
                    sendEnd(player_infos->g);
                    player_infos->g->state_game = 3;
                }
                pthread_mutex_unlock(&(player_infos->g->verrou_server));
            }else if(strncmp(action, "LEMOV", 5) == 0){
                pthread_mutex_lock(&(player_infos->verrou_player));
                for(int i = 0; i<distance; i++){
                    int x = player_infos->x;
                    int y = player_infos->y;
                    if(y-1 < 0 || lab[x][y-1] == CHARWALL){
                        break;
                    }
                    else {
                        player * col = player_meet(player_infos, x, y-1);
                        if(col != NULL){
                            if(sendCol(player_infos->g, player_infos->name, col->name, x-1, y) == -1){
                                break;
                            }
                            sleep(5);
                        }
                        res_move = move_player(player_infos, x, y-1);
                        if(res_move == 1){
                            flag_ghost = flag_ghost + 1;
                            player_infos->g->nb_ghosts--;
                            if(sendScore(player_infos) == -1){
                                break;
                            }
                        }
                    }
                }
                pthread_mutex_unlock(&(player_infos->verrou_player));
                if(sendMove(sock, player_infos, flag_ghost) == -1){
                    break;
                }
                pthread_mutex_lock(&(player_infos->g->verrou_server));
                if(player_infos->g->nb_ghosts == 0){
                    sendEnd(player_infos->g);
                    player_infos->g->state_game = 3;
                }
                pthread_mutex_unlock(&(player_infos->g->verrou_server));
            }else if(strncmp(action, "RIMOV", 5) == 0){
                pthread_mutex_lock(&(player_infos->verrou_player));
                for(int i = 0; i<distance; i++){
                    int x = player_infos->x;
                    int y = player_infos->y;
                    if(y+1 >= player_infos->g->laby->lenY || lab[x][y+1] == CHARWALL){
                        break;
                    }
                    else {
                        player * col = player_meet(player_infos, x, y+1);
                        if(col != NULL){
                            if(sendCol(player_infos->g, player_infos->name, col->name, x-1, y) == -1){
                                break;
                            }
                            sleep(5);
                        }
                        res_move = move_player(player_infos, x, y+1);
                        if(res_move == 1){
                            flag_ghost = flag_ghost + 1;
                            player_infos->g->nb_ghosts--;
                            if(sendScore(player_infos) == -1){
                                break;
                            }
                        }
                    }
                }
                pthread_mutex_unlock(&(player_infos->verrou_player));
                if(sendMove(sock, player_infos, flag_ghost) == -1){
                    break;
                }
                pthread_mutex_lock(&(player_infos->g->verrou_server));
                if(player_infos->g->nb_ghosts == 0){
                    sendEnd(player_infos->g);
                    player_infos->g->state_game = 3;
                }
                pthread_mutex_unlock(&(player_infos->g->verrou_server));
            }else if(strncmp(action, "IQUIT", 5) == 0){
                if(sendQuit(sock) == -1){
                    break;
                }
                break;
            }else if(strncmp(action, "GLIS?", 5) == 0){
                //Liste des joueurs dans la partie du joueur
                if(sendGList(sock, player_infos->g) == -1){
                    break;
                }
            }else if(strncmp(action, "MALL?", 5) == 0) {
                char buff_tmp[200];
                memcpy(buff_tmp, message+6, 200);
                int size_tmp = strlen(buff_tmp);
                char *mess = malloc(size_tmp-2);
                memcpy(mess, buff_tmp, size_tmp-3);
                mess[strlen(mess)] = '\0';

                if(sendMessAll(sock, player_infos, mess) == -1){
                    break;
                }
            }else if(strncmp(action, "SEND?", 5) == 0){
                char id[8];
                memcpy(id, message+6, 8);
                char buff_tmp[200];
                memcpy(buff_tmp, message+15, 200);
                int size_tmp = strlen(buff_tmp);
                char *mess = malloc(size_tmp-2);
                memcpy(mess, buff_tmp, size_tmp-3);
                mess[strlen(mess)] = '\0';

                if(sendMess(sock, player_infos, id, mess) == -1){
                    break;
                }
            }else{
                if(sendDunno(sock) == -1){
                    break;
                }
            }
        }else{
            if(player_infos->g->state_game == 3){
                if(sendQuit(sock) == -1){
                    break;
                }
                break;
            }
        }

        free(action);

        buffer_size = buffer_size-len_message;
        char * buff_tmp = malloc(buffer_size);
        memmove(buff_tmp, message+len_message, buffer_size);
        memset(message, 0, 1024);
        memmove(message, buff_tmp, buffer_size);
        free(buff_tmp);
    }
    //Cas d'erreur: Joueur s'est deconnecté, une erreur s'est produite ou la partie est finie.
    if(player_infos != NULL && player_infos->g != NULL){
        game *g = player_infos->g;
        remove_player_game(player_infos, g->id_game);
        player_infos = NULL;
    }
    free(message);
    close(sock);


    return 0;
}
