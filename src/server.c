#include "../includes/server.h"

game games_started[];
game games_not_started[];

int main(int argc, char ** argv) {
    if(argc<2){
        printf("Missing port number !\n");
        exit(EXIT_FAILURE);
    }

    //Creation de la socket
    int sock_server = socket(PF_INET, SOCK_STREAM, 0);
    if(sock_server==-1){
        perror("socket");
        exit(EXIT_FAILURE);
    }

    struct sockaddr_in addr_sock = {
            .sin_family = AF_INET,
            .sin_port = htons(atoi(argv[1])),
            .sin_addr.s_addr = htonl(INADDR_ANY)
    };

    //Bind socket
    int r = bind(sock_server, (struct sockaddr *)&addr_sock, sizeof(addr_sock));
    if(r==-1){
        perror("bind");
        exit(EXIT_FAILURE);
    }

    //
    r = listen(sock_server, 0);
    if(r!=0){
        perror("listen");
        exit(EXIT_FAILURE);
    }

    //Boucle principale du serveur
    while(1){
        break;
    }
    close(sock_server);
    return 0;
}
