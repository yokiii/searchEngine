package pkg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
	private static List<Set<String>> supportCount=new ArrayList<>();
	private static Set<String> itemset = new HashSet<>();
	private static List<List<String>> process_itemsets;
	private static List<Double> process_support;
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
		//first round
		processing_itemsets(FirstItems,min_sup);
		List<combo> combos = new ArrayList<>();
		Map<String, Double> freqItems = new HashMap<>();
		boolean next = true;
		while (next) {
			if (process_itemsets.size() == 0){
				next=false;
				break;
			}
			List<List<String>> process_itemsets_cur = new ArrayList<>(process_itemsets);
			List<Double> process_support_cur = new ArrayList<>(process_support);
			Map<List<String>,Double> itemsets_supp_map_cur= new HashMap<>(itemsets_support_map); 
			combo temp = new combo(process_itemsets_cur,process_support_cur,itemsets_supp_map_cur);
			combos.add(temp);
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
				sb.append(process_support_cur.get(i)*100+"%");
				String itemstring = sb.toString();
				System.out.println(itemstring);
				freqItems.put(itemstring, process_support_cur.get(i));
			}
			List<List<String>> nextRound = verify(process_itemsets_cur);
			processing_itemsets(nextRound,min_sup);
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
		try{
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
		} catch (Exception e){
			System.out.println(e);
		}
		
		
	
	}
	public static List<List<String>> associationRule (List<combo> c, double conf) {
		List<List<String>> rules = new ArrayList<List<String>>();
		for (int i = 1;i < c.size(); i++) {
			combo cur = c.get(i);
			combo pre = c.get(i - 1);
			for (List<String> itemset : cur.items) {
				double support = cur.mapping.get(itemset);
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
					Double support2 = pre.mapping.get(temp);
					Double sup = support / support2;
					if(sup >= conf) {
						String supString = sup.toString();
						List<String> tempRule = new ArrayList<String>();
						tempRule.add(imply);
						tempRule.add(itemset.get(k));
						tempRule.add(supString);
						tempRule.add(cur.mapping.get(itemset).toString());
						rules.add(tempRule);
					}
				}
			}
		}
		return rules;
	}
	
	public static List<List<String>> verify(List<List<String>> process_itemsets_this){
		List<List<String>> rst = new ArrayList<List<String>>();
		for (List<String> item1 : process_itemsets_this){
			for (List<String> item2 : process_itemsets_this) {
				boolean check = true;
				int n = item1.size() - 1;
				if (item1.get(n).compareTo(item2.get(n)) < 0) {
					for (int i = 0; i < n; i++) {
						if (item1.get(i).compareTo(item2.get(i))!=0) {
							check = false;
							break;
						}
					}
					if (check==true) {
						List<String> temp = new ArrayList<String>(item1);
						temp.add(item2.get(n));
						boolean unique = checking(temp,process_itemsets_this);
						if (unique==true) {
							rst.add(temp);
						}
					}
				}
				
			}
		}
		
		return rst;
	}
	public static boolean checking(List<String> stringset,List<List<String>> process_itemsets_this){
		boolean rst = true;
		for (int i = 0; i < stringset.size(); i++) {
			boolean exist = false;
			ArrayList<String> temp = new ArrayList<String>(stringset);
			temp.remove(i);
			for (List<String> item : process_itemsets_this){
				boolean equal = true;
				for (int j = 0;j < item.size(); j++)
					if (!temp.get(j).equals(item.get(j))) {
						equal = false;
						break;
					}
				if (equal==true){
					exist = true;
				}
			}
			if (exist==false) {
				rst = false;
				break;
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
				String[] row = currentLine.split(",");
				List<String> col = new ArrayList<>();
				for(int i=0;i<row.length;i++){
					//if(i==8||i==9||i==15||i==16) continue;
					if(row[i].length() == 0 || row[i].equals("")){
						continue;
					}else{
						col.add(row[i]);
					}
				}
				result.add(col);
			}

				br.close();
		} catch (Exception e){
			System.out.println(e);
			
		}
		return result;
	}
	public static void processing_itemsets(List<List<String>> Items,Double min_sup){
		process_itemsets = new ArrayList<List<String>>();
		process_support = new ArrayList<Double>();
		itemsets_support_map = new HashMap<List<String>,Double>();
		for(int i = 0; i< Items.size(); i++){
			double value = calculate(Items.get(i));
			if (value < min_sup) {
				continue;
			}else{
				process_itemsets.add(Items.get(i));
				process_support.add(value);
				itemsets_support_map.put(Items.get(i), value);
			}
		}
	}
	
	
	public static Double calculate(List<String> item){
		double value = 0;
		Set<String> current_set = new HashSet<String>();
		for(int i = 0; i< item.size(); i++){
			current_set.add(item.get(i));
		}
		for (Set<String> s : supportCount){
			if(s.containsAll(current_set)) {
				value++;
			}
		}
		Double rst = value/supportCount.size();
		return rst;	
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
