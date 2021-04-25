import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

names = ['wait die', 'wound wait', 'graph', 'timeout']

with open('out4.txt', 'r') as f:
    content = f.read()

data = {}

blocks = content.split('--\n')
for i, n in enumerate(names):
    data[n] = np.array([x.split(' ')[1] for x in blocks[i][:-1].split('\n')], dtype=float)

data['x'] = np.array([x.split(' ')[0] for x in blocks[0][:-1].split('\n')], dtype=float)

df = pd.DataFrame(data)
for n in names:
    plt.plot('x', n, data=df)

# 1
# plt.title('n reads(2000 records)')
# plt.xlabel('read transactions')
# plt.ylabel('run time(ms)')

# 2
# plt.title('n writes(2000 records)')
# plt.xlabel('write transactions')
# plt.ylabel('run time(ms)')
# plt.xlim(0, 500)
# plt.ylim(0, 5000)

# 3
# plt.title('n writes(2000 records, max 500 writes)')
# plt.xlabel('write transactions')
# plt.ylabel('run time(ms)')
# plt.xlim(0, 300)
# plt.ylim(0, 1000)

# 4
plt.title('n records, n writes')
plt.xlabel('records')
plt.ylabel('run time(ms)')
plt.xlim(0, 600)
plt.ylim(0, 1000)

plt.legend()
plt.show()