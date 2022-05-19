#include "../includes/server.h"
#include <stdint.h>

int sendGames(int fd){
    pthread_mutex_lock(&verrou_main);
    uint8_t n = getNbNotStarted();

    char * games = "GAMES \0";
    char * stars = "***\0";

    char * mess_games = malloc(sizeof(uint8_t) + strlen(games) + strlen(stars));

    int len = 0;

    memmove(mess_games+len, games, strlen(games)); len += strlen(games);
    memmove(mess_games+len, &n, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(mess_games+len, stars, strlen(stars)); len += strlen(stars);
    int r = send(fd, mess_games, len, 0);
    free(mess_games);
    if(r==-1){
        pthread_mutex_unlock(&verrou_main);
        perror("send");
        return -1;
    }


    char * ogame = "OGAME \0";
    for(int i=0;i<NB_GAMES;i++){
        if(game_list[i]->state_game == 1){
            pthread_mutex_lock(&(game_list[i]->verrou_server));
            char * message = malloc(strlen(ogame) + sizeof(uint8_t)*2 + 1 + sizeof(stars));
            uint8_t idgame = game_list[i]->id_game;
            uint8_t nbJoueurs = game_list[i]->nb_players;
            pthread_mutex_unlock(&(game_list[i]->verrou_server));
            len = 0;
            memmove(message+len, ogame, strlen(ogame)); len += strlen(ogame);
            memmove(message+len, &idgame, sizeof(idgame)); len += sizeof(idgame);
            memmove(message+len, " ", 1); len += 1;
            memmove(message+len, &nbJoueurs, sizeof(nbJoueurs)); len += sizeof(nbJoueurs);
            memmove(message+len, stars, strlen(stars)); len += strlen(stars);

            r = send(fd, message, len, 0);
            free(message);
            if(r==-1){
                pthread_mutex_unlock(&verrou_main);
                perror("send");
                return -1;
            }
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
        pthread_mutex_unlock(&g->verrou_server);
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
        pthread_mutex_lock(&(p->verrou_player));
        char id[8]; memset(id,0,8); memmove(id, p->name, 8);

        char x[4];
        if(p->x>99){
            snprintf(x, 128, "%d", p->x);
        }else if(p->x>9){
            snprintf(x, 128, "0%d", p->x);
        }else{
            snprintf(x, 128, "00%d", p->x);
        }
        x[3] = '\0';

        char y[4];
        if(p->y>99){
            snprintf(y, 128, "%d", p->y);
        }else if(p->y>9){
            snprintf(y, 128, "0%d", p->y);
        }else{
            snprintf(y, 128, "00%d", p->y);
        }
        y[3] = '\0';

        char s[5];
        if(p->score>999) {
            snprintf(s, 128, "%d", p->score);
        }else if(p->score>99){
            snprintf(s, 128, "0%d", p->score);
        }else if(p->x>9){
            snprintf(s, 128, "00%d", p->score);
        }else{
            snprintf(s, 128, "000%d", p->score);
        }
        s[4] = '\0';
        pthread_mutex_unlock(&(p->verrou_player));
        int len = strlen(stars) + len_gplyr + 8 + strlen(s) + strlen(y) + strlen(x) + 3;

        char * message = malloc(len);
        memset(message, 0, len);
        len = 0;

        memmove(message+len, gplyr, len_gplyr); len += len_gplyr;
        memmove(message+len, id, 8); len += 8;
        memmove(message+len, " ", 1); len += 1;
        memmove(message+len, x, strlen(x)); len += strlen(x);
        memmove(message+len, " ", 1); len += 1;
        memmove(message+len, y, strlen(y)); len += strlen(y);
        memmove(message+len, " ", 1); len += 1;
        memmove(message+len, s, strlen(s)); len += strlen(s);
        memmove(message+len, stars, strlen(stars)); len += strlen(stars);

        r = send(fd, message, len, 0);
        free(message);
        if(r==-1){
            pthread_mutex_unlock(&g->verrou_server);
            return -1;
        }
    }

    pthread_mutex_unlock(&g->verrou_server);
    return 0;
}

int sendSize(int fd, game * g){
    char * size = "SIZE! \0";
    char * stars = "***\0";
    pthread_mutex_lock(&(g->verrou_server));
    printf("ici\n");
    uint8_t m = g->id_game;
    uint16_t h = g->laby->lenX;
    uint16_t w = g->laby->lenY;
    printf("ici\n");
    pthread_mutex_unlock(&(g->verrou_server));

    h = htole16(h);
    w = htole16(w);

    int len = strlen(size) + strlen(stars) + sizeof(uint16_t)*2 + sizeof(uint8_t) + 2;
    char * message = malloc(len);
    memset(message, 0, len);
    len = 0;
    memmove(message+len, size, strlen(size)); len += strlen(size);
    memmove(message+len, &m, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(message+len, " ", 1); len += 1;
    memmove(message+len, &h, sizeof(uint16_t)); len += sizeof(uint16_t);
    memmove(message+len, " ", 1); len += 1;
    memmove(message+len, &w, sizeof(uint16_t)); len += sizeof(uint16_t);
    memmove(message+len, stars, strlen(stars)); len += strlen(stars);

    int r = send(fd, message, len, 0);
    free(message);
    if(r==-1){
        return -1;
    }
    return 0;

}

int sendList(int fd, game * g){
    char * list = "LIST! \0";
    char * stars = "***\0";
    char * playr = "PLAYR \0";
    pthread_mutex_lock(&(g->verrou_server));

    uint8_t m = g->id_game;
    uint8_t s = g->nb_players;

    int len = strlen(list) + strlen(stars) + sizeof(uint8_t)*2 + 1;

    char * message = malloc(len);
    memset(message, 0, len);
    len = 0;
    memmove(message+len, list, strlen(list)); len += strlen(list);
    memmove(message+len, &m, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(message+len, " ", 1); len += 1;
    memmove(message+len, &s, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(message+len, stars, strlen(stars)); len += strlen(stars);


    int r = send(fd, message, len, 0);
    free(message);
    if(r==-1){
        pthread_mutex_unlock(&(g->verrou_server));
        return -1;
    }

    len = strlen(playr) + strlen(stars) + 8;

    for(int i=0;i<s;i++){
        char * message_player = malloc(len);
        int len_player = 0;
        player * p = get_n_player(g->list.first ,i);

        pthread_mutex_lock(&(p->verrou_player));
        memmove(message_player+len_player, playr, strlen(playr)); len_player += strlen(playr);
        memmove(message_player+len_player, p->name, 8); len_player += 8;
        memmove(message_player+len_player, stars, strlen(stars)); len_player += strlen(stars);
        pthread_mutex_unlock(&(p->verrou_player));

        int r = send(fd, message_player, len_player, 0);
        free(message_player);
        if(r==-1){
            pthread_mutex_unlock(&(g->verrou_server));
            return -1;
        }
    }

    pthread_mutex_unlock(&(g->verrou_server));
    return 0;
}

int sendRegno(int fd){
    char * message = "REGNO***\0";
    if(send(fd, message, strlen(message), 0) == -1){
        return -1;
    }
    return 0;
}

int sendRegok(int fd, int8_t m){
    char * regok = "REGOK \0";
    char * stars = "***\0";
    char * message = malloc(strlen(regok) + 3 + sizeof(int8_t));
    int len = 0;
    memmove(message+len, regok, strlen(regok)); len += strlen(regok);
    memmove(message+len, &m, sizeof(int8_t)); len += sizeof(int8_t);
    memmove(message+len, stars, strlen(stars)); len += strlen(stars);

    int r = send(fd, message, len, 0);
    free(message);
    if(r==-1){
        return -1;
    }
    return 0;
}

int sendUnrok(int fd, int8_t m){
    char * unrok = "UNROK \0";
    char * stars = "***\0";
    char * message = malloc(strlen(unrok) + 3 + sizeof(int8_t));
    int len = 0;
    memmove(message+len, unrok, strlen(unrok)); len += strlen(unrok);
    memmove(message+len, &m, sizeof(int8_t)); len += sizeof(int8_t);
    memmove(message+len, stars, strlen(stars)); len += strlen(stars);

    int r = send(fd, message, len, 0);
    free(message);
    if(r==-1){
        return -1;
    }
    return 0;
}

int sendStart(int fd, player * p){
    char * welco = "WELCO \0";
    int len_welco = strlen(welco);
    char * posit = "POSIT \0";
    int len_posit = strlen(posit);
    char * stars = "***\0";
    int len_stars = strlen(stars);

    char * ip = malloc(15);
    memset(ip, '#', 15);
    memmove(ip, p->g->ip, strlen(p->g->ip));

    int len = len_welco + sizeof(uint8_t)*2 + sizeof(uint16_t)*2 + 15 + 4 + len_stars + 5;
    char * buffer = malloc(len);
    memset(buffer, 0, len);
    len = 0;

    memmove(buffer + len, welco, len_welco); len += len_welco;
    memmove(buffer + len, &p->g->id_game, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(buffer + len, " ", 1); len += 1;
    memmove(buffer + len, &p->g->laby->lenX, sizeof(uint16_t)); len += sizeof(uint16_t);
    memmove(buffer + len, " ", 1); len += 1;
    memmove(buffer + len, &p->g->laby->lenY, sizeof(uint16_t)); len += sizeof(uint16_t);
    memmove(buffer + len, " ", 1); len += 1;
    memmove(buffer + len, &p->g->nb_ghosts, sizeof(uint8_t)); len += sizeof(uint8_t);
    memmove(buffer + len, " ", 1); len += 1;
    memmove(buffer + len, ip, 15); len += 15;
    memmove(buffer + len, " ", 1); len += 1;
    memmove(buffer + len, p->g->port, 4); len += 4;
    memmove(buffer + len, stars, len_stars); len += len_stars;

    int r = send(fd, buffer, len, 0);
    free(buffer);
    if(r == -1){
        return -1;
    }

    char x[4];
    if(p->x>99){
        snprintf(x, 128, "%d", p->x);
    }else if(p->x>9){
        snprintf(x, 128, "0%d", p->x);
    }else{
        snprintf(x, 128, "00%d", p->x);
    }
    x[3] = '\0';

    char y[4];
    if(p->y>99){
        snprintf(y, 128, "%d", p->y);
    }else if(p->y>9){
        snprintf(y, 128, "0%d", p->y);
    }else{
        snprintf(y, 128, "00%d", p->y);
    }
    y[3] = '\0';

    len = len_posit + len_stars + 8 + 3*2 + 2;
    char * message = malloc(len);
    len = 0;

    memmove(message + len, posit, len_posit); len += len_posit;
    memmove(message + len, p->name, 8); len += 8;
    memmove(message + len, " ", 1); len += 1;
    memmove(message + len, x, 3); len += 3;
    memmove(message + len, " ", 1); len += 1;
    memmove(message + len, y, 3); len += 3;
    memmove(message + len, stars, len_stars); len += len_stars;

    r = send(fd, message, len, 0);
    free(message);
    if(r == -1){
        return -1;
    }
    return 0;
}

int sendMove(int fd, player *p, int ghost){
    int count;
    if(ghost > 0){
        char *movef = "MOVEF \0";
        int len_movef = strlen(movef);
        char *stars = "***\0";
        int len_stars = strlen(stars);

        int len = len_movef + len_stars + 3*2 + 3 + 4;
        char *message = malloc(len);
        len = 0;
        pthread_mutex_lock(&(p->verrou_player));
        char x[4];
        if(p->x>99){
            snprintf(x, 128, "%d", p->x);
        }else if(p->x>9){
            snprintf(x, 128, "0%d", p->x);
        }else{
            snprintf(x, 128, "00%d", p->x);
        }
        x[3] = '\0';

        char y[4];
        if(p->y>99){
            snprintf(y, 128, "%d", p->y);
        }else if(p->y>9){
            snprintf(y, 128, "0%d", p->y);
        }else{
            snprintf(y, 128, "00%d", p->y);
        }
        y[3] = '\0';

        char sc[5];
        p->score = p->score + ghost;
        if(p->score > 999){
            snprintf(sc, 128, "%d", p->score);
        }
        else if(p->score > 99){
            snprintf(sc, 128, "0%d", p->score);
        }
        else if(p->score > 9){
            snprintf(sc, 128, "00%d", p->score);
        }
        else{
            snprintf(sc, 128, "000%d", p->score);
        }
        sc[4] = '\0';
        pthread_mutex_unlock(&(p->verrou_player));

        memmove(message + len, movef, len_movef); len += len_movef;
        memmove(message + len, x, 3); len += 3;
        memmove(message + len, " ", 1); len += 1;
        memmove(message + len, y, 3); len += 3;
        memmove(message + len, " ", 1); len += 1;
        memmove(message + len, sc, 4); len += 4;
        memmove(message + len, stars, len_stars); len += len_stars;

        count = send(fd, message, len, 0);
        free(message);
        if(count == -1){
            return -1;
        }
    }
    else{
        char *move = "MOVE! \0";
        int len_move = strlen(move);
        char *stars = "***\0";
        int len_stars = strlen(stars);

        int len = len_move + len_stars + 3*2 + 1;
        char *message = malloc(len);
        len = 0;

        pthread_mutex_lock(&(p->verrou_player));
        char x[4];
        if(p->x>99){
            snprintf(x, 128, "%d", p->x);
        }else if(p->x>9){
            snprintf(x, 128, "0%d", p->x);
        }else{
            snprintf(x, 128, "00%d", p->x);
        }
        x[3] = '\0';

        char y[4];
        if(p->y>99){
            snprintf(y, 128, "%d", p->y);
        }else if(p->y>9){
            snprintf(y, 128, "0%d", p->y);
        }else{
            snprintf(y, 128, "00%d", p->y);
        }
        y[3] = '\0';
        pthread_mutex_unlock(&(p->verrou_player));

        memmove(message + len, move, len_move); len += len_move;
        memmove(message + len, x, 3); len += 3;
        memmove(message + len, " ", 1); len += 1;
        memmove(message + len, y, 3); len += 3;
        memmove(message + len, stars, len_stars); len += len_stars;

        count = send(fd, message, len, 0);
        free(message);
        if(count == -1){
            return -1;
        }
    }
    return 0;
}

int sendQuit(int fd){
    char *bye = "GOBYE***\0";
    int len_bye = strlen(bye);
    int count = send(fd, bye, len_bye, 0);
    if(count == -1){
        return -1;
    }
    return 0;
}