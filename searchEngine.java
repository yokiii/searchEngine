package adbProject1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.util.AttributeFactory;

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
	  private static String query;
	  private static List<List<String>> rawResult =new ArrayList<List<String>>();
	  
	  private final static int top = 10;
	  
	  
	  public static void main(String[] args) throws Exception {
		  
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
	    query = args[2];
	    
	    //Preicsion
	    precision = Double.parseDouble(args[3]);
	    if(precision < 0 || precision > 1){
	      System.err.println("Please enter a valid precision number");
	      System.exit(1);
	    } 
	    //--------------------------------------------------------
	    
	    //No.2 ----------------------------------Search && Parse
	    
	    //---------------------------------------------------
	    
	      Search(engineKey, apiKey, top, query);  //return rawResult
	      double realPrecision=Feedback(); //return double real prescision by user;
	      if(checkPrecision(realPrecision)){
	        System.out.println("----------------------reached ideal precision----------------------");
	        System.exit(1);
	      }
	      else{
	    	  System.out.println(realPrecision);
	      }
	      
	      while(true){
	        query = expand(query);
	        Search(engineKey, apiKey, top, query);
	        realPrecision = Feedback();
	        if(checkPrecision(realPrecision)){
	        System.out.println("reached ideal precision");
	        System.exit(1);
	        }
	       
	        
	        
	}
	 

	  }
	  
	  // return the search query result (already finished parsing)
	  public static void Search(String engineKey, String apiKey, int top, String query) throws IOException{
	 
	  	
	  	HttpTransport httpTransport = new NetHttpTransport();
	      JsonFactory jsonFactory = new JacksonFactory();
	      Customsearch customsearch = new Customsearch(httpTransport, jsonFactory,null);
	      List<Result> resultList = null;
	      Customsearch.Cse.List list = customsearch.cse().list(query);
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
	  
	  //collect user feedback and get the real precision 
	  public static double Feedback(){
		  int count = 0;
		  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		  for(int i = 0; i< rawResult.size(); i++){
			  	System.out.printf("Result %d\n", i + 1);
				System.out.println("-----------------------------");
				System.out.printf("Title: %s\n", rawResult.get(i).get(0));
				System.out.printf("Display Url: %s\n", rawResult.get(i).get(1));
				System.out.printf("Description: %s\n", rawResult.get(i).get(2));
				System.out.print("Do you think is relevant or not? (Y/N) :");
				while(true){
				try {
					String peranswer = br.readLine();
					if(peranswer.equalsIgnoreCase("Y")){
						
						rawResult.get(i).add("True");
						count++;
						break;
					} 
					else if(peranswer.equalsIgnoreCase("N")){
						rawResult.get(i).add("False");
						break;
					}
					else{
						System.out.println("Please input a valid response");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println("Please Input valid response. ");
				}
				}
				
			  
		  }
		  return (double) count / top;
	  }
	  
	  
	  public static String expand(String oldquery){
		  List<String[]> mergingtitle = new ArrayList<String[]>();
		  List<String[]> mergingcontent = new ArrayList<String[]>();

		  //---------------- relevant word----------------
		  // retrieve the passed title and description
		  for(int i = 0; i< rawResult.size(); i++){
			  if(rawResult.get(i).get(3) == "True"){
				  String[] title = rawResult.get(i).get(0).split(" ");
				  mergingtitle.add(title);
				  String[] content = rawResult.get(i).get(2).split(" ");
				  mergingcontent.add(content);
			  }
		  }
		  Double entryInverse = (double) (1/rawResult.size());
		  HashMap<String, Double> titleNew = new HashMap<String, Double>();
			for (String[] t : mergingtitle) {
					for(String s: t){
						if (StopWords.stopWordsList.contains(s))
							continue;
						else if (titleNew.containsKey(s)) {
							double freq = titleNew.get(s) +entryInverse;
							titleNew.put(s, freq);
						}
						else{
							titleNew.put(s,entryInverse);
						}		
					}
			}
			 //HashMap<String, Integer> contentNew = new HashMap<String, Integer>();
			 for (String[] t : mergingcontent) {
					for(String s: t){
						if (StopWords.stopWordsList.contains(s))
							continue;
						else if (titleNew.containsKey(s)) {
							double freq = titleNew.get(s) +entryInverse;
							titleNew.put(s, freq);
						}
						else{
							titleNew.put(s,entryInverse);
						}	
					}
			 }
			 String[] queryModify = query.split(" ");
		        for(String s: queryModify){
					if (StopWords.stopWordsList.contains(s))
						continue;
					else if (titleNew.containsKey(s)) {
						double freq = titleNew.get(s) +1;
						titleNew.put(s, freq);
					}
					else{
						titleNew.put(s,1.0);
					}	
		        }
			 Set<Entry<String, Double>> entries = titleNew.entrySet();
			 Comparator<Entry<String, Double>> valueComparator = new Comparator<Entry<String, Double>>() {

			@Override
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				Double v1 = o1.getValue(); 
				Double v2 = o2.getValue();
				return v1.compareTo(v2);
			} 
		   };
	        List<Entry<String, Double>> listOfEntries = new ArrayList<Entry<String, Double>>(entries);
	        Collections.sort(listOfEntries, valueComparator);
	        List<String> qReset = new ArrayList<String>();
	        int qSize=5;
	        if(query.length()>5){
	        	qSize=10;
	        }
	        for(int i=0; i<=qSize;i++){
	        	qReset.add(listOfEntries.get(i).getKey());
	        }
	        StringBuilder sb = new StringBuilder();
	        for(String s: qReset){
	        	sb.append(s);
	        	sb.append(" ");
	        }
		  return sb.toString();
		  
	  }
	  

}
