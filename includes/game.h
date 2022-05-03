#include "player.h"
#include "maze.h"

#define NB_GAMES 30

//Structure - Parties
typedef struct game{
    uint8_t nb_players;
    player_array list;
    uint8_t id_game;
    int is_start;
    //0 -> Libre
    //1 -> Not start
    //2 -> En cours
    int state_game;
    pthread_mutex_t verrou_server;
    maze * laby;
    //TODO: Fantômes (les places dans le maze ?)
} game;

void init_game_list();

int  getNbNotStarted();