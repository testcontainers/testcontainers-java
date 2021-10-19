FROM alpine:3.14

HEALTHCHECK --interval=1s CMD test -e /testfile

COPY write_file_and_loop.sh write_file_and_loop.sh
RUN chmod +x write_file_and_loop.sh

CMD ["/write_file_and_loop.sh"]
