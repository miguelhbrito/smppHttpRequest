# smppHttpRequest

## Os arquivos client.service e client.sh se encontram no pacote client, dentro do projeto.

Salvar o client.service no /etc/systemd/system

Salvar o client.sh no /usr/bin e habilidar chmod a+x client.sh

Para iniciar o serviço, no terminal digite:

> sudo systemctl enable client.service

> sudo systemctl start client.service

Para parar o serviço digite no terminal:

> sudo systemctl stop client.service

Verificar status do serviço digite no terminal:

> sudo systemctl status client.service

Obs: toda vez que ocorrer alteração no arquivo e for gerado outro ".jar" devera ser feito tais ações no terminal:

> sudo systemctl stop client.service

> sudo systemctl daemon-reload

> sudo systemctl start client.service
