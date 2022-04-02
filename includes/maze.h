#define CHARPATH ' '
#define CHARWALL 'X'

typedef struct maze{
    char ** maze;
    int lenX;
    int lenY;
    //TODO: mettre liste avec position ds joueurs ici ?
} maze;

void getAMaze(maze * laby);
void generatorMaze(char** maze, char** visited, int sX, int sY, int lenX, int lenY);
void printMaze(char ** maze, int lenX, int lenY);
