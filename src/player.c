#include "../includes/server.h"

int is_same_player(player * p1, player * p2){
    return strncmp(p1->name,p2->name, 8) == 0;
}

int name_taken(player_node * first, char name[8]){
    if(first == NULL){
        return 0;
    }else if(strncmp(first->p->name, name, 8) == 0){
        return 1;
    }else{
        return name_taken(first->next,name);
    }
}

int in_list(player_node * first, player * p){
    if(first == NULL){
        return 0;
    }else if(is_same_player(first->p, p)){
        return 1;
    }else{
        return in_list(first->next,p);
    }
}

int len_list(player_node * first){
    if(first == NULL){
        return 0;
    }
    return 1 + len_list(first->next);
}

player_node * add_player(player_node * first, player * p){
    if(first != NULL && in_list(first,p)){
        return first;
    }
    player_node * new = malloc(sizeof(player_node));
    new->p = p;
    new->next = first;
    return new;
}

player_node * remove_player(player_node * first, player * p){
    if(first==NULL){
        return NULL;
    }else if(is_same_player(first->p, p)){
        return first->next;
    }else if(is_same_player(first->next->p, p)){
        player_node * to_free = first->next;
        first->next = first->next->next;
        free(to_free);
        return first;
    }else{
        first->next = remove_player(first->next, p);
        return first;
    }
}

player * get_n_player(player_node * first, int n){
    if(first == NULL) {
        return NULL;
    }else if(n==0){
        return first->p;
    }
    return get_n_player(first->next, n-1);
}

player * init_player(char pseudo[8], char port[4], char ip[15]){
    player *p = malloc(sizeof(player));
    p->verrou_player = (pthread_mutex_t)PTHREAD_MUTEX_INITIALIZER;
    memcpy(p->name, pseudo, 8);
    memcpy(p->port, port, 4);
    memcpy(p->ip, ip, 15);

    p->score = 0;
    p->x = 0;
    p->y = 0;
    return p;
}

int move_player(player *p, int x, int y){
    p->x = x;
    p->y = y;
    pthread_mutex_lock(&(p->g->verrou_server));
    maze *lab = p->g->laby;
    if(checkGhost(lab, p->g->nb_ghosts, p->x, p->y)==1){
        p->score = p->score + 1;
        pthread_mutex_unlock(&(p->g->verrou_server));
        return 1;
    }
    pthread_mutex_unlock(&(p->g->verrou_server));
    return 0;
}

player *get_player_fromName(player_node *first, char name[8]){
    if(first == NULL) {
        return NULL;
    }
    else if(strcmp(first->p->name, name) == 0){
        return first->p;
    }
    return get_player_fromName(first->next, name);
}