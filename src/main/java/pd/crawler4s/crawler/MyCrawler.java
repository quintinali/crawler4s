package pd.crawler4s.crawler;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import pd.crawler4s.driver.ESdriver;


public class MyCrawler extends WebCrawler{
  private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
      + "|png|mp3|mp3|zip|gz))$");
  private final String index = "pd";
  private final String type = "crawler4j";
  public static ESdriver esd = new ESdriver();
  public static BulkProcessor bulkProcessor = BulkProcessor.builder(
      esd.client,
      new BulkProcessor.Listener() {
        public void beforeBulk(long executionId,
            BulkRequest request) {} 

        public void afterBulk(long executionId,
            BulkRequest request,
            BulkResponse response) {} 

        public void afterBulk(long executionId,
            BulkRequest request,
            Throwable failure) {
          System.out.println("Bulk fails!");
          throw new RuntimeException("Caught exception in bulk: " + request + ", failure: " + failure, failure);
        } 
      }
      )
      .setBulkActions(1000) 
      .setBulkSize(new ByteSizeValue(1, ByteSizeUnit.GB)) 
      .setConcurrentRequests(1) 
      .build();



  @Override
  public boolean shouldVisit(Page referringPage, WebURL url) {
    String href = url.getURL().toLowerCase();
    return !FILTERS.matcher(href).matches()
        && !href.contains("http://ssd.jpl.nasa.gov/sbdb.cgi?sstr=");
  }

  /**
   * This function is called when a page is fetched and ready
   * to be processed by your program.
   */
  @Override
  public void visit(Page page) {
    String url = page.getWebURL().getURL();
    if (page.getParseData() instanceof HtmlParseData) {
      HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
      String text = htmlParseData.getText();
      text = text.replaceAll("[^\\S\\r\\n]+", " ").replaceAll("\\n+", " ").replaceAll("\\s+", " ");
      IndexRequest ir;
      try {
        ir = new IndexRequest(index, type).source(jsonBuilder()
            .startObject()
            .field("URL", url)
            .field("Title", htmlParseData.getTitle())
            .field("Time", new Date())
            .field("content", text)
            .field("fileType", "webpage")
            .endObject());
        bulkProcessor.add(ir);
      } catch (IOException e) {
        e.printStackTrace();
      } 
    }
  }

  public MyCrawler() {
  }

}
