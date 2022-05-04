//Répondre au message GAME? et à la premiere connexion
int sendGames(int fd);

//Envoyer DUNNO***
int sendDunno(int fd);

//Envoyer la liste des joueurs dans la partie (en cours)
int sendGList(int fd, game * g);

int sendSize(int fd, game * g);

int sendList(int fd, game * g);