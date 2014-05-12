#!/usr/bin/env sh
# ./ndcg.sh ranked.txt relevanceFile
java -Xmx1024m -cp bin/ edu.stanford.cs276.NdcgMain $1 $2
