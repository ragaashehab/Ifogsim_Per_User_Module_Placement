package org.fog.utils;

public class Config {

	public static final double RESOURCE_MGMT_INTERVAL = 100; //100;
	public static int MAX_SIMULATION_TIME = 6000;    //  100000   ECG 6000/5000  ----PR 600000=600s=10minute    //50000;   // 10000;  // 1000ms =1s
	public static int RESOURCE_MANAGE_INTERVAL = 10;   //100  //10
	public static String FOG_DEVICE_ARCH = "x86";
	public static String FOG_DEVICE_OS = "Linux";
	public static String FOG_DEVICE_VMM = "Xen";
	public static double FOG_DEVICE_TIMEZONE = 10.0;
	public static double FOG_DEVICE_COST = 3.0;
	public static double FOG_DEVICE_COST_PER_MEMORY = 0.05;
	public static double FOG_DEVICE_COST_PER_STORAGE = 0.001;
	public static double FOG_DEVICE_COST_PER_BW = 0.0;
}
