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


public class MainEngine{
  
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
	  String mergingtitle = "";
	  String mergingcontent = "";
	  //---------------- relevant word----------------
	  // retrieve the passed title and description
	  for(int i = 0; i< rawResult.size(); i++){
		  if(rawResult.get(i).get(3) == "True"){
			  mergingtitle+= rawResult.get(i).get(0) + ";";
			  mergingcontent += rawResult.get(i).get(2)+";";
		  }
	  }
	  //initialize stopword list
	  RAMDirectory directory = new RAMDirectory();
	  try {
		IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(
		            Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36)));
		Document doc = new Document();
		doc.add(new Field("fieldname",mergingtitle,Field.Store.YES, Field.Index.ANALYZED));
		writer.addDocument(doc);
		writer.close();
	} catch (CorruptIndexException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (LockObtainFailedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	  
	  
	  
	  // find dif 
	  // 
	  return new String();
	  
  }
  
}
