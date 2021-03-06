package com.birdy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TweetController {

    @RequestMapping("/")
    public String index() {
        return "Hello!";
    }

    @RequestMapping(value = "/tweets", produces = "application/json; charset=utf-8")
    public String findTweets(@RequestParam(value = "q", required = false) String query,
                             @RequestParam(value = "limit", required = false) Integer limit,
                             @RequestParam(value = "lng", required = false) Double lng,
                             @RequestParam(value = "lat", required = false) Double lat,
                             @RequestParam(value = "offset", required = false) Integer offset,
                             @RequestParam(value = "radius", required = false) Integer radius) {

        try {
            LuceneWrapper lw = Main.luceneWrapper;
            if(limit == null) limit = 10;
            // Set radius equal to 100 miles if not set
            if(radius == null) radius = 160934;
            if(offset == null) offset = 0;

            BooleanQuery.Builder bq = new BooleanQuery.Builder();

            if(query != null && query.length() > 0) {
                String[] fields = {"title", "text", "user", "hashtags"};
                Map<String, Float> boosts = new HashMap<>() {{
                    put("title", 2.0f);
                    put("text", 1.0f);
                    put("user", 2.5f);
                    put("hashtags", 2.0f);
                }};
                Query baseQuery = new MultiFieldQueryParser(fields, lw.analyzer, boosts).parse(query);
                bq.add(baseQuery, BooleanClause.Occur.MUST);
            }
            if(lng != null && lat != null)
                bq.add(LatLonPoint.newDistanceQuery("location", lat, lng, radius), BooleanClause.Occur.MUST);

            TweetResponse response = lw.search(Main.indexReader, bq.build(), limit, offset);

            Gson gson = new Gson();
            return gson.toJson(response);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
