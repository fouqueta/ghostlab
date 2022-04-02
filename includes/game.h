#include "player.h"
#include "maze.h"

typedef struct game{
    int nb_players;
    p_array list;
    int id_game;

    maze * laby;
    //TODO: Fantômes (les places dans le maze ?)
} game;
