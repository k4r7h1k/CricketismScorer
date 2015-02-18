package com.cricketism.scorer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainClass {
    public static HashMap<String, Integer> playerPoints = new HashMap<String, Integer>();
    private static String[] SML = { "RG Sharma", "DJG Sammy", "NL McCullum", "RML Taylor", "GD Elliott", "TG Southee", "GC Wilson", "V Kohli", "Sohail Khan", "AC Evans",
            "Taskin Ahmed", "Shapoor Zadran", "Gulbadin Naib", "Kamran Shazad" };
    private static String[] ACU = { "AM Rahane", "Mohammad Irfan", "Q de Kock", "Sikandar Raza", "MM Ali", "KS Williamson", "JE Taylor", "Mahmudullah", "Aftab Alam",
            "RD Berrington", "MJ Clarke", "CJ Jordan", "PKD Chase", "Amjad Ali" };
    private static String[] AJ = { "KC Sangakkara", "DA Warner", "AM Phangiso", "Mirwais Ashraf", "Arafat Sunny", "Farhaan Behardien", "Imran Tahir", "Luke Ronchi",
            "Iain Wardlaw", "SC Williams", "CR Ervine", "HMRKB Herath", "KJ Coetzer", "GS Ballance" };
    private static String[] WM = { "SR Thompson", "Sabbir Rahman", "Nawroz Mangal", "Krishna Chandran", "MN Samuels", "UT Yadav", "TA Boult", "ST Finn", "CS MacLeod", "DR Smith",
            "AF Milne", "MH Cross", "SR Watson", "Nasir Jamshed" };

    public static HashMap<String, Integer> teamPoints = new HashMap<String, Integer>();

    public static void main(String args[]) throws IOException {
        // Get Document from URL
        Document doc = Jsoup.connect("http://www.espncricinfo.com/icc-cricket-world-cup-2015/content/series/509587.html?template=fixtures").get();
        // JQuery style Selector, gets all <a> inside play_team css class
        Elements matchesLink = doc.select(".play_team a");
        for (Element e : matchesLink) {
            // Gets href of <a>
            parseScorecard(e.attr("abs:href"));
        }
        teamPoints.put("Srini Mama Loyalists", calculateTeamPoints(SML));
        teamPoints.put("A-Class United", calculateTeamPoints(ACU));
        teamPoints.put("Aadama Jaichomada", calculateTeamPoints(AJ));
        teamPoints.put("Wicked Maidens", calculateTeamPoints(WM));
        printMap(sortByComparator(playerPoints, false));
    }

    public static int calculateTeamPoints(String[] playerNames) {
        int points = 0;
        for (String player : playerNames) {
            points += playerPoints.getOrDefault(player, 0);
        }
        return points;
    }

    public static void parseScorecard(String link) throws IOException {
        String matchCode=link.replaceAll(".*match", "");
        link = link += "?view=scorecard";
        // Timeout to connect statement to avoid Timedoutexception, if you get
        // same exception increase timeout
        File f = new File("cache"+matchCode);
        Document doc;
        if(f.exists() && !f.isDirectory()) { 
            /* do something */
            doc = Jsoup.parse(f,"utf-8","http://www.espncricinfo.com/");
        }
        else{
            doc=Jsoup.connect(link).timeout(10000).get();
            if(!link.contains("current")){
                cacheHTML(doc.html(),"cache"+matchCode);
            }
            
        }
        Elements playerNames = doc.select(".playerName");
        Set<String> players= new HashSet<String>();
        for (Element player : playerNames) players.add(player.html());
        // Gets all elements with batsman-name css class
        Elements batsmanName = doc.select(".batsman-name");
        for (Element batsman : batsmanName) {
            batsman = batsman.parent();
            int numberOfChild = batsman.children().size();
            if (numberOfChild < 2)
                continue;
            String name = batsman.child(1).select("a").get(0).html();
            int runs = Integer.parseInt(batsman.child(3).html());
            int balls = Integer.parseInt(batsman.child(numberOfChild - 4).html());
            int fours = Integer.parseInt(batsman.child(numberOfChild - 3).html());
            int six = Integer.parseInt(batsman.child(numberOfChild - 2).html());
            String srate = batsman.child(numberOfChild - 1).html();
            float sr = 0;
            if (!srate.equals("-"))
                sr = Float.parseFloat(srate);
            playerPoints.put(name, playerPoints.getOrDefault(name, 0) + calculateBattingPoints(runs, balls, fours, six, sr));

        }
        Elements bowlerName = doc.select(".bowler-name");
        for (Element bowler : bowlerName) {
            bowler = bowler.parent();
            int numberOfChild = bowler.children().size();
            if (numberOfChild < 2)
                continue;
            String name = bowler.child(1).select("a").get(0).html();
            float over = Float.parseFloat(bowler.child(2).html());
            int maidens = Integer.parseInt(bowler.child(3).html());
            int wickets = Integer.parseInt(bowler.child(5).html());
            float rpo = Float.parseFloat(bowler.child(6).html());
            playerPoints.put(name, playerPoints.getOrDefault(name, 0) + calculateBowlingPoints(over, maidens, wickets, rpo));
        }
        String momInfo = doc.select(".match-information").get(1).child(1).select("span").get(0).html().replaceAll("\\s\\(.*\\)$", "");
        playerPoints.put(momInfo, playerPoints.getOrDefault(momInfo, 0) + 100);
        
        for (Element batsman : batsmanName) {
            batsman = batsman.parent();
            int numberOfChild = batsman.children().size();
            if (numberOfChild < 2)
                continue;
            String dismissalMode = batsman.child(2).html().replaceAll("\\s<span.*$", "").replace("†", "");
            // #TODO Handle Dismissal mode
            if (dismissalMode.matches("^c .* b .*$")) {
                String fielder="";
                String catcherName = dismissalMode.replaceAll("\\sb\\s.*", "").replaceAll("^c\\s", "");
                if (catcherName.equals("&amp;")) {
                    catcherName = dismissalMode.replaceAll("^c &amp; b ", "");
                }
                int count = 0;
                for (String player:players){
                    if(player.contains(catcherName)){
                        fielder=player;
                        count++;
                    }
                }
                if(count==1){
                    playerPoints.put(fielder, playerPoints.getOrDefault(fielder, 0) + 20);
                }
                else handleFieldingPointCase(catcherName, link);
            } else if (dismissalMode.matches("^st .* b .*$")) {
                String stumper = dismissalMode.replaceAll("\\sb\\s.*", "").replaceAll("^st\\s", "");
                int count=0;
                for (String player:players){
                    if(player.contains(stumper)){
                        stumper=player;
                        count++;
                    }
                }
                if(count==1){
                    playerPoints.put(stumper, playerPoints.getOrDefault(stumper, 0) + 25);
                }
                else handleFieldingPointCase(stumper, link);
            } else if (dismissalMode.matches("^run out.*$")) {
                String[] runout=dismissalMode.replaceAll("^run out \\(", "").replace(")", "").split("/");
                int roP= runout.length==1?50:25;
                for (String fielder:runout){
                    int count = 0;
                    for (String player:players){
                        if(player.contains(fielder)){
                            fielder=player;
                            count++;
                        }
                    }
                    if (count==1){
                        playerPoints.put(fielder, playerPoints.getOrDefault(fielder, 0) + roP);
                    }
                    else handleFieldingPointCase(fielder, link);
                }
            }
        }
    }
    public static void handleFieldingPointCase(String fielderName,String match){
        System.out.println("Two Matching player names for the fielder "+fielderName+" in this match "+match );
    }
    public static int calculateBattingPoints(int run, int balls, int fours, int sixes, float sr) {
        int points = 0;
        points += run;
        points += (4 * fours);
        points += (6 * sixes);
        if (run >= 100)
            points += 75;
        else if (run >= 75)
            points += 60;
        else if (run >= 50)
            points += 40;
        else if (run >= 25)
            points += 20;
        if (run >= 20 || balls >= 15) {
            if (sr >= 175)
                points += 75;
            else if (sr >= 135)
                points += 60;
            else if (sr >= 100)
                points += 40;
            else if (sr >= 90)
                points += 25;
            else if (sr >= 80)
                points += 10;
            else if (sr >= 70)
                points += 0;
            else if (sr >= 50)
                points -= 10;
            else
                points -= 25;
        }
        return points;
    }

    public static int calculateBowlingPoints(float overs, int maidens, int wicket, float eco) {
        int points = 0;
        points += (25 * wicket);
        points += (10 * maidens);
        if (wicket >= 6)
            points += 75;
        else if (wicket >= 5)
            points += 50;
        else if (wicket >= 4)
            points += 40;
        else if (wicket >= 3)
            points += 30;
        else if (wicket >= 2)
            points += 20;
        if (overs >= 3) {
            if (eco >= 7)
                points -= 40;
            else if (eco >= 6)
                points -= 20;
            else if (eco >= 5)
                points -= 0;
            else if (eco >= 4)
                points += 20;
            else if (eco >= 3)
                points += 40;
            else if (eco >= 2)
                points += 60;
            else
                points += 75;
        }
        return points;
    }

    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void printMap(Map<String, Integer> map) {
        for (Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
    public static void cacheHTML(String htmlContent, String saveLocation) throws IOException {
        Writer writer =
                new OutputStreamWriter(
                   new FileOutputStream(saveLocation), "UTF-8");
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(htmlContent.toString());
        bufferedWriter.close();
        System.out.println("Downloading completed successfully..!");
    }
}
