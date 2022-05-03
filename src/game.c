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