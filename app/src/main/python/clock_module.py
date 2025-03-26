import time

def service_started():
    print("Clock service started")

def update_clock():
    # Return the current time as a formatted string.
    return time.strftime("%H:%M:%S")
