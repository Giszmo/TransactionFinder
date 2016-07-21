# TransactionFinder

The idea with this repository is to build a service that allows clients to quickly learn about activity on their wallet, without having to either download the whole blockchain or share their addresses with servers.

While one approach to not share addresses is to share Bloom filters, this is a very bad practice, as knowing a user's Bloom filter allows an attacker to learn all addresses even if the bloom filter contained a lot of noise.

Here, the concept of Bloom filters is also used but with the client downloading filters instead of sharing filters. This way, the user can close in on blocks that relevant to him and download whole blocks as if he were a full node.

The exact address one is interested in, does not get revealed that easily.

So far TransactionFinder parses the blocks folder's `blk*.dat` files and creates one Bloom filter per block and serializes them into 1MB files using [protocol buffers](https://github.com/google/protobuf).

Currently, 0.0001% false positives was chosen, assuming clients have around 1000 addresses that they want to monitor. With fewer addresses, a laxer filter might make more sense.

The whole blockchain parses into 1.5GB of filters, using only the P2PKH addresses of the outputs.

Missing parts are:

* also add the input addresses and transaction hashes
* adjust false positive probability
* implement a web api (add it to mycelium bqs)
