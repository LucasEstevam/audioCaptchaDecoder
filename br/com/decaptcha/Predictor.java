package br.com.decaptcha;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;


public class Predictor {
	
	private double y_lower;
	private double y_upper;
	private boolean y_scaling = false;
	private double[] feature_max;
	private double[] feature_min;
	private double lower = -1.0;
	private double upper = 1.0;
	private double y_max = -Double.MAX_VALUE;
	private double y_min = Double.MAX_VALUE;
	private svm_model model;
	private static String charRef = "0123456789abcdefghijklmnopqrstuvwxyz";
	
	public Predictor(String modelFile, String scalingFile)
	{
		//read scaling file
		int idx, c;
		int max_index = 0;
		double fmin, fmax;
		try {			
			BufferedReader fp_restore = new BufferedReader(new FileReader(scalingFile));
			if((c = fp_restore.read()) == 'y')
			{
				fp_restore.readLine();
				fp_restore.readLine();		
				fp_restore.readLine();		
			}
			fp_restore.readLine();
			fp_restore.readLine();

			String restore_line = null;
			while((restore_line = fp_restore.readLine())!=null)
			{
				StringTokenizer st2 = new StringTokenizer(restore_line);
				idx = Integer.parseInt(st2.nextToken());
				max_index = Math.max(max_index, idx);
			}
			fp_restore = rewind(fp_restore, scalingFile);
			
			feature_max = new double[(max_index+1)];
			feature_min = new double[(max_index+1)];
			
			fp_restore.mark(2);				// for reset
			if((c = fp_restore.read()) == 'y')
			{
				fp_restore.readLine();		// pass the '\n' after 'y'
				StringTokenizer st = new StringTokenizer(fp_restore.readLine());
				y_lower = Double.parseDouble(st.nextToken());
				y_upper = Double.parseDouble(st.nextToken());
				st = new StringTokenizer(fp_restore.readLine());
				y_min = Double.parseDouble(st.nextToken());
				y_max = Double.parseDouble(st.nextToken());
				y_scaling = true;
			}
			else
				fp_restore.reset();

			if(fp_restore.read() == 'x') {
				fp_restore.readLine();		// pass the '\n' after 'x'
				StringTokenizer st = new StringTokenizer(fp_restore.readLine());
				lower = Double.parseDouble(st.nextToken());
				upper = Double.parseDouble(st.nextToken());
				
				while((restore_line = fp_restore.readLine())!=null)
				{
					StringTokenizer st2 = new StringTokenizer(restore_line);
					idx = Integer.parseInt(st2.nextToken());
					fmin = Double.parseDouble(st2.nextToken());
					fmax = Double.parseDouble(st2.nextToken());
					if (idx <= max_index)
					{
						feature_min[idx] = fmin;
						feature_max[idx] = fmax;
					}
				}
			}
			fp_restore.close();
			
			
		}
		catch (Exception e) {
			System.err.println("can't open file " + scalingFile);		
		}
		
		try {
			model = svm.svm_load_model(modelFile);
			
		} catch (IOException e) {
			System.err.println("can't open file " + modelFile);
		}
		
		
	}
	
	private double scale(int index, double value)
	{
		double result;
		if(feature_max[index] == feature_min[index])
			return value;
		
			if(value == feature_min[index])
				result = lower;
			else if(value == feature_max[index])
				result = upper;
			else
				result = lower + (upper-lower) * 
					(value-feature_min[index])/
					(feature_max[index]-feature_min[index]);
		
		return result;
	}
	
	private BufferedReader rewind(BufferedReader fp, String filename) throws IOException
	{
		fp.close();
		return new BufferedReader(new FileReader(filename));
	}
	
	
	public Prediction predict(double[] features)
	{		
		double probs[] = new double[model.nr_class];
		int[] labels=new int[model.nr_class];
		svm.svm_get_labels(model,labels);
		svm_node[] x = new svm_node[features.length];
		for(int i =0 ; i< features.length; i++)
		{
			x[i] = new svm_node();
			x[i].index = i+1;
			x[i].value = scale(i+1,features[i]);
			
		}
		
		double v = svm.svm_predict_probability(model, x, probs);
		
		double prob = 1;
		for(int j = 0; j < labels.length; j++)
		{
			if(labels[j] == (int) v)
			{
				prob = probs[j];
			}
		}
		
		Prediction pred = new Prediction(charRef.substring((int)v,(int)v + 1),prob );
		
		return pred;
	}

}
