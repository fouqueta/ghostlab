#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <time.h>
#include <string.h>
#include <pthread.h>

#include "game.h"
#include "parser.h"

extern games games_started;
extern games games_not_started;

void* listen_player(void* args);