

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Mining {
	private static List<combo> combos = new ArrayList<>();
	private static List<Set<String>> supportCount=new ArrayList<>();
	private static Set<String> itemset = new HashSet<>();
	private static List<List<String>> process_itemsets;
	private static List<Double> process_support;
	private static Map<List<String>,Double> itemsets_support_map;
	private static Map<List<String>, Double> freqItems = new HashMap<>();
	public static void main(String[] args) throws Exception {
		if(args.length != 3){
			System.out.println("Usage: <csv file name> <min_sup> <min_conf>");
			System.exit(1); 
		}
		
		// CSV File
		
		String fileName = args[0];
		
		// min_sup and min_conf between 0 and 1
		double min_sup = Double.parseDouble(args[1]);
		if(min_sup < 0 || min_sup > 1){
			System.err.println("Please enter a valid minimum support value (between 0 and 1)");
			System.exit(1);
		} 
		
		double min_conf =  Double.parseDouble(args[2]);
		if(min_conf < 0 || min_conf > 1){
			System.err.println("Please enter a valid minimum support value (between 0 and 1)");
			System.exit(1);
		} 
		
		// parsing CSV file 
		List<List<String>> parsingList = parsing(fileName);
		//printing(parsingList);
		//organize data
		List<List<String>> FirstItems = new ArrayList<List<String>>();

		for (List<String> items : parsingList) {
			Set<String> temp = new HashSet<String>();
			for (String item : items) {
				itemset.add(item);
				temp.add(item);
			}
			supportCount.add(temp);
		}
		
		for (String item : itemset) {
			ArrayList<String> cur = new ArrayList<String>();
			cur.add(item);
			FirstItems.add(cur);
		}
		processing_itemsets(FirstItems,min_sup);
		
		while (true) {
			if (process_itemsets.size() == 0) break;
			combo temp = new combo(process_itemsets,process_support,itemsets_support_map);
			combos.add(temp);
			for (int i = 0; i < process_itemsets.size(); i++) {
				freqItems.put(process_itemsets.get(i), process_support.get(i));
			}
			List<List<String>> nextRound = verify();
			processing_itemsets(nextRound,min_sup);
		}
		
		//Descending Order
		List<Map.Entry<List<String>, Double>> mapL = new LinkedList<Map.Entry<List<String>, Double>>(freqItems.entrySet());
		Collections.sort(mapL, new Comparator<Map.Entry<List<String>, Double>>(){
			public int compare(Map.Entry<List<String>, Double> m1, Map.Entry<List<String>, Double> m2){
				return m2.getValue().compareTo(m1.getValue());
			}
		});
		
		
	
	}
	
	public static List<List<String>> verify(){
		List<List<String>> rst = new ArrayList<List<String>>();
		for (List<String> item1 : process_itemsets){
			for (List<String> item2 : process_itemsets) {
				boolean check = true;
				int n = item1.size() - 1;
				if (item1.get(n).compareTo(item2.get(n)) < 0) {
					for (int i = 0; i < n; i++) {
						if (item1.get(i).compareTo(item2.get(i))!=0) {
							check = false;
							break;
						}
					}
					if (check) {
						List<String> temp = new ArrayList<String>(item1);
						temp.add(item2.get(n));
						if (checking(temp)) {
							rst.add(temp);
						}
					}
				}
				
			}
		}
		
		return rst;
	}
	public static boolean checking(List<String> stringset){
		boolean res = true;
		for (int i = 0; i < stringset.size(); i++) {
			ArrayList<String> temp = new ArrayList<String>(stringset);
			temp.remove(i);
			boolean exist = false;
			for (List<String> item : process_itemsets){
				boolean equal = true;
				for (int j = 0;j < item.size(); j++)
					if (!temp.get(j).equals(item.get(j))) {
						equal = false;
						break;
					}
				if (equal) exist = true;
			}
			if (!exist) {
				res = false;
				break;
			}
		}
		
		return res;
	}
	
	public static void printing(List<List<String>> l){
		int n = l.size();
		for(int i = 0; i< n; i++){
			for(int j = 0; j< l.get(i).size(); j++){
				System.out.print(l.get(i).get(j) + " ");
			}
			System.out.println();	 
		}
		
	}
	
	public static List<List<String>> parsing(String fileName){
		List<List<String>> result = new ArrayList<>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String currentLine;
			int count = 0;
			while((currentLine = br.readLine()) != null){
				count ++;
				if(count == 1){
					continue;
				}
				String[] column = currentLine.split(",");
				List<String> l = new ArrayList<>();
				for(String c: column){
					if(c.length() == 0 || c.equals("")){
						continue;
					}else{
						l.add(c);
					}
				}
				result.add(l);
			}
		//	if(br != null){
				br.close();
		//	}
		} catch (Exception e){
			
		}
		return result;
	}
	public static void processing_itemsets(List<List<String>> Items,Double min_sup){
		process_itemsets = new ArrayList<List<String>>();
		process_support = new ArrayList<Double>();
		itemsets_support_map = new HashMap<List<String>,Double>();
		for (List<String> item : Items) {
			double c = counting(item);
			/* Greater then or equal to */
			if (c >= min_sup) {
				process_itemsets.add(item);
				process_support.add(c);
			}
		}
		for(int i = 0; i < process_itemsets.size(); i++)
			itemsets_support_map.put(process_itemsets.get(i), process_support.get(i));
	}
	
	public static Double counting(List<String> item){
		Set<String> set = new HashSet<String>();
		for (String t : item) 
			set.add(t);
		double count = 0;
		for (Set<String> itemset : supportCount)
			if(itemset.containsAll(set)) count++;
		count = count / supportCount.size();
		return count;
		
	}


}
class combo{
	List<List<String>> items;
	List<Double> sups;
	Map<List<String>,Double> mapping;
	public combo(List<List<String>> process_itemsets, List<Double> process_support, Map<List<String>,Double> map){
		items=process_itemsets;
		sups=process_support;
		mapping = map;
	}
}
