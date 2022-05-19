//Char - Chemin
#define CHARPATH 'O'

//Char - Murs
#define CHARWALL 'X'

#define X_DEFAULT 10
#define Y_DEFAULT 10

//Structure - Labyrinthe
typedef struct maze{
    char ** maze;
    int lenX;
    int lenY;
    int ** ghosts;
} maze;

//Remplissage d'une structure maze avec un labyrinthe
void getAMaze(maze * laby);

//Genere un labyrinthe
void generatorMaze(char** maze, char** visited, int sX, int sY, int lenX, int lenY);

//Affiche un labyrinthe
void printMaze(char ** maze, int lenX, int lenY);

//Initialise les fantomes
void initGhosts(maze * laby, int nb_ghosts);

//Verifie si un fantome se trouve sur une position donnee.
//Renvoie 1 si un fantome s'y trouve, 0 sinon.
int checkGhost(maze * laby, int nb_ghosts, int x, int y);