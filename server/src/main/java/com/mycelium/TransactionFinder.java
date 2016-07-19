package com.mycelium;

import com.google.common.collect.Range;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;

public class TransactionFinder {
    private static final String blocksFolder = "/media/leo/edcb37f6-df83-4634-a709-6c7dc9ab6287/bitcoin/blocks/";
    private static final String filterFolder = blocksFolder;
    private static final int targetSize = 500000;
    private static final double targetFalsePositive = 0.001;
    private static final int firstBlock = 0;

    public static void main(String... args) throws BlockStoreException {
        MainNetParams mainNetParams = MainNetParams.get();
        Context.getOrCreate(mainNetParams);
        NetworkParameters np = new MainNetParams();
        List<File> blockChainFiles = new ArrayList<>();
        File dir = new File(blocksFolder);
        String[] chld = dir.list();
        for (String fn : chld) {
            if(fn.endsWith(".dat") && fn.matches(".*blk.*dat")) {
                blockChainFiles.add(new File(blocksFolder + fn));
            }
        }
        BlockFileLoader bfl = new BlockFileLoader(np, blockChainFiles);

        long rangeStart = firstBlock;
        long addressCount = 0;
        long blockHeight = 0;
        BloomFilter<String> currentBloomFilter = new FilterBuilder(targetSize, targetFalsePositive).<String>buildBloomFilter();

        // Iterate over the blocks in the dataset.

        for (Block block : bfl) {
            if(blockHeight >= firstBlock) {
                for (Transaction tx : block.getTransactions()) {
                    // TODO: 7/18/16 figure out which addresses spent something.
                    // parse which address received something
                    for (TransactionOutput tout : tx.getOutputs()) {
                        try {
                            Address p2PKH = tout.getAddressFromP2PKHScript(mainNetParams);
                            // Address p2SH = tout.getAddressFromP2SH(mainNetParams);
                            // ignoring p2sh, op RETURN and unknown scripts for now
                            if (p2PKH != null) {
                                currentBloomFilter.add(p2PKH.toString());
                                addressCount++;
                            }
                        } catch (ScriptException ex) {
                            System.out.println(ex.getMessage() + ": ignoring.");
                        }
                    }
                }
                if (addressCount > targetSize) {
                    addressCount = currentBloomFilter.getEstimatedPopulation().longValue();
                    if (addressCount > targetSize) {
                        Range<Long> range = Range.closed(rangeStart, blockHeight);
                        storeBloomFilter(range, currentBloomFilter.getBitSet());
                        System.out.println(range + " estimated population is " + addressCount);
                        currentBloomFilter.clear();
                        rangeStart = blockHeight + 1;
                        addressCount = 0;
                    }
                }
            }
            blockHeight++;
        }
    }

    private static void storeBloomFilter(Range<Long> range, BitSet bitSet) {
        byte[] bt = bitSet.toByteArray();
        String filename = String.format("%sBFbits-%07d-%07d.dat", filterFolder, range.lowerEndpoint(), range.upperEndpoint());
        File outFile = new File(filename);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.write(bt);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
