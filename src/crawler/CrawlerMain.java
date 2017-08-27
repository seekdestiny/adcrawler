package crawler;

/**
 * Created by jiayangan on 10/12/16.
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ad.Ad;

public class CrawlerMain {
    public static void main(String[] args) throws IOException {
        if(args.length < 2)
        {
            System.out.println("Usage: Crawler <rawQueryDataFilePath> <adsDataFilePath> <proxyFilePath> <logFilePath> <urlFilePath>");
            System.exit(0);
        }
        ObjectMapper mapper = new ObjectMapper();
        String rawQueryDataFilePath = args[0];
        String adsDataFilePath = args[1];
        String proxyFilePath = args[2];
        String logFilePath = args[3];
        String urlFilePath = args[4];
        AmazonCrawler crawler = new AmazonCrawler(proxyFilePath, logFilePath, urlFilePath);
        File file = new File(adsDataFilePath);
        // if file doesnt exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        Set<String> queryHistory = new HashSet<>();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        try (BufferedReader br = new BufferedReader(new FileReader(rawQueryDataFilePath))) {

            String line;
            while ((line = br.readLine()) != null) {
                if(line.isEmpty())
                    continue;
                System.out.println(line);
                String[] fields = line.split(",");
                String query = fields[0].trim();
                double bidPrice = Double.parseDouble(fields[1].trim());
                int campaignId = Integer.parseInt(fields[2].trim());
                int queryGroupId = Integer.parseInt(fields[3].trim());
                if (queryHistory.contains(query)) {
                    continue;
                }
                queryHistory.add(query);
                List<Ad> ads =  crawler.GetAdBasicInfoByQuery(query, bidPrice, campaignId, queryGroupId);
                for(Ad ad : ads) {
                    String jsonInString = mapper.writeValueAsString(ad);
                    //System.out.println(jsonInString);
                    bw.write(jsonInString);
                    bw.newLine();
                }
                Thread.sleep(5000);

                if (ads.size() == 0) {
                    continue;
                }

                String category = ads.get(0).category;
                System.out.println(category);

                List<String> subQueries = Utility.getNgram(query);
                for (String subQuery : subQueries) {
                    if (queryHistory.contains(subQuery)) continue;
                    queryHistory.add(subQuery);
                    System.out.println(subQuery);
                    List<Ad> subAds = crawler.GetAdBasicInfoByQuery(subQuery, bidPrice, campaignId, queryGroupId);
                    if (subAds.size() == 0) {
                        continue;
                    }
                    for(Ad subAd : subAds) {
                       if (subAd.category == null || !subAd.category.equals(category)) {
                            continue;
                        }
                        String jsonInString = mapper.writeValueAsString(subAd);
                        //System.out.println(jsonInString);
                        bw.write(jsonInString);
                        bw.newLine();
                    }
                    Thread.sleep(5000);
                }
            }
            bw.close();
        }catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        crawler.cleanup();
    }
}
