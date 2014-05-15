#!/usr/bin/env sh
# ./test.sh taskType
java -Xmx1024m -cp bin/ edu.stanford.cs276.Rank data/pa3.signal.$2 $1
java -Xmx1024m -cp bin/ edu.stanford.cs276.NdcgMain ranked.txt data/pa3.rel.$2

