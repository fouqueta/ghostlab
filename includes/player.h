//Redéfinition - Correction d'includes récursifs
typedef struct game game;
struct game;

//Structure - Joueur
typedef struct player{
    char name[8];
    char port[4];
    game * g;

    int x;
    int y;
    int score;

    pthread_mutex_t verrou_player;

} player;

//Structure - Liste chainée de joueurs
typedef struct player_node{
    player * p;
    struct player_node * next;
} player_node;

typedef struct player_array{
    player_node * first;
} player_array;

//Regarde si deux joueurs sont égaux
int is_same_player(player * p1, player * p2);

//Regarde si le nom est déjà utilisé par un autre joueur
int name_taken(player_node * first, char name[8]);

//Regarde si un joueur est déjà dans la liste
int in_list(player_node * first, player * p);

//Retourne le nombre de joueurs
int len_list(player_node * first);

//Ajoute un joueur à la liste
player_node * add_player(player_node * first, player * p);

//Retire un joueur de la liste
player_node * remove_player(player_node * first, player * p);

//Retourne le n-éme joueur de la liste
player * get_n_player(player_node * first, int n);

player * init_player(char pseudo[8], char port[4]);

int move_player(player *p, int x, int y);
