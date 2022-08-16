FROM alpine:3.16
ADD folder/someFile.txt /someFile.txt
RUN cat /someFile.txt
ADD test.txt /test.txt
RUN cat /test.txt
CMD ping -c 5 www.google.com
