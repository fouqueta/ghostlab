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
extern pthread_mutex_t verrou_main;

void* listen_player(void* args);