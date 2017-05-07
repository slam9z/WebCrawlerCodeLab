package io.bittiger.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kozue on 5/6/17.
 */
public class AdsUtilities {

    private static final Version _LUCENE_VERSION = Version.LUCENE_40;

    private static final List<String> stopWords = Arrays.asList(
            ".", "\"", ",", "?", "!", ":", ";", "(", "[",
            "]", "{", "}", "&", "/", "...",
            "-", "+", "*", "|", "),"
    );

    private static final CharArraySet stopWordsCharArraySet = new CharArraySet(_LUCENE_VERSION, stopWords, false);

    public AdsUtilities() {
    }

    public static class AdsUtilitiesHolder{
        public static AdsUtilities adsUtilities = new AdsUtilities();
    }

    public static AdsUtilities getInstance(){
        return AdsUtilitiesHolder.adsUtilities;
    }

    public static String cleanData(String input) {
        StringReader reader = new StringReader(input.toLowerCase());
        Tokenizer tokenizer = new StandardTokenizer(_LUCENE_VERSION, reader);
        TokenStream tokenStream = new StandardFilter(_LUCENE_VERSION, tokenizer);
        tokenStream = new StopFilter(_LUCENE_VERSION, tokenStream, stopWordsCharArraySet);

        StringBuilder sb = new StringBuilder();
        CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String term = charTermAttribute.toString();
                sb.append(term + " ");
            }

        } catch (IOException e) {
            System.err.println("Error");
            e.printStackTrace();
        } finally {
            try {
                tokenStream.end();
                tokenStream.close();
                tokenizer.close();
            } catch (IOException ioError){

            }
        }
        return sb.toString();
    }

    public static void depudeAds(String inFile, String outFile) throws IOException {

        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
        BufferedReader reader = Files.newBufferedReader(Paths.get(inFile));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = null;
        Set<Integer> url_set = new HashSet<>();
        int id = 2000;
        String line = "";
        while ((line = reader.readLine()) != null) {
            jsonObject = jsonParser.parse(line.trim()).getAsJsonObject();
            String detail_url = jsonObject.get("detail_url").getAsString();
            String title = jsonObject.get("title").getAsString();
            if(url_set.add(detail_url.hashCode())){
                String query = jsonObject.get("query").getAsString();
                String tokens = cleanData(query);
                jsonObject.addProperty("query", query + " " + tokens);
                jsonObject.addProperty("id",id);
                if(jsonObject.get("price").getAsDouble()==0.0){
                    jsonObject.addProperty("price", 30 + Math.random()*(480-30+1));
                }
                String keywords = cleanData(title);
                jsonObject.addProperty("keyWords",keywords);
                jsonObject.addProperty("title",title);
                id += 1;
                objectMapper.writeValue(writer,jsonObject);
            }
        }
    }


}
