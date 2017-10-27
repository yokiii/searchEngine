import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

public class ISE {
	public static void main(String[] args) throws Exception {
		 //No.1 ---------------------------------parse argument----------------------------------
		if(args.length != 6){
			System.out.println("Usage: <APIKey> <Engine ID> <r> <t> <q> <k>");
			System.exit(1); 
		}
		
		//API KEY
		String apiKey = args[0];
		
		//Account 
		String engineKey =args[1];
		
		// r between 1 and 4 
		Integer r = Integer.parseInt(args[2]);
		if(r <= 0 || r > 4){
			System.err.println("Please enter a valid r (between 1 and 4)");
		    System.exit(1);
		}
		String[] types = {"Live_In", "Located_In", "OrgBased_In", "Work_For"};
		String type = types[r-1];
		
		// t between 0 and 1
		double t = Double.parseDouble(args[3]);
		if(t < 0 || t > 1){
		      System.err.println("Please enter a valid t (between 0 and 1)");
		      System.exit(1);
		    } 
		
		// seed query
		String q = args[4];
		
		// number of output
		Integer k = Integer.parseInt(args[5]);
		if(k <= 0){
			System.err.println("Please enter valid number of output");
		    System.exit(1);
		}
		
		//Initialize X tuple set
		Set<Entry<String, Double>> X = Collections.emptySet();
		
		// Obtain top-10 Webpages for query q
		ArrayList<String> urlResult = Search(engineKey, apiKey, 10, q);
		for(int i = 0; i< urlResult.size(); i++){
			System.out.println(urlResult.get(i));
		}
		
		List<String> plainTextResult = new ArrayList<>();
		for(int i = 0; i<urlResult.size(); i++){
			try{
			Document doc = Jsoup.connect(urlResult.get(i)).timeout(1000).get();
			String text = doc.body().text();  /// Mark : Body or Text
			plainTextResult.add(text);
			}catch (IOException e){
				System.out.println(urlResult.get(i) + "cannot be extracted");
				continue;
			}
			
		}
		
		for(int j = 0; j<plainTextResult.size(); j++){
		System.out.println(plainTextResult.get(j));
		}
		
		
		
		
		
	}
	// Search for url
	 public static ArrayList<String> Search(String engineKey, String apiKey, int top, String query) throws IOException{
		 
			ArrayList<String> urlResult =new ArrayList<>();
		  	HttpTransport httpTransport = new NetHttpTransport();
		      JsonFactory jsonFactory = new JacksonFactory();
		      Customsearch customsearch = new Customsearch.Builder(httpTransport, jsonFactory, null).setApplicationName("E6111 Search Engine").build();
		      List<Result> resultList = null;
		      Customsearch.Cse.List list = customsearch.cse().list(query);
		      list.setKey(apiKey);
		      list.setCx(engineKey);
		      Search results = list.execute();
		      resultList = results.getItems();
		          if(resultList != null && resultList.size() > 0){
		              for(Result result: resultList){
		              	urlResult.add(result.getFormattedUrl());
		              }
		       }
		          return urlResult;
		      }
	

}
