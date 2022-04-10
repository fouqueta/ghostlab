#include "../includes/server.h"

games games_started = {
        .len = 0
};
games games_not_started = {
        .len = 0
};

pthread_mutex_t verrou_main = PTHREAD_MUTEX_INITIALIZER;

int main(int argc, char ** argv) {
    if(argc<2){
        printf("Missing port number !\n");
        exit(EXIT_FAILURE);
    }

    //Creation de la socket
    int sock_server = socket(PF_INET, SOCK_STREAM, 0);
    if(sock_server==-1){
        perror("socket");
        exit(EXIT_FAILURE);
    }

    struct sockaddr_in addr_sock = {
            .sin_family = AF_INET,
            .sin_port = htons(atoi(argv[1])),
            .sin_addr.s_addr = htonl(INADDR_ANY)
    };

    //Bind socket
    int r = bind(sock_server, (struct sockaddr *)&addr_sock, sizeof(addr_sock));
    if(r==-1){
        perror("bind");
        exit(EXIT_FAILURE);
    }

    //
    r = listen(sock_server, 0);
    if(r!=0){
        perror("listen");
        exit(EXIT_FAILURE);
    }

    //Test for message GAMES
    //TODO: Delete
    /*games_not_started.game_list = malloc(sizeof(game));
    games_not_started.game_list[0] = malloc(sizeof(game));
    games_not_started.game_list[0]->nb_players = 1;
    games_not_started.game_list[0]->id_game = 1;
    games_not_started.game_list[0]->laby = malloc(sizeof(maze));
    games_not_started.game_list[0]->laby->lenY = 10;
    games_not_started.game_list[0]->laby->lenX = 10;
    getAMaze(games_not_started.game_list[0]->laby);
    games_not_started.len = 1;*/

    //Boucle principale du serveur
    while(1){
        player * p = malloc(sizeof(player));
        struct sockaddr_in c;
        socklen_t size = sizeof(c);
        int sock2 = accept(sock_server, (struct sockaddr *)&c, &size);
        if(sock2<0){
            perror("accept");
            exit(EXIT_FAILURE);
        }
        p->sock = sock2;
        pthread_t th;
        pthread_create(&th,NULL,listen_player,p);
    }
    close(sock_server);
    return 0;
}

void* listen_player(void* args){
    player * p = (player *) args;
    memset(p->name,0,8);

    int r = recv(p->sock, p->name, 8, MSG_NOSIGNAL);
    if(r==-1){
        perror("recv");
        close(p->sock);
        return NULL;
    }else if(r==0){
        printf("Error");
        close(p->sock);
        return NULL;
    }

    if(sendGames(p->sock) == -1){
        printf("Error in sendGames\nConnexion with %s is stopped !", p->name);
        close(p->sock);
        return NULL;
    }


    char * message = malloc(256);
    while(1){
        memset(message, 0, 256);
        int len = recv(p->sock, message, 256, MSG_NOSIGNAL);
        if(len==-1){
            perror("recv");
            break;
        }else if(len<5){
            printf("Error");
            break;
        }

        char * action = malloc(6);
        memcpy(action, message, 5);
        action[5] = '\0';

        if(strncmp(action, "GAME?", 5) == 0 && len==8){
            sendGames(p->sock);
        }
        else{
            sendDunno(p->sock);
        }

        free(action);
    }
    free(message);
    close(p->sock);
    free(p);


    return 0;
}
