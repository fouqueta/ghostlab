#include "../includes/server.h"

void init_game_list(){
    game_list = malloc(sizeof(game) * 30);
    for(int i=0;i<NB_GAMES;i++){
        game_list[i] = malloc(sizeof(game));
        game_list[i]->state_game = 0;
        game_list[i]->id_game = i;
        game_list[i]->nb_players = 0;
        game_list[i]->nb_ready = 0;
        game_list[i]->nb_ghosts = 0;
        game_list[i]->verrou_server = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
        game_list[i]->cond = (pthread_cond_t)PTHREAD_COND_INITIALIZER;
    }

    //TODO: DELETE, juste pour le teste d'avoir une partie
    game_list[21]->state_game = 1;
    game_list[21]->nb_players = 1;
    game_list[21]->laby = malloc(sizeof(maze));
    game_list[21]->laby->lenX = 10;
    game_list[21]->laby->lenY = 20;
    game_list[21]->nb_ready = 1;
    game_list[21]->nb_ghosts = 10;
    player * p = malloc(sizeof(player));
    memmove(p->name,"12345678",8);
    p->verrou_player = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
    player_node * pn = malloc(sizeof(player_node));
    pn->p = p;
    player_array pa = {
            .first = pn
    };
    game_list[21]->list = pa;
}

int getNbNotStarted(){
    int i = 0;
    for(int j=0;j<NB_GAMES;j++){
        if(game_list[j]->state_game==1){
            i++;
        }
    }
    return i;
}

int8_t get_empty_game(){
    for(int8_t i = 0; i<NB_GAMES; i++){
        if(game_list[i]->state_game == 0)
            return i;
    }
    return -1;
}

void init_a_game(int m){
    game_list[m]->state_game = 1;
    game_list[m]->laby = malloc(sizeof(maze));
    game_list[m]->laby->lenX = 30;
    game_list[m]->laby->lenY = 30;

    /*getAMaze(game_list[m]->laby);*/
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
    if(game_list[m]->nb_players == 0){
        game_list[m]->state_game = 0;
        free(game_list[m]->laby);
    }
    pthread_mutex_unlock(&(game_list[m]->verrou_server));
    free(player_infos);
}

void* gameFunc(void* args){
    game * g = (game *)args;
    srand(time(NULL));
    pthread_mutex_lock(&(g->verrou_server));
    getAMaze(g->laby);
    initGhosts(g->laby, 10);
    pthread_mutex_unlock(&(g->verrou_server));
    while(g->state_game == 1){
        printf("fantomes\n");
        sleep(5);
        //TODO: Deplacer fantomes
        /*int i = rand() % g->nb_ghosts;
        while(1){

            pthread_mutex_lock(&(g->verrou_server));
            int d = rand() % 4;
            int c;
            int x;
            int y;
            switch (d) {
                case 0:
                    c = rand() % 10;
                    x = g->laby->ghosts[i][0];
                    y = g->laby->ghosts[i][1]-c;
                    printf("%d %d %d %d\n", d, c, x, y);
                    if(y>-1 && g->laby->maze[x][y] == CHARPATH ){
                        g->laby->ghosts[i][1] = y;
                    }
                    break;
                case 1:
                    c = rand() % (int)(g->laby->lenY)*0.10;
                    x = g->laby->ghosts[i][0];
                    y = g->laby->ghosts[i][1]+c;
                    printf("%d %d %d %d\n", d, c, x, y);
                    if(y < g->laby->lenY && g->laby->maze[x][y] == CHARPATH ){
                        g->laby->ghosts[i][1] = y;
                    }
                    break;
                case 2:
                    c = rand() % (int)(g->laby->lenX)*0.10;
                    x = g->laby->ghosts[i][0]-c;
                    y = g->laby->ghosts[i][1];
                    printf("%d %d %d %d\n", d, c, x, y);
                    if(x>-1 && g->laby->maze[x][y] == CHARPATH ){
                        g->laby->ghosts[i][0] = x;
                    }
                    break;
                case 3:
                    c = rand() % (int)(g->laby->lenX)*0.10;
                    x = g->laby->ghosts[i][0]+c;
                    y = g->laby->ghosts[i][1];
                    printf("%d %d %d %d\n", d, c, x, y);
                    if(x < g->laby->lenX && g->laby->maze[x][y] == CHARPATH ){
                        g->laby->ghosts[i][0] = x;
                    }
                    break;
            }
            pthread_mutex_unlock(&(g->verrou_server));*/
        }

    }

    return 0;
}