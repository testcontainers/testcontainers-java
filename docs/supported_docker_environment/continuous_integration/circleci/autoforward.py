#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import dataclasses
import threading
import sys
import signal
import subprocess
import json
import re
import time


@dataclasses.dataclass(frozen=True)
class Forward:
    port: int

    def __ne__(self, other):
        return not self.__eq__(other)

    @staticmethod
    def parse_list(ports):
        r = []
        for port in ports.split(","):
            port_splits = port.split("->")
            if len(port_splits) < 1:
                continue
            host, port = Forward.parse_host(port_splits[0], "localhost")
            if port is not None:
                r.append(Forward(port))
        return r

    @staticmethod
    def parse_host(s, default_host):
        s = re.sub("/.*$", "", s)
        hp = s.split(':')
        if len(hp) == 1:
            return default_host, int(hp[0])
        if len(hp) == 2:
            return hp[0], int(hp[1])
        return None, None


class PortForwarder:
    def __init__(self, forward, local_bind_address="127.0.0.1"):
        self.process = subprocess.Popen([
            'ssh',
            '-N',
            f'-L{local_bind_address}:{forward.port}:localhost:{forward.port}',
            "remote-docker"
        ])

    def stop(self):
        self.process.kill()


class DockerForwarder:
    def __init__(self):
        self.running = threading.Event()
        self.running.set()

    def start(self):
        forwards = {}
        try:
            while self.running.is_set():
                new_forwards = self.container_config()
                existing_forwards = list(forwards.keys())
                for forward in new_forwards:
                    if forward in existing_forwards:
                        existing_forwards.remove(forward)
                    else:
                        print(f"adding forward {forward}")
                        forwards[forward] = PortForwarder(forward)

                for to_clean in existing_forwards:
                    print(f"stopping forward {to_clean}")
                    forwards[to_clean].stop()
                    del forwards[to_clean]
                time.sleep(.8)
        finally:
            for forward in forwards.values():
                forward.stop()

    @staticmethod
    def container_config():
        def cmd(cmd_array):
            out = subprocess.Popen(cmd_array,
                                   universal_newlines=True,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
            out.wait()
            return out.communicate()[0]

        try:
            stdout = cmd(['docker', 'ps', '--format', "'{{json .}}'"])
            stdout = stdout.replace("'", "")
            configs = map(lambda l: json.loads(l), stdout.splitlines())
            forwards = []
            for c in configs:
                if c is None or c['Ports'] is None:
                    continue
                ports = c['Ports'].strip()
                if ports == '':
                    continue
                forwards += Forward.parse_list(ports)
            return forwards
        except RuntimeError:
            print("Unexpected error:", sys.exc_info()[0])
            return []

    def stop(self):
        print("stopping")
        self.running.clear()


def main():
    forwarder = DockerForwarder()

    def handler(*_):
        forwarder.stop()

    signal.signal(signal.SIGINT, handler)

    forwarder.start()


if __name__ == "__main__":
    main()
