"""Bounded, ordered USB access; control/ACK requests precede background polls."""
from collections import deque
from contextlib import contextmanager
import threading
import time


class SerialBusyError(TimeoutError):
    """The USB queue was busy; this request never touched the board."""


class SerialGate:
    def __init__(self):
        self.condition = threading.Condition()
        self.busy = False
        self.control = deque()
        self.polls = deque()

    @contextmanager
    def turn(self, urgent):
        ticket = object()
        queue = self.control if urgent else self.polls
        deadline = time.monotonic() + (3 if urgent else .25)
        with self.condition:
            queue.append(ticket)
            while self.busy or queue[0] is not ticket or (not urgent and self.control):
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    queue.remove(ticket)
                    self.condition.notify_all()
                    raise SerialBusyError('USB transport busy; request not sent and not queued for later execution')
                self.condition.wait(remaining)
            queue.popleft()
            self.busy = True
        try:
            yield
        finally:
            with self.condition:
                self.busy = False
                self.condition.notify_all()
