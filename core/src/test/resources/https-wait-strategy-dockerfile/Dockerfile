FROM nginx:1.17-alpine

# Create keypair and self-signed certificate for https test
RUN apk update && apk add bash openssl && openssl req -batch -x509 -nodes -days 365 -newkey rsa:2048 -subj "/CN=localhost" -keyout /etc/ssl/private/nginx-selfsigned.key -out /etc/ssl/certs/nginx-selfsigned.crt

ADD nginx-ssl.conf /etc/nginx/conf.d/default.conf
