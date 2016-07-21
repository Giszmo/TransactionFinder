package com.mycelium.transactionfinder;

import com.google.protobuf.ByteString;
import com.mycelium.transactionfinder.TransactionFinderProtos.*;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.BlockFileLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;

public class BlockchainParser {
    private static final String BLOCKS_FOLDER = "/media/leo/edcb37f6-df83-4634-a709-6c7dc9ab6287/bitcoin/blocks/";
    private static final String FILTER_FOLDER = "/tmp/";
    private static final double TARGET_FALSE_POSITIVE = 0.000001;
    private static final int FIRST_BLOCK = 0;

    public static void main(String... args) throws BlockStoreException {
        MainNetParams mainNetParams = MainNetParams.get();
        Context.getOrCreate(mainNetParams);
        NetworkParameters np = new MainNetParams();
        List<File> blockChainFiles = new ArrayList<>();
        File dir = new File(BLOCKS_FOLDER);
        String[] chld = dir.list();
        for (String fn : chld) {
            if(fn.matches(".*blk.*dat")) {
                blockChainFiles.add(new File(BLOCKS_FOLDER + fn));
            }
        }

        Collections.sort(blockChainFiles);
        BlockFileLoader bfl = new BlockFileLoader(np, blockChainFiles);
        BlockBloomFilterList.Builder blockBloomFilterList = BlockBloomFilterList.newBuilder();
        int byteCounter = 0;

        int rangeStart = FIRST_BLOCK;
        int blockHeight = 0;
        BloomFilter<String> currentBloomFilter;

        for (Block block : bfl) {
            BlockBloomFilterList.BlockBloomFilter.Builder blockBloomFilter = BlockBloomFilterList.BlockBloomFilter.newBuilder();
            blockBloomFilter.setBlockHash(ByteString.copyFrom(block.getHash().getBytes()));
            if(blockHeight >= FIRST_BLOCK) {
                int addressCount = 0;
                for (Transaction tx : block.getTransactions()) {
                    addressCount += tx.getOutputs().size();
                }
                currentBloomFilter = new FilterBuilder(addressCount, TARGET_FALSE_POSITIVE).<String>buildBloomFilter();
                for (Transaction tx : block.getTransactions()) {
                    // TODO: 7/18/16 figure out which addresses spent something.
                    // parse which address received something
                    for (TransactionOutput tout : tx.getOutputs()) {
                        try {
                            // Address p2SH = tout.getAddressFromP2SH(mainNetParams);
                            // ignoring p2sh, op RETURN and unknown scripts for now
                            Address p2PKH = tout.getAddressFromP2PKHScript(mainNetParams);
                            if (p2PKH != null) {
                                currentBloomFilter.add(p2PKH.toString());
                            }
                        } catch (ScriptException ex) {
                            System.out.println(ex.getMessage() + ": ignoring.");
                        }
                    }
                }
                blockBloomFilter
                        .setSize(currentBloomFilter.getSize())
                        .setHashes(currentBloomFilter.getHashes())
                        .setHashMethod(BlockBloomFilterList.HashMethod.MD5)
                        .setBits(ByteString.copyFrom(currentBloomFilter.getBitSet().toByteArray()));
                blockBloomFilterList.addFilters(blockBloomFilter.build());
                byteCounter += currentBloomFilter.getBitSet().length() / 8 + 32 + 5 + 4 + 4;
                if(byteCounter > 1000000) {
                    storeBloomFilters(rangeStart, blockHeight, blockBloomFilterList);
                    rangeStart = blockHeight+1;
                    byteCounter = 0;
                    blockBloomFilterList = BlockBloomFilterList.newBuilder();
                }
            }
            blockHeight++;
        }
    }

    private static void storeBloomFilters(int blockHeightStart, int blockHeightStop, BlockBloomFilterList.Builder builder) {
        String filename = String.format("%sBFbits-%07d-%07d.dat", FILTER_FOLDER, blockHeightStart, blockHeightStop);
        try {
            builder.build().writeTo(new FileOutputStream(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
