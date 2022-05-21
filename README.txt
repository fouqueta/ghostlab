L3 Informatique - Programmation réseaux
Projet: GhostLab
=====================================================

## Groupe (44)
Cunha Teixeira José Miguel, 21950500.  
Rochier Marie, 21955449.
Fouquet Aurélie, 21983830.

## Compilation et exécution
Après avoir décompressé l'archive PR-projet-groupe44.zip, on se déplace dans le dossier créé.
On utilise alors la commande 'make' pour compiler le projet.
Dans ce protocole, on considère les machines comme étant sur le même réseau local.

1. Serveur
Dans un terminal, on exécute la commande:
```
./server [port]
```
Avec port correspondant au port souhaité.

2. Client
Dans un autre terminal, on exécute la commande:
```
java -classpath src/ Client [port] [adresse_ip]
```
Avec port correspondant au port auquel le serveur que nous souhaitons rejoindre est lié,
et adresse_ip qui est un argument optionnel.
Si cet argument n'est pas précisé, l'adresse ip par défaut sera lulu.informatique.univ-paris-diderot.fr .

Il est possible d'utiliser la commande 'make distclean' pour nettoyer le dossier src.

## Fonctionnalités
Toutes les fonctionnalités du protocole de base sont implémentées.
Les fonctionnalités supplémentaires sont:
1. Le choix des actions (par le client) se fait avec des raccourcis qui sont précisés
à chaque étape, que ce soit dans la partie ou avant.
Chacun de ces raccourcis est associé à une requête, qui est envoyée au serveur afin d'être traitée?

2. Chaque fantôme vaut un certain nombre de points.

3. La taille du labyrinthe est modifiable par les joueurs inscrits 
à une partie et tant que celle-ci n'est pas commencée.
La requête envoyée par un joueur souhaitant modifier la taille est [SIZEM h w***],
avec h la valeur pour la hauteur et w pour la largeur.

Si l'une de ces deux valeurs est égale à 0, la valeur par défaut sera celle renseignée dans maze.h.
Les joueurs peuvent envoyer autant de requête qu'ils le souhaitent,
mais la modification ne sera autorisée que toutes les 10 secondes.

Le serveur répond [SIZEO***] lorsque la modification est possible
ou [SIZEN***] si ce n'est pas possible 
(notamment si une requête de modification a déjà été envoyée il y a moins de 10 secondes).

Le temps d'attente est partagé entre cette requête et la requête [NUMGH f***].
Ces paramètres respectent la spécification du protocole de base des messages TCP.

4. Le nombre de fantômes dans le labyrinthe est modifiable par les joueurs inscrits
à une partie et tant que celle-ci n'est pas commencée.
La requête envoyée par un joueur souhaitant modifier le nombre de fantômes est [NUMGH f***],
avec f le nombre de fantômes.

Si f est égal à 0, la valeur par défaut sera celle renseignée dans maze.h.
De même, la modification ne sera prise en compte que toutes les 10 secondes.
Le serveur répond [NUMGO***] lorsque la modification est possible
ou [NUMGN***] si ce n'est pas possible 

Le temps d'attente est partagé entre cette requête et la requête [SIZEM h w***].
Ces paramètres respectent la spécification du protocole de base des messages TCP.

5. Le joueur découvre son environnement au fur et à mesure de ses déplacements,
et en conserve une trace.
Dans le client, le joueur peut observer ses déplacements avec un affichage 
des (maximum) 9x9 cases alentours. 
Lorsqu'il rencontre un mur, '?' est remplacé par '#'.
Si c'est un chemin, '?' sera remplacé par ' '.

6. TODO collision

7. Consulter nombre de fantômes

## Architecture
Le serveur a été développé en langage C et le client en Java.

### server.c, server.h
Ces fichiers composent le programme principal du serveur.
Il crée un thread par joueur qui se connecte, reçoit ses messages et agit en fonction.

### game.c, game.h
Ces fichiers contiennent la structure de données d'une partie et les méthodes associées.
Ces méthodes permettent le déplacement aléatoire des fantômes et leur capture, 
l'initialisation d'une partie, l'ajout, la suppression et le placement d'un joueur dans une partie, 
le choix du port de la partie, et le gagnant d'une partie.
On considère que le gagnant d'une partie est celui ayant le plus de points à la fin d'une partie.
Si plusieurs joueurs ont le même nombre de points, le gagnant sera choisi au hasard parmi ces joueurs.

### maze.c, maze.h
A la manière de game.c et game.h, ces fichiers contiennent la structure de données d'un labyrinthe 
et les méthodes associées.
Ces méthodes permettent d'initialiser un labyrinthe et les fantômes qui s'y trouvent.
Par défaut, la taille du labyrinthe est 30x30 avec 10 fantômes.

### player.c, player.h
De même, ces fichiers contiennent la structure de données d'un joueur et les méthodes associées.
Ces méthodes permettent de récupérer un joueur en fonction de son nom ou de son numéro dans 
la liste des joueurs de la partie, initialiser un joueur et le déplacer ou 
vérifier que deux joueurs n'ont pas le même nom.

### send.c, send.h
Ces fichiers contiennent les méthodes relatives à l'envoi de message au client.

### Client.java, InGameMulticast.java, InGameTCP.java, InGameUDP.java
Le programme principal du client est Client.java et il utilise les services
InGameMulticast.java, InGameTCP.java et InGameUDP.java pour la communication.
Chacun de ces fichiers implémente Runnable et gère un type de communication et les messages qui y sont associés.
