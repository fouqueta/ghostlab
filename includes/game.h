#include "player.h"
#include "maze.h"


#define NB_GAMES 30

//Structure - Parties
typedef struct game{
    uint8_t nb_players;
    player_array list;
    uint8_t id_game;
    //0 -> Libre
    //1 -> Non commencée
    //2 -> En cours
    //3 -> terminée, mais il y a encore des joueurs
    int state_game;
    int nb_ready;
    uint8_t nb_ghosts;
    pthread_mutex_t verrou_server;

    pthread_mutex_t verrou_for_cond;
    pthread_cond_t cond;

    maze * laby;
    char * ip;
    char port[4];
} game;

void init_game_list();

int getNbNotStarted();

int8_t get_empty_game();

void init_a_game(int m);

int add_player_game(player * player_infos, int m);

void remove_player_game(player * player_infos, int m);

void* gameFunc(void* args);

void placePlayers(game * g);

void set_port(game * g);

int not_use(char * port);
