FROM alpine:latest

HEALTHCHECK --interval=1s CMD test -e /testfile

ADD write_file_and_loop.sh write_file_and_loop.sh
RUN chmod +x write_file_and_loop.sh

CMD ["/write_file_and_loop.sh"]
