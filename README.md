# searchEngine
Project 1 Group 28
Name:	Jing Luo 	Uni:	jl4259
Name:	Yoki Yuan	Uni:	yy2738
———————————————————————————————————————————————
Files submitted:
project1.jar  //runnable jar file of the program
MainEngine.java  //source code of the java file
queryTranscript.txt  //queryTranscript of the three queries: jaguar, per se, and brin
README.txt
———————————————————————————————————————————————
Description of how to run the program:
java -jar </filepath/project1.jar> <EngineKey> <apiKey> <query> <precision>

All libraries have been included in the jar file.

Installation for JAVA8 & JDK on Ubuntu:

First, install default JRE/JDK
sudo apt-get update
sudo apt-get install default-jre
sudo apt-get install deault-jdk

Second step, update java version
sudo apt-get update
sudo apt-get install oracle-java8-installer

———————————————————————————————————————————————
Internal Design of the project:

The program has one class containing all necessary methods and functions to run the program. in the main class MainEngine, the main function first parses four arguments of EngineKey, ApiKey, query string, and precision. Error message will be printed out if the arguments are incorrect and exit the program. After parsing those values, the function will call the Search method which takes arguments of the engine key, apikey, and query and uses google custom search api to run the search. The top ten results are automatically returned. The program formatted the result into a list of list with each list containing a list of four strings: title, url, snippet, and a last value for marking if the result is relevant or not to the user after feedback. The program then calls the method Feedback to get user feed back on the rawResult. Feedback method returns the calculated precision. if the precision is smaller than the input ideal precision, the program will enter the loop of expand-search-feedback-check-expand. in the loop, the program first calls the expand method to modify the query with given relevant results from the previous round. It then passes this newquery into the search method and returns a new rawResult that feeds back to the Feedback method. Feedback then get user’s opinion and calculate precision. This loop will end until the precision reaches the ideal precision or it becomes zero. 
The work flow looks like this:

	|
  |-—> \|/  [enginekey,apikey,query,precision]
  |   [search]
  |	|
  |    \|/  [rawResult]
  | [Feedback]
  |	|
  |    \|/  [relevant rawResult, precision]
  | [checkPrecision]   
  |	|   
  |    \|/  —————> exit if precision reaches ideal precision or 0;	
  |  [expand]
  |	|
  |     |   [newquery]
  |_____|  	
———————————————————————————————————————————————
query-modification method:

This project is a classic relevance feedback model which takes user input into consideration.Our modification method falls into three parts:

Data cleansing:

rawResult is made of a list of list of string[]: [ [result1],[result2],[result3]…],
each result is made of [[url string],[title string],[content string],[relevant mark]].
The program replaces all the non-alphanumerical characters with a whitespace, and eliminate irrelevant words using stop-word list when calculating term frequency.Because non-alphanumerical words have been replaced with white space, things like <b/> and other formatting symbols will be eliminated with stop-word list. 
 
term frequency calculation:

Because we cannot delete words from original query, the classic way of Rocchio’s algorithm is not the best idea because we do not want to accidentally remove original words from the query. In this program we only take the top 10 results into consideration so we have a relatively small pool of result. We modified the weighing scheme of df-idf simply into df*tf. For the term frequency, we weighed different sections of the results differently, with title section having a count of 5 for each appearance, url has a count of 1 and content has a count of 3. Addition to the hashmap that keeps track of word appearance in all documents, there is another hashmap that record the document number that word has appeared in. Because the program runs three loop program to count all three list of url, content, and title, this hashmap will not count repetitive appearance for terms appeared in a single document in all three sections. 

Overall weighing score:

After completing term frequency weighing, each term is multiplied by it’s appearance time in documents. so a term that appeared in three documents that in total appeared in title once, content twice and once in url’s weight is simply: (5+3*2+1)*3
The program then sorts the entries of the hashmap containing terms and their weighs using collections.sort with modified compare method. The original query is automatically added to the new query and from this list of weighing score, we simply pick the top two to add to the new query. In this step, we do not include any word that is a combination of the original query or contains original query without whitespace. 
———————————————————————————————————————————————
Result:

Although our program uses a quite simple algorithm to modify query, the result confirms with the reference implementation. The modified query is the same as the reference implementation and it reaches precision with only one pass. We checked other query on “columbia” and “barnard” by only selecting relevant terms for columbia university and barnard college, the new query after one pass becomes “columbia university york” and “barnard college women”.  
———————————————————————————————————————————————
Engine API Key:	014169872148657544733:h23d3xx2jqg
Engine ID:	AIzaSyDoxgH303sENS1T_dXNfgUkSjY_5mfOIk8



 
