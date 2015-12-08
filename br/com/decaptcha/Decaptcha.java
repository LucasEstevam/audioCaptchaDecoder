package br.com.decaptcha;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.ioe.tprsa.audio.FeatureExtract;
import org.ioe.tprsa.audio.PreProcess;
import org.ioe.tprsa.audio.feature.FeatureVector;



public class Decaptcha {
	
	private static double noiseInterval = 0.2;
	private static double soundInterval = 0.25;
	private static double windowSize = 0.01;
	private static String charRef = "0123456789abcdefghijklmnopqrstuvwxyz";
	private static int samplePerFrame = 1024;
	
	private static int findMaxAbs(double[] buffer)
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
	
	public static void main(String[] args) {
		
		//readTrain();
		//readTest();
		
		Predictor p = new Predictor("train.scale.model","train_scaling.out");
		Decoder d = new Decoder(p);
		
		//double v[] = {107.5097, -5.2335, 1.9824, 3.5233, -4.2744, -4.4463, 0.1876, 1.1636, 0.4034, -0.5985, -0.5716, 0.0199};
		//p.predict(v);
		
		final File folder = new File("test/");	
		
		int filecount = 0;
		int right = 0;
		int wrong = 0;
		int rightLength = 0;
		int wrongLength = 0;
		int rightLetters = 0;
		int wrongLetters = 0;
		for(final File file : folder.listFiles())
		{	
			
				
				filecount++;
				
				System.out.println("Handling file " + filecount);
				int currentChar = 0;
				String characters = file.getName().substring(0, file.getName().indexOf("."));
				
				try {
					FileInputStream stream = new FileInputStream(file);
					WavFile wavFile = WavFile.openWavFile(stream);
					String code = d.decode(wavFile).getCode();
					System.out.println("Decoded: " + code + " - real: "+ characters);
					if(code.equals(characters))
					{
						right++;
					}else
					{
						wrong++;
					}
					
					if(code.length() == characters.length())
					{
						rightLength++;
						for(int i = 0; i < code.length(); i++)
						{
							if(code.charAt(i) == characters.charAt(i))
							{
								rightLetters++;
							}else
							{
								wrongLetters++;
							}
						}
					}else
					{
						wrongLength++;
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WavFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		}
		
		System.out.println("Total right: " + right);
		System.out.println("Total wrong: " + wrong);
		System.out.println("Length right: " + rightLength);
		System.out.println("Length wrong: " + wrongLength);
		System.out.println("Letters right: " + rightLetters);
		System.out.println("Letters wrong: " + wrongLetters);
		
		

	}	
	
	private static void train()
	{
		File trainingData = new File("train.out");
	}
	
	
	private static void readTrain()
	{
		List<FoundSegment> trainingSegments = new LinkedList<FoundSegment>();
		int segErrors = 0;
		int filecount = 0;
		final File folder = new File("train/");
		
		
		
		for(final File file : folder.listFiles())
		{			
			
			try {
				
				filecount++;
				System.out.println("Handling file " + filecount);
				int currentChar = 0;
				String characters = file.getName().substring(0, file.getName().indexOf("."));
				
				WavFile wavFile = WavFile.openWavFile(file);
				wavFile.display();

				long sampleRate = wavFile.getSampleRate();
				
				double[] noiseBuffer = new double[(int)(sampleRate*noiseInterval)];
				int totalRead = wavFile.readFrames(noiseBuffer, noiseBuffer.length);
				double maxNoise = noiseBuffer[findMaxAbs(noiseBuffer)];
				double noiseThreshold = 1.8*maxNoise*maxNoise;	
				
				List<FoundSegment> segments = new ArrayList<FoundSegment>();
				
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
						if(currentChar >= characters.length())
						{
							overflow = true;
							break;
						}
						
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
						
						segments.add(new FoundSegment(soundBuffer,characters.substring(currentChar,currentChar+1), vec));
						currentChar++;
						
					}				
				}
				
				if(segments.size() == characters.length() && !overflow)
				{
					for(FoundSegment s : segments)
					{
						trainingSegments.add(s);
					}
									
				}else
				{
					segErrors++;
					System.out.println("Seg errors: " + segErrors);
				}
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WavFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("Total seg errors: "+segErrors );
		System.out.println("Total training segments: "+ trainingSegments.size());
		File trainingFile = new File("train.out");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(trainingFile));
			for(FoundSegment s : trainingSegments)
			{
				
				for(int i = 0; i < s.getVector().getMfccFeature().length; i++)
				{
					writer.write(charRef.indexOf(s.getCharacter()) +" ");
					for(int j = 0; j < s.getVector().getMfccFeature()[i].length; j++)
					{
						writer.write((j+1) + ":" + s.getVector().getMfccFeature()[i][j] + " ");
					}
					writer.write("\n");
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	

	private static void readTest()
	{
		List<FoundSegment> trainingSegments = new LinkedList<FoundSegment>();
		int segErrors = 0;
		int filecount = 0;
		final File folder = new File("test/");	
		
		
		for(final File file : folder.listFiles())
		{	
			
			try {		
				filecount++;
				System.out.println("Handling file " + filecount);
				int currentChar = 0;
				String characters = file.getName().substring(0, file.getName().indexOf("."));
				
				WavFile wavFile = WavFile.openWavFile(file);
				wavFile.display();
				int offset;
				long sampleRate = wavFile.getSampleRate();
				long numFrames = wavFile.getNumFrames();
				int numBits = wavFile.getValidBits();
				
				double[] noiseBuffer = new double[(int)(sampleRate*noiseInterval)];
				int totalRead = wavFile.readFrames(noiseBuffer, noiseBuffer.length);
				double maxNoise = noiseBuffer[findMaxAbs(noiseBuffer)];
				double noiseThreshold = 1.8*maxNoise*maxNoise;	
				
				List<FoundSegment> segments = new ArrayList<FoundSegment>();
				
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
						if(currentChar >= characters.length())
						{
							overflow = true;
							break;
						}
						
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
						
						segments.add(new FoundSegment(soundBuffer,characters.substring(currentChar,currentChar+1), vec));
						currentChar++;
						
					}				
				}
				
				if(segments.size() == characters.length() && !overflow)
				{
					for(FoundSegment s : segments)
					{
						trainingSegments.add(s);
					}
									
				}else
				{
					segErrors++;
					System.out.println("Seg errors: " + segErrors);
				}
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WavFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("Total seg errors: "+segErrors );
		System.out.println("Total training segments: "+ trainingSegments.size());
		File trainingFile = new File("test.out");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(trainingFile));
			for(FoundSegment s : trainingSegments)
			{
				
				for(int i = 0; i < s.getVector().getMfccFeature().length; i++)
				{
					writer.write(charRef.indexOf(s.getCharacter()) +" ");
					for(int j = 0; j < s.getVector().getMfccFeature()[i].length; j++)
					{
						writer.write((j+1) + ":" + s.getVector().getMfccFeature()[i][j] + " ");
					}
					writer.write("\n");
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
