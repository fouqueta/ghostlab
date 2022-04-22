#include "player.h"
#include "maze.h"

//Structure - Parties
typedef struct game{
    uint8_t nb_players;
    player_array list;
    uint8_t id_game;
    int is_start;
    pthread_mutex_t verrou_server;
    maze * laby;
    //TODO: Fantômes (les places dans le maze ?)
} game;

//Structure - Ensemble de parties
typedef struct games{
    game ** game_list;
    uint8_t len;
} games;
