package com.cricketism.scorer;

public class Player {
	String name;
	int points;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getPoints() {
		return points;
	}
	public void setPoints(int points) {
		this.points = points;
	}
	public Player(String name) {
		super();
		this.name = name;
		this.points = 0;
	}
	public void addPoints(int point){
		this.points+=point;
	}
	public Player() {
		super();
		// TODO Auto-generated constructor stub
	}
	
}
