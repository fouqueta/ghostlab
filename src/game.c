#include "../includes/server.h"

void init_game_list(){
    game_list = malloc(sizeof(game) * 30);
    for(int i=0;i<NB_GAMES;i++){
        game_list[i] = malloc(sizeof(game));
        game_list[i]->state_game = 0;
        game_list[i]->id_game = i;
        game_list[i]->is_start = 0;
        game_list[i]->nb_players = 0;
        game_list[i]->verrou_server = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
    }

    //TODO: DELETE, juste pour le teste d'avoir une partie
    /*game_list[21]->state_game = 1;
    game_list[21]->nb_players = 1;
    game_list[21]->laby = malloc(sizeof(maze));
    game_list[21]->laby->lenX = 10;
    game_list[21]->laby->lenY = 20;
    printf("ici\n");
    player * p = malloc(sizeof(player));
    memmove(p->name,"12345678",8);
    p->verrou_player = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
    player_node * pn = malloc(sizeof(player_node));
    pn->p = p;
    player_array pa = {
            .first = pn
    };
    game_list[21]->list = pa;*/
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