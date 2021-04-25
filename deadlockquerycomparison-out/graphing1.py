import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

data = []
maxtimes = []
with open('out.txt', 'r') as f:
    for i in range(3):
        data.append([float(f.readline()[:-1].split(' ')[1])] * 30)
    timeout = []    
    for i in range(30):
        sp = f.readline()[:-1].split(' ')
        maxtimes.append(float(sp[0]))
        timeout.append(float(sp[1]))
    data.append(timeout)

df = pd.DataFrame({
    'MAX_TIME': maxtimes,
    'WAIT_DIE': data[0],
    'WOUND_WAIT': data[1],
    'GRAPH': data[2],
    'TIMEOUT': data[3]})

plt.title('MAX_TIME vs total runtime(in ms)')
plt.plot('MAX_TIME', 'WAIT_DIE', data=df)
plt.plot('MAX_TIME', 'WOUND_WAIT', data=df)
plt.plot('MAX_TIME', 'GRAPH', data=df)
plt.plot('MAX_TIME', 'TIMEOUT', data=df)
plt.ylim(20, 300)

plt.legend()

plt.show()