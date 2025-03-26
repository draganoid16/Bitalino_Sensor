import numpy as np

# Persistent variable to hold the current array
current_array = np.array([])

def add_one():
    """Appends the value 1 to the current array and returns the updated array."""
    global current_array
    # Append a new 1 to the array
    current_array = np.append(current_array, 1)
    return current_array
