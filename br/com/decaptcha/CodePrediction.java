package br.com.decaptcha;
import java.util.ArrayList;



public class CodePrediction {
	private ArrayList<Prediction> predictions;
	
	public CodePrediction(ArrayList<Prediction> predictions)
	{
		this.predictions = predictions;
	}
	
	public String getCode()
	{
		String code = "";
		for(Prediction p:predictions)
		{
			code = code.concat(p.getCharacter());
		}
		return code;
	}
	public ArrayList<Prediction> getPredictions()
	{
		return this.predictions;
	}
	
}
