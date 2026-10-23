import threading
import time
import pytest
from scripts.serial_gate import SerialGate, SerialBusyError


def test_timed_out_poll_is_discarded_instead_of_running_after_release():
    gate = SerialGate()
    observed = []

    def poll():
        try:
            with gate.turn(False):
                observed.append('executed')
        except SerialBusyError:
            observed.append('discarded')

    with gate.turn(True):
        thread = threading.Thread(target=poll)
        thread.start()
        thread.join(1)
        assert not thread.is_alive()
    assert observed == ['discarded']
    with gate.turn(True):
        observed.append('new request')
    assert observed == ['discarded', 'new request']


def test_control_requests_precede_background_poll_without_reordering_controls():
    gate = SerialGate()
    observed = []

    def request(name, urgent):
        with gate.turn(urgent):
            observed.append(name)

    def queued(count, urgent):
        deadline = time.monotonic() + .15
        while time.monotonic() < deadline:
            with gate.condition:
                if len(gate.control if urgent else gate.polls) == count:
                    return
            time.sleep(.001)
        pytest.fail('Test worker did not enter the waiting queue')

    with gate.turn(True):
        threads = []
        for name, urgent, count in [('background', False, 1), ('command', True, 1), ('ack', True, 2)]:
            thread = threading.Thread(target=request, args=(name, urgent))
            thread.start()
            threads.append(thread)
            queued(count, urgent)
    for thread in threads:
        thread.join(1)
    assert observed == ['command', 'ack', 'background']
