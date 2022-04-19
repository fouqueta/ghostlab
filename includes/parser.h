//Fonction pour repondre au message GAME? et à la premiere connexion
int sendGames(int fd);

//Fonction pour envoyer DUNNO***
int sendDunno(int fd);

//Envoie la liste des joueurs dans la partie(en cours)
int sendGList(int fd, game * g);
