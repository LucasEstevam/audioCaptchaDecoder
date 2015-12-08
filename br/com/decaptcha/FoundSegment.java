package br.com.decaptcha;
import org.ioe.tprsa.audio.feature.FeatureVector;


public class FoundSegment {
	
	private double[] buffer;
	private String character;
	private FeatureVector vector;
	
	public FoundSegment(double[] buffer, String character, FeatureVector vector)
	{
		this.buffer = buffer;
		this.character = character;
		this.vector = vector;
	}
	
	public String getCharacter()
	{
		return this.character;
	}
	
	public FeatureVector getVector()
	{
		return this.vector;
	}
	
	public double[] getBuffer()
	{
		return this.buffer;
	}
	
	public void setCharacter(String value)
	{
		this.character = value;
	}
	
	public double getMaxPower()
	{
		double maxPower = 0;
		for(int i = 0; i < buffer.length; i++)
		{
			if(buffer[i]*buffer[i] > maxPower)
			{
				maxPower = buffer[i]*buffer[i];
			}
		}
		return maxPower;
	}

}
