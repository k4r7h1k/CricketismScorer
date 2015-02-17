package com.cricketism.scorer;

import java.io.IOException;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class MainClass {
	public static HashMap<String,Integer> playerPoints = new HashMap<String,Integer>();
	public static void main(String args[]) throws IOException {
		Document doc = Jsoup.connect("http://www.espncricinfo.com/icc-cricket-world-cup-2015/content/series/509587.html?template=fixtures").get();
		Elements matchesLink = doc.select(".play_team a");
		for (Element e:matchesLink){
			System.out.println(e.attr("abs:href"));
			parseScorecard(e.attr("abs:href"));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		for (String player:playerPoints.keySet()){
			System.out.println(player+" "+playerPoints.get(player));
		}
	}
	public static void parseScorecard(String link) throws IOException{
		link = link+="?view=scorecard";
		Document doc = Jsoup.connect(link).timeout(5000).get();
		Elements batsmanName = doc.select(".batsman-name");
		String returnData=new String();
		for (Element batsman:batsmanName){
			batsman=batsman.parent();
			int numberOfChild = batsman.children().size();
			if (numberOfChild<2)
				continue;
			String name=batsman.child(1).select("a").get(0).html();
			//String dismissalMode=batsman.child(2).html().replaceAll("\\s<span.*$", "");
			int runs = Integer.parseInt(batsman.child(3).html());
			int balls = Integer.parseInt(batsman.child(numberOfChild-4).html());
			int fours = Integer.parseInt(batsman.child(numberOfChild-3).html());
			int six = Integer.parseInt(batsman.child(numberOfChild-2).html());
			String srate=batsman.child(numberOfChild-1).html();
			float sr=0;
			if (!srate.equals("-"))
			 sr= Float.parseFloat(srate);
			if (playerPoints.containsKey(name)){
				playerPoints.put(name, playerPoints.get(name)+calculateBattingPoints(runs, balls, fours, six, sr));
			}
			else{
				playerPoints.put(name, calculateBattingPoints(runs, balls, fours, six, sr));
			}
		}
		Elements bowlerName = doc.select(".bowler-name");
		for (Element bowler:bowlerName){
			bowler=bowler.parent();
			int numberOfChild = bowler.children().size();
			if (numberOfChild<2)
				continue;
			String name=bowler.child(1).select("a").get(0).html();
			float over=Float.parseFloat(bowler.child(2).html());
			int maidens = Integer.parseInt(bowler.child(3).html());
			int wickets=Integer.parseInt(bowler.child(5).html());
			float rpo=Float.parseFloat(bowler.child(6).html());
			if (playerPoints.containsKey(name)){
				playerPoints.put(name, playerPoints.get(name)+calculateBowlingPoints(over, maidens, wickets, rpo));
			}
			else{
				playerPoints.put(name, calculateBowlingPoints(over, maidens, wickets, rpo));
			}
			
		}
		String momInfo = doc.select(".match-information").get(1).child(1).select("span").get(0).html();
		momInfo=momInfo.replaceAll("\\s\\(.*\\)$", "");
		if (playerPoints.containsKey(momInfo)){
			playerPoints.put(momInfo, playerPoints.get(momInfo)+100);
		}
		else{
			playerPoints.put(momInfo, 100);
		}
		for (Element batsman:batsmanName){
			batsman=batsman.parent();
			int numberOfChild = batsman.children().size();
			if (numberOfChild<2)
				continue;
			String dismissalMode=batsman.child(2).html().replaceAll("\\s<span.*$", "");
			//#TODO Handle Dismissal mode
			
		}
	}
	public static int calculateBattingPoints(int run,int balls, int fours, int sixes, float sr){
		int points=0;
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
                points -=25;
        }
		return points;
	}
	public static int calculateBowlingPoints(float overs,int maidens, int wicket, float eco){
		int points=0;
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
}
