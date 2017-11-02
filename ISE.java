package adbProject2;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;
import edu.stanford.nlp.*;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations.RelationMentionsAnnotation;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.KBPTriplesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.RelationExtractorAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;


/*import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.apache.tika.sax.xpath.XPathParser;*/
import org.xml.sax.ContentHandler;

public class ISE {
	static int total;
	static int urlIndex;
	static Map<String,Double> rstList; 

	
	static List<String> url = new ArrayList<String>();
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
		
		//name entities
		String[] entities = new String[2];
		if(r==1){
			entities[0]="PEOPLE";
			entities[1]="LOCATION";
		}
		if(r==2){
			entities[0]="LOCATION";
			entities[1]="LOCATION";
		}
		if(r==3){
			entities[0]="ORGANIZATION";
			entities[1]="LOCATION";
		}
		if(r==4){
			entities[0]="ORGANIZATION";
			entities[1]="PEOPLE";
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
		//Set<Entry<String, Double>> X = Collections.emptySet();
		int totals=0;
		int round=0;
		urlIndex=0;
		rstList= new HashMap<String,Double>();
		while(total<k){
			round++;
			System.out.println("========QUERY ROUND "+round+"=========="+"QUERY:"+q);
		// Obtain top-10 Webpages for query q
			ArrayList<String> urlResult = Search(engineKey, apiKey, 10, q);
			HashMap<String,Double> rstList = new HashMap<>();

			for(int i = 0; i< urlResult.size(); i++){
				System.out.println(urlResult.get(i));
			}
		
		//Extract Information from selected webpage
			List<List<String>> plainTextResult = Extract(urlResult);
		
		//First Pipeline to filter the plainTextResult
			List<List<String>> firstPipe = FirstPipeline(plainTextResult, urlResult, entities);
			for(int i = 0; i<firstPipe.get(0).size(); i++){
				System.out.println(firstPipe.get(0).get(i));
			}
		
		//Second Piepline to calculate
			List<RelationMention> secondPipe = SecondPipeline(firstPipe,type,t,entities);
			if(secondPipe.isEmpty()){
				System.out.println("no more satisfied result,exiting...");
				System.exit(0);
			}
			
			Set<Entry<String, Double>> entries = rstList.entrySet();
			Comparator<Entry<String, Double>> valueComparator = new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
	 				Double v1 = o1.getValue(); 
	 				Double v2 = o2.getValue();
	 				return v2.compareTo(v1);
				} 
			};
			List<Entry<String, Double>> listOfEntries = new ArrayList<Entry<String, Double>>(entries);
			Collections.sort(listOfEntries, valueComparator);
			System.out.println("===========ALL RELATIONS============");
			for(Entry<String,Double> ent: listOfEntries){
				String[] values=((String) ent.getKey()).split(";");
				Double confi = Math.round((double)ent.getValue()*100)/100.0;
				System.out.println("RELATION TYPE:"+type);
				System.out.println("CONFIDENCE:"+confi );
				System.out.println("ENTITY #1"+values[0]);
				System.out.println("ENTITY #2"+values[1]);
				System.out.println("---------------------------------------------------");

			}
			totals=listOfEntries.size();
			if(totals>=k){
				System.out.println("Reached number of tuple goals, exiting...");
				System.exit(1);
			}
			String[] nquery = listOfEntries.get(0).getKey().split(";");
			String first = nquery[0].split("(")[0];
			String second = nquery[1].split("(")[0];
			q = first+" "+second;
		}
		/*
		 // For Debug Only
		Properties props1 = new Properties();
		props1.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, ner");
		props1.setProperty("parse.model", "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		props1.setProperty("ner.useSUTime", "0");
		StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props1);
		RelationExtractorAnnotator rew = new RelationExtractorAnnotator(props1);
		List<List<RelationMention>> second = new ArrayList<>();
		  
		//List<List<RelationMention>> secondPipe = new ArrayList<>();
		try{
			String sentence = "In June 2006, Gates announced that he would be transitioning from full-time work at Microsoft to part-time work and full-time work at the Bill & Melinda Gates Foundation";
			Annotation document2 = new Annotation(sentence);
			pipeline2.annotate(document2);	
			rew.annotate(document2);
			for (CoreMap s : document2.get(CoreAnnotations.SentencesAnnotation.class)) {
				System.out.println("For sentence " + s.get(CoreAnnotations.TextAnnotation.class)); // for test
				//for (CoreLabel token : s.get(CoreAnnotations.TokensAnnotation.class)) {
				//	String ner = token.get(NamedEntityTagAnnotation.class);
					//if(ner.equalsIgnoreCase(entities[0]) || ner.equalsIgnoreCase(entities[1])){
				
						List<RelationMention> rls = s.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
						second.add(rls);
					//}
				//}
			}		
		}catch(Exception e){
			System.out.println("Something wrong !!!!");
		}
		for(int i= 0; i<second.size(); i++){
			List<RelationMention> sub = second.get(i);
			for(RelationMention test: sub){
			System.out.println("type:"+test.getType());
			System.out.println("fullvalue:"+test.getFullValue());
			System.out.println("type:"+test.getTypeProbabilities());
			Counter<String> probs = test.getTypeProbabilities();
			System.out.println("default:"+probs.getCount("_NR"));
			List<EntityMention> rst = test.getEntityMentionArgs();
			for(EntityMention en: rst){
				System.out.println("entityType:"+en.getType());
				System.out.println("entityValue:"+en.getValue());
			}
			//System.out.println(sub.size());
			//for(RelationMention rl: sub){
				//	System.out.println(rl.toString());
			}	//}
			
			
		}
		*/

	}
	//Second pipelin
	public static List<RelationMention> SecondPipeline(List<List<String>> firstPipe, String type,double t,String[] entities){
		Properties props1 = new Properties();
		props1.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, ner");
		props1.setProperty("parse.model", "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		props1.setProperty("ner.useSUTime", "0");
		StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props1);
		RelationExtractorAnnotator rew = new RelationExtractorAnnotator(props1);
		
		List<RelationMention> secondPipe = new ArrayList<>();
		
		
		for(int i = 0; i<firstPipe.size(); i++){
			int count = 0;
			System.out.println("processing:" +url.get(urlIndex));
			urlIndex++;
			//List<RelationMention> current = new ArrayList<>();
			for(int j = 0; j<firstPipe.get(i).size(); j++){
				try{
					String sentence = firstPipe.get(i).get(j);
					Annotation document2 = new Annotation(sentence);
					pipeline2.annotate(document2);	
					rew.annotate(document2);
					for (CoreMap s : document2.get(CoreAnnotations.SentencesAnnotation.class)) {
						List<RelationMention> rls = s.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
						for(RelationMention relation: rls){
							if(relation.getType().equalsIgnoreCase(type)){
								Counter<String> probs = relation.getTypeProbabilities();
								Double confidence = probs.getCount(type);
									List<EntityMention> rst = relation.getEntityMentionArgs();
									List<List<String>> type1 = new ArrayList<List<String>>();
									for(EntityMention en: rst){
										List<String> temp = new ArrayList<String>();
										temp.add(en.getType());
										temp.add(en.getValue());
										type1.add(temp);
										//System.out.println("ENTITYTYPE:"+en.getType());
										//System.out.println("ENTITYVALUE:"+en.getValue());
									}
										if((type1.get(0).get(0).equalsIgnoreCase(entities[0])||type1.get(0).get(0).equalsIgnoreCase(entities[1]))&&(type1.get(1).get(0).equalsIgnoreCase(entities[0])||type1.get(1).get(0).equalsIgnoreCase(entities[1]))){
											System.out.println("Summary:"+relation.toString());
											System.out.println("=============EXTRACT RELATION===========");
											System.out.println("SENTENCE: " + s.get(CoreAnnotations.TextAnnotation.class)); // for test
											System.out.println("Relation:"+relation.getType());
											System.out.println("Confidence Score:"+confidence);
											for(int z=0;z<type1.size();z++){
												System.out.println("ENTITYTYPE:"+type1.get(z).get(0));	
												System.out.println("ENTITYVALUE:"+type1.get(z).get(1));	
												
											}
											System.out.println("=============END OF RELATION DESC===========");
											if(confidence>=t){
												String keyCheck = type1.get(0).get(1).toLowerCase()+"("+type1.get(0).get(0)+")"+";"+type1.get(1).get(1).toLowerCase()+"("+type1.get(1).get(0)+")";
												if(!rstList.containsKey(keyCheck)){
													System.out.println("ADD TO HASHMAP");
													rstList.put(keyCheck, confidence);
													secondPipe.add(relation);
												}
												else{
													Double conf = rstList.get(keyCheck);
													if(confidence>conf){
														System.out.println("ADD TO HASHMAP");
														rstList.put(keyCheck,confidence);
														secondPipe.add(relation);
													}
												}
												
												count++;
												break;
											}
									
										}
							}
						}
					}
				}catch(Exception e){
					System.out.println("Something wrong !!!!");
				}
			}
			total+=count;
			System.out.println("sentence extracted:"+count+" overall:"+total);
			//secondPipe.add(current);
		}

			//System.out.println(secondPipe.size());

			
	//System.out.println("elations extracted from this website:"+count);
	return secondPipe;	
	}
	
	//First piepeline
	public static List<List<String>> FirstPipeline(List<List<String>> plainTextResult, ArrayList<String> urlResult, String[] entities){
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
		StanfordCoreNLP pipeline1 = new StanfordCoreNLP(props);
		List<List<String>> firstPipe = new ArrayList<>();
		
		for(int i = 0; i<plainTextResult.size(); i++){
			//System.out.println("Processing:"+urlResult.get(i));
			List<String> current = new ArrayList<>();
			for(int j = 0; j<plainTextResult.get(i).size(); j++){
				String text =plainTextResult.get(i).get(j);
				Annotation document1 = new Annotation(text);
				pipeline1.annotate(document1);
				for (CoreMap s : document1.get(CoreAnnotations.SentencesAnnotation.class)) {
					for (CoreLabel token : s.get(CoreAnnotations.TokensAnnotation.class)) {
							String ner = token.get(NamedEntityTagAnnotation.class);
							//System.out.println("For sentence "
					        //          + token.get(CoreAnnotations.TextAnnotation.class));
					       // System.out.println("Relation "
					       //            + token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
					        if(ner.equalsIgnoreCase(entities[0]) || ner.equalsIgnoreCase(entities[1])){
									current.add(s.get(CoreAnnotations.TextAnnotation.class));
									//System.out.println("For sentence "
									  //                + token.get(CoreAnnotations.TextAnnotation.class));
									//System.out.println("Relation "
									//	                   + token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
									//System.out.println("satisfy entity requirement");      
									break;
								}
				
					}
					
				}
			}
			firstPipe.add(current);
		}
		return firstPipe;
	}	
		
		
	
	// Extract Information from selected url
	public static List<List<String>> Extract(ArrayList<String> urlResult){
		/*
		// Apache Tika version
		List<String> plainTextResult = new ArrayList<>();
		for(int i = 0; i<urlResult.size(); i++){
			try{
				/*
			InputStream input = new URL(urlResult.get(i)).openStream();
			ContentHandler handler = new BodyContentHandler();
	        Metadata metadata = new Metadata();
	        //AutoDetectParser parser = new AutoDetectParser();
	        new HtmlParser().parse(input, handler, metadata, new ParseContext());
	        String plainText = handler.toString();
	        plainTextResult.add(plainText);
	        
				ContentHandler handler = new ToXMLContentHandler();;
				//XPathParser xhtmlParser = new XPathParser("xhtml", XHTMLContentHandler.XHTML);
		        //Matcher divContentMatcher = xhtmlParser.parse("/xhtml:html/xhtml:body/descendant::node()");
		        //ContentHandler handler = new ToXMLContentHandler(divContentMatcher);
		        AutoDetectParser parser = new AutoDetectParser();
		        Metadata metadata = new Metadata();
		        InputStream input = new URL(urlResult.get(i)).openStream();
		        parser.parse(input, handler, metadata);
		        plainTextResult.add(handler.toString().replaceAll("\\<.*?>",""));
		        
			}catch (IOException e){
				System.out.println(urlResult.get(i) + "cannot be extracted");
				continue;
			}
			
		}
		
		//for(int j = 0; j<plainTextResult.get(0); j++){
			System.out.println(plainTextResult.get(0));
		//}
			
			*/
		
		
		// Jsoup Version
		//List<String> plainTextResult = new ArrayList<>();
		List<List<String>> plainTextResult = new ArrayList<>();
		for(int i = 0; i<urlResult.size(); i++){
			if(url.contains(urlResult.get(i))) continue;
			try{
			Document doc = Jsoup.connect(urlResult.get(i)).timeout(10000).get();
			Elements paragraphs = doc.select("p");
			//String text = doc.body().text();  /// Mark : Body or Text
			List<String> curr = new ArrayList<>();
			for(Element p: paragraphs){
				String text = p.text().replaceAll("\\<.*?>","");
				curr.add(text);
			}
			plainTextResult.add(curr);
			url.add(urlResult.get(i));
			//url.add(urlResult.get(i));
			}catch (IOException e){
				System.out.println(urlResult.get(i) + "cannot be extracted");
				continue;
			}
			
		}
		/*
		//printing
		for(int j = 0; j<plainTextResult.get(1).size(); j++){
			System.out.println(plainTextResult.get(1).get(j));
		}
		*/
		
		return plainTextResult;
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
		            	String s = "";
		              	String urlTest = result.getFormattedUrl();
		              	if(urlTest.startsWith("www")) s+="https://";
		              	s+=urlTest;
		              	urlResult.add(s);
		              }
		          }
		          return urlResult;
		      }
}