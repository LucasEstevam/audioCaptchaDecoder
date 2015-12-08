package br.com.decaptcha;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Decoder {
	private SegmentFinder finder;
	private Predictor p;
	
	public Decoder(Predictor p)
	{
		this.finder = new SegmentFinder();
		this.p = p;
	}
	
	public CodePrediction decode(WavFile file)
	{
			
		ArrayList<Prediction> predictions = new ArrayList<Prediction>();
		List<FoundSegment> segments = finder.findSegments(file);
		
		while(segments.size() > 5)
		{
			//extra stuff was found. try to use anyway discarding last segments
			segments.remove(5);
		}
		
		for(FoundSegment s : segments)
		{			
			Map<String, Double> probabilities = new HashMap<String,Double>();
			double[][] features = s.getVector().getMfccFeature();
			for(int i =0; i<features.length; i++)
			{
				//do prediction and get probability
				//add to dictionary
				Prediction pred = p.predict(features[i]);
				if(!probabilities.containsKey(pred.getCharacter()))
				{
					probabilities.put(pred.getCharacter(), pred.getProbability());
				}else
				{
					probabilities.put(pred.getCharacter(), probabilities.get(pred.getCharacter())+ pred.getProbability() );
				}				
			}		
			double maxProb = 0;
			String maxString = "";
			for(Map.Entry<String, Double> e: probabilities.entrySet())				
			{
				if(e.getValue() > maxProb)
				{
					maxProb = e.getValue();
					maxString = e.getKey();
				}				
			}
			predictions.add(new Prediction(maxString,maxProb));			
			
		}
		
		
		return new CodePrediction(predictions);	
	}

}
