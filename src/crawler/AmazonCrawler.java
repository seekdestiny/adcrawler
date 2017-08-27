package crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.net.*;

//import org.apache.log4j.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ad.Ad;

/**
 * Created by jiayangan on 10/13/16.
 */


public class AmazonCrawler {
    private static final String AMAZON_QUERY_URL = "https://www.amazon.com/s/ref=nb_sb_noss?field-keywords=";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36";
    private final String authUser = "ganjiayan";
    private final String authPassword = "Bi7jU9TI";
    private List<String> proxyList;
    private List<String> titleList;
    private List<String> categoryList;
    private Set<String> urlSet;

    BufferedWriter logBFWriter;
    BufferedWriter urlBFWriter;

    private int index = 0;
    private int id = 2000;

    public AmazonCrawler(String proxy_file, String log_file, String url_file) {
        initProxyList(proxy_file);

        initHtmlSelector();

        initLog(log_file);

        initUrlHistory(url_file);
    }

    public void cleanup() {
        if (logBFWriter != null) {
            try {
                logBFWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (urlBFWriter != null) {
            try {
                urlBFWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initProxyList(String proxy_file) {
        proxyList = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(proxy_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                String ip = fields[0].trim();
                proxyList.add(ip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                authUser, authPassword.toCharArray());
                    }
                }
        );

        System.setProperty("http.proxyUser", authUser);
        System.setProperty("http.proxyPassword", authPassword);
        System.setProperty("socksProxyPort", "61336"); // set proxy port
    }

    private void initHtmlSelector() {
        titleList = new ArrayList<String>();
        titleList.add(" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > div:nth-child(1)  > a > h2");
        titleList.add(" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > a > h2");

        categoryList = new ArrayList<String>();
        categoryList.add("#refinements > div.categoryRefinementsSection > ul.forExpando > li > a > span.boldRefinementLink");
        categoryList.add("#refinements > div.categoryRefinementsSection > ul.forExpando > li:nth-child(1) > a > span.boldRefinementLink");


    }

    private void initLog(String log_path) {
        try {
            File log = new File(log_path);
            // if file doesnt exists, then create it
            if (!log.exists()) {
                log.createNewFile();
            }
            FileWriter fw = new FileWriter(log.getAbsoluteFile());
            logBFWriter = new BufferedWriter(fw);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initUrlHistory(String url_file) {
        urlSet = new HashSet<String>();
        try {
            File urlHistory = new File(url_file);
            // if file doesnt exists, then create it
            if (!urlHistory.exists()) {
                urlHistory.createNewFile();
            }
            FileWriter fw = new FileWriter(urlHistory.getAbsoluteFile(), true);
            urlBFWriter = new BufferedWriter(fw);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(url_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                urlSet.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setProxy() {
        //rotate
        if (index == proxyList.size()) {
            index = 0;
        }
        String proxy = proxyList.get(index);
        System.setProperty("socksProxyHost", proxy); // set proxy server
        index++;
    }

    private void testProxy() {
        System.setProperty("socksProxyHost", "199.101.97.149"); // set proxy server
        System.setProperty("socksProxyPort", "61336"); // set proxy port
        String test_url = "http://www.toolsvoid.com/what-is-my-ip-address";
        try {
            Document doc = Jsoup.connect(test_url).userAgent(USER_AGENT).timeout(10000).get();
            String iP = doc.select("body > section.articles-section > div > div > div > div.col-md-8.display-flex > div > div.table-responsive > table > tbody > tr:nth-child(1) > td:nth-child(2) > strong").first().text(); //get used IP.
            System.out.println("IP-Address: " + iP);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public List<Ad> GetAdBasicInfoByQuery(String query, double bidPrice,int campaignId,int queryGroupId) {
        List<Ad> products = new ArrayList<>();
        try {
            //testProxy();
            setProxy();

            String url = AMAZON_QUERY_URL + query;
            HashMap<String,String> headers = new HashMap<String,String>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, sdch, br");
            headers.put("Accept-Language", "en-US,en;q=0.8");
            Document doc = Jsoup.connect(url).headers(headers)
                                             .userAgent(USER_AGENT).timeout(100000).get();

            //System.out.println(doc.text());
            //#s-results-list-atf
            //Elements results = doc.select("#s-results-list-atf").select("li");
            //Elements results = doc.getElementById("s-results-list-atf").select("li");
            //System.out.println(doc.select("#s-results-list-atf").html());
            Elements results = doc.select("li[data-asin]");
            System.out.println("num of results = " + results.size());
            for(int i = 0; i < results.size();i++) {
                Ad ad = new Ad();
                ad.adId = id++;
                ad.query = query;
                ad.query_group_id = queryGroupId;
                ad.keyWords = new ArrayList<>();
                //#result_2 > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > div:nth-child(1) > a > h2
                //#result_3 > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > div:nth-child(1) > a > h2
                for (String title : titleList) {
                    String title_ele_path = "#result_"+Integer.toString(i)+ title;
                    Element title_ele = doc.select(title_ele_path).first();
                    if(title_ele != null) {
                        System.out.println("title = " + title_ele.text());
                        ad.title = title_ele.text();
                        ad.keyWords = Utility.cleanedTokenize(ad.title);
                        break;
                    }
                }

                if (ad.title == "") {
                    logBFWriter.write("cannot parse title for query: " + query);
                    logBFWriter.newLine();
                    continue;
                }

                String thumbnail_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-left > div > div > a > img";
                Element thumbnail_ele = doc.select(thumbnail_path).first();
                if(thumbnail_ele != null) {
                    //System.out.println("thumbnail = " + thumbnail_ele.attr("src"));
                    ad.thumbnail = thumbnail_ele.attr("src");
                } else {
                    logBFWriter.write("cannot parse thumbnail for query:" + query + ", title: " + ad.title);
                    logBFWriter.newLine();
                    continue;
                }

                String detail_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-left > div > div > a";
                Element detail_url_ele = doc.select(detail_path).first();
                if(detail_url_ele != null) {
                    String detail_url = detail_url_ele.attr("href");
                    //System.out.println("detail = " + detail_url);
                    ad.detail_url = detail_url.replaceAll("ref.*$", "");
                } else {
                    logBFWriter.write("cannot parse detail for query:" + query + ", title: " + ad.title);
                    logBFWriter.newLine();
                    continue;
                }


                String brand_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div.a-row.a-spacing-small > div > span:nth-child(2)";
                Element brand = doc.select(brand_path).first();
                if(brand != null) {
                    //System.out.println("brand = " + brand.text());
                    ad.brand = brand.text();
                }
                //#result_2 > div > div > div > div.a-fixed-left-grid-col.a-col-right > div:nth-child(3) > div.a-column.a-span7 > div.a-row.a-spacing-none > a > span > span > span
                ad.bidPrice = bidPrice;
                ad.campaignId = campaignId;
                ad.price = 0.0;

                String price_whole_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div:nth-child(3) > div.a-column.a-span7 > div.a-row.a-spacing-none > a > span > span > span";
                String price_fraction_path = "#result_"+Integer.toString(i)+" > div > div > div > div.a-fixed-left-grid-col.a-col-right > div:nth-child(3) > div.a-column.a-span7 > div.a-row.a-spacing-none > a > span > span > sup.sx-price-fractional";
                Element price_whole_ele = doc.select(price_whole_path).first();
                if(price_whole_ele != null) {
                    String price_whole = price_whole_ele.text();
                    //System.out.println("price whole = " + price_whole);
                    //remove ","
                    //1,000
                    if (price_whole.contains(",")) {
                        price_whole = price_whole.replaceAll(",","");
                    }

                    try {
                        ad.price = Double.parseDouble(price_whole);
                    } catch (NumberFormatException ne) {
                        // TODO Auto-generated catch block
                        ne.printStackTrace();
                        //log
                    }
                }

                Element price_fraction_ele = doc.select(price_fraction_path).first();
                if(price_fraction_ele != null) {
                    //System.out.println("price fraction = " + price_fraction_ele.text());
                    try {
                        ad.price = ad.price + Double.parseDouble(price_fraction_ele.text()) / 100.0;
                    } catch (NumberFormatException ne) {
                        ne.printStackTrace();
                    }
                }
                //System.out.println("price = " + ad.price );

                //category
                for (String category : categoryList) {
                    Element category_ele = doc.select(category).first();
                    if(category_ele != null) {
                        //System.out.println("category = " + category_ele.text());
                        ad.category = category_ele.text();
                        break;
                    }
                }
                if (ad.category  == "") {
                    logBFWriter.write("cannot parse category for query:" + query + ", title: " + ad.title);
                    logBFWriter.newLine();
                    continue;
                }

                if (urlSet.contains(ad.detail_url)) {
                    continue;
                }

                urlSet.add(ad.detail_url);
                urlBFWriter.write(ad.detail_url);
                urlBFWriter.newLine();
                products.add(ad);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return products;
    }
}
