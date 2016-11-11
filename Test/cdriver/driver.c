#include <libotr/proto.h>
#include <libotr/privkey.h>
#include <libotr/message.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <sys/select.h>

#define MAXDATASIZE 1024

void inject_msg(void *opdata, const char *accountname,
	    const char *protocol, const char *recipient, const char *message){
		int clientfd= *(int*)opdata;
		send(clientfd, message, strlen(message), 0);
		send(clientfd,"\n", 1, 0);
		printf("\033[31mInjecting message to the recipient\n\033[0m");

}

void gone_secure(void *opdata, ConnContext *context){
	printf("\033[31mAKE succeeded\033[0m\n");
}

int main(){
	OTRL_INIT;
	OtrlUserState userstate;
	char *newmessage = NULL;
	char *nlmsg=NULL;
	OtrlMessageAppOps ui_ops;
	int sockfd, clientfd, recvbytes, recv_ret;
	socklen_t sin_size;
	char buf[1024], pwdbuf[1024];
	char *dbuf;
	struct sockaddr_in remote_addr, my_addr;
	fd_set fds;
	int one = 1;
	
	/* initialize ui callbacks */
	memset(&ui_ops, 0, sizeof(OtrlMessageAppOps));
	ui_ops.inject_message= inject_msg;
	ui_ops.gone_secure=gone_secure;
	printf("\033[31mStarting driver...\033[0m\n");

	/* initialize socket connection */
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	my_addr.sin_family=AF_INET;
	my_addr.sin_port= htons(3333);
	my_addr.sin_addr.s_addr = INADDR_ANY;
	bzero(&(my_addr.sin_zero),8);

	/* Allow rebinding to address */
	setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));

	if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(struct sockaddr)) == -1) {
		perror("bind");
		exit(1);
	}
	if (listen(sockfd, 1) == -1) {
		perror("listen");
		exit(1);
	}
	sin_size = sizeof(struct sockaddr_in);
	if ((clientfd = accept(sockfd, (struct sockaddr *)&remote_addr, \
					&sin_size)) == -1) {
		perror("accept");
	}
	printf("\033[31mConnected to Bob\033[0m\n");

	/* create userstate, and read the private key */
	userstate = otrl_userstate_create();
	getcwd(pwdbuf, 1024);
	strcat(pwdbuf, "/private_key");
	otrl_privkey_read(userstate, pwdbuf);


	/* Clients send messages asynchronously*/
	while(1){
		FD_ZERO(&fds);
		FD_SET(0, &fds);
		FD_SET(clientfd, &fds);
		select(clientfd+1, &fds, NULL, NULL, NULL);
		if(FD_ISSET(0, &fds)){
			/* send message */
			fgets(buf, 1023, stdin);
			buf[strlen(buf)-1]= '\0';
			printf("\033[31mTo OTR:%d:\033[0m%s\n\033[0m",strlen(buf), buf);
			dbuf = strdup(buf);
			otrl_message_sending(userstate, &ui_ops, NULL, "bob@msn.com",
					"msn", "alice@msn.com", &dbuf, NULL, &newmessage,
					OTRL_FRAGMENT_SEND_SKIP, NULL, NULL, NULL, NULL);
			free(dbuf);
			nlmsg = (char*)malloc(strlen(newmessage)+2);			
			memcpy(nlmsg, newmessage, strlen(newmessage));
			nlmsg[strlen(newmessage)]='\n';
			nlmsg[strlen(newmessage)+1]='\0';
			send(clientfd, nlmsg, strlen(nlmsg), 0);
			printf("\033[31mTo network:%d:\033[35m%s\033[0m\n\033[0m", strlen(newmessage), newmessage);
		}else{
			/*receive message */
			recvbytes=recv(clientfd, buf, MAXDATASIZE, 0);
			if(recvbytes==0) break;
			recvbytes--;
			buf[recvbytes] = '\0';
			if(buf[recvbytes-1]=='\r'){
				 recvbytes--;
				 buf[recvbytes]='\0';
			}
			printf("\033[31mFrom network:%d:\033[35m %s\033[0m\n",recvbytes, buf);
			recv_ret=otrl_message_receiving(userstate, &ui_ops, &clientfd, "bob@msn.com",
					"msn", "alice@msn.com", buf, &newmessage, NULL,
					NULL, NULL, NULL, NULL);
			if(recv_ret==0)
				printf("\033[31mFrom OTR:%d:\033[0m %s\n", newmessage ? strlen(newmessage) : 0, newmessage);
		}
	}
	return 0;
}










