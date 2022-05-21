//Répondre au message GAME? et à la premiere connexion
int sendGames(int fd);

//Envoyer DUNNO***
int sendDunno(int fd);

//Envoyer la liste des joueurs dans la partie (en cours)
int sendGList(int fd, game * g);

int sendSize(int fd, game * g);

int sendList(int fd, game * g);

int sendRegno(int fd);

int sendRegok(int fd, int8_t m);

int sendUnrok(int fd, int8_t m);

int sendStart(int fd, player * p);

int sendMove(int fd, player *p, int ghost);

int sendQuit(int fd);

int sendMess(int fd, player *prov, char id_dest[8], char *message);

int sendMessAll(int fd, player *prov, char *message);

int sendGhost(game *g, int x, int y);

int sendScore(player *p);

int sendEnd(game *g);

int sendSizeo(int fd);

int sendSizen(int fd);

int sendCol(game * g, char name1[8], char name2[8], int posx, int posy);

int sendNumgo(int fd);

int sendNumgn(int fd);

int sendNbgh(int fd, game *g);