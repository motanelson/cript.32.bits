import starttime
import time
global a

a = 0


def mysubs():
    global a
    print(a)
    a=a+1


mytimer=starttime.stimes(1.00,mysubs)
time.sleep(10)
mytimer.sstops()


