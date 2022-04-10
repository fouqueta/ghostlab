#include "../includes/server.h"

int sendGames(int fd){
    pthread_mutex_lock(&verrou_main);
    uint8_t n = games_not_started.len;

    char * games = "GAMES \0";
    char * stars = "***\0";

    char * mess_games = malloc(sizeof(uint8_t) + strlen(games) + strlen(stars));

    int len = 0;

    memmove(mess_games+len, games, strlen(games)); len += strlen(games);
    memmove(mess_games+len, &n, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(mess_games+len, stars, strlen(stars)); len += strlen(stars);
    int r = send(fd, mess_games, len, 0);
    if(r==-1){
        perror("send");
        return -1;
    }
    free(mess_games);

    char * ogame = "OGAME \0";
    for(int i=0;i<n;i++){
        char * message = malloc(strlen(ogame) + sizeof(uint8_t)*2 + 1 + sizeof(stars));
        uint8_t idgame = games_not_started.game_list[i]->id_game;
        uint8_t nbJoueurs = games_not_started.game_list[i]->nb_players;

        len = 0;
        memmove(message+len, ogame, strlen(ogame)); len += strlen(ogame);
        memmove(message+len, &idgame, sizeof(idgame)); len += sizeof(idgame);
        memmove(message+len, " ", strlen(" ")); len += strlen(" ");
        memmove(message+len, &nbJoueurs, sizeof(nbJoueurs)); len += sizeof(nbJoueurs);
        memmove(message+len, stars, strlen(stars)); len += strlen(stars);

        r = send(fd, message, len, 0);
        if(r==-1){
            perror("send");
            return -1;
        }
    }
    pthread_mutex_unlock(&verrou_main);
    return 0;
}

int sendDunno(int fd){
    char * message = "DUNNO***\0";
    if(send(fd, message, strlen(message), 0) == -1){
        return -1;
    }
    return 0;
}