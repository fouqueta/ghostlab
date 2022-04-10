#include "player.h"
#include "maze.h"

typedef struct game{
    uint8_t nb_players;
    player_array list;
    uint8_t id_game;

    maze * laby;
    //TODO: Fantômes (les places dans le maze ?)
} game;

typedef struct games{
    game ** game_list;
    uint8_t len;
}games;
