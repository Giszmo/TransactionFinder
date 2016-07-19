# TransactionFinder

The idea with this repository is to build a service that allows clients to quickly learn about activity on their account, without having to either download the whole blockchain or share their addresses.

While one approach to not share addresses is to use Bloom filters, this is a very bad practice, as knowing a user's Bloom filter allows an attacker to learn all addresses even if the bloom filter contained a lot of noise.

Here, the concept of Bloom filters is also used but with the client downloading filters instead of sharing filters. This way, the user can close in on blocks that might interest him for his addresses and download whole blocks as if he were a full node. The exact address one is interested in, does not get revealed that easily.

So far TransactionFinder parses the blocks folder's blk*.dat files and creates Bloom filters containing 500k elements (output addresses) each. The BloomFilter gets written to a folder.

Currently, 0.1% false positives was chosen, assuming clients have around 1000 addresses that they want to monitor. With fewer addresses, a laxer filter might make more sense. Also it is not clear to me if using half a filter can make sense. This way the filter could be streamed and interpreted during streaming and canceled if a negative result was confirmed for all relevant addresses.

Missing parts are:

* also add the input addresses
* provide filters for bigger and smaller spans
* adjust false positive probability
* implement a client library
* implement a web api
