public class MainEngine{
  
  private static String engineKey;
  private static String apiKey;
  private static double precision;
  private static double 
  private static String[] query;
  private static String rawResult;
  
  private final int top = 10;
  
  public static void main(String[] args) throws Exception{
    //No.1 ---------------------------------parse argument
    if(arg.length != 4){
      System.err.println("Usage: MainEngine <EngineKey> <ApiKey> <query> <precision>");
      System.exit(1);   
    }
    // Engine Key
    engineKey = args[0];
    // API Key
    apiKey = args[1];
    //Query
    query = args[2].split(" ");
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
        System.println("reached ideal precision");
        System.exit(1);
      }
      
        while(true){
        query = expand(query);
        Search(engineKey, apiKey, top, query);
        Feedback();
        if(checkPrecision(realPrecision)){
        System.println("reached ideal precision");
        System.exit(1);
        }
        
}