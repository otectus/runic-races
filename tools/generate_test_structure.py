"""Generate our minimal, original stone-floor Forge GameTest fixture (no world save)."""
from pathlib import Path
import gzip, struct
def string(s):
    b=s.encode(); return struct.pack('>H',len(b))+b
def tag(t,name,value): return bytes([t])+string(name)+value
def integer(n): return struct.pack('>i',n)
def intlist(ns): return b'\x03'+integer(len(ns))+b''.join(map(integer,ns))
floor=[]
for x in range(10):
    for z in range(10):
        floor.append(tag(9,'pos',intlist([x,0,z]))+tag(3,'state',integer(0))+b'\0')
payload=tag(3,'DataVersion',integer(3465))+tag(9,'size',intlist([10,8,10]))
payload+=tag(9,'palette',b'\x0a'+integer(1)+tag(8,'Name',string('minecraft:stone'))+b'\0')
payload+=tag(9,'blocks',b'\x0a'+integer(len(floor))+b''.join(floor))+tag(9,'entities',b'\x0a'+integer(0))+b'\0'
target=Path(__file__).resolve().parents[1]/'src/gameTest/resources/data/runic_races/structures/empty.nbt'
target.parent.mkdir(parents=True,exist_ok=True)
target.write_bytes(gzip.compress(b'\x0a\0\0'+payload,mtime=0))
