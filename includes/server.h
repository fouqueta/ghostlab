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
#include <endian.h>

#include "game.h"
#include "parser.h"

//TODO: Changer la façon dont les parties sont stockées ?
extern game ** game_list;

//Verrou principal
extern pthread_mutex_t verrou_main;

//Fonction qui écoute le joueur
void* listen_player(void* args);