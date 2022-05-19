#include "../includes/server.h"

void getAMaze(maze * laby){
    srand(time(NULL));
    int lenX = laby->lenX;
    int lenY = laby->lenY;
    laby->maze = malloc(lenX * sizeof(char *));
    char ** visited = malloc(lenX * sizeof(char *));
    for(int i=0;i<lenX;i++){
        laby->maze[i] = malloc(lenY);
        visited[i] = malloc(lenY);
        for(int j=0;j<lenY;j++){
            laby->maze[i][j] = CHARWALL;
            visited[i][j] = ' ';
        }
    }
    generatorMaze(laby->maze, visited, 0, 0, lenX, lenY);
    printMaze(laby->maze,lenX,lenY);
    //printMaze(visited,lenX,lenY);
    for(int i=0;i<lenX;i++){
        free(visited[i]);
    }
    free(visited);
}

void printMaze(char ** maze, int lenX, int lenY){
    for(int n = 0; n<lenX; n++)
    {
        for(int k = 0; k<lenY; k++)
        {
            printf("%c", maze[n][k]);
        }
        printf("%c", '\n');
    }
}

void updateVisited(char ** maze, char ** visited, int sX, int sY, int lenX, int lenY){
    if (sX + 1 < lenX && maze[sX+1][sY] != CHARPATH) {
        int tempX = sX + 1;
        if (tempX + 1 < lenX && visited[tempX + 1][sY] == 'v')
            visited[tempX][sY] = '#';
        else if (sY > 0 && visited[tempX][sY - 1] == 'v')
            visited[tempX][sY] = '#';
        else if (sY + 1 < lenY && visited[tempX][sY + 1] == 'v')
            visited[tempX][sY] = '#';
    }
    if (sX > 0 && maze[sX-1][sY] != CHARPATH) {
        int tempX = sX - 1;
        if (tempX > 0 && visited[tempX - 1][sY] == 'v')
            visited[tempX][sY] = '#';
        else if (sY > 0 && visited[tempX][sY - 1] == 'v')
            visited[tempX][sY] = '#';
        else if (sY + 1 < lenY && visited[tempX][sY + 1] == 'v')
            visited[tempX][sY] = '#';
    }
    if (sY + 1 < lenY && maze[sX][sY+1] != CHARPATH) {
        int tempY = sY + 1;
        if (tempY + 1 < lenY && visited[sX][tempY + 1] == 'v')
            visited[sX][tempY] = '#';
        else if (sX > 0 && visited[sX - 1][tempY] == 'v')
            visited[sX][tempY] = '#';
        else if (sX + 1 < lenX && visited[sX + 1][tempY] == 'v')
            visited[sX][tempY] = '#';
    }
    if (sY > 0 && maze[sX][sY-1] != CHARPATH) {
        int tempY = sY - 1;
        if (tempY > 0 && visited[sX][tempY - 1] == 'v')
            visited[sX][tempY] = '#';
        else if (sX > 0 && visited[sX - 1][tempY] == 'v')
            visited[sX][tempY] = '#';
        else if (sX + 1 < lenX && visited[sX + 1][tempY] == 'v')
            visited[sX][tempY] = '#';
    }
}

void generatorMaze(char** maze, char** visited, int sX, int sY, int lenX, int lenY){
    while((sY>0 && visited[sX][sY-1]== ' ') ||
            (sX>0 && visited[sX-1][sY]== ' ') ||
            (sY<lenY-1 && visited[sX][sY+1]== ' ') ||
            (sX<lenX-1 && visited[sX+1][sY]== ' ')){

        //usleep(1000*500);
        int r = rand()%4;
        /*printf("%d,%d, %d\n", sX, sY, r);
        printMaze(maze, lenX, lenY);
        printMaze(visited, lenX, lenY);*/
        if(r==0){
            if(sX+1<lenX && visited[sX+1][sY] == ' '){
                sX++;
                maze[sX][sY] = CHARPATH;
                visited[sX][sY] = 'v';
                updateVisited(maze,visited,sX,sY,lenX,lenY);
                generatorMaze(maze,visited,sX,sY,lenX,lenY);
                sX--;
            }
        }else if(r==1){
            if(sX>0 && visited[sX-1][sY] == ' '){
                sX--;
                maze[sX][sY] = CHARPATH;
                visited[sX][sY] = 'v';
                updateVisited(maze,visited,sX,sY,lenX,lenY);
                generatorMaze(maze,visited,sX,sY,lenX,lenY);
                sX++;
            }
        }else if(r==2){
            if(sY+1<lenY && visited[sX][sY+1] == ' '){
                sY++;
                maze[sX][sY] = CHARPATH;
                visited[sX][sY] = 'v';
                updateVisited(maze,visited,sX,sY,lenX,lenY);
                generatorMaze(maze,visited,sX,sY,lenX,lenY);
                sY--;
            }
        }else{
            if(sY>0 && visited[sX][sY-1] == ' ') {
                sY--;
                maze[sX][sY] = CHARPATH;
                visited[sX][sY] = 'v';
                updateVisited(maze,visited,sX,sY,lenX,lenY);
                generatorMaze(maze, visited, sX, sY, lenX, lenY);
                sY++;
            }

        }

    }
}

void initGhosts(maze * laby, int nb_ghosts){
    srand(time(NULL));
    laby->ghosts = malloc(sizeof(int *)*nb_ghosts);
    for(int i=0;i<nb_ghosts;i++){
        int x = 0;
        int y = 0;
        while(1){
            x = rand() % laby->lenX;
            y = rand() % laby->lenY;

            if(laby->maze[x][y] == CHARPATH)
                break;
        }
        laby->ghosts[i] = malloc(2* sizeof(int));
        laby->ghosts[i][0] = x;
        laby->ghosts[i][1] = y;
        printf("Fantome %d x: %d y: %d\n", i, x, y);
    }
}

int checkGhost(maze *laby, int nb_ghosts, int x, int y){
    for(int i=0;i<nb_ghosts;i++){
        if(laby->ghosts[i][0] == x && laby->ghosts[i][1] == y){
            int ** ghosts = malloc(sizeof(int *)*(nb_ghosts-1));
            int k = 0;
            for(int j=0;j<nb_ghosts;j++){
                if(j!=i){
                    ghosts[k] = malloc(2* sizeof(int));
                    ghosts[k][0] = laby->ghosts[j][0];
                    ghosts[k][1] = laby->ghosts[j][1];
                    free(laby->ghosts[j]);
                    k++;
                }
            }
            free(laby->ghosts);
            laby->ghosts = ghosts;
            return 1;
        }
    }
    return 0;
}