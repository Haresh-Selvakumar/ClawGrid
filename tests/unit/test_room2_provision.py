"""Provisioning validation uses synthetic ROM text; no device access or key generation."""
import sys
from pathlib import Path
import pytest
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'scripts'))
from provision_room2 import identify

EXISTING={'node_a_mac':'e0:72:a1:f5:bc:b0','node_b_mac':'e0:72:a1:f6:a9:68'}
def text(chip='ESP32-D0WD-V3 (revision v3.1)',mac='02:00:00:00:00:03'):
    return f'Chip type:          {chip}\nMAC:                {mac}\n'
def test_distinct_classic_identity_from_rom_only():
    r=identify(text(),EXISTING)
    assert r['node_id']==3 and r['room_id']==2 and r['sensor']=='DHT11'
@pytest.mark.parametrize('chip',['ESP32-S3 (revision v0.2)','ESP32-C3','ESP32-H2','ESP8266','unknown'])
def test_wrong_chip_is_not_accepted_as_classic(chip):
    with pytest.raises(ValueError): identify(text(chip),EXISTING)
@pytest.mark.parametrize('mac',list(EXISTING.values()))
def test_existing_roles_cannot_be_reassigned(mac):
    with pytest.raises(ValueError): identify(text(mac=mac),EXISTING)
