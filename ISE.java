import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;
import edu.stanford.nlp.*;
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
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

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
		
		/*
		for(int j = 0; j<plainTextResult.size(); j++){
		System.out.println(plainTextResult.get(j));
		}
		*/
		
		//Test of String input
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
		StanfordCoreNLP pipeline1 = new StanfordCoreNLP(props);
		
		Properties props1 = new Properties();
		props1.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, ner");
		props1.setProperty("parse.model", "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		props1.setProperty("ner.useSUTime", "0");
		StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props1);
		RelationExtractorAnnotator rew = new RelationExtractorAnnotator(props1);
		
		
		/*for(int j=0; j<plainTextResult.size();j++ ){
			String text = plainTextResult.get(j);
			
			//first pipeline
			List<String> firstPipe = new ArrayList<String>();
			Annotation document1 = new Annotation(text);
			pipeline1.annotate(document1);
			
			for (CoreMap s : document1.get(CoreAnnotations.SentencesAnnotation.class)) {
				for (CoreLabel token : s.get(CoreAnnotations.TokensAnnotation.class)) {
						String ner = token.get(NamedEntityTagAnnotation.class);
						if(ner.equalsIgnoreCase(entities[0]) || ner.equalsIgnoreCase(entities[1])){
							firstPipe.add(s.get(CoreAnnotations.TextAnnotation.class));	
							
							System.out.println("For sentence "
					                  + token.get(CoreAnnotations.TextAnnotation.class));
					           System.out.println("Relation "
					                    + token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
					                    
							break;	
						}
				  }
			}
			
			if(firstPipe == null || firstPipe.size() == 0){
				continue;
				
			}else{
				
				for(int i = 0; i<firstPipe.size(); i++){
					System.out.println(firstPipe.get(i));
				}
				
			
			//second pipeline
				List<List<RelationMention>> secondPipe = new ArrayList<>();
				//List<Set<RelationMention>> secondPipe = new ArrayList<>();
				for(String str: firstPipe){
					
				
					try{
						//String sentence = "In June 2006, Gates announced that he would be transitioning from full-time work at Microsoft to part-time work and full-time work at the Bill & Melinda Gates Foundation";
						Annotation document2 = new Annotation(str);
						pipeline2.annotate(document2);	
						rew.annotate(document2);
						for (CoreMap s : document2.get(CoreAnnotations.SentencesAnnotation.class)) {
							System.out.println("For sentence " + s.get(CoreAnnotations.TextAnnotation.class)); // for test
							for (CoreLabel token : s.get(CoreAnnotations.TokensAnnotation.class)) {
								String ner = token.get(NamedEntityTagAnnotation.class);
								if(ner.equalsIgnoreCase(entities[0]) || ner.equalsIgnoreCase(entities[1])){
							
									List<RelationMention> rls = s.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
									secondPipe.add(rls);
								}
							}
						}		
					}catch(Exception e){
						System.out.println("Something wrong !!!!");
					}
					System.out.println(secondPipe.size());
					
						//Set<RelationMention> uniqueRelationMentions = new HashSet<>(relationMentions);
					    //secondPipe.add(uniqueRelationMentions);
					    //System.out.println("Extracted the following:");
					    //for(RelationMention m: uniqueRelationMentions){
					    //	System.out.println(m);
					    //}
					
				
				
				}

			}
				
		}*/
		
		// For Debug Only
		List<List<RelationMention>> secondPipe = new ArrayList<>();
		try{
			String sentence = "In June 2006, Gates announced that he would be transitioning from full-time work at Microsoft to part-time work and full-time work at the Bill & Melinda Gates Foundation";
			Annotation document2 = new Annotation(sentence);
			pipeline2.annotate(document2);	
			rew.annotate(document2);
			for (CoreMap s : document2.get(CoreAnnotations.SentencesAnnotation.class)) {
				System.out.println("For sentence " + s.get(CoreAnnotations.TextAnnotation.class)); // for test
				for (CoreLabel token : s.get(CoreAnnotations.TokensAnnotation.class)) {
					String ner = token.get(NamedEntityTagAnnotation.class);
					if(ner.equalsIgnoreCase(entities[0]) || ner.equalsIgnoreCase(entities[1])){
				
						List<RelationMention> rls = s.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
						secondPipe.add(rls);
					}
				}
			}		
		}catch(Exception e){
			System.out.println("Something wrong !!!!");
		}
		for(int i= 0; i<secondPipe.size(); i++){
			List<RelationMention> sub = secondPipe.get(i);
			System.out.println(sub.size());
			//for(RelationMention rl: sub){
				//	System.out.println(rl.toString());
				//}
			
			
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
