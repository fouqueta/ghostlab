#include "../includes/server.h"

void init_game_list(){
    pthread_mutex_lock(&(verrou_main));
    game_list = malloc(sizeof(game) * 30);
    for(int i=0;i<NB_GAMES;i++){
        game_list[i] = malloc(sizeof(game));
        game_list[i]->state_game = 0;
        game_list[i]->id_game = i;
        game_list[i]->nb_players = 0;
        game_list[i]->nb_ready = 0;
        game_list[i]->nb_ghosts = 0;
        game_list[i]->ip = "225.26.29.8\0";
        game_list[i]->verrou_server = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;

        game_list[i]->verrou_for_cond = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
        game_list[i]->cond = (pthread_cond_t)PTHREAD_COND_INITIALIZER;
        game_list[i]->last_update = 0;
    }
    pthread_mutex_unlock(&(verrou_main));
}

int getNbNotStarted(){
    int i = 0;
    for(int j=0;j<NB_GAMES;j++){
        pthread_mutex_lock(&(game_list[j]->verrou_server));
        if(game_list[j]->state_game==1){
            i++;
        }
        pthread_mutex_unlock(&(game_list[j]->verrou_server));
    }
    return i;
}

int8_t get_empty_game(){
    for(int8_t i = 0; i<NB_GAMES; i++){
        pthread_mutex_lock(&(game_list[i]->verrou_server));
        if(game_list[i]->state_game == 0){
            pthread_mutex_unlock(&(game_list[i]->verrou_server));
            return i;
        }
        pthread_mutex_unlock(&(game_list[i]->verrou_server));
    }
    return -1;
}

void init_a_game(int m){
    pthread_mutex_lock(&(game_list[m]->verrou_server));
    game_list[m]->state_game = 1;
    game_list[m]->laby = malloc(sizeof(maze));
    game_list[m]->laby->lenX = X_DEFAULT;
    game_list[m]->laby->lenY = Y_DEFAULT;
    game_list[m]->nb_ghosts = NB_GHOSTS_DEFAULT;
    pthread_mutex_unlock(&(game_list[m]->verrou_server));
}

int add_player_game(player * player_infos, int m){
    pthread_mutex_lock(&(game_list[m]->verrou_server));
    game_list[m]->list.first = add_player(game_list[m]->list.first, player_infos);

    if(!(in_list(game_list[m]->list.first, player_infos))) {
        pthread_mutex_unlock(&(game_list[m]->verrou_server));
        return -1;
    }

    game_list[m]->nb_players++;
    pthread_mutex_unlock(&(game_list[m]->verrou_server));

    pthread_mutex_lock(&(player_infos->verrou_player));
    player_infos->g = game_list[m];
    pthread_mutex_unlock(&(player_infos->verrou_player));
    return 0;
}

void remove_player_game(player * player_infos, int m){
    pthread_mutex_lock(&(game_list[m]->verrou_server));
    game_list[m]->list.first = remove_player(game_list[m]->list.first, player_infos);
    game_list[m]->nb_players--;
    game_list[m]->nb_ready--;
    if(game_list[m]->nb_players == 0 && (game_list[m]->state_game == 2 || game_list[m]->state_game == 3)){
        game_list[m]->state_game = 0;
        for(int i=0;i<game_list[m]->laby->lenX;i++){
            free(game_list[m]->laby->maze[i]);
        }
        free(game_list[m]->laby->maze);

        for(int i=0;i<game_list[m]->nb_ghosts;i++){
            free(game_list[m]->laby->ghosts[i]);
        }
        free(game_list[m]->laby->ghosts);
        free(game_list[m]->laby);
        game_list[m]->nb_ghosts = 0;

    }else if(game_list[m]->nb_players == 0 && game_list[m]->state_game == 1){
        game_list[m]->state_game = 0;
        game_list[m]->nb_ghosts = 0;
    }
    pthread_mutex_unlock(&(game_list[m]->verrou_server));
    free(player_infos);
}

void* gameFunc(void* args){
    game * g = (game *)args;
    srand(time(NULL));
    sleep(10);
    pthread_mutex_lock(&(g->verrou_server));
    int state = g->state_game;
    int nb_player = g->nb_players;
    int nb_ghosts = g->nb_ghosts;
    int id_game = g->id_game;
    pthread_mutex_unlock(&(g->verrou_server));
    while(state == 2 && nb_player > 0 && nb_ghosts > 0){
        int i = rand() % g->nb_ghosts;
        int bool = 1;
        while(bool){
            pthread_mutex_lock(&(g->verrou_server));
            int d = rand() % 4;
            int c;
            int x;
            int y;
            switch (d) {
                case 0:
                    c = (rand() % (int)(g->laby->lenY)*0.10) + 1;
                    x = g->laby->ghosts[i][0];
                    y = g->laby->ghosts[i][1]-c;

                    if(y>-1 && g->laby->maze[x][y] == CHARPATH){
                        printf("Fantome %d est maintenant en x: %d y: %d\n", i, x, y);
                        g->laby->ghosts[i][1] = y;
                        bool = 0;
                        if(sendGhost(g, x, y) == -1){
                            break;
                        }
                    }
                    break;
                case 1:
                    c = (rand() % (int)(g->laby->lenY)*0.10) + 1;
                    x = g->laby->ghosts[i][0];
                    y = g->laby->ghosts[i][1]+c;
                    if(y < g->laby->lenY && g->laby->maze[x][y] == CHARPATH ){
                        printf("Fantome %d est maintenant en x: %d y: %d\n", i, x, y);
                        g->laby->ghosts[i][1] = y;
                        bool = 0;
                        if(sendGhost(g, x, y) == -1){
                            break;
                        }
                    }
                    break;
                case 2:
                    c = (rand() % (int)(g->laby->lenX)*0.10) + 1;
                    x = g->laby->ghosts[i][0]-c;
                    y = g->laby->ghosts[i][1];
                    if(x>-1 && g->laby->maze[x][y] == CHARPATH ){
                        printf("Fantome %d est maintenant en x: %d y: %d\n", i, x, y);
                        g->laby->ghosts[i][0] = x;
                        bool = 0;
                        if(sendGhost(g, x, y) == -1){
                            break;
                        }
                    }
                    break;
                case 3:
                    c = (rand() % (int)(g->laby->lenX)*0.10) + 1;
                    x = g->laby->ghosts[i][0]+c;
                    y = g->laby->ghosts[i][1];
                    if(x < g->laby->lenX && g->laby->maze[x][y] == CHARPATH ){
                        printf("Fantome %d est maintenant en x: %d y: %d\n", i, x, y);
                        g->laby->ghosts[i][0] = x;
                        bool = 0;
                        if(sendGhost(g, x, y) == -1){
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }
            pthread_mutex_unlock(&(g->verrou_server));
        }
        sleep(10);
        pthread_mutex_lock(&(g->verrou_server));
        state = game_list[id_game]->state_game;
        nb_player = game_list[id_game]->nb_players;
        nb_ghosts = game_list[id_game]->nb_ghosts;
        pthread_mutex_unlock(&(g->verrou_server));
    }

    return 0;
}

void placePlayers(game * g){
    player_node * node = g->list.first;
    srand(time(NULL));
    for(int i=0; i<g->nb_players;i++){
        pthread_mutex_lock(&(node->p->verrou_player));
        while(1){
            int x = rand() % g->laby->lenX;
            int y = rand() % g->laby->lenY;
            if(g->laby->maze[x][y] == CHARPATH){
                node->p->x = x;
                node->p->y = y;
                break;
            }
        }
        pthread_mutex_unlock(&(node->p->verrou_player));
        node = node->next;
    }
}

void set_port(game * g){
    while(1){
        for(int i=1024;i<10000;i++){
            char port[128];
            memset(port, 0, 128);
            snprintf(port, 128, "%d", i);
            if(not_use(port)){
                memmove(g->port, port, 4);
                return;
            }
        }
    }
}

int not_use(char * port){
    for(int i=0;i<NB_GAMES;i++){
        if(game_list[i]->state_game == 2 && strncmp(game_list[i]->port, port, 4) == 0) {
            pthread_mutex_unlock(&(game_list[i]->verrou_server));
            return 0;
        }
    }
    return 1;
}

player *getWinner(game *g){
    player_node *first = g->list.first;
    int max = 0;
    player *win;
    uint8_t cpt = 0;
    while(cpt < g->nb_players){
        pthread_mutex_lock(&(first->p->verrou_player));
        srand(time(NULL));
        int rd;
        if(first->p->score > max){
            win = first->p;
            max = first->p->score;
        }
        else if(first->p->score == max && max != 0){
            rd = rand() % 2;
            if(rd == 1){
                win = first->p;
            }
        }
        pthread_mutex_unlock(&(first->p->verrou_player));
        first = first->next;
        cpt = cpt + 1;
    }
    return win;
}