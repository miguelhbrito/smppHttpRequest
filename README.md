# smppHttpRequest

Salvar o client.service no /etc/systemd/system
Salvar o client.sh no /usr/bin e habilidar chmod a+x client.sh

Para iniciar o serviço, no terminal digite:

> sudo systemctl enable client.service
> sudo systemctl start client.service

Para parar:

> sudo systemctl stop client.service

Verificar status:

> sudo systemctl status client.service

Obs, toda vez que ocorrer alteração no arquivo e for gerado outro ".jar" devera ser feito tais ações:

> sudo systemctl stop client.service
> sudo systemctl daemon-reload
> sudo systemctl start client.service
