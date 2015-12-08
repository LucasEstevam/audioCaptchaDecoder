package br.com.decaptcha;

public class Prediction {
	private String character;
	private double probability;
	
	public String getCharacter()
	{
		return this.character;
	}
	
	public double getProbability()
	{
		return this.probability;
	}
	
	public Prediction(String character, double probability)
	{
		this.character = character;
		this.probability = probability;		
	}

	
}
