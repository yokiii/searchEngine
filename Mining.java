package pkg;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class Mining {
	private static List<List<String>> process_itemsets;
	private static Map<List<String>,Double> itemsets_support_map;
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
		
		
		//organize data
		List<List<String>> FirstItems = new ArrayList<List<String>>();
		Set<String> itemset = new HashSet<>();
		List<Set<String>> supportCount=new ArrayList<>();
		//add marketbasket into the total count and each eligible item into its own basket
		for (List<String> items : parsingList) {
			Set<String> temp = new HashSet<String>();
			for (String item : items) {
				itemset.add(item);
				temp.add(item);
			}
			supportCount.add(temp);
		}
		//adhere to the format, put the initial 1 item per sets into a list and then add to first item for easy computation
		for (String item : itemset) {
			List<String> cur = new ArrayList<String>();
			cur.add(item);
			FirstItems.add(cur);
		}
		//first round
		processing_itemsets(FirstItems,min_sup,supportCount);
		List<Map<List<String>,Double>> combos = new ArrayList<>();
		Map<String, Double> freqItems = new HashMap<>();
		boolean next = true;
		while (next==true) {
			//no itesets left then stop here
			if (process_itemsets.size() == 0){
				next=false;
				break;
			}
			Map<List<String>,Double> itemsets_supp_map_cur= new HashMap<>(itemsets_support_map); 
			Set<List<String>> temp_cur = itemsets_support_map.keySet();
			List<List<String>> process_itemsets_cur = new ArrayList<List<String>>(temp_cur);
			//add items to combo for later association rule computation
			combos.add(itemsets_supp_map_cur);
			for (int i = 0; i < process_itemsets_cur.size(); i++) {
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				for(String item: process_itemsets_cur.get(i)){
					sb.append(item);
					sb.append(",");
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append("]");
				sb.append(", ");
				sb.append(itemsets_supp_map_cur.get(process_itemsets_cur.get(i))*100+"%");
				String itemstring = sb.toString();
				System.out.println(itemstring);
				freqItems.put(itemstring, itemsets_supp_map_cur.get(process_itemsets_cur.get(i)));
			}
			//compute itemsets for the next round N+1 items
			List<List<String>> nextRound = verify(process_itemsets_cur);
			processing_itemsets(nextRound,min_sup,supportCount);
		}
		
		//Descending Order
		List<Map.Entry<String, Double>> mapL = new LinkedList<Map.Entry<String, Double>>(freqItems.entrySet());
		Collections.sort(mapL, new Comparator<Map.Entry<String, Double>>(){
			public int compare(Map.Entry<String, Double> m1, Map.Entry<String, Double> m2){
				return m2.getValue().compareTo(m1.getValue());
			}
		});
		
		List<List<String>> rules = associationRule(combos,min_conf);
		Map<String,Double> rst = new HashMap<>();
		for(List<String> seg : rules) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			sb.append(seg.get(0));
			sb.append("]");
			sb.append(" => ");
			sb.append("[");
			sb.append(seg.get(1));
			sb.append("]");
			sb.append(" ");
			Double conf = Double.parseDouble(seg.get(2));
			Double supp = Double.parseDouble(seg.get(3));
			sb.append("(Conf: "+ 100 * conf + "%, Supp:" + 100 * supp + "%)");
			System.out.println(sb.toString());
			rst.put(sb.toString(), conf);
		}
		
		List<Map.Entry<String, Double>> mapRule = new LinkedList<Map.Entry<String, Double>>(rst.entrySet());
		Collections.sort(mapRule, new Comparator<Map.Entry<String, Double>>(){
			public int compare(Map.Entry<String, Double> m1, Map.Entry<String, Double> m2){
				return m2.getValue().compareTo(m1.getValue());
			}
		});
		
		//output file
			PrintWriter writer = new PrintWriter(new FileWriter("output.txt"));
			writer.println("==Frequent itemsets (min_sup=" + 100 * min_sup + "%)");
			for (Map.Entry<String, Double> freq : mapL) {
				writer.println(freq.getKey());
			}
			writer.println("==High-confidence association rules (min_conf=" + 100 * min_conf + "%)");
			for (Map.Entry<String, Double> asso : mapRule) {
				writer.println(asso.getKey());
			}
			writer.close();	
	
	}
	public static List<List<String>> associationRule (List<Map<List<String>,Double>> c, double conf) {
		List<List<String>> rules = new ArrayList<List<String>>();
		for (int i = 1;i < c.size(); i++) {
			//only one item on the right side
			Map<List<String>, Double> cur = c.get(i);
			Set<List<String>> cur_list_temp = cur.keySet();
			List<List<String>> cur_list = new ArrayList<List<String>>(cur_list_temp);
			Map<List<String>, Double> pre = c.get(i - 1);
			for (List<String> itemset : cur_list) {
				double support = cur.get(itemset);
				for(int k = 0; k < itemset.size(); k++) {
					List<String> temp = new ArrayList<String>(itemset);
					temp.remove(k);
					StringBuilder sb = new StringBuilder();
					for(String s: temp){
						sb.append(s);
						sb.append(",");
					}
					sb.deleteCharAt(sb.length()-1);
					String imply = sb.toString().trim();
					Double sup = support / pre.get(temp);
					if(sup >= conf) {
						String supString = sup.toString();
						// add left side, right side, conf and support into a list
						List<String> tempRule = new ArrayList<String>();
						tempRule.add(imply);
						tempRule.add(itemset.get(k));
						tempRule.add(supString);
						tempRule.add(cur.get(itemset).toString());
						rules.add(tempRule);
					}
				}
			}
		}
		return rules;
	}
	
	public static int helper(String st1, String st2){
		return st1.compareTo(st2);
	}
	
	
	// compute combination itemsets for the nextround 
	public static List<List<String>> verify(List<List<String>> process_itemsets_this){
		List<List<String>> rst = new ArrayList<List<String>>();
		for (List<String> first : process_itemsets_this){
			int size = first.size() - 1;
			String first1=first.get(size);
			for (List<String> second : process_itemsets_this) {
				String addition = second.get(size);
				String second1=second.get(size);
				if (helper(first1, second1) >= 0) {
					continue;
				}
				else{
					boolean check = true;
					for (int i = 0; i < size; i++) {
						String first2=first.get(i);
						String second2=second.get(i);
						
						if (helper(first2, second2)!=0) {
							check = false;
							break;
						}
					}
					
					if (check) {
						List<String> temp = new ArrayList<String>(first);
						temp.add(addition);
						boolean checking = false;
						
						for (int i = 0; i < temp.size(); i++) {
							boolean exist = false;
							List<String> temp1 = new ArrayList<String>(temp);
							temp1.remove(i);
							for (List<String> item : process_itemsets_this){
								for (int j = 0;j < item.size(); j++)
									if (!temp1.get(j).equals(item.get(j))) {
										break;
									}
								exist = true;
								}
							
							if (exist == false) {
								break;
							}
							checking = true;
						}
						
						if (checking) {
							rst.add(temp);
						}
					}
				}
				
			}
		}
		
		return rst;
	}
	
	//public static void printing(List<List<String>> l){
		//int n = l.size();
		//for(int i = 0; i< n; i++){
		//	for(int j = 0; j< l.get(i).size(); j++){
		//		System.out.print(l.get(i).get(j) + " ");
			//}
			//System.out.println();	 
	//	}
		
//	}
	
	
	private static List<List<String>> parsing(String file) throws IOException {
        List<List<String>> rst = new ArrayList<List<String>>();
        Reader in = new FileReader(file);
        Iterable<CSVRecord> recs = CSVFormat.RFC4180.parse(in);
        for (CSVRecord rec : recs) {
            List<String> row = new ArrayList<String>();
            for (int i = 0; i < rec.size(); i++) {
            	//if(i==8||i==9||i==15||i==16) continue;
            	if(rec.get(i).isEmpty()) continue;
            	else row.add(rec.get(i));
            }
            rst.add(row);
        }
        return rst;
    }
	
	// add eligible itemsets into the mapping
	public static void processing_itemsets(List<List<String>> Items,Double min_sup, List<Set<String>> supportCount){
		process_itemsets = new ArrayList<List<String>>();
		itemsets_support_map = new HashMap<List<String>,Double>();
		for(int i = 0; i< Items.size(); i++){
			double value = 0;
			Set<String> current_set = new HashSet<String>();
			List<String> temp = Items.get(i);
			for(int j = 0; j< temp.size(); j++){
				current_set.add(temp.get(j));
			}
			for (Set<String> s : supportCount){
				if(s.containsAll(current_set)) {
					value++;
				}
			}
			int size = supportCount.size();
			value = value/size;
			if (value < min_sup) {
				continue;
			}else{
				process_itemsets.add(Items.get(i));
				itemsets_support_map.put(Items.get(i), value);
			}
		}
	}
}
