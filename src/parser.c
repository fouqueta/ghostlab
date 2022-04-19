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

int sendGList(int fd, game * g){
    pthread_mutex_lock(&g->verrou_server);
    uint8_t n = g->nb_players;

    char *glis = "GLIS! ";
    char * stars = "***\0";

    char * mess_list = malloc(sizeof(uint8_t) + strlen(glis) + strlen(stars));
    memset(mess_list, 0, sizeof(uint8_t) + strlen(glis) + strlen(stars));
    int len = 0;

    memmove(mess_list+len, glis, strlen(glis)); len += strlen(glis);
    memmove(mess_list+len, &n, sizeof(n)); len += sizeof(n);
    memmove(mess_list+len, stars, strlen(stars)); len += strlen(stars);
    int r = send(fd, mess_list, len, 0); free(mess_list);
    if(r==-1){
        free(mess_list);
        return -1;
    }

    char * gplyr = "GPLYR \0";
    int len_gplyr = strlen(gplyr);
    for(int i=0;i<n;i++){
        player * p = get_n_player(g->list.first ,i);
        if(p==NULL){
            return -1;
        }
        char id[8]; memset(id,0,8); memmove(id, p->name, 8);

        char x[128];
        if(p->x<100){
            snprintf(x, 128, " %d ", p->x);
        }else if(p->x<10){
            snprintf(x, 128, " 0%d ", p->x);
        }else{
            snprintf(x, 128, " 00%d ", p->x);
        }
        x[5] = '\0';

        char y[128];
        if(p->y<100){
            snprintf(y, 128, "%d ", p->y);
        }else if(p->y<10){
            snprintf(y, 128, "0%d ", p->y);
        }else{
            snprintf(y, 128, "00%d ", p->y);
        }
        y[4] = '\0';

        char s[128];
        if(p->score<1000) {
            snprintf(s, 128, "%d", p->score);
        }else if(p->score<100){
            snprintf(s, 128, "0%d", p->score);
        }else if(p->x<10){
            snprintf(s, 128, "00%d", p->score);
        }else{
            snprintf(s, 128, "000%d", p->score);
        }
        s[4] = '\0';

        int len = 3 + len_gplyr + 8 + strlen(s) + strlen(y) + strlen(x);

        char * message = malloc(len);
        memset(message, 0, len);
        len = 0;

        memmove(message+len, gplyr, len_gplyr); len += len_gplyr;
        memmove(message+len, x, strlen(x)); len += strlen(x);
        memmove(message+len, y, strlen(y)); len += strlen(y);
        memmove(message+len, s, strlen(s)); len += strlen(s);
        memmove(message+len, stars, strlen(stars)); len += strlen(stars);

        r = send(fd, message, len, 0);
        free(message);
        if(r==-1){
            return -1;
        }
    }

    pthread_mutex_unlock(&g->verrou_server);
    return 0;
}