import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;



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
  
  
  private final static int top = 10;
  
  
  public static void main(String[] args) throws Exception {
	  
    //No.1 ---------------------------------parse argument----------------------------------
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
    
    //No.2 ----------------------------------Main: Search && Parse && Expand-----------------------------

    List<List<String>> rawResult = Search(engineKey, apiKey, top, query);  //return rawResult
    double realPrecision=Feedback(rawResult); //return double real prescision by user;
      if(checkPrecision(realPrecision)){
        System.out.println("Already reached the ideal precision!");
        System.exit(1);
      }
      else{
    	  System.out.println(realPrecision);
      }
      
      while(true){
        String newquery = expand(query, rawResult);
        rawResult=Search(engineKey, apiKey, top, newquery);
        realPrecision = Feedback(rawResult);
        if(checkPrecision(realPrecision)){
        System.out.println("After Expansion, we reached the ideal precision");
        System.exit(1);
        	}      
      }
  }
  //------------------------------------Supporting Method below -------------------------------------------
  
  // return the raw search query result (already finished parsing)
  public static List<List<String>> Search(String engineKey, String apiKey, int top, String query) throws IOException{
 
	List<List<String>> rawResult =new ArrayList<List<String>>();
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
              	List<String> rst = new ArrayList<String>();
                  rst.add(result.getHtmlTitle());
                  rst.add(result.getFormattedUrl());
                  rst.add(result.getHtmlSnippet());
                  rawResult.add(rst);

              }
       }
          return rawResult;
      }
  

  
  // return the check precision result
  public static boolean checkPrecision(double realPrecision){
    if(realPrecision == 0 || realPrecision >= precision) return true;
    else return false;
  }
  
  //collect user feedback and return back the real precision 
  public static double Feedback(List<List<String>> rawResult){
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
  
  
  public static String expand(String querry,List<List<String>> rawResult) throws Exception{
	  //get relevant result
	  List<String[]> mergingtitle = new ArrayList<String[]>();
	  List<String[]> mergingcontent = new ArrayList<String[]>();
	  for(int i = 0; i< rawResult.size(); i++){
		  if(rawResult.get(i).get(3) == "True"){
			  String rawTitle = rawResult.get(i).get(0).replaceAll("[^A-Za-z0-9]", " ").toLowerCase();
			  String[] title = rawTitle.split(" ");
			  mergingtitle.add(title);
			  String rawContent = rawResult.get(i).get(2).replaceAll("[^A-Za-z0-9]", " ").toLowerCase();
			  String[] content = rawContent.split(" ");
			  mergingcontent.add(content);
		  }
	  }
	  
	  //stop word
	  HashSet<String> stopwSet = transfer();
	  
	  //calculate df of all words
	 // Double entryInverse = (double) (1/rawResult.size());
	  HashMap<String, Double> weightedTitle = new HashMap<String, Double>();
	  for (String[] title : mergingtitle) {
		  for(String s: title){
			  if (stopwSet.contains(s)) continue;
			  else if (weightedTitle.containsKey(s)) {
				  double freq = weightedTitle.get(s) +7;
				  weightedTitle.put(s, freq);
			  }
			  else{
				  weightedTitle.put(s,7.0);
			  }		
		  }
	  }
	  
	  
		 for (String[] content : mergingcontent) {
				for(String s: content){
					if (stopwSet.contains(s))
						continue;
					else if (weightedTitle.containsKey(s)) {
						double freq = weightedTitle.get(s) +3;
						weightedTitle.put(s, freq);
					}
					else{
						weightedTitle.put(s,3.0);
					}	
				}
		 }
		 
		 // Sorting based on the relevant score
		 String[] queryModify = query.split(" ");
		 Set<Entry<String, Double>> entries = weightedTitle.entrySet();
		 Comparator<Entry<String, Double>> valueComparator = new Comparator<Entry<String, Double>>() {
			 		@Override
			 		public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
			 				Double v1 = o1.getValue(); 
			 				Double v2 = o2.getValue();
			 				return v1.compareTo(v2);
			 		} 
	   };
	   
	   // Retrieve the new query based on the sorting result. 
        List<Entry<String, Double>> listOfEntries = new ArrayList<Entry<String, Double>>(entries);
        Collections.sort(listOfEntries, valueComparator);
        List<String> queryReset = new ArrayList<String>();
        
       for(String s: queryModify){
    	   queryReset.add(s.toLowerCase());
       }
       
       // add new word in the new query
        for(int i=listOfEntries.size()-1; queryReset.size()<=queryModify.length+2;i--){
        	if(queryReset.contains(listOfEntries.get(i).getKey())) continue;
        	queryReset.add(listOfEntries.get(i).getKey());

        }
        
        StringBuilder sb = new StringBuilder();
        for(String s: queryReset){
        	sb.append(s);
        	sb.append(" ");
        }
        
      // refresh the raw result list
      rawResult.clear();
      System.out.println(sb.toString());
      
	  return sb.toString();
	  
  }
  
 // Put a list of common words that needs to be eliminate in the hashset. 
  public static HashSet<String> transfer() throws Exception{
	  //Scanner s = new Scanner(new File("src/stop.txt"));
	  	String stopWords[] = {"a","ii","about","above","according","across","39","actually","ad",
	  			"adj","ae","af","after","afterwards","ag","again","against","ai","al","all","almost",
	  			"alone","along","already","also","although","always","am","among","amongst","an","and",
	  			"another","any","anyhow","anyone","anything","anywhere","ao","aq","ar","are","aren","aren't",
	  			"around","arpa","as","associate","at","au","aw","az","b","ba","bb","bd","be","became","because",
	  			"become","becomes","becoming","been","before","beforehand","begin","beginning","behind","being",
	  			"below","beside","besides","between","beyond","bf","bg","bh","bi","billion","bj","bm","bn",
	  			"bo","both","br","bs","bt","but","buy","bv","bw","by","bz","c","ca","can","can't","cannot",
	  			"caption","cc","cd","cf","cg","ch","ci","ck","cl","click","cm","cn","co","co.","com","copy",
	  			"could","couldn","couldn't","cr","cs","cu","cv","cx","cy","cz","d","de","did","didn","didn't",
	  			"dj","dk","dm","do","does","doesn","doesn't","don","don't","down","during","dz","e","each",
	  			"ec","edu","ee","eg","eh","eight","eighty","either","else","elsewhere","end","ending","enough",
	  			"er","es","et","etc","even","ever","every","everyone","everything","everywhere","except","f",
	  			"few","fi","fifty","find","first","five","fj","fk","fm","fo","for","former","formerly","forty",
	  			"found","four","fr","free","from","further","fx","g","ga","gb","gd","ge","get","gf","gg","gh",
	  			"gi","gl","gm","gmt","gn","go","gov","gp","gq","gr","gs","gt","gu","gw","gy","h","had","has",
	  			"hasn","hasn't","have","haven","haven't","he","he'd","he'll","he's","help","hence","her","here",
	  			"here's","hereafter","hereby","herein","hereupon","hers","herself","him","himself","his","hk",
	  			"hm","hn","home","homepage","how","however","hr","ht","htm","html","http","hu","hundred","i",
	  			"i'd","i'll","i'm","i've","i.e.","id","ie","if","il","im","in","inc","inc.","indeed","information",
	  			"instead","int","into","io","iq","ir","is","isn","isn't","it","it's","its","itself","j","je","jm",
	  			"jo","join","jp","k","ke","kg","kh","ki","km","kn","kp","kr","kw","ky","kz","l","la","last","later",
	  			"latter","lb","lc","least","less","let","let's","li","like","likely","lk","ll","lr","ls","lt",
	  			"ltd","lu","lv","ly","m","ma","made","make","makes","many","maybe","mc","md","me","meantime",
	  			"meanwhile","mg","mh","microsoft","might","mil","million","miss","mk","ml","mm","mn","mo","more",
	  			"moreover","most","mostly","mp","mq","mr","mrs","ms","msie","mt","mu","much","must","mv","mw","mx",
	  			"my","myself","mz","n","na","namely","nc","ne","neither","net","netscape","never","nevertheless",
	  			"new","next","nf","ng","ni","nine","ninety","nl","no","nobody","none","nonetheless","noone","nor",
	  			"not","nothing","now","nowhere","np","nr","nu","nz","o","of","off","often","om","on","once","one",
	  			"one's","only","onto","or","org","other","others","otherwise","our","ours","ourselves","out","over",
	  			"overall","own","p","pa","page","pe","per","perhaps","pf","pg","ph","pk","pl","pm","pn","pr","pt","pw",
	  			"py","q","qa","r","rather","re","recent","recently","reserved","ring","ro","ru","rw","s","sa","same","sb",
	  			"sc","sd","se","seem","seemed","seeming","seems","seven","seventy","several","sg","sh","she","she'd",
	  			"she'll","she's","should","shouldn","shouldn't","si","since","site","six","sixty","sj","sk","sl","sm",
	  			"sn","so","some","somehow","someone","something","sometime","sometimes","somewhere","sr","st","still",
	  			"stop","su","such","sv","sy","sz","t","taking","tc","td","ten","tells","text","tf","tg","test","th",
	  			"than","that","that'll","that's","the","their","them","themselves","then","thence","there","there'll",
	  			"there's","thereafter","thereby","therefore","therein","thereupon","these","they","they'd","they'll",
	  			"they're","they've","thirty","this","those","though","thousand","three","through","throughout","thru",
	  			"thus","tj","tk","tm","tn","to","together","too","toward","towards","tp","tr","trillion","tt","tv","tw",
	  			"twenty","two","tz","u","ua","ug","uk","um","under","unless","unlike","unlikely","until","up","upon","us",
	  			"use","used","using","uy","uz","v","va","vc","ve","very","vg","vi","via","vn","vu","w","was","wasn","wasn't",
	  			"we","we'd","we'll","we're","we've","web","webpage","website","welcome","well","were","weren","weren't","wf",
	  			"what","what'll","what's","whatever","when","whence","whenever","where","whereafter","whereas","whereby",
	  			"wherein","whereupon","wherever","whether","which","while","whither","who","who'd","who'll","who's","whoever",
	  			"NULL","whole","whom","whomever","whose","why","will","with","within","without","won","won't","would",
	  			"wouldn","wouldn't","ws","www","x","y","ye","yes","yet","you","you'd","you'll","you're","you've","your",
	  			"yours","yourself","yourselves","yt","yu","z","za","zm","zr","10","z","href"
	  	};
		 List<String> l = new ArrayList<String>();
		 for(int j = 0; j<stopWords.length; j++){
			 l.add(stopWords[j]);
		 }
		 
		 HashSet<String> h = new HashSet<String>();
		 for(int i = 0;i<l.size();i++){
			 h.add(l.get(i));
		 }
		 
		 return h;
	  
  }
  
  
}
