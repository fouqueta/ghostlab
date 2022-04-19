//Redefinition pour eviter des problèmes avec les includes recursifs !
typedef struct game game;
struct game;

//Structure reprensentant un joueur
typedef struct player{
    char name[8];
    int port;
    struct game * g;

} player;

//List chainéé de joeurs
typedef struct player_node{
    player * p;
    struct player_node * next;
} player_node;

typedef struct player_array{
    player_node * first;
} player_array;

//regarde si deux joueurs sont égaux
int is_same_player(player * p1, player * p2);

//Regarde si le nom est déjà utilisé par quelqu'un de la liste
int name_taken(player_node * first, char name[8]);

//Regarde si un joueur est déjà dans la liste
int in_list(player_node * first, player * p);

//Rajoute un joueur à la liste
player_node * add_player(player_node * first, player * p);

//Retire un joueur de la liste
player_node * remove_player(player_node * first, player * p);

