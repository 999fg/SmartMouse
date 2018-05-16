clf = open("out.csv", "r") # clf = computer log file
mlf = open("outlog.csv", "r") # mlf = mobile log file
result = open("result.csv", "w")

cll = clf.readlines()
mll = mlf.readlines()

cllIndex = 0
mllIndex = 0

print (len(mll))

for i in range(len(cll)):
	if (mllIndex == len(mll)):
		break
	cllSplit = cll[i].split(',')
	mllSplit = mll[mllIndex].split(',')
	cllTime = cllSplit[2].strip('\n')
	mllTime = mllSplit[2].strip('\n')
	if (cllTime >= mllTime):
		result.write(mllSplit[0]+","+mllSplit[1]+","+cllSplit[0]+","+cllSplit[1]+","+mllTime+"\n");
		mllIndex = mllIndex + 1

result.close()

clf.close()
mlf.close()

#print (cll)
#print (mll)
