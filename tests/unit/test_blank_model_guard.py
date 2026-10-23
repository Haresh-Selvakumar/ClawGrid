import pytest
from scripts.flash_native import validate_blank_model_partition


def test_blank_matching_model_region_is_installable():
    validate_blank_model_partition(b'partition' + b'\xff' * 20, b'partition', b'\xff' * 0x600000)


@pytest.mark.parametrize('region', [b'\xff' * 20, b'\x00' + b'\xff' * (0x600000 - 1)], ids=['incomplete', 'existing-data'])
def test_existing_or_incomplete_model_is_preserved(region):
    with pytest.raises(RuntimeError, match='not entirely blank'):
        validate_blank_model_partition(b'partition', b'partition', region)


def test_partition_difference_stops_installation():
    with pytest.raises(RuntimeError, match='partition table differs'):
        validate_blank_model_partition(b'changed', b'partition', b'\xff' * 0x600000)
