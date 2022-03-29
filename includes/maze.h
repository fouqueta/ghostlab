#define CHARPATH ' '
#define CHARWALL 'X'

void getAMaze(game * g, int lenX, int lenY);
void generatorMaze(char** maze, char** visited, int sX, int sY, int lenX, int lenY);
void printMaze(char ** maze, int lenX, int lenY);
