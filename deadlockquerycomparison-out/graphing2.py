import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

names = ['wait die', 'wound wait', 'graph', 'timeout']

with open('out4.txt', 'r') as f:
    content = f.read()

data = {}
blocks = content.split('-separator-\n')
for i, n in enumerate(names):
    lines = blocks[i][:-1].split('\n')
    vals = []
    for l in lines:
        dd = np.array(l.split(' ')[1:], dtype=float)
        vals.append(np.mean(dd))
    data[n] = np.array(vals, dtype=float)

data['x'] = [x.split(' ')[0] for x in blocks[0][:-1].split('\n')]

df = pd.DataFrame(data)
for n in names:
    plt.plot('x', n, data=df)

# 2
# plt.title('difference between delay times')
# plt.xlabel('difference(ms)')
# plt.ylabel('runtime(ms)')

# 3
# plt.title('max extra delay time vs runtime')
# plt.xlabel('max extra delay time(ms)')
# plt.ylabel('runtime(ms)')

# 4
plt.title('sleep between session 1')
plt.xlabel('sleep time(ms)')
plt.ylabel('runtime(ms)')

# 5
# plt.title('sleep between session 2')
# plt.xlabel('sleep time(ms)')
# plt.ylabel('runtime(ms)')

# 6
# plt.title('sleep before session 2')
# plt.xlabel('sleep time(ms)')
# plt.ylabel('runtime(ms)')

# 7
# plt.title('reverse order')
# plt.xlabel('sleep time(ms) before session 1')
# plt.ylabel('runtime(ms)')

plt.legend()
plt.show()
