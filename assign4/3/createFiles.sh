i=1
mkdir nodeFile
while [ $i -lt 101 ]
do
	touch ./nodeFile/File$i
	i=$((i+1))

	echo $i
done