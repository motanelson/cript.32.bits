import threading
import time


class stimes:
    def __init__(self,sleeps:float,subs):
        self.sleeps=sleeps
        self.subs=subs
        self.stops=False
        self.the=threading.Thread(target=self.subst)
        self.the.start()
    def subst(self):
        while 1:
            if self.stops:
                break
            time.sleep(self.sleeps)
            if self.stops:
                break
            self.subs()

    def sstops(self):
        self.stops=True
