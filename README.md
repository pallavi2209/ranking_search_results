##Ranking search results:

Implementing three different ranking functions and used the
NDCG metric for evaluating the effectiveness of the ranking function. The
estimation of parameters for the ranking functions has been done manually.

1. Cosine Similarity:
Implemented a variant of cosine similarity (with the L1-
Norm) as the ranking function. This essentially involves constructing the
document vector and the query vector and then taking their dot product.

2. BM25F:
Implemented the BM25F ranking algorithm.

3. Smallest Window:
Inncorporated window sizes into the ranking algorithm. For a given query, the smallest window wq,d
is defined to be the smallest sequence of tokens in document d such that all
of the terms in the query q for are present in that sequence.
