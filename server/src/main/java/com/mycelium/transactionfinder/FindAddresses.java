package com.mycelium.transactionfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.ByteString;
import com.mycelium.transactionfinder.TransactionFinderProtos.*;

import javax.xml.bind.DatatypeConverter;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider;

public class FindAddresses {
    private static final String FILTER_FOLDER = "/tmp/";
    private static final String[] DUMMY_WALLET_ADDRESSES = new String[]{"15QDbdvbRoXcTEmyKey7d9CFcB82LdCzUC","15qygZYHwuDmSPq9nbFPkwoNciWQJYzyfm","14fdnj2zfMz2o3QAxDkJ8F9ZjoHVSqmvHV"};
    public static void main(String... args) {
        File dir = new File(FILTER_FOLDER);
        List<File> filterFiles = new ArrayList<>();
        String[] chld = dir.list();
        for (String fn : chld) {
            if(fn.startsWith("BF")) {
                filterFiles.add(new File(FILTER_FOLDER + fn));
            }
        }
        Collections.sort(filterFiles);
        for(File filterFile:filterFiles) {
            try {
                String fileName = FILTER_FOLDER + filterFile.getName();
                BlockBloomFilterList blockBloomFilterList = BlockBloomFilterList.parseFrom(new FileInputStream(fileName));
                for(BlockBloomFilterList.BlockBloomFilter blockBloomFilter: blockBloomFilterList.getFiltersList()) {
                    FilterBuilder builder = new FilterBuilder(blockBloomFilter.getSize(), blockBloomFilter.getHashes())
                            .hashFunction(HashProvider.HashMethod.valueOf(blockBloomFilter.getHashMethod().toString()));
                    BloomFilter<String> filter = builder.buildBloomFilter();
                    filter.getBitSet().or(BitSet.valueOf(blockBloomFilter.getBits().toByteArray()));
                    //System.out.println(BloomFilterConverter.toJson(filter).toString());
                    for(String address: DUMMY_WALLET_ADDRESSES) {
                        if(filter.contains(address)) {
                            ByteString blockHash = blockBloomFilter.getBlockHash();
                            System.out.println(address + " might be in block " + DatatypeConverter.printHexBinary(blockHash.toByteArray()));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
