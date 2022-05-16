//Char - Chemin
#define CHARPATH 'O'

//Char - Murs
#define CHARWALL 'X'

//Structure - Labyrinthe
typedef struct maze{
    char ** maze;
    int lenX;
    int lenY;
    int ** ghosts;
    //TODO: Liste avec position ds joueurs ici ?
} maze;

//Remplissage d'une structure maze avec un labyrinthe
void getAMaze(maze * laby);

//Génère un labyrinthe
void generatorMaze(char** maze, char** visited, int sX, int sY, int lenX, int lenY);

//Affiche un labyrinthe
void printMaze(char ** maze, int lenX, int lenY);

void initGhosts(maze * laby, int nb_ghosts);
