package com.winterwell.moneyscript.lang.num;

import com.winterwell.maths.stats.distributions.d1.MeanVar1D;

public class StudentsTTest {

	private MeanVar1D sample1;
	private MeanVar1D sample2;

	public StudentsTTest(MeanVar1D sample1, MeanVar1D sample2) {
		this.sample1 = sample1;
		this.sample2 = sample2;
	}

	/**
	 * 
	 * @return t statistic, using Welch's t-test
	 */
	public double getTStatistic() {
		double dMean = sample1.getMean() - sample2.getMean();
		// https://en.wikipedia.org/wiki/Student%27s_t-test#Calculations
		// For different sample sizes & variances
		double sbar = Math.sqrt(sample1.getVariance()/sample1.getCount()) + (sample2.getVariance()/sample2.getCount());
		return dMean / sbar;
	}
}
