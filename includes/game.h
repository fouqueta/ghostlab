#include "player.h"

typedef struct game{
    int nb_players;
    p_array list;
    int id_game;

    char ** maze;
} game;