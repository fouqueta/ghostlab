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

//TODO: changer le façon dont les parties sont stockés ?
extern games games_started;
extern games games_not_started;

//Verrou principal
extern pthread_mutex_t verrou_main;

//Fonction qui écoute le joueur
void* listen_player(void* args);