"""Build the portable firmware adapter with the pinned host compiler, no boards."""
from pathlib import Path
import subprocess
import sys
import hashlib

ROOT = Path(__file__).resolve().parents[1]

def build(output=None):
    requested_output=output
    output = Path(output or ROOT/'benchmarks/.host_build/dev_controller.exe')
    output.parent.mkdir(parents=True, exist_ok=True)
    common = ROOT/'firmware/common'
    sources=[ROOT/'tests/native/dev_controller.cpp',*sorted((common/'runtime').glob('*.h')),*sorted((common/'safety_kernel').glob('*.[ch]'))]
    digest=hashlib.sha256((sys.executable+';ziglang=0.13.0').encode()+b''.join(p.read_bytes() for p in sources)).hexdigest()
    # Windows locks running executables. A source change gets a new artifact,
    # so a running host demo cannot block a later build or test campaign.
    if requested_output is None:output=output.with_name('dev_controller-'+digest[:12]+'.exe')
    fingerprint=output.with_suffix('.sha256')
    if output.is_file() and fingerprint.is_file() and fingerprint.read_text()==digest:return output
    objects=[]
    for name in ('safety_kernel/safety_kernel.c',):
        obj=output.with_name(output.stem+'-'+Path(name).stem+'.o');objects.append(str(obj))
        subprocess.run([sys.executable,'-m','ziglang','cc','-std=c11','-O2',f'-I{common/"safety_kernel"}','-c',str(common/name),'-o',str(obj)],check=True,timeout=180)
    subprocess.run([sys.executable,'-m','ziglang','c++','-std=c++17','-O2',*[f'-I{common/n}' for n in ('runtime','safety_kernel')],str(ROOT/'tests/native/dev_controller.cpp'),*objects,'-o',str(output)],check=True,timeout=180)
    fingerprint.write_text(digest)
    return output

if __name__=='__main__':
    print(build())
