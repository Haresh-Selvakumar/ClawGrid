import pytest
from scripts.flash_native import validate_pair_identity, read_identity

PAIR=dict(node_a_mac='e0:72:a1:f5:bc:b0',node_b_mac='e0:72:a1:f6:a9:68')

def test_accepts_updated_pair():
    validate_pair_identity(b'ClawGrid:pair:A=e0:72:a1:f5:bc:b0;B=e0:72:a1:f6:a9:68\0',PAIR)

@pytest.mark.parametrize('image',[b'old unmarked firmware',b'ClawGrid:pair:A=e0:72:a1:f6:a9:68;B=e0:72:a1:f5:bc:b0\0'])
def test_rejects_old_pair(image):
    with pytest.raises(RuntimeError,match='Rebuild'): validate_pair_identity(image,PAIR)

def test_parses_esptool_mac_only():
    assert read_identity('MAC: E0:72:A1:F5:BC:B0\n')==PAIR['node_a_mac']
    with pytest.raises(RuntimeError): read_identity('Serial port COM8 could not open')
