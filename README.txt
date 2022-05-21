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
Avec h et w, les valeurs pour la hauteur et la largeur respectivement.
Si l'une de ces deux valeurs est égale à 0, le labyrinthe prendra la taille par défaut.
Ces paramètres respectent la spécification du protocole de base des messages TCP.

## Architecture
Le serveur a été développé en langage C et le client en Java.

### game.c, game.h
TODO

### maze.c, maze.h
Ces fichiers 
Par défaut, la taille du labyrinthe est 30x30.

### player.c, player.h
TODO

### send.c, send.h
TODO

### server.c, server.h
TODO

### Client.java
TODO