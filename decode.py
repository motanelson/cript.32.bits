print("\033c\033[47;31m\ngive me a binary 32 bits .bin file to decript ? ")
a=input().strip()
b=a.replace(".bin","")
print("\033[47;31m\ngive me a password to encript ? ")
c=input().strip()
f1=open(a,"rb")
f=f1.read()
f1.close()
r=b''
counter=0
counter1=0
g=c.encode()
for ff in f:
   if counter1==3:
       i=int(ff)
       ii=int(g[counter])
       fff=0xff & (i-ii)
       rr=bytearray([fff])
       r=r+rr
       counter=counter+1
       if counter>=len(g):
           counter=0
   counter1=(counter1+1)& 3
f1=open(b+".txt","bw")
f1.write(r)
f1.close()