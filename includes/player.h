typedef struct player{
    char name[6];
    int sock;

} player;

typedef struct player_node{
    player p;
    struct player_node * next;
} player_node;

typedef struct player_array{
    player_node * first;
} player_array;
