package adbProject1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

public class searchEngine {
	  private static String engineKey;
	  private static String apiKey;
	  private static double precision;
	  //private static double 
	  private static String[] query;
	  private static List<List<String>> rawResult =new ArrayList<List<String>>();
	  private static int top = 10;
	  
	  public static void main(String[] args) throws Exception{
	    //No.1 ---------------------------------parse argument
	    if(args.length != 4){
	        System.err.println("Usage: MainEngine <EngineKey> <ApiKey> <query> <precision>");
	        System.exit(1);
	    }
	    // Engine Key
	    engineKey = args[0];
	    // API Key
	    apiKey = args[1];
	    //Query
	    query = args[2].split(" ");
	    String query1 = args[2];
	    //Preicsion
	    precision = Double.parseDouble(args[3]);
	    if(precision < 0 || precision > 1){
	        System.err.println("Please enter a valid precision number");
	        System.exit(1);
	    } 
	    //--------------------------------------------------------
	    
	    //No.2 ----------------------------------Search && Parse
	    
	    //---------------------------------------------------
	      Search(engineKey, apiKey, top, query, query1);  //return rawResult
	     // double realPrecision=Feedback(); //return double real prescision by user;
	      //if(checkPrecision(realPrecision)){
	        //  System.out.println("reached ideal precision");
	         // System.exit(1);
	     // }
	      
	      //  while(true){
	        //query = expand(query);
	        //Search(engineKey, apiKey, top, query,query1);
	        //Feedback();
	           // if(checkPrecision(realPrecision)){
	               // System.out.println("reached ideal precision");
	                //System.exit(1);
	           // }
	       // }
	  }
	    public static void Search(String engineKey, String apiKey, int top, String[] query, String query1) throws IOException{
	        //URL url = new URL ("https://www.googleapis.com/customsearch/v1?key=" +apiKey+ "&amp;cx=" +engineKey+ "&amp;q=" +query+"&amp;alt=json");
	        //HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        //conn.setRequestMethod("GET");
	        //conn.setRequestProperty("Accept", "application/json");
	        //BufferedReader br = new BufferedReader(new InputStreamReader ( ( conn.getInputStream() ) ) );
	         // conn.getInputStream();
	        //conn.disconnect();
	    	
	    	HttpTransport httpTransport = new NetHttpTransport();
	        JsonFactory jsonFactory = new JacksonFactory();
	        Customsearch customsearch = new Customsearch(httpTransport, jsonFactory,null);
	        List<Result> resultList = null;
	        Customsearch.Cse.List list = customsearch.cse().list(query1);
	        list.setKey(apiKey);
	        list.setCx(engineKey);
	        Search results = list.execute();
	        resultList = results.getItems();
                if(resultList != null && resultList.size() > 0){
                    for(Result result: resultList){
                    	List<String> rst = new ArrayList<String>();
                        rst.add(result.getHtmlTitle());
                        rst.add(result.getFormattedUrl());
                        rst.add(result.getHtmlSnippet());
                        rawResult.add(rst);
                   
                        //System.out.println(result.getHtmlSnippet());
                       // System.out.println("----------------------------------------");
                    }
             }
	        }
	    
	    // return the check precision result
	    public static boolean checkPrecision(double realPrecision){
	        if(realPrecision == 0 || realPrecision >= precision) return true;
	        else return false;
	    }
	    
	    //parse the query and collect user feedback and get the real precision
	    public static double Feedback(){
	    }


}
