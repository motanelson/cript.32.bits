print("\033c\033[47;31m\ngive me a text .txt file to encript ? ")
a=input().strip()
b=a.replace(".txt","")
print("\033[47;31m\ngive me a password to encript ? ")
c=input().strip()
f1=open(a,"rb")
f=f1.read()
f1.close()
r=b''
counter=0
g=c.encode()
for ff in f:
   i=int(ff)
   ii=int(g[counter])
   fff=0xff & (i+ii)
   rr=bytearray([fff,0,0,0])
   r=r+rr
   counter=counter+1
   if counter>=len(g):
       counter=0

f1=open(b+".bin","bw")
f1.write(r)
f1.close()