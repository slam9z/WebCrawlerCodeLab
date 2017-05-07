package io.bittiger.util;

import io.bittiger.ad.Ad;
import io.bittiger.crawler.AmazonCrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Created by kozue on 5/7/17.
 */
public class QueryUtilities {

    private static final String SEPARATOR = ",";

    public static QueryUtilities queryUtilitiesInstance;

    //Initialization-on-demand holder idiom - thread safe
    public static class QueryUtilitiesHolder{
        public static QueryUtilities queryUtilities = new QueryUtilities();
    }

    public static QueryUtilities getInstance() {
        return QueryUtilitiesHolder.queryUtilities;
    }

    private static List<String> nGrams(String query){
        List<String> res = new ArrayList<String>();
        if (query == null || query.length() == 0)
            return res;
        String[] words = query.split(" ");
        for(int n = 2; n < words.length; n++) {
            for (int i = 0; i < words.length - n + 1; i++) {
                res.add(concat(words, i, i + n));
            }
        }
        return res;

    }

    private static String concat(String[] w, int start, int end){
        StringBuilder sb = new StringBuilder();
        for(int j = start; j < end; j++ ){
            sb.append((j > start ? " ": "") + w[j]);
        }
        return sb.toString();
    }

    public static List<Ad> generateAdsByFile(String rawDataFile, String proxy_file, String log_file){

        List<Ad> res = new ArrayList<>();
        AmazonCrawler amzCrawler = new AmazonCrawler(proxy_file, log_file);
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(rawDataFile),
                StandardCharsets.UTF_8)) {

            Stream<String> lines = reader.lines();
            List<String[]> keywords = lines.map(line -> (line.toLowerCase().split(SEPARATOR)))
                    .collect(toList());

            for(String[] kword : keywords){
                String query = kword[0];
                int campaignId = Integer.parseInt(kword[2]);
                int queryGroupId = Integer.parseInt(kword[3]);

                List<Ad> adslist = amzCrawler.GetAdBasicInfoByQuery(query,getRandomBidPrice(0.0,50.0),campaignId+1, queryGroupId);
                String category = adslist.get(0).category;

                List<String> subqueryList = nGrams(query);
                for (String subquery: subqueryList) {
                    if (!query.contains(subquery)) {
                        List<Ad> subadsList = amzCrawler.GetAdBasicInfoByQuery(subquery,getRandomBidPrice(0.0,50.0),campaignId+1, queryGroupId);
                        String subCategory = subadsList.get(0).category;
                        if (subCategory.equals(category)) {
                            adslist.addAll(subadsList);
                        }
                    }
                }
                res.addAll(adslist);
            }

        } catch (IOException ex) {
            System.err.println("IO Error: " + ex.getMessage());
        }
        return res;
    }

    private static double getRandomBidPrice(double rangeMin, double rangeMax){
        Random rand = new Random();
        double randomValue = rangeMin + (rangeMax - rangeMin) * rand.nextDouble();
        return randomValue;
    }



}
