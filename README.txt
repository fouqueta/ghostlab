L3 Informatique - Programmation réseaux
Projet: GhostLab
=====================================================

## Groupe (44)
Cunha Teixeira José Miguel, 21950500.  
Rochier Marie, 21955449.
Fouquet Aurélie, 21983830.

## Compilation et exécution
Après avoir décompressé l'archive PR-projet-groupe44.zip, 
on se déplace dans le dossier créé.
On utilise alors la commande 'make' pour compiler le projet.
Dans ce protocole, on considère les machines comme étant sur le même réseau local.

1. Serveur
Dans un terminal, on exécute la commande:
```
./server [port]
```
Avec l'argument port correspondant au port souhaité.

2. Client
Dans un autre terminal, on exécute la commande:
```
java -classpath src/ Client [port] [adresse_ip]
```
Avec port correspondant au port auquel le serveur que nous souhaitons rejoindre s'est lié,
et adresse_ip qui est un argument optionnel.
Si cet argument n'est pas précisé, 
l'adresse ip par défaut sera lulu.informatique.univ-paris-diderot.fr .

Il est possible d'utiliser la commande 'make distclean' pour nettoyer le dossier src.

## Fonctionnalités
Toutes les fonctionnalités du protocole de base sont implémentées.
Les fonctionnalités supplémentaires sont:
1. Les fantômes peuvent rapportés un nombre de points différent.
2. La taille du labyrinthe est modifiable par les joueurs inscrits 
à une partie et tant que celle ci n'est pas commencée.
La requête envoyée par un joueur souhaitant modifier la taille est:
```
[SIZEM h w***]
```
Avec h la valeur pour la hauteur et w pour la largeur.
Si l'une de ces deux valeurs est égale à 0, le labyrinthe prendra la taille par défaut.
Ces paramètres respectent la spécification du protocole de base des messages TCP.

## Architecture
Le serveur a été développé en langage C et le client en Java.

### server.c, server.h
Ces fichiers composent le programme principal du serveur.
Il crée un thread par joueur qui se connecte, reçoit ses messages et agit en fonction.

### game.c, game.h
Ces fichiers contiennent toutes les informations d'une partie.
Ils contiennent les méthodes permettant le déplacement aléatoire des fantômes et leur capture, 
l'initialisation d'une partie, l'ajout, la suppression et le placement d'un joueur dans une partie, 
le choix du port de la partie, et le gagnant d'une partie.
On considère que le gagnant d'une partie est celui ayant le plus de points à la fin d'une partie.
Si plusieurs joueurs ont le même nombre de points, le gagnant sera choisi au hasard parmi ces joueurs.

### maze.c, maze.h
A la manière de game.c et game.h, ces fichiers contiennent les informations sur un labyrinthe.
Ils contiennent les méthodes permettant d'initialiser un labyrinthe et les fantômes qui s'y trouvent.
Par défaut, la taille du labyrinthe est 30x30.
TODO: Nombre de fantômes.

### player.c, player.h
TODO

### send.c, send.h
TODO

### Client.java, InGameMulticast.java, InGameTCP.java, InGameUDP.java
Le programme principal du client est Client.java et il utilise les services
InGameMulticast.java, InGameTCP.java et InGameUDP.java pour la communication.
Chacun de ces fichiers implémente Runnable et gère un type de communication et les messages qui y sont associés.