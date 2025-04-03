import time
from bitalino import BITalino

def start_bitalino_service(mac_address, running_time=5, batteryThreshold=30, samplingRate=1000, nSamples=10):
    acqChannels = [0, 1, 2, 3, 4, 5]
    digitalOutput_on = [1, 1]
    digitalOutput_off = [0, 0]

    # Connect to BITalino using the passed MAC address.
    device = BITalino(mac_address)

    # Set battery threshold.
    device.battery(batteryThreshold)

    version = device.version()
    print("BITalino version:", version)

    # Start acquisition.
    device.start(samplingRate, acqChannels)

    start_time = time.time()
    samples = []
    while (time.time() - start_time) < running_time:
        sample_data = device.read(nSamples)
        print(sample_data)
        samples.append(sample_data)

    # Turn BITalino LED and buzzer on.
    device.trigger(digitalOutput_on)
    time.sleep(running_time)
    # Turn BITalino LED and buzzer off.
    device.trigger(digitalOutput_off)

    # Stop acquisition and close the connection.
    device.stop()
    device.close()

    return "Samples: " + str(samples)
