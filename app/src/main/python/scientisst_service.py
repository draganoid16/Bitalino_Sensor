import time
from sense import ScientISST

def start_scientisst_service(mac_address: str,
                             sampling_rate: int = 1000,
                             channels: list = [1,2,3,4,5,6],
                             duration: float = 5.0) -> str:
    """
    Connect to the ScientISST Sense device, record for `duration` seconds,
    then return a newline‑delimited dump of the frames.
    """
    # 1) instantiate
    dev = ScientISST(mac_address)

    # 2) (auto) connect happens inside the constructor in sense.py;
    #    if not, call dev.connect() here.

    # 3) start with your frequency & channels
    dev.start(sampling_rate, channels)

    # 4) read until time’s up
    frames = []
    t0 = time.time()
    while time.time() - t0 < duration:
        batch = dev.read()           # sense.py’s read() returns a list of frames
        frames.extend(batch)

    # 5) stop & 6) disconnect
    dev.stop()
    dev.disconnect()

    # Serialize as text
    return "\n".join(str(f) for f in frames)
