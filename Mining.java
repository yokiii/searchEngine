import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Mining {
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
		printing(parsingList);
		
	
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


}
