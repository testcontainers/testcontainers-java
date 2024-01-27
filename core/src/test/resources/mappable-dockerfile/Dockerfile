FROM alpine@sha256:c5b1261d6d3e43071626931fc004f70149baeba2c8ec672bd4f27761f8e1ad6b
ADD folder/someFile.txt /someFile.txt
RUN cat /someFile.txt
ADD test.txt /test.txt
RUN cat /test.txt
CMD ping -c 5 www.google.com
