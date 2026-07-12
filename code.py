
print("\033c\033[47;31m\ngive me a text .txt file to encript ? ")
a=input().strip()
b=a.replace(".txt","")
print("\033[47;31m\ngive me a password to encript ? ")
c=input().strip()
f1=open(a,"r")
f=f1.read()
f1.close()
f=f.encode("utf-32")
c=c.encode("utf-32")
r=None
counter=0
g=c
r=''
for ff in f:
   i=int(ff)
   ii=int(g[counter])
   fff=0xffff & (i+ii)
   rr=chr(fff)
   r=r+rr
   counter=counter+1
   if counter>=len(g):
       counter=0

f1=open(b+".bin","w",encoding="utf-32")
f1.write(r)
f1.close()