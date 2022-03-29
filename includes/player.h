typedef struct player{
    char name[6];
    int port_udp;
} player;

typedef struct player_array{
    player p;
    struct player_array * next;
} p_array;