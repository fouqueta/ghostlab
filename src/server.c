#include "../includes/server.h"

/*game games_started[];
game games_not_started[];*/

int main(void) {
    game * g = malloc(sizeof(game));
    getAMaze(g,20,50);
    printMaze(g->maze, 20,50);
}
