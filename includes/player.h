typedef struct player{
    char name[8];
    int sock;

} player;

typedef struct player_node{
    player * p;
    struct player_node * next;
} player_node;

typedef struct player_array{
    player_node * first;
} player_array;

int is_same_player(player * p1, player * p2);

int name_taken(player_node * first, char name[8]);

int in_list(player_node * first, player * p);

player_node * add_player(player_node * first, player * p);

player_node * remove_player(player_node * first, player * p);

