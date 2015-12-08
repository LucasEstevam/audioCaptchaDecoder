package br.com.decaptcha;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ioe.tprsa.audio.FeatureExtract;
import org.ioe.tprsa.audio.PreProcess;
import org.ioe.tprsa.audio.feature.FeatureVector;



public class SegmentFinder {
	
	private static double noiseInterval = 0.2;
	private static double soundInterval = 0.25;
	private static double windowSize = 0.01;
	private static String charRef = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static int samplePerFrame = 1024;
	
	private int findMaxAbs(double[] buffer)
	{
		double max = 0;
		int maxloc = 0;
		for(int i = 0; i < buffer.length; i++)
		{
			if(Math.abs(buffer[i]) > max)
			{
				max = Math.abs(buffer[i]);
				maxloc = i;
			}
		}
		return maxloc;
	}
	
	
	public List<FoundSegment> findSegments(WavFile wavFile)
	{
		List<FoundSegment> segments = new ArrayList<FoundSegment>();
		
		try{
		
			long sampleRate = wavFile.getSampleRate();
			
			double[] noiseBuffer = new double[(int)(sampleRate*noiseInterval)];
			int totalRead = wavFile.readFrames(noiseBuffer, noiseBuffer.length);
			double maxNoise = noiseBuffer[findMaxAbs(noiseBuffer)];
			double noiseThreshold = 1.8*maxNoise*maxNoise;		
			
			boolean done = false;
			boolean overflow = false;	
		
			while(!done)
			{
				double[] buf = new double[(int)(sampleRate*windowSize)];
				
				totalRead = wavFile.readFrames(buf, buf.length);
				
				if(totalRead < buf.length)
				{
					//at the end of the file. give up.
					done = true;
					break;					
				}
				
				double maxBuf = buf[findMaxAbs(buf)];
				
				if(maxBuf*maxBuf > noiseThreshold)
				{
					//sound has been found.
					
					//read 1 second around it
					double[] soundBuffer = new double[(int)(sampleRate*soundInterval)];
					System.arraycopy(buf, 0, soundBuffer, 0, buf.length);//copy buf to sound array			
					
					wavFile.readFrames(soundBuffer, soundBuffer.length - buf.length); // read some more to sound array					
					double[] discardBuffer = new double[(int)(sampleRate*(1-soundInterval))];
					wavFile.readFrames(discardBuffer, discardBuffer.length);
					
					//WavFile file2 = WavFile.newWavFile(new File("temp.wav"),1, soundBuffer.length, numBits, sampleRate);
					//file2.writeFrames(soundBuffer, soundBuffer.length);
					//file2.close();
				
					PreProcess prp = new PreProcess(soundBuffer, samplePerFrame, (int)sampleRate);
					FeatureExtract fExt = new FeatureExtract(prp.framedSignal, (int)sampleRate, samplePerFrame);
					fExt.makeMfccFeatureVector();
					FeatureVector vec = fExt.getFeatureVector();
					
					segments.add(new FoundSegment(soundBuffer,"", vec));
					
					
				}				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (WavFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return segments;
	}

}
