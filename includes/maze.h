//Char pour le chemin
#define CHARPATH ' '

//Char reprensant les murs
#define CHARWALL 'X'

//Structure reprensentant le labyrinthe
typedef struct maze{
    char ** maze;
    int lenX;
    int lenY;
    //TODO: mettre liste avec position ds joueurs ici ?
} maze;

//Repenlit le structure maze avec un labyrinthe
void getAMaze(maze * laby);

//Genere un labyrinthe
void generatorMaze(char** maze, char** visited, int sX, int sY, int lenX, int lenY);

//Affiche un labyrinthe
void printMaze(char ** maze, int lenX, int lenY);
